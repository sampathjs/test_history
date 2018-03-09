package com.matthey.openlink.reporting.util;

import java.util.HashMap;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public class PnLUtils 
{
	private static HashMap<Integer, Integer> s_indexesToCurrencies = null;
	private static HashMap<Integer, Integer> s_spotPriceIndexes = null;
	private static HashMap<Integer, Boolean> s_preciousMetals = null;
	
	private static void initMapping(Session session)
	{	
		s_indexesToCurrencies = new HashMap<Integer, Integer>();
		
		String query = "select idx.index_id, idx.idx_subgroup, isg.name subgroup_name, isg.code code, c.id_number ccy_id " +
			"from idx_def idx, idx_subgroup isg, currency c " +
			"where idx.db_status = 1 and idx.idx_subgroup = isg.id_number and isg.code = c.name";
		
		Table tblData = session.getIOFactory().runSQL(query);
		
		for (int row = 0; row < tblData.getRowCount(); row++)
		{
			int thisIndex = tblData.getInt("index_id", row);
			int thisCcy = tblData.getInt("ccy_id", row);
			
			s_indexesToCurrencies.put(thisIndex, thisCcy);
		}				
	}
	
	public static int getCcyForIndex(Session session, int index)
	{
		if (s_indexesToCurrencies == null)
		{
			initMapping(session);
		}
		
		if (s_indexesToCurrencies.containsKey(index))
		{
			return s_indexesToCurrencies.get(index);
		}
		else
		{
			return 0;
		}		
	}
	
	private static void initCurrencyBasedData(Session session)
	{
		s_preciousMetals = new HashMap<Integer, Boolean>();
		s_spotPriceIndexes = new HashMap<Integer, Integer>();
		
		Table tblData = session.getIOFactory().runSQL("SELECT * from currency");

		for (int row = 0; row < tblData.getRowCount(); row++)
		{
			int thisCcy = tblData.getInt("id_number", row);
			boolean thisPrec = (tblData.getInt("precious_metal", row) == 1);
			int spotIndex = tblData.getInt("spot_index", row);
			
			s_preciousMetals.put(thisCcy, thisPrec);
			s_spotPriceIndexes.put(thisCcy, spotIndex);
		}	
	}	
	
	public static int getIndexForCcy(Session session, int ccy)
	{
		if (s_spotPriceIndexes == null)
		{
			initCurrencyBasedData(session);
		}
		
		if (s_spotPriceIndexes.containsKey(ccy))
		{
			return s_spotPriceIndexes.get(ccy);
		}
		else
		{
			return 0;
		}
	}	
}
