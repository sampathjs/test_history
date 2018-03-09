/*$Header: /cvs/master/olf/plugins/standard/report/STD_Start_End_BUnit_Param.java,v 1.9 2012/07/03 15:19:13 chrish Exp $*/
/*

 File Name:                     STD_Start_End_BUnit_Param.java

 Parameter Script:              This is a parameter script

 Date Written:                  July 27, 2004
 Author:                        Trisha Bain

 Revision History: Apr 25, 2012 -DTS94239: Replaced Ref.getModuleId() with Util.canAccessGui().
                           Mar 05, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
 	           Sep 23, 2010 - Wrapped function calls to DBaseTable.execISql with a try-catch block
                            Aug 06, 2010 - Replaced function calls to loadFromDb(WithWhatWhereSQL) with calls to execISql
                                           Replaced function calls to OpenJVS String library with calls to the standard Java String library
                                           Replaced function calls to Util.exitFail with throwing an OException
                            Jul 25, 2007 (bpaszkie) - Change IBUNIT_TABLE to select from database
                            Sep 28, 2006 (bpaszkie) - Change BUnits picklist to run for Internal Business Units only
                            Jun 09, 2005 (jross) - Add INC_Standard and Set ASK_WINDOW default value to TRUE
                            Sep 14, 2004 (tbain) - Added Report Manager functionality
                            Oct 04, 2004 (tbain) - Added WORKFLOW flag
                            Oct 11, 2004 (tbain) - Updated documentation header
                            Nov 18, 2004 (tbain) - Updated Report Manager functionality to be more "name tolerant"
                            Dec 22, 2004 (tbain) - Changed Start Date and End Date from type COL_STRING to ASK_DATE in ASK_Window

 Description:                   When ASK_WINDOW is set to 1, an interactive pop-up ASK window will be generated.
                                Otherwise, the script will run with default values, set by the user.
                                When EDIT_BUNIT is set to 1, only the Business Units specified in the script will be used,
                                Otherwise, the script will run with the default of all Business Units

 Report Manager:               Must have a DATE input field named "Start Date"
                               Must have a DATE input field named "End Date"
                               Must have a Picklist of business units named "Business Units"

Recommended Script Category? 	N/A

 Parameters:
 param name      type         description
 ==========      ====         ===========
 ASK_WINDOW      int          1 - generates pop up ASK window for user input

 EDIT_BUNIT      int          1 - uses only Business Units specified in script
                              0 - uses all Business Units with new/validated trades in database

 START_DATE      int          julian representation of the default start date

 END_DATE        int          julian representation of the default end date

 tBUnits         table        add/remove rows to/from this table when EDIT_BUNIT is 1 (see below)


 Format of argt:
 col name         col type        description
 ========         ========        ===========
 workflow         int             1 when script is run from a workflow, else 0

 start_date       int             Julian representation of the start date

 end_date         int             Julian representation of the end date

 bunits           table           table of business units / table format: return_val (int), ted_str_value (String), return_value (int)

 */
