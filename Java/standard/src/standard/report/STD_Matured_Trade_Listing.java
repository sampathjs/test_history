/*$Header: /cvs/master/olf/plugins/standard/report/STD_Matured_Trade_Listing.java,v 1.11 2013/04/19 18:00:25 rdesposi Exp $*/

/*
File Name:                      STD_Matured_Trade_Listing.java

Report Name:                    Matured Trade Listing

Output File Name                [Business Unit Name].Matured_Trades.tlt
                                STD_Matured_Trade_Listing.rpt
                                STD_Matured_Trade_Listing.csv
                                STD_Matured_Trade_Listing.html
                                STD_Matured_Trade_Listing.pdf
                                STD_Matured_Trade_Listing.log
                                USER_Matured_Trades

Available RptMgr Outputs:       m_INCStandard.Print Crystal
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

Revision History                Jan 23, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                Oct 05, 2010 - Replaced function calls to fill* with a call to select()
                                             - Restructured code into smaller functions
                                Sep 28, 2010 - Wrapped function calls to DBaseTable.execISql with a try-catch block
                                Aug 06, 2010 - Replaced function calls to DBaseTable.loadFromDb(WithWhatWhereSQL) with calls to DBaseTable.execISql(...). 
                                             - Replaced function calls to the OpenJVS String library with calls to the standard Java String library. 
                                             - Replaced function calls to Util.exitFail() with throwing an OException. 
                                Mar 03, 2005 - Add Util.exitFail() check for output generation
                                Feb 24, 2005 - Configured script to run with INC_Standard
                                Nov 30, 2004 - Added m_INCStandard.save_local fix, new htmlViewer, retval check for crystal, removed last update, updated by cols
                                Oct 04, 2004 - Added View/Save CRYSTAL_EXPORT_TYPES.HTML/CRYSTAL_EXPORT_TYPES.PDF Functionality, Workflow Flag
                                Sep 22, 2004 - Changed Param Script from STD_Business_Unit_Param, added Report Manager Functionality
                                Mar 29, 2004 - Formatted Last Update field.

Parameter Script:               STD_Start_End_BUnit_Param.java (no parameter runs for OCalendar.today() for  all Business Units
Display Script:                 None

Report Description:             
For each business unit in the parameter script, this report will search the
database for all trades that Mature within the given date range.

Recommended Script Category? 	N/A

Columns:
   Deal Num              ab_tran.deal_tracking_num
   Toolset               ab_tran.toolset
   Instrument Type       ab_tran.ins_type
   Trade Date            ab_tran.trade_date
   Maturity Date         ab_tran_event.event_date
   Counterparty          ab_tran.external_lentity
   Position              ab_tran.position
   Unit                  parameter.unit
   Ccy                   ab_tran.currency
   Portfolio             ab_tran.internal_portfolio
   Trader                ab_tran.internal_contact

Group by: Business Unit
Sort by:  Deal Num
 */

