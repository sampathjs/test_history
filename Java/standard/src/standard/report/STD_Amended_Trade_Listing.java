/*$Header: /cvs/master/olf/plugins/standard/report/STD_Amended_Trade_Listing.java,v 1.11.8.1 2014/08/04 17:52:55 chrish Exp $*/

/*
File Name:                      STD_Amended_Trade_Listing.java

Report Name:                    Amended Trade Listing

Output File Name:               [Business Unit Name].Amended_Trades.tlt 
                                STD_Amended_Trade_Listing.rpt   
                                STD_Amended_Trade_Listing.csv   
                                STD_Amended_Trade_Listing.html   
                                STD_Amended_Trade_Listing.pdf  
                                STD_Amended_Trade_Listing.log   
                                USER_Amended_Trades

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

Date Of Last Revision:          Jan 23, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                Dec 28, 2012 - DTS103465: Output empty table with success status when tran_num_table is an empty table.
                                Oct 21, 2010 - Broke up code into smaller functions; removed compiler warnings
					     - Replaced function calls to DBaseTable.loadFromDbWithWhatWhere with calls to DBaseTable.execISql
                                Jul 12, 2010 - Replaced Util.exitFail w/ throw OException
                                Jun 30, 2010 - Replaced loadFromDbWithWhatWhere with DBaseTable.execISql
					     - Replaced OpenLink String library with Java String functions
                                Mar 03, 2005 - Add Util.exitFail() check for output generation
                                Feb 18, 2005 - Update script to run with INC_Standard
                                Nov 23, 2004 - Added RM Param - Energy vs Financial Report generation
                                             - Moved workflow param, added htmlViewer, added retval checks for crystal
                                             - Use ab_tran_event table to query for Amended Open events instead of using ab_tran.last_update
                                Oct  4, 2004 - Added View/Save CRYSTAL_EXPORT_TYPES.HTML/CRYSTAL_EXPORT_TYPES.PDF Functionality, Workflow Flag
                                Sep 17, 2004 - Added Report Manager Functionality
                                Aug 24, 2004 - Change param to STD_Start_End_BUnit_Param 
                                Mar 30, 2004 - Formatted Last Update field.

Parameter Script:               (Optional)STD_Start_End_BUnit_Param.java (runs for all Business Units when param isn't used)

Display Script:                 None

Recommended Script Category? 	N/A

Report Description:             
For the dates specifies, for each business unit 
in the parameter script (or all if no parameter script is specified), 
this report will report the trades that were Amended.

Columns:
  Deal Num               ab_tran.deal_tracking_num
  Tran Num               ab_tran_event.tran_num
  Instrument Type        ab_tran.ins_type
  Trade Date             ab_tran.trade_date
  Amend Date             ab_tran_event.event_date
  Counterparty           ab_tran.external_lentity
  Portfolio              ab_tran.internal_portfolio
  Unit                   parameter.unit
  Ccy                    ab_tran.currency (only in ENERGY report) 
  Position               ab_tran.position (only in ENERGY report)
  Ticker                 header.ticker (only in FINANCIAL report)
  Cusip                  header.cusip  (only in FINANCIAL report)
  ISIN                   header.isin   (only in FINANCIAL report)
  Trader                 ab_tran.internal_contact

  group by: Business Unit
  sort by:  Deal Num, Tran Num

 */

package standard.report;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@ScriptAttributes(allowNativeExceptions=false)
public class STD_Amended_Trade_Listing implements IScript {

	private JVS_INC_Standard m_INCStandard;
	
	private String error_log_file;

