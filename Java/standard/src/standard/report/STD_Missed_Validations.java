/*$Header: /cvs/master/olf/plugins/standard/report/STD_Missed_Validations.java,v 1.13 2013/04/19 18:00:25 rdesposi Exp $*/
/*
File Name:                      STD_Missed_Validations.java

Report Name:                    Missed Validations Report For (Date)

OutPut File Name:               STD_Missed_Validations.exc
                                STD_Missed_Validations.rpt
                                STD_Missed_Validations.csv
                                STD_Missed_Validations.html
                                STD_Missed_Validations.pdf
                                STD_Missed_Validations.log
                                USER_Missed_Validations

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
                                Aug 17, 2011 - Added passing output table of missed validations results to workflow
								Nov 09, 2010 - Broke up code into smaller functons
                                Sep 28, 2010 - Wrapped function calls to DBaseTable.execISql with a try-catch block
                                Sep 02, 2010 - Replaced function calls to DBaseTable.loadFromDb* with calls to DBaseTable.execISql
                                             - Replaced function calls to the OpenJVS String library with calls to the Java String library
                                             - Replaced function calls to Util.exitFail with throwing an OException
                                Feb 02, 2010 - In Altering strWhere clause  replaced loop with TABLE_QueryInsertN() function.
                                Feb 16, 2007 - Replaced several database queries with one query
                                Mar 03, 2005 - Add Util.exitFail() check for output generation
                                Feb 24, 2005 - Configured script to run with INC_Standard
                                Dec 16, 2004 - Added retval crystal check, pdf fix, new htmlViewer code, bridged the OpenJvs to VB gap, user table fix
                                Oct 11, 2004 - ASP Output functionality, CRYSTAL_EXPORT_TYPES.HTML and CSV output capability added.

Main Script:                    This
Parameter Script:               STD_Business_Unit_Param.java (optional)
                                If no parameter is specified then this script will run for all Business Units
Display Script:                 None

Report Description:             
This report will search the database for all trades with the NEW trans statuses (i.e. Cancelled New, Buyout New ...)

This report will exit with a status of FAILED if it finds any 
deals that are at one or more of the above statuses.
This report will exit with a status of SUCCEEDED if it does NOT
find any deals at the above statuses.

Assumption:                 None

Instruction:
Set to_workflow to 1 to pass output table of missed validations to workflow (default)
Set to_workflow to 1 NOT to pass output table of missed validations to workflow

Use EOD Results?            0

EOD Results that are used:

When can the script be run?

Recommended Script Category? 	N/A

Columns:
   Deal Num               ab_tran.deal_tracking_num
   Tran Num               ab_tran.tran_num
   Tran Status            ab_tran.tran_status
   Instrument Type        ab_tran.ins_type
   Trade Date             ab_tran.trade_date
   Counterparty           ab_tran.external_lentity
   Portfolio              ab_tran.internal_portfolio
   Reference              ab_tran.reference
   Unit                   ins_parameter.unit     (Energy Report Only)
   Ticker                 header.ticker      (Financial Report Only)
   Cusip                  header.cusip       (Financial Report Only)
   Ccy                    ab_tran.currency
   Position               ab_tran.position   (Energy Report Only)
   Trader                 ab_tran.internal_contact
   Start Date             ins_parameter.start_date
   End Date               ins_parameter.mat_date

Group by Business Unit
Sort by Deal Num, Tran Num

 */

