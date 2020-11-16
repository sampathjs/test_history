package com.matthey.openlink.pnl;

import com.matthey.openlink.pnl.MTL_Position_Utilities.PriceComponentType;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;

import java.util.HashSet;
import java.util.Vector;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

public abstract class PNL_ReportEngine implements IScript {
	
	/* Made these protected, in-case sub class needs them for anything */
	protected int reportDate;
	protected int startDate = -1;
	protected int today;
	protected int runType;
	protected ODateTime extractDateTime;
	
	protected boolean isEODRun = false;
	protected boolean useSavedEODSimData = false;
	protected int calcStartDate;
	protected int calcEndDate;
	
	protected COGPnlTradingPositionHistoryBase m_positionHistory = null;
	Basic_PNL_Aggregator	m_interestPNLAggregator = null;
	Basic_PNL_Aggregator	m_fundingPNLAggregator = null;
	Basic_PNL_Aggregator	m_fundingInterestPNLAggregator = null;
	
	protected String intBUList = "";
	protected String extendedBUList = "";
	protected Vector<Integer> intBUVector = new Vector<>();
	protected HashSet<Integer> intBUSet = new HashSet<>();
	
	protected static class RefConversionData {
		String m_colName;
		SHM_USR_TABLES_ENUM m_refID;
	}

	protected enum DateConversionType {
		TYPE_DMY,
		TYPE_MY,
		TYPE_QY
	}

	protected static class DateConversionData {
		String m_colName;
		DateConversionType m_type;
	}

	protected static class TableConversionData {
		String m_colName;
		String m_tableQuery;		
	}

	protected Vector<RefConversionData> m_refConversions = new Vector<>();
	protected Vector<DateConversionData> m_dateConversions = new Vector<>();
	protected Vector<TableConversionData> m_tableConversions = new Vector<>();
	