package standard.report;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions = false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class STD_Matured_Trade_Listing implements IScript {

	private JVS_INC_Standard m_INCStandard;

	public STD_Matured_Trade_Listing() {
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();

		int start_date, end_date, numRows, x, bunit, exit_fail;

		String str_Start, str_End, date_string, date_string_end, period_string, event_where_clause, tran_where_clause;
		String what, where;
		String bunit_aux, reporttitle, filename, tabletitle;
		String sFileName = "STD_Matured_Trade_Listing";
		String sReportTitle = "Matured Trade Listing";
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);
		String errorMessages = "";
		String queryTableName;
		Table party, temp, event_table, abtran_table, param_table, output, tblTemp, ins_num_list, tCrystal, tParam;

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		party = Table.tableNew();

		/* Check to see that this Script was run with a Param Script */
		if (argt.getNumRows() == 0 || argt.getColNum("bunits") <= 0) {
			m_INCStandard.Print(error_log_file, "INFO", "This script can take STD_Start_End_BUnit_Param.java");
			m_INCStandard.Print(error_log_file, "INFO", "No Param Script Found - Running for today with all Business Units");

			/*** Set Default Values ***/
			start_date = OCalendar.today();
			end_date = OCalendar.today();

			temp = Table.tableNew();
			
			try {
				DBaseTable.execISql(temp, "SELECT party_id FROM party WHERE party_class=1 AND int_ext=0");
			} catch (OException oex) {
				m_INCStandard.Print(error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage());
			}
			
			party.select(temp, "party_id(bunit)", "party_id GT 0");
			temp.destroy();
			
		} else {
			start_date = argt.getInt("start_date", 1);
			end_date = argt.getInt("end_date", 1);
		
			temp = argt.getTable("bunits", 1).copyTable();
			party.select(temp, "return_val(bunit)", "return_val GT 0");
			temp.destroy();
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file, argt);
		exit_fail = 0;

		str_Start = OCalendar.formatJdForDbAccess(start_date);
		str_End   = OCalendar.formatJdForDbAccess(end_date);

		date_string = OCalendar.formatDateInt(start_date);
		date_string_end = OCalendar.formatDateInt(end_date);  

		if(start_date == end_date) {
			period_string = " For " + date_string;
		} else {
			period_string = " From " + date_string  + " To " + date_string_end;
		}
			

		event_table = CreateEventTable(); 
		abtran_table = CreateAbtranTable(); 
		param_table = CreateParamTable(); 
		output = CreateOutputTable(); 

		FormatOutputTable(output);

		tblTemp = output.cloneTable();
		output.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		/* hide unused columns */
		output.colHide( "ins_num");
		output.colHide( "internal_bunit"); 
		output.colHide( "tran_num");

		/* start the loop for each business unit */
		int eventDateQid;
		int insNumQid;
		
		numRows = party.getNumRows();
		
		for (x = 1; x <= numRows; x++) {
			event_table.clearRows();
			param_table.clearRows();
			abtran_table.clearRows();

			bunit = party.getInt("bunit", x);

			/* Do the Matured Trade Listing Report */
			/* Make a where clause for that report */
			event_where_clause = "WHERE event_type = "
					+ EVENT_TYPE_ENUM.EVENT_TYPE_CLOSE.toInt()
					+ " and event_date >= '" + str_Start + "'"
					+ " and event_date <= '" + str_End + "'";

			try {
				DBaseTable.execISql(event_table, "SELECT tran_num,event_date,para_position FROM ab_tran_event " + event_where_clause);
			} catch (OException oex) {
				m_INCStandard.Print(error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage());
			}
			
			if (event_table.getNumRows() > 0 )
			{
				/* fill in the event info into the output table */
	            what = "event_date(mat_date)";
	            where = "tran_num EQ $tran_num";
	    		output.select(event_table, what, where);
	    		
				eventDateQid = Query.tableQueryInsert(event_table, "tran_num", "query_result_plugin");
	
				tran_where_clause = "WHERE tran_status IN ("
						+ TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ", "
						+ TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()
						+ ") AND internal_bunit = " + bunit
						+ " AND query_result = tran_num AND unique_id = "
						+ eventDateQid;
	            
	            queryTableName = Query.getResultTableForId(eventDateQid);
	            if ( queryTableName == null && eventDateQid > 0 )
		        {
		        	queryTableName = "query_result_plugin";
		        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + eventDateQid 
		        			+ " does not have a query result table. Default " + queryTableName + " table will be used.");
		        }
	            
				try {
					DBaseTable.execISql( abtran_table, "SELECT deal_tracking_num,tran_num,ins_num,toolset,ins_type,external_lentity," +
						"trade_date,currency,internal_portfolio,position,internal_contact,internal_bunit " +
						"FROM ab_tran, " + queryTableName + " "+ tran_where_clause );
				}
				catch( OException oex ) {
					m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
				}
				/* fill in the ab tran information in the output table */
				what = "deal_tracking_num, tran_num, ins_num, toolset, ins_type, external_lentity, trade_date, currency, "
						+ "internal_portfolio(int_portfolio), position, internal_contact, internal_bunit";
				where = "deal_tracking_num GT 0";
				output.select(abtran_table, what, where);
	
				Query.clear( eventDateQid );
				if (abtran_table.getNumRows() > 0)
				{
					/* make a list of instrument numbers */
					ins_num_list = Table.tableNew();
					ins_num_list.addCol( "ins_num", COL_TYPE_ENUM.COL_INT);
					abtran_table.copyColDistinct( "ins_num", ins_num_list, "ins_num");
					
					/* get para info from db for ins_nums in list */
					insNumQid = Query.tableQueryInsert( ins_num_list, "ins_num", "query_result_plugin" );
					
					queryTableName = Query.getResultTableForId(insNumQid);
					if ( queryTableName == null && insNumQid > 0 )
			        {
			        	queryTableName = "query_result_plugin";
			        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + insNumQid 
			        			+ " does not have a query result table. Default " + queryTableName + " table will be used.");
			        }
					
					try {
						DBaseTable.execISql( param_table, "SELECT DISTINCT ins_num, unit FROM parameter, " + queryTableName +
							" WHERE ins_num = query_result AND unique_id = " + insNumQid );
					}
					catch( OException oex ) {
						m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
					}
					Query.clear( insNumQid );
					
					/* fill in the param info into the output table */
		            what = "unit";
		            where = "ins_num EQ $ins_num";
		    		output.select(param_table, what, where);
		    		
		    		
		    		ins_num_list.destroy();
				}
			}
			
			output.colHide( "tran_num");
			output.colHide( "ins_num");
			output.copyRowAddAll( tblTemp);

			/* sort the output table by deal tracking number */
			output.group( "deal_tracking_num");

			bunit_aux = Table.formatRefInt(bunit, SHM_USR_TABLES_ENUM.PARTY_TABLE);
			reporttitle = "Matured Trades for " + bunit_aux;
			filename = bunit_aux + ".Matured_Trades.tlt";

			if (m_INCStandard.report_viewer != 0) {
				tabletitle = sReportTitle + period_string + "\nBusiness Unit: " + bunit_aux;
				m_INCStandard.STD_PrintTextReport(output, filename, reporttitle, tabletitle, error_log_file);
			}

			
			output.clearRows(); // very important
		}

		tblTemp.formatSetJustifyRight("last_update");

		tCrystal = tblTemp.copyTableFormatted(0);

		FormatTempTable(tblTemp);

		/* View Table */
		if(m_INCStandard.view_table != 0) {
			m_INCStandard.STD_ViewTable(tblTemp, sReportTitle + period_string, error_log_file); 
		}

		/* Dump to CSV */
		if(m_INCStandard.csv_dump != 0) {
			m_INCStandard.STD_PrintTableDumpToFile(tblTemp, sFileName, sReportTitle + period_string, error_log_file);
		}   

		/* Create USER Table */            
		if(m_INCStandard.user_table != 0) {
			temp = Table.tableNew();
			temp.select( tblTemp, "*", "deal_tracking_num GT -1");
			
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_Matured_Trades", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				errorMessages = errorMessages + "Error value returned from m_INCStandard.STD_SaveUserTable ";
				exit_fail = 1;
			}
			temp.destroy();
		}

		/* Generate/Save/m_INCStandard.Print Crystal Report Output */
		if(m_INCStandard.STD_UseCrystalOutput() != 0) {
			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle + period_string, sFileName);

