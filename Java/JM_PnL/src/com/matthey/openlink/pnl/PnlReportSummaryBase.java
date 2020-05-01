/*
 * 
 */
package com.matthey.openlink.pnl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

public abstract class PnlReportSummaryBase extends PNL_ReportEngine
{
	protected void generateOutputTableFormat(Table output) throws OException
	{		
		output.addCol("type", COL_TYPE_ENUM.COL_STRING);
		output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		output.addCol("date", COL_TYPE_ENUM.COL_INT);

		output.addCol("opening_volume", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("opening_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("opening_price", COL_TYPE_ENUM.COL_DOUBLE);

		output.addCol("closing_volume", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("closing_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("closing_price", COL_TYPE_ENUM.COL_DOUBLE);

		output.addCol("trading_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("interest_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("total_funding_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("funding_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("funding_interest_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);

		output.addCol("trading_pnl_this_month", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("volume_buy_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("volume_sell_today", COL_TYPE_ENUM.COL_DOUBLE);
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);

		regDateConversion(output, "date");
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#populateOutputTable(com.olf.openjvs.Table)
	 */
	@Override
	protected void populateOutputTable(Table output) throws OException
	{
		int reportCloseDate = reportDate + 1;

		PluginLog.info("PNL_Report_Summary::populateOutputTable called.\n");
		OConsole.message("PNL_Report_Summary::populateOutputTable called.\n");

		Table openPosData = m_positionHistory.getOpenPositionsForDates(reportDate, reportCloseDate);
		Table dealDetailsData = m_positionHistory.getPositionData();

		for (int row = 1; row <= dealDetailsData.getNumRows(); row++)
		{
			Table deals = dealDetailsData.getTable("deals", row);

			output.addRow();
			int outRow = output.getNumRows();

			int bunit = dealDetailsData.getInt("bunit", row);
			int metalCcy = dealDetailsData.getInt("metal_ccy", row);
			String type = MTL_Position_Utilities.isPreciousMetal(metalCcy) ? "Metal" : "Currency";

			output.setInt("bunit", outRow, bunit);
			output.setInt("metal_ccy", outRow, metalCcy);
			output.setString("type", outRow, type);

			int reportSOM = OCalendar.getSOM(reportDate);

			double todayProfit = 0.0, thisMonthProfit = 0.0, todayBuyVolume = 0.0, todaySellVolume = 0.0;

			for (int dealsRow = 1; dealsRow <= deals.getNumRows(); dealsRow++)
			{
				int date = deals.getInt("date", dealsRow);
				double profit = deals.getDouble("profit", dealsRow);
				double volume = deals.getDouble("volume", dealsRow);

				if (date == reportDate)
				{
					todayProfit += profit;

					if (volume > 0.0)
					{
						todayBuyVolume += volume;
					}
					else
					{
						todaySellVolume += volume;
					}
				}
				if ((date <= reportDate) && (date >= reportSOM))
				{
					thisMonthProfit += profit;
				}
			}

			output.setDouble("trading_pnl_today", outRow, todayProfit);
			output.setDouble("trading_pnl_this_month", outRow, thisMonthProfit);
			output.setDouble("volume_buy_today", outRow, todayBuyVolume);
			output.setDouble("volume_sell_today", outRow, todaySellVolume);
		}		

		output.select(openPosData, "open_volume (opening_volume), open_value (opening_value), open_price (opening_price)", 
				"bunit EQ $bunit AND metal_ccy EQ $metal_ccy AND open_date EQ " + reportDate);
		output.select(openPosData, "open_volume (closing_volume), open_value (closing_value), open_price (closing_price)", 
				"bunit EQ $bunit AND metal_ccy EQ $metal_ccy AND open_date EQ " + reportCloseDate);


		Table interestData = m_interestPNLAggregator.getDataForGivenDate(reportDate);

		if ((interestData != null))
		{
			if (interestData.getNumRows() > 0)
			{
				output.select(interestData, "SUM, value (interest_pnl_today)", "int_bu EQ $bunit AND group EQ $metal_ccy");
			}			
			interestData.destroy();
		}

		Table fundingData = m_fundingPNLAggregator.getDataForGivenDate(reportDate);

		if ((fundingData != null))
		{
			if (fundingData.getNumRows() > 0)
			{
				output.select(fundingData, "SUM, value (funding_pnl_today)", "int_bu EQ $bunit AND group EQ $metal_ccy");
			}			
			fundingData.destroy();
		}	

		Table fundingInterestData = m_fundingInterestPNLAggregator.getDataForGivenDate(reportDate);

		if ((fundingInterestData != null))
		{
			if (fundingInterestData.getNumRows() > 0)
			{
				output.select(fundingInterestData, "SUM, value (funding_interest_pnl_today)", "int_bu EQ $bunit AND group EQ $metal_ccy");
			}			
			fundingInterestData.destroy();
		}			

		output.mathAddCol("funding_pnl_today", "funding_interest_pnl_today", "total_funding_pnl_today");
		output.setColValInt("date", reportDate);

		List<COG_PNL_Grouping> missingKeys = new ArrayList<COG_PNL_Grouping>();
		List<COG_PNL_Grouping> relevantMetalAndBunitList = m_positionHistory.inititalizeAllMetalAndBUnitList();

		if (m_positionHistory.getDealHistoryMap() != null  && !m_positionHistory.getDealHistoryMap().isEmpty()) {
			for (COG_PNL_Grouping key: relevantMetalAndBunitList) {
				if (m_positionHistory.getDealHistoryMap().get(key).isEmpty()) {
					missingKeys.add(key);
				}
			}
		}
		
		if (!missingKeys.isEmpty()) {
			PluginLog.info("Missing keys size :"+missingKeys.size());
			processTheMissingMetals(missingKeys, output);
		}
	}

	private void processTheMissingMetals (List<COG_PNL_Grouping> missingKeys,Table output)throws OException {
		Table openPositionResults = Util.NULL_TABLE;
		Table openTradingPosition =Util.NULL_TABLE;

		try {
			openTradingPosition = Table.tableNew();
			int extractId = 0;
			int extractDate = m_positionHistory.retreiveTheExtractDateFromOpenTradingPosition();
			int openDate = OCalendar.parseString("-2fom");
			int closeDate = openDate-1;
			openTradingPosition.setTableName(m_positionHistory.getPnlUserTableHandler().getOpenTradingPositionTableName());
			DBUserTable.structure(openTradingPosition);

			for (COG_PNL_Grouping missingKey : missingKeys) {
				Integer bUnit= missingKey.m_bunit;
				Integer metalCcy = missingKey.m_metalCcyGroup;
				int extractTime = ODateTime.getServerCurrentDateTime().getTime();

				openPositionResults = m_positionHistory.populateTheOutputFromOpenTradingPosition(bUnit, metalCcy);
				if (Table.isTableValid(openPositionResults)== 0 || openPositionResults.getNumRows() < 1 ) {
					continue;
				}

				Double openPrice= openPositionResults.getDouble("open_price", 1);
				Double openValue= openPositionResults.getDouble("open_value",1);
				Double openVolume= openPositionResults.getDouble("open_volume", 1);
				String type = MTL_Position_Utilities.isPreciousMetal(metalCcy) ? "Metal" : "Currency";
				if (Double.compare(openValue, BigDecimal.ZERO.doubleValue()) != 0 && Double.compare(openVolume, BigDecimal.ZERO.doubleValue()) != 0) {
					PluginLog.info("Adding the values in the output table for bunit :" + bUnit + " and metal :"+metalCcy);
					output.addRowsWithValues(""+"("+type+")"+","+bUnit+","+metalCcy+","+reportDate+","+openVolume+","+openValue+","+openPrice+","+openVolume+","+openValue+","+openPrice+","+0+","+0+","+0+","+0+","+0+","+0+","+0+","+0+","+"("+")"+","+"("+")"+","+"("+")"); 
				}

				openTradingPosition.addRow();
				// add the cols here 
				openTradingPosition.setColValInt("extract_id", extractId);
				openTradingPosition.setColValInt("extract_date", extractDate);
				openTradingPosition.setColValInt("extract_time",extractTime);
				openTradingPosition.setColValInt("bunit", bUnit);
				openTradingPosition.setColValInt("metal_ccy", metalCcy);
				openTradingPosition.setColValInt("open_date", openDate);
				openTradingPosition.setColValDouble("open_volume", openVolume);
				openTradingPosition.setColValDouble("open_price",openPrice);
				openTradingPosition.setColValDouble("open_value", openValue);
				openTradingPosition.setColValInt("close_date",closeDate);

                if (Table.isTableValid(openPositionResults)==1) {
                	openPositionResults.destroy();
    				openPositionResults = Util.NULL_TABLE;
                }
			}
			
            PluginLog.info("Inserting the rows for missing metal in open trading position table !!!!");
			DBUserTable.insert(openTradingPosition);

		} catch(Exception e) {
			PluginLog.error("Failed to fetch the data from open trading position table and insert new entry into it !!!" + e.getMessage());
		} finally{ 
			if (Table.isTableValid(openPositionResults) == 1) {
				openPositionResults.destroy();
			}
			
			if (Table.isTableValid(openTradingPosition) == 1) {
				openTradingPosition.destroy();
			}
		}
	}
}