	protected void setupParameters(Table argt) throws OException {
		/* Set default values */
		today = OCalendar.today(); 
		reportDate = OCalendar.today();     
		runType = SIMULATION_RUN_TYPE.EOD_SIM_TYPE.toInt();
		extractDateTime = ODateTime.getServerCurrentDateTime();		
		
		Table paramsTable = argt.getTable("PluginParameters", 1);
		
		int intBURow = paramsTable.unsortedFindString(1, "internalBU", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if (intBURow > 0) {
			intBUList = paramsTable.getString(2, intBURow).trim();
			
			if (intBUList.length() > 0) {										
				String[] intBUListSplit = intBUList.split(",");
				
				for (String buSample : intBUListSplit) {
					try {
						buSample = buSample.trim();
						int bu = Integer.parseInt(buSample);
						if (bu > 0) {
							intBUVector.add(bu);
							intBUSet.add(bu);
						}
					} catch (Exception ignored) {
					}
				}	
				
				// Extended BU list includes any units that will map to those of interest to final report, 
				// since their data will have to be retrieved
				extendedBUList = PNL_BusinessUnitMapper.getExtendedBUList(intBUSet);
			}
		}
		
		Logging.info("PNL_ReportEngine:: intBUList = '" + intBUList + "'\n"		
				+ "PNL_ReportEngine:: extendedBUList = '" + extendedBUList + "'\n" 
				+ "PNL_ReportEngine:: intBUVector = '" + intBUVector.toString() + "'\n" 
				+ "PNL_ReportEngine:: intBUSet = '" + intBUSet.toString() + "'\n");
		
		int reportDateRow = paramsTable.unsortedFindString(1, "reportDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (reportDateRow > 0) {
			try {
				String reportDateValue = paramsTable.getString(2, reportDateRow);
				String[] reportDateSplit = reportDateValue.split(" ");
				reportDateValue = reportDateSplit[0];
				reportDate = OCalendar.parseString(reportDateValue);				
			} catch(Exception e) {
				Logging.error("PNL_ReportEngine::setupParameters could not parse report date, defaulting to today");
				reportDate = today;
			}
		}
		
		int startDateRow = paramsTable.unsortedFindString(1, "startDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (startDateRow > 0) {
			try {
				String startDateValue = paramsTable.getString(2, startDateRow);
				startDate = OCalendar.parseString(startDateValue);				
			} catch(Exception e) {
				Logging.error("PNL_ReportEngine::setupParameters could not parse report date, defaulting to today");
			}
		}

		int isEODRow = paramsTable.unsortedFindString(1, "isEOD", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (isEODRow > 0) {
			try {
				String isEODValue = paramsTable.getString(2, isEODRow);
				isEODRun = isEODValue.equals("Yes");
				Logging.info("PNL_ReportEngine::setupParameters - isEODValue is: " + isEODValue + ", isEODRun is " + (isEODRun ? "true" : "false") + "\n");
			} catch(Exception e) {
				Logging.error("PNL_ReportEngine::setupParameters could not parse isEODRow field, defaulting to false.\n");
				isEODRun = false;
			}
		}
		
		int useSavedEODSimDataRow = paramsTable.unsortedFindString(1, "useSavedEODSimData", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (useSavedEODSimDataRow > 0) {
			try {
				String useSavedEODSimDataValue = paramsTable.getString(2, useSavedEODSimDataRow);
				useSavedEODSimData = useSavedEODSimDataValue.equals("Yes");
				Logging.info("PNL_ReportEngine::setupParameters - useSavedEODSimData is: " + useSavedEODSimDataValue + ", useSavedEODSimData is " + (useSavedEODSimData ? "true" : "false") + "\n");
			} catch(Exception e) {
				Logging.error("PNL_ReportEngine::setupParameters could not parse useSavedEODSimData field, defaulting to false.\n");
				useSavedEODSimData = false;
			}
		}		
		
		if (isEODRun) {
			calcStartDate = OCalendar.getSOM(OCalendar.getSOM(today)-1);
			calcEndDate = today;
			reportDate = today; 
		} else {
			int regenerateDate = startDate == - 1 ? getUserTableHandler().retrieveRegenerateDate() : startDate;
			
			if ((regenerateDate > 0) && (regenerateDate < reportDate)) {
				calcStartDate = regenerateDate;
			} else {
				calcStartDate = reportDate;
			}
						
			calcEndDate = reportDate;
		}
		Logging.info("Calculations will run from: " + OCalendar.formatJd(calcStartDate) + " to " + OCalendar.formatJd(calcEndDate) + "\n");
	}
	
	public void initialiseProcessors() throws OException {
		// Create a new position history instance
		m_positionHistory = new COG_PNL_Trading_Position_History();
		
		if (startDate == -1) {
			m_positionHistory.initialise(intBUVector);
		} else {
			m_positionHistory.initialise(intBUVector, startDate);
		}
		
		if (!isEODRun) {
			m_positionHistory.loadDataUpTo(calcStartDate - 1);
		}

		m_interestPNLAggregator = new Basic_PNL_Aggregator();
		m_interestPNLAggregator.initialise(PriceComponentType.INTEREST_PNL);
		
		m_fundingPNLAggregator = new Basic_PNL_Aggregator();
		m_fundingPNLAggregator.initialise(PriceComponentType.FUNDING_PNL);		
		
		m_fundingInterestPNLAggregator = new Basic_PNL_Aggregator();
		m_fundingInterestPNLAggregator.initialise(PriceComponentType.FUNDING_INTEREST_PNL);			
	}
	
	protected void processAdhocData() throws OException {
		String sqlFXComFutQuery = "SELECT ab.tran_num "
				+ "FROM ab_tran ab "
				+ "WHERE ab.toolset IN (9, 17) "
					+ "AND ab.tran_type = 0 "
					+ "AND ab.tran_status IN (2, 3, 4) "
					+ "AND ab.current_flag = 1";
		
		String sqlComSwapQuery = "SELECT ab.tran_num "
				+ "FROM ab_tran ab "
				+ "WHERE ab.toolset IN (15) "
					+ "AND ab.tran_type = 0 "
					+ "AND ab.tran_status IN (2, 3, 4) "
					+ "AND ab.current_flag = 1";
		
		sqlFXComFutQuery += " AND trade_date >= " + calcStartDate;
		sqlFXComFutQuery += " AND trade_date <= " + calcEndDate;
		
		sqlComSwapQuery += " AND start_date <= " + calcEndDate;
		sqlComSwapQuery += " AND maturity_date >= " + calcStartDate;
		
		// Use extendedBUList, as we need access to all deals from BU's that map to reporting BU
		if (extendedBUList.length() > 0) {
			sqlFXComFutQuery += " AND internal_bunit IN (" + extendedBUList + ")";
			sqlComSwapQuery += " AND internal_bunit IN (" + extendedBUList + ")";
		}
		
		String finalSqlQuery = sqlFXComFutQuery + " UNION " + sqlComSwapQuery;
		Table simResults = Util.NULL_TABLE;
		Table tranNums = new Table("");
		int queryId = -1;
		
		try {
			DBaseTable.execISql(tranNums, finalSqlQuery);
			Logging.info(finalSqlQuery + "\n");
			
			// If there are no transactions of relevance, exit now
			if (tranNums.getNumRows() < 1)
				return;
			
			queryId = Query.tableQueryInsert(tranNums, "tran_num");
			
	        Table tblSim = Sim.createSimDefTable();
	        Sim.addSimulation(tblSim, "Sim");
	        Sim.addScenario(tblSim, "Sim", "Base", Ref.getLocalCurrency());

	        /* Build the result list */
	        Table tblResultList = Sim.createResultListForSim();
	        SimResult.addResultForSim(tblResultList, SimResultType.create("USER_RESULT_JM_RAW_PNL_DATA"));
	        SimResult.addResultForSim(tblResultList, SimResultType.create("USER_RESULT_JM_INTEREST_PNL_DATA"));
	        Sim.addResultListToScenario(tblSim, "Sim", "Base", tblResultList);

			// Create reval table
			Table revalParam = Table.tableNew("SimArgumentTable");
	        Sim.createRevalTable(revalParam);
	        revalParam.setInt("QueryId", 1, queryId);
	        revalParam.setTable("SimulationDef", 1, tblSim);		
	         		
	        // Package into a table as expected by Sim.runRevalByParamFixed
	        Table revalTable = Table.tableNew("RevalTable");
	        revalTable.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
	        revalTable.addRow();
	        revalTable.setTable("RevalParam", 1, revalParam);

        // Run the simulation
		simResults = Sim.runRevalByParamFixed(revalTable);    		
			
			if (Table.isTableValid(simResults) == 1) {	   
				Logging.info("ReportEngine:: Processing ad-hoc simulation results...\n");
				Table genResults = SimResult.getGenResults(simResults, 1);
				
				if (Table.isTableValid(genResults) == 1) {
					Table rawPnlData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum("USER_RESULT_JM_RAW_PNL_DATA"), -2, -2, -2);
					Table interestPnlData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum("USER_RESULT_JM_INTEREST_PNL_DATA"), -2, -2, -2);
					
					if (Table.isTableValid(rawPnlData) == 1) {
						m_positionHistory.addDealsToProcess(rawPnlData, calcEndDate);	
						m_fundingPNLAggregator.addDealsToProcess(rawPnlData);
					}
					if (Table.isTableValid(interestPnlData) == 1) {
						m_interestPNLAggregator.addDealsToProcess(interestPnlData);
						m_fundingInterestPNLAggregator.addDealsToProcess(interestPnlData);
					}
				}
			}	
			
		} finally {
			if (queryId > 0) {
				Query.clear(queryId);	
			}
			if (Table.isTableValid(tranNums) == 1) {
				tranNums.destroy();	
			}
			if (Table.isTableValid(simResults) == 1) {
				simResults.destroy();
			}
		}
	}
	
	public void execute(IContainerContext context) throws OException {		
		initLogging();
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		
		setupParameters(argt);
		
		generateOutputTableFormat(returnt);
		registerConversions(returnt);
		
		String RUN_MODE_COL_NAME = "ModeFlag";
		if (argt.getInt(RUN_MODE_COL_NAME, 1) == 0) {
			performConversions(returnt);
			return;
		}		
		
		initialiseProcessors();
		
		// MAXHACK: For now, just always run the data
		processAdhocData();
		
		if (m_positionHistory != null) {
			m_positionHistory.generatePositions();
		}

		// On EOD runs, update the trading position history and the open trading positions
		if (isEODRun) {
			getUserTableHandler().recordTradingPositionHistory(m_positionHistory);
			getUserTableHandler().recordOpenTradingPositions(m_positionHistory, getFirstOpenDate(), getLastOpenDate());
			
			// Now, make a note that we don't need to regenerate any past dates
			getUserTableHandler().setRegenerateDate(-1);
		}
				
		populateOutputTable(returnt);
		performConversions(returnt);
		Logging.close();
	}
	
	private int getFirstOpenDate() throws OException {
		return OCalendar.getSOM(OCalendar.getSOM(today) - 1);
	}

	private int getLastOpenDate() {
		return today + 1;
	}

		
	/**
	 * Register a RefID-type data conversion for the final output table
	 */
	protected void regRefConversion(Table dataTable, String colName, SHM_USR_TABLES_ENUM refID) throws OException
	{    	
		RefConversionData convData = new RefConversionData();

		dataTable.addCol(colName + "_str", COL_TYPE_ENUM.COL_STRING);

		convData.m_colName = colName;
		convData.m_refID = refID;

		m_refConversions.add(convData);
	}

	/**
	 * Register the need for date formatting on given column for the final output table
	 */
	protected void regDateConversion(Table dataTable, String colName) throws OException
	{
		/* If no parameter passed in, assume DMY */
		regDateConversion(dataTable, colName, DateConversionType.TYPE_DMY);

	}

	
	/**
	 * Register the need for date formatting on given column for the final output table
	 */
	protected void regDateConversion(Table dataTable, String colName, DateConversionType type) throws OException
	{ 
		DateConversionData convData = new DateConversionData();

		dataTable.addCol(colName + "_str", COL_TYPE_ENUM.COL_STRING);

		convData.m_colName = colName;
		convData.m_type = type;

		m_dateConversions.add(convData);
	}
	
	
	/**
	 * Perform data type conversions on the final output table according to registered requirements	 
	 */
	protected void performConversions(Table output) throws OException
	{
		for (RefConversionData conv : m_refConversions)
		{
			output.copyColFromRef(conv.m_colName, conv.m_colName + "_str", conv.m_refID);
			output.setColName(conv.m_colName, "orig_" + conv.m_colName);
			output.setColName(conv.m_colName + "_str", conv.m_colName);
		}

		for (DateConversionData conv : m_dateConversions)
		{
			switch (conv.m_type)
			{
			case TYPE_DMY:
				output.copyColFormatDate(conv.m_colName, conv.m_colName + "_str", 
						DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_EUROPE);
				break;
			case TYPE_MY:
				for (int row = 1; row <= output.getNumRows();row++)
				{
					int jd = output.getInt(conv.m_colName, row);
					String monthStr = OCalendar.getMonthStr(jd) + "-" + OCalendar.getYear(jd);
					output.setString(conv.m_colName + "_str", row, monthStr);
				}    				  				
				break;
			case TYPE_QY:
				output.copyColFormatDate(conv.m_colName, conv.m_colName + "_str", 
						DATE_FORMAT.DATE_FORMAT_QUARTER_YEAR, DATE_LOCALE.DATE_LOCALE_EUROPE);      				
				break;    		
			}
			output.setColName(conv.m_colName, "orig_" + conv.m_colName);
			output.setColName(conv.m_colName + "_str", conv.m_colName);     		
		}

		for (TableConversionData conv : m_tableConversions)
		{
			Table convTable = Table.tableNew();
			DBaseTable.execISql(convTable, conv.m_tableQuery);

			// Force the column names, just in case
			convTable.setColName(1, "id");
			convTable.setColName(2, "name");

			output.select(convTable, "name(" + conv.m_colName + "_str" + ")", "id EQ $" + conv.m_colName); 

			output.setColName(conv.m_colName, "orig_" + conv.m_colName);
			output.setColName(conv.m_colName + "_str", conv.m_colName);
		}    	
	}
	
	/**
	 * Initialise standard Plugin log functionality
	 */
	private void initLogging() {
		try {
			Logging.init(this.getClass(),
						 ConfigurationItemPnl.CONST_REP_CONTEXT,
						 ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Generate custom output table format
	 */
	protected abstract void generateOutputTableFormat(Table output) throws OException;
	
	/**
	 * Generate the data for the custom output table
	 */
	protected abstract void populateOutputTable(Table output) throws OException;
	
	/**
	 * Register any ref-format column conversions
	 */
	protected abstract void registerConversions(Table output) throws OException;   
	
	
	public abstract IPnlUserTableHandler getUserTableHandler();
	
}