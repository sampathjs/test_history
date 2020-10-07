/*$Header: /cvs/master/olf/plugins/standard/process/STD_EOD_Mature_Trades.java,v 1.13 2013/04/19 17:54:53 rdesposi Exp $*/
/*
File Name:                      STD_EOD_Mature_Trades.java

Report Name:                    EOD Mature Trades

Output File Name:               STD_EOD_Mature_Trades.exc
                                STD_EOD_Mature_Trades.rpt
                                STD_EOD_Mature_Trades.csv
                                STD_EOD_Mature_Trades.html
                                STD_EOD_Mature_Trades.pdf
                                STD_EOD_Mature_Trades.log
                                USER_EOD_Mature_Trades

Revision History:               Apr 03, 2013 -  (rdesposi) Added query_result_plugin
								Jan 22, 2013 -  (kzhu)     Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                Nov 16, 2010 - (adelacruz) Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
                                                           Replaced calls to the OpenJVS String library with calls to the Java String library
                                                           Replaced Util.exitFail with throwing an OException
                                Mar 04, 2005 - (tbain)     Configured script to run with INC_Standard, add exit_fail flag
                                Feb 03, 2005 - (tbain)     Standardized script for use with Report Manager
                                Jul 10, 2002 - (lbrzozow)  Added call to a new script function EndOfDay.matureMasterIns, to mature shared instrument (e.g. ComFut, RateFut) holding records.

Main Script:                    This
Parameter Script:               STD_Business_Unit_Param.java
Display Script:                 None
Script category: 		N/A
Script Description:
This script will change the tran_status to TRAN_STATUS_MATURE for
trades that have a close event_date <= mature_up_to_this_date

Assumption:

Instructions:
Set mature_up_to_this_date to the date for which trades should mature.
Example 1:
   If you want all trades that close today and/or before today to mature
   (this is the default) then set 'mature_up_to_this_date = OCalendar.today();'
Example 2:
   If you want all trades that close 60 days before today and/or before
   that date then set 'mature_up_to_this_date = OCalendar.today() - 60;'

Note:
mature_up_to_this_date can not be greater than the system (business) date

Report Manager Instructions:
   (OPTIONAL) DATE Field named "Mature To Date"

Use EOD Results?

EOD Results that are used:

When can the script be run?

Columns:
 Column Name                     Description                        Database Table/Formula
 -----------                     -----------                        ----------------------
 Internal Business Unit          Internal Business Unit             ab_tran.internal_bunit
 Tran Num                        Transaction Number                 ab_tran.tran_num

 */
