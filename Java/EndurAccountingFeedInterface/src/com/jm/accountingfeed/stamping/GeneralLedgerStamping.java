package com.jm.accountingfeed.stamping;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableGeneralLedgerDataColumns;
import com.jm.accountingfeed.enums.EndurTranInfoField;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.JDEStatus;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.util.Constants;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Stamping class for General Ledger
 * It stamps the "General Ledger" tran info to "Sent" or "Cancelled Sent" on the extracted deals
 * @author jains03
 *
 */
public class GeneralLedgerStamping extends Stamping 
{
	@Override
	protected void initialisePrerequisites() throws OException 
	{
		// No initialisation
	}
	
	@Override
	protected String getAuditUserTable() 
	{
		return ExtractionTableName.GENERAL_LEDGER.toString();
	}

	@Override
	protected void stampRecords() throws OException 
	{
		int numRows = tblRecordsToStamp.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
		    int toolset = tblRecordsToStamp.getInt("toolset", row);
            String fixingsComplete = tblRecordsToStamp.getString("fixings_complete", row);
            int tranNum = tblRecordsToStamp.getInt("tran_num", row);
            int tranStatus = tblRecordsToStamp.getInt("tran_status", row);
            if(TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() == toolset && !"Y".equalsIgnoreCase(fixingsComplete))
            {
                PluginLog.info("Skipping GL stamping of Commodity trade not yet Fixed " + tranNum);
                continue;
            }
			
			try 
			{
			    stampGLRecord(tranNum, tranStatus);
			    /* Set processed = "P" */
			    tblRecordsToStamp.setString(BoundaryTableGeneralLedgerDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.PROCESSED.toString());
			} 
			catch (Exception e) 
			{
                String errMsg = "Exception in stamping." + e.getMessage();
                PluginLog.error(errMsg + ".tranNum=" + tranNum);
                /* Set processed = "E" */
                tblRecordsToStamp.setString(BoundaryTableGeneralLedgerDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.ERROR.toString());
                tblRecordsToStamp.setString(BoundaryTableGeneralLedgerDataColumns.ERROR_MSG.toString(), row, errMsg);
            }
		}
		//Delete toolset and fixings_complete columns to make the structure of tblRecordsToStamp consistent with Boundary table
		tblRecordsToStamp.delCol("toolset");
        tblRecordsToStamp.delCol("fixings_complete");
	}
	
	/**
	 * Updated the 'General Ledger' tran-info field of the transaction.
	 * If trade is Validated, set to "Sent"
	 * If trade is Cancelled, set to "Cancelled Sent"
	 * @param tranNum
	 * @param tranStatus
	 * @throws OException
	 */
	private void stampGLRecord(int tranNum, int tranStatus) throws OException
	{
		Table tranInfo = null;
		
		try
		{
			tranInfo = Table.tableNew();
	        Transaction.retrieveTranInfo(tranInfo, tranNum);
	        
	        int glInfoRow = tranInfo.unsortedFindString("Type", EndurTranInfoField.GENERAL_LEDGER.toString(), SEARCH_CASE_ENUM.CASE_INSENSITIVE);
	        
	        JDEStatus nextJdeStatus = (tranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt())? JDEStatus.CANCELLED_SENT : JDEStatus.SENT;
	        tranInfo.setString("Value", glInfoRow, nextJdeStatus.toString());
	        
	        tranInfo.delCol(1);
	        Transaction.insertTranInfo(tranInfo, tranNum);
		}
		finally
		{
			if (tranInfo != null) 
			{
				tranInfo.destroy();
			}
		}
	}

	/**
	 * This method updates the 'last_update' timestamp (in GL boundary table) of the Stamped records
	 */
	@Override
	protected void stampingProcessed() throws OException 
	{
        tblRecordsToStamp.deleteWhereString("process_status", "");
        
        Table tblUserTable = tblRecordsToStamp.copyTable();
        tblUserTable.setTableName(ExtractionTableName.GENERAL_LEDGER.toString());
        
        tblUserTable.setColValDateTime("last_update", ODateTime.getServerCurrentDateTime());
        
        /* Need to group for update to work */
        tblUserTable.clearGroupBy();
        tblUserTable.group("extraction_id, deal_num");
        tblUserTable.groupBy();
        
        int ret = DBUserTable.update(tblUserTable);
        
        if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
        {
            String appMessage = DBUserTable.dbRetrieveErrorInfo(ret, "DBUserTable.update() failed");
            PluginLog.error(appMessage);
            
            throw new AccountingFeedRuntimeException("Unable to set audit record process_status to complet. " + appMessage);
        }   
	}

	@Override
	protected void cleanup() throws OException 
	{
		
	}
	
	/**
	 * Fetch records to be stamped.
	 * Populate toolset and fixings_complete columns in the Table containing records to stamp. 
	 */
	@Override
	protected Table getRecordsToStamp() throws OException
    {
        Table tblData = Table.tableNew("Records to Stamp");
        
        String sqlQuery = 
                "SELECT audit_data.extraction_id," + 
        				"audit_data.deal_num," +
        				"audit_data.tran_num," +
        				"ab_tran.tran_status," +
        				"audit_data.region," +
        				"audit_data.time_in," +
        				"audit_data.last_update," +
        				"audit_data.payload," +
        				"audit_data.process_status," +
        				"audit_data.error_msg," +
        				"audit_data.message_key_1," +
        				"audit_data.message_key_2," +
        				"audit_data.message_key_3 \n" +
                ", ab_tran.toolset, market_data.fixings_complete  \n" +
                    "FROM " + getAuditUserTable() + " audit_data \n" +
                " JOIN ab_tran on " +
                    " ab_tran.deal_tracking_num = audit_data.deal_num and  ab_tran.current_flag =1 \n" +
                "AND audit_data.region = '" + getRegion() + "' \n" +
                " JOIN ab_tran_info on " +
				" ab_tran_info.tran_num = audit_data.tran_num \n" +
				" JOIN tran_info_types on "  +
					" tran_info_types.type_id = ab_tran_info.type_id " +
				" AND tran_info_types.type_name = 'General Ledger' and ab_tran_info.value IN('" +
                JDEStatus.PENDING_SENT.toString()+ "','" + 
				JDEStatus.PENDING_CANCELLED.toString()+
                    "')AND audit_data.process_status = '" + AuditRecordStatus.NEW + "' \n" +
                " LEFT JOIN " + Constants.USER_JM_JDE_EXTRACT_DATA  + " as market_data  on " +
                    " market_data.deal_num = audit_data.deal_num  \n"
					;
        int ret = DBaseTable.execISql(tblData, sqlQuery);
        
        if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
        {
            throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
        }
        
        return tblData;
    }

}
