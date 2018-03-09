/*$Header: /cvs/master/olf/plugins/standard/process/STD_EOD_Gen_Inv_Param.java,v 1.6 2012/05/16 15:11:09 dzhu Exp $*/

/*
File Name:                    STD_EOD_Gen_Inv_Param.java

Report Name:                  None

Output File Name(s):          N/A

Date Of Last Revision:        November 2, 2009

Revision History:             Apr 25, 2012 -DTS94239: Replaced Ref.getModuleId() with Util.canAccessGui().
                              Mar 02, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
							  Nov 16, 2010 - Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
                                           - Replaced calls to the OpenJVS String library with calls to the Java String library
                              11/02/09 mraziano - Created param script for STD_EOD_Generate_Inventory that passes all
                                                  internal business units


Author:                       mraziano

Main Script:                  STD_EOD_Generate_Inventory.java
Parameter Script:             This is a Parameter script
Display Script:               None needed
Script category: 		N/A
Script Description:           This script is based off of STD_Business_Unit_Param. It will take all internal business units, irregardless if they
                              are used by trades or not. It is a param script for STD_EOD_Generate_Invetory .

                              Otherwise, the script defaults to use ALL Internal Business Units.
                              By setting EDIT_BUNITS to 1, it will use only the selected Internal Business Units specified in script.

                              This param script will also accept parameters from the Report Manager when appropriately configured.

Assumptions:                  None

Instructions:                 To add or remove business units add or remove the business unit names.

Report Manager:               1. Setup a task using this script as the parameter script in the task editor.
                              2. Add the report to Report Manager that uses this OpenJvs task.
                              3. When adding the report, configure it with the SHM_USR_TABLES_ENUM.IBUNIT_TABLE standard pick list.
                              The pick list label should be "Business Units".

Uses EOD Results?             False

Which EOD Results are used?   N/A

When can the script be run?   Anytime

Parameters:

param name      type          description
==========      ====          ===========
EDIT_BUNITS     int           1 - uses only Business Units specified in script
                              0 - uses all Business Units with new/validated trades in database

argt            table         add/remove rows to/from this table when EDIT_BUNITS is 1 (see below)


Format of argt:

col name         col type        description
========         ========        ===========
bunit            int             list of business units to be used in main script (one ID per row)

workflow         int             1 when script is run from a workflow, else 0

 */

package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class STD_EOD_Gen_Inv_Param implements IScript {

	private JVS_INC_Standard m_INCStandard;

	public STD_EOD_Gen_Inv_Param(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		int ASK_WINDOW  = 0;
		int EDIT_BUNITS = 0;

		int accessGui, bunit, workflow, ask_window;
		int numRows, i;
		String error_log_file, strReportName, str_temp, str_temp_upper;
		String strName = "";
		Table tAsk, bunits, party, temp, tbunits;

		String sFileName = "STD_EOD_Gen_Inv_Param";
		error_log_file = Util.errorInitScriptErrorLog(sFileName);

		m_INCStandard.Print(error_log_file, "START", "*** Start of param script - " + sFileName + " ***");

		accessGui = Util.canAccessGui();
		if(accessGui == 0)
			ASK_WINDOW = 0;

		argt.addCol( "bunit", COL_TYPE_ENUM.COL_INT);
		argt.addCol( "workflow", COL_TYPE_ENUM.COL_INT);

		if(argt.getColNum( "out_params") <= 0) {  // Running OpenJvs Task from Trading Manager
			m_INCStandard.Print(error_log_file, "INFO", "Not Running with Report Manager");

			if(EDIT_BUNITS != 0)
			{
				/********************* USER CONFIGURABLE SECTION ***************************/
				/* Fill in the Business Units in quotes below                              */
				argt.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT1")));
				argt.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT2")));
				argt.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT3")));
				/* repeat the above line for multiple business units                       */
				/***************************************************************************/

				m_INCStandard.Print(error_log_file, "INFO", "Running With Only User Specified Values");

			}else {
				m_INCStandard.Print(error_log_file, "INFO", "Running With All Possible Values");

				/* Load all internal Business Units with new and validated deals */
				party = Table.tableNew();
				try{
					DBaseTable.execISql(party, "SELECT party_id internal_bunit, short_name FROM party WHERE int_ext=0 and party_class=1" );
				}
				catch( OException oex ){
					m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
				}
				argt.select( party, "internal_bunit(bunit)", "internal_bunit GT 0");
				party.destroy();
			}


		}else {  // Running OpenJvs Task in ReportManager
			m_INCStandard.Print(error_log_file, "INFO", "Running with Report Manager");

			strReportName = argt.getString( "report_name", 1);
			temp = (argt.getTable( "inp_params", 1)).copyTable();
			numRows = temp.getNumRows();
			for(i = 1; i <= numRows; i++)
			{
				str_temp = temp.getString( "arg_name", i);
				str_temp_upper = str_temp.toUpperCase();

				if( str_temp_upper.contains("BUSINESS") || str_temp_upper.contains("BUNIT") )
				{
					strName = str_temp;
					i = numRows + 1;
				}

			}
			temp.destroy();

			tbunits = RptMgr.getArgList(argt, strReportName, strName);

			end:
			{
				if(Table.isTableValid(tbunits) != 0) {
					if(tbunits.getNumRows() < 1) {
						m_INCStandard.Print(error_log_file, "ERROR", "Must pick a business unit from pick list: Business Units");
						break end;
					}
					tbunits.copyCol( "id", argt, "bunit");
				}else {
					m_INCStandard.Print(error_log_file, "ERROR", "Script requires use of pick list: Business Units");
					break end;
				}
			}//endif RM
		}

		if(argt.getNumRows() < 1) argt.addRow();

		if(accessGui == 0){
			argt.setInt( "workflow", 1, 1);
		}else
			argt.setInt( "workflow", 1, 0);

		m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");

	}





}
