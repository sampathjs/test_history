/*$Header: /cvs/master/olf/plugins/standard/process/STD_Adhoc_UnMature_Trades.java,v 1.7 2013/01/22 21:23:09 dzhu Exp $*/
/*
File Name:                       STD_Adhoc_UnMature_Trades.java

Report Name:                     Adhoc UnMatured Trade Listing

Output File Name:                STD_Adhoc_UnMature_Trades.txt
                                 STD_Adhoc_UnMature_Trades.rpt/.html/.pdf/.csv
                                 STD_Adhoc_UnMature_Trades.log
                                 USER_Adhoc_UnMature_Trades

Available RptMgr Outputs:        Print Crystal
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

Author:                          Jin Chen
Creation Date:                   Jan 13, 2009

Revision History:                Jan 22, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id
                                 Oct 07, 2010 - (adelacruz) Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
                                              - Replaced function calls to the OpenJVS String library with calls to the Java String library
                                              - Replaced Util.exitFail with throwing an OException
                                 Jan 13, 2009 - New Script (DTS 49058)

Main Script:                     This script
Parameter Script:                STD_Adhoc_Parameter or STD_Saved_Query_Param
Display Script:                  None
Script category: N/A
Report Description:              This script will attempt to unmature matured transactions of type TRADING 
                                 queried in the adhoc browser or specified by a saved query.  Use STD_Adhoc_Parameter
                                 for the former query method, STD_Saved_Query_Param for the latter.  It will display a report with contents
                                 representing the successfully unmatured transactions.  Any transactions that fail to unmature will generate log file
                                 error(s), and will not be displayed on the report.

Assumptions:                     None

Instructions:                    None

Uses EOD Results?                No

Which EOD Results are used?      None

When can the script be run?      Adhoc

Columns:                         Column            Source                           Group    Title Break   Sum?
                                 Business Unit     ab_tran.internal_bunit           1        Yes
                                 Instrument Type   ab_tran.ins_type                 2        No
                                 Maturity Date     ab_tran.maturity_date            3        No
                                 Deal Num          ab_tran.deal_tracking_num        4        No
                                 Toolset           ab_tran.toolset            
                                 Trade Date        ab_tran.trade_date         
                                 Counterparty      ab_tran.external_lentity   
                                 Position          ab_tran.position                                        No
                                 Unit              parameter.unit                 
                                 Ccy               ab_tran.currency           
                                 Portfolio         ab_tran.internal_portfolio 
                                 Trader            ab_tran.internal_contact   
 */

