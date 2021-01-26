package com.olf.jm.reportbuilder;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Table;
import com.olf.openjvs.OException;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.jm.logging.Logging;

/* History:
 * 2020-10-29	V1.0	dnagy	- Initial Version
 * Assembles the main Operation dashboard table from the staging tables
 * Part of  the Management Report 3 pack - Operational Reports
 */
	
public class ManagementReportOpsDashboard implements IScript {
	
	public static String dashboard_table_name = "USER_MR3_DASHBOARD";
	
	public static String[] regions = {"JM PMM UK", "JM PMM US", "JM PMM HK", "JM PMM CN", "JM PM LTD"};
	public static String[] intervals = {"PERIOD_COLUMN_NAME>=(SELECT DATEADD(m, DATEDIFF(m, 0, business_date), 0) )",
			"PERIOD_COLUMN_NAME>=(SELECT DATEADD(m, DATEDIFF(m, 0, business_date)-1, 0) ) and PERIOD_COLUMN_NAME<(SELECT DATEADD(m, DATEDIFF(m, 0, business_date), 0) )",
			"PERIOD_COLUMN_NAME>=(SELECT '01-Apr-'+CASE WHEN MONTH(business_date)>=4 THEN CONVERT(varchar, YEAR(business_date)) ELSE CONVERT(varchar, YEAR(business_date)-1) END)",
			"PERIOD_COLUMN_NAME>=(SELECT DATEADD(year, -1, business_date))"};
			
	public int display_order;
	
	public void execute(IContainerContext context) throws OException {
	
		Table dashboard = Table.tableNew(dashboard_table_name);
		
		try {
			Logging.init(this.getClass(), "", "");
			Logging.info("Starting " + getClass().getSimpleName());
		
	        String what, from, report_name, region_match, where_add_1, where_add_2, period_column_name = "";
	        String metal_cash = "Metal";
	        Boolean region_split = true;

	        DBUserTable.load(dashboard);
	        dashboard.clearRows();
			
	        report_name= "All trades";
	        what = "count(*)";
	        from = "ab_tran, configuration, party";
	        region_match = "short_name = ";
	        where_add_1 = " and ab_tran.internal_bunit=party.party_id and toolset not in (35, 8)"; //not call notice or composer toolsets
		    period_column_name = "input_date";
		    display_order = 1;
		    
	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);
	        
	        report_name= "Commercial trades";
	        what = "count(*)";
	        from = "USER_MR3_COMMERCIAL, configuration";
	        region_match = "internal_bunit = ";
	        where_add_1 = "";
	        
	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);
	        
