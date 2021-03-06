// !!! see also the twin called JM_OUT_DocOutput differing only in super class !!!
package com.openlink.jm.bo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.jm.sc.bo.util.BOInvoiceUtil;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.JvsExitException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.FILE_OBJECT_LINK_TYPE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.jm.bo.docoutput.UpdateErrorInUserTable;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2016-MM-DD	V1.0	-	pwallace	- Initial version
 * 2016-02-25	V1.1	-	jwaechter	- added linking of documents to deals after processing
 * 2016-02-26	V1.2	-	jwaechter	- now retrieving tran nums from stldoc_details for processed 
 *                                        document instead from framework provided memory table
 * 2016-08-11   V1.3    -   scurran     - add validation around email addresses in the distribution list  
 * 2016-10-26   V1.4    -   scurran     - add additional logging around the output parameters to help with debugging EPI-208                                       
 * 2016-10-28	V1.5	-   jwaechter	- method loadDealsForDocument now looks at stldoc_details_hist
 *                                        instead of stldoc_details to ensure cancelled documents
 *                                        are processed correctly   
 * 2017-05-24	V1.6	-	jwaechter	- Added logic for double confirmations.    
 * 2017-11-08   V1.7    -   lma         - Added two checks more before generate documents                                    
 * 2019-03-13   V1.8    -   jneufert    - Add status '2 Received' as possible prior status for Cancellations
 * 2020-01-10	V1.9	-	Pramod Garg - Insert the erroneous entry in USER_jm_auto_doc_email_errors table 
 * 										   if failed to make connection to mail server
 * 2020-01-31	V1.9    -   Agrawa01    - Check for missing Cancellation Document info fields
 * 2020-01-31	V2.0	-	YadavP03	- Added method to set Document Info field when the script succeeds and fails^
 * 2020-03-25   V2.1        YadavP03  	- memory leaks, remove console prints & formatting changes
 * 2020-06-05   V2.2	-   jwaechter	- modified control logic as base class is not longer throwing an exception on success
 **/

