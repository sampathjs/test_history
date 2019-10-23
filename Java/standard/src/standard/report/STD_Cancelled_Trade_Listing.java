/*$Header: /cvs/master/olf/plugins/standard/report/STD_Cancelled_Trade_Listing.java,v 1.10 2013/01/23 16:01:47 dzhu Exp $*/

/*
File Name:                      STD_Cancelled_Trade_Listing.java

Report Name:                    Cancelled Trade Listing 

Output File Name:               [Business Unit Name].STD_Cancelled_Trade_Listing.tlt
                                STD_Cancelled_Trade_Listing.rpt
                                STD_Cancelled_Trade_Listing.csv
                                STD_Cancelled_Trade_Listing.html
                                STD_Cancelled_Trade_Listing.pdf
                                STD_Cancelled_Trade_Listing.log
                                USER_Cancelled_Trades

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

Revision History:               Jan 23, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                Nov 09, 2011 - Replaced use of query_result table with query_result_plugin
                                Oct 27, 2010 - Broke up code into smaller functions. Removed compiler warnings.
                                Jul 01, 2010 - Replaced loadFromDbWithWhatWhere with DBaseTable.execISql
                                             - Replaced OpenLink String library with Java String functions
                                Mar 03, 2005 - Add Util.exitFail() check for output generation
                                Feb 22, 2005 - Configured Script to run with INC_Standard
                                Nov 29, 2004 - Added RM code for Energy/Financial Report, m_INCStandard.save_local fix, new htmlViewer code, retval crystal check, 
                                             - use ab_tran_event.event_date instead of ab_tran.last_updated
                                Oct  4, 2004 - Added View/Save CRYSTAL_EXPORT_TYPES.HTML/CRYSTAL_EXPORT_TYPES.PDF Functionality, Workflow Flag
                                Sep 17, 2004 - Added Report Manager Functionality
                                Aug 20, 2004 - Changed param script to STD_Start_End_Date_BUnit_Param
                                Mar 29, 2004 - Formatted Last Update field.


Parameter Script:               STD_Start_End_BUnit_Param.java (no parameter then OCalendar.today() for all Business Units)
Display Script:                 None
Recommended Script Category? 	N/A

Report Description:             
For the dates specifies, for each business unit in the parameter script (or all if no parameter script is specified), 
this report will report the trades that were Cancelled.

Columns:
  Deal Num               ab_tran.deal_tracking_num
  Tran Num               ab_tran_event.tran_num
  Instrument Type        ab_tran.ins_type
  Trade Date             ab_tran.trade_date
  Cancel Date            ab_tran_event.event_date
  Counterparty           ab_tran.external_lentity
  Portfolio              ab_tran.internal_portfolio
  Unit                   parameter.unit
  Ccy                    ab_tran.currency (only in ENERGY report) 
  Position               ab_tran.position (only in ENERGY report)
  Ticker                 header.ticker (only in FINANCIAL report)
  Cusip                  header.cusip  (only in FINANCIAL report)
  ISIN                   header.isin   (only in FINANCIAL report)
  Trader                 ab_tran.internal_contact
 */

