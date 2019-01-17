package com.matthey.openlink.pnl;


import java.util.HashMap;
import java.util.Vector;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.UTIL_DEBUG_TYPE;
import com.openlink.util.logging.PluginLog;

public class MTL_Position_Utilities 
{
	static class IdxData
	{
		int m_idxID;
		int m_idxMarket;
		int m_currency;
		HashMap<Integer, GptData> m_gptData;
	}
	
	static class GptData
	{
		int m_idxID;
		int m_gptID;
		double m_deltaShift;
	}
	
	private static Table s_indexData = null;
	private static HashMap<Integer, IdxData> s_idxMap = null;	
	private static void initialiseIndexData() throws OException
	{
		if (s_indexData == null)
		{
			s_indexData = Index.loadAllDefs();
			// s_indexData.viewTable();
			
			s_idxMap = new HashMap<Integer, IdxData>();
			
			for (int row = s_indexData.getNumRows(); row >= 1; row--)
			{
				IdxData data = new IdxData();
				data.m_idxID = s_indexData.getInt("index_id", row);
				data.m_idxMarket = s_indexData.getInt("market_id", row);
				data.m_currency = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, s_indexData.getString("currency", row));
				data.m_gptData = new HashMap<Integer, GptData>();
				
				s_idxMap.put(data.m_idxID, data);
			}
		}
	}
	
	private static Table s_allGptData = null;	
	private static void initialiseGptData() throws OException
	{			
		if (s_allGptData == null)
		{
			initialiseIndexData();
			
			s_allGptData = Table.tableNew();
			String sqlQuery = "select idx.index_id, gpt.* from idx_gpt_def gpt, idx_def idx where idx.index_version_id = gpt.index_version_id and idx.db_status = 1";
			
			int ret = DBaseTable.execISql(s_allGptData, sqlQuery);

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
			{
				throw new RuntimeException("Unable to run query: " + sqlQuery);
			}   	
			
			for (int row = s_allGptData.getNumRows(); row >= 1; row--)
			{
				GptData gptData = new GptData();
				
				gptData.m_idxID = s_allGptData.getInt("index_id", row);
				gptData.m_gptID = s_allGptData.getInt("gpt_id", row);
				gptData.m_deltaShift = s_allGptData.getDouble("delta_shift", row);
				
				if (s_idxMap.containsKey(gptData.m_idxID))
				{
					if (!s_idxMap.get(gptData.m_idxID).m_gptData.containsKey(gptData.m_gptID))
					{
						s_idxMap.get(gptData.m_idxID).m_gptData.put(gptData.m_gptID, gptData);
					}
				}
			}			
		}	
	}	
	
	private static HashMap<Integer, Integer> s_indexesToCurrencies = null;
	
	private static void initMapping() throws OException
	{
		Table tblData = Table.tableNew();
		s_indexesToCurrencies = new HashMap<Integer, Integer>();
		
		String query = "select idx.index_id, idx.idx_subgroup, isg.name subgroup_name, isg.code code, c.id_number ccy_id " +
			"from idx_def idx, idx_subgroup isg, currency c " +
			"where idx.db_status = 1 and idx.idx_subgroup = isg.id_number and isg.code = c.name";
		
		int ret = DBaseTable.execISql(tblData, query);

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
		{
			throw new RuntimeException("Unable to run query: " + query);
		}   
		
		// tblData.viewTable();
		
		for (int row = 1; row <= tblData.getNumRows(); row++)
		{
			int thisIndex = tblData.getInt("index_id", row);
			int thisCcy = tblData.getInt("ccy_id", row);
			
			s_indexesToCurrencies.put(thisIndex, thisCcy);
		}
		
		tblData.destroy();				
	}
	
	// Converts the "Precious Metal" index into an appropriate "Precious Metal" as currency.id
	// E.g. for "XAU.EUR", gets "XAU" currency
	public static int getCcyForIndex(int index) throws OException
	{
		if (s_indexesToCurrencies == null)
		{
			initMapping();
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
	
	// Gets the payment currency of the index, e.g. for "XAU.EUR", gets "EUR"
	public static int getPaymentCurrencyForIndex(int index) throws OException
	{
		initialiseIndexData();
		
		if (s_idxMap.containsKey(index))
		{
			return s_idxMap.get(index).m_currency;
		}
		
		return -1;
	}
	
	public static int getDefaultFXIndexForCcy(int ccy) throws OException
	{
		if (s_spotPriceIndexes == null)
		{
			initCurrencyBasedData();
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
	
	public static double getDeltaShift(int indexID, int gptID) throws OException
	{
		if (s_allGptData == null)
		{
			initialiseGptData();
		}
		
		if (s_idxMap.containsKey(indexID))
		{
			if (s_idxMap.get(indexID).m_gptData.containsKey(gptID))
			{
				return s_idxMap.get(indexID).m_gptData.get(gptID).m_deltaShift;
			}
		}		
		
		return 1.0;
	}	
	
	public static int getIndexMarket(int indexID) throws OException
	{
		if (s_idxMap == null)
		{
			initialiseIndexData();
		}
		
		if (s_idxMap.containsKey(indexID))
		{
			return s_idxMap.get(indexID).m_idxMarket;
		}
		
		return -1;
	}
	
	private static HashMap<Integer, Boolean> s_preciousMetals = null;
	private static HashMap<Integer, Integer> s_spotPriceIndexes = null;
	private static HashMap<Integer, Boolean> s_quoteConventions = null;
	
	private static void initCurrencyBasedData() throws OException
	{
		Table tblData = Table.tableNew();
		s_preciousMetals = new HashMap<Integer, Boolean>();
		s_spotPriceIndexes = new HashMap<Integer, Integer>();
		s_quoteConventions = new HashMap<Integer, Boolean>();
		
		int ret = DBaseTable.execISql(tblData, "SELECT * from currency");

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
		{
			throw new RuntimeException("Unable to run query: SELECT * from currency");
		}   
		
		for (int row = 1; row <= tblData.getNumRows(); row++)
		{
			int thisCcy = tblData.getInt("id_number", row);
			boolean thisPrec = (tblData.getInt("precious_metal", row) == 1);
			int spotIndex = tblData.getInt("spot_index", row);
			boolean convention = (tblData.getInt("convention", row) > 0);
			
			s_preciousMetals.put(thisCcy, thisPrec);
			s_spotPriceIndexes.put(thisCcy, spotIndex);
			s_quoteConventions.put(thisCcy, convention);
		}
		
		tblData.destroy();		
	}	

	public static boolean isPreciousMetal(int ccy) throws OException
	{
		if (s_preciousMetals == null)
		{
			initCurrencyBasedData();
		}
		
		if (s_preciousMetals.containsKey(ccy))
		{
			return s_preciousMetals.get(ccy);
		}
		else
		{
			return false;
		}
	}	
	
	public static boolean getConvention(int ccy) throws OException
	{
		if (s_quoteConventions == null)
		{
			initCurrencyBasedData();
		}
		
		if (s_quoteConventions.containsKey(ccy))
		{
			return s_quoteConventions.get(ccy);
		}
		else
		{
			return false;
		}
	}	
	
	public static Vector<Integer> getAllMetalCurrencyGroups() throws OException
	{
		Vector<Integer> retVal = new Vector<Integer>();
		
		if (s_preciousMetals == null)
		{
			initCurrencyBasedData();
		}		
		
		for (Integer i : s_preciousMetals.keySet())
		{
			//if (i > 0)
			//{
				retVal.add(i);
			//}
		}
		
		return retVal;
	}
	
	public static double getSpotGptRate(int indexID) throws OException
	{
		boolean bFoundGpt = false;
		double spotRate = 1.0;
		
		Table data = Index.loadAllGpts(indexID);		
		
		for (int row = 1; row <= data.getNumRows(); row++)
		{
			if (data.getString("name", row).equalsIgnoreCase("Spot"))
			{
				spotRate = data.getDouble("effective.mid", row);
				bFoundGpt = true;
				break;
			}
		}
		
		if (!bFoundGpt)
		{
			// Let's try retrieving the output value for "settle" date
			spotRate = getRateForDate(indexID, OCalendar.parseString("2d", indexID));
		}
		
		return spotRate;
	}
	
	public static double getRateForDate(int indexID, int date) throws OException
	{
		boolean bRateFound = false;
		double rate = 1.0;
		PluginLog.info("MTL_Position_Utilities::getRateForDate - " + Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, indexID) + " - " + OCalendar.formatJd(date) + "\n");
		OConsole.message("MTL_Position_Utilities::getRateForDate - " + Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, indexID) + " - " + OCalendar.formatJd(date) + "\n");
		
		if (date >= OCalendar.today())
		{
			Table data = Index.getOutput(indexID, "1cd");
			
			if (Debug.isAtLeastMedium(UTIL_DEBUG_TYPE.DebugType_GENERAL.toInt()))
			{
				data.viewTable();
			}
			
			for (int row = 1; row <= data.getNumRows(); row++)
			{
				if (data.getInt("Date", row) == date)
				{
					bRateFound = true;
					
					if (data.getColNum("Price (Mid)") > 0)
					{
						rate = data.getDouble("Price (Mid)", row);
					}
					else if (data.getColNum("Disc. Factor") > 0)
					{
						rate = data.getDouble("Disc. Factor", row);
					}	
					break;
				}
			}
		}
		
		if (bRateFound)
		{
			PluginLog.info("MTL_Position_Utilities::getRateForDate found " + rate + "\n");
			OConsole.message("MTL_Position_Utilities::getRateForDate found " + rate + "\n");
		}
		else
		{
			PluginLog.info("MTL_Position_Utilities::getRateForDate date not found\n");
			OConsole.message("MTL_Position_Utilities::getRateForDate date not found\n");
		}
		
		return rate;
	}
	
	private static Vector<Integer> s_allInternalBUnits = null;
	public static Vector<Integer> getAllInternalBUnits() throws OException
	{
		if (s_allInternalBUnits == null)
		{
			Table int_bunits = Util.tableLoadBunitListForUser(0);			

			s_allInternalBUnits = new Vector<Integer>();
			for (int row = 1; row <= int_bunits.getNumRows(); row++)
			{
				int bUnit = int_bunits.getInt(1, row);
				
				s_allInternalBUnits.add(bUnit);				
			}
		}
		return s_allInternalBUnits;
	}	
	
	public class PriceComponentType 
	{
		public static final int TRADING_MARGIN_PNL = 0;
		public static final int INTEREST_PNL = 1;
		public static final int FUNDING_PNL = 2;
		public static final int FUNDING_INTEREST_PNL = 3;
	}	

	private static Table s_histPricesData = null;	
	
	private static int s_earliestDate = -1;
	
	// Refresh the historical prices saved table
	public static void refreshHistPrices() throws OException
	{
		// If we do not have an earliest date yet, we haven't loaded anything, so don't refresh anything
		// We will load the historical data on first touch which will set s_earliestDate in initHistPrices
		if (s_earliestDate > 0)
		{
			s_histPricesData = new Table("Historical Prices since: " + s_earliestDate);
			int ret = DBaseTable.execISql(s_histPricesData, "SELECT * FROM idx_historical_prices WHERE reset_date >= '" + OCalendar.formatJdForDbAccess(s_earliestDate) + "'");	
			
			s_histPricesData.group("index_id, ref_source");
					
			s_histPricesData.addCol("reset_date_int", COL_TYPE_ENUM.COL_INT);
			s_histPricesData.addCol("start_date_int", COL_TYPE_ENUM.COL_INT);		
			
			for (int row = 1; row <= s_histPricesData.getNumRows(); row++)
			{
				s_histPricesData.setInt("reset_date_int", row, s_histPricesData.getDate("reset_date", row));	
				s_histPricesData.setInt("start_date_int", row, s_histPricesData.getDate("start_date", row));			
			}			
		}
	}
	
	// Re-initialise historical prices, if necessary (i.e. prices requested for dates which have not been saved yet
	private static void initHistPrices(int earliestDate) throws OException
	{
		if ((earliestDate < s_earliestDate) || (s_earliestDate < 0))
		{
			s_earliestDate = earliestDate; 
			if (s_histPricesData != null)
			{
				s_histPricesData.destroy();
			}
		}
		else
		{
			return;
		}
		
		refreshHistPrices();		
	}

	public static double getHistPrice(int fxIndex, int resetDate, int rfisDate, int refSource) throws OException
	{
		double retValue = 0.0;
		
		initHistPrices(resetDate);
		
		int startRow = s_histPricesData.findInt("index_id", fxIndex, SEARCH_ENUM.FIRST_IN_GROUP);
		int endRow = s_histPricesData.findInt("index_id", fxIndex, SEARCH_ENUM.LAST_IN_GROUP);
		
		// If not found an entry for this index, skip it
		if ((startRow < 1) || (endRow < 1))
		{
			return retValue;
		}
		
		for (int row = startRow; row <= endRow; row++)
		{
			int rowRefSource = s_histPricesData.getInt("ref_source", row);
			int rowResetDate = s_histPricesData.getInt("reset_date_int", row);
			int rowRfisDate = s_histPricesData.getInt("start_date_int", row);
			
			if ((refSource == rowRefSource) && (resetDate == rowResetDate) && (rfisDate == rowRfisDate))
			{
				retValue = s_histPricesData.getDouble("price", row);
				break;
			}
		}
		
		return retValue;
	}
	
	public static boolean hasHistPrice(int fxIndex, int resetDate, int rfisDate, int refSource) throws OException
	{
		boolean retValue = false;
		
		initHistPrices(resetDate);
		
		int startRow = s_histPricesData.findInt("index_id", fxIndex, SEARCH_ENUM.FIRST_IN_GROUP);
		int endRow = s_histPricesData.findInt("index_id", fxIndex, SEARCH_ENUM.LAST_IN_GROUP);
		
		// If not found an entry for this index, skip it
		if ((startRow < 1) || (endRow < 1))
		{
			return retValue;
		}		
		
		for (int row = startRow; row <= endRow; row++)
		{
			int rowRefSource = s_histPricesData.getInt("ref_source", row);
			int rowResetDate = s_histPricesData.getInt("reset_date_int", row);
			int rowRfisDate = s_histPricesData.getInt("start_date_int", row);
			
			if ((refSource == rowRefSource) && (resetDate == rowResetDate) && (rfisDate == rowRfisDate))
			{
				retValue = true;
				break;
			}
		}
		
		return retValue;
	}	
}
