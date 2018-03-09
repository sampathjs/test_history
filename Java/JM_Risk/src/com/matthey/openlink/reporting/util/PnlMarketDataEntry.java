package com.matthey.openlink.reporting.util;

public class PnlMarketDataEntry 
{
	PnlMarketDataUniqueID m_uniqueID;
	
	int m_indexID;
	int m_metalCcy;
	int m_fixingDate;
	
	double m_spotRate;
	double m_forwardRate;
	double m_usdDF;
	
	private static int s_USD = 0;
	
	PnlMarketDataEntry()
	{
		m_metalCcy = s_USD;
		m_spotRate = 1.0;
		m_forwardRate = 1.0;
		m_usdDF = 1.0;
	}
}