package standard.report;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class STD_Missed_Validations implements IScript {
	
	private JVS_INC_Standard m_INCStandard;
	
	private String error_log_file;

	public STD_Missed_Validations() {
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		
		/******************** USER CONFIGURABLE PARAMETERS *********************/
		//int to_workflow = 0;    // Output table will NOT be sent to workflow
		int to_workflow = 1;    // Output table will be sent to workflow (default)
		/***********************************************************************/

		int report_type = m_INCStandard.STD_GetReportType();
		int exit_fail = 0;  
		int start_date = OCalendar.today(); 
		int qid;

		String date_string = OCalendar.formatDateInt(start_date);
		String sFileName = "STD_Missed_Validations"; 
		String sReportTitle = "Missed Validations Report";
		String errorMessages = "";

		Table report = Util.NULL_TABLE;
		Table temp = Util.NULL_TABLE;
		Table tParam = Util.NULL_TABLE;
		Table tCrystal = Util.NULL_TABLE;
		Table log_errors_table;


		error_log_file = Util.errorInitScriptErrorLog(sFileName);
		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");
		m_INCStandard.STD_InitRptMgrConfig(error_log_file, argt);

		qid = Query.tableQueryInsert(argt, "bunit", "query_result_plugin");  

		/* Load appropriate transactions */
		report = GetTransactions(qid);
			
		/* Format table for the report */
		FormatReportTable(report, report_type);
		
		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0) {
			m_INCStandard.STD_PrintTableDumpToFile(report, sFileName, sReportTitle + " For " + date_string, error_log_file);
		}  

		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0) {
			temp = Table.tableNew();
			temp.select( report, "*", "deal_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_Missed_Validations", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				exit_fail = 1;
				errorMessages += "Error value returned from m_INCStandard.STD_SaveUserTable.\n";
			}
			temp.destroy();
		}

		GroupReportTable(report);

		/*** Generate/Save/m_INCStandard.Print Crystal Report Output ***/
		if(m_INCStandard.STD_UseCrystalOutput() != 0) {

			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);
			tParam.addCol( "report_type", COL_TYPE_ENUM.COL_INT);
			tParam.setInt( "report_type", 1, report_type);

			tCrystal = report.copyTable();
			TABLE_convertAllColsToStringWithException(tCrystal, COL_TYPE_ENUM.COL_DOUBLE.toInt(), error_log_file);
			//tCrystal.printTableToTtx( "c:\\temp\\"+ sFileName + ".ttx");

			tCrystal.clearGroupBy();
			tCrystal.addGroupBy( "internal_bunit");
			tCrystal.addGroupBy( "deal_num");
			tCrystal.addGroupBy( "tran_num");
			tCrystal.groupBy();

