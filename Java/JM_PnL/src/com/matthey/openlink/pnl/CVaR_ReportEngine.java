package com.matthey.openlink.pnl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.openjvs.enums.UTIL_DEBUG_TYPE;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

public abstract class CVaR_ReportEngine implements IScript {
	
	/* Made these protected, in-case sub classes needs them for anything */
	protected int reportDate;
	protected int today;
	protected int runType;
	protected ODateTime extractDateTime;
	
	protected boolean isEODRun = false;
	protected boolean useSavedEODSimData = false;
	protected boolean isSummaryView = true;
		
	protected String intBUList = "";
	protected Vector<Integer> intBUVector = new Vector<Integer>();
	
	private static String RUN_MODE_COL_NAME = "ModeFlag";
	
	protected final static String CVAR_DIRECTION_POSITIVE = "Positive";
	protected final static String CVAR_DIRECTION_NEGATIVE = "Negative";
	
	
	protected class RefConversionData {
		String m_colName;
		SHM_USR_TABLES_ENUM m_refID;
	}

	protected enum DateConversionType {
		TYPE_DMY,
		TYPE_MY,
		TYPE_QY
	}

	protected class DateConversionData {
		String m_colName;
		DateConversionType m_type;
	}

	protected class TableConversionData {
		String m_colName;
		String m_tableQuery;		
	}
	
	protected Vector<RefConversionData> m_refConversions = new Vector<RefConversionData>();
	protected Vector<DateConversionData> m_dateConversions = new Vector<DateConversionData>();
	protected Vector<TableConversionData> m_tableConversions = new Vector<TableConversionData>();	
	
	protected static class MatBucketDefinition {
		int m_bucketID;
		String m_bucketLabel;
		
		MatBucketDefinition(int bucketID, String bucketLabel) {
			m_bucketID = bucketID;
			m_bucketLabel = bucketLabel;
		}
	}
	
	protected static class MetalDefinition {
		int m_metalID;
		String m_lowercaseLabel;
		
		MetalDefinition(int metalID, String lowercaseLabel) {
			m_metalID = metalID;
			m_lowercaseLabel = lowercaseLabel;
		}		
	}
	
	protected static Map<Integer, MatBucketDefinition> getMaturityBuckets() throws OException {
		Map<Integer, MatBucketDefinition> map = new HashMap<Integer, MatBucketDefinition>();
		Table data = new Table("user_jm_cvar_maturity_buckets");
		
		try {
			DBUserTable.load(data);
			int rows = data.getNumRows();
			for (int row = 1; row <= rows; row++) {
				int id = data.getInt("id", row);
				String label = data.getString("label", row);
				
				map.put(id, new MatBucketDefinition(id, label));
			}
		} finally {
			if (Table.isTableValid(data) == 1) {
				data.destroy();
			}
		}
		
		return map;
	}
	
