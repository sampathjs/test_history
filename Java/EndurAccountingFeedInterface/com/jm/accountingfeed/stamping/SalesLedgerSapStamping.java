package com.jm.accountingfeed.stamping;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableSalesLedgerSapDataColumns;
import com.jm.accountingfeed.enums.EndurDocumentStatus;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.JDEStatus;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.util.Constants;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.logging.PluginLog;


public class SalesLedgerSapStamping extends SalesLedgerStamping
{
	@Override
	protected String getAuditUserTable() 
	{
		return ExtractionTableName.SALES_LEDGER_SAP.toString();
	}
	
	@Override
	protected void stampRecords() throws OException 
	{
		int numRows = tblRecordsToStamp.getNumRows();

		if (tblRecordsToStamp == null || numRows == 0)
		{
			PluginLog.info("No records found to stamp for current extraction..");
			
			return;
		}
		
		Table tblSlDocTracking = Table.tableNew(Constants.USER_JM_SL_DOC_TRACKING);
		int ret = DBUserTable.structure(tblSlDocTracking);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new AccountingFeedRuntimeException("Unable to get structure of table: " + Constants.USER_JM_SL_DOC_TRACKING);
		}
		
		/* Loop through boundary table and identify records to be stamped */
		for (int row = 1; row <= numRows; row++)
		{
			int docNum = tblRecordsToStamp.getInt("endur_doc_num", row);
			int docStatus = tblRecordsToStamp.getInt("endur_doc_status", row);
			
			try
			{
				String jdeStatus = JDEStatus.SENT.toString();
				jdeStatus = (docStatus == EndurDocumentStatus.CANCELLED.id()) ? JDEStatus.CANCELLED_SENT.toString() : jdeStatus;
				
				stampDocument(docNum, jdeStatus);
				
				/* Set processed = "P" */
				tblRecordsToStamp.setString(BoundaryTableSalesLedgerSapDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.PROCESSED.toString());
			}
			catch (Exception e)
			{
				/* Log an error and continue with other audit records */
				
				String error = "Unable to stamp document in: " + Constants.USER_JM_SL_DOC_TRACKING + ", " + docNum + ", " + e.getMessage();
				
				PluginLog.error(error);

                tblRecordsToStamp.setString(BoundaryTableSalesLedgerSapDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.ERROR.toString());
                tblRecordsToStamp.setString(BoundaryTableSalesLedgerSapDataColumns.ERROR_MSG.toString(), row, error);
			}
		}
	}

	/**
	 * Mark a document in user_jm_sl_doc_tracking as "Sent" or "Cancelled Sent"
	 * 
	 * @param docNum
	 * @param jdeStatus
	 * @throws OException
	 */
	private void stampDocument(int docNum, String sapStatus) throws OException 
	{
		Table userTableData = Table.tableNew(Constants.USER_JM_SL_DOC_TRACKING);
		userTableData.addCols("I(document_num) S(sap_status) T(last_update) I(personnel_id)");
		
		/* Need to group for update to work */
		userTableData.clearGroupBy();
		userTableData.group("document_num");
		userTableData.groupBy();
		
		int findRow = tblTrackingData.findInt("document_num", docNum, SEARCH_ENUM.FIRST_IN_GROUP);
		
		int newRow = userTableData.addRow();
		userTableData.setInt("document_num", newRow, docNum);
		userTableData.setString("sap_status", newRow, sapStatus);
		userTableData.setDateTime("last_update", newRow, ODateTime.getServerCurrentDateTime());
		userTableData.setInt("personnel_id", newRow, Ref.getUserId()); 		
		
		int ret = -1;
		if (findRow > 0)
		{
			ret = DBUserTable.update(userTableData);
		}
		else
		{
			ret = DBUserTable.insert(userTableData);
		}
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			String appMessage = DBUserTable.dbRetrieveErrorInfo(ret, "DBUserTable.update() failed");
		
			throw new AccountingFeedRuntimeException("Unable to insert/update SL Sap record for doc_num: " + docNum + ". " + appMessage);
		}
	}
}
