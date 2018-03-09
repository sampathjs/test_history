package com.olf.jm.metalutilisationstatement.tpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalutilisationstatement.MetalsUtilisationConstRepository;
import com.olf.openjvs.OException;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.Document;
import com.olf.openrisk.backoffice.DocumentDefinition;
import com.olf.openrisk.backoffice.DocumentProcessResult;
import com.olf.openrisk.backoffice.DocumentStatus;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.Transaction;

/**
 * 
 * TPM plugin generates invoices for the given list of transaction numbers.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 15-Dec-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 18-Oct-2016 |               | J. Waechter     | Added setting of TPM variables for rerun control                                |
 * | 003 | 24-Oct-2016 |               | J. Waechter     | Added error handling and rerunning                                              |
 * | 004 | 26-Oct-2016 |               | J. Waechter     | taking care of corrupted documents from the first run                           | 
 * | 005 | 27-Oct-2016 |               | J. Waechter     | processing of documents to status 2 Sent to Cp has been converted from          | 
 * |     |             |               |                 | OC to JVS as OC does not allow processing without outputting and auto output    |
 * |     |             |               |                 | is switched on for invoices leading to two invoices being generated the same    |
 * |     |             |               |                 | time																			   |
 * | 006 | 01-Nov-2016 |               | J. Waechter     | replaced APM version of OLF_RETURN_CODE with core version                       |
 * |     |             |               |                 | now no longer outputting invoice when processing from                           |
 * |     |             |               |                 | 0 Undesignated to 1 Generated                                                   |
 * | 007 | 02-Nov-2016 |               | J. Waechter     | refactored rerunning to create new document in case it could not be generated   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class GenerateInvoices extends AbstractProcessStep {

	private MetalsUtilisationConstRepository constants = new MetalsUtilisationConstRepository();

	@Override
	public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
		Logging.init(context, getClass(), "", "");
		try {
			return process(context, variables);	        	
		} catch (Throwable t) {
			Logging.error("Encountered Exception", t);
			throw t;
		} finally {
			Logging.close();
		}
	}

	private Table process(Context context, Variables variables) {
		try {
			long wflowId = Tpm.getWorkflowId();
			Tpm.setVariable(wflowId, "PluginStart", "Yes");
			DocumentStatus docStatusSent2Cp = (DocumentStatus) context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DocumentStatus,
					"2 Sent to CP");
			DocumentStatus docStatusNewDocument = (DocumentStatus) context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DocumentStatus,
					"New Document");

			DocumentDefinition[] defs = context.getBackOfficeFactory().retrieveDocumentDefinitions();
			DocumentDefinition invoiceDef = null;
			for (DocumentDefinition def : defs) {
				if (def.getName().equals("Invoices")) {
					invoiceDef = def;
					Logging.info("Found 'Invoices' document definition");
					break;
				}
			}
			try (Table output = context.getTableFactory().createTable();
					Variable var = variables.getVariable("TranNums");
					ConstTable table = var.getValueAsTable();
					Table tranNums = table.asTable();
					Variable rvar = variables.getVariable("RetryCount");
					){

				int retryCount = rvar.getValueAsInt();
				// get distinct transactions
				Set<Integer> distTranNums = new HashSet<>();
				for (int row = tranNums.getRowCount()-1; row >= 0; row--) {
					int tranNum = tranNums.getInt("tran_num", row);
					distTranNums.add(tranNum);
				}
				
				output.addColumns("Int[tran_num], String[file_path]");

				DocumentDefinition def = getInvoiceDefinition(context);

				Set<Integer> docIds = getDistinctDocumentIds(context, distTranNums);

				for (int docId : docIds) { // retrieving documents in status generated only
					Document doc = context.getBackOfficeFactory().retrieveDocument(docId);
					com.olf.openjvs.Table events = getEventAndTranNum (context, doc);
					if (events.getNumRows() > 0) {
						processDocViaJVS(docStatusNewDocument, invoiceDef,
								events);
					}
					events.destroy();														
				}
				
				long[] eventNums = getRentalsInterestEventNums(context, tranNums, distTranNums);
				Logging.info("Generating invoices for event numbers " + Arrays.toString(eventNums));
				DocumentProcessResult results = null;
				if (eventNums != null && eventNums.length > 0) {
					results = def.process(eventNums, false, false);
				}
				docIds = getDistinctDocumentIds(context, distTranNums);
				if (docIds.size() <= 0) {
					throw new RuntimeException("No invoice document has been generated");
				}
				
				for (int docId : docIds) { // processing documents in status generated only
					boolean succeeded=false;
					int count=0;
					while (!succeeded && count++ < 4) {
						try {
							Logging.info("Processing invoice/credit note #" + docId + "  to next status");
							Document doc = context.getBackOfficeFactory().retrieveDocument(docId);
							com.olf.openjvs.Table events = getEventAndTranNum (context, doc);
							if (events.getNumRows() > 0) {
								succeeded = processDocViaJVS(docStatusSent2Cp, invoiceDef,
										events);
							}
							events.destroy();								
						} catch (Throwable t) {
							Logging.error("Could not process document #" + docId, t);
						}
					}
				}
				Tpm.setVariable(wflowId, "PluginEnd", "Yes");
				return null;

			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e); 
			}
		} catch (OException ex) {
			Logging.error("Error setting TPM variables", ex);
			throw new RuntimeException (ex);
		}
	}

	private boolean processDocViaJVS(DocumentStatus docStatusSent2Cp,
			DocumentDefinition invoiceDef, com.olf.openjvs.Table events)
			throws OException {
		boolean succeeded;
		com.olf.openjvs.Table processTable = com.olf.openjvs.Table.tableNew("Process Table");
		com.olf.openjvs.Table outputResults = com.olf.openjvs.Table.tableNew();

		events.addCol("next_doc_status", COL_TYPE_ENUM.COL_INT);
		events.setColValInt("next_doc_status", docStatusSent2Cp.getId());
		int result = StlDoc.processDocs(invoiceDef.getId(), 0, events, processTable, outputResults);
		if (result != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			succeeded=false;
		} else {
			succeeded=true;
		}
		processTable.destroy();
		outputResults.destroy();
		return succeeded;
	}

	private com.olf.openjvs.Table getEventAndTranNum(Session session, Document doc) throws OException {
		String sql = 
				"\nSELECT d.event_num, abe.tran_num"
			+   "\nFROM stldoc_details d"
			+   "\n INNER JOIN ab_tran_event abe"
			+ 	"\n   ON abe.event_num = d.event_num"
			+	"\nWHERE d.document_num=" + doc.getId()
				;
		return session.getTableFactory().toOpenJvs(session.getIOFactory().runSQL(sql));
	}

	/**
	 * Get the Invoices BO document definition.
	 * 
	 * @param context Current session context
	 * @return definition
	 */
	private DocumentDefinition getInvoiceDefinition(Context context) {
		String invoiceDefName = constants.getBoInvoiceDefinition();
		DocumentDefinition[] defs = context.getBackOfficeFactory().retrieveDocumentDefinitions();
		for (DocumentDefinition def : defs) {
			if (def.getName().equals(invoiceDefName)) {
				return def;
			}
		}
		throw new RuntimeException("No document definitiona has been found with the name of 'Invoices'");

	}

	/**
	 * For each of the transaction numbers, find the cash settlement events that relate to the Rentals Interest payment type.
	 * 
	 * @param context Current session context
	 * @param tranNums List of transaction numbers
	 * @return array of event numbers
	 */
	private long[] getRentalsInterestEventNums(Context context, Table tranNums, Set<Integer> distTranNums) {

		ArrayList<Long> events = new ArrayList<>();

		Map<Integer, Set<Long>> eventsInDocuments = getEventsInDocuments(context, distTranNums);
		// Find all the event numbers
		for (TableRow row : tranNums.getRows()) {
			try (Transaction tran = context.getTradingFactory().retrieveTransactionById(row.getInt("tran_num"))) {
				Logging.info("Retrieving cash settlement with payment type 'Rentals Interest' event numbers for transaction " + tran.getTransactionId());
				for (DealEvent event : tran.getDealEvents()) {
					if (event.getField("Event Type").getValueAsString().equals("Cash Settlement") && event.getField("Pymt Type").getValueAsString().startsWith("Rentals Interest")) {
						long eventNum = event.getField("Event Num").getValueAsLong();
						if (eventsInDocuments.containsKey(tran.getTransactionId()) &&
								eventsInDocuments.get(tran.getTransactionId()).contains(eventNum)) {
							// skip event that is already within a document
							Logging.info("Skipping event #" + eventNum + " as it associated to an existing invoice");
						} else {
							Logging.info("Processing event #" + eventNum + " as it is not associated to any invoice yet");
							events.add(eventNum);                    		
						}
					}
				}
			}
		}
		// Convert array list into long array
		long[] eventNums = new long[events.size()];
		for (int i = 0; i < events.size(); i++) {
			eventNums[i] = events.get(i);
		}
		return eventNums;
	}

	/**
	 * Returns the list of events that are in a document mapped per transaction number
	 * (maps trans ids to list of events) 
	 * @param tranNums
	 * @return
	 */
	private Map<Integer, Set<Long>> getEventsInDocuments(Context context, Set<Integer> tranNums) {
		String sql = 
				"\nSELECT h.document_num, d.tran_num, d.event_num"
						+	"\nFROM stldoc_header h "
						+	"\nINNER JOIN stldoc_details d"
						+	"\n  ON d.document_num = h.document_num"
						+	"\n    AND d.doc_version = h.doc_version"
						+	"\n    AND d.tran_num IN (" + toCommaList(tranNums) + ")"
						+	"\n    AND h.doc_type = 1" // 1 = Invoice
						;
		Map<Integer, Set<Long>> map = new HashMap<>();
		if (tranNums.size() == 0) {
			return map;
		}
		try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
			for (int row=sqlResult.getRowCount()-1; row>=0; row--) {
				int tranNum = sqlResult.getInt("tran_num", row);
				long eventNum = sqlResult.getLong("event_num", row);
				Set<Long> events;
				if (map.containsKey(tranNum)) {
					events = map.get(tranNum);
				} else {
					events = new HashSet<>();
					map.put(tranNum, events);
				}
				events.add(eventNum);
			}
		}
		return map;
	}

	private String toCommaList(Set<Integer> tranNums) {
		StringBuilder sb = new StringBuilder();
		boolean start=true;
		for (Integer i : tranNums) {
			if (!start) {
				sb.append(",");
			}
			start = false;
			sb.append(i);
		}
		return sb.toString();
	}

	/**
	 * Determine that the process results has a document number. If it hasn't it may be that the document was never generated.
	 * 
	 * @param context Current session context
	 * @param results Invoice generation results
	 * @return true if document number exists
	 */
	private Set<Integer> getDistinctDocumentIds(Context context, Set<Integer> tranNums) {
		Set<Integer> docIds = new HashSet<>();
		if (tranNums.size() == 0) {
			return docIds;
		}

		String sql = 
				"\nSELECT h.document_num, d.tran_num, d.event_num"
						+	"\nFROM stldoc_header h "
						+	"\nINNER JOIN stldoc_details d"
						+	"\n  ON d.document_num = h.document_num"
						+	"\n    AND d.doc_version = h.doc_version"
						+	"\n    AND d.tran_num IN (" + toCommaList(tranNums) + ")"
						+	"\n    AND h.doc_type = 1" // 1 = Invoice
						+  "\n WHERE h.doc_status=5" // 5 = Generated
						;
		try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
			for (int row=sqlResult.getRowCount()-1; row>=0; row--) {
				int docNum = sqlResult.getInt("document_num", row);
				docIds.add(docNum);
			}
		}
		Logging.info("The following documents are in status 'Generated' and are going to processed further: "
				+ docIds);
		return docIds;

	}
}
