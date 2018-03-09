package com.matthey.openlink.pnl;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class COG_PNL_Deal_Entry implements Comparable<Object>
{
	private PNL_EntryDataUniqueID m_id; 
	private int m_date;
	private double m_volume, m_price;
	private double m_profit = 0.0;
	private double m_volumeConsideredForDealProfit = 0.0;
	
	public void initialise(Table data, int row) throws OException
	{
		m_id = new PNL_EntryDataUniqueID(
				data.getInt("deal_num", row),
				data.getInt("deal_leg", row),
				data.getInt("deal_pdc", row),
				data.getInt("deal_reset_id", row));
		
		if (data.getColNum("date") > 0)
		{
			m_date = data.getInt("date", row);	
			m_volume = data.getDouble("volume", row);
			m_price = data.getDouble("price", row);				
		}
		else
		{
			m_date = data.getInt("deal_date", row);	
			m_volume = data.getDouble("delivery_volume", row);
			m_price = data.getDouble("delivery_price", row);					
		}
	}
	
	public int date()
	{
		return m_date;
	}	
	
	public int dealNum()
	{
		return m_id.m_dealNum;
	}	
	
	public int dealLeg()
	{
		return m_id.m_dealLeg;
	}	
	
	public int dealPdc()
	{
		return m_id.m_dealPdc;
	}	
	
	public int dealResetID()
	{
		return m_id.m_dealReset;
	}		
	
	public double volume()
	{
		return m_volume;
	}
	
	public double price()
	{
		return m_price;
	}
	
	public BUY_SELL_ENUM buySell()
	{
		BUY_SELL_ENUM value = (m_volume > 0) ? BUY_SELL_ENUM.BUY : BUY_SELL_ENUM.SELL;
		
		return value;
	}
	
	public double cost()
	{
		return m_volume * m_price;
	}
	
	public double profit()
	{
		return m_profit;
	}	
	
	public void setProfit(double profit, double volumeConsideredForDealProfit)
	{
		m_profit = profit;
		m_volumeConsideredForDealProfit = volumeConsideredForDealProfit;
	}
	
	public static Table createDealExtract() throws OException
	{
		Table dealExtract = new Table("Cost of Goods P&L - Deal Extract");
		
		dealExtract.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		dealExtract.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		dealExtract.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		dealExtract.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
		
		dealExtract.addCol("date", COL_TYPE_ENUM.COL_INT);
		dealExtract.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		dealExtract.addCol("volume", COL_TYPE_ENUM.COL_DOUBLE);
		dealExtract.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
		dealExtract.addCol("profit", COL_TYPE_ENUM.COL_DOUBLE);
		dealExtract.addCol("accum_profit", COL_TYPE_ENUM.COL_DOUBLE);
		dealExtract.addCol("volume_for_deal_profit", COL_TYPE_ENUM.COL_DOUBLE);
		
		dealExtract.setColFormatAsRef("buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
		dealExtract.setColFormatAsDate("date");
		
		dealExtract.setColFormatAsNotnl("volume",  12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		dealExtract.setColFormatAsNotnl("price",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		dealExtract.setColFormatAsNotnl("profit",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());		
		dealExtract.setColFormatAsNotnl("accum_profit",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		dealExtract.setColFormatAsNotnl("volume_for_deal_profit",  12, 5, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		
		return dealExtract;
	}

	/*
	 * Returns newly accumulated profit value
	 */
	public double addToDealExtract(Table dealExtract, double accumulatedProfit) throws OException
	{
		dealExtract.addRow();
		int row = dealExtract.getNumRows();
		
		double newAccumProfit = m_profit + accumulatedProfit;
		
		dealExtract.setInt("deal_num", row, m_id.m_dealNum);
		dealExtract.setInt("deal_leg", row, m_id.m_dealLeg);
		dealExtract.setInt("deal_pdc", row, m_id.m_dealPdc);
		dealExtract.setInt("deal_reset_id", row, m_id.m_dealReset);
		
		dealExtract.setInt("date", row, m_date);
		dealExtract.setInt("buy_sell", row, buySell().toInt());
		dealExtract.setDouble("volume", row, m_volume);
		dealExtract.setDouble("price", row, m_price);
		dealExtract.setDouble("profit", row, m_profit);
		dealExtract.setDouble("accum_profit", row, newAccumProfit);
		dealExtract.setDouble("volume_for_deal_profit", row, m_volumeConsideredForDealProfit);
		
		return newAccumProfit;
	}

	@Override
	public int compareTo(Object arg0) {
		if (this == arg0)
			return 0;
		if (arg0 == null)
			return 1;
		if (getClass() != arg0.getClass())
			return 1;
		COG_PNL_Deal_Entry other = (COG_PNL_Deal_Entry) arg0;

		if (m_date != other.m_date)
		{
			return (m_date < other.m_date) ? -1 : 1;
		}
		if (m_id.m_dealNum != other.m_id.m_dealNum)
		{
			return (m_id.m_dealNum < other.m_id.m_dealNum) ? -1 : 1;
		}
		
		return -1;
	}
}