/**
 * Custom version of BO_DocOutput_wMail for JM. 
 * @author pwallace
 * @version 2.2
 */
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_OUT_DocOutput_wMail extends com.openlink.jm.bo.docoutput.BO_DocOutput_wMail {
	
	private final String DOC_STATUS_CANCELLED = "Cancelled";
	private final String DOC_STATUS_SENDING_FAILED = "Sending Failed";
	private final String DOC_STATUS_CANCELLATION_FAILED = "Cancellation Failed";

	public void execute(IContainerContext context) throws OException 	{
		ConstRepository constRepo= new ConstRepository("BackOffice", "JM_OUT_DocOutput_wMail");

		String
		logLevel = constRepo.getStringValue("logLevel", "Error"),
		logFile  = constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log"),
		logDir   = constRepo.getStringValue("logDir", null),
		outputFormConfirmCopy = constRepo.getStringValue("outputFormConfirmCopy", "JM-Confirm-Copy"),
		outputFormConfirmAcksCopy = constRepo.getStringValue("outputFormConfirmCopy", "JM_Confirm_Copy_Acks");

		try {
			Logging.init( this.getClass(), "BackOffice", "JM_OUT_DocOutput_wMail");
		} catch (Exception e) {
			OConsole.oprint("Unable to initialise logger");
		}
		
		resetRegenrateDocInfo(context.getArgumentsTable().getTable("process_data", 1), EnumRegenrateOutput.YES);
		Table argt = context.getArgumentsTable();

		Table tblProcessData = argt.getTable("process_data", 1);
		int docTypeId = tblProcessData.getInt("doc_type", 1);
		String docType = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, docTypeId); // doc_type column of table stldoc_document_type

		// Terminate in case of the Confirmation Copy Output form is being processed but the necessary
		// field containing  the email addresses is empty.
		int outputFormId = tblProcessData.getInt("output_form_id", 1);
		String outputForm = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_OUTPUT_FORMS_TABLE, outputFormId);

		if (outputForm.equals(outputFormConfirmCopy) || outputForm.equals(outputFormConfirmAcksCopy)) {
			
			/* 2017-11-08   V1.7 Added two checks more before generate documents     
			 * If the output form is equal to JM-Confirm-CopyORJM-Confirm-Copy-Acks
			 * then
			 * 1. iffield olfExtJMConfirmCopyBUShortName = None or olfExtBUShortName =olfExtJMConfirmCopyBUShortName 
			 * 	then EXITwithout creating an output and without failing (i.e. status should not set to Sending failed)
			 *  If olfExtBUShortName = olfExtJMConfirmCopyBUShortName
			 * 	then EXITwithout creating an output and without failing (i.e. status should not set to Sending failed)
			 * 2. else ifoutput form = JM-Confirm-Copy-AcksANDmove_to_status <> Fixed and Sent(=doc_status_id = 19)
			 * 	then EXIT without creating an output and without failing (i.e. status should not set to Sending failed)
			 * else GenerateCopy Confirmas implemented
			 */
			Table userData = tblProcessData.getTable("user_data", 1);
	        int findRow = userData.unsortedFindString("col_name", "olfExtBUShortName", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
	        int findRow1 = userData.unsortedFindString("col_name", "olfExtJMConfirmCopyBUShortName", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
	    
	        if(findRow > 0 && findRow <= userData.getNumRows() && findRow1 > 0 && findRow1 <= userData.getNumRows()) {
	            String olfExtBUShortName  = userData.getString("col_data", findRow);
	            String olfExtJMConfirmCopyBUShortName = userData.getString("col_data", findRow1);
	            if("None".equalsIgnoreCase(olfExtJMConfirmCopyBUShortName)) {
	            	resetRegenrateDocInfo(tblProcessData, EnumRegenrateOutput.NO);
	            	return;
	            }
	            if(olfExtBUShortName.equalsIgnoreCase(olfExtJMConfirmCopyBUShortName)) {
	            	resetRegenrateDocInfo(tblProcessData, EnumRegenrateOutput.NO);
	            	return;
	            }
	        } 

	        String moveToStatus = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, tblProcessData.getInt("next_doc_status", 1));
            Logging.debug("\nMove To Status: " + moveToStatus);
	        if (outputForm.equals(outputFormConfirmAcksCopy) && !"3 Fixed and Sent".equalsIgnoreCase(moveToStatus)){
	            resetRegenrateDocInfo(tblProcessData, EnumRegenrateOutput.NO);
	        	return;
	        }
	        	        
			if(validateEmailData(tblProcessData)) {
				String errorMessage = "Invalid email address detected";
				Logging.error(errorMessage );
				argt.setString("output_filename", 1, "");
				Table processData = argt.getTable("process_data", 1);
				Table outputData = processData.getTable ("output_data", 1);
				
				int row = outputData.findInt("outparam_seqnum", 4, SEARCH_ENUM.FIRST_IN_GROUP); // path
				if (row > 0) {
					outputData.setString("outparam_value", row, "");
				}
				 row = outputData.findInt("outparam_seqnum", 5, SEARCH_ENUM.FIRST_IN_GROUP); // filename
				if (row > 0) {
					outputData.setString("outparam_value", row, "");
				}
				return;
		 	}						
		}

		
		if ("Invoice".equals(docType) && DOC_STATUS_CANCELLED.equals(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, tblProcessData.getInt("doc_status", 1)))){

			String xmlData = tblProcessData.getTable("xml_data", 1).getTable("XmlData",1).getString("XmlData", 1);
			Table tblUserData = tblProcessData.getTable("user_data", 1);

			int userDataNumcols = tblUserData.getNumCols();
			for (int i = 0; ++i <=userDataNumcols;){
				argt.insertCol(tblUserData.getColName(i), i, COL_TYPE_ENUM.fromInt(tblUserData.getColType(i)));
			}
			tblUserData.copyRowAddAllByColName(argt);

			int row = argt.unsortedFindString("col_name", "*SourceEventData", SEARCH_CASE_ENUM.CASE_SENSITIVE);
			Table tblEventData = argt.getTable("doc_table", row);
			tblEventData.select(tblProcessData, "doc_status(next_doc_status),last_doc_status(curr_doc_status)", "document_num EQ $document_num");

			JM_GEN_DocNumbering dn = new JM_GEN_DocNumbering();
			dn.setXmlData(xmlData);
			dn.execute(context);
			xmlData = dn.getXmlData();
			
			argt.deleteWhereValue("user_id", 0);
			for (int i = 0; ++i <=userDataNumcols;){
				argt.delCol(1);
			}

			tblProcessData.getTable("xml_data", 1).getTable("XmlData",1).setString("XmlData", 1, xmlData);
			
			//Check for missing Cancellation Document info fields & throw OException with a proper message
			checkForMissingCancelDocInfoFields(tblProcessData, tblUserData);
		}
		
		int docStatusId = tblProcessData.getInt("doc_status", 1);
		int docNum = tblProcessData.getInt("document_num", 1);
		//Check previous transitions of the input document to 'Sent to CP' status in STLDOC_header_hist table to stop generation of Invoice documents
		//for specific scenarios like No previous transition in 'Sent to CP' status & current transition is from '1 Generated' to 'Cancelled'.
		//This will stop - i) generation of PDF, ii) Sending email to client & iii) linking document to deal
		if ("Invoice".equals(docType) && 4 == docStatusId && !checkAnyPrevTransitionToSentToCP(docNum)) {
           	resetRegenrateDocInfo(tblProcessData, EnumRegenrateOutput.NO);
			return;
		}
		
		int previewFlag = argt.getInt("preview_flag", 1);		
		if (previewFlag != 1) {
			
			Logging.debug( tblProcessData.getTable("output_data", 1).exportCSVString() + " Output Params Direct");
			debugLogTable(tblProcessData.getTable("output_data", 1));
			// Validate Email Addresses 
			if(validateEmailData(tblProcessData)) {
				String errorMessage = "Invalid email address detected";
				Logging.error(errorMessage );
			 
				int documentNumber = tblProcessData.getInt("document_num", 1);
				int documentStatus = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, DOC_STATUS_SENDING_FAILED);
				// Move the document to a failed status
				StlDoc.processDocToStatus(documentNumber, documentStatus);
			 
				throw new OException(errorMessage);
				// Util.exitFail(errorMessage);
		 	}
		}
		Table deals = Util.NULL_TABLE;
		try {
		    // due to removal of PluginLog.exitWithStatus() in the base class, there is no longer an exception
			// thrown in all circumstances. For that reason the logic to link the document 
			// (previous succeed case in the catch statement) has been moved up after the
			// call to super.execute
			super.execute(context);	
			linkDealToTransaction(context);
        	resetRegenrateDocInfo(tblProcessData, EnumRegenrateOutput.NO);
		} catch (OException ex) {
			Logging.info(String.format("SC EXIT: %s\n%s\n\t \nCAUSE:%s\n",ex.getMessage(), ex.getLocalizedMessage(), ex.getCause()==null ? "":ex.getCause().getLocalizedMessage()));
		 			
			// This code is probably redundant now due the validation of email addresses higher up. Due to 
			// time / testing constraints it's being left in place for the time being. 
			Table params = tblProcessData.getTable("output_data", 1);
		
			for (int row=params.getNumRows(); row>0; row--) {
				String parameter = params.getString("outparam_name", row);
				if (parameter.contains("Mail Recipients") && params.getString("outparam_value", row).startsWith("change this to either your email")) {
					
					deals = loadDealsForDocument(tblProcessData.getInt("document_num", 1));
					StringBuilder dealNumbers = new StringBuilder();
					for (int dealRow=deals.getNumRows(); dealRow >0; dealRow--) {
						int dealNum = deals.getInt("deal_tracking_num", dealRow);
						dealNumbers.append(dealNum).append(",");
					}
					if (dealNumbers.length()>1){
						dealNumbers.deleteCharAt(dealNumbers.length()-1);
					}
					Logging.info(String.format("Unable to process Document %d Deals:%s, Receipient e-mail INVALID",tblProcessData.getInt("document_num", 1), dealNumbers.toString()));
				}
			}
		} finally{
			if(Table.isTableValid(deals) == 1){
				deals.destroy();
			}
			Logging.close();
		}		
	}
	

	/**
	 * Sets the Document Info "Regenerate PDF".
	 * 
	 * @param tblProcessData
	 * @param enumVal
	 * @throws OException
	 */
    protected void resetRegenrateDocInfo(Table tblProcessData, EnumRegenrateOutput enumVal)throws OException {
        
        int docNum = tblProcessData.getInt("document_num", 1);
        if(enumVal == EnumRegenrateOutput.NO){
            StlDoc.saveInfoValue(docNum, "Regenerate PDF", EnumRegenrateOutput.NO.name());
        }else {
            StlDoc.saveInfoValue(docNum, "Regenerate PDF", EnumRegenrateOutput.YES.name());
        
        }
        Logging.info("Setting Regenerate PDF on document# " + docNum + " to " + enumVal.name());
    }
	

	/**
	 * Checks for CancellationDocNum & CancellationVATNum document info fields.
	 * 
	 * @param tblProcessData
	 * @param tblUserData
	 * @throws OException
	 */
	private void checkForMissingCancelDocInfoFields(Table tblProcessData, Table tblUserData) throws OException {
		boolean cancelDocNumMissing = false;
		String errorMsg = "";
		int documentNumber = tblProcessData.getInt("document_num", 1);
		
		String cancelDocNum = BOInvoiceUtil.getValueFromGenData(tblUserData, "olfStlDocInfo_CancelDocNum");
		cancelDocNum = (cancelDocNum == null) ? "" : cancelDocNum.trim();
		if (cancelDocNum == null || cancelDocNum.trim().isEmpty()) {
			errorMsg = "Cancellation Doc Num document info field value found null or empty during document cancellation";
			cancelDocNumMissing = true;
		}

		String strCancelVATDocNum = BOInvoiceUtil.getValueFromGenData(tblUserData, "olfStlDocInfo_CancelVATNum");
		if (BOInvoiceUtil.isVATInvoiceApplicable(tblUserData) 
				&& (strCancelVATDocNum == null || strCancelVATDocNum.trim().isEmpty())) {
			errorMsg += (errorMsg.length() > 0) ? ", " : "";
			errorMsg += "Cancellation VAT Num document info field value found null or empty during document cancellation";
			cancelDocNumMissing = true;
		}
		
		if (cancelDocNumMissing) {
			errorMsg += errorMsg.length() > 0 ?  ". Processing it to 'Cancellation Failed'." : "";
			Logging.error(errorMsg);
			
			int documentStatus = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, DOC_STATUS_CANCELLATION_FAILED);
			// Move the document to a Cancellation Failed status
			StlDoc.processDocToStatus(documentNumber, documentStatus);
			throw new OException(errorMsg);
		} else {
			Logging.info("Required cancellation doc info fields are not missing for document #" + documentNumber);
		}
	}
	
	private void linkDealToTransaction(IContainerContext context) throws OException {
		Table dealTable = Util.NULL_TABLE;
		Transaction deal = null;
		try{

//			context.getArgumentsTable().viewTable();
			Table argt = context.getArgumentsTable();
			int previewFlag = argt.getInt("preview_flag", 1);		
			if (previewFlag == 1) {
				return;
			}

			Table  processData = argt.getTable("process_data", 1);
			Table  userData = processData.getTable("user_data", 1);
			String outputFilename = argt.getString("output_filename", 1);
			Set<Integer> tranNums = new HashSet<> ();
			int docTypeId = processData.getInt("doc_type", 1);
			String docType = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, docTypeId); // doc_type column of table stldoc_document_type
			boolean isPdf = outputFilename.endsWith(".pdf");
			int dealDocType = (isPdf)?20001:1;   // value from column "type_id" in file_object_type table
			int docNum = processData.getInt("document_num", 1);
			double currSettleAmt = processData.getDouble("saved_settle_amount", 1);
			int docStatusId = processData.getInt("doc_status", 1);
			String docStatus = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, docStatusId);
			
			java.util.Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");		
			String comment = "Sent on: " + sdf.format(date);
			
			String reference = docType;
			if ("Invoice".equals(docType) && currSettleAmt < 0) {
				reference = "Credit Note";
			}
			if ("Cancelled".equals(docStatus)) {
				reference += " - Cancellation";
			}
			
			if ("Confirm".equals(docType)) {
				userData.sortCol("col_name");
				int rowPriorSetDocumentNum = userData.findString("col_name", "Prior_Sent_Document_Num", SEARCH_ENUM.FIRST_IN_GROUP);
				if (rowPriorSetDocumentNum > 0) {
					String priorSentDocumentNumAsString = userData.getString("col_data", rowPriorSetDocumentNum);
					int priorSentDocumentNum = (priorSentDocumentNumAsString.trim().length() > 0)?Integer.parseInt(priorSentDocumentNumAsString):0;
					if (priorSentDocumentNum > 0 && !"Cancelled".equals(docStatus)) {
						reference += " - Amendment";
					}
				}
			}
			
			dealTable = loadDealsForDocument (docNum); 
			for (int row=dealTable.getNumRows(); row >=1; row--) {
//				int tranGroup = processData.getInt("tran_group", row);
				int dealNum = dealTable.getInt("deal_tracking_num", row);
				Set<Integer> transactions = getTransactionsForTranGroup (dealNum);
				tranNums.addAll(transactions);
			}
		
			//dealTable.destroy();
			for (Integer tranNum : tranNums) {

					deal = Transaction.retrieve(tranNum);
					deal.addDealDocument(outputFilename, dealDocType , 0, reference, comment	, FILE_OBJECT_LINK_TYPE.FILE_OBJECT_LINK_TYPE_FILE);
					Logging.info(String.format("Linked document %s to deal %d", outputFilename, 
									deal.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt())));
					deal.saveDealDocumentTable();
			}
		
		}finally{
			if(Table.isTableValid(dealTable)== 1){
				dealTable.destroy();
			}
			if (deal != null) {
				deal.destroy();
			}
		}
	}

	/**
	 * Check for any previous transition of the input document to '2 Sent to CP' or '2 Received' status in STLDOC_header_hist table.
	 * If not found, then return false.
	 * If found, then return true.
	 * 
	 * @param docNum
	 * @return
	 * @throws OException
	 */
	private boolean checkAnyPrevTransitionToSentToCP(int docNum) throws OException {
		int rowCount = 0;
		Table sqlResult = Util.NULL_TABLE;
		
		try {
			sqlResult = Table.tableNew();
			String sql = String.format("SELECT count(*) row_count FROM stldoc_header_hist h WHERE h.doc_status IN (7, 23) "  //V1.8: Add status '2 Received'
					+ " AND h.document_num = %d", docNum);
			
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL: " + sql));
			}
			
			rowCount = sqlResult.getInt("row_count", 1);
			
		} finally {
			if(Table.isTableValid(sqlResult) == 1){
				TableUtilities.destroy(sqlResult);	
			}
			
		}
		
		return (rowCount > 0);
	}
	
	private Table loadDealsForDocument(int docNum) throws OException {
		String sql = String.format(
				"\nSELECT distinct d.deal_tracking_num "
			+   "\nFROM stldoc_details_hist d"
			+   "\nWHERE d.document_num = %d",
				docNum);
		Table sqlResult = null;			
		sqlResult = Table.tableNew("Trannums for tran group");
		int ret = DBaseTable.execISql(sqlResult, sql);
		if (ret != OLF_RETURN_SUCCEED) {
			sqlResult = TableUtilities.destroy(sqlResult);
			throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql));
		} 
		return sqlResult;
	}

	private Set<Integer> getTransactionsForTranGroup(int tranGroup) throws OException {
		Set<Integer> transForGroup = new HashSet<Integer>();
		
		String sql = String.format(
				"\nSELECT ab2.tran_num "
			+   "\nFROM ab_tran ab"
			+   "\nINNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group AND ab2.current_flag=1"
			+   "\nWHERE ab.deal_tracking_num = %d AND ab.current_flag = 1",
				tranGroup);
		Table sqlResult = null;			
		try {
			sqlResult = Table.tableNew("Trannums for tran group");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql));
			} 
			for (int row=sqlResult.getNumRows(); row >= 1; row--) {
				int tranNum = sqlResult.getInt("tran_num", row);
				transForGroup.add(tranNum);
			}
		} finally {
			if(Table.isTableValid(sqlResult) == 1){
				TableUtilities.destroy(sqlResult);	
			}
			
		}
		return transForGroup;
	}
	
	private boolean validateEmailData(Table tblProcessData) throws OException {
		boolean invalidEmailAddress = false;
		Table params = tblProcessData.getTable("output_data", 1);
		for (int row=params.getNumRows(); row>0; row--) {
			String parameter = params.getString("outparam_name", row);
			if (parameter.contains("Mail Recipients") 
				&& params.getString("outparam_value", row).startsWith("change this to either your email")) {
				processDocumentError(tblProcessData, "Receipient e-mail INVALID. " +  params.getString("outparam_value", row));
				invalidEmailAddress = true;
			} else if (parameter.contains("Mail Recipients")){
				String emailRecipients = params.getString("outparam_value", row);
				
				String[] recipients = emailRecipients.split(",");
				
				for(String recipient : recipients) {
					if(recipient.trim().startsWith("%") && recipient.trim().endsWith("%")) {
						// gen data value
						
						// Output email address field contains a document field.
						// Check that the document field contains data.
						Table tblUserData = tblProcessData.getTable("user_data", 1);
                 
						String parameterValue = recipient.trim();
						String docFieldName = parameterValue.substring(1, parameterValue.length()-1);
                 
						int emailDataRow = tblUserData.unsortedFindString("col_name", docFieldName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
                  
						if(emailDataRow < 0 || emailDataRow > tblUserData.getNumRows()) {
							// Field not found in the gen data
							processDocumentError(tblProcessData, "Recipient e-mail INVALID. Document field " +  docFieldName + " not found in the generation data.");
							invalidEmailAddress = true;
						}
						
						String emailAddresses =   tblUserData.getString("col_data", emailDataRow);	
						if(emailAddresses == null || emailAddresses.length() == 0) {
							processDocumentError(tblProcessData, "Recipient e-mail INVALID. Field " +  docFieldName + " does not contain a value.");
							invalidEmailAddress = true;
						} else  {
							String[] emailAddresseList = emailAddresses.split(",");
							for(String emailAddress : emailAddresseList) {
								if(!isValidEmailAddress(emailAddress.trim())) {
									processDocumentError(tblProcessData, "Recipient e-mail INVALID. " +  emailAddress.trim() + " is not a valid address.");
									invalidEmailAddress = true;
								}
							}
						}								
					} else {
						// assume it's a hard coded email address
						if(!isValidEmailAddress(recipient.trim())) {
							processDocumentError(tblProcessData, "Recipient e-mail INVALID. " +  recipient + " is not a valid address.");
							invalidEmailAddress = true;
						}
					}
				}
			}
		}
		
		return invalidEmailAddress;
		
	}
	
	
    private boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }
    
    private void processDocumentError(Table tblProcessData, String errorDetails) throws OException {
    	
    	Table deals = Util.NULL_TABLE;
    	try{

    		deals = loadDealsForDocument(tblProcessData.getInt("document_num", 1));
    		StringBuilder dealNumbers = new StringBuilder();
    		for (int dealRow=deals.getNumRows(); dealRow >0; dealRow--) {
    			int dealNum = deals.getInt("deal_tracking_num", dealRow);
    			dealNumbers.append(dealNum).append(",");
    		}
    		if (dealNumbers.length()>1){
    			dealNumbers.deleteCharAt(dealNumbers.length()-1);
    		}
    		Logging.info(String.format("Unable to process Document %d Deals:%s, %s",tblProcessData.getInt("document_num", 1), dealNumbers.toString(), errorDetails));
    		
    		UpdateErrorInUserTable.insertErrorRecord(tblProcessData,dealNumbers.toString(), errorDetails );    	
        
    	}finally{
    		if(Table.isTableValid(deals) == 1){
    			deals.destroy();
    		}
    	}
    }
    
    private void debugLogTable(Table tableToLog) throws OException {
    	
    	int numRows = tableToLog.getNumRows();
    	int numColumns = tableToLog.getNumCols();
    	
    	Logging.debug("Logging Table " + tableToLog.getTableName() + " num rows: " + numRows + " num columns: " + numColumns);
    	
    	for(int row = 1; row<= numRows; row++) {
    		StringBuffer output = new StringBuffer();
    		output.append("Row [").append(row).append("] ");
    		for(int col = 1; col<= numColumns; col++) {
    			
    			
    			
    			String columnName = tableToLog.getColName(col);
    			output.append(columnName).append(": [" );

    			switch (COL_TYPE_ENUM.fromInt(tableToLog.getColType(columnName))) {
    				case COL_INT:
    					output.append(tableToLog.getInt(col, row)).append("] ");
    					break;
    				case COL_STRING:
    					output.append(tableToLog.getString(col, row)).append("] ");  
    					break;
    				case COL_TABLE:
    					output.append("Table Data] ");   
    					break;
				default:
					output.append("Unsupported Type] "); 
					break;
    				
    			}
    		}
    		Logging.debug(output.toString());
    	}
    }

}
