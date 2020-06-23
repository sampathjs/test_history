/**
 * Status: under construction
 * @author jbonetzky
 * @version 0.1
 */
/*
 * History:
 * 0.1 - initial version
 * 0.2 - fixed 'remapNonSystemDocNums'
 * 0.3 - populate 'Invoice Date' if field exist but empty
 * 0.4 - in case of cancellations: populate 'Invoice Date' w/ current business date if user didn't change
 * 0.5 - removed 'Invoiced Date' (moved logic to Provisional)
 * 0.6 - additional statuses for Prepayment, Provisional, Final for updating settle amount, new doc num per each generation
 * 0.7 - save Our Doc Num also to Prep/Prov Doc Num, save Last Proc Doc Status
 * 0.8 - don't apply the numbering within previewing documents
 * 0.9 - save also non-generated doc num to Prep/Prov Doc Num
 * 0.10 - revised Preview handling
 * 0.11 - revised StlDoc Info Name retrieval
 * 0.12 - populate Xml Data too
 * 0.13 - global xml data
 * 0.14 - sub_type
 * 28.07.16  jwaechter	    - added synchronisation of the document number to USER_consecutive_number
 * 25.03.20  YadavP03  	- memory leaks, remove console prints & formatting changes
 */

package com.openlink.sc.bo.docnums;

