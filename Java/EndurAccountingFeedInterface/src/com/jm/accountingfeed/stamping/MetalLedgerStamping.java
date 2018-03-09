package com.jm.accountingfeed.stamping;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableMetalLedgerDataColumns;
import com.jm.accountingfeed.enums.EndurTranInfoField;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.JDEStatus;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Stamping class for Metal Ledger
 * It stamps the "Metals Ledger" tran info to "Sent" or "Cancelled Sent" on the extracted deals
 * @author jains03
 *
 */
public class MetalLedgerStamping extends Stamping 
{
    @Override
    protected String getAuditUserTable() 
    {
        return ExtractionTableName.METALS_LEDGER.toString();
    }

    @Override
    protected void initialisePrerequisites() throws OException 
    {
        // No initialisation
    }

    @Override
    protected void stampRecords() throws OException 
    {
        int numRows = tblRecordsToStamp.getNumRows();
        
        for (int row = 1; row <= numRows; row++)
        {
            int tranNum = tblRecordsToStamp.getInt("tran_num", row);
            int tranStatus = tblRecordsToStamp.getInt("tran_status", row);
            
            try 
            {
                stampMLRecord(tranNum, tranStatus);
                /* Set processed = "P" */
                tblRecordsToStamp.setString(BoundaryTableMetalLedgerDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.PROCESSED.toString());
            } 
            catch (Exception e) 
            {
                String errMsg = "Exception in stamping." + e.getMessage();
                PluginLog.error(errMsg + ".tranNum=" + tranNum);
                /* Set processed = "E" */
                tblRecordsToStamp.setString(BoundaryTableMetalLedgerDataColumns.PROCESS_STATUS.toString(), row, AuditRecordStatus.ERROR.toString());
                tblRecordsToStamp.setString(BoundaryTableMetalLedgerDataColumns.ERROR_MSG.toString(), row, errMsg);
            }
        }
    }
    
    /**
     * Updated the 'Metal Ledger' tran-info field of the transaction.
     * If trade is Validated, set to "Sent"
     * If trade is Cancelled, set to "Cancelled Sent"
     * @param tranNum
     * @param tranStatus
     * @throws OException
     */
    private void stampMLRecord(int tranNum, int tranStatus) throws OException
    {
    	Table tranInfo = null;
    	
    	try
    	{
    		tranInfo = Table.tableNew();
            Transaction.retrieveTranInfo(tranInfo, tranNum);
            
            int glInfoRow = tranInfo.unsortedFindString("Type", EndurTranInfoField.METAL_LEDGER.toString(), SEARCH_CASE_ENUM.CASE_INSENSITIVE);
            
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
     * This method updates the 'last_update' timestamp (in ML boundary table) of the Stamped records
     */
    @Override
    protected void stampingProcessed() throws OException 
    {
        tblRecordsToStamp.deleteWhereString("process_status", "");
        
        Table tblUserTable = tblRecordsToStamp.copyTable();
        tblUserTable.setTableName(ExtractionTableName.METALS_LEDGER.toString());
        
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
}