//			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0){
//				exit_fail = 1;
//				errorMessages += "Error value returned from m_INCStandard.STD_OutputCrystal.\n";
//			}

			tParam.destroy();
			tCrystal.destroy();
		}

		report.groupTitleAbove( "internal_bunit");
		report.colHide( "internal_bunit");

		/*** Create Report Viewer EXC File ***/
		if(m_INCStandard.report_viewer != 0){
			m_INCStandard.STD_PrintTextReport(report, sFileName + ".exc", sReportTitle + " For " + date_string, sReportTitle + "\nCurrent Date: " + OCalendar.formatDateInt(OCalendar.today()), error_log_file);
		}

		/*** View Table ***/
		if(m_INCStandard.view_table != 0){
			m_INCStandard.STD_ViewTable(report, sReportTitle + " For " + date_string, error_log_file); 
		}

		if(report.getNumRows() > 0) {
			errorMessages = "Found deals with missed validations.\n";
			exit_fail = 1;
		}

		/* copy output table for passing it to workflow of to_workflow = 1*/
		log_errors_table = report.copyTable();
		report.destroy();

		m_INCStandard.Print(error_log_file, "END", "*** End of STD_Missed_Validations script ***");

		if (exit_fail == 1)
			if(to_workflow == 1)
				Util.exitFail(log_errors_table);
			else
				throw new OException( errorMessages );
		else 
		{
		   log_errors_table.destroy();
		   return;
		}
	}
	
	//================================================================================================//
	// Local Function Definitions                                                                     //
	//================================================================================================//
	
	private Table GetTransactions(int qid) throws OException
	{
		Table report = Table.tableNew();
		
		report.addCol( "deal_num",           COL_TYPE_ENUM.COL_INT );
		report.addCol( "tran_num",           COL_TYPE_ENUM.COL_INT );
		report.addCol( "ins_num",            COL_TYPE_ENUM.COL_INT );
		report.addCol( "tran_status",        COL_TYPE_ENUM.COL_INT );
		report.addCol( "ins_type",           COL_TYPE_ENUM.COL_INT );
		report.addCol( "trade_date",         COL_TYPE_ENUM.COL_INT );
		report.addCol( "external_lentity",   COL_TYPE_ENUM.COL_INT );
		report.addCol( "internal_portfolio", COL_TYPE_ENUM.COL_INT );
		report.addCol( "reference",          COL_TYPE_ENUM.COL_STRING );
		report.addCol( "unit",               COL_TYPE_ENUM.COL_INT );
		report.addCol( "ticker",             COL_TYPE_ENUM.COL_STRING );
		report.addCol( "cusip",              COL_TYPE_ENUM.COL_STRING );
		report.addCol( "currency",           COL_TYPE_ENUM.COL_INT );
		report.addCol( "position",           COL_TYPE_ENUM.COL_DOUBLE );
		report.addCol( "internal_contact",   COL_TYPE_ENUM.COL_INT );
		report.addCol( "internal_bunit",     COL_TYPE_ENUM.COL_INT );
		report.addCol( "start_date",         COL_TYPE_ENUM.COL_INT );
		report.addCol( "mat_date",           COL_TYPE_ENUM.COL_INT );

		String strWhat = " SELECT a.deal_tracking_num deal_num, a.tran_num, a.ins_num, a.tran_status, a.ins_type, "
			+ "a.trade_date, a.external_lentity, a.internal_portfolio, a.reference, ip.unit, h.ticker, h.cusip, "
			+ "a.currency, a.position, a.internal_contact, p.party_id internal_bunit, ip.start_date, ip.mat_date ";

		String strFrom = " FROM ab_tran a, trans_status t, party p, ins_parameter ip, header h ";

		String strWhere = " WHERE " +
				"t.trans_status_id = a.tran_status AND " +
				"p.party_id = a.internal_bunit AND " +
				"ip.ins_num = a.ins_num AND " +
				"a.ins_num = h.ins_num AND " +
				"p.party_class = 1 AND " +
				"p.int_ext = 0 AND " +
				"a.trade_flag = 1 AND " +
				"t.new_flag = 1 AND " +
				"ip.param_seq_num = 0 ";
		String queryTableName;
		if (qid > 0)
		{
			queryTableName = Query.getResultTableForId(qid);
			if ( queryTableName == null )
			{
				queryTableName = "query_result_plugin";
				m_INCStandard.Print(error_log_file, "ERROR", "Query id " + qid 
						+ " does not have a query result table. Default " + queryTableName + " table will be used.");
			}
			strFrom  += " , " + queryTableName + " q ";
			strWhere += " AND q.unique_id = " + qid + " AND a.internal_bunit = q.query_result ";  
		}
		else
		{
			m_INCStandard.Print(error_log_file, "PARAM", "No parameter script found. ALL Business Units will be included in this report");     
		}

		/*  Below loop has been replaced by TABLE_Query_InsertN() function and modified SQL.  FB 02/02/2010  */
		/*Alter strWhere clause to select only business units in argt if applicable 
		if(argt.getNumRows() > 0 || argt.getColNum( "bunit") >= 0){
			argt.convertColToString( 1);
			for(i = argt.getNumRows(); i > 0; i--){ 	
				strParamParties += argt.getString( "bunit", i);
				if(i > 1) 
					strParamParties += ", ";
			}
		   strWhere += " and a.internal_bunit in (" + strParamParties + ")";
		} else{
			m_INCStandard.Print(error_log_file, "PARAM", "No parameter script found.  ALL Business Units will be included in this report");
		}
	   */

		/* Load ab_tran table */
		try
		{
			DBaseTable.execISql( report, strWhat + strFrom + strWhere );
		}
		catch( OException oex )
		{
			m_INCStandard.Print( error_log_file, "ERROR", "OException at execute(), unsuccessful database query, " + oex.getMessage() );
		}
		finally
		{
			Query.clear(qid);
		}

		return report;
	}

	//------------------------------------------------------------------------------------------------//

	private void FormatReportTable(Table report, int report_type) throws OException
	{
		/* Load Party and trader Names */
		report.setColFormatAsRef( "external_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		report.setColFormatAsRef( "internal_bunit",   SHM_USR_TABLES_ENUM.PARTY_TABLE);
		report.setColFormatAsRef( "internal_contact", SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);

		report.colHide( "ins_num");
		//report.colHide( "external_lentity_s");

		/* Column Formatting */
		report.setColFormatAsRef( "ins_type",    		SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		report.setColFormatAsRef( "tran_status", 		SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		report.setColFormatAsRef( "currency",    		SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		report.setColFormatAsRef( "unit",    			SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		report.setColFormatAsRef( "internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		report.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC,COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		report.setColFormatAsDate( "trade_date");  
		report.setColFormatAsDate( "start_date");  
		report.setColFormatAsDate( "mat_date");  

		report.setRowHeaderWidth( 25); //squeeze left
		
		report.formatSetWidth( "deal_num",     			9);
		report.formatSetWidth( "tran_num",              9); 
		report.formatSetWidth( "tran_status",          15);
		report.formatSetWidth( "ins_type",             20);
		report.formatSetWidth( "ticker",               20);
		report.formatSetWidth( "cusip",                20);
		report.formatSetWidth( "isin",                 20);
		report.formatSetWidth( "position",             14);
		report.formatSetWidth( "currency",              5);
		report.formatSetWidth( "unit",                 12);
		report.formatSetWidth( "external_lentity",     27);
		report.formatSetWidth( "internal_bunit",       27);
		report.formatSetWidth( "internal_portfolio",   25);
		report.formatSetWidth( "reference",            20);
		report.formatSetWidth( "internal_contact",     13);
		report.formatSetWidth( "tran_status",          20);
		report.formatSetWidth( "trade_date",           12);
		report.formatSetWidth( "start_date",           12);
		report.formatSetWidth( "mat_date",             12);

		/* Format the table and set column titles */
		report.setColTitle( "deal_num",    			"Deal\nNum");
		report.setColTitle( "tran_num",    			"Tran\nNum");
		report.setColTitle( "tran_status", 			"Tran\nStatus");
		report.setColTitle( "ins_type",    			"Instrument\nType");
		report.setColTitle( "ticker",      			" \nTicker");
		report.setColTitle( "cusip",       			" \nCusip");
		report.setColTitle( "unit",        			" \nUnit");
		report.setColTitle( "position",    			" \nPosition");
		report.setColTitle( "currency",    			" \nCcy");
		report.setColTitle( "external_lentity",   	" \nCounterparty");
		report.setColTitle( "internal_portfolio", 	" \nPortfolio");
		report.setColTitle( "reference",          	" \nReference");
		report.setColTitle( "internal_contact",   	" \nTrader");
		report.setColTitle( "trade_date",         	"Trade\nDate");
		report.setColTitle( "start_date",         	"Start\nDate");
		report.setColTitle( "mat_date",           	"End\nDate");
		report.setColTitle( "internal_bunit", 		"Business\nUnit");

		if(report_type == 1) {
			report.colHide( "ticker");
			report.colHide( "cusip");
		}
		if(report_type == 2) {
			report.colHide( "unit");
			report.colHide( "position");
		}
	}
	
	//------------------------------------------------------------------------------------------------//

	private void GroupReportTable(Table report) throws OException
	{
		report.clearGroupBy();
		report.addGroupBy( "internal_bunit");
		report.addGroupBy( "deal_num");
		report.addGroupBy( "tran_num");
		report.groupByFormatted();
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
	private void TABLE_convertAllColsToStringWithException(Table table, int except_col_type, String error_log_file) throws OException
	{
		int curr_col = 0;
		int total_cols = table.getNumCols();

		if( total_cols == 0 )
		{
			m_INCStandard.Print(error_log_file, "ERROR", "Invalid table was found when calling TABLE_convertAllColsToStringWithException()");
			return;
		}
		
		for(curr_col=1; curr_col<=total_cols; curr_col++)
		{
			if(  table.getColFormat( curr_col) == COL_FORMAT_TYPE_ENUM.FMT_NONE.toInt())
				continue;
			if(  table.getColType( curr_col) == except_col_type)
				continue;
			table.convertColToString( curr_col);
		}
	}
	
}
