package com.matthey.testutil;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Start a collection of op services
 * @author KailsM01
 * 
 */
public class StartOpServices extends StopOpServices
{
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
				OpService.startMonitoring(opServiceName);	
				tblInputData.setString(SAPTestUtilitiesConstants.STATUS, row, "Op service:" + opServiceName + " started!");
			}
			catch (Exception e)
			{
				Logging.error("Error encountered during turn on of: " + opServiceName);
				tblInputData.setString(SAPTestUtilitiesConstants.STATUS, row, e.getMessage());
			}
		}
		
		return tblInputData;
	}

	@Override
	public String getOperationName() 
	{
		return "Bulk Start Op Services";
	}
}