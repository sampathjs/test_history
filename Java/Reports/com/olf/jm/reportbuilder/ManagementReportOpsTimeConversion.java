package com.olf.jm.reportbuilder;

import com.olf.jm.logging.Logging;
import com.olf.jm.reportbuilder.WorkingMinutesCalculator;
import java.sql.Timestamp;
import java.util.Date;    
import java.text.SimpleDateFormat;  
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBUserTable;

/*
 * History:
 * 2020-11-04	V1.0	dnagy	- Initial Version
 * Calculates business time difference for confirmation delays, i.e. only counting working hours and weekdays between the specified timestamps
 * Part of the Management Report 3 pack - Operational Reports
 */

public class ManagementReportOpsTimeConversion implements IScript {

	public void execute(IContainerContext context) throws OException {
	
        String user_table_name = "USER_MR3_BO_STATUS";

		Table dates = Table.tableNew();
		Table usertable = Table.tableNew(user_table_name);

        try {
			Logging.init(this.getClass(), "", "");
			Logging.info("Starting " + getClass().getSimpleName());
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date parsedDate = new Date();
			
			String sql = "select document_num, FORMAT(last_update,'yyyy-MM-dd HH:mm:ss') as last_update, FORMAT(last_tran_insert,'yyyy-MM-dd HH:mm:ss') as last_tran_insert, business_delay from " + user_table_name;
			DBase.runSqlFillTable(sql, dates);
			int rownum = dates.getNumRows();
			int workmin = 0;
			
			if (rownum > 0) {
				for (int i=1; i <= rownum; i++) {

					String start = dates.getString("last_tran_insert", i);
					String end = dates.getString("last_update", i);

					parsedDate = dateFormat.parse(start);
					Timestamp start_date =  new Timestamp(parsedDate.getTime());
					parsedDate = dateFormat.parse(end);
					Timestamp end_date =  new Timestamp(parsedDate.getTime());

					workmin = Math.max(WorkingMinutesCalculator.getWorkingMinutes(start_date, end_date),0);
					
					dates.setInt("business_delay", i, workmin);
				}
			}
			
			DBUserTable.load(usertable);
			usertable.select(dates, "business_delay", "document_num EQ $document_num");
            DBUserTable.clear(usertable);
			DBUserTable.bcpIn(usertable);
			
        } catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.info(message);
			for (StackTraceElement ste : e.getStackTrace() )  {
				Logging.error(ste.toString(), e);
			}
		} finally {

			dates.destroy();
			usertable.destroy();
				
			Logging.info("End " + getClass().getSimpleName());
			Logging.close();
		}

	}
}