package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class STD_EOD_Mature_Trades implements IScript {

	private JVS_INC_Standard m_INCStandard;
	public STD_EOD_Mature_Trades(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		/******************** USER CONFIGURABLE PARAMETERS *********************/

		int mature_up_to_this_date = OCalendar.today();
		// To Mature all trades up to 30 days ago you might use this instead:
		// int mature_up_to_this_date = OCalendar.today() - 30;

		int suppress_output = 1;   // Set to 1 to suppress all forms of output (script runs faster)
		//int suppress_output = 0;  // Set to 0 to allow output generations (script may run slower)
		/***********************************************************************/

		String sFileName = "STD_EOD_Mature_Trades";
		String sReportTitle = "EOD Mature Trades";
		String error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);

		int queryId;
		int num_of_rows, bunit_loop, bunit_int, retval, i, temp_date, exit_fail, generate_output = 0;
		String err_msg = "";
		String date_string, bunit_string, what=null, where, from=null, str_temp, str_temp_upper, strDate=null, sReportManager, temp_val;
		String queryTableName;
		Table tOutput=Util.NULL_TABLE, ab_tran=Util.NULL_TABLE, mat_table=Util.NULL_TABLE, tBunit, tParam, tCrystal, temp ;

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		/*** Check if a param script was used ***/
		if(argt.getNumRows() <= 0 || argt.getColNum("bunit") < 0){
			m_INCStandard.Print(error_log_file, "ERROR", "This script REQUIRES the parameter script STD_Business_Unit_Param.java");
			m_INCStandard.Print(error_log_file, "ERROR", "Since no Parameter script was specified this script has failed.");
			m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");
			throw new OException( "Running script without param script 'STD_Business_Unit_Param'" );
		}

		tBunit = Table.tableNew();
		tBunit.addCol( "bunit", COL_TYPE_ENUM.COL_INT);
		argt.copyCol( "bunit", tBunit, "bunit");

		/*** Check to see if Report Manager is being run ***/
		if(argt.getColNum( "out_params") > 0){
			suppress_output = 0;
			argt.deleteWhereValue( "report_id", 0);
			sReportManager = argt.getString( "report_name", 1);

			temp = argt.getTable( "inp_params", 1);
			if(Table.isTableValid(temp)==1)
			{
				temp = temp.copyTable();
				num_of_rows = temp.getNumRows();
			}
			else
				num_of_rows = 0;

			for(i = 1; i <= num_of_rows; i++)
			{
				str_temp = temp.getString( "arg_name", i);
				str_temp_upper = str_temp.toUpperCase();
				if( str_temp_upper.contains("MATURE") ){
					strDate = str_temp;
					i = num_of_rows + 1;
				}
			}
			if(Table.isTableValid(temp) == 1)
				temp.destroy();

			if( strDate == null || strDate.length() <= 0 ){
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria found for Mature To Date  - using default criteria");
			}else{
				temp_val = RptMgr.getTextEditStr(argt, sReportManager, strDate);
				temp_date  = OCalendar.parseString(temp_val);
				if(temp_date < 0){
					try{
						temp_date = Integer.parseInt(temp_val);
					}
					catch( NumberFormatException nfe ){
						m_INCStandard.Print( error_log_file, "ERROR", "NumberFormatException: " + nfe.getMessage() );
					}
				}
				if(temp_date > 0)
					mature_up_to_this_date = temp_date;
			}
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file,argt);
		exit_fail = 0;

		// if(!suppress_output && (m_INCStandard.STD_UseCrystalOutput() || m_INCStandard.view_table || m_INCStandard.report_viewer || m_INCStandard.csv_dump || m_INCStandard.user_table))
		if(suppress_output == 0 && (m_INCStandard.STD_UseCrystalOutput()!=0 || m_INCStandard.view_table!=0 || m_INCStandard.report_viewer!=0 || m_INCStandard.csv_dump!=0 || m_INCStandard.user_table!=0))
			generate_output = 1;

		date_string = OCalendar.formatDateInt(mature_up_to_this_date);
		m_INCStandard.Print (error_log_file, "INFO", "Maturing all trades closing up to and including " + date_string);

		if(generate_output != 0){
			tOutput = Table.tableNew();
			tOutput.addCol( "tran_num", COL_TYPE_ENUM.COL_INT);
			tOutput.addCol( "bunit", COL_TYPE_ENUM.COL_INT);
			ab_tran = Table.tableNew();
			mat_table = Table.tableNew();

			what  = " SELECT tran_num, tran_status, internal_bunit bunit ";
			from  = " FROM ab_tran ";
			where = " WHERE tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
			try{
				DBaseTable.execISql( ab_tran, what + from + where );
			}
			catch( OException oex ){
				m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
			}
		}

		retval = EndOfDay.matureMasterIns(mature_up_to_this_date);
		if(retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			m_INCStandard.Print(error_log_file, "ERROR", DBUserTable.dbRetrieveErrorInfo(retval, "EndOfDay.matureMasterIns() failed" ) );

		if(generate_output != 0){
			if (ab_tran.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert( ab_tran, "tran_num", "query_result_plugin");
				queryTableName = Query.getResultTableForId(queryId);
				if ( queryTableName == null && queryId > 0)
				{
					queryTableName = "query_result_plugin";
					m_INCStandard.Print(error_log_file, "ERROR", "Query id " + queryId
							+ " does not have a query result table. Default " + queryTableName + " table will be used.");
				}
				from = " FROM ab_tran, " + queryTableName;
				where = " WHERE tran_num = query_result AND unique_id = " + queryId +
						" AND tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
				try{
					DBaseTable.execISql( mat_table, what + from + where);
				}
				catch( OException oex ){
					m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
				}
				Query.clear( queryId );
				mat_table.copyColAppend( "tran_num", tOutput, "tran_num");
				mat_table.clearRows();
			}
		}

		num_of_rows = tBunit.getNumRows();

		for(bunit_loop = 1; bunit_loop <= num_of_rows; bunit_loop++)
		{
			bunit_int = tBunit.getInt( "bunit", bunit_loop);
			retval = EndOfDay.matureTrades(bunit_int, mature_up_to_this_date);
			if(retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				bunit_string = Table.formatRefInt(bunit_int, SHM_USR_TABLES_ENUM.PARTY_TABLE);
				m_INCStandard.Print (error_log_file, "ERROR", "Failure on Business Unit: " + bunit_string);
				m_INCStandard.Print (error_log_file, "ERROR", DBUserTable.dbRetrieveErrorInfo(retval, "EndOfDay.matureTrades() failed" ) );

				if(generate_output != 0){
					tOutput.destroy();
					mat_table.destroy();
					ab_tran.destroy();
				}

				throw new OException( "Failed to mature trades for Bunit " + bunit_string );
			}

			if(generate_output != 0){
				if (ab_tran.getNumRows() > 0)
				{
					queryId = Query.tableQueryInsert( ab_tran, "tran_num", "query_result_plugin" );
					queryTableName = Query.getResultTableForId(queryId);
					if ( queryTableName == null && queryId > 0)
					{
						queryTableName = "query_result_plugin";
						m_INCStandard.Print(error_log_file, "ERROR", "Query id " + queryId
								+ " does not have a query result table. Default " + queryTableName + " table will be used.");
					}
					from = " FROM ab_tran, " + queryTableName;
					where = " WHERE tran_num = query_result AND unique_id = " + queryId +
							" AND tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt() + "and internal_bunit = " + bunit_int;
					try{
						DBaseTable.execISql( mat_table, what + from + where);
					}
					catch( OException oex ){
						m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
					}
					Query.clear( queryId );
					mat_table.copyColAppend( "tran_num", tOutput, "tran_num");
					mat_table.clearRows();
				}
			}

		}

		if(generate_output == 0)
		{
			m_INCStandard.Print (error_log_file, "END", "*** End of " + sFileName + " script ***");
			return;
		}

		tOutput.select( ab_tran, "bunit", "tran_num EQ $tran_num");
		tOutput.makeTableUnique();

		tOutput.setColTitle( "tran_num", "Tran\nNum");
		tOutput.setColTitle( "bunit", "Business\nUnit");
		tOutput.setColFormatAsRef( "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);

		tOutput.formatSetWidth( "tran_num", 8);
		tOutput.formatSetWidth( "bunit", 30);

		tOutput.groupFormatted( "bunit, tran_num");

		/*** Generate/Save/m_INCStandard.Print Crystal Report Output ***/
		if(m_INCStandard.STD_UseCrystalOutput() != 0){

			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);
			tParam.setString( "date", 1, date_string);

			tCrystal = tOutput.copyTableFormatted( 0);
			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0){
				exit_fail = 1;
				err_msg += "Error value returned from m_INCStandard.STD_OutputCrystal()\n";
			}

			tCrystal.destroy();
			tParam.destroy();
		}

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0){
			m_INCStandard.STD_PrintTableDumpToFile(tOutput, sFileName, sReportTitle, error_log_file);
		}

		/*** Create USER Table ***/
		if(m_INCStandard.user_table != 0){
			temp = Table.tableNew();
			temp.select( tOutput, "*", "tran_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_EOD_Mature_Trades", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				exit_fail = 1;
				err_msg += "Error value returned form m_INCStandard.STD_SaveUserTable()\n";
			}
			temp.destroy();
		}

		tOutput.groupTitleAbove( "bunit");
		tOutput.colHide( "bunit");

		/*** View Table ***/
		if(m_INCStandard.view_table != 0){
			m_INCStandard.STD_ViewTable(tOutput, sReportTitle, error_log_file);
		}

		/*** Create Report Viewer EXC File ***/
		if(m_INCStandard.report_viewer != 0){
			m_INCStandard.STD_PrintTextReport(tOutput, sFileName + ".exc", sReportTitle, sReportTitle + "\nCurrent Date: " + date_string, error_log_file);
		}

		tBunit.destroy();
		tOutput.destroy();
		mat_table.destroy();
		ab_tran.destroy();

		m_INCStandard.Print (error_log_file, "END", "*** End of " + sFileName + " script ***");

		if(exit_fail != 0)
		   throw new OException( err_msg );
		return;
	}

}

