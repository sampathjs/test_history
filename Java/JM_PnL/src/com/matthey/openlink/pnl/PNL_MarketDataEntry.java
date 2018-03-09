package com.matthey.openlink.pnl;


public class PNL_MarketDataEntry 
{
	PNL_EntryDataUniqueID m_uniqueID;
	
	int m_tradeDate;
	
	int m_indexID;
	int m_metalCcy;
	int m_fixingDate;
	
	double m_spotRate;
	double m_forwardRate;
	double m_usdDF;
	
	private static int s_USD = 0;
	
	PNL_MarketDataEntry()
	{
		m_metalCcy = s_USD;
		m_spotRate = 1.0;
		m_forwardRate = 1.0;
		m_usdDF = 1.0;
	}
}
