/*$Header: /cvs/master/olf/plugins/standard/report/STD_Deleted_Trade_Listing.java,v 1.11 2013/04/19 18:00:24 rdesposi Exp $*/
/*
File Name:                      STD_Deleted_Trade_Listing.java

Report Name:                    Deleted Trade Listing

Output File Name:               [Business Unit Name].Deleted_Trades.tlt    
                                STD_Deleted_Trade_Listing.rpt
                                STD_Deleted_Trade_Listing.csv
                                STD_Deleted_Trade_Listing.html
                                STD_Deleted_Trade_Listing.pdf
                                STD_Deleted_Trade_Listing.log
                                USER_Deleted_Trades

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

Revision Description:           Jan 23, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                Nov 10, 2010 - Broke up code into smaller functions
                                Jul 14, 2010 - Replaced Util.exitFail w/ throw OException
                                Jul 09, 2010 - Replaced loadFromDb* w/ execISql
                                             - Replaced OpenLink String lib w/ standard Java String functions
                                Mar 03, 2005 - Add Util.exitFail() check for output generation
                                Feb 22, 2005 - Configured script to run with INC_Standard
                                Nov 29, 2004 - Added RM code for Energy/Financial Report, m_INCStandard.save_local fix, Added new htmlViewer, retval check for crystal
                                Oct  4, 2004 - Added View/Save CRYSTAL_EXPORT_TYPES.HTML/CRYSTAL_EXPORT_TYPES.PDF Functionality, Workflow Flag
                                Sep 17, 2004 - Added Report Manager Functionality
                                Aug 19, 2004 - Changed Param script so date range can be specified

Main Script:                    This is a Main script
Parameter Script:               STD_Start_End_BUnit_Param.java (no parameter - runs for OCalendar.today() for all Business Units)
Display Script:                 None needed

Report Description:             
For the dates specifies, for each business unit in the parameter script (or all if no parameter script is specified), 
this report will report the trades that were Deleted.

Assumptions:                                         
None


Uses EOD Results?
False

Which EOD Results are used?
N/A

When can the script be run?  
Anytime

Recommended Script Category? 	N/A

Columns:
1.  Deal Num       :  The Deal Number of the deal. Taken from ab_tran.deal_tracking_num
2.  Tran Num       :  The Transaction Number of the deal. Taken from ab_tran.tran_num
3.  Instrument Type:  The Instrument Type of the deal. Taken from ab_tran.ins_type
4.  Trade Date     :  The trade date of the deal. Taken from ab_tran.trade_date
5.  Delete Date    :  The date and time of the last update of the deal. Taken from ab_tran.last_update
6.  Counterparty   :  The Counterparty of the deal. Taken from ab_tran.external_lentity
7.  Portfolio      :  The portfolio of the deal. Taken from ab_tran.internal_portfolio
8.  Unit           :  Taken from parameter.unit
9.  Ccy            :  Taken from ab_tran.currency  (only in ENERGY report)
10. Position       :  Taken from ab_tran.position  (only in ENERGY report)
11. Ticker         :  Taken from header.ticker     (only in FINANCIAL report)
12. Cusip          :  Taken from header.cusip      (only in FINANCIAL report) 
13. ISIN           :  Taken from header.isin       (only in FINANCIAL report)
14. Trader         :  Trader of the deal. Taken from ab_tran.internal_contact

 */