	        report_name = "Document status 'Disputed'";
	        from = "USER_MR3_BO_STATUS,configuration";
	        region_match = "internal_bunit = ";
		    where_add_1 = " and doc_status = 'Disputed'";
		    period_column_name = "last_update";
		    	        
	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);

		    report_name = "Confirmation delay > 36 hours";
		    what = "count(case when business_delay>2160 then 1 end)";  // business_delay is in minutes, 36 * 60 = 2160
		    period_column_name = "trade_time";
		    where_add_1 = " and doc_status = '3 Confirmed'";
		    		    
	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);

	        report_name = "Cancelled commercial trades";
	        what = "count(*)";
	        from = "USER_MR3_COMMERCIAL, configuration";
	        region_match = "internal_bunit = ";
		    period_column_name = "input_date";
		    where_add_1 = " and tran_status = 'Cancelled'";

	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);
		    
	        report_name = "Physical trades";
	        what = "count(*)";
	        from = "(select tran_num, internal_bunit, input_date from USER_MR3_COMMPHYS union select tran_num, internal_bunit, input_date from USER_MR3_CASH) L, configuration";
		    where_add_1 = "";

	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);

	        report_name = "Cancelled physical trades";
	        what = "count(*)";
	        from = "(select tran_num, tran_status, internal_bunit, input_date from USER_MR3_COMMPHYS union select tran_num, tran_status, internal_bunit, input_date from USER_MR3_CASH) L, configuration";
		    where_add_1 = " and tran_status = 'Cancelled'";

	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);

	        report_name = "Receipts";
	        what = "sum(parcel_count)";
	        from = "USER_MR3_COMMPHYS,configuration";
		    period_column_name = "maturity_date";
		    where_add_1 = " and buy_sell=0";

	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);

	        report_name = "Dispatches";
	        what = "sum(parcel_count)";
	        from = "USER_MR3_COMMPHYS,configuration";
		    where_add_1 = " and buy_sell=1";

	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);

	        what = "count(*)";
	        from = "USER_MR3_CASH,configuration";
		    period_column_name = "input_date";
	        where_add_1 = "";
	        where_add_2 = "";

	        for (int j=1; j <= 2; j++) {
		        for (int i=0; i <= 2; i++) {
		        	report_name = ((metal_cash == "Metal") ? "Metal transfers" : "Charges") 
		        	  + ((where_add_2.indexOf("Amended")>0) ? " - Amended" : "") + ((where_add_2.indexOf("Cancelled")>0) ? " - Cancelled & Deleted" : "");
		        	where_add_1 = " and metal='" + metal_cash + "'" + where_add_2;
		        	blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);
			        where_add_2 = " and tran_status = 'Amended'";
			        if (i == 1) { where_add_2 = " and tran_status in ('Cancelled','Deleted')"; }
		        }
	        	metal_cash = "Cash";
	        	where_add_2="";
	        }
		        
	        report_name = "Dispatches requiring approval";
	        from = "USER_MR3_TPM_LOGS,configuration";
		    where_add_1 = " and definition_name='Dispatch'";
		    period_column_name = "row_creation";
	        region_match = " assigned_group not like ";
		    
	        blockfill(dashboard, report_name, what, from, false, region_match, where_add_1, period_column_name);
		    
	        report_name = "Metal Transfers requiring approval";
		    where_add_1 = " and definition_name='Metal Transfer'";

	        blockfill(dashboard, report_name, what, from, false, region_match, where_add_1, period_column_name);
	        
	        report_name = "Dispatch delay > 1 day";
	        what = "count(case when delay>0 then 1 end)";
	        from = "USER_MR3_DEAL_HISTORY,configuration";
		    period_column_name = "first_timestamp";
	        region_match = "internal_bunit = ";
	        where_add_1 = "";
	        
	        /* report_name = "Order delay > 5 days";
	        what = "count(case when delay>5 then 1 end)";
	        from = "USER_MR3_COMMPHYS,configuration";
``		    period_column_name = "maturity_date";
	        region_match = "internal_bunit = ";
	        where_add_1 = ""; */

	        blockfill(dashboard, report_name, what, from, region_split, region_match, where_add_1, period_column_name);
			
			cashtypesadd(dashboard);
			
	        DBUserTable.clear(dashboard);
	        DBUserTable.bcpIn(dashboard);
	        
		} 
		catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.info(message);
			for (StackTraceElement ste : e.getStackTrace() )  {
				Logging.error(ste.toString(), e);
			}
		} finally {
			dashboard.destroy();
			Logging.info("End " + getClass().getSimpleName());
			Logging.close();
		}
		
	}
	
	public void blockfill (Table dashboard, String report_name, String what, String from, Boolean region_split, String region_match, String where_add_1, String period_column_name) {
		try {
			Table result = Table.tableNew();
			String sql = "";
			int rowslide = dashboard.getNumRows();
			
			for (int i = 0; i < regions.length; i++) {

	        	dashboard.addRow();
	        	dashboard.setString("report_name", i+1+rowslide, report_name);
	        	dashboard.setInt("display_order", i+1+rowslide, display_order);
	        	if (region_split) {
		       		dashboard.setString("region", i+1+rowslide, regions[i]);
		        } else {
		       		dashboard.setString("region", i+1+rowslide, "ALL");
		        }

		        for (int j = 0; j < intervals.length; j++) {
		        	result.clearRows();
		        	sql = "select " + what + " from " + from + " where " + region_match + "'" + regions[i] +
		        			"' and " + intervals[j] + where_add_1;
		        	sql = sql.replace("PERIOD_COLUMN_NAME", period_column_name);
		        	
					DBase.runSqlFillTable(sql, result);
					dashboard.setInt(j+2, i+1+rowslide, result.getInt(1, 1));
		        }

				if (!region_split) { break; }

	        }
	        
	        ++display_order;
		} 
		catch (OException e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.info(message);
			for (StackTraceElement ste : e.getStackTrace() )  {
				Logging.error(ste.toString(), e);
			}
		}
	}
		
	public void cashtypesadd (Table dashboard) {

		try {
			Table tbl_curr_month = Table.tableNew();
			Table tbl_last_month = Table.tableNew();
			Table tbl_YTD = Table.tableNew();
			Table tbl_last_year = Table.tableNew();
			Table all_in = Table.tableNew();
			
			String sql_curr_month = "select cflow_type, count(*) as cur_month, '' as region, 99 as display_order from USER_MR3_CASH, configuration where cflow_type != 'Upfront'" 
				+ " and input_date>(SELECT DATEADD(m, DATEDIFF(m, 0, business_date), 0) ) group by cflow_type";
			String sql_last_month = "select cflow_type, count(*) as last_month, '' as region, 99 as display_order from USER_MR3_CASH, configuration where cflow_type != 'Upfront'" 
				+ " and input_date>(SELECT DATEADD(m, DATEDIFF(m, 0, business_date)-1, 0) ) "
				+ " and input_date<=(SELECT DATEADD(m, DATEDIFF(m, 0, business_date), 0) ) group by cflow_type";
			String sql_YTD = "select cflow_type, count(*) as YTD, '' as region, 99 as display_order from USER_MR3_CASH, configuration where cflow_type != 'Upfront'" 
				+ " and input_date>(SELECT '01-Apr-'+CASE WHEN MONTH(business_date)>=4 THEN CONVERT(varchar, YEAR(business_date)) ELSE CONVERT(varchar, YEAR(business_date)-1) END) group by cflow_type";
			String sql_last_year = "select cflow_type, count(*) as last_year, '' as region, 99 as display_order from USER_MR3_CASH, configuration where cflow_type != 'Upfront'" 
				+ " and input_date>(SELECT DATEADD(year, -1, business_date)) group by cflow_type";
				
			DBase.runSqlFillTable(sql_curr_month, tbl_curr_month);
			DBase.runSqlFillTable(sql_last_month, tbl_last_month);
			DBase.runSqlFillTable(sql_YTD, tbl_YTD);
			DBase.runSqlFillTable(sql_last_year, tbl_last_year);

			all_in.select(tbl_curr_month, "cflow_type, region, display_order", "cur_month GT 0");
			all_in.select(tbl_last_month, "cflow_type, region, display_order", "last_month GT 0");
			all_in.select(tbl_YTD, "cflow_type, region, display_order", "YTD GT 0");
			all_in.select(tbl_last_year, "cflow_type, region, display_order", "last_year GT 0");
			all_in.makeTableUnique();
			all_in.select(tbl_curr_month, "cflow_type, cur_month, region, display_order", "cflow_type EQ $cflow_type");
			all_in.select(tbl_last_month, "cflow_type, last_month, region, display_order", "cflow_type EQ $cflow_type");
			all_in.select(tbl_YTD, "cflow_type, YTD, region, display_order", "cflow_type EQ $cflow_type");
			all_in.select(tbl_last_year, "cflow_type, last_year, region, display_order", "cflow_type EQ $cflow_type");
			all_in.sortCol("cflow_type");

			//dashboard.addRowsWithValues("(*** CASH TRANSFER TYPES SECTION ***),,,,,,99");
			dashboard.select(all_in, "cflow_type(report_name), region, display_order, cur_month, last_month, YTD, last_year", "cur_month GT -1");

			tbl_curr_month.destroy();
			tbl_last_month.destroy();
			tbl_YTD.destroy();
			tbl_last_year.destroy();
			all_in.destroy();

		}
		catch (OException e) {
				String message = "Exception caught:" + e.getMessage();
				Logging.info(message);
				for (StackTraceElement ste : e.getStackTrace() )  {
					Logging.error(ste.toString(), e);
			}
		}

	}
		
}

