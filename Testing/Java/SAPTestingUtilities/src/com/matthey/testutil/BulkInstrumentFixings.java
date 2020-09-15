package com.matthey.testutil;

import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.jm.logging.Logging;

/**
 * Fix a group of deals using the historical price saved for todays trading/business date
 * 
 * Child class should be responsible for providing the correct list of deals that
 * have a valid fixing date = today, via the generateInputData function
 */
public abstract class BulkInstrumentFixings extends BulkOperationScript
{
	@Override
	public Table generateInputData() throws OException 
	{
		return null;
	}

	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		Table tblTemp = null;
		
		try
		{
			int today = OCalendar.today();
			String todayStr = OCalendar.formatDateInt(today);

			Logging.info("Attempting to run instrument fixings for " + tblInputData.getNumRows() + "deals, for date: " + todayStr);
		
			if (tblInputData.getNumRows() == 0)
			{
				Logging.info("No instruments to be fixed..");
				return tblInputData;
			}
			
			/* Filter table to fetch tran numbers only */
			tblTemp = Table.tableNew();
			tblTemp.select(tblInputData, "tran_num", "tran_num GT -1");
			
			Table tblResetInfo = com.olf.openjvs.EndOfDay.resetDealsByTranList(tblTemp, OCalendar.today());
			
			int numRows = tblResetInfo.getNumRows();
			for (int row = 1; row <= numRows; row++)
            {
				if (tblResetInfo.getInt("success", row) == 0)
				{
					Logging.error("Error occured during fixing - please see output log file");
				}
            }
			
			Logging.info("Instrument fixings completed!");

			if (tblResetInfo.getNumRows() > 0)
			{
				return tblResetInfo;
			}
		}
		catch (Exception e)
		{
			com.matthey.testutil.common.Util.printStackTrace(e);
			throw new SapTestUtilRuntimeException("Error occured during InstrumentFixings: " + e.getMessage(), e);
		}
		finally
		{
			if (tblTemp != null)
			{
				tblTemp.destroy();
			}
		}
		
		return tblInputData;
	}

	@Override
	public String getOperationName() 
	{
		return "Bulk Instrument Fix";
	}
}
