package com.jm.stamping.adhoc;

import java.util.Arrays;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class LedgerFieldsStampingAdhoc_M implements IScript {

	private String fieldName;
	private String fieldValue;
	private String dealNums;
	private String documentNums;
	private String taskName;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table output = Util.NULL_TABLE;
		
		try {			
			Table argt = context.getArgumentsTable();
			if (Table.isTableValid(argt) != 1) {
				throw new OException("Invalid argt received from context");
			}
			
			init(argt);
			output = defineOutputTblStructure();
			
			if (this.dealNums != null && !this.dealNums.equals("")) {
				Logging.info(String.format("Processing field-> %s to value->%s for deals->%s", this.fieldName, this.fieldValue, this.dealNums));
				updateDealLedgerField(output);
				
			} else if (this.documentNums != null && !this.documentNums.equals("")) {
				Logging.info(String.format("Processing field-> %s to value->%s for documents->%s", this.fieldName, this.fieldValue, this.documentNums));
				insertOrUpdateDocLedgerField(output);
			}
			
			generateOutputReport(output);
			
			output.group(Constants.OUTPUT_TABLE_COL_STATUS);
			int findRow = output.findString(Constants.OUTPUT_TABLE_COL_STATUS, Constants.FAIL, SEARCH_ENUM.FIRST_IN_GROUP);
			if (findRow > -1) {
				throw new OException(String.format("All rows are not processed successfully. Refer to report starting with %s.. present in outdir/reports folder", this.taskName));
			}
			
		} catch (Exception e) {
			String message = String.format("Error occurred in main script: %s", e.getMessage());
			Logging.error(message);
			throw new OException(message);
			
		} finally {
			Logging.close();
			if (Table.isTableValid(output) == 1) {
				output.destroy();
			}
		}
	}

	/**
	 * Insert/Update entry present in USER_jm_sl_doc_tracking table for the document numbers passed from parameter script.
	 * 
	 * @param output
	 * @throws OException
	 */
	private void insertOrUpdateDocLedgerField(Table output) throws OException {
		Table dataToUpdate = Util.NULL_TABLE;
		Table trackingData = Util.NULL_TABLE;
		String status = null;
		String message = null;
		
		try {
			List<String> documents = Arrays.asList(this.documentNums.split(","));
			trackingData = loadDocTrackingData();
			
			for (String document : documents) {
				document = document.trim();
				int row = output.addRow();
				try {
					int docNum = Integer.parseInt(document);
					int findRow = trackingData.findInt(Constants.USER_SL_DOC_TRACKING_TABLE_COL_DOC_NUM, docNum, SEARCH_ENUM.FIRST_IN_GROUP);
					dataToUpdate = trackingData.cloneTable();
					
					output.setInt(Constants.OUTPUT_TABLE_COL_DOC_NUM, row, docNum);
					output.setString(Constants.OUTPUT_TABLE_COL_NEW_VALUE, row, this.fieldValue);
					
					if (findRow > -1) {
						Logging.info(String.format("Processing to update field-> %s to value->%s for documents->%s", this.fieldName, this.fieldValue, document));
						String currValue = trackingData.getString(Constants.USER_SL_DOC_TRACKING_TABLE_COL_SL_STATUS, findRow);
						output.setString(Constants.OUTPUT_TABLE_COL_PREV_VALUE, row, currValue);

						if (isValuesMatching(currValue, this.fieldValue)) {
							status = Constants.NO_CHANGE;
							message = "Existing value and new value is matching";
							
							Logging.info(String.format("No updates, %s field existing value found to be same as new value (%s) for document_num->%s", this.fieldName, 
									this.fieldValue, document));
						} else {
							trackingData.copyRowAdd(findRow, dataToUpdate);
							updateDocLedgerField(dataToUpdate);
							
							status = Constants.SUCCESS;
							message = "Updated successfully";
							Logging.info(String.format("Successfully updated %s field value to %s for document_num->%s", this.fieldName, this.fieldValue, document));
						}
						
					} else {
						Logging.info(String.format("Processing to insert field-> %s to value->%s for documents->%s", this.fieldName, this.fieldValue, document));
						insertDocLedgerField(dataToUpdate, document);
						
						status = Constants.SUCCESS;
						message = "Inserted successfully";
						Logging.info(String.format("Successfully inserted %s field value to %s for document_num->%s", this.fieldName, this.fieldValue, document));
					}
					
				} catch (NumberFormatException e) {
					status = Constants.FAIL;
					message = String.format("NumberFormatException: Error in parsing document_num %s to integer (error message: %s)", document, e.getMessage());
					Logging.error(message);
					
				} catch (OException oe) {
					status = Constants.FAIL;
					message = oe.getMessage();
					Logging.error(oe.getMessage());
					Logging.error(String.format("Error in updating/inserting %s field value to %s for document_num->%s", this.fieldName, this.fieldValue, document));
					
				} finally {
					if (Table.isTableValid(dataToUpdate) == 1) {
						dataToUpdate.destroy();
					}
				}
				
				output.setString(Constants.OUTPUT_TABLE_COL_STATUS, row, status);
				output.setString(Constants.OUTPUT_TABLE_COL_COMMENT, row, message);
			}
			
		} finally {
			if (Table.isTableValid(trackingData) == 1) {
				trackingData.destroy();
			}
		}
	}

	/**
	 * Insert new entry in USER_jm_sl_doc_tracking table for a document number.
	 * 
	 * @param dataToUpdate
	 * @param documentNum
	 * @throws OException
	 */
	private void insertDocLedgerField(Table dataToUpdate, String documentNum) throws OException {
		dataToUpdate = fetchDocumentDetails(dataToUpdate, documentNum);
		dataToUpdate.setString(Constants.USER_SL_DOC_TRACKING_TABLE_COL_SL_STATUS, dataToUpdate.getNumRows(), this.fieldValue);
		dataToUpdate.setDateTime(Constants.USER_SL_DOC_TRACKING_TABLE_COL_LAST_UPDATE, 1, ODateTime.getServerCurrentDateTime());
		dataToUpdate.setInt(Constants.USER_SL_DOC_TRACKING_TABLE_COL_PERSONNEL_ID, 1, Ref.getUserId());
		DBUserTable.insert(dataToUpdate);
	}
	
	/**
	 * Update existing entry in USER_jm_sl_doc_tracking table for a document number.
	 * 
	 * @param dataToUpdate
	 * @throws OException
	 */
	private void updateDocLedgerField(Table dataToUpdate) throws OException {
		dataToUpdate.setString(Constants.USER_SL_DOC_TRACKING_TABLE_COL_SL_STATUS, 1, this.fieldValue);
		dataToUpdate.setDateTime(Constants.USER_SL_DOC_TRACKING_TABLE_COL_LAST_UPDATE, 1, ODateTime.getServerCurrentDateTime());
		dataToUpdate.setInt(Constants.USER_SL_DOC_TRACKING_TABLE_COL_PERSONNEL_ID, 1, Ref.getUserId());
		dataToUpdate.group(Constants.USER_SL_DOC_TRACKING_TABLE_COL_DOC_NUM);
		DBUserTable.update(dataToUpdate);
	}

	/**
	 * To update the tran_info - General Ledger / Metal Ledger field value for the input deal numbers.
	 * 
	 * @param output
	 * @throws OException
	 */
	private void updateDealLedgerField(Table output) throws OException {
		String status = null;
		String message = null;
		List<String> deals = Arrays.asList(this.dealNums.split(","));
		
		for (String sTranNum : deals) {
			int row = output.addRow();
			try {
				int tranNum = Integer.parseInt(sTranNum.trim());
				Transaction txn = Transaction.retrieve(tranNum);
				String currValue = txn.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, this.fieldName);
				
				output.setInt(Constants.OUTPUT_TABLE_COL_DEAL_NUM, row, tranNum);
				output.setString(Constants.OUTPUT_TABLE_COL_PREV_VALUE, row, currValue);
				output.setString(Constants.OUTPUT_TABLE_COL_NEW_VALUE, row, this.fieldValue);
				
				if (isValuesMatching(currValue, this.fieldValue)) {
					status = Constants.NO_CHANGE;
					message = "Existing value and new value is matching";
					Logging.info(String.format("No updates, %s field existing value found to be same as new value (%s) for deal with tran_num->%s", this.fieldName, 
							this.fieldValue, sTranNum));
				} else {
					txn.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, this.fieldName, this.fieldValue);
					txn.saveTranInfo();
					
					status = Constants.SUCCESS;
					message = "";
					Logging.info(String.format("Successfully updated %s field value to %s for deal with tran_num->%s", this.fieldName, this.fieldValue, sTranNum));
				}
				
			} catch (NumberFormatException e) {
				status = Constants.FAIL;
				message = String.format("NumberFormatException: Error in parsing tran_num %s to integer (error message: %s)", sTranNum, e.getMessage());
				Logging.error(message);
				
			} catch (OException oe) {
				status = Constants.FAIL;
				message = oe.getMessage();
				Logging.error(oe.getMessage());
				Logging.error(String.format("Error in updating %s field value to %s for deal with tran_num->%s", this.fieldName, this.fieldValue, sTranNum));
			}
			
			output.setString(Constants.OUTPUT_TABLE_COL_STATUS, row, status);
			output.setString(Constants.OUTPUT_TABLE_COL_COMMENT, row, message);
		}
	}
	
	private boolean isValuesMatching(String oldVal, String newVal) {
		return ((oldVal == null && newVal == null) || (oldVal != null && newVal != null && oldVal.equals(newVal)));
	}
	
	/**
	 * Generate output report containing status, error message etc. for all document/deal numbers passed from the parameter script.
	 * 
	 * @param tData
	 * @throws OException
	 */
	private void generateOutputReport(Table tData) throws OException {
		StringBuilder fileName = new StringBuilder();
		Table refInfo = Util.NULL_TABLE;
		
		try {
			refInfo = Ref.getInfo();
			this.taskName = refInfo.getString("task_name", 1);
			
			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append(this.taskName);
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");

			tData.printTableDumpToFile(fileName.toString());
		} finally {
			if (Table.isTableValid(refInfo) == 1) {
				refInfo.destroy();
			}
		}
	}

	/**
	 * Fetch document details from stldoc_header table for the input document number.
	 * 
	 * @param dataToUpdate
	 * @param documentNum
	 * @return
	 * @throws OException
	 */
	private Table fetchDocumentDetails(Table dataToUpdate, String documentNum) throws OException {
		String sqlQuery = String.format("SELECT sh.document_num, '' sl_status, sh.last_update, sh.personnel_id, sh.doc_status, sh.last_doc_status, sh.doc_version,"
					+ " sh.stldoc_hdr_hist_id, '' sap_status "
		        + "\n FROM stldoc_header sh " 
		        + "\n WHERE sh.document_num = %s ", documentNum);
		
		Logging.info(String.format("Executing SQL query: %s", sqlQuery));
		DBaseTable.execISql(dataToUpdate, sqlQuery);
		if (dataToUpdate.getNumRows() == 0) {
			throw new OException(String.format("No rows found in stldoc_header table for document_num->%s", documentNum));
		}
		
		return dataToUpdate;
	}
	
	/**
	 * This method is used to load USER_jm_sl_doc_tracking table data.
	 * 
	 * @return
	 * @throws OException
	 */
	private Table loadDocTrackingData() throws OException {
		Table trackingData = Table.tableNew();
		trackingData.setTableName(Constants.USER_JM_SL_DOC_TRACKING_TABLE);
		DBUserTable.load(trackingData);
		return trackingData;
	}
	
	/**
	 * 
	 * @return
	 * @throws OException
	 */
	private Table defineOutputTblStructure() throws OException {
		Table tOutput = Table.tableNew();
		tOutput.addCol(Constants.OUTPUT_TABLE_COL_DEAL_NUM, COL_TYPE_ENUM.COL_INT);
		tOutput.addCol(Constants.OUTPUT_TABLE_COL_DOC_NUM, COL_TYPE_ENUM.COL_INT);
		tOutput.addCol(Constants.OUTPUT_TABLE_COL_PREV_VALUE, COL_TYPE_ENUM.COL_STRING);
		tOutput.addCol(Constants.OUTPUT_TABLE_COL_NEW_VALUE, COL_TYPE_ENUM.COL_STRING);
		tOutput.addCol(Constants.OUTPUT_TABLE_COL_STATUS, COL_TYPE_ENUM.COL_STRING);
		tOutput.addCol(Constants.OUTPUT_TABLE_COL_COMMENT, COL_TYPE_ENUM.COL_STRING);
		return tOutput;
	}
	
	/**
	 * Initialising instance variables from the argument table.
	 */
	protected void init(Table argT) throws Exception {
		ConstRepository constRepo = new ConstRepository(Constants.CONST_REPO_CONTEXT, Constants.CONST_REPO_SUB_CONTEXT);
		initialiseLogger(constRepo);
		
		this.fieldName = argT.getString(Constants.LEDGER_FIELD_NAME_IDENTIFIER, 1);
		this.fieldValue = argT.getString(Constants.LEDGER_FIELD_VALUE_INDETIFIER, 1);
		
		this.dealNums = argT.getString(Constants.DEAL_NUMS_INDETIFIER, 1);
		this.documentNums = argT.getString(Constants.DOC_NUMS_INDETIFIER, 1);
	}
	
	/**
     * Initialise Logging by retrieving log settings from ConstRepository.
     *
     * @param context the context
     */
    protected void initialiseLogger(ConstRepository constRepo) {
		
        try {
            Logging.init(this.getClass(), "", "");

        } catch (Exception ex) {
        	Logging.error("Error initializing logging");
            throw new RuntimeException("Error initializing logging", ex);
        }       
    }

}
