package com.matthey.openlink.pnl;

import java.util.Collection;
import java.util.Vector;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public interface IPnlUserTableHandler {

	public String getMarketDataTableName();
	
	public void recordMarketData(
			Vector<PNL_MarketDataEntry> dataEntries) throws OException;

	/**
	 * For a given set of unique keys (deal-leg-pdc-reset), delete all entries for these
	 * @param dataKeys
	 * @throws OException
	 */
	public void deleteMarketData(
			Collection<PNL_EntryDataUniqueID> dataKeys) throws OException;

	/**
	 * Retrieves market data from USER_jm_pnl_market_data for a single deal 
	 * @param dealNum - deal to use
	 * @return
	 * @throws OException
	 */
	public Vector<PNL_MarketDataEntry> retrieveMarketData(int dealNum)
			throws OException;

	/**
	 * Retrieves market data from USER_jm_pnl_market_data for all deals in input table
	 * @param dealData - table of deal numbers, column name "deal_num" is used
	 * @return
	 * @throws OException
	 */
	public Vector<PNL_MarketDataEntry> retrieveMarketData(
			Table dealData) throws OException;

	/**
	 * Retrieves market data from USER_jm_pnl_market_data for all deals in input table
	 * @param dealData - table of deal numbers, column name as per dealNumColName is used
	 * @param dealNumColName - column name that stores deal numbers
	 * @return
	 * @throws OException
	 */
	public Vector<PNL_MarketDataEntry> retrieveMarketData(
			Table dealData, String dealNumColName) throws OException;

	public void recordTradingPositionHistory(
			COGPnlTradingPositionHistoryBase m_positionHistory) throws OException;

	public Table retrieveTradingPositionHistory(int startDate,
			int endDate) throws OException;

	public void recordOpenTradingPositions(
			COGPnlTradingPositionHistoryBase m_positionHistory, int firstOpeningDate,
			int lastOpeningDate) throws OException;

	public Table retrieveOpenTradingPositions(int date)
			throws OException;

	public int retrieveRegenerateDate() throws OException;

	public void setRegenerateDate(int date) throws OException;

}