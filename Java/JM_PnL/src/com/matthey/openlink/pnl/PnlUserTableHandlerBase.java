package com.matthey.openlink.pnl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Vector;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 * 2020-06-05   V1.2    jainv02  - Add support for Implied EFP calculation for Comfut (EPI-1254)
 */

public abstract class PnlUserTableHandlerBase implements IPnlUserTableHandler {
	
	public abstract String getMarketDataTableName();
	
	public abstract String getOpenTradingPositionTableName();

	public abstract String getTradingPnlHistoryTableName();
	
	public String getImpliedEFPTableName() {
		return "USER_jm_implied_efp";
	}
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#recordMarketData(java.util.Vector)
	 */
	@Override
	public void recordMarketData(Vector<PNL_MarketDataEntry> dataEntries) throws OException {		
		Table data = new Table(getMarketDataTableName());
		Table deleteData = new Table(getMarketDataTableName());
		
		try {
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
			
			for (PNL_MarketDataEntry entry : dataEntries) {
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
			int retVal = -1;
			
			retVal = DBUserTable.delete(deleteData);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.info("PNL_UserTableHandler::recordMarketData DBUserTable.delete succeeded.\n");
			} else {
				PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
				
				// Try one more time, after sleeping for 1 second
				try {
					Thread.sleep(1000);
				} catch (Exception e) {				
				}
				
				retVal = DBUserTable.delete(deleteData);
				if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.info("PNL_UserTableHandler::recordMarketData secondary DBUserTable.delete succeeded.\n");
				} else {
					PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
				}
			}
			
			retVal = DBUserTable.insert(data);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.info("PNL_UserTableHandler::recordMarketData DBUserTable.insert succeeded.\n");
			} else {
				PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.insert failed") + "\n");
				
				// Try one more time, after sleeping for 1 second
				try {
					Thread.sleep(1000);
				} catch (Exception e) {				
				}
				
				retVal = DBUserTable.insert(data);
				if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.info("PNL_UserTableHandler::recordMarketData secondary DBUserTable.insert succeeded.\n");
				} else {
					PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler secondary DBUserTable.insert failed") + "\n");
				}
			}
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
			if (Table.isTableValid(deleteData) == 1) {
				deleteData.destroy();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#deleteMarketData(java.util.Collection)
	 */
	@Override
	public void deleteMarketData(Collection<PNL_EntryDataUniqueID> dataKeys) throws OException {
		Table deleteData = new Table(getMarketDataTableName());
		
		try {
			deleteData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			deleteData.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
			deleteData.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
			deleteData.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
					
			for (PNL_EntryDataUniqueID key : dataKeys) {
				int row = deleteData.addRow();
				
				deleteData.setInt("deal_num", row, key.m_dealNum);
				deleteData.setInt("deal_leg", row, key.m_dealLeg);
				deleteData.setInt("deal_pdc", row, key.m_dealPdc);
				deleteData.setInt("deal_reset_id", row, key.m_dealReset);
			}		
					
			PluginLog.info("PNL_UserTableHandler::deleteMarketData will use dataset of size: " + deleteData.getNumRows() + "\n");
			int retVal = -1;
			
			retVal = DBUserTable.delete(deleteData);
			if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.info("PNL_UserTableHandler::deleteMarketData DBUserTable.delete succeeded.\n");
			} else {
				PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
				
				// Try one more time, after sleeping for 1 second
				try {
					Thread.sleep(1000);
				} catch (Exception e) {				
				}
				
				retVal = DBUserTable.delete(deleteData);
				if (retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.info("PNL_UserTableHandler::deleteMarketData secondary DBUserTable.delete succeeded.\n");
				} else {
					PluginLog.info(DBUserTable.dbRetrieveErrorInfo(retVal, "PNL_UserTableHandler DBUserTable.delete failed") + "\n");
				}
			}
		} finally {
			if (Table.isTableValid(deleteData) == 1) {
				deleteData.destroy();
			}
		}
	}
	
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#retrieveMarketData(int)
	 */
	@Override
	public Vector<PNL_MarketDataEntry> retrieveMarketData(int dealNum) throws OException {		
		String sqlQuery = "SELECT * FROM " + getMarketDataTableName() + " WHERE deal_num = " + dealNum + " ORDER BY deal_leg, deal_pdc, deal_reset_id";
		Table data = new Table("");
		
		try {
			DBase.runSqlFillTable(sqlQuery, data);
			Vector<PNL_MarketDataEntry> dataEntries = populateMarketDataFromTable(data);
			return dataEntries;
			
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();	
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#retrieveMarketData(com.olf.openjvs.Table)
	 */
	@Override
	public Vector<PNL_MarketDataEntry> retrieveMarketData(Table dealData) throws OException {
		return retrieveMarketData(dealData, "deal_num");
	}
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#retrieveMarketData(com.olf.openjvs.Table, java.lang.String)
	 */
	@Override
	public Vector<PNL_MarketDataEntry> retrieveMarketData(Table dealData, String dealNumColName) throws OException {		
		int queryID = Query.tableQueryInsert(dealData, dealNumColName);
		String sqlQuery = 
				"SELECT ujpm.* FROM " + getMarketDataTableName() + " ujpm, query_result qr " +
				"WHERE qr.query_result = ujpm.deal_num AND qr.unique_id = " + queryID + " " + 
				"ORDER BY deal_leg, deal_pdc, deal_reset_id";
		Table data = new Table("");
		
		try {
			DBase.runSqlFillTable(sqlQuery, data);
			Vector<PNL_MarketDataEntry> dataEntries = populateMarketDataFromTable(data);
			return dataEntries;
			
		} finally {
			if (queryID > 0) {
				Query.clear(queryID);	
			}
			if (Table.isTableValid(data) == 1) {
				data.destroy();	
			}
		}
	}		
	
	/**
	 * Given a table of data from USER_jm_pnl_market_data, converts to PNL_MarketDataEntry structures
	 * @param data - table of data in same format as USER_jm_pnl_market_data
	 * @return
	 * @throws OException
	 */
	private Vector<PNL_MarketDataEntry> populateMarketDataFromTable(Table data) throws OException {
		Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
		int rows = data.getNumRows();
		
		for (int row = 1; row <= rows; row++) {
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
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#recordTradingPositionHistory(com.matthey.openlink.pnl.COG_PNL_Trading_Position_History)
	 */
	@Override
	public void recordTradingPositionHistory(COGPnlTradingPositionHistoryBase input) throws OException {
		Table data = input.getPositionData();
		Table dbData = null;
		ODateTime dt = ODateTime.getServerCurrentDateTime();
		
		try {
			int rows = data.getNumRows();
			for (int row = 1; row <= rows; row++) {
				Table details = data.getTable("trading_pos", row);
				
				if (Table.isTableValid(details) == 1) {
					details.setColValInt("extract_date", dt.getDate());
					details.setColValInt("extract_time", dt.getTime());
					
					details.setColValInt("bunit", data.getInt("bunit", row));
					details.setColValInt("metal_ccy", data.getInt("metal_ccy", row));
					
					if (dbData == null) {
						dbData = details.cloneTable();
					}
					details.copyRowAddAll(dbData);
				}			
			}
			
			if (dbData != null) {
				//dbData.setTableName("USER_JM_Trading_PNL_History");
				dbData.setTableName(getTradingPnlHistoryTableName()); 
				dbData.setColFormatNone("buy_sell");
				
				DBUserTable.clear(dbData);
				DBUserTable.insert(dbData);
			}
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
			if (Table.isTableValid(dbData) == 1) {
				dbData.destroy();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#retrieveTradingPositionHistory(int, int)
	 */
	@Override
	public Table retrieveTradingPositionHistory(int startDate, int endDate) throws OException {
		// TODO check this table
		String sqlQuery = "SELECT * FROM " + getTradingPnlHistoryTableName() + " WHERE deal_date >= " + startDate + " AND deal_date <= " + endDate;
		Table results = new Table(getTradingPnlHistoryTableName() + " for: " + OCalendar.formatDateInt(startDate)  + " - " + OCalendar.formatDateInt(endDate));
		DBase.runSqlFillTable(sqlQuery, results);		

		return results;
	}
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#recordOpenTradingPositions(com.matthey.openlink.pnl.COG_PNL_Trading_Position_History, int, int)
	 */
	@Override
	public void recordOpenTradingPositions(COGPnlTradingPositionHistoryBase input, int firstOpeningDate, int lastOpeningDate) throws OException {
		Table data = input.getOpenPositionsForDates(firstOpeningDate, lastOpeningDate);
		try {
			ODateTime dt = ODateTime.getServerCurrentDateTime();
			
			data.setColValInt("extract_date", dt.getDate());
			data.setColValInt("extract_time", dt.getTime());		
			data.setTableName(getOpenTradingPositionTableName());	
			
			DBUserTable.insert(data);
			
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
		}
	}

	public  Table retreiveDataFromOpenTradingPositions(int bUnit, int metalCcy) throws OException {
		int todayDate = OCalendar.today();
		String resultQuery = "SELECT TOP 1 open_price,open_volume,open_value "
				+ " FROM " + getOpenTradingPositionTableName() 
				+ " WHERE bunit=" + bUnit + " and metal_ccy=" + metalCcy + " and extract_date < "+ todayDate 
				+ " ORDER BY extract_date desc, extract_time desc, open_date DESC ";
		Table results = new Table("");
		DBase.runSqlFillTable(resultQuery, results);
		return results;
	}
	
	public int retriveExtractDate()throws Exception {
		int todayDate = OCalendar.today();
		int extractDate = 0;
		Table results = Util.NULL_TABLE;
		try {
			results = Table.tableNew();
			String resultQuery = "SELECT max(extract_date) as extract_date "
					+ " FROM " + getOpenTradingPositionTableName() 
					+ " WHERE extract_date < " + todayDate + "";
			
			DBase.runSqlFillTable(resultQuery, results);
			if ((Table.isTableValid(results)==1)&& results.getNumRows() >0) {
				extractDate = results.getInt("extract_date", 1);
			}
		} finally {
			if (Table.isTableValid(results) == 1) {
				results.destroy();
			}
		}
		return extractDate;
	} 
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#retrieveOpenTradingPositions(int)
	 */
	public Table retrieveOpenTradingPositions(int date) throws OException {
		
		String sqlQuery = "SELECT *, 0 delete_me FROM " + getOpenTradingPositionTableName() 
				+ " WHERE open_date = " + date + "\n" 
				+ " ORDER BY bunit, metal_ccy, extract_id, extract_date, extract_time";
		
		Table results = new Table("");
		DBase.runSqlFillTable(sqlQuery, results);
		
		//results.group("bunit, metal_ccy, extract_id, extract_date, extract_time");
		int rowCount = results.getNumRows();
		if (rowCount>0){
			
			PluginLog.info("PNLUserTableHandlerBase::retrieveOpenTradingPositions before iteration size: " + rowCount);
			int currentBU = results.getInt("bunit", rowCount);
			int currentMetalCCY = results.getInt("metal_ccy", rowCount);
			int priorBU = 0;
			int priorMetalCCY = 0;
	
			for (int i = rowCount; i >= 2; i--){
				priorBU = results.getInt("bunit", i-1);
				priorMetalCCY = results.getInt("metal_ccy", i-1);
	
				boolean doesMatchPriorBU = (currentBU == priorBU);
				boolean doesMatchPriorGroup = (currentMetalCCY == priorMetalCCY);
				
				// If the two rows match, delete the earlier one from output
				if (doesMatchPriorBU && doesMatchPriorGroup) {
					results.setInt("delete_me", i-1, 1);
				}
				currentBU = priorBU;
				currentMetalCCY = priorMetalCCY;
			}
			results.deleteWhereValue("delete_me" , 1);
			PluginLog.info("PNLUserTableHandlerBase::retrieveOpenTradingPositions before iteration size: " + results.getNumRows());
		}
		results.delCol("delete_me");
		results.group("bunit, metal_ccy, extract_id, extract_date, extract_time");
		return results;
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#retrieveRegenerateDate()
	 */
	@Override
	public int retrieveRegenerateDate() throws OException {
		int regenerateDate = -1;
		String sqlQuery = "SELECT * FROM USER_jm_regen_pnl_data";
		Table results = new Table("USER_jm_regen_pnl_data");
		
		try {
			DBase.runSqlFillTable(sqlQuery, results);
			if ((Table.isTableValid(results) == 1) && (results.getNumRows() > 0)) {
				regenerateDate = results.getInt(1, 1);
			}
			return regenerateDate;
			
		} finally {
			if (Table.isTableValid(results) == 1) {
				results.destroy();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#setRegenerateDate(int)
	 */
	@Override
	public void setRegenerateDate(int date) throws OException {
		Table data = new Table("USER_jm_regen_pnl_data");
		try {
			data.addCol("regenerate_date", COL_TYPE_ENUM.COL_INT);
			data.addRow();
			
			data.setInt(1, 1, date);
			
			DBUserTable.clear(data);
			DBUserTable.insert(data);
			
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.IPnlUserTableHandler#saveImpliedEFP(int, double)
	 */
	@Override
	public void saveImpliedEFP(int dealNum, double impliedEFP)
			throws OException {
		Table userTable = Util.NULL_TABLE;
		boolean existing = false;

		try {
			double impliedEFPExisting = getExistingImpliedEFP(dealNum);
			if(Math.abs(impliedEFPExisting) > BigDecimal.ZERO.doubleValue()){
				existing = true;
			}

			if( existing && (Double.compare(Math.abs(impliedEFPExisting - impliedEFP),BigDecimal.ZERO.doubleValue()
					) == 0)){
				PluginLog.info("EFP already exists in user table and is same as the modified one. Skipping saving the EFP: Deal Num: " + dealNum + " ; EFP: " + impliedEFP + "\n");
				return;
			}

			PluginLog.info("Saving EFP: Deal Num: " + dealNum + " ; EFP: " + impliedEFP + "\n");
			userTable = new Table(getImpliedEFPTableName());

			userTable.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			userTable.addCol("implied_efp", COL_TYPE_ENUM.COL_DOUBLE);
			userTable.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);
			userTable.addRow();

			userTable.setInt("deal_num",1, dealNum);
			userTable.setDouble("implied_efp",1, impliedEFP);
			userTable.setDateTime("last_update",1, ODateTime.getServerCurrentDateTime());
			userTable.group("deal_num");

			if(existing){
				DBUserTable.update(userTable);
			} else{
				DBUserTable.insert(userTable);
			}
			
			PluginLog.info("Saved EFP: Deal Num: " + dealNum + " ; EFP: " + impliedEFP + "\n");
		}
		finally{
			if(Table.isTableValid(userTable) == 1){
				userTable.destroy();
			}
		}
	}

	public double getExistingImpliedEFP(int dealNum) throws OException {
		Table result = Util.NULL_TABLE;
		double impliedEFP = BigDecimal.ZERO.doubleValue();

		try {
			result = Table.tableNew();
			String sql = "SELECT implied_efp from " + getImpliedEFPTableName() +" WHERE deal_num = " + dealNum;

			DBaseTable.execISql(result, sql);
			if (result.getNumRows() == 1) {
				impliedEFP = result.getDouble(1, 1);
			}

			return impliedEFP;

		} finally {
			if (Table.isTableValid(result) == 1) {
				result.destroy();
			}
		}
	}

}
