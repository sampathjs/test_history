package com.matthey.openlink.jde_extract;

import com.matthey.openlink.pnl.ConfigurationItemPnl;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-07-11	V1.0	mstseglov	- Initial Version
 */

/**
 * This class manages access to the user tables relevant to JM's JDE Extract
 * @author mstseglov
 * @version 1.0
 */
public class JDE_UserTableHandler 
{
	/**
	 * Records the deal data in USER_jm_jde_deal_data, removing any prior entry for the deal in question
	 * @param input
	 * @throws OException
	 */
	public static void recordDealData(Table input) throws OException
	{	
		try
		{
			Table data = new Table(JDE_Extract_Common.S_JDE_STAGING_AREA_TABLE);
			Table deleteData = new Table(JDE_Extract_Common.S_JDE_STAGING_AREA_TABLE);
			
			// Set the output data table format and contents from input
			setOutputFormat(data);		
			input.copyRowAddAllByColName(data);
			
			// Add the current date + time
			ODateTime dt = ODateTime.getServerCurrentDateTime();
			data.setColValInt("entry_date", dt.getDate());
			data.setColValInt("entry_time", dt.getTime());			
			
			// Data to be deleted is keyed by deal number
			deleteData.select(data, "deal_num", "deal_num GT 0");
			
			// Delete old data
			doDelete(deleteData);
			
			// Insert new data
			doInsert(data);			
		}
		catch (Exception e)
		{			
			PluginLog.info(e.getMessage());
			OConsole.message(e.getMessage());			
		}
	}
	
	/**
	 * Runs the "insert to user table" operation, retrying once in case of transient DB errors
	 * @param data - data to insert
	 * @throws OException
	 */
	private static void doInsert(Table data) throws OException
	{
		String message;		
		int retVal = -1;
				
		message = "JDE_UserTableHandler::doInsert will use dataset of size: " + data.getNumRows() + "\n";
		PluginLog.info(message);
		OConsole.message(message);		
		
		retVal = DBUserTable.insert(data);
		
		if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			message = "JDE_UserTableHandler::doInsert DBUserTable.insert succeeded.\n";
			PluginLog.info(message);
			OConsole.message(message);
		}
		else
		{
			message = DBUserTable.dbRetrieveErrorInfo(retVal, "JDE_UserTableHandler::doInsert DBUserTable.insert failed") + "\n";
			PluginLog.info(message);
			OConsole.message(message);
			
			// Try one more time, after sleeping for 1 second
			try
			{
				Thread.sleep(1000);
			}
			catch (Exception e)
			{				
				PluginLog.info(e.getMessage());							
			}
			
			retVal = DBUserTable.insert(data);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				message = "JDE_UserTableHandler::doInsert secondary DBUserTable.insert succeeded.\n";
			}
			else
			{
				message = DBUserTable.dbRetrieveErrorInfo(retVal, "JDE_UserTableHandler::doInsert secondary DBUserTable.insert failed") + "\n";
			}
			PluginLog.info(message);
			OConsole.message(message);			
		}			
	}

	/**
	 * Runs the "delete from user table" operation, retrying once in case of transient DB errors
	 * @param deleteData
	 * @throws OException
	 */
	private static void doDelete(Table deleteData) throws OException
	{
		String message;
		
		message = "JDE_UserTableHandler::doDelete will use dataset of size: " + deleteData.getNumRows() + "\n";
		PluginLog.info(message);
		OConsole.message(message);
		
		int retVal = -1;
		
		retVal = DBUserTable.delete(deleteData);
		if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			message = "JDE_UserTableHandler::doDelete DBUserTable.delete succeeded.\n";
			PluginLog.info(message);
			OConsole.message(message);
		}
		else
		{
			message = DBUserTable.dbRetrieveErrorInfo(retVal, "JDE_UserTableHandler::doDelete DBUserTable.delete failed") + "\n";
			PluginLog.info(message);
			OConsole.message(message);
			
			// Try one more time, after sleeping for 1 second
			try
			{
				Thread.sleep(1000);
			}
			catch (Exception e)
			{	
				PluginLog.info(e.getMessage());
			}
			
			retVal = DBUserTable.delete(deleteData);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				message = "JDE_UserTableHandler::doDelete secondary DBUserTable.delete succeeded.\n";
			}
			else
			{
				message = DBUserTable.dbRetrieveErrorInfo(retVal, "JDE_UserTableHandler::doDelete DBUserTable.delete failed") + "\n";
			}
			PluginLog.info(message);
			OConsole.message(message);			
		}		
	}
	
	/**
	 * Sets the expected output format that matches the USER_JM_JDE_Extract_Data USER table
	 * @param workData
	 * @throws OException
	 */
	public static void setOutputFormat(Table workData) throws OException
	{
		workData.addCol("entry_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("entry_time", COL_TYPE_ENUM.COL_INT);
		
		workData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("fixings_complete", COL_TYPE_ENUM.COL_STRING);

		workData.addCol("from_currency", COL_TYPE_ENUM.COL_INT);
		workData.addCol("to_currency", COL_TYPE_ENUM.COL_INT);

		workData.addCol("delivery_date", COL_TYPE_ENUM.COL_INT);

		workData.addCol("metal_volume_uom", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("settlement_value", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("spot_equiv_value", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("interest", COL_TYPE_ENUM.COL_DOUBLE);
				
		workData.addCol("uom", COL_TYPE_ENUM.COL_INT);		
		workData.addCol("metal_volume_toz", COL_TYPE_ENUM.COL_DOUBLE);		
		
		workData.addCol("trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("spot_equiv_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("conv_factor", COL_TYPE_ENUM.COL_DOUBLE);
		
		workData.addCol("fx_fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
	}
}