package standard.report;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class STD_Cancelled_Trade_Listing implements IScript {

	private JVS_INC_Standard m_INCStandard;

	private String error_log_file;

	public STD_Cancelled_Trade_Listing() {
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		
		Table party = Util.NULL_TABLE;
		Table temp  = Util.NULL_TABLE;
		Table allBunits = Util.NULL_TABLE;
		Table tran_num_table = Util.NULL_TABLE;
		Table tParam, tCrystal = Util.NULL_TABLE;

		String sReportTitle = "Cancelled Trade Listing";
		String sFileName  = "STD_Cancelled_Trade_Listing";
		String errorMessage = "";

		int x, bunit, start_date, end_date = 0;
		int num_rows, exit_fail = 0;
		int report_type = m_INCStandard.STD_GetReportType();

		error_log_file = Util.errorInitScriptErrorLog(sFileName);

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		/* Check to see that this Script was run with a Parameter Script */
		if (argt.getNumRows() == 0 || argt.getColNum( "bunits") <= 0)
		{
			m_INCStandard.Print(error_log_file, "INFO", "This script can take STD_Start_End_BUnit_Param.java");
			m_INCStandard.Print(error_log_file, "INFO", "No Param Script Found - Running for today with all Business Units");

			/* Set Default Values for dates */
			start_date = OCalendar.today();
			end_date = OCalendar.today();

			party = GetBunitInformation();
		}
		else
		{
			/* Get dates values */
			start_date = argt.getInt("start_date", 1);
			end_date   = argt.getInt("end_date", 1);

			/* Get Bunit Information */
			party = Table.tableNew("Party");
			temp = argt.getTable( "bunits", 1).copyTable();
			party.select( temp, "return_val(bunit)", "return_val GT 0");
			temp.destroy();
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file,argt);   

		allBunits = CreateAllBunitsTable();
		tran_num_table = CreateTranNumTable(start_date, end_date);

		/* Start the loop for each Business Unit */
		num_rows = party.getNumRows();
		
		for(x = 1; x <= num_rows; x++)
		{
			bunit = party.getInt( "bunit", x);
			AllBunitListing(bunit, allBunits, start_date, end_date, report_type, tran_num_table);
		}

		/* Set column formatting */
		FormatAllBunitsTable(allBunits, report_type);

		if(start_date == end_date)
			sReportTitle = "Cancelled Trade Listing For " + OCalendar.formatDateInt(start_date);
		else 
			sReportTitle = "Cancelled Trade Listing From " + OCalendar.formatDateInt(start_date) + " To " + OCalendar.formatDateInt(end_date);

		/*** View Table ***/
		if(m_INCStandard.view_table != 0)
		{
			m_INCStandard.STD_ViewTable(allBunits, sReportTitle, error_log_file); 
		}

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0)
		{
			m_INCStandard.STD_PrintTableDumpToFile(allBunits, sFileName, sReportTitle, error_log_file);
		}  

		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0)
		{
			temp = Table.tableNew();
			temp.select( allBunits, "*", "deal_tracking_num GT -1");

			if(m_INCStandard.STD_SaveUserTable(temp, "USER_Cancelled_Trades", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				exit_fail = 1;  
				errorMessage = "Error saving user table: USER_Cancelled_Trades";
			}

			temp.destroy();
		}

		/*** View, m_INCStandard.Print or Save Crystal ***/
		if(m_INCStandard.STD_UseCrystalOutput() != 0)
		{
			// tCrystal.printTableToTtx( "c:\\temp\\"+ sFileName + ".ttx");

			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);
			tParam.addCol( "report_type", COL_TYPE_ENUM.COL_INT);
			tParam.setInt( "report_type", 1, report_type);

			allBunits.setColFormatNone( "position");
			allBunits.formatSetJustifyRight( "event_date");
			allBunits.formatSetJustifyRight( "trade_date");

			tCrystal = allBunits.copyTableFormatted( 0);
			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0)
			{
				exit_fail = 1;
				errorMessage = "Error viewing crystal output";
			}

			tParam.destroy();
			tCrystal.destroy();
		}

		if (allBunits != null && Table.isTableValid(allBunits) != 0) allBunits.destroy();
		if (party != null && Table.isTableValid(party) != 0) party.destroy();
		if (tran_num_table != null && Table.isTableValid(tran_num_table) != 0) tran_num_table.destroy();

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");

		if(exit_fail != 0)
		   throw new OException(errorMessage);
		return;
	}

	//================================================================================================//
	// Local Function Definitions                                                                     //
	//================================================================================================//

	private Table GetBunitInformation() throws OException
	{
		Table temp = Table.tableNew();
		Table party = Table.tableNew("Party");

		String sql = "SELECT party_id FROM party WHERE party_class = 1 AND int_ext = 0";

		try
		{
			DBaseTable.execISql(temp, sql);
		}
		catch(OException oex)
		{
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());
			throw oex;
		}

		party.select(temp, "party_id(bunit)", "party_id GT 0");
		temp.destroy();
		
		return party;
	}
	
	private Table CreateAllBunitsTable() throws OException
	{
		Table allBunits = Table.tableNew();
	
		allBunits.addCol( "int_bunit", 			"Business\nUnit", 	SHM_USR_TABLES_ENUM.PARTY_TABLE);   
		allBunits.addCol( "deal_tracking_num", 			COL_TYPE_ENUM.COL_INT);
		allBunits.addCol( "tran_num", 					COL_TYPE_ENUM.COL_INT);    
		allBunits.addCol( "ins_num", 					COL_TYPE_ENUM.COL_INT);   
		allBunits.addCol( "ins_type", 			"Ins\nType" , 		SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		allBunits.addCol( "trade_date", 				COL_TYPE_ENUM.COL_INT); 
		allBunits.addCol( "event_date", 				COL_TYPE_ENUM.COL_INT); 
		allBunits.addCol( "external_lentity", 	" \nCounterparty", 	SHM_USR_TABLES_ENUM.PARTY_TABLE); 
		allBunits.addCol( "internal_portfolio", " \nPortfolio", 	SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		allBunits.addCol( "unit", 				" \nUnit", 			SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);   
		allBunits.addCol( "currency", 			" \nCurrency",		SHM_USR_TABLES_ENUM.CURRENCY_TABLE);   
		allBunits.addCol( "position",			" \nPosition", 		Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());  
		allBunits.addCol( "ticker", 					COL_TYPE_ENUM.COL_STRING); 
		allBunits.addCol( "cusip", 						COL_TYPE_ENUM.COL_STRING); 
		allBunits.addCol( "isin", 						COL_TYPE_ENUM.COL_STRING);
		allBunits.addCol( "internal_contact",	" \nTrader", 		SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
		
		return allBunits;
	}

	private void FormatAllBunitsTable(Table allBunits, int report_type) throws OException
	{
		allBunits.setRowHeaderWidth( 1);
		allBunits.setColFormatAsDate( "trade_date");
		allBunits.setColFormatAsDate( "event_date");
	
		allBunits.formatSetWidth( "internal_portfolio", 25);
		allBunits.formatSetWidth( "external_lentity", 	25);
		allBunits.formatSetWidth( "trade_date", 		12);
		allBunits.formatSetWidth( "event_date", 		12);
		allBunits.formatSetWidth( "unit", 				12);
		allBunits.formatSetWidth( "ins_type", 			20);
		allBunits.formatSetWidth( "ticker", 			20);
		allBunits.formatSetWidth( "cusip", 				20);
		allBunits.formatSetWidth( "isin", 				20);
		allBunits.formatSetWidth( "internal_contact", 	15);
		allBunits.formatSetWidth( "int_bunit", 			25);
	
		allBunits.setColTitle( "int_bunit", 		"Business\nUnit");
		allBunits.setColTitle( "deal_tracking_num", "Deal\nNum");
		allBunits.setColTitle( "tran_num", 			"Tran\nNum");
		allBunits.setColTitle( "trade_date", 		"Trade\nDate");
		allBunits.setColTitle( "event_date", 		"Cancel\nDate");
		allBunits.setColTitle( "ticker", 			" \nTicker");
		allBunits.setColTitle( "cusip", 			" \nCusip");
		allBunits.setColTitle( "isin", 				" \nISIN");
	
		allBunits.showTitleBreaks();
	
		/* Order by Deal Num, Tran Num */
		allBunits.addGroupBy( "int_bunit");
		allBunits.addGroupBy( "deal_tracking_num");
		allBunits.addGroupBy( "tran_num");
		allBunits.groupBy();
	
		/* Hide unwanted columns */
		allBunits.delCol( "ins_num");
	
		if(report_type == 1){
			allBunits.colHide( "ticker");
			allBunits.colHide( "cusip");
			allBunits.colHide( "isin");
		}
		if(report_type == 2){
			allBunits.colHide( "currency");
			allBunits.colHide( "position");
		}
	}

	private Table CreateTranNumTable(int start_date, int end_date) throws OException
	{
		Table tran_num_table = Table.tableNew();
		tran_num_table.addCol( "tran_num", COL_TYPE_ENUM.COL_INT);
		tran_num_table.addCol( "event_date", COL_TYPE_ENUM.COL_INT);

		String sql =
			"SELECT" + 
			"		tran_num" + 
			"		,event_date " + 
			"FROM " + 
			"		ab_tran_event " + 
			"WHERE " + 
			"		event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CANCELLED.toInt() + " " + 
			"AND " + 
			"		event_date >= '" + OCalendar.formatJdForDbAccess(start_date) + "' " + 
			"AND " + 
			"		event_date < '" + OCalendar.formatJdForDbAccess(end_date + 1) + "' ";

		try
		{
			DBaseTable.execISql(tran_num_table, sql);
		}
		catch(OException oex)
		{
			m_INCStandard.Print(error_log_file,	"ERROR", oex.getMessage());
			throw oex;
		}
	
		return tran_num_table;
	}

	private void AllBunitListing(int bunit, Table allBunits, int start_date, int end_date, 
			int report_type, Table tran_num_table) throws OException
	{
		Table abtran_table = Util.NULL_TABLE;
		Table output       = Util.NULL_TABLE;
	
		String date_string     = OCalendar.formatDateInt(start_date);
		String date_string_end = OCalendar.formatDateInt(end_date);   
		String period_string;
		String bunit_aux;
		String filename;
		String tabletitle;
		String report_title;
	
		int queryId = 0;
	
		if(start_date == end_date)
			period_string = "For " + date_string;
		else
			period_string = "From " + date_string  + " To " + date_string_end;
	
		if(tran_num_table.getNumRows() > 0)
			queryId = Query.tableQueryInsert(tran_num_table, "tran_num","query_result_plugin");
		if ( queryId > 0 ){
			/* Get transaction information */
			abtran_table = GetTransactionInformation(queryId, bunit); 
		
			/* Add Event Date to table */
			abtran_table.select( tran_num_table, "event_date", "tran_num EQ $tran_num");
			if (abtran_table.getNumRows() > 0)
			{
				/* Make a list of instrument numbers */
				queryId = Query.tableQueryInsert(abtran_table, "ins_num","query_result_plugin");
				
				/* Get parameter info from database for instrument numbers in list */
				GetParameterInformation(abtran_table, queryId);
			
				/* Get header info from database for instrument numbers in list */
				GetHeaderInformation(abtran_table);
			}
		}
		
		/* Create the report title */
		bunit_aux = Table.formatRefInt(bunit, SHM_USR_TABLES_ENUM.PARTY_TABLE);
		report_title = "Cancelled Trades for " + bunit_aux; 
		filename = bunit_aux + ".Cancelled_Trades.tlt";
		
		output = CreateOutputTable(abtran_table, report_type);
	
		/*** Create the report ***/
		if(m_INCStandard.report_viewer != 0)
		{
			tabletitle = "Cancelled Trade Listing " + period_string + "\nBusiness Unit: " + bunit_aux;       
			m_INCStandard.STD_PrintTextReport(output, filename, report_title, tabletitle, error_log_file);
		}
	
		if (abtran_table != null && Table.isTableValid(abtran_table) != 0) abtran_table.destroy();
	
		output.setColValInt( "int_bunit", bunit);
		output.copyRowAddAll( allBunits );
		output.destroy();
	}

	private Table GetTransactionInformation(int queryId, int bunit) throws OException
	{
		Table abtran_table = Table.tableNew();
		
		String queryTableName = Query.getResultTableForId(queryId);
        if ( queryTableName == null && queryId > 0 )
        {
        	queryTableName = "query_result_plugin";
        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + queryId 
        			+ " does not have a query result table. Default " + queryTableName + " table will be used.");
        }
        
		abtran_table.addCol( "deal_tracking_num", 			COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "tran_num", 					COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "ins_num", 					COL_TYPE_ENUM.COL_INT);   
		abtran_table.addCol( "ins_type", 			"Ins\nType" , 		SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		abtran_table.addCol( "external_lentity", 	" \nCounterparty", 	SHM_USR_TABLES_ENUM.PARTY_TABLE); 
		abtran_table.addCol( "trade_date", 					COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "currency", 			" \nCurrency" ,		SHM_USR_TABLES_ENUM.CURRENCY_TABLE);   
		abtran_table.addCol( "internal_portfolio", 	" \nPortfolio", 	SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		abtran_table.addCol( "position",			" \nPosition", 		Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());  
		abtran_table.addCol( "internal_contact",	" \nTrader", 		SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);

		String sql = 
			"SELECT" + 
			"		deal_tracking_num" + 
			"		,tran_num" + 
			"		,ins_num" + 
			"		,ins_type" + 
			"		,external_lentity" + 
			"		,trade_date" + 
			"		,currency" + 
			"		,internal_portfolio" + 
			"		,position" + 
			"		,internal_contact " + 
			"FROM " + 
			"		ab_tran, " + queryTableName +  " " + 
			"WHERE " + 
			"		internal_bunit = " + bunit + " " +
			"AND " + 
			"		tran_num = query_result " + 
			"AND " + 
			"		unique_id = " + queryId;

		try
		{
			DBaseTable.execISql(abtran_table, sql);
		}
		catch(OException oex)
		{
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());
			throw oex;
		}
		finally
		{
			Query.clear(queryId);
		}

		return abtran_table;
	}
	
	private void GetParameterInformation(Table abtran_table, int queryId) throws OException
	{
		Table param_table = Table.tableNew();
		String queryTableName = Query.getResultTableForId(queryId); 
        if ( queryTableName == null && queryId > 0 )
        {
        	queryTableName = "query_result_plugin";
        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + queryId 
        			+ " does not have a query result table. Default " + queryTableName + " table will be used.");
        }
        
		String sql = 
			"SELECT" + 
			"		ins_num" + 
			"		,unit " + 
			"FROM " + 
			"		parameter, " + queryTableName + " " + 
			"WHERE " + 
			"		ins_num = query_result " + 
			"AND " + 
			"		unique_id = " + queryId + 
			"AND " + 
			"		param_seq_num = 0";

		try
		{
			DBaseTable.execISql(param_table, sql);
		}
		catch(OException oex)
		{
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());
			throw oex;
		}	
		finally
		{
			Query.clear(queryId);
		}

		abtran_table.select( param_table, "unit", "ins_num EQ $ins_num");
		param_table.destroy();
	}
	
	private void GetHeaderInformation(Table abtran_table) throws OException
	{
		Table header_table = Table.tableNew();
		String sql = 
			"SELECT" +
			"	ins_num" +
			"	,ticker" +
			"	,cusip" +
			"	,isin " +
			"FROM " +
			"	header";

		try
		{
			DBaseTable.execISql(header_table, sql);
		}
		catch(OException oex)
		{
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());
			throw oex;
		}

		abtran_table.select( header_table, "ticker, cusip, isin", "ins_num EQ $ins_num");
		header_table.destroy();
	}
	
	private Table CreateOutputTable(Table abtran_table, int report_type) throws OException
	{
		Table output = Table.tableNew();
		
		output.addCol( "int_bunit", 		 "Business\nUnit", 		SHM_USR_TABLES_ENUM.PARTY_TABLE);   
		output.addCol( "deal_tracking_num", 		COL_TYPE_ENUM.COL_INT);
		output.addCol( "tran_num", 					COL_TYPE_ENUM.COL_INT);
		output.addCol( "ins_num", 					COL_TYPE_ENUM.COL_INT);   
		output.addCol( "ins_type", 			 "Instrument\nType", 	SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		output.addCol( "trade_date", 				COL_TYPE_ENUM.COL_INT); 
		output.addCol( "event_date", 				COL_TYPE_ENUM.COL_INT);  
		output.addCol( "external_lentity", 	 " \nCounterparty", 	SHM_USR_TABLES_ENUM.PARTY_TABLE); 
		output.addCol( "internal_portfolio", " \nPortfolio", 		SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		output.addCol( "unit", " \nUnit", 							SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);   
		output.addCol( "currency", 			 " \nCcy",				SHM_USR_TABLES_ENUM.CURRENCY_TABLE);      
		output.addCol( "position",			 " \nPosition", 		Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());  
		output.addCol( "ticker", 					COL_TYPE_ENUM.COL_STRING);
		output.addCol( "cusip",  					COL_TYPE_ENUM.COL_STRING);
		output.addCol( "isin",   					COL_TYPE_ENUM.COL_STRING);
		output.addCol( "internal_contact",	 " \nTrader", 			SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
	
		/* Set column formatting */
		output.setRowHeaderWidth( 1);
		
		output.setColFormatAsDate( "trade_date");
		output.setColFormatAsDate( "event_date");
		
		output.formatSetWidth( "internal_portfolio", 	25);
		output.formatSetWidth( "external_lentity", 		25);
		output.formatSetWidth( "trade_date", 			12);
		output.formatSetWidth( "unit", 					12);
		output.formatSetWidth( "ins_type", 				20);
		output.formatSetWidth( "internal_contact", 		25);
		output.formatSetWidth( "personnel_id", 			25);
		output.formatSetWidth( "event_date", 			12);
		output.showTitleBreaks();
	
		/* Set output titles */
		output.setColTitle( "deal_tracking_num", 	"Deal\nNum");
		output.setColTitle( "tran_num", 			"Tran\nNum");
		output.setColTitle( "trade_date", 			"Trade\nDate");
		output.setColTitle( "event_date",			"Cancel\nDate");
	
		/* Hide unwanted columns */
		output.colHide( "ins_num");
		output.colHide( "int_bunit");
		if (abtran_table != null && Table.isTableValid(abtran_table) == 1 && abtran_table.getNumRows() > 0 )
		{
			/* Copy the transaction information into the output table */
			String what = "deal_tracking_num, tran_num, ins_num, ins_type, " +
					"trade_date, event_date, external_lentity, internal_portfolio, " +
					"unit, currency, position, ticker, cusip, isin, internal_contact";
			
			output.select( abtran_table, what, "deal_tracking_num GT 0");
		}
		/* Sort the output table by deal tracking number */
		output.group( "deal_tracking_num");
		
		if(report_type == 1){
			output.colHide( "ticker");
			output.colHide( "cusip");
			output.colHide( "isin");
		}
		if(report_type == 2){
			output.colHide( "currency");
			output.colHide( "position");
		}
	
		return output;
	}

}
