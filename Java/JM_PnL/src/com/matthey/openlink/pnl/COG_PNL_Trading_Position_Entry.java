package com.matthey.openlink.pnl;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;


public class COG_PNL_Trading_Position_Entry 
{
	private int m_openingDate = 0, m_closingDate;
	private double m_openingVolume, m_closingVolume;
	private double m_openingPrice, m_closingPrice;
	
	// private boolean m_doesPositionSwitchSign = false;
	private COG_PNL_Deal_Entry m_dealEntry = null;
	
	public void setOpeningPosition(COG_PNL_Trading_Position_Entry previousEntry)
	{
		m_openingDate = previousEntry.m_closingDate;
		m_openingVolume = previousEntry.m_closingVolume;
		m_openingPrice = previousEntry.m_closingPrice;
	}
	
	public void setOpeningPosition(int date, double volume, double price)
	{
		m_openingDate = date;
		m_openingVolume = volume;
		m_openingPrice = price;
	}	
	
	public void processDealEntry(COG_PNL_Deal_Entry dealEntry)
	{
		m_dealEntry = dealEntry;
		m_closingDate = dealEntry.date();
		m_closingVolume = m_openingVolume + dealEntry.volume();
		
		double dealProfit = 0.0, volumeConsideredForDealProfit = 0.0;
		
		if (((m_openingVolume >= 0) && (dealEntry.buySell() == BUY_SELL_ENUM.BUY)) ||
			((m_openingVolume <= 0) && (dealEntry.buySell() == BUY_SELL_ENUM.SELL)))
		{
			if (Math.abs(m_closingVolume) > 0.00001)
			{
				m_closingPrice = ( m_openingVolume * m_openingPrice + dealEntry.cost() ) / m_closingVolume;
			}
			else
			{
				// Should only happen if we started at zero, and had a bad deal (with zero volume)
				m_closingPrice = 0.0;
			}			
			
			// Deal profit remains at zero - all that changes is valuation of trading position			
		}
		else if (((m_openingVolume > 0) && (dealEntry.buySell() == BUY_SELL_ENUM.SELL) && (m_closingVolume > 0) ) ||
				 ((m_openingVolume < 0) && (dealEntry.buySell() == BUY_SELL_ENUM.BUY) && (m_closingVolume < 0) ))
		{
			m_closingPrice = m_openingPrice;		
			
			// Profit is based on the deal volume, and the difference in deal price and trading position price
			
			dealProfit = ( -1 * dealEntry.volume() ) * (dealEntry.price() - m_openingPrice);		
			volumeConsideredForDealProfit = dealEntry.volume();
		}
		else
		{
			m_closingPrice = dealEntry.price();
						
			// Profit is based on the opening volume, and the difference in deal price and trading position price
			// This is because remainder of the deal now is considered a part of trading position
			dealProfit = m_openingVolume * (dealEntry.price() - m_openingPrice);
			volumeConsideredForDealProfit = m_openingVolume;
		}
		
		dealEntry.setProfit(dealProfit, volumeConsideredForDealProfit);
	}
	
