/*$Header: /cvs/master/olf/plugins/standard/report/STD_Missing_Reset.java,v 1.15 2013/08/08 16:09:56 chrish Exp $*/
/*
File Name:                  STD_Missing_Reset.java

Report Name:                Missing Resets Report for (Date)

Output File Name:           STD_Missing_Reset.rst
                            STD_Missing_Reset.rpt
                            STD_Missing_Reset.csv
                            STD_Missing_Reset.html
                            STD_Missing_Reset.pdf
                            STD_Missing_Reset.log
                            USER_Missing_Reset

Available RptMgr Outputs:   m_INCStandard.Print Crystal
                            Table Viewer
                            Crystal Viewer
                            Save Crystal
                            Report Viewer
                            CSV
                            View CRYSTAL_EXPORT_TYPES.HTML
                            Save CRYSTAL_EXPORT_TYPES.HTML
                            View CRYSTAL_EXPORT_TYPES.PDF (Crystal 9 and up)
                            Save CRYSTAL_EXPORT_TYPES.PDF (Crystal 9 and up)
                            User Table
                            Save Public
                            Save Local


Revision History:           Nov 21, 2011 - Added cflow missing resets and notional resets to the report
							Oct 21, 2011 - Added to report Swap deals with unknown cashflow values.
 							Aug 22, 2011 - Add logic for optional argt input (STD_Missing_Reset_Param). These values override input hardcoded values.
 							Aug 16, 2011 - Added passing table of missed resets results to workflow
						 	Mar 17, 2011 - DTS 59654 - Update standard script to support User Defined Instrument types.
						 	Nov 09, 2010 - Broke up code into smaller functions
                            Sep 28, 2010 - Wrapped function calls to DBaseTable.execISql with a try-catch block
                            Sep 02, 2010 - Replaced function calls to DBaseTable.loadFromDb* with calls to DBaseTable.execISql
                                         - Replaced function calls to the OpenJVS String library with calls to the Java String library
                                         - Replaced function calls to Util.exitFail with throwing an OException
                            Sep 27, 2007 - DTS33462 Set exit fail status to true when there are missing resets.
                            May 01, 2007 - DTS30093: Optimization
                            Mar 03, 2005 - Add Util.exitFail() check for output generation
                            Feb 24, 2005 - Configured script to run with INC_Standard
                            Dec 16, 2004 - Added retval crystal check, pdf fix, new htmlViewer code, user table fix, RM params
                            Oct 05, 2004 - ASP Output functionality added, CRYSTAL_EXPORT_TYPES.HTML and CSV output capability added.

Main Script:                This
Parameter Script:           None
Display Script:             None

Report Description:         This report will search the database for all validated (and amended, cancelled if the flag is set to 1)
                            trades that are missing resets.

Assumption:                 None

Instruction:                
Check user configurable constants to view Crystal report, to exclude holding instruments, and to include amended 
and canceled transactions.

Report Manager Instructions:
   (Optional) Use SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist named "Include Amended Trades"
   (Optional) Use SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist named "Include Cancelled Trades"
   (Optional) Use SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist named "Exclude Holding Trades"
   (Optional) Use TRUE_FALEE_TABLE Picklist named "Use Business Date"
Workflow Instructions:
	Set to_workflow to 1 to pass output table of missing resets reals to workflow (default)
	Set to_workflow to 0 NOT to pass output table of missing resets reals to workflow

Use EOD Results?

EOD Results that are used : 

When can the script be run?

Columns:
   Internal Bunit               ab_tran.internal_bunit
   Projection Index             parameter.proj_index
   Index Source                 parameter.index_src
   Reset Date                   reset.reset_date
   Deal Num                     ab_tran.deal_tracking_num
   Tran Num                     ab_tran.tran_num
   Tran Status                  ab_tran.tran_status
   Ins Num                      ab_tran.ins_num
   Ins Type                     ab_tran.ins_type
   Start Date                   reset.start_date
   End Date                     reset.end_date
   Book                         ab_tran.book
   Ticker                       header.ticker (Financial Report Only)
   Cusip                        header.cusip  (Financial Report Only)
   Projection Index Tenor       parameter.proj_index_tenor (Energy Report Only)
   Reset Type					short description of the missing reset

 */