package standard.report;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class STD_Start_End_BUnit_Param_Mature implements IScript {

	private JVS_INC_Standard m_INCStandard;

	public STD_Start_End_BUnit_Param_Mature(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		/************************************* USER CONFIGURABLE PARAMETERS ******************************************/
		int ASK_WINDOW = 1;   // Set this flag to 1 to pop up ASK Window Interaction
		//int ASK_WINDOW = 0;

		int EDIT_BUNIT = 1;   // Set this flag to 1 to run with only Business Units specified in script
		// int EDIT_BUNIT = 0;  //                  0 to run will ALL Business Units

		int START_DATE = OCalendar.today();
		int END_DATE   = OCalendar.today();
		/***************************************************************************************/

		Table  tBUnits=Util.NULL_TABLE,tDefaultBUnits,tAsk,tSelectedBUnits, temp;
		//   int       iScriptSucceeded,iCount;
		int start, end, numRows, i;
		//  String    rptMgr_inputName;
		String strReportName;
		//   String sTemp;
		String str_temp, str_temp_upper, strStart=null, strEnd=null, strBUnit=null, sWhere;
		String sFileName = "STD_Start_End_BUnit_Param";
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);
		String errorMessages = "";


		argt.addCol( "workflow", COL_TYPE_ENUM.COL_INT);
		if(argt.getNumRows() < 1) argt.addRow();

		if (Util.canAccessGui() == 0) {
			argt.setInt( "workflow", 1, 1);
			ASK_WINDOW = 0;
		}else {
			argt.setInt( "workflow", 1, 0);
		}

		m_INCStandard.Print(error_log_file, "START", "*** Start of param script - " + sFileName + " ***");

		argt.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
		argt.addCol( "end_date",   COL_TYPE_ENUM.COL_INT);
		argt.addCol( "bunits",     COL_TYPE_ENUM.COL_TABLE);

		/* Prepare the Run Types Pick List and Default tables */

		if(argt.getColNum( "out_params") <= 0) {  // Running OpenJvs Task from Trading Manager
			m_INCStandard.Print(error_log_file, "INFO", "Not running with Report Manager");
			tBUnits        = Table.tableNew ("Business Units");

			if(ASK_WINDOW == 0)
			{
				if(EDIT_BUNIT == 1)
				{
					tBUnits.addCol( "return_val", COL_TYPE_ENUM.COL_INT);

					/****************************** USER CONFIGURABLE SECTION **********************************/
					/* Fill in the Business Units in quotes below                                              */
					tBUnits.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT1")));
					tBUnits.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT2")));
					tBUnits.addRowsWithValues( Integer.toString(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "BUNIT3")));
					/* repeat the above line for multiple business units                                       */
					/*                                                                                         */
					/*******************************************************************************************/
					m_INCStandard.Print(error_log_file, "INFO", "Running for only User Specified Business Units");
					/*** Format bunits table ***/
					tBUnits.addCol( "ted_str_value", COL_TYPE_ENUM.COL_STRING);
					tBUnits.addCol( "return_value", COL_TYPE_ENUM.COL_INT);
					tBUnits.copyColFromRef( "return_val", "ted_str_value", SHM_USR_TABLES_ENUM.PARTY_TABLE);
					tBUnits.copyCol( "return_val", tBUnits, "return_value");

				}else{
					m_INCStandard.Print(error_log_file, "INFO", "Running for all Business Units");
					sWhere = "WHERE int_ext  = " +  CONF_INT_EXT.INTERNAL.toInt() + " and party_class = 1 and party_status = 1";
					try{
						DBaseTable.execISql( tBUnits, "SELECT party_id, short_name FROM party " + sWhere );
					}
					catch( OException oex ){
						errorMessages = "OException at execute(), Failed to load BUnit info from the database, " + oex.getMessage();
						m_INCStandard.Print( error_log_file, "ERROR", errorMessages );
						throw new OException( errorMessages );
					}

					tBUnits.addCol( "return_value", COL_TYPE_ENUM.COL_INT);
					tBUnits.setColName( 1, "return_val");
					tBUnits.setColName( 2, "ted_str_value");
					tBUnits.copyCol( 1, tBUnits, 3);
				}

				if(END_DATE < START_DATE){
					m_INCStandard.Print(error_log_file, "ERROR", "Invalid Dates - End Date must occur after Start Date -- Using Default Behavior (running only for Start Date)");
					END_DATE = START_DATE;
				}

				/*** Format argt ***/
				argt.setInt( "start_date", 1, START_DATE);
				argt.setInt( "end_date",   1, END_DATE);
				argt.setTable( "bunits",     1, tBUnits.copyTable());
				tBUnits.destroy();

				m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
				return;
			}

			m_INCStandard.Print(error_log_file, "INFO", "Running in Adhoc Mode");

			// Load all Internal Business Units
			sWhere = "WHERE int_ext  = " +  CONF_INT_EXT.INTERNAL.toInt() + " and party_class = 1 and party_status = 1";
			try{
				DBaseTable.execISql( tBUnits, "SELECT short_name label, party_id id FROM party " + sWhere );
			}
			catch( OException oex ){
				errorMessages = "OException at execute(), Failed to load BUnit info from the database, " + oex.getMessage();
				m_INCStandard.Print( error_log_file, "ERROR", errorMessages );
				throw new OException( errorMessages );
			}

			tBUnits.group( "label");
			tBUnits.insertRowBefore( 1);
			tBUnits.setString( "label", 1, "ALL Internal Business Units");
			tBUnits.setInt( "id",   1, 1);
			tDefaultBUnits = tBUnits.cloneTable();
			tBUnits.copyRowAdd( 1, tDefaultBUnits);
			tBUnits.colHide( "id");

			/* Prepare the Ask table */
			do{
				tAsk = Table.tableNew ("Ask");

				if(Ask.setTextEdit (tAsk
						,"Start of Date Range"
						,OCalendar.formatDateInt (OCalendar.today())
						,ASK_TEXT_DATA_TYPES.ASK_DATE
						,"Please select the range Start Date"
						,1)!=1
						||
						Ask.setTextEdit (tAsk
								,"End of Date Range"
								,OCalendar.formatDateInt (OCalendar.today ())
								,ASK_TEXT_DATA_TYPES.ASK_DATE
								,"Please select the range End Date"
								,1)!=1
								||
								Ask.setAvsTable (tAsk
										,tBUnits
										,"Business Units"
										,1
										,ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt()
										,2
										,tDefaultBUnits
										,"Please select the required Business Unit(s)"
										,1)!=1)
				{
					errorMessages = "An error occurred in the Parameter Script while initialising the ASK table\n"
						+ "The report has been cancelled.";
					Ask.ok ( errorMessages );
					tBUnits.destroy();
					tDefaultBUnits.destroy();
					tAsk.destroy();

					m_INCStandard.Print(error_log_file, "ERROR", "User Clicked Cancel");
					m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
					throw new OException( errorMessages + " User Clicked Cancel" );
				}

				/* Get User to select parameters */
				if(Ask.viewTable (tAsk,"Adhoc","Please Complete the Following Fields") == 0)
				{
					errorMessages = "The Adhoc Ask has been cancelled.";
					Ask.ok ( errorMessages );
					tBUnits.destroy();
					tDefaultBUnits.destroy();
					tAsk.destroy();

					m_INCStandard.Print(error_log_file, "ERROR", "User Clicked Cancel");
					m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
					throw new OException( errorMessages + " User Clicked Cancel" );
				}

				/* Verify Start and End Dates */
				start = OCalendar.parseString (tAsk.getTable( "return_value", 1).getString("return_value", 1));
				end   = OCalendar.parseString (tAsk.getTable( "return_value", 2).getString("return_value", 1));

				if(start < 0)
				{
					Ask.ok ("The start date is invalid");
					start = -1;
				}
				if(end < 0)
				{
					Ask.ok ("The end date is invalid");
					start = -1;
				}
				if(start > end)
				{
					Ask.ok ("The start date cannot occur after the end date");
					start = -1;
				}

			}while(start == -1);

			/* Verify Business Units selection */
			tSelectedBUnits = tAsk.getTable( "return_value", 3).copyTable();
			tSelectedBUnits.group( "return_val");
			if(tSelectedBUnits.findInt( "return_val", 1, SEARCH_ENUM.FIRST_IN_GROUP) > 0)
			{
				if(tSelectedBUnits.getNumRows() > 1){
					if(Ask.okCancel ("You have selected ALL Business Units.\nClick OK to run the report using All Business Units, or Cancel to stop the report.") == 0)
					{
						tBUnits.destroy();
						tDefaultBUnits.destroy();
						tAsk.destroy();
						tSelectedBUnits.destroy();

						m_INCStandard.Print(error_log_file, "ERROR", "User Clicked Cancel");
						m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
						throw new OException( "User Clicked Cancel" );
					}
				}
				tSelectedBUnits.clearRows();
				tSelectedBUnits.select( tBUnits, "id(return_val), label(ted_str_value), id(return_value)", "id GT 1");
			}else{
				tSelectedBUnits.copyColFromRef( "return_val", "ted_str_value", SHM_USR_TABLES_ENUM.PARTY_TABLE);
			}

			/* Add data to argt */
			argt.setInt( "start_date", 1, start);
			argt.setInt( "end_date",   1, end);
			argt.setTable( "bunits",     1, tSelectedBUnits.copyTable());

			/* Clean up */
			tBUnits.destroy();
			tDefaultBUnits.destroy();
			tAsk.destroy();
			tSelectedBUnits.destroy();

			m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
			return;

		}else { // Running OpenJvs Task from Report Manager
			m_INCStandard.Print(error_log_file, "INFO", "Running with Report Manager");

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
				str_temp = temp.getString( "arg_name", i);
				str_temp_upper = str_temp.toUpperCase();
				if( str_temp_upper.contains("START") ){
					strStart = str_temp;
				}else if( str_temp_upper.contains("END") ){
					strEnd = str_temp;
				}else if( str_temp_upper.contains("BUSINESS") || str_temp_upper.contains("BUNIT") ){
					strBUnit = str_temp;
				}
			}
			if(Table.isTableValid(temp) == 1)
				temp.destroy();


			if( strBUnit == null || strBUnit.isEmpty() ){
				m_INCStandard.Print(error_log_file, "ERROR", "Param Script Requires a IBUNIT_TABLE Picklist Field named 'Business Units'");
			}else{
				tBUnits  = RptMgr.getArgList(argt, strReportName, strBUnit).copyTable();
			}

			if( strStart == null || strStart.isEmpty() ){
				m_INCStandard.Print(error_log_file, "ERROR", "Param Script Requires a DATE Field named 'Start Date'");
				start = -1;
			}else{
				str_temp = RptMgr.getTextEditStr(argt, strReportName, strStart);
				start   = OCalendar.parseString(str_temp);
				if(start < 0){
					try{
						start = Integer.parseInt( str_temp );
					}
					catch( NumberFormatException nfe ){
						m_INCStandard.Print(error_log_file, "ERROR", "NumberFormatException, using default Start Date" );
						start = START_DATE;
					}
				}
			}

			if( strEnd == null || strEnd.isEmpty() ){
				m_INCStandard.Print(error_log_file, "ERROR", "Param Script Requires a DATE Field named 'End Date'");
				end = -1;
			}else{
				str_temp = RptMgr.getTextEditStr(argt, strReportName, strEnd);
				end      = OCalendar.parseString(str_temp);
				if(end < 0){
					try{
						end = Integer.parseInt( str_temp );
					}
					catch( NumberFormatException nfe ){
						m_INCStandard.Print(error_log_file, "ERROR", "NumberFormatException, using default End Date");
						end = END_DATE;
					}
				}
			}

			if(start < 0){
				m_INCStandard.Print(error_log_file, "ERROR", "Invalid Start Date -- Using Default Start Date");
				start = START_DATE;
			}

			if(end < 0){
				m_INCStandard.Print(error_log_file, "ERROR", "Invalid End Date -- Using Default End Date");
				end = END_DATE;
			}

			if(end < start){
				m_INCStandard.Print(error_log_file, "ERROR", "Invalid Dates - End Date must occur after Start Date -- Using Default Behavior (running only for Start Date)");
				end = start;
			}

			argt.setInt( "start_date", 1, start);
			argt.setInt( "end_date",   1, end);

			if(tBUnits !=null && Table.isTableValid(tBUnits) != 0) {
				if(tBUnits.getNumRows() < 1) {
					m_INCStandard.Print(error_log_file, "ERROR", "Must pick a business unit from pick list: Business Units");
					tBUnits.destroy();
					//goto end;
					m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
					return;
				}
				tBUnits.insertCol( "return_val", 1, COL_TYPE_ENUM.COL_INT);
				tBUnits.copyCol( "id", tBUnits, "return_val");
				tBUnits.setColName( 2, "ted_str_value");
				tBUnits.setColName( 3, "return_value");
				argt.setTable( "bunits", 1, tBUnits.copyTable());
				tBUnits.destroy();
			}else {
				m_INCStandard.Print(error_log_file, "ERROR", "Script requires use of pick list: Business Units");
				m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
				return;
				//goto end;
			}
		}//endif RM

		//end:
		m_INCStandard.Print(error_log_file, "END", "*** End of param script - " + sFileName + " ***\n");
	}

}