	public STD_Amended_Trade_Listing(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		Table party, temp, output, tParam, tCrystal, tblTemp;
		Table tran_num_table, abtran_table, ins_num_list, param_table, header_table;

		int tranNumQueryId = 0, insNumQueryId = 0, start_date, end_date, num_rows, x, bunit, exit_fail = 0;
		int report_type = m_INCStandard.STD_GetReportType();

		String str_Start, str_End, date_string, date_string_end, period_string;
		String what, bunit_aux, filename, queryTableName;
		String sFileName = "STD_Amended_Trade_Listing";
		String sReportTitle = "Amended Trade Listing";
		String errorMessage = "";
		
		error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		party = Table.tableNew();

		/* Check to see that this Script was run with a Parameter Script */
		if(argt.getNumRows() == 0 || argt.getColNum( "bunits") <= 0)
		{
			m_INCStandard.Print(error_log_file, "INFO", "This script can take STD_Start_End_BUnit_Param.java");
			m_INCStandard.Print(error_log_file, "INFO", "No Param Script Found - Running for today with all Business Units");

			/* Set Default Values */
			start_date = OCalendar.today();
			end_date = OCalendar.today();

			LoadPartyTable(party);
		}
		else
		{
			start_date = argt.getInt( "start_date", 1);
			end_date   = argt.getInt( "end_date", 1);
			temp = argt.getTable( "bunits", 1);
			party.select( temp, "return_val(bunit)", "return_val GT 0");
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file, argt); 

		str_Start = OCalendar.formatJdForDbAccess(start_date);
		str_End   = OCalendar.formatJdForDbAccess(end_date + 1);

		date_string		= OCalendar.formatDateInt(start_date);
		date_string_end = OCalendar.formatDateInt(end_date);   
		if(start_date == end_date)
			period_string = "For " + date_string;
		else
			period_string = "From " + date_string  + " To " + date_string_end;

		output = CreateOutputTable();
		tblTemp = output.cloneTable();
		FormatOutputTable(output);

		num_rows = party.getNumRows();

		tran_num_table = CreateTranNumTable(str_Start, str_End);

		if (tran_num_table.getNumRows() == 0)
		{
			m_INCStandard.Print(error_log_file, "INFO", "No transaction info found.");   	
		}
		else
		{
			try
			{
				tranNumQueryId = Query.tableQueryInsert(tran_num_table, "tran_num", "query_result_plugin");   
			}
			catch(OException oex)
			{
				party.destroy();
				output.destroy();
				tblTemp.destroy();
				tran_num_table.destroy();
				m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage()); 
				throw oex;
			}
			/* Start the loop for each business unit */
			for(x = 1; x <= num_rows; x++)
			{     
				bunit = party.getInt( "bunit", x);
				bunit_aux = Table.formatRefInt(bunit, SHM_USR_TABLES_ENUM.PARTY_TABLE);
	
				/* Get transaction info from database */
				abtran_table = GetTransactionInformation(tranNumQueryId, bunit);

				/* Add Event Date to table */
				abtran_table.select( tran_num_table, "event_date", "tran_num EQ $tran_num");
	
				/* Make a list of instrument numbers */
				ins_num_list = Table.tableNew();
				ins_num_list.addCol( "ins_num",COL_TYPE_ENUM.COL_INT);
				abtran_table.copyColDistinct( "ins_num", ins_num_list, "ins_num");
	
				if (ins_num_list.getNumRows() > 0 )
				{
					insNumQueryId = Query.tableQueryInsert(ins_num_list, "ins_num", "query_result_plugin");
				}
				else
				{
					m_INCStandard.Print(error_log_file, "INFO", " No instrument info were found for " + bunit_aux + ".");
					continue;
				}
				
		        queryTableName = Query.getResultTableForId(insNumQueryId);
		        
		        if ( queryTableName == null && insNumQueryId > 0)
		        {
		        	queryTableName = "query_result_plugin";
		        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + insNumQueryId + " does not have a query result table. Default " + queryTableName + " table will be used.");
		        }
				/* Get parameter info from database for ins nums in list */
				param_table = GetParameterInformation(insNumQueryId, queryTableName);
				
				/* Get header info from database for ins nums in list */
				header_table = GetHeaderInformation(insNumQueryId, queryTableName);
				if (insNumQueryId != 0){
					Query.clear(insNumQueryId);
				}

				abtran_table.select( param_table, "unit", "ins_num EQ $ins_num");
				abtran_table.select( header_table, "ticker, cusip, isin", "ins_num EQ $ins_num");
	
				/* Copy the ab tran information into the output table */
				what = "internal_bunit, deal_tracking_num, tran_num, ins_num, ins_type, trade_date, event_date, external_lentity, "+
				"internal_portfolio, unit, currency, position, ticker, cusip, isin, internal_contact";
				output.select( abtran_table, what, "deal_tracking_num GT 0");
	
				output.colHide( "internal_bunit");
				
				/* Sort the output table by deal tracking number */
				output.group( "deal_tracking_num");
	
				/* Create the report  */
				sReportTitle = "Amended Trades for " + bunit_aux; 
				filename = bunit_aux + ".Amended_Trades.tlt";
	
				if(m_INCStandard.report_viewer != 0) {
					if(report_type == 2) {
						output.colHide( "currency");
						output.colHide( "position");
					} else {
						output.colHide( "ticker");
						output.colHide( "cusip");
						output.colHide( "isin");
					}
					m_INCStandard.STD_PrintTextReport(output, filename, sReportTitle, "Amended Trade Listing " + period_string + "\nBusiness Unit: " + bunit_aux, error_log_file);
				}
	
				if ( param_table != null && Table.isTableValid( param_table) != 0) param_table.destroy();
				if (header_table != null && Table.isTableValid(header_table) != 0) header_table.destroy();
				if (ins_num_list != null && Table.isTableValid(ins_num_list) != 0) ins_num_list.destroy();
				if (abtran_table != null && Table.isTableValid(abtran_table) != 0) abtran_table.destroy();
	
				output.copyRowAddAll( tblTemp);
	
				output.clearRows();
			}   /* the Business Unit loop ends here */
			if (tranNumQueryId != 0){
				Query.clear(tranNumQueryId);
			}
		}
		tran_num_table.destroy();

		sReportTitle = "Amended Trade Listing " + period_string;

		tblTemp.formatSetJustifyRight( "event_date");
		tblTemp.formatSetJustifyRight( "trade_date");

		/*** View, m_INCStandard.Print or Save Crystal ***/
		if(m_INCStandard.STD_UseCrystalOutput() != 0) {
			tCrystal = tblTemp.copyTableFormatted( 0);

			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);
			tParam.addCol( "report_type", COL_TYPE_ENUM.COL_INT);
			tParam.setInt( "report_type", 1, report_type);

			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0)
			{
				exit_fail = 1;
				errorMessage = "\nError view crystal output.\n";
			}

			tParam.destroy();   
			tCrystal.destroy();  
		}

		FormatTempTable(tblTemp, report_type);

		/*** View Table ***/
		if(m_INCStandard.view_table != 0) {
			m_INCStandard.STD_ViewTable(tblTemp, sReportTitle, error_log_file); 
		}

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0) {
			m_INCStandard.STD_PrintTableDumpToFile(tblTemp, sFileName, sReportTitle, error_log_file);
		} 

		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0) {
			temp = Table.tableNew();
			temp.select( tblTemp, "*", "deal_tracking_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_Amended_Trades", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				exit_fail = 1;
				errorMessage = "\nError saving user table.\n";
			}

			temp.destroy();
		}

		/* purge the tables created for this report */
		if (output != null && Table.isTableValid(output) != 0) output.destroy();
		if (tblTemp != null && Table.isTableValid(tblTemp) != 0) tblTemp.destroy(); 
		if (party != null && Table.isTableValid(party) != 0) party.destroy();

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");

		if(exit_fail != 0)
		   throw new OException(errorMessage);
		return;
	}

	//================================================================================================//
	// Local Function Definitions                                                                     //
	//================================================================================================//

	private void LoadPartyTable(Table party) throws OException
	{
		Table temp = Table.tableNew();

		String sql = 
			"SELECT" + 
			"	party_id " + 
			"FROM " + 
			"	party " + 
			"WHERE " + 
			"	party_class = 1 " + 
			"AND " + 
			"	int_ext = 0";

		try
		{
			DBaseTable.execISql(temp, sql);
		}
		catch(OException oex)
		{
			temp.destroy();
			m_INCStandard.Print(error_log_file, "INFO", oex.getMessage());
			throw oex;
		}

		party.select( temp, "party_id(bunit)", "party_id GT 0");
		temp.destroy();
	}
	
	private Table CreateOutputTable() throws OException
	{
		Table output = Table.tableNew();
		
		output.addCol( "internal_bunit", 	 "Business\nUnit", 	SHM_USR_TABLES_ENUM.PARTY_TABLE);  
		output.addCol( "deal_tracking_num", 					COL_TYPE_ENUM.COL_INT);
		output.addCol( "tran_num", 								COL_TYPE_ENUM.COL_INT);   
		output.addCol( "ins_num", 								COL_TYPE_ENUM.COL_INT);   
		output.addCol( "ins_type", 			 "Instrument\nType",SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		output.addCol( "trade_date", 							COL_TYPE_ENUM.COL_INT);   
		output.addCol( "event_date", 							COL_TYPE_ENUM.COL_INT); 
		output.addCol( "external_lentity", 	 " \nCounterparty", SHM_USR_TABLES_ENUM.PARTY_TABLE);   
		output.addCol( "internal_portfolio", " \nPortfolio", 	SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		output.addCol( "unit", 				 " \nUnit", 		SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);  
		output.addCol( "currency", 			 " \nCcy" ,			SHM_USR_TABLES_ENUM.CURRENCY_TABLE);   
		output.addCol( "position", 								COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol( "ticker", 								COL_TYPE_ENUM.COL_STRING);
		output.addCol( "cusip", 								COL_TYPE_ENUM.COL_STRING);
		output.addCol( "isin", 									COL_TYPE_ENUM.COL_STRING);
		output.addCol( "internal_contact",	 " \nTrader", 		SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);

		/* set column formatting */
		output.setRowHeaderWidth( 1);
		output.setColFormatAsDate( "trade_date");
		output.setColFormatAsDate( "event_date");
		
		output.formatSetWidth( "internal_portfolio", 35);
		output.formatSetWidth( "internal_contact", 	 25);
		output.formatSetWidth( "internal_bunit", 	 35);
		output.formatSetWidth( "ticker", 			 25);
		output.formatSetWidth( "cusip", 			 25);
		output.formatSetWidth( "isin", 				 25);
		output.formatSetWidth( "personnel_id", 		 15);
		output.formatSetWidth( "external_lentity", 	 35);
		output.formatSetWidth( "trade_date", 		 12);
		output.formatSetWidth( "event_date", 		 12);
		output.formatSetWidth( "unit", 				 12);
		output.formatSetWidth( "ins_type", 			 20);

		output.showTitleBreaks();
		output.formatSetJustifyRight( "event_date");
		
		return output;
	}

	private void FormatOutputTable(Table output) throws OException
	{
		/* set output titles */
		output.setColTitle( "deal_tracking_num", "Deal\nNum");
		output.setColTitle( "tran_num",          "Tran\nNum");
		output.setColTitle( "trade_date",        "Trade\nDate");
		output.setColTitle( "event_date"  ,      "Amend\nDate");
		output.setColTitle( "position",          " \nPosition");
		output.setColTitle( "ticker",            " \nTicker");
		output.setColTitle( "cusip",             " \nCusip");
		output.setColTitle( "isin",              " \nISIN");

		output.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		
		/* hide unused columns */
		output.colHide( "ins_num");
	}

	private Table CreateTranNumTable(String str_Start, String str_End) throws OException
	{
		Table tran_num_table = Table.tableNew();
		
		tran_num_table.addCol( "tran_num", COL_TYPE_ENUM.COL_INT);
		tran_num_table.addCol( "event_date", COL_TYPE_ENUM.COL_INT);

		String sql = 
			"SELECT" + 
			"	tran_num, " + 
			"	event_date " + 
			"FROM " + 
			"	ab_tran_event " + 
			"WHERE " + 
			"	event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_AMENDED_OPEN.toInt() + " " +
			"AND " + 
			"	event_date >= '" + str_Start + "' " + 
			"AND " + 
			"	event_date < '" + str_End + "'";

		try
		{
			DBaseTable.execISql(tran_num_table, sql);
		}
		catch(OException oex)
		{
			tran_num_table.destroy();
			m_INCStandard.Print(error_log_file, "INFO", oex.getMessage());   	
			throw oex;
		}

		return tran_num_table;
	}
	
	private Table GetTransactionInformation(int tranNumQueryId, int bunit) throws OException
	{
		Table abtran_table = Table.tableNew();
		
		abtran_table.addCol( "internal_bunit", 		" \nBusiness Unit", SHM_USR_TABLES_ENUM.PARTY_TABLE);  
		abtran_table.addCol( "deal_tracking_num", 						COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "tran_num", 								COL_TYPE_ENUM.COL_INT);   
		abtran_table.addCol( "ins_num", 								COL_TYPE_ENUM.COL_INT);   
		abtran_table.addCol( "ins_type", 			" \nInstrument" , 	SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		abtran_table.addCol( "trade_date", 								COL_TYPE_ENUM.COL_INT);  
		abtran_table.addCol( "external_lentity", 	" \nCounterparty", 	SHM_USR_TABLES_ENUM.PARTY_TABLE); 
		abtran_table.addCol( "internal_portfolio", 	" \nPortfolio", 	SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		abtran_table.addCol( "currency", 			" \nCcy" ,			SHM_USR_TABLES_ENUM.CURRENCY_TABLE);   
		abtran_table.addCol( "position", 								COL_TYPE_ENUM.COL_DOUBLE);
		abtran_table.addCol( "internal_contact",	" \nTrader", 		SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
		abtran_table.addCol( "event_date", 								COL_TYPE_ENUM.COL_INT);
		String queryTableName;
        queryTableName = Query.getResultTableForId(tranNumQueryId);
        
        if ( queryTableName == null && tranNumQueryId > 0 )
        {
        	queryTableName = "query_result_plugin";
        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + tranNumQueryId 
        			+ " does not have a query result table. Default " + queryTableName + " table will be used.");
        }

		String sql = 
			"SELECT" + 
			"		internal_bunit" + 
			"		,deal_tracking_num" + 
			"		,tran_num" + 
			"		,ins_num" + 
			"		,ins_type" + 
			"		,trade_date" + 
			"		,external_lentity" + 
			"		,internal_portfolio" + 
			"		,currency" + 
			"		,position" + 
			"		,internal_contact " + 
			"FROM " + 
			"		ab_tran, " + queryTableName + " " +
			"WHERE " +
			"		tran_num = query_result " +
			"AND " + 
			"	unique_id = " + tranNumQueryId + " " +
			"AND " + 
			"		internal_bunit = " + bunit;

		try
		{
			DBaseTable.execISql(abtran_table, sql);
		}
		catch(OException oex)
		{
			abtran_table.destroy();
			m_INCStandard.Print(error_log_file,	"INFO",	"No Param Script Found - Running for today with all Business Units");
			throw oex;
		}

		return abtran_table;
	}
	
	private Table GetParameterInformation(int insNumQueryId, String queryTableName) throws OException
	{
		String what, from, where;
		Table param_table = Table.tableNew();

		what = "SELECT ins_num, unit "; 
		from = "FROM parameter, " + queryTableName + " ";
		where = "WHERE param_seq_num = 0 AND ins_num = query_result AND unique_id = " + insNumQueryId;
		
		try
		{
			DBaseTable.execISql(param_table, what + from + where);
		}
		catch(OException oex)
		{
			param_table.destroy();
			m_INCStandard.Print(error_log_file,	"ERROR", oex.getMessage());
			throw oex;
		}

		return param_table;
	}


	private Table GetHeaderInformation(int insNumQueryId, String queryTableName) throws OException
	{
		String what, from, where;
		Table header_table = Table.tableNew();

		what = "SELECT ins_num, ticker, cusip, isin "; 
		from = "FROM header, " + queryTableName + " ";
		where = "WHERE ins_num = query_result AND unique_id = " + insNumQueryId;
		
		try
		{
			DBaseTable.execISql(header_table, what + from + where);
		}
		catch(OException oex)
		{
			header_table.destroy();
			m_INCStandard.Print(error_log_file,	"ERROR", oex.getMessage());
			throw oex;
		}
		
		return header_table;
	}

	private void FormatTempTable(Table tblTemp, int report_type) throws OException
	{
		tblTemp.setColTitle( "deal_tracking_num", "Deal\nNum");   
		tblTemp.setColTitle( "tran_num",          "Tran\nNum");
		tblTemp.setColTitle( "trade_date",        "Trade\nDate");
		tblTemp.setColTitle( "event_date",        "Amend\nDate");
		tblTemp.setColTitle( "position",          " \nPosition");
		tblTemp.setColTitle( "ticker",            " \nTicker");
		tblTemp.setColTitle( "cusip",             " \nCusip");
		tblTemp.setColTitle( "isin",              " \nISIN");
		tblTemp.colShow( "internal_bunit");
		tblTemp.setColTitle( "internal_bunit", 	  "Business\nUnit");
		tblTemp.colHide( "ins_num");
		tblTemp.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		/* Order by Deal Num, Tran Num */
		tblTemp.addGroupBy( "internal_bunit");
		tblTemp.addGroupBy( "deal_tracking_num");
		tblTemp.addGroupBy( "tran_num");
		tblTemp.groupBy();
		
		if(report_type == 2)
		{
			tblTemp.colHide( "currency");
			tblTemp.colHide( "position");
		}else
		{
			tblTemp.colHide( "ticker");
			tblTemp.colHide( "cusip");
			tblTemp.colHide( "isin");
		}
	}

}