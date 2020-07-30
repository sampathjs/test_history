package com.matthey.openlink.pnl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

public abstract class COGPnlTradingPositionHistoryBase {

	public abstract PnlUserTableHandlerBase getPnlUserTableHandler();
	private int m_startingDate = 0;
	
	private class OpeningPosition {
		int m_date;
		double m_volume;
		double m_price;
	}
	
	// Two maps - one to group the relevant deal entries by COG key, another to group trading positions by same key
	private Map<COG_PNL_Grouping, SortedSet<COG_PNL_Deal_Entry> > m_dealHistory = new HashMap<COG_PNL_Grouping, SortedSet<COG_PNL_Deal_Entry> >();
	private Map<COG_PNL_Grouping, Vector<COG_PNL_Trading_Position_Entry> > m_positionHistory = new HashMap<COG_PNL_Grouping, Vector<COG_PNL_Trading_Position_Entry> >();
	private Map<COG_PNL_Grouping, OpeningPosition> m_openingPositions = new HashMap<COG_PNL_Grouping, OpeningPosition>();
	private Vector<Integer> relevantBuList = new Vector<Integer>();
	private List<COG_PNL_Grouping> m_relevantMetalAndBuList = new ArrayList<COG_PNL_Grouping>();
	
	private void initialiseTradingPositionMaps(Vector<Integer> buList) throws OException {
		setRelevantBuList(buList);
		for (int metal : MTL_Position_Utilities.getAllMetalCurrencyGroups()) {
			Vector<Integer> relevantBUnits = buList;
			
			if ((relevantBUnits == null) || (relevantBUnits.size() < 1)) {
				relevantBUnits = MTL_Position_Utilities.getAllInternalBUnits();
			}
			
			for (int bunit : relevantBUnits) {
				COG_PNL_Grouping key = new COG_PNL_Grouping();
				key.m_metalCcyGroup = metal;
				key.m_bunit = bunit; 				
				
				m_dealHistory.put(key, new TreeSet<COG_PNL_Deal_Entry>());
				m_positionHistory.put(key, new Vector<COG_PNL_Trading_Position_Entry>());
			}
		}
	}	
	
