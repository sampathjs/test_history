package com.matthey.openlink.pnl;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

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
	
	@Override
	protected void populateOutputTable(Table output) throws OException
	{
		int reportCloseDate = reportDate + 1;
		
		Logging.info("PNL_Report_Summary::populateOutputTable called.\n");
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
	}
}