package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;	

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class JM_Adhoc_UnMature_Trades implements IScript {
	
	private JVS_INC_Standard m_INCStandard;
	public JM_Adhoc_UnMature_Trades(){
		m_INCStandard = new JVS_INC_Standard();
	}
	
	private String errorMessages;
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		int exit_fail = 0;
		int qid_col
		,query_n_col_num;

		String file_name = "STD_Adhoc_UnMature_Trades"; 
		String report_title = "Adhoc UnMatured Trade Listing";
		String error_log_file = Util.errorInitScriptErrorLog(file_name);
		errorMessages = "";
		
		String crystal_file_name = file_name;

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + file_name + " script ***");

		/*** Check if a param script was used ***/
		if(argt.getNumRows() < 1 
				|| (qid_col=argt.getColNum("QueryId")) < 1
				|| (query_n_col_num=argt.getColNum( "QueryName")) < 1)
		{
			m_INCStandard.Print(error_log_file, "PARAM", "No param script was used. This script must be run with STD_Adhoc_Parameter or STD_Saved_Query_Param");
			errorMessages += "No param script was used. This script must be run with STD_Adhoc_Parameter or STD_Saved_Query_Param. ";
			exit_fail=1;
		}
		else
		{
			/*** Get Param Values From argt ***/
			int qid=argt.getInt( qid_col, 1);

			if(qid > 0)
			{
				String query_method
				,query_name=argt.getString( query_n_col_num, 1);

				m_INCStandard.STD_InitRptMgrConfig(error_log_file, argt);   

				if( query_name != null && ! query_name.isEmpty() )
				{
					query_method="saved query";
				}
				else
				{
					query_method="adhoc trade browser";
				}
				m_INCStandard.Print(error_log_file, "INFO", "Using transactions from "+query_method);

				if(exec(file_name, report_title, crystal_file_name, qid, query_method, error_log_file) == 1){
					exit_fail = 0;
				}
				else{
					errorMessages += "Error value returned from exec(). ";
					exit_fail = 1;
				}
			}
		}

		m_INCStandard.Print(error_log_file, "END", "*** End of " + file_name + " script ***");

		if(exit_fail != 0)
		   throw new OException( errorMessages );
		return;
	}

	int exec(String file_name, String report_title, String crystal_file_name, int qid, String query_method, String error_log_file) throws OException
	{
		int retval=1
		,num_rows, bool_flag;
		Table query_t=Table.tableNew();
		/*** Create Output Table ***/
		Table output_t = create_output_table(error_log_file);

		String queryTableName = Query.getResultTableForId(qid);
		if ( queryTableName == null && qid > 0 )
		{
			queryTableName = Query.getResultTableForId(qid)/* "query_result"*/;
			m_INCStandard.Print(error_log_file, "ERROR", "Query id " + qid 
					+ " does not have a query result table. Default " + queryTableName + " table will be used.");
		}

		String what = " SELECT "
			/* to output */
			+"         distinct abt.internal_bunit"
			+"       ,abt.ins_type"
			+"       ,abt.maturity_date"
			+"       ,abt.deal_tracking_num"
			+"       ,abt.toolset"
			+"       ,abt.trade_date"
			+"       ,abt.external_lentity"
			+"       ,abt.position"
			+"       ,p.unit"
			+"       ,abt.currency"
			+"       ,abt.internal_portfolio"
			+"       ,abt.internal_contact"
			/* script use */
			+"       ,abt.tran_num"
			+"       ,0 AS mat_succeed ";
		String from = " FROM "
				+"         ab_tran abt"
				+"       ,parameter p"
				+"       ," + queryTableName + " qr ";
		String where = " WHERE "
					+"        qr.unique_id="+qid
					//+"   and abt.tran_type="+TRAN_TYPE_ENUM.TRAN_TYPE_TRAN.toInt()
					+"   and abt.tran_status = "+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()
					+"   and abt.tran_num = qr.query_result"
					+"   and p.ins_num = abt.ins_num";

		try{
			DBaseTable.execISql( query_t, what + from + where);
		}
		catch( OException oex ){
			m_INCStandard.Print( error_log_file, "ERROR", "OException at exec(), unsuccessful database query, " + oex.getMessage() );
			retval = 0;
		}

		if( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			m_INCStandard.Print(error_log_file, "ERROR", "Initial query for data failed");
			errorMessages += "Initial query for data failed. ";
			retval=0;
		}
		else if ((num_rows=query_t.getNumRows()) <1)
		{
			m_INCStandard.Print(error_log_file, "ERROR", "Transactions specified by the "+query_method+" are not valid for this process.");
		}
		else
		{
			query_t.colConvertDateTimeToInt( "maturity_date" );
			query_t.colConvertDateTimeToInt( "trade_date" );
			query_t.setColFormatNone( "maturity_date" );
			query_t.setColFormatNone( "trade_date" );
			
			int i, tran_num;
			String tsel_what_s;

			for(i=1;i<=num_rows;i++)
			{
				tran_num=query_t.getInt("tran_num",i);

				m_INCStandard.Print(error_log_file, "INFO","Attempting to unmature TranNum: "+tran_num);

				bool_flag = Transaction.unmatureDeal(tran_num);
				if( bool_flag != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					m_INCStandard.Print(error_log_file, "INFO","Could not unmature TranNum: "+tran_num + " because "+ DBUserTable.dbRetrieveErrorInfo(bool_flag,"Transaction.unmatureDeal() failed"));						
					retval = 0;
				}
				else
				{
					query_t.setInt( "mat_succeed", i, 1);
					m_INCStandard.Print(error_log_file, "INFO", "Tran #"+tran_num+" unmature successfully."); 
				}
			}
			
			tsel_what_s = tsel_cr_csv_list_from_cols(output_t, query_t);
			if( tsel_what_s == null || tsel_what_s.isEmpty() )
			{
				m_INCStandard.Print(error_log_file, "ERROR", 
						"Found no matching columns to select into output from query table."
						+" Check that tables have matching columns (by name)");
			}
			else
			{
				/*** Populate Output Table ***/
				if(output_t.select(query_t
						,tsel_what_s
						,"mat_succeed EQ 1")!=OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					m_INCStandard.Print(error_log_file, "ERROR", "Selection of data into output table failed!  Output will be blank");
				}
			}
		}

		if (gen_output(output_t, file_name, crystal_file_name, report_title, error_log_file, 0) == 1 && retval == 1){
			retval =1;
		}
		else{
			errorMessages += "Error value returned from gen_output(). ";
			retval =0;
		}

		/*** Destroy all Tables ***/
		query_t.destroy();
		output_t.destroy();  

		return retval;
	}

	/*
   to be used with TABLE_Select
   For every column in dest
   check that this column exists (by name) in src 
   if so, include it in a csv list of cols
   this helps construct the what clause of a table select
   and mimics TABLE_CopyRowAddAllByColName, but adds where constraints 
	 */
	String tsel_cr_csv_list_from_cols(Table dest, Table src) throws OException
	{
		int i
		,num_cols=dest.getNumCols()
		,has_length=0;

		String col_name,out_s = "";

		for(i=1; i <= num_cols; i++)  
		{
			if(src.getColNum( (col_name=dest.getColName(i))) > 0)
			{
				if(has_length != 0)
				{
					out_s+=",";
				}
				out_s += col_name;
				has_length=1;
			}
		}
		return out_s;
	}

	Table create_output_table(String error_log_file) throws OException
	{
		Table output_t=Table.tableNew();
		m_INCStandard.Print(error_log_file, "INFO", "Creating output table");

		output_t.addCol( "internal_bunit", "Business\nUnit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output_t.addCol( "ins_type", "Instrument\nType", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		output_t.addCol( "maturity_date", COL_TYPE_ENUM.COL_INT, "Maturity\nDate");
		output_t.addCol( "deal_tracking_num", COL_TYPE_ENUM.COL_INT, "Deal\nNum");
		output_t.addCol( "toolset", " \nToolset", SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
		output_t.addCol( "trade_date", COL_TYPE_ENUM.COL_INT, "Trade\nDate");
		output_t.addCol( "external_lentity", " \nCounterparty", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output_t.addCol( "position", COL_TYPE_ENUM.COL_DOUBLE, " \nPosition");
		output_t.addCol( "unit", " \nUnit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		output_t.addCol( "currency", " \nCcy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		output_t.addCol( "internal_portfolio", " \nPortfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		output_t.addCol( "internal_contact", " \nTrader", SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);

		/*** Set Column Widths ***/
		output_t.formatSetWidth( "internal_bunit", 32);
		output_t.formatSetWidth( "ins_type", 32);
		output_t.formatSetWidth( "maturity_date", 12);
		output_t.formatSetWidth( "deal_tracking_num", 10);
		output_t.formatSetWidth( "toolset", 32);
		output_t.formatSetWidth( "trade_date", 12);
		output_t.formatSetWidth( "external_lentity", 32);
		output_t.formatSetWidth( "position", 15);
		output_t.formatSetWidth( "unit", 10);
		output_t.formatSetWidth( "currency", 5);
		output_t.formatSetWidth( "internal_portfolio", 32);
		output_t.formatSetWidth( "internal_contact", 32);

		return output_t;
	}

	int gen_output(Table output_t, String file_name, String crystal_file_name, String report_title, String error_log_file, int ttx) throws OException
	{
		int retval=1;
		int use_crystal_output=0;
		String ttx_path="c:\\temp";


		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0)
		{
			Table ut_t = Table.tableNew();
			ut_t.select( output_t, "*", "deal_tracking_num GT -1"); 

			if(m_INCStandard.STD_SaveUserTable(ut_t, "USER_Adhoc_UnMature_Trades", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				errorMessages += "Error value returned fro m_INCStandard.STD_SaveUserTable. ";
				retval=0;
			}

			ut_t.destroy();
		}

		if( (use_crystal_output=m_INCStandard.STD_UseCrystalOutput()) == 1
				|| m_INCStandard.csv_dump == 1)
		{
			Table cry_csv_t = format_for_cry_csv(output_t);
			//cry_csv_t.printTableToTtx( "c:\\temp\\cry_csv_t.ttx");
			use_crystal_output = 0; // FIXME need to resolve why Std fails to generate PDF!!!
			/*** Generate/Save/m_INCStandard.Print Crystal Report Output ***/
			if(use_crystal_output != 0)
			{
				Table param_t = m_INCStandard.STD_CreateParameterTable(report_title, file_name)
				, crystal_t = cry_csv_t.copyTableFormatted( 0);

				crystal_t.formatSetWidth( "internal_bunit", 32);
				crystal_t.formatSetWidth( "ins_type", 32);
				crystal_t.formatSetWidth( "maturity_date", 12);
				//crystal_t.formatSetWidth( "deal_tracking_num", 10);
				crystal_t.formatSetWidth( "toolset", 32);
				crystal_t.formatSetWidth( "trade_date", 12);
				crystal_t.formatSetWidth( "external_lentity", 32);
				//crystal_t.formatSetWidth( "position", 15);
				crystal_t.formatSetWidth( "unit", 10);
				crystal_t.formatSetWidth( "currency", 5);
				crystal_t.formatSetWidth( "internal_portfolio", 32);
				crystal_t.formatSetWidth( "internal_contact", 32);

				if(ttx != 0)
				{
					crystal_t.printTableToTtx( ttx_path + "\\" + file_name + ".ttx");
				}
				if(m_INCStandard.STD_OutputCrystal(crystal_t, param_t, crystal_file_name, file_name, error_log_file) == 0){
						errorMessages += "Error value returned from m_INCStandard.STD_OutputCrystal(). ";
					retval=0;
				}

				param_t.destroy();
				crystal_t.destroy();
			}

			/*** Dump to CSV ***/
			if(m_INCStandard.csv_dump != 0)
			{
				m_INCStandard.STD_PrintTableDumpToFile(cry_csv_t, file_name, report_title, error_log_file);
			}   
			cry_csv_t.destroy();
		}

		if(m_INCStandard.view_table == 1|| m_INCStandard.report_viewer == 1)
		{
			Table vt_rv_t = format_for_vt_rv(output_t);


			/*** Create Report Viewer TXT File ***/
			if(m_INCStandard.report_viewer != 0)
			{
				m_INCStandard.STD_PrintTextReport(vt_rv_t, file_name + ".txt", report_title, report_title + "\nCurrent Date: " + OCalendar.formatDateInt(OCalendar.today()), error_log_file);
			}

			/*** View Table ***/
			if(m_INCStandard.view_table != 0)
			{

				m_INCStandard.STD_ViewTable(vt_rv_t, report_title, error_log_file);
			}

			vt_rv_t.destroy();
		}
		return retval;
	}

	Table format_for_cry_csv(Table output_t) throws OException
	{
		Table c=output_t.copyTable();

		c.groupFormatted( "internal_bunit, ins_type, maturity_date, deal_tracking_num");
		c.setColFormatAsDate( "trade_date");
		c.setColFormatAsDate( "maturity_date");
		c.formatSetJustifyRight( "trade_date");    
		c.formatSetJustifyRight( "maturity_date"); 
		c.setRowHeaderWidth( 32); 
		return c;
	}

	Table format_for_vt_rv(Table output_t) throws OException
	{
		/*
		 * crystal and csv formatting is a subset of that which is required for table-viewer/report-viewer
		 */
		Table v = format_for_cry_csv(output_t);

		v.groupTitleAbove( "internal_bunit");
		// v.colHide( "internal_bunit");
		// no group sum/sum
		v.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		return v;
	}

}