   /*$Header: /cvs/master/olf/plugins/standard/report/STD_Business_Unit_Param.java,v 1.14 2013/01/30 20:23:49 dzhu Exp $*/
/*==========================================================================================================================================

File Name:                      STD_Business_Unit_Param.java

Author:                         Melinda Chin

Revision History:               Jan 29, 2013 - Removed extra columns that may exist in argt table when running as a task from Desktop
                                Apr 25, 2012 -DTS94239: Replaced Ref.getModuleId() with Util.canAccessGui(). 
                                Mar 05, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
								Jul 01, 2010 - Replaced loadFromDbWithWhatWhere with DBaseTable.execISql
                                             - Replaced OpenLink String library with Java String functions
                                Jul 25, 2007 - DTS 29014 - update script to select only Internal Business Units
                                Dec 06, 2006 - DTS 28723 - fixed bug in workflow mode. Brought script up to current standards
                                Jun 09, 2005 - Add INC_Standard and Set ASK_WINDOW default value to 1
                                May 24, 2005 - Changed to Explicit Declaration
                                Aug 17, 2004 - Simplified Script, added option for ask window, fixed RM errors
                                Sep 28, 2004 - fixed Report Manager Param names
                                Oct 05, 2004 - added workflow flag
                                Oct 11, 2004 - updated documentation header

Main Script:                    None needed
Parameter Script:               This is a Parameter script
Display Script:                 None needed

Script Description:             This is a parameter script used to specify which Internal Business Units to use in a main script. This script can
                                generate an interactive pop up ASK window when ASK_WINDOW is set to 1. Otherwise, the script defaults to
                                use ALL Internal Business Units that are used by trades at a new or validated status. By setting EDIT_BUNITS
                                to 1, it will use only the selected Internal Business Units specified in script. This param script will
                                also accept parameters from the Report Manager when appropriately configured.

Assumptions:                    None

Instructions:                   To add or remove business units add or remove the business unit names.

Report Manager Instructions:    1. Setup a task using this script as the parameter script in the task editor.
                                2. Add the report to Report Manager that uses this OpenJvs task.
                                3. When adding the report, configure it with the SHM_USR_TABLES_ENUM.IBUNIT_TABLE standard pick list. The pick list label
                                   should be "Business Units".

Uses EOD Results?               False

Which EOD Results are used?     N/A

When can the script be run?     Anytime

Recommended Script Category? 	N/A

Parameters:
param name      type         description
==========      ====         ===========
ASK_WINDOW      int          1 - generates pop up ASK window for user input

EDIT_BUNITS     int          1 - uses only Business Units specified in script
                             0 - uses all Business Units with new/validated trades in database

argt            table        add/remove rows to/from this table when EDIT_BUNITS is 1 (see below)


Format of argt:
col name         col type        description
========         ========        ===========
bunit            int             list of business units to be used in main script (one ID per row)

workflow         int             1 when script is run from a workflow, else 0

==========================================================================================================================================*/

