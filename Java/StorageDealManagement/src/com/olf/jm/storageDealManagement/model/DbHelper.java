package com.olf.jm.storageDealManagement.model;

import java.util.Date;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

public class DbHelper {
	
	public static String getLinkedReceiptBatchesSql(int dealTrackingNum) {
		String sql =    " SELECT distinct csdc_i.delivery_id as delivery_id, csh_i.location_id as location_id, csdc_i.batch_id \n" +
						" FROM comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i, ab_tran ab_i \n" +
						" WHERE  csh_i.delivery_id = csdc_i.delivery_id \n" +
						" AND csh_i.bav_flag = 1 \n"  +
						" AND csh_i.total_quantity > 0.0 \n" +
						" AND csdc_i.batch_id > 0 \n" +
						" AND ab_i.ins_num = csh_i.ins_num\n" +
						" AND ab_i.deal_tracking_num = " + dealTrackingNum + "\n" +
						" AND ab_i.current_flag = 1\n" +
						" AND ab_i.ins_sub_type = 9204\n" +
					  	" AND 0=(SELECT COUNT(*) \n" +
						"        FROM comm_sched_deliv_deal csdd2 \n" +
						"        WHERE csdd2.delivery_id = csdc_i.delivery_id \n" +
						"        AND deal_num <> 6)\n" +
						" AND 1=(SELECT count (*) " +
						"		   FROM comm_sched_deliv_deal csdd3 " +
						"        WHERE csdc_i.source_delivery_id <> csdc_i.delivery_id " +
						"        AND csdc_i.source_delivery_id <> 0 " +
						"        AND csdd3.delivery_id = csdc_i.source_delivery_id " +
						"        AND deal_num <> 6)";	
		return sql;
	}
	
	public static String getUnLinkedReceiptBatchesSql(int dealTrackingNum) {
		String sql = " SELECT distinct csdc_i.source_delivery_id as delivery_id, csh_i.location_id as location_id, csdc_i.batch_id \n" +
			 		  " FROM comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i , ab_tran ab_i \n" + 
			 		  " WHERE csh_i.delivery_id = csdc_i.delivery_id \n" +
			 		  " AND csh_i.bav_flag = 1 \n" +
			 		  " AND csh_i.total_quantity > 0.0 \n" +
			 		  " AND csdc_i.batch_id > 0 \n" +
			 		  " AND ab_i.ins_num = csh_i.ins_num \n" +
			 		  " AND ab_i.current_flag = 1 \n" +
			 		  " AND ab_i.tran_status in (3) \n" + 
			 		  " AND ab_i.deal_tracking_num = " + dealTrackingNum + " \n" + 
			 		  " AND ab_i.ins_sub_type = 9204 \n" +
			 		  " AND 0=(SELECT count (*) \n" +
			 		  "        FROM comm_sched_deliv_deal csdd2 \n" +
			 		  "        WHERE csdd2.delivery_id = csdc_i.delivery_id \n" +
			 		  "        AND deal_num <> 6) \n" +
			 		  " AND 0=(SELECT count (*) \n" +
			 		  "        FROM comm_sched_deliv_deal csdd3 \n" +
			 		  "        WHERE csdc_i.source_delivery_id <> csdc_i.delivery_id \n" +
			 		  "        AND csdc_i.source_delivery_id <> 0 \n" +
			 		  "        AND csdd3.delivery_id = csdc_i.source_delivery_id \n" +
	 		 		  "        AND deal_num <> 6)";
		return sql;
	}
	
	public static String buildSqlCommStoreMaturingOnDate( Session session, String location, String metal, Date maturityDate) {
		
		String sql = baseSql(location, metal);
		
		CalendarFactory calendarFactory = session.getCalendarFactory();
		String sqlMaturityDate = calendarFactory.getSQLString(maturityDate);
		sql = sql  + " AND maturity_date <= '" + sqlMaturityDate + "' \n";
		
		return sql.toString();
	}
	public static String buildSqlCommStoreMaturingOnDateByVar( Session session, String location, String metal, Date maturityDate) {
		
		String sql = baseSql(location, metal);
		
		CalendarFactory calendarFactory = session.getCalendarFactory();
		Date currentDate = calendarFactory.createSymbolicDate("0d").evaluate();
		String sqlMaturityDate = calendarFactory.getSQLString(maturityDate);
		String sqlCurrentDate = calendarFactory.getSQLString(currentDate);
		sql = sql  + " AND maturity_date <= '" + sqlMaturityDate + "' \n"
				   + " AND maturity_date >= '" + sqlCurrentDate + "' \n";
		
		PluginLog.info("sql: " + sql);
		
		return sql.toString();
	}
	
	public static String buildSqlCommStoreAfterDate( Session session, String location, String metal, Date testDate) {
		
		String sql = baseSql(location, metal);
		
		CalendarFactory calendarFactory = session.getCalendarFactory();
		String sqlTestDate = calendarFactory.getSQLString(testDate);
		
		sql = sql + " AND start_date <= '" + sqlTestDate + "' \n" + 
					" AND maturity_date > '" + sqlTestDate + "' \n";
		
		return sql;
	}
	
	
	
    public static Table runSql(Session session, final String sql) {
       
        
        IOFactory iof = session.getIOFactory();
        
        PluginLog.debug("About to run SQL. \n" + sql);
        
        
        Table storageDeals = null;
        try {
            storageDeals = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            PluginLog.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        
        return storageDeals;       
    }
    
	private static String baseSql(String location, String metal) {
		
		// Filter for location
		String filterLocationSQL = "";
		if (location != null && location.length() > 0) {
			if (!"*".equalsIgnoreCase(location)){
				if (location.indexOf("'")==0){
					filterLocationSQL = " AND location_name IN (" + location + ")";
				} else {
					filterLocationSQL = " AND location_name = '" + location + "'";
				}
			}
		}
		
		// Filter for metal
		String filterMetalSQL = "";
		if (metal != null && metal.length() > 0) {
			if (!"*".equalsIgnoreCase(metal)){
				if (metal.indexOf("'")==0){
					filterMetalSQL = " AND isg.name IN (" + metal + ")";
				} else {
					filterMetalSQL = " AND isg.name = '" + metal + "'";
				}				
			}
		}
		String sql = " SELECT DISTINCT deal_tracking_num, tran_num, reference,  start_date, maturity_date,\n" + 
					 " gppv.location_id, location_name, gppv.idx_subgroup, isg.name AS metal_name \n" +
					 " FROM ab_tran ab \n" +
					 " JOIN gas_phys_param_view gppv ON ab.ins_num = gppv.ins_num \n" + 
					 " JOIN gas_phys_location ghl ON (ghl.location_id = gppv.location_id " + filterLocationSQL + ")\n" +
					 " JOIN idx_subgroup isg ON (isg.id_number = gppv.idx_subgroup "  +  filterMetalSQL + ")\n" +
					 " WHERE ins_type = 48030 \n" +  // COMM-STOR	
					 " AND  tran_status = 3 \n"; // only validated trades	

		// " AND  deal_tracking_num NOT IN (401386, 401387)"; // exclude these trades	
		return sql;
	}
	
	
}
