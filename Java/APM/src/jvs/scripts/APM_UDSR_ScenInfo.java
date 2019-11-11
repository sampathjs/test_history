/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                      APM_UDSR_ScenInfo.mls
 
Report Name:                    NONE 
 
Output File Name:               NONE
 
Author:                         Maksim Stseglov
Creation Date:                  Dec 10, 2008
 
Revision History:               10-Mar-2014 - Converted from AVS to OpenJVS
                                                
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    APM Scen Info result, generates a single row of data, and a column per each property in "Scenario Properties" group
                                
Assumptions:                    
 
Instructions:                                          
  
Uses EOD Results?
 
Which EOD Results are used?
 
When can the script be run?  
*/ 
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_ScenInfo implements IScript {
 
int gToday;

// *****************************************************************************
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   int operation;

   operation = argt.getInt( "operation", 1);

   if (operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
         compute_result(argt.getTable( "sim_def", 1), argt.getInt( "scen_id", 1), returnt);
   else if (operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
         format_result();
   
   Util.exitSucceed();
} 

void LogDebugMessage(String sProcessingMessage) throws OException
{
   String msg;
   msg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + " : " + sProcessingMessage + "\n";
   OConsole.oprint( msg);      
}

// *****************************************************************************
void compute_result(Table sim_def, int scenarioID, Table returnt) throws OException
{
   Table    tListResultTypes, tResConfig=null, tValuesTable, tScenarioIDs;
   int         iResultRow=0, iResultID=0;
   int         retval, i, iRow;
   String      sName, sValue, sCachedTableName, sWhere, scenarioName;

   retval = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();

   /* Prepare the result parameters table - this is our data source for populating the return table */
   sCachedTableName = "Pfolio Result Attrs";
   tListResultTypes = Table.getCachedTable(sCachedTableName);
   if(Table.isTableValid(tListResultTypes) == 0)
   {
      tListResultTypes = Table.tableNew();
      retval = DBase.runSql("select * from pfolio_result_attr_groups");
      if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed" ) );
      else
      {
         retval = DBase.createTableOfQueryResults(tListResultTypes);
         if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.createTableOfQueryResults() failed" ) );
         else
            Table.cacheTable(sCachedTableName, tListResultTypes);
      }
   }

   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
   {
      OConsole.oprint("\nUser Result Error:\n\tProblem when retrieving result attribute groups\n\n");
      throw new OException("Error: Problem when retrieving result attribute groups.");
   }

   iResultRow = tListResultTypes.unsortedFindString( "res_attr_grp_name", "APM Scenario Properties", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
   if (iResultRow > 0)
      iResultID = tListResultTypes.getInt( "res_attr_grp_id", iResultRow);

   if (iResultID > 0)
       tResConfig = SimResult.getResultConfig(iResultID);   
   
   if (tResConfig != null)
       tResConfig.sortCol( "res_attr_name");

   /* We always populate the returned table with "Scenario Date" and its sort order, scenario ID and dataset type */
   returnt.addCols( "S(scenario_date) I(scenario_date_sort) I(scenario_id)");

   returnt.addRow();

   returnt.setColValString( "scenario_date", OCalendar.formatDateInt(OCalendar.today()));
   returnt.setColValInt( "scenario_date_sort", OCalendar.today());

   /* Add scenario ID from the APM scenarios here */
   if (sim_def.getColNum( "APM Scenario IDs") > 0)
   {
      scenarioName = sim_def.getTable( "scenario_def", 1).getString("scenario_name", scenarioID);

      tScenarioIDs = sim_def.getTable( "APM Scenario IDs", 1);
      iRow = tScenarioIDs.findString( "scenario_name", scenarioName, SEARCH_ENUM.FIRST_IN_GROUP);
      if (iRow > 0 )
         returnt.setColValInt( "scenario_id", tScenarioIDs.getInt( 1, iRow));
      else
         returnt.setColValInt( "scenario_id", scenarioID);  
   }
   else
   {
      returnt.setColValInt( "scenario_id", scenarioID);  
   }
   
   /* Iterate over all parameters, and populate the returns table with each */
   for (i = 1; i <= tResConfig.getNumRows(); i++)
   {
      sName = tResConfig.getString( "res_attr_name", i);
      sValue = tResConfig.getString( "value", i);

      /* Check for sort order being available - we cache these tables for efficiency */
      sCachedTableName = "APM Sim Property " + sName;

      tValuesTable = Table.getCachedTable(sCachedTableName);

      if(Table.isTableValid(tValuesTable) == 0)
      {
         /* If we haven't cached it yet, load up the data from USER_APM_UserFilterValues */
         tValuesTable = Table.tableNew(sCachedTableName);

         sWhere = "filter = '" + sName + "'";

         DBaseTable.loadFromDbWithSQL(tValuesTable, "id, sort_order", "USER_APM_UserFilterValues", sWhere);

         tValuesTable.sortCol( "id");

         Table.cacheTable(sCachedTableName, tValuesTable);
      }

      /* Convert the property name to lowercased, no blanks, so it can be used as a column name in Endur table */
      sName = sName.toLowerCase();
      sName = sName.replaceAll("\\s+", "");  /* stripBlanks */

      returnt.addCol( sName, COL_TYPE_ENUM.COL_STRING);
      returnt.setColValString( sName, sValue);

	  String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	  if (envVar != null)
	  {
		  envVar = envVar.toUpperCase();
	    
		  if (envVar.equals("TRUE"))
		  {   
			returnt.clearGroupBy ();
			returnt.addGroupBy ("scenario_id");
			returnt.groupBy ();
		  }
	  }  	
      /* If there are entries in the sort order table, then add the correct one as well */
      else if (tValuesTable.getNumRows() > 0)
      {
         returnt.addCol( sName + "_sort", COL_TYPE_ENUM.COL_INT);

         iRow = tValuesTable.findString( "id", sValue, SEARCH_ENUM.FIRST_IN_GROUP);

         if (iRow > 0)
         {
            returnt.setColValInt( sName + "_sort", tValuesTable.getInt( "sort_order", iRow));
         }
         else
         {
            /* Use one for values that are not present in the sort order table */
            returnt.setColValInt( sName + "_sort", 1);
         }
      }
      else
      {
         /* Use one for values that are not present in the sort order table */
         returnt.setColValInt( sName + "_sort", 1);
      }
   }   
   
} 


// *****************************************************************************
void format_result() throws OException
{   
} // format_result/0.


}

