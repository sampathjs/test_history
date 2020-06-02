package com.matthey.openlink.pnl;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

/**
 * This class is responsible for aggregating any P&L data which does not require complex computation
 * This includes 
 * 
 *
 */
public class Basic_PNL_Aggregator 
{
	Table m_data = null;
	int m_pnlType = -1;
	
	public void initialise(int pnlType) throws OException
	{
		m_pnlType = pnlType;
	}
	
	public void addDealsToProcess(Table data) throws OException
	{
		if (m_data == null)
		{
			m_data = data.cloneTable();
		}		
		
		m_data.select(data, "*", "pnl_type EQ " + m_pnlType);
		
	}
	
	public Table getData() throws OException
	{
		return (m_data != null) ? m_data.copyTable() : new Table("");
	}
	
	public Table getDataForGivenDate(int date) throws OException
	{
		if (m_data != null)
		{
			Table dateSpecificData = m_data.cloneTable();
			
			dateSpecificData.select(m_data, "*", "date EQ " + date);
			
			return dateSpecificData;
		}
		
		return new Table("");
	}
	
	public double getColumnSumForGivenDate(String colName, int date) throws OException
	{
		double value = 0.0;
		
		if (m_data != null)
		{
			for (int row = 1; row <= m_data.getNumRows(); row++)
			{
				int rowDate = m_data.getInt("date", row);
				
				if (rowDate == date)
				{
					value += m_data.getDouble(colName, row);
				}
			}
		}
		
		return value;
	}	
	
	/**
	 * This method returns pnl data for date range greater than given date
	 * @param date
	 * @return
	 * @throws OException
	 */
	public Table getDataForInterestPnl() throws OException
	{
		if (m_data != null)
		{
			Table dateSpecificData = m_data.copyTable();
			return dateSpecificData;
		}
		
		return Util.NULL_TABLE;
	}
	
}
