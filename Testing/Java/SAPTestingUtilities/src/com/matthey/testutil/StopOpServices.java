package com.matthey.testutil;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;

/**
 * Stop a collection of op services
 * @author KailaM01
 */
public class StopOpServices extends BulkOperationScript
{
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_stopping_op_service", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		
		try
		{
			int ret = tblCsvData.inputFromCSVFile(csvPath);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + csvPath);
			}
			
			/* Fix header column names! */
			com.matthey.testutil.common.Util.updateTableWithColumnNames(tblCsvData);
		}
		catch (Exception e)
		{
			com.matthey.testutil.common.Util.printStackTrace(e);
			throw new SapTestUtilRuntimeException("Unable to run generateInputData()", e);
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
		
		return tblCsvData;
	}

	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		tblInputData.addCol(SAPTestUtilitiesConstants.STATUS, COL_TYPE_ENUM.COL_STRING);
		
		int numRows = tblInputData.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			String opServiceName = tblInputData.getString(1, row);
			
			try
			{
				OpService.stopMonitoring(opServiceName);	
			}
			catch (Exception e)
			{
				Logging.error("Error encountered during turn off of: " + opServiceName);
				tblInputData.setString(SAPTestUtilitiesConstants.STATUS, row, e.getMessage());
				continue;
			}
			
			tblInputData.setString(SAPTestUtilitiesConstants.STATUS, row, "Op service:" + opServiceName + " stoped!");
		}
		
		return tblInputData;
	}

	@Override
	public String getOperationName() 
	{
		return "Bulk Stop Op Services";
	}
}