package standard.report;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class STD_Missing_Reset implements IScript {

	private JVS_INC_Standard m_INCStandard;

	private Table output;

	private String error_log_file;

	public STD_Missing_Reset() {
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		/***************************************************************************************************/
		/******************** USER CONFIGURABLE PARAMETERS *************************************************/
		/***************************************************************************************************/
		//int amended = 1;         	// set this flag to 1 if you want to search for amended trades
		int amended = 0;

		//int cancelled = 1;      	// set this flag to 1 if you want to search for cancelled trades.
		int cancelled = 0;

		// int exclude_holding = 1;	// set this flag to 1 if you want to exclude holding transaction from the report, set to 0 otherwise
		int exclude_holding = 0;

		//int business_date = 1;   	// Flag to specify whether to use current date (Market Manager date) or business date. 
		int business_date = 0; 		// Set the business_date to 0 to use current.

		//int to_workflow = 0;    	// Output table will NOT be sent to workflow
		int to_workflow = 1;    	// Output table will be sent to workflow (default)

		/***************************************************************************************************/
		/***************************************************************************************************/

		Table argt = context.getArgumentsTable();
		Table fmt_output, temp, tParam;
		Table missing_resets_table;

		String sFileName = "STD_Missing_Reset"; 
		String sReportTitle = "Missing Resets Report";
		String errorMessages = "";
		String str_temp, str_temp_upper, strAmend = "", strCancel = "", strExclude = "", strBDate = null;
		String sqlAux, sReportManager, today_dt;

		int i, numRows, temp_return, today, exit_fail = 0, report_type = m_INCStandard.STD_GetReportType();  
		error_log_file = Util.errorInitScriptErrorLog(sFileName);
		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		/* Get parameters from Report Manager if available */
		if(argt.getColNum( "out_params") > 0)
		{
			temp = argt.getTable( "inp_params", 1).copyTable();
			sReportManager = argt.getString( "report_name", 1);

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
				if( str_temp_upper.contains("AMEND") ) {
					strAmend = str_temp;
				} else if( str_temp_upper.contains("CANCEL") ) {
					strCancel = str_temp;
				} else if( str_temp_upper.contains("DATE") || str_temp_upper.contains("BUSINESS")  ) {
					strBDate = str_temp;
				} else if( str_temp_upper.contains("EXCLUDE") || str_temp_upper.contains("HOLD") ) {
					strExclude = str_temp;
				}    
			}
			if(Table.isTableValid(temp) == 1)
				temp.destroy();

			if( strAmend == null || strAmend.isEmpty() ) {
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria for including Amended Trades found - using default criteria");
			} else {
				temp_return = RptMgr.getArgList(argt, sReportManager, strAmend).getInt( "id", 1);
				if (temp_return == 1 || temp_return == 0)
					amended = temp_return;
			}

			if( strCancel == null || strCancel.isEmpty() ) {
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria for including Cancelled Trades found - using default criteria");
			} else {
				temp_return = RptMgr.getArgList(argt, sReportManager, strCancel).getInt( "id", 1);
				if (temp_return == 1 || temp_return == 0) 
					cancelled = temp_return;
			}

			if( strExclude == null || strExclude.isEmpty() ) {
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria for including Holding Trades found - using default criteria");
			} else {
				temp_return = RptMgr.getArgList(argt, sReportManager, strExclude).getInt( "id", 1);
				if (temp_return == 1 || temp_return == 0) 
					exclude_holding = temp_return;
			}

			if( strBDate == null || strBDate.isEmpty() ) {
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria for using Business Date found - using default criteria");
			} else {
				temp_return = RptMgr.getArgList(argt, sReportManager, strBDate).getInt( "id", 1);
				if (temp_return == 1 || temp_return == 0) 
					business_date = temp_return;
			}
		}
		else if(argt.getColNum( "out_params") <= 0){
			if (argt.getNumRows() > 0 && argt.getColNum( "amended") > 0){
				amended =   argt.getInt( "amended", 1);
				cancelled = argt.getInt( "cancelled", 1);
				exclude_holding = argt.getInt( "exclude_holding", 1);
				business_date = argt.getInt( "business_date", 1);
			}
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file, argt);

		/* Get date and convert it from julian format to a date String */
		today = GetDate(business_date);
		today_dt = OCalendar.formatJdForDbAccess(today);

		CreateOutputTable();

		sqlAux = GetSqlAux(today_dt, amended, cancelled);

		/* generate missing price/rate resets */ 
		PopulateOutputTableWithDeals(today_dt, sqlAux);  			
		PopulateOutputTableWithDates(today_dt, sqlAux);				

		/* generate missing cash flow resets */ 
		PopulateMissingCFlowResets(today_dt, sqlAux);				
		PopulateMissingCFlowCommodityResets(today_dt, sqlAux);		

		/* generate notional resets */ 
		PopulateMissingNotionalResets(today_dt, sqlAux);			
		PopulateMissingNotionalCurrencyResets(today_dt, sqlAux); 	


		if(output.getNumRows() > 0)	{
			if(exclude_holding != 0) 
				output.deleteWhereValue( "tran_type", TRAN_TYPE_ENUM.TRAN_TYPE_HOLDING.toInt());

			/* Exclude ENGY-EXCH-FUT, MTL-EXCH-FWD, MTL-EXCH-FUT, SOFT-EXCH-FUT, ENGY-EXCH-AVG-FWD, ENGY-EXCH-AVG-FUT. */
			ExcludeExchInsFromOutput();
		}

		/* Format output table and make a copy of it */
		FormatOutputTable();
		fmt_output = output.copyTableFormatted( 0);   
		GroupTables(fmt_output);
		m_INCStandard.Print(error_log_file, "INFO", "Loaded  " + fmt_output.getNumRows() + " rows.");

		/*** Generate/Save/m_INCStandard.Print Crystal Report Output ***/
//		if(m_INCStandard.STD_UseCrystalOutput() != 0) {
//
//			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);
//			tParam.addCol( "report_type", COL_TYPE_ENUM.COL_INT);
//			tParam.setInt( "report_type", 1, report_type);
//			tParam.setString( "date", 1, OCalendar.formatDateInt(today));
//
//			fmt_output.printTableToTtx( "c:\\temp\\"+ sFileName + ".ttx");
//
//			if(m_INCStandard.STD_OutputCrystal(fmt_output, tParam, sFileName, sFileName, error_log_file) == 0){
//				exit_fail = 1;
//				errorMessages += "Error value returned from m_INCStandard.STD_OutputCrystal.\n";
//			}
//			tParam.destroy();
//		} 

		FormatOutputTableForReport(report_type);

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0) 
			m_INCStandard.STD_PrintTableDumpToFile(output, sFileName, sReportTitle, error_log_file);

		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0) {
			temp = Table.tableNew();
			temp.select( fmt_output, "*", "deal_tracking_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_Missing_Reset", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				exit_fail = 1;
				errorMessages += "Error value returned from m_INCStandard.STD_SaveUserTable.\n";
			}
			temp.destroy();
		} 

		GroupOutputTable();

		/*** Create Report Viewer TXT File ***/  
		if(m_INCStandard.report_viewer != 0)
			m_INCStandard.STD_PrintTextReport(output, sFileName + ".rst", sReportTitle, sReportTitle + 
					"\nCurrent Date: " + OCalendar.formatDateInt(today), error_log_file);

		/*** View Table ***/
		if(m_INCStandard.view_table != 0) 
			m_INCStandard.STD_ViewTable(output, sReportTitle, error_log_file); 

		/*********************DTS#33462 Missing Trade Reset*************************************
		 * This section will set the exit status to fail when there are trades that need resets.
		 **************************************************************************************/
		if(output.getNumRows() > 0)	{
			exit_fail  = 1;
			m_INCStandard.Print(error_log_file, "ERROR","There are records that are missing reset");
			errorMessages += "There are records that are missing reset\n";
		}
		/***************************DTS#33462***************************************************/

		/* Purge all tables created for this report */
		missing_resets_table = output.copyTable();
		if (output != Util.NULL_TABLE && Table.isTableValid(output) != 0) output.destroy();
		if (fmt_output != Util.NULL_TABLE && Table.isTableValid(fmt_output) != 0)  fmt_output.destroy();

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");

		if(exit_fail == 1)
			if (to_workflow == 1)
				Util.exitFail(missing_resets_table);
			else {
				missing_resets_table.destroy();
				throw new OException( errorMessages );
			}
		else 
			if (to_workflow == 1)
				Util.exitSucceed(missing_resets_table);
			else {				
				missing_resets_table.destroy();
				return;
			}
	}

	//================================================================================================//
	// Local Function Definitions                                                                     //
	//================================================================================================//

	private int GetDate(int business_date) throws OException
	{
		int today;

		if(business_date == 1)
			today = Util.getBusinessDate();
		else  
			today = OCalendar.today();

		return today;
	} 

	//------------------------------------------------------------------------------------------------//

	private void CreateOutputTable() throws OException
	{
		output = Table.tableNew();

		output.addCol( "internal_bunit", 	COL_TYPE_ENUM.COL_INT);
		output.addCol( "reset_date", 		COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol( "deal_tracking_num", COL_TYPE_ENUM.COL_INT);
		output.addCol( "tran_num", 			COL_TYPE_ENUM.COL_INT);
		output.addCol( "tran_status", 		COL_TYPE_ENUM.COL_INT);
		output.addCol( "ins_num", 			COL_TYPE_ENUM.COL_INT);
		output.addCol( "ins_type", 			COL_TYPE_ENUM.COL_INT); 
		output.addCol( "start_date", 		COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol( "end_date", 			COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol( "book", 				COL_TYPE_ENUM.COL_STRING);
		output.addCol( "ticker", 			COL_TYPE_ENUM.COL_STRING);
		output.addCol( "cusip", 			COL_TYPE_ENUM.COL_STRING);
		output.addCol( "tran_type", 		COL_TYPE_ENUM.COL_INT);
		output.addCol( "toolset", 			COL_TYPE_ENUM.COL_INT); 
		output.addCol( "reset_type",		COL_TYPE_ENUM.COL_STRING);
		output.addCol( "proj_index", 		COL_TYPE_ENUM.COL_INT);
		output.addCol( "index_src", 		COL_TYPE_ENUM.COL_INT);
		output.addCol( "proj_index_tenor", 	COL_TYPE_ENUM.COL_INT);
	}

	//------------------------------------------------------------------------------------------------//

	private String GetSqlAux(String today_dt, int amended, int cancelled) throws OException
	{
		String sqlAux;

		if(amended == 1 && cancelled == 1)
			sqlAux =  " ((ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ") OR " +
			"(exists (" +
			"		select * " +
			"		from ab_tran_event " +
			"		where " +
			"			ab_tran_event.tran_num = ab_tran.tran_num AND " +
			"			ab_tran_event.event_type in ( " 
			+ EVENT_TYPE_ENUM.EVENT_TYPE_AMENDED.toInt() + ","        	// 19
			+ EVENT_TYPE_ENUM.EVENT_TYPE_CANCELLED.toInt() + ","        // 10
			+ EVENT_TYPE_ENUM.EVENT_TYPE_BUYOUT.toInt() + ") AND " + 	// 15
			"			ab_tran_event.event_date = '" + today_dt + "'" +
			"		)" +
			")) ";

		else if(amended == 1)
			sqlAux =  " ((ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ") OR " +
			"(exists (" +
			"		select * " +
			"		from ab_tran_event " +
			"		where " +
			"			ab_tran_event.tran_num = ab_tran.tran_num AND " +
			"			ab_tran_event.event_type in ( " +
			+ EVENT_TYPE_ENUM.EVENT_TYPE_AMENDED.toInt() + ","
			+ EVENT_TYPE_ENUM.EVENT_TYPE_BUYOUT.toInt() + ") AND " +
			"			ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED.toInt() + " AND " +
			"			ab_tran_event.event_date = '" + today_dt + "'" +
			"		)" +
			")) ";

		else if(cancelled == 1)
			sqlAux =  " ((ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ") OR " +
			"(exists (" +
			"		select * " +
			"		from ab_tran_event " +
			"		where " +
			"			ab_tran_event.tran_num = ab_tran.tran_num AND " +
			"			ab_tran_event.event_type in (" 
			+ EVENT_TYPE_ENUM.EVENT_TYPE_CANCELLED.toInt() + ","
			+ EVENT_TYPE_ENUM.EVENT_TYPE_BUYOUT.toInt() + ") AND " +
			"			ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + " AND " +
			"			ab_tran_event.event_date = '" + today_dt + "'" +
			"		)" +
			")) ";

		else
			sqlAux =  " ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();

		return sqlAux;
	}

	//------------------------------------------------------------------------------------------------//
	private void PopulateOutputTableWithDeals(String today_dt, String sqlAux) throws OException
	{
		String sqlWhat =
			" SELECT " +
			"	ab_tran.internal_bunit, " +
			"	parameter.index_src, 	" +
			"	parameter.proj_index_tenor, " +
			"	reset.reset_date, 		" +
			"	ab_tran.deal_tracking_num, " +
			"	ab_tran.tran_num, 		" +
			"	ab_tran.tran_status, " +
			"	ab_tran.ins_num, 		" +
			"	ab_tran.ins_type, " + 
			"	reset.start_date, 		" +
			"	reset.end_date, " +
			"	ab_tran.book, 			" +
			"	header.ticker, " +
			"	header.cusip, 			" +
			"	ab_tran.tran_type, " +
			"	ab_tran.toolset, " + 
			"	parameter.proj_index ";
		String sqlFrom = 
			" FROM reset, ab_tran, header, parameter ";
		String sqlWhere = 
			" WHERE reset.reset_date <= '" + today_dt + "'" + 
			"	AND reset.calc_type > 1 " +
			"	AND reset.value_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() +
			"   AND ab_tran.base_ins_type != "+ INS_TYPE_ENUM.comm_inv.toInt() + 
			"	AND ab_tran.ins_num = header.ins_num " +
			"	AND ab_tran.tran_num = header.tran_num " +
			"	AND parameter.ins_num = header.ins_num " +
			"	AND header.ins_num = reset.ins_num " +
			"	AND parameter.ins_num = reset.ins_num " +
			"	AND parameter.param_seq_num = reset.param_seq_num " +
			"	AND ";

		m_INCStandard.Print(error_log_file, "INFO", "Loading data...");
		Table tempTable = Table.tableNew();
		try {
			DBaseTable.execISql(tempTable, sqlWhat+sqlFrom+sqlWhere + sqlAux);
		}
		catch( OException oex )	{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}
		tempTable.addCol("reset_type", COL_TYPE_ENUM.COL_STRING);
		tempTable.setColValString("reset_type", "Price/Rate Reset"); /* only price/rate resets found so far. */
		output.select(tempTable, "*", "tran_num GT 0");
		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void PopulateOutputTableWithDates(String today_dt, String sqlAux) throws OException
	{
		Table tempTable = Table.tableNew(); 

		String sqlWhat = 
			" SELECT" +
			"	ab_tran.internal_bunit,		parameter.proj_index, " +
			"	parameter.spot_index, 		parameter.proj_index_tenor, " +
			"	parameter.index_src, " +
			"	profile_reset.reset_date, 	ab_tran.deal_tracking_num, " +
			"	ab_tran.tran_num, 			ab_tran.tran_status, " +
			"	ab_tran.ins_num, 			ab_tran.ins_type, " + 
			"	ab_tran.book, 				header.ticker, " +
			"	header.cusip, 				ab_tran.tran_type, " +
			"	ab_tran.toolset ";
		String sqlFrom =
			" FROM profile_reset, ab_tran, header, parameter ";
		String sqlWhere = 
			" WHERE profile_reset.reset_date <= '" + today_dt + "'" + 
			"	AND profile_reset.reset_status = "+VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + 
			"   AND ab_tran.base_ins_type != "+ INS_TYPE_ENUM.comm_inv.toInt() + 
			"	AND ab_tran.ins_num = header.ins_num  " +
			"	AND ab_tran.tran_num = header.tran_num  " +
			"	AND parameter.ins_num = header.ins_num  " +
			"	AND header.ins_num = profile_reset.ins_num  " +
			"	AND parameter.ins_num = profile_reset.ins_num  " +
			"	AND parameter.param_seq_num = profile_reset.param_seq_num " +
			"	AND ";

		try	{
			DBaseTable.execISql(tempTable, sqlWhat + sqlFrom + sqlWhere + sqlAux);
		}
		catch( OException oex )	{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		if(tempTable.getNumRows() > 0)	{
			//if the deal has a spot_idx then set it as a proj_idx
			int num_rows = tempTable.getNumRows();
			for (int row  = 1; row <= num_rows; row++){
				int spot_idx = tempTable.getInt("spot_index", row);
				if (spot_idx != 0)
					tempTable.setInt("proj_index", row, spot_idx);
			}

			tempTable.addCol( "start_date", COL_TYPE_ENUM.COL_DATE_TIME);
			tempTable.addCol( "end_date", 	COL_TYPE_ENUM.COL_DATE_TIME);
			tempTable.addCol( "reset_type",	COL_TYPE_ENUM.COL_STRING);
			tempTable.setColValString("reset_type", "Price/Rate Reset"); 
			tempTable.copyCol( "reset_date", tempTable, "start_date");
			tempTable.copyCol( "reset_date", tempTable, "end_date");
			tempTable.delCol("spot_index");
			output.select( tempTable, "*", "tran_num GT 0");
		}

		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void PopulateMissingCFlowResets(String today_dt, String sqlAux) throws OException
	{
		Table tempTable = Table.tableNew(); 
		/*** looking for cash-flow missing resets only ***/
		/* automatically assume it is a spot index and if not depending on the type select proper index or leave the field blank */

		String sqlWhat = 
			"SELECT " +
			"	ab_tran.internal_bunit, 		parameter.spot_index as proj_index," + 
			" 	prh.ref_source as index_src, " +
			" 	parameter.proj_index_tenor, 	physcash.cflow_date as reset_date," +
			" 	ab_tran.deal_tracking_num, 		ab_tran.tran_num," +
			" 	ab_tran.tran_status, 			ab_tran.ins_num," +
			" 	ab_tran.ins_type,				ab_tran.book," +
			" 	header.ticker,					header.cusip," +
			" 	ab_tran.tran_type, 				ab_tran.toolset," +
			"	physcash.cflow_type"; 
		String sqlFrom =
			" FROM  ab_tran, header, parameter, physcash, param_reset_header prh";
		String sqlWhere =
			" WHERE ab_tran.ins_num = header.ins_num 		" +
			"	AND ab_tran.tran_num = header.tran_num " +
			"	AND ab_tran.toolset != "+ TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + //getting commodity deals cflow resets separately
			" 	AND ab_tran.base_ins_type != "+ INS_TYPE_ENUM.comm_inv.toInt() + 
			" 	AND ab_tran.ins_num = parameter.ins_num " +  		
			" 	AND ab_tran.ins_num = physcash.ins_num	" + 
			" 	AND parameter.param_seq_num = physcash.param_seq_num " + 
			" 	AND parameter.ins_num = header.ins_num 	" +
			"	AND physcash.cflow_status = "+ VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() +
			" 	AND header.ins_num = physcash.ins_num 	" +
			"	AND  parameter.ins_num = physcash.ins_num  " +
			" 	AND physcash.cflow_date <= '" + today_dt + "'" + 
			" 	AND prh.ins_num = parameter.ins_num and prh.param_seq_num = parameter.param_seq_num "+	
			"   AND";

		try {
			DBaseTable.execISql(tempTable, sqlWhat + sqlFrom + sqlWhere + sqlAux);
		}
		catch( OException oex )	{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		if(tempTable.getNumRows() > 0){
			tempTable.addCol( "start_date", COL_TYPE_ENUM.COL_DATE_TIME);
			tempTable.addCol( "end_date", 	COL_TYPE_ENUM.COL_DATE_TIME);
			tempTable.addCol( "reset_type",	COL_TYPE_ENUM.COL_STRING);
			tempTable.copyCol( "reset_date", tempTable, "start_date");
			tempTable.copyCol( "reset_date", tempTable, "end_date");

			int num_rows = tempTable.getNumRows();

			/*** check for the proj_index = 0 and look for the proper curve in the underlying instrument 
			 * CFLOW_TYPE.PREPAYMENT_PRINCIPAL_CFLOW - spot_index of the underlying instrument
			 * CFLOW_TYPE.COUPON_PAYMENT_CFLOW - proj_index of the underlying instrument
			 * if the above logic did not set the proj_index then for the rest of the deals the proj_index will remain blank */			

			//preparing table for the underlying transactions and respective projIndex and spot_index
			Table insNumTable = Table.tableNew();
			insNumTable.addCol("ins_num", COL_TYPE_ENUM.COL_INT);
			tempTable.copyColDistinct("ins_num", insNumTable, "ins_num");
			int numRows = insNumTable.getNumRows();
			String unique_ins = "";
			for(int i = 1; i <= numRows; i++) {	
				unique_ins += insNumTable.getInt( "ins_num", i);
				if(i < numRows) unique_ins+= ", ";
			}
			insNumTable.destroy();
			Table underlyingIdx = Table.tableNew();
			getUnderlyingIdx(underlyingIdx, "tran_underlying_link", "underlying_tran", unique_ins);
			getUnderlyingIdx(underlyingIdx, "constituent_underlying_data", "underlying_tran_num", unique_ins);
			getUnderlyingIdx(underlyingIdx, "ins_component_map", "underlying_tran_num", unique_ins);
			underlyingIdx.makeTableUnique();

			Table prepayPrinciplePay = Table.tableNew();
			prepayPrinciplePay.select(tempTable, "*", "proj_index EQ 0 and cflow_type EQ "+CFLOW_TYPE.PREPAYMENT_PRINCIPAL_CFLOW.toInt());
			if(prepayPrinciplePay.getNumRows() > 0){
				prepayPrinciplePay.select(underlyingIdx, "spot_index(proj_index)", " deriv_ins_num EQ $ins_num ");
				tempTable.select(prepayPrinciplePay, "*", "tran_num EQ $tran_num and reset_date EQ $reset_date " +
						" and cflow_type EQ "+CFLOW_TYPE.PREPAYMENT_PRINCIPAL_CFLOW.toInt());
			}
			prepayPrinciplePay.destroy();

			Table couponPayment = Table.tableNew();
			couponPayment.select(tempTable, "*", "proj_index EQ 0 and cflow_type EQ "+ CFLOW_TYPE.COUPON_PAYMENT_CFLOW.toInt());
			if(couponPayment.getNumRows() > 0){
				couponPayment.select(underlyingIdx, "proj_index", " deriv_ins_num EQ $ins_num");
				tempTable.select(prepayPrinciplePay, "*", "tran_num EQ $tran_num and reset_date EQ $reset_date " +
						" and cflow_type EQ "+CFLOW_TYPE.COUPON_PAYMENT_CFLOW.toInt());
			}
			couponPayment.destroy();
			underlyingIdx.destroy();

			//set the reset type string
			for (int row  = 1; row <= num_rows; row++){
				int reset_type = tempTable.getInt("cflow_type", row);
				tempTable.setString("reset_type", row, "Cash Flow - " + Ref.getShortName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, reset_type));

			}//end for			
			tempTable.delCol("cflow_type");
			output.select( tempTable, "*", "tran_num GT 0");
		}
		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void getUnderlyingIdx(Table underlyingIdx, String tableName, String tranField, String unique_ins) throws OException
	{
		DBaseTable.execISql(underlyingIdx,  
				"SELECT tul.ins_num as deriv_ins_num, tul."+tranField+", h.ins_num as underlying_ins_num, " +
				"	p.proj_index, p.spot_index " +
				" FROM "+ tableName + " tul, header h, parameter p " +
				" WHERE h.tran_num = tul."+tranField+" and p.ins_num = h.ins_num  and tul.param_seq_num = p.param_seq_num " +
				" and p.ins_num IN (" + unique_ins + ")");
	}

	//------------------------------------------------------------------------------------------------//

	private void PopulateMissingCFlowCommodityResets(String today_dt, String sqlAux) throws OException
	{
		Table tempTable = Table.tableNew(); 
		/* looking for cash-flow missing resets for commodities */
		String sqlWhat = 
			"SELECT " +
			" 	ab_tran.internal_bunit, 		parameter.proj_index," + 
			" 	parameter.proj_index_tenor,  	physcash.cflow_date as reset_date," +
			"	parameter.index_src, " +//getting index src
			" 	ab_tran.deal_tracking_num,	 	ab_tran.tran_num," +
			" 	ab_tran.tran_status, 		 	ab_tran.ins_num," +
			" 	ab_tran.ins_type, 				ab_tran.book," +
			" 	header.ticker, 				 	header.cusip," +
			" 	ab_tran.tran_type, 				ab_tran.toolset," +
			" 	physcash.cflow_type ";
		String sqlFrom =
			" FROM  ab_tran, header, parameter, physcash ";
		String sqlWhere	=
			" WHERE ab_tran.ins_num = header.ins_num 		AND ab_tran.tran_num = header.tran_num " +
			" 	AND ab_tran.toolset = "+ TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + 
			" 	AND ab_tran.base_ins_type != "+ INS_TYPE_ENUM.comm_inv.toInt() + 
			" 	AND ab_tran.ins_num = parameter.ins_num " +  		
			" 	AND ab_tran.ins_num = physcash.ins_num	" + 
			" 	AND parameter.param_seq_num = physcash.param_seq_num " +
			" 	AND parameter.ins_num = header.ins_num 	AND physcash.cflow_status = "+VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() +
			" 	AND header.ins_num = physcash.ins_num 	AND  parameter.ins_num = physcash.ins_num  " +
			" 	AND physcash.cflow_date <= '"+today_dt +"' " + 
			" 	AND ";

		try {
			DBaseTable.execISql(tempTable, sqlWhat + sqlFrom +sqlWhere + sqlAux);
		}
		catch( OException oex )	{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		if(tempTable.getNumRows() > 0){
			tempTable.addCol( "start_date", COL_TYPE_ENUM.COL_DATE_TIME);
			tempTable.addCol( "end_date", 	COL_TYPE_ENUM.COL_DATE_TIME);
			tempTable.addCol( "reset_type",	COL_TYPE_ENUM.COL_STRING);
			tempTable.addCol( "index_src", COL_TYPE_ENUM.COL_INT);
			tempTable.copyCol( "reset_date", tempTable, "start_date");
			tempTable.copyCol( "reset_date", tempTable, "end_date");

			int num_rows = tempTable.getNumRows();

			for (int row  = 1; row <= num_rows; row++){
				int reset_type = tempTable.getInt("cflow_type", row);
				tempTable.setString("reset_type", row, "Cash Flow - " + Ref.getShortName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, reset_type));				
			}					
			//copy tempTable to output
			tempTable.delCol("cflow_type");
			output.select( tempTable, "*", "tran_num GT 0");
		}
		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void PopulateMissingNotionalResets(String today_dt, String sqlAux) throws OException
	{
		Table tempTable = Table.tableNew(); 
		/* looking for notional missing resets */
		String sqlWhat = 
			"SELECT " +
			"	ab_tran.internal_bunit, parameter.proj_index_tenor, " +
			//" parameter.proj_index, parameter.index_src, " + 
			//param_index field and index.src are intentionally left blank
			" 	rst.reset_date, 		ab_tran.deal_tracking_num, " +
			" 	ab_tran.tran_num,	  	ab_tran.tran_status, " +
			" 	ab_tran.ins_num,		ab_tran.ins_type, " +
			" 	ab_tran.book,			header.ticker, " +
			" 	header.cusip,		 	ab_tran.tran_type,  " +
			" 	ab_tran.toolset,  		rst.reset_calc_type, " +
			" 	rst.reset_start_date as start_date, " +
			" 	rst.reset_end_date as end_date ";
		String sqlFrom = 
			" FROM ab_tran, rst_param_reset_tree rst, header, parameter" ;
		String sqlWhere = 
			" WHERE rst.reset_date <= '"+today_dt+"' " + 
			" 	AND rst.reset_value_status =  " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() +
			" 	AND ab_tran.ins_num = rst.ins_num " +
			" 	AND header.ins_num = ab_tran.ins_num " +
			" 	AND parameter.ins_num = ab_tran.ins_num " +
			" 	AND parameter.param_seq_num = rst.param_seq_num " +
			"	AND ab_tran.base_ins_type != "+ INS_TYPE_ENUM.comm_inv.toInt() +
			" 	AND "; 

		try {
			DBaseTable.execISql(tempTable, sqlWhat + sqlFrom + sqlWhere + sqlAux);
		}
		catch( OException oex ) {
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		if(tempTable.getNumRows() > 0) {
			tempTable.addCol( "reset_type",	COL_TYPE_ENUM.COL_STRING);//
			int num_rows = tempTable.getNumRows();
			for (int row  = 1; row <= num_rows; row++){
				int reset_type = tempTable.getInt("reset_calc_type", row);
				tempTable.setString("reset_type", row, "Profile Reset - " +Ref.getShortName(SHM_USR_TABLES_ENUM.RESET_PARAM_CALC_TYPE_TABLE, reset_type)); 

			}
		}
		tempTable.delCol("reset_calc_type");
		output.select( tempTable, "*", "tran_num GT 0");
		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void PopulateMissingNotionalCurrencyResets(String today_dt, String sqlAux) throws OException
	{
		Table tempTable = Table.tableNew(); 
		/* looking for profile notional currency indexed missing resets */
		String sqlWhat = 
			"SELECT" +
			" 	ab_tran.internal_bunit, 		pcp.fx_index as proj_index," +
			"	parameter.index_src, " +  
			" 	parameter.proj_index_tenor, 	pnr.reset_date, " +
			" 	pnr.rfis_date as start_date, 	ab_tran.deal_tracking_num, " +
			" 	ab_tran.tran_num,				ab_tran.tran_status, " +
			" 	ab_tran.ins_num,  				ab_tran.ins_type, " +
			" 	ab_tran.book,  					header.ticker, " +
			" 	header.cusip,  					ab_tran.tran_type,  " +
			" 	ab_tran.toolset ";
		String sqlFrom = 
			" FROM ab_tran, profile_notnl_reset pnr, header, profile_currency_param pcp, parameter ";
		String sqlWhere	=
			" WHERE pnr.reset_date <= '"+today_dt+"' " + 
			" 	AND pnr.reset_status = " +VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() +
			" 	AND ab_tran.ins_num = pnr.ins_num " +
			" 	AND header.ins_num = ab_tran.ins_num " +
			" 	AND parameter.ins_num = ab_tran.ins_num " +
			" 	AND parameter.param_seq_num = pcp.param_seq_num " +
			" 	AND ab_tran.ins_num = pcp.ins_num " +
			" 	AND pnr.param_seq_num = pcp.param_seq_num "+
			" 	AND ab_tran.base_ins_type != "+ INS_TYPE_ENUM.comm_inv.toInt() + " AND ";

		try	{
			DBaseTable.execISql(tempTable, sqlWhat + sqlFrom + sqlWhere + sqlAux);
		}
		catch( OException oex )	{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		if(tempTable.getNumRows() > 0){
			tempTable.addCol( "reset_type",	COL_TYPE_ENUM.COL_STRING);
			int num_rows = tempTable.getNumRows();
			for (int row  = 1; row <= num_rows; row++){
				tempTable.setString("reset_type", row, "Profile Notional Reset");
			}
		}				 
		/* copy tempTable to output */
		tempTable.delCol("cflow_type");
		output.select( tempTable, "*", "tran_num GT 0");
		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void ExcludeExchInsFromOutput() throws OException
	{
		Table tempTable = Table.tableNew(); 

		String sqlStmt = 
			" SELECT distinct p.ins_num, p.settlement_type FROM parameter p, ab_tran a " +
			" WHERE p.ins_num = a.ins_num AND " +
			"		a.base_ins_type in ( " +
			+ INS_TYPE_ENUM.energy_exch_avg_forward.toInt() + ", " 
			+ INS_TYPE_ENUM.energy_exch_avg_future.toInt() + ", " 
			+ INS_TYPE_ENUM.energy_exch_future.toInt() + ", " 
			+ INS_TYPE_ENUM.mtl_exch_forward.toInt() + ", " 
			+ INS_TYPE_ENUM.mtl_exch_future.toInt() + ", " 
			+ INS_TYPE_ENUM.soft_exch_future.toInt() + ", " 
			+ INS_TYPE_ENUM.prec_exch_future.toInt() + " ) AND " +
			"		p.settlement_type = 2";
		try	{
			DBaseTable.execISql(tempTable, sqlStmt);
		}
		catch( OException oex )	{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		if(tempTable.getNumRows() > 0){
			output.select( tempTable, "settlement_type", "ins_num EQ $ins_num");
			output.deleteWhereValue( "settlement_type", 2);
			output.delCol( "settlement_type");
		} 
		tempTable.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private void FormatOutputTable() throws OException
	{		
		output.delCol("toolset");
		output.delCol("tran_type");

		output.setColFormatAsRef( "internal_bunit", 	 SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output.setColFormatAsRef( "tran_status", 		 SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE); 
		output.setColFormatAsRef( "ins_type", 			 SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		output.setColFormatAsRef( "proj_index", 		 SHM_USR_TABLES_ENUM.INDEX_TABLE);
		output.setColFormatAsDate( "reset_date");
		output.setColFormatAsRef(  "index_src", 		 SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
		output.setColFormatAsRef(  "internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);

		output.setColFormatAsPymtPeriod( "proj_index_tenor");
		output.setColFormatAsDate( "start_date");
		output.setColFormatAsDate( "end_date");

		output.formatSetJustifyLeft( "internal_bunit");
		output.formatSetJustifyLeft( "tran_status");
		output.formatSetJustifyLeft( "ins_type");
		output.formatSetJustifyLeft( "proj_index");
		output.formatSetJustifyLeft( "proj_index_tenor");
		output.formatSetJustifyLeft( "index_src");
		output.formatSetJustifyLeft( "reset_date");
		output.formatSetJustifyLeft( "start_date");
		output.formatSetJustifyLeft( "end_date");
		output.formatSetJustifyLeft( "reset_type");

		output.formatSetWidth( "reset_date",         12);
		output.formatSetWidth( "start_date",         12);
		output.formatSetWidth( "end_date",           12);
		output.formatSetWidth( "internal_bunit",     45);
		output.formatSetWidth( "proj_index",         20);
		output.formatSetWidth( "index_src",          15);
		output.formatSetWidth( "deal_tracking_num",   9);
		output.formatSetWidth( "tran_num",            9);
		output.formatSetWidth( "tran_status",        15);
		output.formatSetWidth( "ins_num",             9);
		output.formatSetWidth( "ins_type",           20);
		output.formatSetWidth( "book",               20);
		output.formatSetWidth( "cusip",              20);
		output.formatSetWidth( "ticker",             20);
		output.formatSetWidth( "reset_type",	     35); 
	}

	//------------------------------------------------------------------------------------------------//

	private void FormatOutputTableForReport(int report_type) throws OException
	{
		String space;

		if(report_type == 2) space = "";
		else space = " \n";

		output.setColTitle( "internal_bunit",    space + "Internal\nBunit");
		output.setColTitle( "tran_num",          space + "Tran\nNum");
		output.setColTitle( "tran_status",       space + "Tran\nStatus");
		output.setColTitle( "deal_tracking_num", space + "Deal\nNum");
		output.setColTitle( "ins_type",          space + "Ins\nType");      
		output.setColTitle( "proj_index",        space + "Projection\nIndex");
		output.setColTitle( "index_src",         space + "Index\nSource");
		output.setColTitle( "book",              space + " \nBook");
		output.setColTitle( "ins_num",           space + "Ins\nNum");
		output.setColTitle( "reset_date",        space + "Reset\nDate"); 
		output.setColTitle( "start_date",        space + "Start\nDate");
		output.setColTitle( "end_date",          space + "End\nDate");
		output.setColTitle( "ticker",            space + " \nTicker");
		output.setColTitle( "cusip",             space + " \nCusip");
		output.setColTitle( "reset_type",        space + "Reset\nType");

		output.formatSetJustifyRight( "reset_date");
		output.formatSetJustifyRight( "start_date");
		output.formatSetJustifyRight( "end_date");

		output.colHide("cflow_type");
		if(report_type == 2){
			// Hide the proj_index_tenor column to OConsole.oprint Finance report
			output.colHide( "proj_index_tenor");
			output.colHide( "start_date");
			output.colHide( "end_date");			
		}else{
			// Hide the ticker and cusip columns
			output.colHide( "ticker");
			output.colHide( "cusip");
			output.setColTitle( "proj_index_tenor",  "Projection\nIndex\nTenor"); 
		}
	}

	//------------------------------------------------------------------------------------------------//

	private void GroupTables(Table fmt_output) throws OException
	{
		/* Sort by Business Unit, Reset Date, Projection Index, Index Source, deal num, tran num and ins num. */ 
		fmt_output.clearGroupBy();
		fmt_output.groupFormatted( "internal_bunit, reset_date, proj_index, index_src,  deal_tracking_num, tran_num, start_date");

		/* Sort by Business Unit, Reset Date, Projection Index, Index Source, deal num, tran num and ins num.  */
		output.clearGroupBy();
		output.groupFormatted( "internal_bunit, reset_date, proj_index, index_src,  deal_tracking_num, tran_num, start_date");
	}

	//------------------------------------------------------------------------------------------------//

	private void GroupOutputTable() throws OException
	{
		/* Sort by Business Unit, Reset Date, Projection Index, Index Source, deal num, tran num and ins num. */
		output.clearGroupBy();
		output.groupFormatted( "internal_bunit, reset_date,  proj_index, index_src, deal_tracking_num, tran_num, start_date");
		output.groupTitleAbove( "internal_bunit");
		output.colHide( "internal_bunit");
	}
}