	protected static Map<Integer, MetalDefinition> getMetals() throws OException {
		Map<Integer, MetalDefinition> map = new HashMap<Integer, MetalDefinition>();
		Table tblData = Table.tableNew();
		
		try {
			int ret = DBaseTable.execISql(tblData, "SELECT * from currency");
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException("Unable to run query: SELECT * from currency");
			}   
			
			int rows = tblData.getNumRows();
			for (int row = 1; row <= rows; row++) {
				int isPreciousMetal = tblData.getInt("precious_metal", row);
				int id = tblData.getInt("id_number", row);		
				String name = tblData.getString("name", row);
				
				if (isPreciousMetal == 1) {
					map.put(id, new MetalDefinition(id, name.toLowerCase()));
				}
			}
		} finally {
			if (Table.isTableValid(tblData) == 1) {
				tblData.destroy();
			}
		}
		return map;
	}
	
	protected class GroupingCriteria {
		int m_counterparty;
		int m_metal;		
		int m_maturityBucket;
		
		public GroupingCriteria(int counterparty, int metal, int maturityBucket) {
			m_counterparty = counterparty;
			m_metal = metal;			
			m_maturityBucket = maturityBucket;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + m_counterparty;
			result = prime * result + m_maturityBucket;
			result = prime * result + m_metal;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GroupingCriteria other = (GroupingCriteria) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (m_counterparty != other.m_counterparty)
				return false;
			if (m_maturityBucket != other.m_maturityBucket)
				return false;
			if (m_metal != other.m_metal)
				return false;
			return true;
		}
		private CVaR_ReportEngine getOuterType() {
			return CVaR_ReportEngine.this;
		}				
	}
	
	protected class PositionData {
		double m_position = 0.0;
		double m_posCVaR = 0.0;
		double m_negCVaR = 0.0;
	}
	
	protected class SummaryPositionData {
		int m_counterparty = 0;
		PositionData m_summaryData = new PositionData();
		Map<GroupingCriteria, PositionData> m_cVaRDetailsMap = new HashMap<GroupingCriteria, PositionData>();
		
		public SummaryPositionData(int counterparty) {
			m_counterparty = counterparty;
		}
	}
	
	protected Map<Integer, SummaryPositionData> m_counterpartySummaryData = new HashMap<Integer, SummaryPositionData>();
	
	protected Set<Integer> m_uniqueCounterparties = new HashSet<Integer>();	
	
	protected void processPortfolioDataTable(Table data) throws OException {
		int rows = data.getNumRows();
		for (int row = 1; row <= rows; row++) {
			int counterparty = data.getInt("counterparty", row);
			int metal = data.getInt("metal", row);
			int bucket = data.getInt("maturity_bucket", row);			
			
			// Skip deals with no counterparty
			if (counterparty == 0)
				continue;
			
			double position = data.getDouble("delta", row);
			double posShockCVaR = data.getDouble("pos_shock_cvar", row);
			double negShockCVaR = data.getDouble("neg_shock_cvar", row);
									
			// Add a new entry for this counterparty
			if (!m_counterpartySummaryData.containsKey(counterparty)) {
				m_counterpartySummaryData.put(counterparty, new SummaryPositionData(counterparty));
			}
			PositionData summaryData = m_counterpartySummaryData.get(counterparty).m_summaryData;
			
			// Add position, positive CVaR and negative CVaR to the per-party summary
			summaryData.m_position += position;
			summaryData.m_posCVaR += posShockCVaR;
			summaryData.m_negCVaR += negShockCVaR;
						
			// Add a new metal+bucket grouping criteria if it does not exist yet
			GroupingCriteria group = new GroupingCriteria(counterparty, metal, bucket);
			Map<GroupingCriteria, PositionData> detailsMap = m_counterpartySummaryData.get(counterparty).m_cVaRDetailsMap;
			if (!detailsMap.containsKey(group)) {
				detailsMap.put(group, new PositionData());
			}
			
			PositionData posData = detailsMap.get(group);
			
			posData.m_position += position;
			posData.m_posCVaR += posShockCVaR;
			posData.m_negCVaR += negShockCVaR;
		}
	}	

	private void setupParameters(Table argt) throws OException {
		/* Set default values */
		today = OCalendar.today(); 
		reportDate = OCalendar.today();     
		runType = SIMULATION_RUN_TYPE.EOD_SIM_TYPE.toInt();
		extractDateTime = ODateTime.getServerCurrentDateTime();		
		
		Table paramsTable = argt.getTable("PluginParameters", 1);
		int reportDateRow = paramsTable.unsortedFindString(1, "reportDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		
		if (reportDateRow > 0) {
			try {
				String reportDateValue = paramsTable.getString(2, reportDateRow);
				String[] reportDateSplit = reportDateValue.split(" ");
				reportDateValue = reportDateSplit[0];
				reportDate = OCalendar.parseString(reportDateValue);
				
			} catch(Exception e) {
				Logging.error("CVaR_ReportEngine::setupParameters could not parse report date, defaulting to today");
				reportDate = today;
			}
		}
		
		int isEODRow = paramsTable.unsortedFindString(1, "isEOD", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (isEODRow > 0) {
			try {
				String isEODValue = paramsTable.getString(2, isEODRow);
				if (isEODValue.equals("Yes")) {
					isEODRun = true;
				} else {
					isEODRun = false;
				}
				PluginLog.info("CVaR_ReportEngine::setupParameters - isEODValue is: " + isEODValue + ", isEODRun is " + (isEODRun ? "true" : "false") + "\n");
			} catch(Exception e) {
				Logging.error("CVaR_ReportEngine::setupParameters could not parse isEODRow field, defaulting to false.\n");
				OConsole.message("CVaR_ReportEngine::setupParameters could not parse isEODRow field, defaulting to false.\n");
				isEODRun = false;
			}
		}
		
		int isSummaryViewRow = paramsTable.unsortedFindString(1, "isSummaryView", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (isSummaryViewRow > 0) {
			try {
				String isSummaryValue = paramsTable.getString(2, isSummaryViewRow);
				if (isSummaryValue.equals("Yes")) {
					isSummaryView = true;
				} else {
					isSummaryView = false;
				}
				Logging.info("CVaR_ReportEngine::setupParameters - isSummaryView is: " + isSummaryValue + ", isSummaryView is " + (isSummaryView ? "true" : "false") + "\n");
				OConsole.message("CVaR_ReportEngine::setupParameters - isSummaryView is: " + isSummaryValue + ", isSummaryView is " + (isSummaryView ? "true" : "false") + "\n");
			}
			catch(Exception e)
			{
				Logging.error("CVaR_ReportEngine::setupParameters could not parse isSummaryView field, defaulting to true.\n");
				OConsole.message("CVaR_ReportEngine::setupParameters could not parse isSummaryView field, defaulting to true.\n");
				isSummaryView = true;
			}
		}		
		
		int useSavedEODSimDataRow = paramsTable.unsortedFindString(1, "useSavedEODSimData", SEARCH_CASE_ENUM.CASE_INSENSITIVE);		
		if (useSavedEODSimDataRow > 0) {
			try {
				String useSavedEODSimDataValue = paramsTable.getString(2, useSavedEODSimDataRow);
				if (useSavedEODSimDataValue.equals("Yes")) {
					useSavedEODSimData = true;
				} else {
					useSavedEODSimData = false;
				}
				Logging.info("CVaR_ReportEngine::setupParameters - useSavedEODSimData is: " + useSavedEODSimDataValue + ", useSavedEODSimData is " + (useSavedEODSimData ? "true" : "false") + "\n");
				OConsole.message("CVaR_ReportEngine::setupParameters - useSavedEODSimData is: " + useSavedEODSimDataValue + ", useSavedEODSimData is " + (useSavedEODSimData ? "true" : "false") + "\n");
			}
			catch(Exception e)
			{
				Logging.error("CVaR_ReportEngine::setupParameters could not parse useSavedEODSimData field, defaulting to false.\n");
				OConsole.message("CVaR_ReportEngine::setupParameters could not parse useSavedEODSimData field, defaulting to false.\n");
				useSavedEODSimData = false;
			}
		}		
	}
	

	/**
	 * Main function to generate the output
	 */
	public void execute(IContainerContext context) throws OException {
		initPluginLog();
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		
		setupParameters(argt);
		
		generateOutputTableFormat(returnt);
		registerConversions(returnt);
		
		if (argt.getInt(RUN_MODE_COL_NAME, 1) == 0) {
			performConversions(returnt);
			return;
		}		
		
		processEODData(generatePortfolioList());
						
		populateOutputTable(returnt);
		performConversions(returnt);
		Logging.close();
		// returnt.viewTable();		
	}

	/**
	 * Iterate over the EOD data portfolio by portfolio
	 */
	private void processEODData(Vector<Integer> portfolioList) throws OException {
		int portfolios = portfolioList.size();
		Table simResults = Util.NULL_TABLE;
		
		for (int i = 0; i < portfolios; i++) {
			try {
				simResults = SimResult.tableLoadSrun(portfolioList.get(i), runType, reportDate, 0);
				if (Table.isTableValid(simResults) == 1) {
					Logging.info("ReportEngine:: Processing simulation results for pfolio: " + Ref.getName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, portfolioList.get(i)) + "(" + portfolioList.get(i) + ")"
							+ ", sim type: " + runType + ", run date: " + OCalendar.formatJd(reportDate) + "\r\n");

			if (Table.isTableValid(simResults) == 1)
			{     		
				Logging.info("ReportEngine:: Processing simulation results for pfolio: "
						+ Ref.getName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, portfolioList.get(i)) + "(" + portfolioList.get(i) + ")"
						+ ", sim type: " + runType + ", run date: " + OCalendar.formatJd(reportDate) + "\r\n");
				OConsole.message("ReportEngine:: Processing simulation results for pfolio: "
						+ Ref.getName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, portfolioList.get(i)) + "(" + portfolioList.get(i) + ")"
						+ ", sim type: " + runType + ", run date: " + OCalendar.formatJd(reportDate) + "\r\n");

				// Iterate over all scenarios, and find the first one with JM Credit VaR Data
				for (int j = 1; j <= simResults.getNumRows(); j++)
				{
					Table genResults = SimResult.getGenResults(simResults, j);
					
					if (Table.isTableValid(genResults) == 1)
					{
						Table cVaRData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum("USER_RESULT_JM_CREDIT_VAR_DATA"), -2, -2, -2);

						if (Table.isTableValid(cVaRData) == 1)
						{
							Logging.info("ReportEngine:: scenario ID " + j + " contains JM Credit VaR Data. Processing.\r\n");
							OConsole.message("ReportEngine:: scenario ID " + j + " contains JM Credit VaR Data. Processing.\r\n");
							processPortfolioDataTable(cVaRData);
							break; // Once we have processed Credit VaR Data for this portfolio, move on 
						}						
					}	
					Logging.info("ReportEngine:: scenario ID " + j + " does not contain JM Credit VaR Data. Skipping.\r\n");
					OConsole.message("ReportEngine:: scenario ID " + j + " does not contain JM Credit VaR Data. Skipping.\r\n");
				}				

				/* Clear out sim results to free memory */
				simResults.destroy();
			}
			else
			{
				if (Debug.isAtLeastMedium(UTIL_DEBUG_TYPE.DebugType_GENERAL.toInt()))
				{
					Logging.error("ReportEngine:: Could not load simulation results for pfolio: "
							+ Ref.getName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, portfolioList.get(i)) + "(" + portfolioList.get(i) + ")"
							+ ", sim type: " + runType + ", run date: " + OCalendar.formatJd(reportDate) + "\r\n");
					
					OConsole.message("ReportEngine:: Could not load simulation results for pfolio: "
							+ Ref.getName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, portfolioList.get(i)) + "(" + portfolioList.get(i) + ")"
							+ ", sim type: " + runType + ", run date: " + OCalendar.formatJd(reportDate) + "\r\n");					
				}
			}
		}
	}
	
	/**
	 * Generate a list of portfolios to process - currently, all portfolios are selected
	 * @return
	 * @throws OException
	 */
	private Vector<Integer> generatePortfolioList() throws OException
	{		
		Table tblData = Table.tableNew();
	
		int ret = DBaseTable.execISql(tblData, "SELECT * from portfolio");

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: SELECT * from portfolio");
		}   
		
		Vector<Integer> portfolioList = new Vector<Integer>(); 
		
		for (int row = 1; row <= tblData.getNumRows(); row++)
		{
			int thisPfolio = tblData.getInt("id_number", row);			
			portfolioList.add(thisPfolio);
		}
		
		tblData.destroy();	
		
		
		return portfolioList;
	}
	
	/**
	 * Register a RefID-type data conversion for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @param refID
	 * @throws OException
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
	 *
	 * @param dataTable
	 * @param colName
	 * @throws OException
	 */
	protected void regDateConversion(Table dataTable, String colName) throws OException
	{
		/* If no parameter passed in, assume DMY */
		regDateConversion(dataTable, colName, DateConversionType.TYPE_DMY);

	}

	
	/**
	 * Register the need for date formatting on given column for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @param type
	 * @throws OException
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
	 * Register a Table-type data conversion for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @param tableQuery
	 * @throws OException
	 */
	protected void regTableConversion(Table dataTable, String colName, String tableQuery) throws OException
	{    	
		TableConversionData convData = new TableConversionData();

		dataTable.addCol(colName + "_str", COL_TYPE_ENUM.COL_STRING);

		convData.m_colName = colName;
		convData.m_tableQuery = tableQuery;

		m_tableConversions.add(convData);
	}

	
	/**
	 * Perform data type conversions on the final output table according to registered requirements	 
	 *
	 * @param output
	 * @throws OException
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
	 * Generate custom output table format
	 * 
	 * @param sourceData
	 * @return 
	 * @return
	 * @throws OException
	 */
	protected abstract void generateOutputTableFormat(Table output) throws OException;
	
	/**
	 * Generate the data for the custom output table
	 * 
	 * @param sourceData
	 * @return 
	 * @return
	 * @throws OException
	 */
	protected abstract void populateOutputTable(Table output) throws OException;
	
	/**
	 * Register any ref-format column conversions
	 * 
	 * @param output
	 * @throws OException
	 */
	protected abstract void registerConversions(Table output) throws OException;   
	
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