/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                      APM_UDSR_ServiceInfo.mls
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Maksim Stseglov 
Creation Date:                  Dec 10, 2008
 
Revision History:               10-Mar-2014 - Converted from AVS to OpenJVS
                                                
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    APM Service Info result, generates a single row of data, and a column for dataset type
                               

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
public class APM_UDSR_ServiceInfo implements IScript {

int gToday;

// *****************************************************************************
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   int operation;

   operation = argt.getInt( "operation", 1);

   if(operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
      compute_result(argt.getTable( "sim_def", 1), argt.getInt( "scen_id", 1), returnt);
   else if (operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
         format_result(returnt);
   
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
 
   /* The service properties follow: */
   returnt.addCols( "I(dataset_type) I(portfolio)");

   returnt.addRow();

   /* Add dataset type here */
   if (sim_def.getColNum( "APM Dataset Type ID") > 0)
   {
      returnt.setColValInt( "dataset_type", sim_def.getInt( "APM Dataset Type ID", 1));
   }
   else
   {
      returnt.setColValInt( "dataset_type", 0);  
   }

   /* Add portfolio here */
   if (sim_def.getColNum( "APM Portfolio ID") > 0)
   {
      returnt.setColValInt( "portfolio", sim_def.getInt( "APM Portfolio ID", 1));
   }
   else
   {
      returnt.setColValInt( "portfolio", 0);  
   }
   
	String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	if (envVar != null)
	{
		envVar = envVar.toUpperCase();
	  
		if (envVar.equals("TRUE"))
		{   
			returnt.clearGroupBy ();
			returnt.addGroupBy ("dataset_type");
			returnt.addGroupBy ("portfolio");
			returnt.groupBy ();
		}
	}     
   
} 


// *****************************************************************************
void format_result(Table returnt) throws OException
{   
   returnt.setColFormatAsRef( "portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
} 


}