	public static Table createOpenTradingPositions() throws OException
	{
		Table openTradingPos = new Table("Open Trading Position");	
		
		openTradingPos.addCol("extract_id", COL_TYPE_ENUM.COL_INT);
		openTradingPos.addCol("extract_date", COL_TYPE_ENUM.COL_INT);
		openTradingPos.addCol("extract_time", COL_TYPE_ENUM.COL_INT);
		
		openTradingPos.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		openTradingPos.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		
		openTradingPos.addCol("open_date", COL_TYPE_ENUM.COL_INT);
		openTradingPos.addCol("open_volume", COL_TYPE_ENUM.COL_DOUBLE);
		openTradingPos.addCol("open_price", COL_TYPE_ENUM.COL_DOUBLE);
		openTradingPos.addCol("open_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		openTradingPos.addCol("close_date", COL_TYPE_ENUM.COL_INT);
		
		return openTradingPos;
	}
	
	
	public static Table createTradingPositionExtract() throws OException
	{
		Table tradingPosExtract = new Table("Trading Position Extract");	
		
		tradingPosExtract.addCol("extract_id", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("extract_date", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("extract_time", COL_TYPE_ENUM.COL_INT);
		
		tradingPosExtract.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		
		tradingPosExtract.addCol("open_date", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("open_volume", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("open_price", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("open_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		tradingPosExtract.addCol("deal_date", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);		
		
		tradingPosExtract.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		
		tradingPosExtract.addCol("delivery_volume", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("delivery_price", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("delivery_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		tradingPosExtract.addCol("deal_profit", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("accum_profit", COL_TYPE_ENUM.COL_DOUBLE);
				
		tradingPosExtract.addCol("close_date", COL_TYPE_ENUM.COL_INT);
		tradingPosExtract.addCol("close_volume", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("close_price", COL_TYPE_ENUM.COL_DOUBLE);
		tradingPosExtract.addCol("close_value", COL_TYPE_ENUM.COL_DOUBLE);		
				
		tradingPosExtract.setColFormatAsRef("buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
		tradingPosExtract.setColFormatAsDate("open_date");
		//tradingPosExtract.setColFormatAsDate("delivery_date");
		tradingPosExtract.setColFormatAsDate("close_date");
		
		tradingPosExtract.setColFormatAsNotnl("delivery_volume",  12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tradingPosExtract.setColFormatAsNotnl("open_volume",  12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tradingPosExtract.setColFormatAsNotnl("close_volume",  12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		tradingPosExtract.setColFormatAsNotnl("delivery_price",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tradingPosExtract.setColFormatAsNotnl("open_price",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tradingPosExtract.setColFormatAsNotnl("close_price",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		
		tradingPosExtract.setColFormatAsNotnl("delivery_value",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tradingPosExtract.setColFormatAsNotnl("opene_value",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tradingPosExtract.setColFormatAsNotnl("close_value",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		
		return tradingPosExtract;
	}	
	
	public void addToTradingPositionExtract(Table tradingPosExtract) throws OException
	{
		tradingPosExtract.addRow();
		int row = tradingPosExtract.getNumRows();
		
		tradingPosExtract.setInt("open_date", row, m_openingDate);
		tradingPosExtract.setDouble("open_volume", row, m_openingVolume);
		tradingPosExtract.setDouble("open_price", row, m_openingPrice);
		tradingPosExtract.setDouble("open_value", row, m_openingVolume * m_openingPrice);
		
		tradingPosExtract.setInt("close_date", row, m_closingDate);
		tradingPosExtract.setDouble("close_volume", row, m_closingVolume);
		tradingPosExtract.setDouble("close_price", row, m_closingPrice);
		tradingPosExtract.setDouble("close_value", row, m_closingVolume * m_closingPrice);
		
		tradingPosExtract.setInt("deal_date", row, m_dealEntry.date());
		
		tradingPosExtract.setInt("deal_num", row, m_dealEntry.dealNum());
		tradingPosExtract.setInt("deal_leg", row, m_dealEntry.dealLeg());
		tradingPosExtract.setInt("deal_pdc", row, m_dealEntry.dealPdc());
		tradingPosExtract.setInt("deal_reset_id", row, m_dealEntry.dealResetID());
		
		tradingPosExtract.setInt("buy_sell", row, m_dealEntry.buySell().toInt());
		
		tradingPosExtract.setDouble("delivery_volume", row, m_dealEntry.volume());
		tradingPosExtract.setDouble("delivery_price", row, m_dealEntry.price());
		tradingPosExtract.setDouble("delivery_value", row, m_dealEntry.cost());		
		
		tradingPosExtract.setDouble("deal_profit", row, m_dealEntry.profit());
	}
	
	public int getOpeningDate()
	{
		return m_openingDate;
	}	
	
	public int getDeliveryDate()
	{
		return m_dealEntry.date();
	}	
	
	public double getOpeningVolume()
	{
		return m_openingVolume;
	}
	
	public double getOpeningPrice()
	{
		return m_openingPrice;
	}	
	
	public double getClosingVolume()
	{
		return m_closingVolume;
	}
	
	public double getClosingPrice()
	{
		return m_closingPrice;
	}		
}