//			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0) {
//				exit_fail = 1;
//				errorMessages = errorMessages + "Error value returned from m_INCStandard.STD_OutputCrystal"; 
//			}
			tParam.destroy();
		}

		tCrystal.destroy();
		abtran_table.destroy();
		param_table.destroy();
		event_table.destroy();
		output.destroy();
		party.destroy();
		tblTemp.destroy();

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");

		if(exit_fail != 0)
		   throw new OException( errorMessages );

		return;
	}

	private Table CreateEventTable() throws OException
	{
		Table event_table = Table.tableNew();
		
		event_table.addCol( "tran_num", 	 COL_TYPE_ENUM.COL_INT);
		event_table.addCol( "event_date", 	 COL_TYPE_ENUM.COL_INT);
		event_table.addCol( "para_position", COL_TYPE_ENUM.COL_DOUBLE);
		
		return event_table;
	}
	
	private Table CreateAbtranTable() throws OException {
		Table abtran_table = Table.tableNew();
	
		abtran_table.addCol( "deal_tracking_num",  COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "tran_num",		   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "ins_num", 		   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "toolset", 		   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "ins_type", 		   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "external_lentity",   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "trade_date",		   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "currency", 		   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "internal_portfolio", COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "position", 		   COL_TYPE_ENUM.COL_DOUBLE); 
		abtran_table.addCol( "internal_contact",   COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "internal_bunit", 	   COL_TYPE_ENUM.COL_INT);
		
		return abtran_table;
	}

	private Table CreateParamTable() throws OException
	{
		Table param_table = Table.tableNew();
	
		param_table.addCol( "ins_num", COL_TYPE_ENUM.COL_INT);
		param_table.addCol( "unit",	   COL_TYPE_ENUM.COL_INT);   
		
		return param_table;
	}

	private Table CreateOutputTable() throws OException 
	{
		Table output = Table.tableNew();
		
		output.addCol( "deal_tracking_num", COL_TYPE_ENUM.COL_INT);
		output.addCol( "tran_num", 			COL_TYPE_ENUM.COL_INT);   
		output.addCol( "ins_num", 			COL_TYPE_ENUM.COL_INT);   
		output.addCol( "toolset", 			COL_TYPE_ENUM.COL_INT);  
		output.addCol( "ins_type", 			COL_TYPE_ENUM.COL_INT);   
		output.addCol( "trade_date", 		COL_TYPE_ENUM.COL_INT);  
		output.addCol( "mat_date", 			COL_TYPE_ENUM.COL_INT);   
		output.addCol( "external_lentity", 	COL_TYPE_ENUM.COL_INT); 
		output.addCol( "position", 			COL_TYPE_ENUM.COL_DOUBLE);  
		output.addCol( "unit", 				COL_TYPE_ENUM.COL_INT);   
		output.addCol( "currency", 			COL_TYPE_ENUM.COL_INT);   
		output.addCol( "int_portfolio", 	COL_TYPE_ENUM.COL_INT);   
		output.addCol( "internal_contact", 	COL_TYPE_ENUM.COL_INT);
		output.addCol( "internal_bunit", 	COL_TYPE_ENUM.COL_INT);

		return output;
	}

	private void FormatOutputTable(Table output) throws OException 
	{
		/* set column formatting */
		output.setRowHeaderWidth( 1);
		
		output.setColFormatAsRef( "ins_num", 			SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		output.setColFormatAsRef( "toolset", 			SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
		output.setColFormatAsRef( "ins_type", 			SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		output.setColFormatAsRef( "internal_contact", 	SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
		output.setColFormatAsRef( "internal_bunit", 	SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output.setColFormatAsRef( "external_lentity", 	SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output.setColFormatAsRef( "currency",			SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		output.setColFormatAsRef( "int_portfolio",		SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		output.setColFormatAsRef( "unit", 				SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		
		output.setColFormatAsDate( "trade_date");
		output.setColFormatAsDate( "mat_date");

		output.formatSetJustifyRight( "trade_date");
		output.formatSetJustifyRight( "mat_date");

		output.formatSetWidth( "ins_type", 			15);
		output.formatSetWidth( "trade_date", 		12);
		output.formatSetWidth( "mat_date", 			12);
		output.formatSetWidth( "internal_contact", 	35);
		output.formatSetWidth( "external_lentity", 	35);
		output.formatSetWidth( "internal_bunit", 	35);
		output.formatSetWidth( "int_portfolio", 	35);
		output.formatSetWidth( "unit", 				8);

		/* set output titles */
		output.setColTitle( "deal_tracking_num", 	"Deal\nNum");
		output.setColTitle( "toolset", 				" \nToolset");
		output.setColTitle( "ins_type", 			"Instrument\nType");
		output.setColTitle( "external_lentity", 	" \nCounterparty");
		output.setColTitle( "trade_date", 			"Trade\nDate");
		output.setColTitle( "mat_date", 			"Maturity\nDate");
		output.setColTitle( "position", 			" \nPosition");
		output.setColTitle( "currency", 			" \nCcy");
		output.setColTitle( "int_portfolio", 		" \nPortfolio");
		output.setColTitle( "internal_contact", 	" \nTrader");
		output.setColTitle( "unit", 				" \nUnit");
	}

	private void FormatTempTable(Table tblTemp) throws OException 
	{
		tblTemp.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		tblTemp.delCol( "ins_num");
		tblTemp.setColTitle( "internal_bunit", "Internal\nBusiness Unit");
		tblTemp.colHide( "tran_num");
	}
}