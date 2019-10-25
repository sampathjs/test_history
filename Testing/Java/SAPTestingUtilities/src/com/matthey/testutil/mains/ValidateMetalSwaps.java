package com.matthey.testutil.mains;

import java.util.List;

import com.matthey.testutil.BulkAmendDealStatus;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/**
 * This utility is to validate metal swap deals provided in the CSV input file
 * Get deals and validate them 
 * @author KailaM01
 *
 */
public class ValidateMetalSwaps extends BulkAmendDealStatus
{
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_swaps_validation", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		Table tblData = Table.tableNew("Deals to cancel");
		Table tblCoverageDeals = null;
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
			
			tblData.select(tblCoverageDeals, "*", "tran_num GT -1");
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to run generateInputData() for validating metal swaps", e);
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
		}
		
		return tblData;
	}

	@Override
	public List<Integer> getAllowedTranStatus() 
	{
		return null;
	}

	@Override
	public TRAN_STATUS_ENUM getDestinationStatus() 
	{
		return TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED;
	}
	
	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		com.matthey.testutil.common.Util.unLockDeals();

		return super.performBulkOperation(tblInputData);
	}
}