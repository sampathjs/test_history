package com.jm.accountingfeed.rb.output;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableGeneralLedgerDataColumns;
import com.jm.accountingfeed.enums.BoundaryTableRefDataColumns;
import com.jm.accountingfeed.enums.BoundaryTableSalesLedgerDataColumns;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2019-01-10	V1.0	jwaechter	- Initial Version
 * 2019-11-06	V1.1	jwaechter	- Added population of USER_jm_jde_interface_run_logs table
 */

/**
 * The Report Builder output plugin for the Shanghai Sales Ledger.
 * As the Shanghai Sales Ledger extracts shares the same XML output format with
 * the Shanghai General Ledger it is derived from {@link ShanghaiGeneralLedgerOutput}.
 * The difference lies solely within a different integration into the auditing system.
 * @author jwaechter
 * @version 1.1
 */

public class ShanghaiSalesLedgerOutput extends ShanghaiGeneralLedgerOutput {

    /**
     * Insert the Audit boundary table for General Ledger extract with the 'Trade' payload xml for every Deal in the Report output 
     */
	@Override
	public void extractAuditRecords() throws OException
	{
		extractSalesLedgerAuditingTable();
	}

	public void extractSalesLedgerAuditingTable() throws OException {
		Table tableToInsert = null;
		try
		{
			tableToInsert = Table.tableNew(reportParameter.getStringValue("boundary_table"));
			int numRows = tblOutputData.getNumRows();
			ODateTime timeIn = ODateTime.getServerCurrentDateTime();
			
			String auditRecordStatusString = null; 
			
			DBUserTable.structure(tableToInsert);
			
			tableToInsert.addNumRows(numRows);
						
			tableToInsert.setColValInt(BoundaryTableRefDataColumns.EXTRACTION_ID.toString(), getExtractionId());
			tableToInsert.setColValString(BoundaryTableRefDataColumns.REGION.toString(), reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()));
			tableToInsert.setColValDateTime(BoundaryTableRefDataColumns.TIME_IN.toString(), timeIn);
			
            for (int row = 1; row <= numRows; row++)			
            {
    			int endurDocNum = tblOutputData.getInt("endur_doc_num", row);
    			int endurDocStatus = tblOutputData.getInt("endur_doc_status", row);
                String payLoad = tblOutputData.getClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row);             
                tableToInsert.setInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_NUM.toString(), row, endurDocNum);
                tableToInsert.setInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_STATUS.toString(), row, endurDocStatus);
                tableToInsert.setClob(BoundaryTableSalesLedgerDataColumns.PAYLOAD.toString(), row, payLoad);
			}
            for (int row = numRows; row > 0; row--) {
                String clob = tableToInsert.getClob(BoundaryTableSalesLedgerDataColumns.PAYLOAD.toString(), row);
                if (clob == null || clob.trim().length() == 0 || clob.equals("XXX")) {
                	tableToInsert.delRow(row);
                }
            }
            // merge different rows for the same document, usually financial deals
            for (int row = tableToInsert.getNumRows(); row >= 1; row--)			
            {
    			int endurDocNum1 = tableToInsert.getInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_NUM.toString(), row);
    			int endurDocStatus1 = tableToInsert.getInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_STATUS.toString(), row);
                for (int row2 = row-1; row2 >= 1; row2--) {
        			int endurDocNum2 = tableToInsert.getInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_NUM.toString(), row2);
        			int endurDocStatus2 = tableToInsert.getInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_STATUS.toString(), row2);
        			if (endurDocNum2 == endurDocNum1 && endurDocStatus2 == endurDocStatus1) {
        				// duplicate keys found, merge CLOBs
                        String payLoad1 = tableToInsert.getClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row);
        				String payLoad2 = tableToInsert.getClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row2);
        				String payLoadTotal = payLoad1 + "\n\n" + payLoad2;
        				tableToInsert.setClob(BoundaryTableSalesLedgerDataColumns.PAYLOAD.toString(), row2, payLoadTotal);
        				tableToInsert.delRow(row);
        				break;
        			}
                }
			}
            
			if(null == errorDetails || errorDetails.isEmpty())
			{
				auditRecordStatusString = AuditRecordStatus.NEW.toString();
			}
			else
			{
				auditRecordStatusString = AuditRecordStatus.ERROR.toString();
				tableToInsert.setColValString(BoundaryTableRefDataColumns.ERROR_MSG.toString(), errorDetails);
			}
			tableToInsert.setColValString(BoundaryTableRefDataColumns.PROCESS_STATUS.toString(), auditRecordStatusString);
            tableToInsert.clearGroupBy();
            tableToInsert.addGroupBy(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_NUM.toString());
            tableToInsert.groupBy();
			int retval = DBUserTable.insert(tableToInsert);
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
			    PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.insert() failed"));
			}
			tableToInsert.destroy();
			tableToInsert = null;
		}
		catch (OException oException)
		{
			String message = "Exception occurred while extracting records.\n" + oException.getMessage();
			PluginLog.error(message);
			Util.printStackTrace(oException);
			throw oException;
		}
		finally
		{
			if (tableToInsert != null)
			{
				tableToInsert.destroy();
			}
		}
	}
}