import java.util.Arrays;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.STLDOC_GEN_DATA_TYPE;
import com.openlink.util.consecutivenumber.model.ConsecutiveNumberException;
import com.openlink.util.consecutivenumber.persistence.ConsecutiveNumber;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class OLI_GEN_DocNumbering implements IScript {
	private String _xmlData = null;
	public void setXmlData(String xmlData) { _xmlData = xmlData; }
	public String getXmlData() { return _xmlData; }

	protected ConstRepository _constRepo;
	protected static boolean _viewTables = true;

	//private final String STLDOC_INFO_TYPE_PREFIX = "stldoc_info_type_";//col_name
	private final String STLDOC_INFO_TYPE_PREFIX = "olfStlDocTypeInfo";//OLI_MOD_DocInfoFields

	private final String THIS_DOC_NUM = "This Doc Num";
	private String _stldoc_info_this_doc_num = "";//< ConstRep

	private final String LAST_DOC_NUM = "Last Doc Num";
	private String _stldoc_info_last_doc_num = "";//< ConstRep

	private final String PREP_DOC_NUM = "Prep Doc Num";
	private String _stldoc_info_prep_doc_num = "";//< ConstRep

	private final String PROV_DOC_NUM = "Prov Doc Num";
	private String _stldoc_info_prov_doc_num = "";//< ConstRep

	private final String GENERATED_STATUS = "Generated Status";
	private String _generated_status = "Generated";//default
	private List<String> _generated_statuses = null;//< ConstRep

	private final String CANCELLED_STATUS = "Cancelled Status";
	private String _cancelled_status = "Cancelled";//default
	private List<String> _cancelled_statuses = null;//< ConstRep

	private final String PREPARED_STATUS = "Prepared Status";
	private String _prepared_status = "Prepared";//default
	private List<String> _prepared_statuses = null;//< ConstRep

	private final String PREPYMT_INVOICE_STATUS = "Prepayment Status";
	private String _prepymt_invoice_status = "Prepayment";//default
	private List<String> _prepymt_invoice_statuses = null;//< ConstRep

	private final String PROV_INVOICE_STATUS = "Provisional Status";
	private String _prov_invoice_status = "Provisional";//default
	private List<String> _prov_invoice_statuses = null;//< ConstRep

//	private final String FINAL_INVOICE_STATUS = "Final Status";
//	private String _final_invoice_status = "Final";//default
//	private List<String> _final_invoice_statuses = null;//< ConstRep

	private final String SENT_STATUS = "Sent Status";
	private String _sent_status = "Sent";//default
	private List<String> _sent_statuses = null;//< ConstRep

	public void execute(IContainerContext context) throws OException {
		//_constRepo = new ConstRepository("BackOffice", "OLI-DocNumbering");
		//initPluginLog ();

		try {
			retrieveSettingsFromConstRep();
			process(context);
		} catch (Exception e) {
			PluginLog.error("Exception: " + e.getMessage());
			PluginLog.exitWithStatus(); // log failure
		}

	//	PluginLog.exitWithStatus();
		PluginLog.info("done - execute method");
	}

	public void remapNonSystemDocNums(Table tbl) throws OException {
		Table tblDocNums = Util.NULL_TABLE;
		try{

			_stldoc_info_this_doc_num = tryRetrieveSettingFromConstRep(THIS_DOC_NUM, _stldoc_info_this_doc_num);
			if (getStlDocInfoTypeId(_stldoc_info_this_doc_num) < 0){
				throw new OException("StlDoc Info Field '"+_stldoc_info_this_doc_num+"' not found - check setup");
			}

			String col_name_s = tbl.getColName(1),
				   col_name_i = tbl.getColName(2);
			
			tblDocNums = Table.tableNew();
			String sql = "select i.value " +col_name_s + ", i.document_num " + col_name_i
					   + "  from stldoc_info i, stldoc_info_types it"
					   + " where it.type_name like '"+_stldoc_info_this_doc_num+"'"
					   + "   and i.type_id = it.type_id";
			DBaseTable.execISql(tblDocNums, sql);

			tbl.select(tblDocNums, col_name_i, col_name_s+" EQ $"+col_name_s);
			//tblDocNums.destroy();
		
		}finally{
			if(Table.isTableValid(tblDocNums) == 1){
				tblDocNums.destroy();
			}
		}
	}

	protected void applyCustomConditions(Table tbl) throws OException {
		
	}

	private void process(IContainerContext context) throws OException {
		Table tblStldocInfoValues = Util.NULL_TABLE;
		Table tblHelp = Util.NULL_TABLE;
		Table tblDocHist = Util.NULL_TABLE;
		Table tblDocNumbering = Util.NULL_TABLE;
		
		try{			
			Table argt = context.getArgumentsTable();
			Table tblEvent = getEventData(argt);// don't destroy!

			if (_viewTables){
				argt.viewTable();
			}

			boolean isPreview = isPreview(argt);

			String next_doc_status = Table.formatRefInt(tblEvent.getInt("next_doc_status", 1), SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
			int curr_doc_status_id = tblEvent.getInt("curr_doc_status", 1);
			int document_num = (isPreview && curr_doc_status_id == 0) ? getMaximumSystemDocNum()+100 : tblEvent.getInt("document_num", 1);
			int doc_type_id = tblEvent.getInt("doc_type", 1);
			int int_le_id = tblEvent.getInt("internal_lentity", 1);

			if (isPreview){
				PluginLog.info("Document "+(curr_doc_status_id>0?document_num+" ":"")+"is previewed");
			}

			tblStldocInfoValues = retrieveStlDocInfoValues(document_num);

			String this_doc_num,
				   last_doc_num,
				   prep_doc_num,
				   prov_doc_num;
			
			if (isPreview && curr_doc_status_id == 0){
				
				this_doc_num = "";
				last_doc_num = "";
				prep_doc_num = "";
				prov_doc_num = "";
			} else {
				this_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_this_doc_num);
				last_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_last_doc_num);
				prep_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_prep_doc_num);
				prov_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_prov_doc_num);
			}

			PluginLog.info(String.format("Running for document:%d (next_doc_status:%s)", document_num, next_doc_status));
			PluginLog.info(String.format("Retrieved docInfo values - OurDocNum:%s, LastDocNum:%s, PrepDocNum:%s, ProvDocNum:%s for document:%d", this_doc_num, last_doc_num, prep_doc_num, prov_doc_num, document_num));
			
			tblHelp = Table.tableNew();
			DBaseTable.execISql(tblHelp, "select * from USER_bo_doc_numbering where doc_type_id="+doc_type_id+" and our_le_id="+int_le_id);
			
			if (tblHelp.getNumRows() == 0) {
			//	if (isStatusInList(next_doc_status, _generated_statuses) ||
			//		isStatusInList(next_doc_status, _prepared_statuses))
				{
					this_doc_num = ""+document_num;
					if (!isPreview){
						StlDoc.saveInfoValue(document_num, _stldoc_info_this_doc_num, this_doc_num);
					}
				}

				if (isStatusInList(next_doc_status, _prepymt_invoice_statuses)) {
					prep_doc_num = this_doc_num;
					if (!isPreview){
						StlDoc.saveInfoValue(document_num, _stldoc_info_prep_doc_num, prep_doc_num);
					}
				}

				if (isStatusInList(next_doc_status, _prov_invoice_statuses)) {
					prov_doc_num = this_doc_num;
					if (!isPreview){
						StlDoc.saveInfoValue(document_num, _stldoc_info_prov_doc_num, prov_doc_num);
					}
				}

				if (isStatusInList(next_doc_status, _cancelled_statuses)) {
					last_doc_num = this_doc_num;
					if (!isPreview){
						StlDoc.saveInfoValue(document_num, _stldoc_info_last_doc_num, last_doc_num);//still same Endur doc num
					}
				}
			} else {
				String doc_num = null;

				if (tblHelp.getColNum("sub_type")>0) {
					tblHelp.select(tblEvent ,"next_doc_status,buy_sell" ,"doc_type EQ $doc_type_id AND internal_lentity EQ $our_le_id");
					tblHelp.makeTableUnique();
					applyCustomConditions(tblHelp);
				}
				if (tblHelp.getNumRows() > 1) {
					PluginLog.info(String.format("Configuration is ambiguous (i.e. more than one row found in USER_bo_doc_numbering) for doc_type_id=%d & our_le_id=%d", doc_type_id, int_le_id));
					PluginLog.debug(tblHelp, "configuration data");
				} else if (tblHelp.getNumRows() < 1){
					tblHelp.destroy();
					PluginLog.info(String.format("Configuration is empty in USER_bo_doc_numbering for doc_type_id=%d & our_le_id=%d", doc_type_id, int_le_id));
					throw new OException("Configuration is empty");
				}

				// if document status is PREPARED:
				// > save the Endur document number in the OUR Doc Num info
				if (isStatusInList(next_doc_status, _prepared_statuses)){
					{
						this_doc_num = ""+document_num;
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_this_doc_num, this_doc_num);
						}
					}

					if (isStatusInList(next_doc_status, _prepymt_invoice_statuses)) {
						prep_doc_num = this_doc_num;
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_prep_doc_num, prep_doc_num);
						}
					}

					if (isStatusInList(next_doc_status, _prov_invoice_statuses)) {
						prov_doc_num = this_doc_num;
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_prov_doc_num, prov_doc_num);
						}
					}

					/* still same Endur doc num; is done below
					if (isStatusInList(next_doc_status, _cancelled_statuses))
					{
						last_doc_num = this_doc_num;
						if (!isPreview)
							StlDoc.saveInfoValue(document_num, _stldoc_info_last_doc_num, last_doc_num);
					}*/
				}

				// if document status is GENERATED:
				// > analyse the document's history:
				//   - do nothing if it was processed once to GENERATED earlier; otherwise...
				//   - generate new OUR Document Number and 
				//     save it in user_bo_doc_numbering as well as in the OUR Doc Num info
				//   enhanced for altering GENERATED and SENT:
				//   - generate new document number if doc was sent to CP since last generation
				//     save it in user_bo_doc_numbering as well as in the OUR Doc Num info
				if (isStatusInList(next_doc_status, _generated_statuses)) {
					PluginLog.info(String.format("next_doc_status: %s matches with the generated_statuses list", next_doc_status));
					tblDocHist = getDocumentHistory(document_num, isPreview);
					if (_viewTables){
						tblDocHist.viewTable();
					}

					if (_sent_statuses == null || _sent_statuses.isEmpty()) {
						boolean documentWasAlreadyGenerated = false;
						for (String str: _generated_statuses) {
							if (tblDocHist.unsortedFindString("doc_status", str, SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0) {
								documentWasAlreadyGenerated = true;
								break;
							}
						}

						if (!documentWasAlreadyGenerated){
							doc_num = getNextDocNumber(tblHelp, isPreview);
						}
					} else {
						int lastGeneratedVersion = -1, lastSentVersion = -1;
						String docStatus;
						for (int row = tblDocHist.getNumRows(); row > 0; --row) {
							docStatus = tblDocHist.getString("doc_status", row);
							if (_generated_statuses.contains(docStatus)){
								lastGeneratedVersion = max(lastGeneratedVersion,tblDocHist.getInt("doc_version", row));
							}
							if (_sent_statuses.contains(docStatus)){
								lastSentVersion = max(lastSentVersion,tblDocHist.getInt("doc_version", row));
							}
						}
						if(Table.isTableValid(tblDocHist) == 1){
							tblDocHist.destroy();
							tblDocHist = null;
						}
						PluginLog.info(String.format("Max LastGeneratedVersion: %d, Max LastSentVersion: %d for document: %d", lastGeneratedVersion, lastSentVersion, document_num));
						if (lastGeneratedVersion <= 0){
							// document was never GENERATED
							doc_num = getNextDocNumber(tblHelp, isPreview);
							PluginLog.info(String.format("Fetched doc info field doc_num value: %s for document: %d", doc_num, document_num));
						} else {
							// document was SENT at least once
							if (lastGeneratedVersion < lastSentVersion) {
								PluginLog.info(String.format("lastGeneratedVersion(%d) < lastSentVersion(%d) - document was never GENERATED since last SENT for document: %d", lastGeneratedVersion, lastSentVersion, document_num));
								// document was never GENERATED since last SENT
								// > we copy the current number to the last number
								last_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_this_doc_num);
								if (!isPreview){
									StlDoc.saveInfoValue(document_num, _stldoc_info_last_doc_num, last_doc_num);
								}

								// > we need a new document number
								doc_num = getNextDocNumber(tblHelp, isPreview);
								PluginLog.info(String.format("Fetched doc info field our_doc_num value: %s for document: %d", doc_num, document_num));
							} else {
								// document was already GENERATED since last SENT > we don't need a new document number
								PluginLog.info(String.format("lastGeneratedVersion(%d) >= lastSentVersion(%d) - document was already GENERATED since last SENT for document: %d", lastGeneratedVersion, lastSentVersion, document_num));
								
								String our_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_this_doc_num);
								if (our_doc_num == null || our_doc_num.trim().isEmpty()) {
									PluginLog.info(String.format("OurDocNum field value found empty for document(generating new value): %d", document_num));
									doc_num = getNextDocNumber(tblHelp, isPreview);
									PluginLog.info(String.format("Fetched doc info field our_doc_num new value: %s for document: %d", doc_num, document_num));
								}
							}
						}
					}

				}

				// if document status is CANCELLED:
				// > analyse the document's history:
				//   - do nothing if it was never processed to SENT earlier; otherwise...
				//   - copy OUR Doc Num to Additional Document Reference (way of backup) and
				//     generate new OUR Document Number and 
				//     save it in user_bo_doc_numbering as well as in the OUR Doc Num info
				if (isStatusInList(next_doc_status, _cancelled_statuses)) {
					PluginLog.info(String.format("next_doc_status: %s matches with the cancelled_statuses list", next_doc_status));
					tblDocHist = getDocumentHistory(document_num, isPreview);
					if (_viewTables){
						tblDocHist.viewTable();
					}

					boolean documentWasReallySent = false;
					if (_sent_statuses != null){
						for (String str: _sent_statuses) {
							if (tblDocHist.unsortedFindString("doc_status", str, SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0) {
								documentWasReallySent = true;
								break;
							}
						}
					}

					if (documentWasReallySent) {
						// > we copy the current number to the last number
						last_doc_num = getStlDocInfoValue(tblStldocInfoValues, _stldoc_info_this_doc_num);
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_last_doc_num, last_doc_num);
						}
						// > we need a new document number
						doc_num = getNextDocNumber(tblHelp, isPreview);
						PluginLog.info(String.format("Fetched doc info field doc_num value: %s for document: %d", doc_num, document_num));
					}

					
				}

				//--------------------------

				if (doc_num != null) {
					if (isPreview){
						PluginLog.info("In Preview:Next value "+(curr_doc_status_id>0?" for doc "+document_num:"")+" is '" + doc_num + "' (simulated)");
					} else{ 
						PluginLog.info("Next value for doc "+document_num+" is '" + doc_num + "'");
					}
					
					{
						this_doc_num = doc_num;
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_this_doc_num, this_doc_num);
						}
					}

					if (isStatusInList(next_doc_status, _prepymt_invoice_statuses)) {
						prep_doc_num = this_doc_num;
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_prep_doc_num, prep_doc_num);
						}
					}

					if (isStatusInList(next_doc_status, _prov_invoice_statuses)) {
						prov_doc_num = this_doc_num;
						if (!isPreview){
							StlDoc.saveInfoValue(document_num, _stldoc_info_prov_doc_num, prov_doc_num);
						}
					}

					if (!isPreview) {
						// update user table
						PluginLog.info(String.format("Updating USER_bo_doc_numbering after fetching doc num value: %s for document: %d", doc_num, document_num));
						tblDocNumbering = Table.tableNew("USER_bo_doc_numbering");
						DBUserTable.structure(tblDocNumbering);
						tblHelp.copyRowAddAllByColName(tblDocNumbering);

						tblDocNumbering.setString("last_number", 1, ""+doc_num);
						tblDocNumbering.setString("reset_number_to", 1, "");

						if (tblHelp.getColNum("sub_type") > 0){
							tblDocNumbering.group("doc_type_id, our_le_id, sub_type");
						}  else {
							tblDocNumbering.group("doc_type_id, our_le_id");
						}
						DBUserTable.update(tblDocNumbering);
						
						ConsecutiveNumber cn;
						try {
							cn = new ConsecutiveNumber("OLI_DocNumbering");
						} catch (ConsecutiveNumberException e) { 
							throw new OException(e.getMessage()); 
						}
						
						String item = ""+tblDocNumbering.getInt("our_le_id", 1) + "_"+tblDocNumbering.getInt("doc_type_id", 1);
						
						if (tblDocNumbering.getColNum("sub_type") > 0){
							item += "_"+tblDocNumbering.getInt("sub_type", 1);
						}
						
						try {
							cn.resetItem(item, Long.parseLong(doc_num)+1);
						} catch (ConsecutiveNumberException e) {
							throw new OException(e.getMessage()); 
						}

						PluginLog.info(String.format("Table USER_bo_doc_numbering updated with value: %d", Long.parseLong(doc_num)+1));
					}
				}
			}

			// write values also in output fields
			setOutputFields(argt, document_num, this_doc_num, last_doc_num, prep_doc_num, prov_doc_num);

			if (_viewTables){
				argt.viewTable();
			}
		
		}finally{
			if(Table.isTableValid(tblHelp) == 1){
				tblHelp.destroy();
			}
			if(Table.isTableValid(tblStldocInfoValues) == 1){
				tblStldocInfoValues.destroy();
			}
			if(Table.isTableValid(tblDocHist) == 1){
				tblDocHist.destroy();
			}
			if(Table.isTableValid(tblDocNumbering) == 1){
				tblDocNumbering.destroy();
			}
		}
	}

	protected final boolean isPreview(Table argt) throws OException {
		
		int row_num = argt.unsortedFindString("col_name", "View/Process", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		return (row_num > 0) ? ("View").equalsIgnoreCase(argt.getString("col_data", row_num)) : false;
	}

	private boolean isStatusInList(String status, List<String> statuses) {
		
		if (statuses != null && !statuses.isEmpty()){
			return statuses.contains(status);
		}
		return false;
	}

	private String peekNextDocNumber(Table tblDocNumberingData) throws OException {
		
		long doc_num = 1L; // initial 'next' number
		String reset_number_to = tblDocNumberingData.getString("reset_number_to", 1).trim();
		String last_number = tblDocNumberingData.getString("last_number", 1).trim();

		if (reset_number_to.length() > 0) {
			
			try {
				doc_num = Long.parseLong(reset_number_to);
			} catch (NumberFormatException e){ 
				throw new OException("NumberFormatException: " + reset_number_to); 
			}
		} else if (last_number.length() > 0) {
			try {
				doc_num = Long.parseLong(last_number) + 1;
			} catch (NumberFormatException e) { 
				throw new OException("NumberFormatException: " + last_number); 
			}
			
		}

		return doc_num >= 0 ? (""+doc_num) : null;
	}

	protected final String getNextDocNumber(Table tblDocNumberingData, boolean isPreview) throws OException {
		PluginLog.info("Inside getNextDocNumber method to generate new OurDocNum field value...");
		if (isPreview){
			return peekNextDocNumber(tblDocNumberingData);
		}

		long doc_num = -1L;
		ConsecutiveNumber cn;
		try {
			cn = new ConsecutiveNumber("OLI_DocNumbering");
		} catch (ConsecutiveNumberException e) { 
			PluginLog.error(e.getMessage());
			throw new OException(e.getMessage()); 
		}
		
		String item = ""+tblDocNumberingData.getInt("our_le_id", 1) + "_"+tblDocNumberingData.getInt("doc_type_id", 1);
		if (tblDocNumberingData.getColNum("sub_type") > 0){
			item += "_"+tblDocNumberingData.getInt("sub_type", 1);
		}

		String reset_number_to = tblDocNumberingData.getString("reset_number_to", 1).trim();
		if (reset_number_to.length() > 0) {
			try {
				doc_num = Long.parseLong(reset_number_to);
				cn.resetItem(item, doc_num+1);
			} catch (NumberFormatException e) {
				PluginLog.error(e.getMessage());
				throw new OException("NumberFormatException: " + reset_number_to);
			} catch (ConsecutiveNumberException e) {
				PluginLog.error(e.getMessage());
				throw new OException(e.getMessage()); 
			}
		}

		try {
			doc_num = cn.next(item);
		} catch (ConsecutiveNumberException e) {
			PluginLog.error(e.getMessage());
			throw new OException(e.getMessage()); 
		}

		PluginLog.info(String.format("Fetched new OurDocNum field value: %d for item:%s", doc_num, item));
		PluginLog.info("Exiting getNextDocNumber method to generate new OurDocNum field value..");
		return doc_num >= 0 ? (""+doc_num) : null;
	}

	// returns a non-freeable table; either the event table or a null table
	private Table getEventData(Table argt) throws OException {
		int row_num = argt.unsortedFindString("col_name", "*SourceEventData", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		return (row_num > 0) ? argt.getTable("doc_table", row_num) : Util.NULL_TABLE;
	}

	private void retrieveSettingsFromConstRep() throws OException {
		
		_generated_statuses = Arrays.asList( tryRetrieveSettingFromConstRep(GENERATED_STATUS, _generated_status).trim().replaceAll("\\s*,\\s*", ",").split(","));

		_cancelled_statuses = Arrays.asList( tryRetrieveSettingFromConstRep(CANCELLED_STATUS, _cancelled_status).trim().replaceAll("\\s*,\\s*", ",").split(","));

		_prepared_statuses = Arrays.asList( tryRetrieveSettingFromConstRep(PREPARED_STATUS, _prepared_status).trim().replaceAll("\\s*,\\s*", ",").split(","));

		_prepymt_invoice_statuses = Arrays.asList( tryRetrieveSettingFromConstRep(PREPYMT_INVOICE_STATUS, _prepymt_invoice_status).trim().replaceAll("\\s*,\\s*", ",").split(","));

		_prov_invoice_statuses = Arrays.asList( tryRetrieveSettingFromConstRep(PROV_INVOICE_STATUS, _prov_invoice_status).trim().replaceAll("\\s*,\\s*", ",").split(","));

//		_final_invoice_statuses = Arrays.asList(
//				tryRetrieveSettingFromConstRep(FINAL_INVOICE_STATUS, _final_invoice_status)
//				.trim().replaceAll("\\s*,\\s*", ",").split(","));

		_sent_statuses = Arrays.asList( tryRetrieveSettingFromConstRep(SENT_STATUS, _sent_status).trim().replaceAll("\\s*,\\s*", ",").split(","));
/*
		_stldoc_info_this_doc_num = tryRetrieveSettingFromConstRep(THIS_DOC_NUM, _stldoc_info_this_doc_num);
		_stldoc_info_last_doc_num = tryRetrieveSettingFromConstRep(LAST_DOC_NUM, _stldoc_info_last_doc_num);
		_stldoc_info_prep_doc_num = tryRetrieveSettingFromConstRep(PREP_DOC_NUM, _stldoc_info_prep_doc_num);
		_stldoc_info_prov_doc_num = tryRetrieveSettingFromConstRep(PROV_DOC_NUM, _stldoc_info_prov_doc_num);

		if (getStlDocInfoTypeId(_stldoc_info_prep_doc_num) < 0)
			PluginLog.warn("StlDoc Info Field '"+_stldoc_info_prep_doc_num+"' not found");
		if (getStlDocInfoTypeId(_stldoc_info_prov_doc_num) < 0)
			PluginLog.warn("StlDoc Info Field '"+_stldoc_info_prov_doc_num+"' not found");

		if (getStlDocInfoTypeId(_stldoc_info_last_doc_num) < 0)
			if (getStlDocInfoTypeId(_stldoc_info_this_doc_num) < 0)
				throw new OException("StlDoc Info Fields '"+_stldoc_info_this_doc_num+"' and '"+_stldoc_info_last_doc_num+"' not found - check setup");
			else
				throw new OException("StlDoc Info Field '"+_stldoc_info_last_doc_num+"' not found - check setup");
		else if (getStlDocInfoTypeId(_stldoc_info_this_doc_num) < 0)
			throw new OException("StlDoc Info Field '"+_stldoc_info_this_doc_num+"' not found - check setup");
*/
		String name; int id;

		id = getStlDocInfoTypeId(name = tryRetrieveSettingFromConstRep(THIS_DOC_NUM, _stldoc_info_this_doc_num));
		if (id >= 0) { 
			_stldoc_info_this_doc_num = name; /*_stldoc_info_this_doc_num_id = id;*/ 
		} else if (name.trim().length()>0) {
			throw new OException("StlDoc Info Field for feature '"+THIS_DOC_NUM+"' not found - invalid name: '"+name+"'");
		}

		id = getStlDocInfoTypeId(name = tryRetrieveSettingFromConstRep(LAST_DOC_NUM, _stldoc_info_last_doc_num));
		if (id >= 0) { 
			_stldoc_info_last_doc_num = name; /*_stldoc_info_last_doc_num_id = id;*/ 
		} else if (name.trim().length()>0) {
			throw new OException("StlDoc Info Field for feature '"+LAST_DOC_NUM+"' not found - invalid name: '"+name+"'");
		}

		id = getStlDocInfoTypeId(name = tryRetrieveSettingFromConstRep(PREP_DOC_NUM, _stldoc_info_prep_doc_num));
		if (id >= 0) { 
			_stldoc_info_prep_doc_num = name; /*_stldoc_info_prep_doc_num_id = id;*/ 
		} else if (name.trim().length()>0) {
			PluginLog.debug("StlDoc Info Field for feature '"+PREP_DOC_NUM+"' not found - invalid name: '"+name+"'");
		}

		id = getStlDocInfoTypeId(name = tryRetrieveSettingFromConstRep(PROV_DOC_NUM, _stldoc_info_prov_doc_num));
		if (id >= 0) { 
			_stldoc_info_prov_doc_num = name; /*_stldoc_info_prov_doc_num_id = id;*/ 
		} else if (name.trim().length()>0) {
			PluginLog.debug("StlDoc Info Field for feature '"+PROV_DOC_NUM+"' not found - invalid name: '"+name+"'");
		}
	}

	private String tryRetrieveSettingFromConstRep(String variable_name, String default_value) {
		
		try { 
			default_value = _constRepo.getStringValue(variable_name, default_value); 
		} catch (Exception e) { 
			PluginLog.warn("Couldn't solve setting for: " + variable_name + " - " + e.getMessage()); 
		} finally{ 
			PluginLog.debug(variable_name + ": " + default_value); 
		}
		return default_value;
	}

	/**
	 * 
	 * @param document_num A document number to retrieve the history for
	 * @return Table containing the cols doc_version, doc_status, doc_status_id, stldoc_hdr_hist_id with doc_version descending ordered
	 * @throws OException
	 */
	private Table getDocumentHistory(int document_num, boolean isPreview) throws OException {
		
		Table tbl = Table.tableNew("history for doc# "+ document_num);
		String sql = "SELECT doc_version, doc_status, doc_status doc_status_id, stldoc_hdr_hist_id"
				   + " FROM stldoc_header_hist WHERE document_num="+document_num
				   + " ORDER BY 1 desc";
		PluginLog.debug(String.format("Executing SQL query(getDocumentHistory): %s", sql));
		int ret = DBaseTable.execISql(tbl, sql);
		tbl.setColFormatAsRef("doc_status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
		tbl.convertColToString(2);
		if (!isPreview) {
			tbl.delRow(1);// the current run
		}
		return tbl;
	}
	private final static int max(int a, int b) { 
		return a > b ? a : b; 
	}

	private int getStlDocInfoTypeId(String name) throws OException {
		Table tbl = Util.NULL_TABLE;
		try{
			tbl = Table.tableNew();
			String sql = "SELECT type_id FROM stldoc_info_types WHERE type_name='" + name + "'";
			int ret = DBaseTable.execISql(tbl, sql);
			ret = (ret == 1 && tbl.getNumRows() == 1) ? tbl.getInt(1, 1) : -1;
			return ret;
		
		}finally{
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

	private Table retrieveStlDocInfoValues(int document_num) throws OException {
		
		Table tbl = Table.tableNew();
		String sql = "SELECT i.value, it.type_id, it.type_name"
				   + "  FROM stldoc_info i, stldoc_info_types it"
				   + " WHERE i.type_id = it.type_id"
				   + "   AND i.document_num = "+document_num;
		
		PluginLog.debug(String.format("Executing SQL query (retrieveStlDocInfoValues)- %s", sql));
		DBaseTable.execISql(tbl, sql);
		return tbl;
	}

	private String getStlDocInfoValue(Table tblValues, String name) throws OException {
		
		int row = tblValues.unsortedFindString("type_name", name, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		return row > 0 ? tblValues.getString("value", row).trim() : "";
	}

	private int getMaximumSystemDocNum() throws OException {
		
		Table tbl = Table.tableNew();
		String sql = "SELECT MAX(document_num) max_document_num FROM stldoc_header";
		try {
			DBaseTable.execISql(tbl, sql);
			return tbl.getNumRows()>0 ? tbl.getInt(1, 1) : 0;
		} finally { 
			if(Table.isTableValid(tbl)==1)
			tbl.destroy(); 
		}
	}

	private void setOutputFields(Table tblOutput, int document_num, String this_doc_num, String last_doc_num, String prep_doc_num, String prov_doc_num) throws OException {
		
		Table tbl = Util.NULL_TABLE;
		Table tblData = Util.NULL_TABLE;
		
		try{

			PluginLog.info(String.format("Updating xml data with doc info fields(OurDocNum: %s) for document: %d", this_doc_num, document_num));
			int this_doc_num_id = getStlDocInfoTypeId(_stldoc_info_this_doc_num),
				last_doc_num_id = getStlDocInfoTypeId(_stldoc_info_last_doc_num),
				prep_doc_num_id = getStlDocInfoTypeId(_stldoc_info_prep_doc_num),
				prov_doc_num_id = getStlDocInfoTypeId(_stldoc_info_prov_doc_num);

			tbl = Table.tableNew("NameMap with Values");
			String sql = 
				"SELECT DISTINCT a.internal_field_name, a.output_field_name col_name, 1 col_type, '' col_data FROM stldoc_tmpl_stdgen_items a, " +
				"(SELECT stldoc_template_id, MAX(stldoc_template_version) stldoc_template_version FROM stldoc_tmpl_stdgen_items GROUP BY stldoc_template_id) h " +
				"WHERE a.stldoc_template_id=h.stldoc_template_id AND a.stldoc_template_version=h.stldoc_template_version " +
				"AND internal_field_name IN ('"+STLDOC_INFO_TYPE_PREFIX+this_doc_num_id+"','"+STLDOC_INFO_TYPE_PREFIX+last_doc_num_id+
				"','"+STLDOC_INFO_TYPE_PREFIX+prep_doc_num_id+"','"+STLDOC_INFO_TYPE_PREFIX+prov_doc_num_id+"')";
			
			PluginLog.debug(String.format("Executing SQL query(in setOutputFields): %s", sql));
			int ret = DBaseTable.execISql(tbl, sql);
			
			tblData = Table.tableNew();
			tblData.addCols("S(internal_field_name)S(col_data)");
			tblData.addRowsWithValues("("+STLDOC_INFO_TYPE_PREFIX+this_doc_num_id+"),("+this_doc_num+")");
			tblData.addRowsWithValues("("+STLDOC_INFO_TYPE_PREFIX+last_doc_num_id+"),("+last_doc_num+")");
			tblData.addRowsWithValues("("+STLDOC_INFO_TYPE_PREFIX+prep_doc_num_id+"),("+prep_doc_num+")");
			tblData.addRowsWithValues("("+STLDOC_INFO_TYPE_PREFIX+prov_doc_num_id+"),("+prov_doc_num+")");
			tbl.select(tblData, "col_data", "internal_field_name EQ $internal_field_name");
			
			if (_viewTables) {
				tbl.viewTable();
			}

			tblOutput.insertCol("_row", 1, COL_TYPE_ENUM.COL_INT);
			tblOutput.setColIncrementInt(1, 1, 1);
			tblOutput.select(tbl, "col_data", "col_type EQ $col_type AND col_name EQ $col_name");
			tblOutput.group("_row");
			tblOutput.delCol(1);

			if (tbl.getNumRows() > 0){
				try {
					STLDOC_GEN_DATA_TYPE dataType = StlDoc.getGenDataType();
					if (dataType == STLDOC_GEN_DATA_TYPE.STLDOC_GEN_DATA_TYPE_XML || dataType == STLDOC_GEN_DATA_TYPE.STLDOC_GEN_DATA_TYPE_ALL) {
						String xmlData = _xmlData == null ? StlDoc.getXmlData() : _xmlData;
						_xmlData = setOutputFields(xmlData, tbl);
						StlDoc.setReturnXmlData(_xmlData);
					}
				} catch (Exception e) { 
					PluginLog.error("Failed to apply to Xml Data: "+e.toString()); 
				}
			}
			
			PluginLog.info(String.format("Updated xml data with doc info fields(OurDocNum: %s) for document: %d", this_doc_num, document_num));
			
		
		}finally{
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();	
			}
			if(Table.isTableValid(tblData) == 1){
				tblData.destroy();	
			}
			
			
		}
	}

	private String setOutputFields(String xml, Table data) throws OException {
		
		StringBuilder builder = new StringBuilder(xml);

		String field, value;
		for (int row = data.getNumRows(), posValueStart, posClosingTag, lengthBefore; row > 0; --row) {
			
			field = data.getString("col_name", row);
			value = data.getString("col_data", row);

			posValueStart = -1;
			
			while ((posValueStart=builder.indexOf("<"+field+" ", posValueStart))>=0) {
				lengthBefore = builder.length();
				if (builder.indexOf("/", posValueStart)<builder.indexOf(">", posValueStart)) {
					// value is empty
					posValueStart = builder.indexOf("/", posValueStart);
					builder.replace(posValueStart, posValueStart+1, ">"+value+"</"+field);
				} else {
					posValueStart = builder.indexOf(">", posValueStart) + 1;
					posClosingTag = builder.indexOf("<", posValueStart);
					builder.replace(posValueStart, posClosingTag, value);
				}
				posValueStart += builder.length()-lengthBefore;
			}
		}

		return builder.toString();
	}

	protected void initPluginLog() {
		String logLevel = "DEBUG", 
			   logFile  = getClass().getSimpleName() + ".log", 
			   logDir   = null;

		try {
			logDir   = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			if (logDir == null){
				PluginLog.init(logLevel);
			} else{ 
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			// do something
		}

		try {
			_viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		} catch (Exception e) {
			// do something
		}
	}
}