	public List<COG_PNL_Grouping> inititalizeAllMetalAndBUnitList() throws OException {
		Table bUnitToDefaultCurrencyMappingTable = Table.tableNew();
		Table bUnits = Util.NULL_TABLE;
		int queryId = 0;
		try {

			Vector<Integer> relevantBUnits = getRelevantBuList();

			if ((relevantBUnits == null) || (relevantBUnits.isEmpty()))
			{
				relevantBUnits = MTL_Position_Utilities.getAllInternalBUnits();
			}

			bUnits = Table.tableNew();
			bUnits.addCol("bunit", COL_TYPE_ENUM.COL_INT);
			for (Integer bUnit: relevantBUnits) {
				if (bUnit== Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PM LTD"))
					continue;
				int row = bUnits.addRow();
				bUnits.setInt("bunit",row,bUnit);
			}

			// check for getNum of rows 
			if (bUnits.getNumRows() >0)
			queryId = Query.tableQueryInsert(bUnits, "bunit");
			
			// check the query 
			String bunitToDefaultCurrencyQuery = "SELECT p.party_id as business_unit ,c.id_number as default_currency "
					+ " FROM currency c "
					+ " JOIN party_info p ON c.name = p.value \n"
					+ " JOIN party_info_types pi ON pi.type_id=p.type_id "
					+ " JOIN query_result qr ON qr.query_result = p.party_id "
					+ " WHERE  qr.unique_id = " +queryId + " and pi.type_name='Preferred Currency'";

			DBaseTable.execISql(bUnitToDefaultCurrencyMappingTable, bunitToDefaultCurrencyQuery);
			
			if (Table.isTableValid(bUnitToDefaultCurrencyMappingTable) == 1 
					&& bUnitToDefaultCurrencyMappingTable.getNumRows() > 0) {
				int numRows = bUnitToDefaultCurrencyMappingTable.getNumRows();
				
				for (int iCurrent=1;iCurrent<=numRows;iCurrent++) {
					int bUnit = bUnitToDefaultCurrencyMappingTable.getInt("business_unit", iCurrent);
					int defaultCurrency = bUnitToDefaultCurrencyMappingTable.getInt("default_currency", iCurrent);
					
					for (int metal : MTL_Position_Utilities.getAllMetalCurrencyGroups()) {
						if (defaultCurrency == metal)
							continue;
						
						COG_PNL_Grouping key = new COG_PNL_Grouping();
						key.m_metalCcyGroup = metal;
						key.m_bunit = bUnit; 
						m_relevantMetalAndBuList.add(key);
					}
				}
			} else {
				Logging.info("No data found in the bUnitToDefaultCurrencyMappingTable. ");
			}

			return m_relevantMetalAndBuList;
			
		} finally {
			if (Table.isTableValid(bUnitToDefaultCurrencyMappingTable) == 1) {
				bUnitToDefaultCurrencyMappingTable.destroy();
			}
			if (Table.isTableValid(bUnits) == 1) {
				bUnits.destroy();
			}
			if (queryId > 0) {
				Query.clear(queryId);
			}
			// clear query id 
		}
	}
	
	/*
	 * This function retrieves the starting point for the opening position
	 * This will likely be in the past, so we re-calculate historical opening positions for any back-dated deals
	 */
	private void initialiseStartingPoint(int date) throws OException {
		m_startingDate = date;
		Table data = getPnlUserTableHandler().retrieveOpenTradingPositions(m_startingDate);
		
		try {
			int rows = data.getNumRows();
			for (int row = 1; row <= rows; row++) {
				COG_PNL_Grouping key = new COG_PNL_Grouping();
				key.m_metalCcyGroup = data.getInt("metal_ccy", row);
				key.m_bunit = data.getInt("bunit", row);					
				
				OpeningPosition openPos = new OpeningPosition();
				openPos.m_date = data.getInt("open_date", row);
				openPos.m_volume = data.getDouble("open_volume", row);
				openPos.m_price = data.getDouble("open_price", row);
				
				m_openingPositions.put(key, openPos);
			}
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
		}
	}
	
	public void initialise(Vector<Integer> buList) throws OException {
		initialiseTradingPositionMaps(buList);
		initialiseStartingPoint(getPnlUserTableHandler().retrieveRegenerateDate());
	}
	
	public void initialise(Vector<Integer> buList, int date) throws OException {
		initialiseTradingPositionMaps(buList);
		initialiseStartingPoint(date);
	}
	
	public void loadDataUpTo(int date) throws OException {
		// Retrieve the deliveries made on starting date (they are not part of the opening position)
		// and up to and including the date passed in
		Table data = getPnlUserTableHandler().retrieveTradingPositionHistory(m_startingDate, date);		
		
		try {
			// The new starting date is one higher than the one passed in, so we will process
			// any sim results' values from this new starting date onwards
			m_startingDate = date + 1;
			int rows = data.getNumRows();
			for (int row = 1; row <= rows; row++) {
				COG_PNL_Deal_Entry entry = new COG_PNL_Deal_Entry();
				COG_PNL_Grouping key = new COG_PNL_Grouping();
									
				entry.initialise(data, row);
				
				key.m_bunit = data.getInt("bunit", row);
				key.m_metalCcyGroup = data.getInt("metal_ccy", row);
				
				if (m_dealHistory.containsKey(key)) {
					m_dealHistory.get(key).add(entry);
				}			
			}
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
		}
	}
	
	/*
	 * Process a list of deal data, assigning them to the correct trading position
	 * This function can be called multiple times - e.g. one per saved sim blob
	 */
	public void addDealsToProcess(Table data, int endDate) throws OException {
		int numRows = data.getNumRows();
		
		for (int row = 1; row <= numRows; row++) {
			// Skip non-Trading Margin P&L rows
			if (data.getInt("pnl_type", row) != MTL_Position_Utilities.PriceComponentType.TRADING_MARGIN_PNL) {
				continue;
			}
			
			// Skip any dates which happen before the starting date, as they are already part of the opening position
			if (data.getInt("date", row) < m_startingDate) {
				continue;
			}
			
			// Skip any entries which are not of interest to us due to being past the reporting date
			if (data.getInt("date", row) > endDate) {
				continue;
			}			
			
			COG_PNL_Deal_Entry entry = new COG_PNL_Deal_Entry();
			COG_PNL_Grouping key = new COG_PNL_Grouping();
								
			entry.initialise(data, row);
			
			key.m_bunit = data.getInt("int_bu", row);
			key.m_metalCcyGroup = data.getInt("group", row);
			
			if (m_dealHistory.containsKey(key)) {
				m_dealHistory.get(key).add(entry);
			}
		}
	}
	
	public Map<COG_PNL_Grouping, SortedSet<COG_PNL_Deal_Entry> > getDealHistoryMap() {
		return m_dealHistory;
	}
	
	public void generatePositions() throws OException {
		initPluginLog();
		for (COG_PNL_Grouping key : m_dealHistory.keySet()) {
			SortedSet<COG_PNL_Deal_Entry> dealEntrySet = m_dealHistory.get(key);
			Vector<COG_PNL_Trading_Position_Entry> output = m_positionHistory.get(key);
			
			for (COG_PNL_Deal_Entry dealEntry : dealEntrySet) {
				COG_PNL_Trading_Position_Entry posEntry = new COG_PNL_Trading_Position_Entry();
				
				// Initialise the opening position for this deal processing based on either
				// the last available entry, or, for first entry, from the initialised data from USER table
				if (output.size() > 0) {
					posEntry.setOpeningPosition(output.lastElement());
				} else {
					Logging.info("Looking for position key: " + key.m_metalCcyGroup + " \\ " + key.m_bunit + "\n");
					if (m_openingPositions.containsKey(key)) {
						OpeningPosition openPos = m_openingPositions.get(key);
						posEntry.setOpeningPosition(openPos.m_date, openPos.m_volume, openPos.m_price);
						Logging.info("Position key found, date: " + openPos.m_date + ", volume: " + openPos.m_volume + ", price: " + openPos.m_price  + "\n");
					}
				}
				
				posEntry.processDealEntry(dealEntry);
				output.add(posEntry);
			}
		}
		Logging.close();
		
	}
	
	public Table getPositionData() throws OException {
		Table output = new Table("Cost of Goods P&L");
		
		output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		output.addCol("trading_pos", COL_TYPE_ENUM.COL_TABLE);
		output.addCol("deals", COL_TYPE_ENUM.COL_TABLE);
		
		for (COG_PNL_Grouping key : m_dealHistory.keySet()) {
			SortedSet<COG_PNL_Deal_Entry> dealEntrySet = m_dealHistory.get(key);
			Vector<COG_PNL_Trading_Position_Entry> positionSet = m_positionHistory.get(key);
			
			// Skip anything where no deals are present
			if (dealEntrySet.size() < 1)
				continue;
			
			output.addRow();
			int row = output.getNumRows();
			
			output.setInt("bunit", row, key.m_bunit);
			output.setInt("metal_ccy", row, key.m_metalCcyGroup);
							
			Table tradingPosTable = COG_PNL_Trading_Position_Entry.createTradingPositionExtract();
			for (COG_PNL_Trading_Position_Entry posEntry : positionSet) {
				posEntry.addToTradingPositionExtract(tradingPosTable);
			}
			
			tradingPosTable.setColValInt("bunit", key.m_bunit);
			tradingPosTable.setColValInt("metal_ccy", key.m_metalCcyGroup);
			
			double accumProfit = 0.0;
			Table dealTable = COG_PNL_Deal_Entry.createDealExtract();			
			for (COG_PNL_Deal_Entry dealEntry : dealEntrySet) {
				accumProfit = dealEntry.addToDealExtract(dealTable, accumProfit);
			}
						
			output.setTable("trading_pos", row, tradingPosTable);
			output.setTable("deals", row, dealTable);
		}
		
		output.setColFormatAsRef("bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output.setColFormatAsRef("metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		return output;
	}
	

	/**
	 * Generate opening positions for all dates within the range given by first and last opening date. 
	 * The "close_date" column value is always equal to "opening date - 1".
	 */
	public Table getOpenPositionsForDates(int firstOpeningDate, int lastOpeningDate) throws OException {
		Table output = COG_PNL_Trading_Position_Entry.createOpenTradingPositions();		
		
		for (COG_PNL_Grouping key : m_dealHistory.keySet()) {
			SortedSet<COG_PNL_Deal_Entry> dealEntrySet = m_dealHistory.get(key);
			Vector<COG_PNL_Trading_Position_Entry> positionSet = m_positionHistory.get(key);
			
			// Skip anything where no deals are present
			if (dealEntrySet.size() < 1)
				continue;
							
			for (Integer reportingDate = firstOpeningDate; reportingDate <= lastOpeningDate; reportingDate++) {
				boolean bFound = false;
				double volume = 0.0, price = 0.0;
				
				for (int offset = 0; offset < positionSet.size(); offset++) {
					if (positionSet.get(offset).getDeliveryDate() >= reportingDate) {
						// This is the first delivery that took place on or after this reporting date - set opening data from this
						volume = positionSet.get(offset).getOpeningVolume();
						price = positionSet.get(offset).getOpeningPrice();
						bFound = true;
						break;
					}
				}
				
				if (!bFound) {
					if (positionSet.size() > 0) {
						// We did not find a delivery on or after this date - set from last available closing volume
						volume = positionSet.lastElement().getClosingVolume();
						price = positionSet.lastElement().getClosingPrice();						
					}
				}
				
				output.addRow();
				int row = output.getNumRows();
				
				output.setInt("bunit", row, key.m_bunit);
				output.setInt("metal_ccy", row, key.m_metalCcyGroup);
				output.setInt("open_date", row, reportingDate);
				output.setDouble("open_volume", row, volume);
				output.setDouble("open_price", row, price);
				output.setDouble("open_value", row, volume * price);
				output.setInt("close_date", row, reportingDate - 1);
			}
		}
	
		return output;				
	}	
	
	public int retreiveTheExtractDateFromOpenTradingPosition() {
		int extractDate = 0;
		try {
			extractDate = getPnlUserTableHandler().retriveExtractDate();
		} catch (Exception e) {
			Logging.error("The error message while retreving the extract date from open trading position  :"+e.getMessage());
		}
		return extractDate;
		
	}

	public Table populateTheOutputFromOpenTradingPosition(Integer bUnit, Integer metalCcy) {
		Table results = Util.NULL_TABLE;
		try {
			results = getPnlUserTableHandler().retreiveDataFromOpenTradingPositions(bUnit,metalCcy);
		} catch (OException e) {
			Logging.error("The error message  while populating the output from open trading position is :"+e.getMessage());
		}
		return results;
	}

	public Vector<Integer> getRelevantBuList() {
		return relevantBuList;
	}

	public void setRelevantBuList(Vector<Integer> relevantBuList) {
		this.relevantBuList = relevantBuList;
	}
	
	/**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private void initPluginLog() throws OException 
	{	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().isEmpty()) 
		{
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().isEmpty()) 
		{
			logFile = this.getClass().getName() + ".log";
		}
		try 
		{
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}
}
