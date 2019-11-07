package com.matthey.testutil.mains;

import com.matthey.testutil.BulkOperationScript;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

/**
 * Abstract class for generating deal data (similar to a trade listing) to be
 * used for comparison outputs
 */
public abstract class GenerateDealCheckpointData extends BulkOperationScript
{		
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_deal_verification", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		Table tblData = Table.tableNew("Deals to cancel");
		Table tblCoverageDeals = null;
		Table tblTransferDeals = null;
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
			tblTransferDeals = CancelSapDeals.getTransferDeals(tblCsvData);
			
			/* Merge everything into one table */
			tblTransferDeals.copyRowAddAllByColName(tblCoverageDeals);
			
			tblData.select(tblCoverageDeals, "*", "tran_num GT -1");
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to run generateInputData() for cancellations", e);
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
		}
		
		return tblData;
	}

	@Override
	public Table performBulkOperation(Table tblInput) throws OException 
	{
		return tblInput;
	}

	@Override
	public String getOperationName() 
	{
		return "Checkpoint Deal Data";
	}
	
	/**
	 * @return Table with output format
	 * @throws OException
	 */
	public abstract Table createOutputFormat() throws OException;
}
