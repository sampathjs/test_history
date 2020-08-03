package com.matthey.openlink.stamping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/**
 * The plugin script for stamping sales ledgers.
 */
@ScriptCategory({EnumScriptCategory.Generic})
public class StampDocuments extends StampLedger {
	private static final Integer STLDOC_STATUS_2_SENT_TO_CP = 7;
	private static final Integer STLDOC_STATUS_2_RECEIVED = 23;

	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.embedded.application.EnumScriptCategory, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, EnumScriptCategory category, ConstTable table) {
		try {
			initialise(context);

			Logging.info(String.format("\n\n********************* Start stamping task: %s ***************************", taskName));
			Session session = context;
			DocumentTrackerDao invoiceTrackingDao =  new DocumentTrackerDao(session);
			String lastUpdate = getDateTimeConfig("lastUpdate");

			/* Add new invoice to document tracking table */
			List<DocumentTracker> newInvoices = invoiceTrackingDao.getNewInvoices(lastUpdate);
			Logging.info(String.format("Total number of new invoices found for stamping : %d",newInvoices == null ? 0 : newInvoices.size()));
			if (newInvoices != null && !newInvoices.isEmpty()) {
				List<DocumentTracker> updatedInvoices = updateNewInvoices(newInvoices);
				invoiceTrackingDao.updateTrackingData(updatedInvoices, true);
				
				String outputDirectory = getStringConfig("csvOutputDir");
				String outputFile = getStringConfig("csvOutputFile");
				Table outputLogs = getOutputLogTable(updatedInvoices);
				writeOutputLog(outputDirectory,outputFile,outputLogs.asCsvString(true));
			}

			/* Update the status for cancelled invoices in document tracking table */
			List<DocumentTracker> cancelledInvoices = invoiceTrackingDao.getCancelledInvoices(lastUpdate);
			Logging.info(String.format("Total number of cancelled invoices found for stamping : %d",cancelledInvoices == null ? 0 : cancelledInvoices.size()));
			if (cancelledInvoices != null && !cancelledInvoices.isEmpty()) {
				List<DocumentTracker> updatedInvoices = updateCancelledInvoices(cancelledInvoices);
				invoiceTrackingDao.updateTrackingData(updatedInvoices, false);
				
				String outputDirectory = getStringConfig("csvOutputDir");
				String outputFile = getStringConfig("csvOutputFile");
				Table outputLogs = getOutputLogTable(updatedInvoices);
				writeOutputLog(outputDirectory,outputFile,outputLogs.asCsvString(true));
			}
			Logging.info(String.format("\n\n********************* End stamping task: %s *****************************", taskName));
		}
		catch(Exception ex) {
			throw new StampingException(String.format("An exception occurred while stamping documents. %s", ex.getMessage()), ex);
		}
		return null;
	}

	/**
	 * Update status of the new invoices.
	 *
	 * @param invoices the invoices
	 * @return the list
	 */
	private List<DocumentTracker> updateNewInvoices (List<DocumentTracker> invoices) {
		List<DocumentTracker> updatedInvoices = new ArrayList<DocumentTracker>();
		for(DocumentTracker invoice: invoices){

			Integer documentNumber = invoice.getDocumentNum();
			Integer docStatus = invoice.getDocStatus();
			Integer docVersion = invoice.getDocVersion();
			Integer lastDocStatus = invoice.getLastDocStatus();
			Integer docHistoryId = invoice.getDocHistoryId();
			Date lastUpdate = invoice.getlastUpdate();
			Integer personnelId = invoice.getPersonnelId();
			String personnelName = invoice.getPersonnelName();
			String coverage = invoice.getCoverage();

			String slStatus =  getDefaultStatus();
			String sapStatus = "";
			/* Set the Sap status only when the Invoice has Sap deals */
			if(coverage.equalsIgnoreCase("Yes")) { sapStatus = getDefaultStatus(); }

			DocumentTracker updatedInvoice = new DocumentTracker.DocumentTrackerBuilder(documentNumber, docStatus, docVersion)
																.LastDocStatus(lastDocStatus)
																.DocHistoryId(docHistoryId)
																.LastUpdate(lastUpdate)
																.PersonnelId(personnelId)
																.PersonnelName(personnelName)
																.SlStatus(slStatus)
																.SapStatus(sapStatus)
																.Coverage(coverage)
																.build();

			updatedInvoices.add(updatedInvoice);
		}
		return updatedInvoices;
	}

	/**
	 * Update status of the cancelled invoices.
	 *
	 * @param invoices the invoices
	 * @return the list
	 */
	private List<DocumentTracker> updateCancelledInvoices (List<DocumentTracker> invoices) {
		List<DocumentTracker> updatedInvoices = new ArrayList<DocumentTracker>();
		for(DocumentTracker invoice: invoices){

			Integer documentNumber = invoice.getDocumentNum();
			Integer docStatus = invoice.getDocStatus();
			Integer docVersion = invoice.getDocVersion();
			Integer lastDocStatus = invoice.getLastDocStatus();
			Integer docHistoryId = invoice.getDocHistoryId();
			Date lastUpdate = invoice.getlastUpdate();
			Integer personnelId = invoice.getPersonnelId();
			String personnelName = invoice.getPersonnelName();
			String sentReceived = invoice.getSentReceived();

			String slStatus =  getSlCancelStatus(invoice.getSlStatus(),sentReceived);
			String sapStatus = getSapCancelStatus(invoice.getSapStatus(),lastDocStatus);

			DocumentTracker updatedInvoice = new DocumentTracker.DocumentTrackerBuilder(documentNumber, docStatus, docVersion)
																.LastDocStatus(lastDocStatus)
																.DocHistoryId(docHistoryId)
																.LastUpdate(lastUpdate)
																.PersonnelId(personnelId)
																.PersonnelName(personnelName)
																.SlStatus(slStatus)
																.SapStatus(sapStatus)
																.SentReceived(sentReceived)
																.build();

			updatedInvoices.add(updatedInvoice);
		}
		return updatedInvoices;
	}

	/**
	 * Gets the Sl cancel status for stamping cancelled invoices. 
	 *
	 * @param currentStatus the current status 
	 * @return the next status after cancellation<br>
	 *         the current status if not valid for cancellation
	 */
	private String getSlCancelStatus(String currentStatus, String sentReceived) {
		String nextStatus = currentStatus;
		LedgerStatus currentLedgerStatus = LedgerStatus.fromString(currentStatus);

		if(null != currentLedgerStatus) {
			/* For same day cancellation, set Sl status to Ignore if document was never "Sent to CP" or "Received" */
			if(currentLedgerStatus.equals(LedgerStatus.PENDING_SENT) && sentReceived.equalsIgnoreCase("No")) {
				nextStatus = LedgerStatus.IGNORE.getValue();
			} 
			else {
				LedgerStatus nextLedgerStatus = CancelLedger.get(currentLedgerStatus);
				if(null != nextLedgerStatus){
					nextStatus = nextLedgerStatus.getValue();
				}
			}
		}
		return nextStatus;
	}

	/**
	 * Gets the Sap cancel status for stamping cancelled invoices. 
	 *
	 * @param currentStatus the current status 
	 * @return the next status after cancellation<br>
	 *         the current status if not valid for cancellation
	 */
	private String getSapCancelStatus(String currentStatus, Integer lastDocStatus) {
		String nextStatus = currentStatus;
		LedgerStatus currentLedgerStatus = LedgerStatus.fromString(currentStatus);

		if(null != currentLedgerStatus) {
			/* For same day cancellation, change Sap status only when the last invoice status was "Sent to CP" or "Received" */
			if(!(currentLedgerStatus.equals(LedgerStatus.PENDING_SENT) && lastDocStatus != STLDOC_STATUS_2_SENT_TO_CP && lastDocStatus != STLDOC_STATUS_2_RECEIVED)) {
				LedgerStatus nextLedgerStatus = CancelLedger.get(currentLedgerStatus);
				if(null != nextLedgerStatus){
					nextStatus = nextLedgerStatus.getValue();
				}
			}
		}
		return nextStatus;
	}

	/**
	 * Gets the default status for stamping new invoices.
	 *
	 * @return the default status
	 */
	private String getDefaultStatus() {
		return LedgerStatus.PENDING_SENT.getValue();
	}

    
	/**
	 * Gets the output log table.
	 *
	 * @return the output log table
	 */
	private Table getOutputLogTable(List<DocumentTracker> invoices){
		Table outputLog = context.getTableFactory().createTable();

		String documentNum = DocumentTrackerTable.DOCUMENT_NUM.getColumnName();
		String slStatus  = DocumentTrackerTable.SL_STATUS.getColumnName();
		String sapStatus  = DocumentTrackerTable.SAP_STATUS.getColumnName();
		String docStatus  = DocumentTrackerTable.DOC_STATUS.getColumnName();
		String lastDocStatus  = DocumentTrackerTable.LAST_DOC_STATUS.getColumnName();
		String docVersion  = DocumentTrackerTable.DOC_VERSION.getColumnName();
		String docHistoryId  = DocumentTrackerTable.STLDOC_HDR_HIST_ID.getColumnName();
		String personnelId  = DocumentTrackerTable.PERSONNEL_ID.getColumnName();
		String lastUpdate  = DocumentTrackerTable.LAST_UPDATE.getColumnName();

		outputLog.addColumn(documentNum, EnumColType.Int);
		outputLog.addColumn(slStatus, EnumColType.String);
		outputLog.addColumn(sapStatus, EnumColType.String);
		outputLog.addColumn(docStatus, EnumColType.Int);
		outputLog.addColumn(lastDocStatus, EnumColType.Int);
		outputLog.addColumn(docVersion, EnumColType.Int);
		outputLog.addColumn(docHistoryId, EnumColType.Int);
		outputLog.addColumn(personnelId, EnumColType.Int);
		outputLog.addColumn(lastUpdate, EnumColType.Date);

		outputLog.addRows(invoices.size());
		int rowId = 0;
		for(DocumentTracker invoice: invoices){
			outputLog.setInt(documentNum, rowId, invoice.getDocumentNum());
			outputLog.setString(slStatus, rowId, invoice.getSlStatus());
			outputLog.setString(sapStatus, rowId, invoice.getSapStatus());
			outputLog.setInt(docStatus, rowId, invoice.getDocStatus());
			outputLog.setInt(lastDocStatus, rowId, invoice.getLastDocStatus());
			outputLog.setInt(docVersion, rowId, invoice.getDocVersion());
			outputLog.setInt(docHistoryId, rowId, invoice.getDocHistoryId());
			outputLog.setInt(personnelId, rowId, invoice.getPersonnelId());
			outputLog.setDate(lastUpdate, rowId, invoice.getlastUpdate());
			rowId++;
		}
		return outputLog;
	}
}
