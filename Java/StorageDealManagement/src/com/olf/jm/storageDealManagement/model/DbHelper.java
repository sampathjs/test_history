package com.olf.jm.storageDealManagement.model;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

public class DbHelper {
	
	public static String getLinkedReceiptBatchesSql(int dealTrackingNum) {
		return    " SELECT distinct csdc_i.delivery_id as delivery_id, csh_i.location_id as location_id, csdc_i.batch_id \n" 
				+ " FROM comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i, ab_tran ab_i \n" 
				+ " WHERE  csh_i.delivery_id = csdc_i.delivery_id \n"
				+ " AND csh_i.bav_flag = 1 \n" 
				+ " AND csh_i.total_quantity > 1.0 \n"
				+ " AND csdc_i.batch_id > 0 \n"
				+ " AND ab_i.ins_num = csh_i.ins_num\n"
				+ " AND ab_i.deal_tracking_num = " + dealTrackingNum + "\n"
				+ " AND ab_i.current_flag = 1\n"
				+ " AND ab_i.ins_sub_type = 9204\n"
				+ " AND 0=(select count (*) \n"
				+ "        from comm_sched_deliv_deal csdd2 \n"
				+ "        where csdd2.delivery_id = csdc_i.delivery_id \n"
				+ "        and deal_num <> 6)\n"
				+ " AND 1=(select count (*) "
				+ "		   from comm_sched_deliv_deal csdd3 "
				+ "        where csdc_i.source_delivery_id <> csdc_i.delivery_id "
				+ "        and csdc_i.source_delivery_id <> 0 "
				+ "        and csdd3.delivery_id = csdc_i.source_delivery_id "
				+ "        and deal_num <> 6)";		
	}
	
	public static String getUnLinkedReceiptBatchesSql(int dealTrackingNum) {
		return 	  " select distinct csdc_i.source_delivery_id as delivery_id, csh_i.location_id as location_id, csdc_i.batch_id \n"
		 		+ " from comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i , ab_tran ab_i \n"
		 		+ " where csh_i.delivery_id = csdc_i.delivery_id \n"
		 		+ " and csh_i.bav_flag = 1 \n"
		 		+ " and csh_i.total_quantity > 1.0 \n"
		 		+ " and csdc_i.batch_id > 0 \n"
		 		+ " and ab_i.ins_num = csh_i.ins_num \n"
		 		+ " and ab_i.current_flag = 1 \n"
		 		+ " and ab_i.tran_status in (3) \n"
		 		+ " and ab_i.deal_tracking_num = " + dealTrackingNum + " \n" 
		 		+ " and ab_i.ins_sub_type = 9204 \n"
		 		+ " and 0=(select count (*) \n"
		 		+ "        from comm_sched_deliv_deal csdd2 \n"
		 		+ "        where csdd2.delivery_id = csdc_i.delivery_id \n"
		 		+ "        and deal_num <> 6) \n"
		 		+ " and 0=(select count (*) \n"
		 		+ "        from comm_sched_deliv_deal csdd3 \n"
		 		+ "        where csdc_i.source_delivery_id <> csdc_i.delivery_id \n"
		 		+ "        and csdc_i.source_delivery_id <> 0 \n"
		 		+ "        and csdd3.delivery_id = csdc_i.source_delivery_id \n"
		 		+ "        and deal_num <> 6)";
	}
	
	public static String buildSqlCommStoreMaturingOnDate( Session session, String location, String metal, Date maturityDate) {
		
		StringBuffer sql = baseSql(location, metal);
		
		CalendarFactory calendarFactory = session.getCalendarFactory();
		String sqlMaturityDate = calendarFactory.getSQLString(maturityDate);
		sql.append(" AND maturity_date = '").append(sqlMaturityDate).append("' \n");
		
		return sql.toString();
	}
	
	public static String buildSqlCommStoreAfterDate( Session session, String location, String metal, Date testDate) {
		
		StringBuffer sql = baseSql(location, metal);
		
		CalendarFactory calendarFactory = session.getCalendarFactory();
		String sqlTestDate = calendarFactory.getSQLString(testDate);
		
		//sql.append(" AND start_date >= '").append(sqlTestDate).append("' \n"); // SMC TODO 
		// start date <= test and maturity date >= test
		sql.append(" AND start_date <= '").append(sqlTestDate).append("' \n");
		sql.append(" AND maturity_date > '").append(sqlTestDate).append("' \n");
		
		return sql.toString();
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
    
	private static StringBuffer baseSql(String location, String metal) {
		StringBuffer sql = new StringBuffer();
		
		sql.append(" SELECT DISTINCT deal_tracking_num, tran_num, reference,  start_date, maturity_date,");
		sql.append(" gppv.location_id, location_name, gppv.idx_subgroup, isg.name AS metal_name \n");
		sql.append(" FROM ab_tran ab \n");
		sql.append(" JOIN gas_phys_param_view gppv ON ab.ins_num = gppv.ins_num \n");
		
		// Filter on location
		sql.append(" JOIN gas_phys_location ghl ON ghl.location_id = gppv.location_id ");
		if (location != null && location.length() > 0) {
			sql.append(" AND location_name = '").append(location).append("'");
			
		}
		sql.append(" \n");
		
		// Filter on metal
		sql.append(" JOIN idx_subgroup isg ON isg.id_number = gppv.idx_subgroup ");
		if (metal != null && metal.length() > 0) {
			sql.append(" AND isg.name = '").append(metal).append("'");
		}
		sql.append(" \n");
		
		sql.append(" WHERE ins_type = 48030 \n"); // COMM-STOR	
		sql.append(" AND  tran_status = 3 \n"); // only validated trades	
		return sql;
	}
	
	
}
