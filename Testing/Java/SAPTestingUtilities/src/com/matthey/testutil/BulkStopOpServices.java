package com.matthey.testutil;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

public abstract class BulkStopOpServices extends BulkOperationScript
{
	@Override
	public Table generateInputData() throws OException 
	{
		return null;
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
				PluginLog.error("Error encountered during turn off of: " + opServiceName);
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