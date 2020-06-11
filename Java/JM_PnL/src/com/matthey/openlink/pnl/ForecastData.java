package com.matthey.openlink.pnl;

import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_DOUBLE;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.PFOLIO_RESULT_TYPE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
public class ForecastData implements IScript {
	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Reports";

	/**
	 * Specifies the constants' repository sub-context parameter.
	 */
	protected static final String REPO_SUB_CONTEXT = "Accounts";
	// column names
		private static final String COL_DEAL_NUMBER = "deal_num";
		private static final String COL_PYMT_DATE = "pymt_date";
		private static final String COL_CURRENCY_ID = "currency_id";
		private static final String COL_PYMT = "pymt";

	// column caption
		private static final String COL_DEAL_NUMBER_CAPTION = "Deal_Number";
		private static final String COL_PYMT_DATE_CAPTION = "Payment_Date";
		private static final String COL_CURRENCY_ID_CAPTION = "currency_id";
		private static final String COL_PYMT_CAPTION = "Payment";

		private String maxReportingDate;

		private String queryName;


	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		setupLog();
		int modeFlag = argt.getInt("ModeFlag", 1);
		PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for Forecast Reports - mode: " + modeFlag);

		if (modeFlag == 0) {
			
			/* Add the Table Meta Data */
			Table pluginMetadata = argt.getTable("PluginMetadata", 1);
			Table tableMetadata = pluginMetadata.getTable("table_metadata", 1);
			Table columnMetadata = pluginMetadata.getTable("column_metadata", 1);
			//Table joinMetadata = pluginMetadata.getTable("join_metadata", 1);

			tableMetadata.addNumRows(1);
			tableMetadata.setString("table_name", 1, getClass().getSimpleName());
			tableMetadata.setString("table_title", 1, getClass().getSimpleName());
			tableMetadata.setString("table_description", 1, getClass().getSimpleName() + " Data Source: ");
			//tableMetadata.setString("pkey_col1", 1, Columns.ACCOUNT_ID.getColumn());

			/* Add the Column Meta Data */

			addColumnsToMetaData(columnMetadata);
			
			

			/* Add the JOIN Meta Data */

			PluginLog.debug("Completed Data Load Script Metadata:");

			return;
		} else {

			processAdhocData(returnt);
			if (modeFlag == 0) {
				return;
			}
		
		
	}
	}
	
	private void setupLog() throws OException {
		
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";


		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, REPO_SUB_CONTEXT);
		String logLevel = constRepo.getStringValue("logLevel","DEBUG");
		String logFile =  this.getClass().getSimpleName()+".log";
		String logDir = constRepo.getStringValue("logDir", abOutDir);;

		try {

			PluginLog.init(logLevel, logDir, logFile);

		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

	private void addColumnsToMetaData(Table tableCreate) throws OException {

		for (Columns column : Columns.values()) {
			addColumnToMetaData(tableCreate, column.getColumn(), column.getTitle(), column.getNameType(), column.getColumnCaption());
		}
	}

	private void addColumnToMetaData(Table columnMetadata, String colColumnName, String colColumnCaption, String columnType, String detailedCaption) throws OException {
		
		int rowAdded = columnMetadata.addRow();
		columnMetadata.setString("table_name", rowAdded, "generated_values");
		columnMetadata.setString("column_name", rowAdded, colColumnName);
		columnMetadata.setString("column_title", rowAdded, colColumnCaption);
		columnMetadata.setString("olf_type", rowAdded, columnType);
		columnMetadata.setString("column_description", rowAdded, detailedCaption);
	}

	protected void processAdhocData(Table returnt) throws OException
	{	
		ConstRepository _constRepo = new ConstRepository(REPO_CONTEXT, "Forecast Report");
		this.queryName = _constRepo.getStringValue("QueryName");
		if (this.queryName == null || "".equals(this.queryName)) {
			throw new OException("Ivalid query name  in Const Repository");
		}
		
		int queryId = Query.run(queryName);
		Table finalReportData = Util.NULL_TABLE;
		finalReportData= Table.tableNew();
        Table tblSim = Sim.createSimDefTable();
        Sim.addSimulation(tblSim, "Sim");
        Sim.addScenario(tblSim, "Sim", "Base", Ref.getLocalCurrency());
        
        
        /* Build the result list */
        Table tblResultList = Sim.createResultListForSim();
        SimResult.addResultForSim(tblResultList,SimResultType.create(SimResult.getResultEnumFromId(253)));
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
		Table simResults = Sim.runRevalByParamFixed(revalTable);    		

		Query.clear(queryId);
			
		if (Table.isTableValid(simResults) == 1)
		{     		   
			PluginLog.info("ReportEngine:: Processing ad-hoc simulation results...\n");
			OConsole.message("ReportEngine:: Processing ad-hoc simulation results...\n");

			Table genResults = SimResult.getGenResults(simResults, 1);
				
			if (Table.isTableValid(genResults) == 1)
			{
				Table rawPnlData = SimResult.findGenResultTable(genResults, PFOLIO_RESULT_TYPE.PNL_DETAIL_RESULT.toInt(), 0, 0, 0);
				
				
				if (Table.isTableValid(rawPnlData) == 1)
				{
					
					finalReportData.select(rawPnlData,  "deal_num,pymt_date,pymt,currency_id" , "deal_num GT 0 and deal_leg GT 0");
				}							
				
			
			}
			Table leaseDealsData = getLeaseData();
			if (Table.isTableValid(leaseDealsData) == 1){
				finalReportData.select(leaseDealsData, "*","deal_num GT 0");
				leaseDealsData.destroy();
			}
		
			
			this.maxReportingDate = _constRepo.getStringValue("maxReportingDate");
			if (this.maxReportingDate == null || "".equals(this.maxReportingDate)) {
				throw new OException("Ivalid maxReportingDate in Const Repository");
			}
			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			int Curr_JulianDate = extractDateTime.getDate();

			int jdConvertDate = OCalendar.parseStringWithHolId(maxReportingDate,0,Curr_JulianDate);
			if (finalReportData.getNumRows()> 0){
				returnt.select(finalReportData, "*","pymt_date GE "+Curr_JulianDate+" and pymt_date LE "+jdConvertDate+ " currency EQ 0");
			}
			simResults.destroy();		
		}	
	}
	private Table getLeaseData() throws OException {
		Table dealData = Util.NULL_TABLE;
		dealData = Table.tableNew();
		try{
		String sql = "SELECT deal_tracking_num  as deal_num, CAST(abe.event_date as INT) as pymt_date,para_position as pymt, abe.currency as currency_id FROM ab_tran ab \n" 
						+"LEFT JOIN ab_tran_event abe \n"
					 	 +"ON ab.tran_num = abe.tran_num \n"
						+"WHERE ab.ins_type in ("+INS_TYPE_ENUM.multileg_loan.toInt()+ ","+INS_TYPE_ENUM.multileg_deposit.toInt() +") \n"
						 +"AND  abe.event_date >= GETDATE() \n"
						 +"AND abe.event_type =" +EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt() +" \n"
						 +"AND abe.currency = 0";
		
		PluginLog.info("Query to be executed: " + sql);
		int ret = DBaseTable.execISql(dealData, sql);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchTPMfailure "));
		}
		}catch (OException exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return dealData;
		
		
	}
	/**
	 * @param returnt - Table to return to report
	 * @throws OException - Error, cannot format table
	 */
		
	protected enum Columns {
		DEAL_NUMBER(COL_DEAL_NUMBER, COL_DEAL_NUMBER_CAPTION,COL_INT,"Title Deal_number"){},
		PYMT_DATE(COL_PYMT_DATE, COL_PYMT_DATE_CAPTION,COL_TYPE_ENUM.COL_DATE,"Title Payment Date"){},
		CURRENCY_ID(COL_CURRENCY_ID, COL_CURRENCY_ID_CAPTION,COL_INT,"Title Currency ID"){},
		PAYMENT(COL_PYMT, COL_PYMT_CAPTION,COL_DOUBLE,"Title Payment"){};
		

		private String _name;
		private String _title;
		private COL_TYPE_ENUM _format;
		private String _columnCaption;

		private Columns(String name, String title, COL_TYPE_ENUM format, String columnCaption) 		{
			_name = name;
			_title = title;
			_format = format;
			_columnCaption = columnCaption;
		}

		public String getColumn() {
			return _name;
		}

		private String getTitle() {
			return _title;
		}

		private COL_TYPE_ENUM getType() {
			return _format;
		}

		private String getColumnCaption() {
			return _columnCaption;
		}

		public String getNameType() {
			String monthString = "";
			COL_TYPE_ENUM thisType = getType();
			switch (thisType)
			{
			case COL_INT:
				monthString = "INT";
				break;
			case COL_DOUBLE:
				monthString = "DOUBLE";
				break;
			case COL_STRING:
				monthString = "CHAR";
				break;
			case COL_DATE_TIME:
				monthString = "DATETIME";
				break;
			default:
				monthString = thisType.toString().toUpperCase();
				break;
			}

			return monthString;
		}
	}
}