package standard.report;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class STD_Deleted_Trade_Listing implements IScript {

	private JVS_INC_Standard m_INCStandard;
	
	private Table allBunits;
	private String error_log_file;

	public STD_Deleted_Trade_Listing() {
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		int x, bunit, start_date, end_date, num_rows, exit_fail = 0;
		int report_type = m_INCStandard.STD_GetReportType();

		Table party = Util.NULL_TABLE;
		Table temp = Util.NULL_TABLE;
		Table tParam = Util.NULL_TABLE;

		String errorMessage = "";
		String reporttitle = "Deleted Trades Report";
		String sFileName = "STD_Deleted_Trade_Listing";

		error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);
		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		party = Table.tableNew("Party");

		/* Check to see that this Script was run with a Parameter Script */
		if(argt.getNumRows() == 0|| argt.getColNum( "bunits") <= 0)
		{
			m_INCStandard.Print(error_log_file, "INFO", "This script can take STD_Start_End_BUnit_Param.java");
			m_INCStandard.Print(error_log_file, "INFO", "No Param Script Found - Running for today with all Business Units");

			/* Set default date values */
			start_date = OCalendar.today();
			end_date = OCalendar.today();

			GetBunitIdInfo(party);
		}
		else
		{
			/* Load date values */
			start_date = argt.getInt( "start_date", 1);
			end_date   = argt.getInt( "end_date",   1);
			
			temp = argt.getTable( "bunits", 1).copyTable();
			party.select( temp, "return_val(bunit)", "return_val GT 0");
			temp.destroy();
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file, argt);   

		/* Create report table */
		CreateAllBunitsTable();

		/* Start the loop for each business unit */
		num_rows = party.getNumRows();
		for(x = 1; x <= num_rows; x++)
		{
			bunit = party.getInt( "bunit", x);
			AllBunitListing(bunit, reporttitle, start_date, end_date, report_type);
		}

		/* Format report table */
		FormatAllBunitsTable();

		if(start_date == end_date)
			reporttitle = "Deleted Trade Listing For " + OCalendar.formatDateInt(start_date);
		else 
			reporttitle = "Deleted Trade Listing From " + OCalendar.formatDateInt(start_date) + " To " + OCalendar.formatDateInt(end_date);

		/* Hide unwanted columns depending on the report type */
		HideColsOutputTable(allBunits, report_type);

		/*** View Table ***/
		if(m_INCStandard.view_table != 0) {
			m_INCStandard.STD_ViewTable(allBunits, reporttitle, error_log_file); 
		}

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0) {
			m_INCStandard.STD_PrintTableDumpToFile(allBunits, sFileName, reporttitle, error_log_file);
		}   

		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0)
		{
			temp = Table.tableNew();
			temp.select( allBunits, "*", "deal_tracking_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_Deleted_Trades", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				exit_fail = 1;
				errorMessage = "Error saving user table.";
			}
			temp.destroy();
		}

		/*** Generate/Save/m_INCStandard.Print Crystal Report Output ***/
		if(m_INCStandard.STD_UseCrystalOutput() != 0)
		{
			tParam = m_INCStandard.STD_CreateParameterTable(reporttitle, sFileName);
			tParam.addCol( "report_type",    COL_TYPE_ENUM.COL_INT);
			tParam.setInt("report_type", 1, report_type);

			TABLE_convertAllColsToStringWithException(allBunits, COL_TYPE_ENUM.COL_DOUBLE.toInt());
			// allBunits.printTableToTtx( "c:\\temp\\"+ sFileName + ".ttx");

			if(m_INCStandard.STD_OutputCrystal(allBunits, tParam, sFileName, sFileName, error_log_file) == 0)
			{
				exit_fail = 1;
				errorMessage = "Error with crystal output.";
			}

			tParam.destroy();
		}

		/* Purge the tables created for this rpeort */
		allBunits.destroy();
		party.destroy();

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");

		if(exit_fail != 0)
		   throw new OException(errorMessage);
		return;
	}

	//================================================================================================//
	// Local Function Definitions                                                                     //
	//================================================================================================//

	private void GetBunitIdInfo(Table party) throws OException
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
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());      	
			throw oex;
		}

		party.select( temp, "party_id(bunit)", "party_id GT 0");
		temp.destroy();
	}

	//------------------------------------------------------------------------------------------------//
	
	private void CreateAllBunitsTable() throws OException
	{
		allBunits = Table.tableNew();
	
		allBunits.addCol( "int_bunit", 			"Business\nUnit", 	SHM_USR_TABLES_ENUM.PARTY_TABLE);   
		allBunits.addCol( "deal_tracking_num", 	COL_TYPE_ENUM.COL_INT);
		allBunits.addCol( "tran_num", 			COL_TYPE_ENUM.COL_INT);   
		allBunits.addCol( "ins_num", 			COL_TYPE_ENUM.COL_INT);   
		allBunits.addCol( "ins_type", 			"Instrument\nType", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		allBunits.addCol( "trade_date", 		COL_TYPE_ENUM.COL_INT);  
		allBunits.addCol( "event_date", 		COL_TYPE_ENUM.COL_INT);  
		allBunits.addCol( "external_lentity", 	" \nCounterparty", 	SHM_USR_TABLES_ENUM.PARTY_TABLE); 
		allBunits.addCol( "int_portfolio", 		" \nPortfolio", 	SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		allBunits.addCol( "unit", 				" \nUnit", 			SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);   
		allBunits.addCol( "currency", 			" \nCcy",			SHM_USR_TABLES_ENUM.CURRENCY_TABLE);   
		allBunits.addCol( "position",			" \nPosition", 		Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());  
		allBunits.addCol( "ticker", 			COL_TYPE_ENUM.COL_STRING);
		allBunits.addCol( "cusip",  			COL_TYPE_ENUM.COL_STRING);
		allBunits.addCol( "isin",   			COL_TYPE_ENUM.COL_STRING);
		allBunits.addCol( "internal_contact",	" \nTrader", 		SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
	}

	//------------------------------------------------------------------------------------------------//

	/************************************************************************* 
	 * Name:        TABLE_convertAllColsToStringWithException()                             
	 * Description: Convert all columns in the given table in memory to type  
	 *              COL_TYPE_ENUM.COL_STRING except given exception columns format 
	 *              such as FORMAT_DATE columns
	 *                                                                       
	 * Author and Dates:   Donald Chan, 5/9/2003                        
	 * Revision:                                                              
	 **************************************************************************/
	private void TABLE_convertAllColsToStringWithException(Table table, int except_col_type) throws OException
	{
		int curr_col = 0;
		int total_cols = table.getNumCols();

		if(total_cols == 0)
		{
			m_INCStandard.Print(error_log_file, "ERROR","\n***OpenJvs Error: Invalid table was found when calling TABLE_convertAllColsToStringWithException()***");
			return;
		}

		table.formatSetJustifyRight("last_update");

		for(curr_col=1; curr_col<=total_cols; curr_col++)
		{
			if(table.getColFormat(curr_col) == COL_FORMAT_TYPE_ENUM.FMT_NONE.toInt())
				continue;

			if(table.getColType(curr_col) == except_col_type)
				continue;

			table.convertColToString( curr_col);
		}
	}

	//------------------------------------------------------------------------------------------------//
	
	private void AllBunitListing(int bunit, String reporttitle, int start_date, int end_date, int report_type) throws OException
	{
		Table abtran_table = Util.NULL_TABLE;
		Table param_table = Util.NULL_TABLE;
		Table output = Util.NULL_TABLE;
		Table ins_num_list = Util.NULL_TABLE;

		int queryId;
		
		String str_Start = OCalendar.formatJdForDbAccess(start_date);
		String str_End   = OCalendar.formatJdForDbAccess(end_date + 1);
		
		String date_string =     OCalendar.formatDateInt(start_date);
		String date_string_end = OCalendar.formatDateInt(end_date);  
		
		String period_string, bunit_aux, filename, tabletitle, what, queryTableName;

		if(start_date == end_date)
			period_string = "For " + date_string;
		else
			period_string = "From " + date_string  + " To " + date_string_end;

		/* Get transaction information */
		abtran_table = GetTransactionInfo(bunit, str_Start, str_End);
		
		output = CreateOutputTable();
		
		if (abtran_table.getNumRows() > 0)
		{
			/* Make a list of instrument numbers */
			ins_num_list = Table.tableNew();
			ins_num_list.addCol( "ins_num",COL_TYPE_ENUM.COL_INT);
			abtran_table.copyColDistinct( "ins_num", ins_num_list, "ins_num");
			queryId = Query.tableQueryInsert(ins_num_list, "ins_num", "query_result_plugin");
	        queryTableName = Query.getResultTableForId(queryId);
	        
	        if ( queryTableName == null && queryId > 0)
	        {
	        	queryTableName = "query_result_plugin";
	        	m_INCStandard.Print(error_log_file, "ERROR", "Query id " + queryId 
	        			+ " does not have a query result table. Default " + queryTableName + " table will be used.");
	        }
	        
			/* Get para info from db for ins nums in list */
			param_table = GetParameterInfo(queryId, queryTableName);
			
			/* Get header info from db for ins nums in list */
			PopulateAbtranTableWithHeaderInfo(queryId, queryTableName, abtran_table);
	
	
			/* Copy the ab tran information into the output table */
			what = "deal_tracking_num, tran_num, ins_num, ins_type, trade_date, " +
					"last_update(event_date), external_lentity, internal_portfolio(int_portfolio), " +
					"currency, position, ticker, cusip, isin, internal_contact";
			output.select( abtran_table, what, "deal_tracking_num GT 0");
	
			/* Copy the param info into the output table */
			output.select( param_table, "unit", "ins_num EQ $ins_num");	
		}
		
		/* sort the output table by deal tracking number */
		output.group( "deal_tracking_num");
		
		/* Create the report  */
		bunit_aux = Table.formatRefInt(bunit, SHM_USR_TABLES_ENUM.PARTY_TABLE);
		reporttitle = "Deleted Trades for " + bunit_aux + " " + period_string; 
		filename = bunit_aux + ".Deleted_Trades.tlt";

		HideColsOutputTable(output, report_type);
		
		/*** Create Report Viewer TXT File ***/
		if(m_INCStandard.report_viewer != 0) {
			tabletitle = "Deleted Trade Listing " + period_string + "\nBusiness Unit: " + bunit_aux;
			m_INCStandard.STD_PrintTextReport(output, filename, reporttitle, tabletitle, error_log_file);
		}

		output.setColValInt( "int_bunit", bunit);
		output.copyRowAddAll( allBunits );

		/* Destroy table pointers */
		if (output != Util.NULL_TABLE && Table.isTableValid(output) != 0) output.destroy();
		if (param_table != Util.NULL_TABLE && Table.isTableValid(param_table) != 0) param_table.destroy();
		if (abtran_table!= Util.NULL_TABLE && Table.isTableValid(abtran_table) != 0) abtran_table.destroy();
		if (ins_num_list!= Util.NULL_TABLE && Table.isTableValid(ins_num_list) != 0) ins_num_list.destroy();
	}

	//------------------------------------------------------------------------------------------------//

	private Table GetTransactionInfo(int bunit, String str_Start, String str_End) throws OException
	{
		Table abtran_table = Table.tableNew();
		
		abtran_table.addCol( "deal_tracking_num", 	COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "tran_num", 			COL_TYPE_ENUM.COL_INT);        
		abtran_table.addCol( "ins_num", 			COL_TYPE_ENUM.COL_INT);   
		abtran_table.addCol( "ins_type", 			COL_TYPE_ENUM.COL_INT);   
		abtran_table.addCol( "external_lentity", 	COL_TYPE_ENUM.COL_INT); 
		abtran_table.addCol( "trade_date", 			COL_TYPE_ENUM.COL_INT);  
		abtran_table.addCol( "currency", 			COL_TYPE_ENUM.COL_INT);   
		abtran_table.addCol( "internal_portfolio", 	COL_TYPE_ENUM.COL_INT); 
		abtran_table.addCol( "position", 			COL_TYPE_ENUM.COL_DOUBLE);  
		abtran_table.addCol( "internal_contact", 	COL_TYPE_ENUM.COL_INT);
		abtran_table.addCol( "last_update", 		COL_TYPE_ENUM.COL_INT); 

		String sql = 
			"SELECT" +
			"	deal_tracking_num" +
			"	,tran_num" +
			"	,ins_num" +
			"	,ins_type" +
			"	,external_lentity" +
			"	,trade_date" +
			"	,currency" +
			"	,internal_portfolio" +
			"	,position" +
			"	,internal_contact" +
			"	,last_update " +
			"FROM " + 
			"	ab_tran " + 
			"WHERE " + 
			"	internal_bunit = " + bunit + " " + 
			"AND " +
			"	last_update >= '" + str_Start + "' " + 
			"AND " +
			"	last_update < '" + str_End + "' " + 
			"AND " +
			"	tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt();

		try
		{
			DBaseTable.execISql(abtran_table, sql);
		}
		catch(OException oex)
		{
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());      	
			throw oex;
		}

		return abtran_table;
	}
	
	//------------------------------------------------------------------------------------------------//

	private Table GetParameterInfo(int queryId, String queryTableName) throws OException
	{
		Table param_table = Table.tableNew();
	
		String sql = 
			"SELECT" +
			"	ins_num" +
			"	,unit " +
			"FROM " +
			"	parameter" +
			"	," + queryTableName + " " +
			"WHERE " +
			"	ins_num = query_result " +
			"AND " +
			"	unique_id = " + queryId + " " + 
			"AND " +
			"	param_seq_num = 0";

		try
		{
			DBaseTable.execISql(param_table, sql);
		}
		catch(OException oex)
		{
			Query.clear(queryId);
			m_INCStandard.Print(error_log_file, "ERROR", oex.getMessage());      	
			throw oex;
		}

		return param_table;
	}
	
	//------------------------------------------------------------------------------------------------//

	private void PopulateAbtranTableWithHeaderInfo(int queryId, String queryTableName, Table abtran_table) throws OException
	{
		Table header_table = Table.tableNew();
		
		String sql = 
			"SELECT" +
			"	ins_num" +
			"	,ticker" +
			"	,cusip" +
			"	,isin " +
			"FROM " +
			"	header" + 
			"	," + queryTableName + " " + 
			"WHERE " + 
			"	ins_num = query_result " + 
			"AND " + 
			"	unique_id = " + queryId;

		try
		{
			DBaseTable.execISql(header_table, sql);
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
		
		abtran_table.select( header_table, "ticker, cusip, isin", "ins_num EQ $ins_num");
		header_table.destroy();
	}
	
	//------------------------------------------------------------------------------------------------//

	private Table CreateOutputTable() throws OException
	{
		Table output = Table.tableNew();
		
		output.addCol( "int_bunit", 		"Business\nUnit", 	SHM_USR_TABLES_ENUM.PARTY_TABLE);   
		output.addCol( "deal_tracking_num", COL_TYPE_ENUM.COL_INT);
		output.addCol( "tran_num", 			COL_TYPE_ENUM.COL_INT);
		output.addCol( "ins_num", 			COL_TYPE_ENUM.COL_INT);   
		output.addCol( "ins_type", 			"Instrument\nType", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);   
		output.addCol( "trade_date", 		COL_TYPE_ENUM.COL_INT);  
		output.addCol( "event_date", 		COL_TYPE_ENUM.COL_INT); 
		output.addCol( "external_lentity", 	" \nCounterparty", 	SHM_USR_TABLES_ENUM.PARTY_TABLE); 
		output.addCol( "int_portfolio", 	" \nPortfolio", 	SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);   
		output.addCol( "unit", 				" \nUnit", 			SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);   
		output.addCol( "currency", 			" \nCcy", 			SHM_USR_TABLES_ENUM.CURRENCY_TABLE);   
		output.addCol( "position",			" \nPosition", 		Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());  
		output.addCol( "ticker", 			COL_TYPE_ENUM.COL_STRING);   
		output.addCol( "cusip", 			COL_TYPE_ENUM.COL_STRING);   
		output.addCol( "isin", 				COL_TYPE_ENUM.COL_STRING);   
		output.addCol( "internal_contact", 	" \nTrader", 		SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);

		/* Set column formatting */
		output.setRowHeaderWidth( 1);
		output.setColFormatAsDate( "trade_date");
		output.setColFormatAsDate( "event_date");

		output.formatSetWidth( "internal_contact", 20);
		output.formatSetWidth( "personnel_id",     20);
		output.formatSetWidth( "event_date",       12);
		output.formatSetWidth( "int_portfolio",    25);
		output.formatSetWidth( "external_lentity", 25);
		output.formatSetWidth( "trade_date",       12);
		output.formatSetWidth( "unit",             12);
		output.formatSetWidth( "ins_type",         20);
		output.showTitleBreaks();

		/* Set output titles */
		output.setColTitle( "deal_tracking_num", "Deal\nNum");
		output.setColTitle( "tran_num",          "Tran\nNum");
		output.setColTitle( "trade_date",        "Trade\nDate");
		output.setColTitle( "event_date" ,      "Delete\nDate");
		output.setColTitle( "ticker",            " \nTicker");
		output.setColTitle( "cusip",             " \nCusip");
		output.setColTitle( "isin",              " \nISIN");

		output.formatSetJustifyRight( "trade_date");
		output.formatSetJustifyRight( "event_date");

		/* Hide unused columns */
		output.colHide( "ins_num");
		output.colHide( "int_bunit");

		return output;
	}
	
	//------------------------------------------------------------------------------------------------//

	private void FormatAllBunitsTable() throws OException
	{
		/* Set column formatting */
		allBunits.setRowHeaderWidth( 1);
		allBunits.setColFormatAsDate( "trade_date");
		allBunits.setColFormatAsDate( "event_date");

		allBunits.formatSetWidth( "internal_contact", 25);
		allBunits.formatSetWidth( "personnel_id",     15);
		allBunits.formatSetWidth( "event_date",       12);
		allBunits.formatSetWidth( "int_portfolio",    25);
		allBunits.formatSetWidth( "external_lentity", 25);
		allBunits.formatSetWidth( "trade_date",       12);
		allBunits.formatSetWidth( "unit",             12);
		allBunits.formatSetWidth( "ins_type",         15);
		allBunits.formatSetWidth( "ticker",           15);
		allBunits.formatSetWidth( "cusip",            15);
		allBunits.formatSetWidth( "isin",             15);

		allBunits.setColTitle( "int_bunit",          "Business\nUnit");
		allBunits.setColTitle( "deal_tracking_num",  "Deal\nNum");
		allBunits.setColTitle( "tran_num",           "Tran\nNum");
		allBunits.setColTitle( "trade_date",         "Trade\nDate");
		allBunits.setColTitle( "event_date",         "Delete\nDate");
		allBunits.setColTitle( "ticker",             " \nTicker");
		allBunits.setColTitle( "cusip",              " \nCusip");
		allBunits.setColTitle( "isin",               " \nISIN");

		allBunits.showTitleBreaks();

		/* Order by Deal Num, Tran Num */
		allBunits.addGroupBy( "int_bunit");
		allBunits.addGroupBy( "deal_tracking_num");
		allBunits.addGroupBy( "tran_num");
		allBunits.groupBy();

		/* Hide unused columns */
		allBunits.delCol( "ins_num");
	}

	//------------------------------------------------------------------------------------------------//

	private void HideColsOutputTable(Table table, int report_type) throws OException
	{
		if(report_type == 1)
		{
			table.colHide( "ticker");
			table.colHide( "cusip");
			table.colHide( "isin");
		}
		if(report_type == 2)
		{
			table.colHide( "currency");
			table.colHide( "position");
		}
	}
}