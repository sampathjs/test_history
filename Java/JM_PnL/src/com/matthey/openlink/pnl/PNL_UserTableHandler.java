package com.matthey.openlink.pnl;

import java.util.Collection;
import java.util.Vector;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.Debug;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.UTIL_DEBUG_TYPE;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-MM-DD	V1.0	mstseglov	- Initial Version
 * 2017-01-25	V1.1	jwaechter	- Added PluginLog
 *                                  - fixed case of user table names
 */

/**
 * Class managing access to the user tables relevant to JMs business 
 * @author mstseglov
 * @version 1.1
 */
public class PNL_UserTableHandler 
{
	public static void recordMarketData(Vector<PNL_MarketDataEntry> dataEntries) throws OException
	{		
		Table data = new Table("USER_jm_pnl_market_data");
		Table deleteData = new Table("USER_jm_pnl_market_data");
		
		data.addCol("entry_date", COL_TYPE_ENUM.COL_INT);
		data.addCol("entry_time", COL_TYPE_ENUM.COL_INT);
		
		data.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		data.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		data.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		data.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
		
		data.addCol("trade_date", COL_TYPE_ENUM.COL_INT);
		
		data.addCol("fixing_date", COL_TYPE_ENUM.COL_INT);
		data.addCol("index_id", COL_TYPE_ENUM.COL_INT);
		data.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		
		data.addCol("spot_rate", COL_TYPE_ENUM.COL_DOUBLE);
		data.addCol("fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
		data.addCol("usd_df", COL_TYPE_ENUM.COL_DOUBLE);
		
		ODateTime dt = ODateTime.getServerCurrentDateTime();		
		
		for (PNL_MarketDataEntry entry : dataEntries)
		{
			int row = data.addRow();
			
			data.setInt("entry_date", row, dt.getDate());
			data.setInt("entry_time", row, dt.getTime());			
			
			data.setInt("deal_num", row, entry.m_uniqueID.m_dealNum);
			data.setInt("deal_leg", row, entry.m_uniqueID.m_dealLeg);
			data.setInt("deal_pdc", row, entry.m_uniqueID.m_dealPdc);
			data.setInt("deal_reset_id", row, entry.m_uniqueID.m_dealReset);
			
			data.setInt("trade_date", row, entry.m_tradeDate);
			
			data.setInt("fixing_date", row, entry.m_fixingDate);
			data.setInt("index_id", row, entry.m_indexID);
			data.setInt("metal_ccy", row, entry.m_metalCcy);
			
			data.setDouble("spot_rate", row, entry.m_spotRate);
			data.setDouble("fwd_rate", row, entry.m_forwardRate);
			data.setDouble("usd_df", row, entry.m_usdDF);
		}		
		
		deleteData.select(data, "deal_num, deal_leg, deal_pdc, deal_reset_id" , "deal_num GE 0");
		
		PluginLog.info("PNL_UserTableHandler::recordMarketData will use dataset of size: " + data.getNumRows() + "\n");
		OConsole.message("PNL_UserTableHandler::recordMarketData will use dataset of size: " + data.getNumRows() + "\n");
		
		int retVal = -1;
		
		retVal = DBUserTable.delete(deleteData);
		if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			PluginLog.info("PNL_UserTableHandler::recordMarketData DBUserTable.delete succeeded.\n");
			OConsole.message("PNL_UserTableHandler::recordMarketData DBUserTable.delete succeeded.\n");
		}
		else
		{
			PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
			OConsole.message(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
			
			// Try one more time, after sleeping for 1 second
			try
			{
				Thread.sleep(1000);
			}
			catch (Exception e)
			{				
			}
			
			retVal = DBUserTable.delete(deleteData);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				PluginLog.info("PNL_UserTableHandler::recordMarketData secondary DBUserTable.delete succeeded.\n");
				OConsole.message("PNL_UserTableHandler::recordMarketData secondary DBUserTable.delete succeeded.\n");
			}
			else
			{
				PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
				OConsole.message(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler secondary DBUserTable.delete failed") + "\n");
			}
		}
		
		retVal = DBUserTable.insert(data);
		if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			PluginLog.info("PNL_UserTableHandler::recordMarketData DBUserTable.insert succeeded.\n");
			OConsole.message("PNL_UserTableHandler::recordMarketData DBUserTable.insert succeeded.\n");
		}
		else
		{
			PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.insert failed") + "\n");
			OConsole.message(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.insert failed") + "\n");
			
			// Try one more time, after sleeping for 1 second
			try
			{
				Thread.sleep(1000);
			}
			catch (Exception e)
			{				
			}
			
			retVal = DBUserTable.insert(data);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				PluginLog.info("PNL_UserTableHandler::recordMarketData secondary DBUserTable.insert succeeded.\n");
				OConsole.message("PNL_UserTableHandler::recordMarketData secondary DBUserTable.insert succeeded.\n");
			}
			else
			{
				PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler secondary DBUserTable.insert failed") + "\n");
				OConsole.message(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler secondary DBUserTable.insert failed") + "\n");
			}
		}		
	}

	/**
	 * For a given set of unique keys (deal-leg-pdc-reset), delete all entries for these
	 * @param dataKeys
	 * @throws OException
	 */
	public static void deleteMarketData(Collection<PNL_EntryDataUniqueID> dataKeys) throws OException
	{		
		Table deleteData = new Table("USER_jm_pnl_market_data");
			
		deleteData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		deleteData.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		deleteData.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		deleteData.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
				
		for (PNL_EntryDataUniqueID key : dataKeys)
		{
			int row = deleteData.addRow();
			
			deleteData.setInt("deal_num", row, key.m_dealNum);
			deleteData.setInt("deal_leg", row, key.m_dealLeg);
			deleteData.setInt("deal_pdc", row, key.m_dealPdc);
			deleteData.setInt("deal_reset_id", row, key.m_dealReset);
		}		
				
		PluginLog.info("PNL_UserTableHandler::deleteMarketData will use dataset of size: " + deleteData.getNumRows() + "\n");
		OConsole.message("PNL_UserTableHandler::deleteMarketData will use dataset of size: " + deleteData.getNumRows() + "\n");
		
		int retVal = -1;
		
		retVal = DBUserTable.delete(deleteData);
		if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			PluginLog.info("PNL_UserTableHandler::deleteMarketData DBUserTable.delete succeeded.\n");
			OConsole.message("PNL_UserTableHandler::deleteMarketData DBUserTable.delete succeeded.\n");
		}
		else
		{
			PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
			OConsole.message(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
			
			// Try one more time, after sleeping for 1 second
			try
			{
				Thread.sleep(1000);
			}
			catch (Exception e)
			{				
			}
			
			retVal = DBUserTable.delete(deleteData);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				PluginLog.info("PNL_UserTableHandler::deleteMarketData secondary DBUserTable.delete succeeded.\n");
				OConsole.message("PNL_UserTableHandler::deleteMarketData secondary DBUserTable.delete succeeded.\n");
			}
			else
			{
				PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
				OConsole.message(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler secondary DBUserTable.delete failed") + "\n");
			}
		}	
	}
	
	
	/**
	 * Retrieves market data from USER_jm_pnl_market_data for a single deal 
	 * @param dealNum - deal to use
	 * @return
	 * @throws OException
	 */
	public static Vector<PNL_MarketDataEntry> retrieveMarketData(int dealNum) throws OException
	{		
		String sqlQuery = "SELECT * from USER_jm_pnl_market_data where deal_num = " + dealNum + " order by deal_leg, deal_pdc, deal_reset_id";
		
		Table data = new Table("");
		DBase.runSqlFillTable(sqlQuery, data);
		
		Vector<PNL_MarketDataEntry> dataEntries = populateMarketDataFromTable(data);
		
		data.destroy();
		
		return dataEntries;
	}
	
	/**
	 * Retrieves market data from USER_jm_pnl_market_data for all deals in input table
	 * @param dealData - table of deal numbers, column name "deal_num" is used
	 * @return
	 * @throws OException
	 */
	public static Vector<PNL_MarketDataEntry> retrieveMarketData(Table dealData) throws OException
	{
		return retrieveMarketData(dealData, "deal_num");
	}
	
	/**
	 * Retrieves market data from USER_jm_pnl_market_data for all deals in input table
	 * @param dealData - table of deal numbers, column name as per dealNumColName is used
	 * @param dealNumColName - column name that stores deal numbers
	 * @return
	 * @throws OException
	 */
	public static Vector<PNL_MarketDataEntry> retrieveMarketData(Table dealData, String dealNumColName) throws OException
	{		
		int queryID = Query.tableQueryInsert(dealData, dealNumColName);
		
		String sqlQuery = 
				"SELECT ujpm.* from USER_jm_pnl_market_data ujpm, query_result qr " +
				"where qr.query_result = ujpm.deal_num and qr.unique_id = " + queryID + " " + 
				"order by deal_leg, deal_pdc, deal_reset_id";
		
		Table data = new Table("");
		DBase.runSqlFillTable(sqlQuery, data);
		Query.clear(queryID);
		
		Vector<PNL_MarketDataEntry> dataEntries = populateMarketDataFromTable(data);
		
		data.destroy();
		
		return dataEntries;
	}		
	
	/**
	 * Given a table of data from USER_jm_pnl_market_data, converts to PNL_MarketDataEntry structures
	 * @param data - table of data in same format as USER_jm_pnl_market_data
	 * @return
	 * @throws OException
	 */
	private static Vector<PNL_MarketDataEntry> populateMarketDataFromTable(Table data) throws OException
	{
		Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
		
		for (int row = 1; row <= data.getNumRows(); row++)
		{
			PNL_MarketDataEntry entry = new PNL_MarketDataEntry();
			
			entry.m_uniqueID = new PNL_EntryDataUniqueID(
					data.getInt("deal_num", row), data.getInt("deal_leg", row), data.getInt("deal_pdc", row), data.getInt("deal_reset_id", row));
			
			entry.m_tradeDate = data.getInt("trade_date", row);
			
			entry.m_fixingDate = data.getInt("fixing_date", row);
			entry.m_indexID = data.getInt("index_id", row);
			entry.m_metalCcy = data.getInt("metal_ccy", row);
			
			entry.m_spotRate = data.getDouble("spot_rate", row);
			entry.m_forwardRate = data.getDouble("fwd_rate", row);
			entry.m_usdDF = data.getDouble("usd_df", row);
			
			dataEntries.add(entry);
		}
		
		return dataEntries;		
	}
	
	public static void recordTradingPositionHistory(COG_PNL_Trading_Position_History input) throws OException
	{
		Table data = input.getPositionData();
		
		Table dbData = null;
		
		ODateTime dt = ODateTime.getServerCurrentDateTime();
		
		for (int row = 1; row <= data.getNumRows(); row++)
		{
			Table details = data.getTable("trading_pos", row);
			
			if (Table.isTableValid(details) == 1)
			{
				details.setColValInt("extract_date", dt.getDate());
				details.setColValInt("extract_time", dt.getTime());
				
				details.setColValInt("bunit", data.getInt("bunit", row));
				details.setColValInt("metal_ccy", data.getInt("metal_ccy", row));
				
				if (dbData == null)
				{
					dbData = details.cloneTable();
				}
				
				details.copyRowAddAll(dbData);
			}			
		}
		
		if (dbData != null)
		{
			//dbData.setTableName("USER_JM_Trading_PNL_History");
			dbData.setTableName("USER_jm_trading_pnl_history");
			dbData.setColFormatNone("buy_sell");
			
			DBUserTable.clear(dbData);
			DBUserTable.insert(dbData);
			
			dbData.destroy();
		}		
	}

	public static Table retrieveTradingPositionHistory(int startDate, int endDate) throws OException
	{
		String sqlQuery = "SELECT * from USER_jm_trading_pnl_history where deal_date >= " + startDate + " AND deal_date <= " + endDate;
	
		OConsole.message("retrieveTradingPositionHistory: " + sqlQuery + "\n");
		
		Table results = new Table("USER_jm_trading_pnl_history for: " + OCalendar.formatDateInt(startDate)  + " - " + OCalendar.formatDateInt(endDate));
		DBase.runSqlFillTable(sqlQuery, results);		

		return results;
	}
	
	public static void recordOpenTradingPositions(COG_PNL_Trading_Position_History input, int firstOpeningDate, int lastOpeningDate) throws OException
	{
		Table data = input.getOpenPositionsForDates(firstOpeningDate, lastOpeningDate);
		
		ODateTime dt = ODateTime.getServerCurrentDateTime();
		
		data.setColValInt("extract_date", dt.getDate());
		data.setColValInt("extract_time", dt.getTime());		

		data.setTableName("USER_jm_open_trading_position");	
		
		// data.viewTable();
		
		DBUserTable.insert(data);		
	}

	public static Table retrieveOpenTradingPositions(int date) throws OException
	{
		String sqlQuery = "SELECT * from USER_jm_open_trading_position where open_date = " + date;
		
		Table results = new Table("");
		DBase.runSqlFillTable(sqlQuery, results);
		
		results.group("bunit, metal_ccy, extract_id, extract_date, extract_time");
		
		for (int i = results.getNumRows(); i >= 2; i--)
		{
			boolean doesMatchPriorBU = (results.getInt("bunit", i) == results.getInt("bunit", i-1));
			boolean doesMatchPriorGroup = (results.getInt("metal_ccy", i) == results.getInt("metal_ccy", i-1));
			
			// If the two rows match, delete the earlier one from output
			if (doesMatchPriorBU && doesMatchPriorGroup)
			{
				results.delRow(i-1);
				i++;
			}
		}
		
		return results;
	}

	public static int retrieveRegenerateDate() throws OException
	{
		int regenerateDate = -1;
		
		String sqlQuery = "SELECT * from USER_jm_regen_pnl_data";
		
		Table results = new Table("USER_jm_regen_pnl_data");
		DBase.runSqlFillTable(sqlQuery, results);

		if ((Table.isTableValid(results) == 1) && (results.getNumRows() > 0))
		{
			regenerateDate = results.getInt(1, 1);
		}
		
		return regenerateDate;
	}
	
	public static void setRegenerateDate(int date) throws OException
	{
		Table data = new Table("USER_jm_regen_pnl_data");
		
		data.addCol("regenerate_date", COL_TYPE_ENUM.COL_INT);
		data.addRow();
		
		data.setInt(1, 1, date);
		
		DBUserTable.clear(data);
		DBUserTable.insert(data);
	}

}
