   /*$Header: /cvs/master/olf/plugins/standard/report/STD_GreeksQueryByPfolio_Param.java,v 1.10 2013/04/19 18:00:25 rdesposi Exp $*/
/*
File Name:                      STD_GreeksQueryByPfolio_Param.java

Author:                         Lidia Brzozowski
Date Created:                   09/12/2001

Revision History:               Jan 23, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                Apr 25, 2012 -DTS94239: Replaced Ref.getModuleId() with Util.canAccessGui().
                                Mar 05, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
	                            Jul 17, 2010 - Replaced Util.exitFail w/ throw OException
                                             - Replaced loadFromDb* w/ execISql
                                             - Replaced OpenLink String lib w/ standard Java String functions
                                Jun 09, 2005 - Add INC_Standard and Set ASK_WINDOW default value to 1
                                May 13, 2005 - Removed Market Data Gathering logic
                                Apr 14, 2005 - Added workflow mode
                                Feb 10, 2005 - Configured param script to run with Report Manager

Main Script:                    STD_Greeks.java
Parameter Script:               This is a parameter script
Display Script:                 STD_Display.java

Script Description:
An Ask box prompts the user for portfolio(s) and report(s).

Recommended Script Category? 	N/A

Assumption:
1. Report will run only for portfolios selected from the pick list table when ASK_WINDOW = 1
2. Report will run for ALL portfolios when ASK_WINDOW = 0 and EDIT_PFOLIOS = 0
3. Report will run only for portfolios specified by user in the 'portfolios' table when ASK_WINDOW = 0 and EDIT_PFOLIOS = 1

Instruction:
User will run the associated task and will select the desired portfolio(s) and report(s).

Report Manager Instructions:
   Must have SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE named "Portfolios"
   Must have STD_Greek_Picklist Named "Report Types"

Parameters:
param name      type         description
==========      ====         ===========
ASK_WINDOW      int          Set to 1 to generate Interactive Pop-up window
                             Set to 0 to run with default values specified in script

EDIT_PFOLIOS    int          Set to 1 to run only for portfolios specified in the Table 'portfolios'
                             Set to 0 to run for ALL available portfolios

RUN_DELTA       int          Set to 1 to generate Delta Report, 0 to suppress report

RUN_GAMMA       int          Set to 1 to generate Gamma Report, 0 to suppress report

RUN_VEGA        int          Set to 1 to generate Vega  Report, 0 to suppress report

RUN_THETA       int          Set to 1 to generate Theta Report, 0 to suppress report

portfolios      Table        Table of Portfolios (User can add/remove rows to specify portfolios when ASK_WINDOW = 0 and EDIT_PFOLIOS = 1)


Format of argt:
col name         col type        description
========         ========        ===========
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

run_report       table           Table of Report Types to be run /format: return_val(int), ted_str_value(String)

workflow         int             1 when run from a workflow, else 0
*/
package standard.report;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class STD_GreeksQueryByPfolio_Param implements IScript {
private JVS_INC_Standard m_INCStandard;
 public STD_GreeksQueryByPfolio_Param(){
	m_INCStandard = new JVS_INC_Standard();

 }

public void execute(IContainerContext context) throws OException
{
	Table argt = context.getArgumentsTable();

   /******************************* USER CONFIGURABLE SECTION *****************************/

      int ASK_WINDOW = 1;        // Set to 1 to generate interactive pop-up window
   // int ASK_WINDOW = 0;          // Set to 0 to use only Portfolios specified below

   // int EDIT_PFOLIOS = 1;      // Set to 1 to run for only the portfolios specified in the section below
      int EDIT_PFOLIOS = 0;        // Set to 0 to run for ALL available portfolios

      int RUN_DELTA = 1;            // Set to 1 to generate Delta Report
   // int RUN_DELTA = 0;

      int RUN_GAMMA = 1;            // Set to 1 to generate Gamma Report
   // int RUN_GAMMA = 0;

      int RUN_VEGA  = 1;            // Set to 1 to generate Vega  Report
   // int RUN_VEGA  = 0;

      int RUN_THETA = 1;            // Set to 1 to generate Theta Report
   // int RUN_THETA = 0;

   /*************************************************************************************/
   String sql, queryTableName;
   int queryId;
   String username, what, where, from, strReportName, str_temp, str_temp_upper, strPfolio=null,strReport=null;
   String sFileName = "STD_GreeksQueryByPfolio_Param";
   String error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);
   int personnel_id, retval, query_id, run_id = 0, numRows, i;

   Table ask_table, pfolio_table, sim_result, portfolios=Util.NULL_TABLE, results=Util.NULL_TABLE, tran_list, temp;

   m_INCStandard.Print(error_log_file, "START", "*** Start of param script - " + sFileName + " ***");

   Sim.createRevalTable(argt);
   argt.addCol( "run_report", COL_TYPE_ENUM.COL_TABLE);
   argt.addCol( "workflow", COL_TYPE_ENUM.COL_INT);

   if(argt.getNumRows() < 1) argt.addRow();

   if (Util.canAccessGui() == 0) {
      ASK_WINDOW = 0;
      argt.setInt( "workflow", 1, 1);
   }else {
      argt.setInt( "workflow", 1, 0);
   }

   if(argt.getColNum( "out_params") <= 0) { // Not Running in Report Manager
      m_INCStandard.Print(error_log_file, "INFO", "Not Using Report Manager");

      if(ASK_WINDOW == 0){
         if(EDIT_PFOLIOS != 0){
            portfolios = Table.tableNew();
            portfolios.addCol( "portfolio", COL_TYPE_ENUM.COL_INT);

            /********************************************* USER CONFIGURABLE SECTION **********************************/
            /* Copy/Delete the following lines to Add/Remove Portfolios from the list                              */

               portfolios.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, "Portfolio 1")));
               portfolios.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, "Portfolio 2")));
               portfolios.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, "Portfolio 3")));
            /**********************************************************************************************************/
         }else{
            portfolios = Table.tableNew();
            sql = "SELECT id_number FROM portfolio WHERE id_number > 0";

            try
            {
            	DBaseTable.execISql(portfolios, sql);
            }
            catch(OException oex)
            {
            	portfolios.destroy();
            	m_INCStandard.Print(
            			error_log_file,
            			"ERROR",
            			oex.getMessage());

            	throw oex;
            }
         }

         results = Table.tableNew();
         results.addCol( "return_val",    COL_TYPE_ENUM.COL_INT);
         results.addCol( "ted_str_value", COL_TYPE_ENUM.COL_STRING);

         if(RUN_DELTA != 0)
            results.addRowsWithValues( Integer.toString(1) + ", (Delta)");
         if(RUN_GAMMA != 0)
            results.addRowsWithValues( Integer.toString(2) + ", (Gamma)");
         if(RUN_VEGA != 0)
            results.addRowsWithValues( Integer.toString(3) + ", (Vega)");
         if(RUN_THETA != 0)
            results.addRowsWithValues( Integer.toString(4) + ", (Theta)");

      }else{

         /* get the personnel_id of the user running this script*/
         username = Ref.getUserName();
         personnel_id = Ref.getValue(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, username);

         /* get the portfolios for which this person is authorized (at least read access)*/
         pfolio_table = Table.tableNew();
         sql = "SELECT portfolio_id FROM portfolio_personnel WHERE personnel_id = " + personnel_id;

         try
         {
         	DBaseTable.execISql(pfolio_table, sql);
         }
         catch(OException oex)
         {
         	pfolio_table.destroy();
         	m_INCStandard.Print(
         			error_log_file,
         			"ERROR",
         			oex.getMessage());

         	throw oex;
         }

         pfolio_table.setColFormatAsRef( "portfolio_id", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
         pfolio_table.groupFormatted( "portfolio_id");

         sim_result = Table.tableNew();
         sim_result.addCol( "sim_id", COL_TYPE_ENUM.COL_INT );
         sim_result.addCol( "Name", COL_TYPE_ENUM.COL_STRING );
         sim_result.addRowsWithValues( "1, (Delta)");
         sim_result.addRowsWithValues( "2, (Gamma)");
         sim_result.addRowsWithValues( "3, (Vega)");
         sim_result.addRowsWithValues( "4, (Theta)");

         sim_result.colHide( "sim_id");

         ask_table = Table.tableNew();
         Ask.setAvsTable( ask_table, pfolio_table, "Select Portfolio(s)", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, Util.NULL_TABLE, "Right Click To Select Portfolio(s)", 1 );
         Ask.setAvsTable( ask_table, sim_result, "Select Result(s)", 2, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, Util.NULL_TABLE, "Right Click To Select Result(s)", 1 );

         retval = Ask.viewTable( ask_table, "STD Greeks by Index", "Select the Indicated Parameters\n\nPlace Mouse on Input Areas for More Details" );
         if(retval != 1 || ask_table.getColNum( "return_value" ) < 1)
         {
            ask_table.destroy();
            pfolio_table.destroy();
            sim_result.destroy();
            throw new OException("User clicked Cancel");
         }

         portfolios = ask_table.getTable( "return_value", 1 ).copyTable();
         results = ask_table.getTable( "return_value", 2 ).copyTable();

         sim_result.destroy();
         ask_table.destroy();
         pfolio_table.destroy();
      }
   }else{// Running OpenJvs Task in ReportManager
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
          if(str_temp_upper.indexOf("FOLIO") != -1){
             strPfolio = str_temp;
          }else if(str_temp_upper.indexOf("REPORT") != -1){
             strReport = str_temp;
          }
      }

    if(Table.isTableValid(temp) == 1)
      temp.destroy();

      if(strPfolio.isEmpty()){
         m_INCStandard.Print(error_log_file, "ERROR", "Script requires SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE Picklist named 'Portfolios'");
         throw new OException("Script requires SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE Picklist named 'Portfolios'");
      }else{
         portfolios = Table.tableNew();
         portfolios.select( RptMgr.getArgList(argt, strReportName, strPfolio), "id, value", "id GT -1");
      }

      if(strReport.isEmpty()){
         m_INCStandard.Print(error_log_file, "ERROR", "Script requires STD_Greeks_Picklist named 'Report Types'");
         throw new OException("Script requires STD_Greeks_Picklist named 'Report Types'");
      }else{
         results = Table.tableNew();
         results.addCol( "return_val", COL_TYPE_ENUM.COL_INT);
         results.addCol( "ted_str_value", COL_TYPE_ENUM.COL_STRING);
         results.select( RptMgr.getArgList(argt, strReportName, strReport), "id(return_val), value(ted_str_value)", "id GT -1");
      }
   }

   portfolios.setColName( 1, "internal_portfolio");
   queryId = Query.tableQueryInsert(portfolios, "internal_portfolio", "query_result_plugin");

   tran_list = Table.tableNew();
   String tranStatusInList =
   	TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + ", " +
   	TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
   queryTableName = Query.getResultTableForId(queryId);
   if ( queryTableName == null && queryId > 0 )
   {
	   queryTableName = "query_result_plugin";
	   m_INCStandard.Print(error_log_file, "ERROR", "Query id " + queryId
			   + " does not have a query result table. Default " + queryTableName + " table will be used.");
   }
   sql =
   	"SELECT" +
   	"	tran_num " +
   	"FROM " +
   	"	ab_tran" +
   	"	," + queryTableName + " " +
   	"WHERE " +
   	"	internal_portfolio = query_result " +
   	"AND " +
   	"	unique_id = " + queryId + " " +
   	"AND " +
   	"	current_flag = 1 " +
   	"AND " +
   	"	trade_flag = 1 " +
   	"AND " +
   	"	tran_status IN(" + tranStatusInList + ")";

   try
   {
   	DBaseTable.execISql(tran_list, sql);
   }
   catch(OException oex)
   {
   	tran_list.destroy();
   	m_INCStandard.Print(
   			error_log_file,
   			"ERROR",
   			oex.getMessage());

   	throw oex;
   }
   finally
   {
   	Query.clear(queryId);
   }

   query_id = Query.tableQueryInsert(tran_list, "tran_num");

   argt.setInt( "QueryId", 1, query_id);
   argt.setInt( "SimRunId", 1, run_id);
   argt.setInt( "RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());

   argt.setTable( "run_report", 1, results.copyTable());

   tran_list.destroy();
   portfolios.destroy();
   results.destroy();

   m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");

}

}



