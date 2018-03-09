package com.jm.accountingfeed.stamping;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableGeneralLedgerDataColumns;
import com.jm.accountingfeed.enums.BoundaryTableSalesLedgerDataColumns;
import com.jm.accountingfeed.enums.EndurDocumentStatus;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.JDEStatus;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.util.Constants;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Stamping class for Sales Ledger
 * It stamps the user_jm_sl_doc_tracking.sl_status to "Sent" or "Cancelled Sent" on the extracted documents
 * @author jains03
 *
 */
public class SalesLedgerStamping extends Stamping 
{
	private Table tblTrackingData = null;
	
	@Override
	protected void initialisePrerequisites() throws OException 
	{
		if (tblRecordsToStamp == null || tblRecordsToStamp.getNumRows() == 0)
		{
			PluginLog.info("No records found to stamp for current extraction, skipping initialisePrerequisites..");
			
			return;
		}
		
		int queryId = Query.tableQueryInsert(tblRecordsToStamp, "endur_doc_num");
		
		try
		{
			/* Cache contents of user_jm_sl_doc_tracking for current documents that are to be stamped */
			if (tblTrackingData == null)
			{
				tblTrackingData = Table.tableNew("SL doc tracking data for current documents");
				
				queryId = Query.tableQueryInsert(tblRecordsToStamp, "endur_doc_num");
				
				if (queryId > 0)
				{
					String sqlQuery = 
						"SELECT doc_tracking.* \n" +
						"FROM " + Constants.USER_JM_SL_DOC_TRACKING + " doc_tracking \n" +
						"JOIN query_result qr ON qr.query_result = doc_tracking.document_num \n " +
						"AND qr.unique_id = " + queryId;
					
					int ret = DBaseTable.execISql(tblTrackingData, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
					}
					
					/* Group for quick sort-based filtering later */
					tblTrackingData.clearGroupBy();
					tblTrackingData.group("document_num");
					tblTrackingData.groupBy();
				}	
			}	
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);		
			}	
		}
	}
	
	@Override
	protected String getAuditUserTable() 
	{
		return ExtractionTableName.SALES_LEDGER.toString();
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
				tblRecordsToStamp.setString(BoundaryTableSalesLedgerDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.PROCESSED.toString());
			}
			catch (Exception e)
			{
				/* Log an error and continue with other audit records */
				
				String error = "Unable to stamp document in: " + Constants.USER_JM_SL_DOC_TRACKING + ", " + docNum + ", " + e.getMessage();
				
				PluginLog.error(error);

                tblRecordsToStamp.setString(BoundaryTableGeneralLedgerDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.ERROR.toString());
                tblRecordsToStamp.setString(BoundaryTableGeneralLedgerDataColumns.ERROR_MSG.toString(), row, error);
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
	private void stampDocument(int docNum, String jdeStatus) throws OException 
	{
		Table userTableData = Table.tableNew(Constants.USER_JM_SL_DOC_TRACKING);
		userTableData.addCols("I(document_num) S(sl_status) T(last_update) I(personnel_id)");
		
		/* Need to group for update to work */
		userTableData.clearGroupBy();
		userTableData.group("document_num");
		userTableData.groupBy();
		
		int findRow = tblTrackingData.findInt("document_num", docNum, SEARCH_ENUM.FIRST_IN_GROUP);
		
		int newRow = userTableData.addRow();
		userTableData.setInt("document_num", newRow, docNum);
		userTableData.setString("sl_status", newRow, jdeStatus);
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
		
			throw new AccountingFeedRuntimeException("Unable to insert/update SL record for doc_num: " + docNum + ". " + appMessage);
		}
	}

    /**
     * This method updates the 'last_update' timestamp (in SL boundary table) of the Stamped records
     */
	@Override
	protected void stampingProcessed() throws OException 
	{
		Table tblUserTable = Table.tableNew(ExtractionTableName.SALES_LEDGER.toString());
		tblUserTable.addCols("I(extraction_id) I(endur_doc_num) S(process_status) T(last_update)");
		
		tblUserTable.select(tblRecordsToStamp, "extraction_id, endur_doc_num, process_status", "process_status EQ " + AuditRecordStatus.PROCESSED.toString());
		tblUserTable.setColValDateTime("last_update", ODateTime.getServerCurrentDateTime());
		
		/* Need to group for update to work */
		tblUserTable.clearGroupBy();
		tblUserTable.group("extraction_id, endur_doc_num");
		tblUserTable.groupBy();
		
		int ret = DBUserTable.update(tblUserTable);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			String appMessage = DBUserTable.dbRetrieveErrorInfo(ret, "DBUserTable.update() failed");
			
			throw new AccountingFeedRuntimeException("Unable to set audit record process_status to complete. " + appMessage);
		}	
	}

	@Override
	protected void cleanup() throws OException 
	{
		if (tblTrackingData != null)
		{
			tblTrackingData.destroy();
		}
	}
}
