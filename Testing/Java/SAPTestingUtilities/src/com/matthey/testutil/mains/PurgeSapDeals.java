package com.matthey.testutil.mains;

import com.matthey.testutil.BulkPurgeDeals;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;

/**
 * Utility to purge deals created using SAP interfaces
 * @author SharmV04
 *
 */
public class PurgeSapDeals extends BulkPurgeDeals
{
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_cancellations", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path for Cancellations: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		Table tblData = Table.tableNew("Deals to cancel");
		Table tblCoverageDeals = null;
		Table tblTransferDeals = null;
		Table tblReferenceDeals = null;
		
		try
		{
			int ret = tblCsvData.inputFromCSVFile(csvPath);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + csvPath);
			}
			
			/* Fix header column names! */
			com.matthey.testutil.common.Util.updateTableWithColumnNames(tblCsvData);
			
			tblCoverageDeals = CancelSapDeals.getCoverageDeals(tblCsvData);
			Logging.debug("tblCoverageDeals table:");
			Util.printTableOnLogTable(tblCoverageDeals);
			
			tblTransferDeals = CancelSapDeals.getTransferDeals(tblCsvData);
			Logging.debug("tblTransferDeals table:");
			Util.printTableOnLogTable(tblTransferDeals);
			
			tblReferenceDeals = CancelSapDeals.getReferenceDeals(tblCsvData);
			Logging.debug("tblReferenceDeals table:");
			Util.printTableOnLogTable(tblReferenceDeals);
			
			/* Merge everything into one table */
			tblTransferDeals.copyRowAddAllByColName(tblCoverageDeals);
			tblReferenceDeals.copyRowAddAllByColName(tblCoverageDeals);
			
			tblData.select(tblCoverageDeals, "*", "tran_num GT -1");
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to run Instrument Fixings", e);
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
			
				tblCsvData.destroy();
			
			if (tblCoverageDeals != null)
			{
				tblCoverageDeals.destroy();
			}
			
			if (tblTransferDeals != null)
			{
				tblTransferDeals.destroy();
			}
			
			if (tblReferenceDeals != null)
			{
				tblReferenceDeals.destroy();
			}
		}
		
		return tblData;
	}
	
	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		com.matthey.testutil.common.Util.unLockDeals();

		return super.performBulkOperation(tblInputData);
	}
}
