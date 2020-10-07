/*$Header: /cvs/master/olf/plugins/standard/report/STD_SavedQueryParam.java,v 1.9 2012/05/15 15:20:12 dzhu Exp $*/
/*
File Name:                      STD_SavedQueryMarketParam.java

Author:                         Melinda Chin

Revision History:     Apr 25, 2012 -DTS94239: Replaced Ref.getModuleId() with Util.canAccessGui().
                               Sep 23, 2010 - Wrapped function calls to DBaseTable.execISql with a try-catch block
                                Aug 11, 2010 - Replaced function calls to DBaseTable.loadFromDB* with calls to DBaseTable.execISql
                                             - Replaced function calls to the OpenJVS String library with calls to the Java String library
                                             - Replaced function calls to Util.exitFail with throwing an OException
                                Jun 30, 2009 - richg
                                   DTS 51082
                                   Remove UPDATE_Browser, UPDATE_CreateTableFromBrowser, UPDATE_CreateTableFromQuery 
                                   calls - these are being deprecated.
                                May 12, 2005 - Removed Market Data Code from script
                                Oct 11, 2004 - updated documentation header
                                Oct 04, 2004 - added m_INCStandard.WORKFLOW flag
                                Sep 28, 2004 - fixed Report Manager Param names
                                Aug 18, 2004 - removed Util.exitFail(), fixed report manager check

Recommended Script Category? 	N/A

Script Description:   
Similar to STD_Adhoc_Parameter.java
Prepares argt for running a simulation based on the deals in a saved query
This script will NOT gather Market Data, use STD_SavedQueryMarketParam if Market Data is needed.
This param script will also accept parameters from the Report Manager when appropriately configured. 

Report Manager Instructions:
   Must have Saved Query field named "Saved Query" 

Parameters:
param name      type            description
==========      ====            ===========
query_name      String          named of Saved Query to be used (Case Sensitive)


Format of argt:
col name         col type        description 
========         ========        ===========
workflow         int             1 when run from a workflow, else 0

QueryId          int             query ID (query_db_id) in batch_sim_defn

Currency         int             currency associated with the portfolio (currency in portfolio_default_cash table). Use Ref.getLocalCurrency()

Portfolio        int             portfolio associated with the simulation (pfolio in sim_header table)

SimRunId         int             simulation run id (sim_run_id in sim_header table)

SimDefId         int             simulation definition id (sim_def_id in sim_header table)

RunType          int             simulation run type (run_type in sim_header table)

ClientPid        int             Client Process ID (for example, ID number in MS Task Manager) this is not used

QueryName        String          query name (query in query_rec table)

RefreshMktd      int             indicates whether market data is refreshed before simulation run, equivalent to Trading Mgr > Misc > Refresh Indexes. 

UseClose         int             indicates whether to use market close prices

ClearCache       int             indicates whether the cache should be cleared after the simulation run (1 means to clear the cache)

MarketData       table           market data

SimulationDef    table           simulation definition (name in sim_def)

UseMarketPrices  int             indicates whether to use market prices

AsmId            int             the services manager ID (not used)

update_criteria  table           query criteria defined in the specified saved query

 */
package standard.report;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class STD_SavedQueryParam_Mature implements IScript {

	private JVS_INC_Standard m_INCStandard;

	public STD_SavedQueryParam_Mature(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		/************** USER CONFIGURABLE PARAMETER***************************/
		String query_name = "My Query";
		/*********************************************************************/
		int accessGui = Util.canAccessGui();
		int query_id, i, numRows, run_id = 0;

		String error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + "STD_SavedQueryParam_Mature");
		String errorMessages = "";
		String strReportName, str_temp, str_temp_upper, strName=null, strQueryName;

		Table temp;
		int flag=0;
		m_INCStandard.Print(error_log_file, "START", "*** Start of Param Script - STD_SavedQueryParam ***");

		argt.addCol( "workflow", COL_TYPE_ENUM.COL_INT);
		if(argt.getNumRows() < 1) argt.addRow();

		if(accessGui == 0)
			argt.setInt( "workflow", 1, 1);
		else
			argt.setInt( "workflow", 1, 0);

		Sim.createRevalTable(argt);

		if(argt.getColNum( "out_params") <= 0) { // Not Running in Report Manager 
			m_INCStandard.Print(error_log_file, "INFO", "Not using Report Manager");

			query_id = Query.run(query_name);
			if(query_id <= 0) {
				errorMessages = "Query Name: " + query_name + " doesn't exist";
				m_INCStandard.Print(error_log_file, "ERROR", errorMessages);
				if(accessGui != 0){
					Ask.ok( errorMessages );
					m_INCStandard.Print(error_log_file, "END", "*** End of Param Script - STD_SavedQueryParam ***\n");
					throw new OException( errorMessages );
				}
				//goto end;
				flag=1;
			}
			else
			{    
				argt.setString( "QueryName", 1, query_name);
			}

		}else { // Running OpenJvs Task in ReportManager
			m_INCStandard.Print(error_log_file, "INFO", "Using Report Manager");

			strReportName = argt.getString( "report_name", 1);

			temp = argt.getTable( "inp_params", 1);
			if(Table.isTableValid(temp)==1)
			{
				temp = temp.copyTable();
				numRows = temp.getNumRows();    
			}     
			else 
				numRows = 0;

			for(i = 1; i <= numRows; i++)
			{
				str_temp = temp.getString( 1, i);
				str_temp_upper = str_temp.toUpperCase();
				if( str_temp_upper.contains("SAVE") || str_temp_upper.contains("QUERY") ){
					strName = str_temp;
					i = numRows + 1;
				}
			}

			if(Table.isTableValid(temp) == 1)
				temp.destroy();

			strQueryName = RptMgr.getArgList(argt, strReportName, strName).getString("value", 1);
			if( strQueryName == null || strQueryName.isEmpty() ){
				errorMessages = "Script requires Saved Query Pick List:  'Saved Query'";
				m_INCStandard.Print(error_log_file, "ERROR", errorMessages);
				m_INCStandard.Print(error_log_file, "END", "*** End of Param Script - STD_SavedQueryParam ***\n");
				Ask.ok( errorMessages );
				throw new OException( errorMessages );
			}

			query_id = Query.run(strQueryName);
			argt.setString( "QueryName", 1, strQueryName);
		} 
		if(flag==0)
		{     
			temp = Table.tableNew();
			try{
				DBaseTable.execISql( temp, "SELECT query_result.query_result FROM query_result WHERE query_result.unique_id = " + query_id );
			}
			catch( OException oex ){
				m_INCStandard.Print(error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
			}
			
			numRows = temp.getNumRows();
			temp.destroy();

			if(numRows < 1){
				errorMessages = "No Transactions in Query";
				m_INCStandard.Print(error_log_file, "ERROR", errorMessages);
				if(accessGui != 0){
					Ask.ok( errorMessages);
					m_INCStandard.Print(error_log_file, "END", "*** End of Param Script - STD_SavedQueryParam ***\n");
					throw new OException( errorMessages );
				}
			}
		} 

		Sim.createRevalTable(argt);

		argt.setInt( "QueryId",  1, query_id);
		argt.setInt( "SimRunId", 1, run_id);
		argt.setInt( "RunType",  1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());

		m_INCStandard.Print(error_log_file, "END", "*** End of Param Script - STD_SavedQueryParam ***\n");
	}

}