package standard.report;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class STD_Business_Unit_Param implements IScript {
private JVS_INC_Standard m_INCStandard;
 public STD_Business_Unit_Param(){
	m_INCStandard = new JVS_INC_Standard();

 }
public void execute(IContainerContext context) throws OException
{
	Table argt = context.getArgumentsTable();

	String sql;

   /********************************** USER CONFIGURABLE PARAMETERS ******************************************/
   /* Set ASK_WINDOW to 1 to pop up interactive ASK window, otherwise:                                   *
    *    Set EDIT_BUNITS to 0, runs with ALL Internal Business Units                                    *
    *    Set EDIT_BUNITS to 1,  runs only with Business Units specified in script                        */

    int ASK_WINDOW  = 1;
    // int ASK_WINDOW  = 0;

    // int EDIT_BUNITS = 1;
    int EDIT_BUNITS = 0;
   /********************************** USER CONFIGURABLE PARAMETERS ******************************************/

   int
      int_module_id=0,
      int_workflow_flag=0,
      int_num_inputs=0,
      int_loop_counter=0,
      int_return_value=0,
      int_exit_fail_flag = 0;

   String
      str_file_name      = "STD_Business_Unit_Param",
      str_error_log_file = Util.errorInitScriptErrorLog(str_file_name),
      str_where="",
      str_report_name="",
      str_temp="",
      str_temp_upper="",
      str_agr_name="",
      errorMessage = "";

   Table
      // freeables
      tbl_ask_window=Util.NULL_TABLE,
      tbl_bunits=Util.NULL_TABLE,

      // non-freeables
      tbl_selected_bunits_NF=Util.NULL_TABLE,
      tbl_inp_params_NF=Util.NULL_TABLE;

   m_INCStandard.Print(str_error_log_file, "START", "*** Start of param script - " + str_file_name + " ***");

   //=====================================================================================================================================//
   // Setup argt                                                                                                                          //
   //=====================================================================================================================================//
   
   //delete QueryId column if running from Desktop
   if (argt.getColNum("QueryId") > 0)
	   argt.delCol("QueryId");
   
   argt.addCol( "bunit",    COL_TYPE_ENUM.COL_INT);
   argt.addCol( "workflow", COL_TYPE_ENUM.COL_INT);


   if (Util.canAccessGui() == 0) 
   {
      int_workflow_flag = 1;
      ASK_WINDOW = 0;
   }
   else
      int_workflow_flag = 0;

   //=====================================================================================================================================//
   // Running OpenJvs Task from Trading Manager                                                                                               //
   //=====================================================================================================================================//
   output: {
   if (argt.getColNum( "out_params") < 1)
   {
      m_INCStandard.Print(str_error_log_file, "INFO", "Running as a task");

      if (ASK_WINDOW == 1)
      {
         /*** Create Pop Up Ask Window ***/

         /* Load all internal Business Units  */
         tbl_bunits = Table.tableNew();

         sql =
         	"SELECT" +
         	"		party_id" +
         	"		,short_name " +
         	"FROM " +
         	"		party " +
         	"WHERE " +
         	"		int_ext = " + CONF_INT_EXT.INTERNAL.toInt() +
         	"AND " +
         	"		party_class = 1 " +
         	"AND " +
         	"		party_status = 1 ";

         try
         {
         	DBaseTable.execISql(tbl_bunits, sql);
         }
         catch(OException oex)
         {
        	 tbl_bunits.destroy();
        	 m_INCStandard.Print(
        			str_error_log_file,
        			"ERROR",
        			oex.getMessage());

        	 throw oex;
         }

         tbl_bunits.colHide( 1);
         tbl_ask_window = Table.tableNew("Ask Window");

         int_return_value = Ask.setAvsTable(tbl_ask_window, tbl_bunits, "Business Units", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, Util.NULL_TABLE, "Please select the required Business Unit(s)", 1);

         if (int_return_value != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
         {
            Ask.ok ("An error occurred in the Parameter Script while initialising the ASK table. The report has been cancelled");
            m_INCStandard.Print(str_error_log_file, "ERROR", "An error occurred in the Parameter Script while initialising the ASK table. The report has been cancelled");
            int_exit_fail_flag = 1;
            errorMessage = "An error occurred in the Parameter Script while initialising the ASK table. The report has been cancelled";
            //goto SCRIPT_END;
            break output;
         }

         int_return_value = Ask.viewTable(tbl_ask_window, "Adhoc", "Please Complete the Following Fields");

         if (int_return_value == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
         {
            // user clicked OK
            tbl_selected_bunits_NF = tbl_ask_window.getTable( "return_value", 1);
            tbl_selected_bunits_NF.group( "return_val");
            argt.select( tbl_selected_bunits_NF, "return_val(bunit)", "return_val GT -1");
         }
         else
         {
            // user clicked Cancel
            m_INCStandard.Print(str_error_log_file, "INFO", "User has clicked cancel");
            int_exit_fail_flag = 1;
            errorMessage = "User has clicked cancel";
            break output; //goto SCRIPT_END;
         }
      }
      else if (ASK_WINDOW == 0)
      {
         m_INCStandard.Print(str_error_log_file, "INFO", "Interactive mode supressed");

         if(EDIT_BUNITS != 0)
         {
            m_INCStandard.Print(str_error_log_file, "INFO", "Running with only user specified business units");

            /********************* USER CONFIGURABLE SECTION ***************************/
            /* Fill in the Business Units in quotes below                              */
               argt.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT1")));
               argt.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT2")));
               argt.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT3")));
            /* repeat the above line for multiple business units                       */
            /********************* USER CONFIGURABLE SECTION ***************************/

            argt.deleteWhereValue( "bunit", 0);

            if (argt.getNumRows() < 1)
            {
               m_INCStandard.Print(str_error_log_file, "ERROR", "No valid business units entered by the user");
               int_exit_fail_flag = 1;
               errorMessage = "No valid business units entered by the user";
               break output; //goto SCRIPT_END;
            }
         }
         else
         {
            m_INCStandard.Print(str_error_log_file, "INFO", "Running with business units for new or validated deals");

            // Load all internal Business Units with new and validated deals
            tbl_bunits = Table.tableNew();

            sql =
            	"SELECT DISTINCT" +
            	"		internal_bunit " +
            	"FROM " +
            	"		ab_tran " +
            	"WHERE " +
            	"		tran_status IN(" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ") " +
            	"AND " +
            	"		trade_flag = 1";

            try
            {
            	DBaseTable.execISql(tbl_bunits, sql);
            }
            catch(OException oex)
            {
            	tbl_bunits.destroy();
	           	m_INCStandard.Print(
	         		str_error_log_file,
	         		"ERROR",
	         		oex.getMessage());

	           	throw oex;
            }

            argt.select( tbl_bunits, "internal_bunit(bunit)", "internal_bunit GT 0");
         }
      }
   }

   //=====================================================================================================================================//
   // Running OpenJvs Task in Report Manager                                                                                                  //
   //=====================================================================================================================================//

   else if (argt.getColNum( "out_params") > 0)
   {
      m_INCStandard.Print(str_error_log_file, "INFO", "Running with Report Manager");

      str_report_name   = argt.getString( "report_name", 1);
      tbl_inp_params_NF = argt.getTable( "inp_params",  1);
      int_num_inputs    = tbl_inp_params_NF.getNumRows();

      for (int_loop_counter = 1; int_loop_counter <= int_num_inputs; int_loop_counter++)
      {
         str_temp       = tbl_inp_params_NF.getString( "arg_name", int_loop_counter);
         str_temp_upper = str_temp.toUpperCase();

         if (str_temp_upper.indexOf("BUSINESS") != -1 || str_temp_upper.indexOf("BUNIT") != -1)
         {
            str_agr_name = str_temp;
            break;
         }
      }

      if (str_agr_name.isEmpty())
      {
         m_INCStandard.Print(str_error_log_file, "ERROR", "Script requires use of pick list 'SHM_USR_TABLES_ENUM.IBUNIT_TABLE' with the label 'Business Units'");
         int_exit_fail_flag = 1;
         errorMessage = "Script requires use of pick list 'SHM_USR_TABLES_ENUM.IBUNIT_TABLE' with the label 'Business Units'";
         break output; //goto SCRIPT_END;
      }
      else
      {
         tbl_selected_bunits_NF = RptMgr.getArgList(argt, str_report_name, str_agr_name);

         if (tbl_selected_bunits_NF.getNumRows() > 0)
            tbl_selected_bunits_NF.copyCol( "id", argt, "bunit");
         else
         {
            m_INCStandard.Print(str_error_log_file, "ERROR", "No business units were selected");
            int_exit_fail_flag = 1;
            errorMessage = "No business units were selected";
            break output; //goto SCRIPT_END;
         }
      }
   }

   //=====================================================================================================================================//
   // clean up and exit                                                                                                                   //
   //=====================================================================================================================================//

}//output END:

   if (argt.getNumRows() < 1)
      argt.addRow();

   argt.setColValInt( "workflow", int_workflow_flag);

   if(tbl_ask_window != null && Table.isTableValid(tbl_ask_window) != 0) tbl_ask_window.destroy();
   if(tbl_bunits != null && Table.isTableValid(tbl_bunits) != 0)     tbl_bunits.destroy();

   m_INCStandard.Print(str_error_log_file, "END", "*** End of " + str_file_name + " parameter script ***");

   if (int_exit_fail_flag == 1)
      throw new OException(errorMessage);
   return;
}





}

