package com.olf.jm.metalutilisationstatement.reportbuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

public class CNYConversioAvgRate extends AbstractGenericScript {
	 private static SimpleDateFormat SDF1 = new SimpleDateFormat("dd-MMM-yyyy");
	    private static SimpleDateFormat SDF2 = new SimpleDateFormat("yyyyMMdd");
	    private static String INDEX_NAME = "FX_USD.CNY";
	
	
	 public Table execute(Session session, ConstTable table) {
	        try {
	        	
	        	Table parameters = table.getTable("PluginParameters", 0);
	        	// Get start/end date range
	            Calendar start = getDateParameter(parameters, "start_date");
	            Calendar end = getDateParameter(parameters, "end_date");
	            return this.getRatesTable(start, end, session);
	        }
	        catch (RuntimeException | ParseException e) {
	            Logging.error("", e);
	            if (e instanceof RuntimeException) {
	                throw (RuntimeException) e;
	            }
	            throw new RuntimeException(e);
	        }
	        finally {
	            Logging.close();
	        }
	    }

	 private Calendar getDateParameter(Table parameters, String parameterName) throws ParseException {
	        Calendar date = Calendar.getInstance();
	        int row = parameters.find(parameters.getColumnId("parameter_name"), parameterName, 0);
	        if (row >= 0) {
	            try {
	                date.setTime(SDF1.parse(parameters.getString("parameter_value", row)));
	            }
	            catch (ParseException e) {
	                date.setTime(SDF2.parse(parameters.getString("parameter_value", row)));
	            }
	        }
	        else
	        {
	        	Logging.info("There is no value set for  "+parameterName+" in the parameter table. Defaulting to current date");
	        }
	        return date;
	    }
	 
	 public Table getRatesTable(Calendar start, Calendar end, Session session)
	 {
		 String sql = "SELECT "+SDF1.format(start.getTime())+" start_date ,idx_def.index_name, "
		 		+ "idx_hist.index_id,  "
		 		+ "Avg(idx_hist.price)avg_price, "
					+ "ref_source.name as source "
			+ "FROM   idx_historical_prices idx_hist  "
			       + "JOIN idx_def  "
			       + "ON idx_def.index_id = idx_hist.index_id  "
			       + "AND idx_def.index_name = '"+INDEX_NAME+"'  "
				   + "JOIN ref_source  "
				   + "ON ref_source.id_number = idx_hist.ref_source	 "		
			+ "WHERE  idx_hist.reset_date >= '"+SDF1.format(start.getTime())+"'  "
			       + "AND idx_hist.reset_date < '"+SDF1.format(end.getTime())+"'  "
			       + "GROUP  BY idx_hist.index_id,  "
			          + "idx_def.index_name, "
					  + "ef_source.name ";
		 Logging.info("Sql "+sql);
		 Table result = session.getIOFactory().runSQL(sql);
		 return result;
	 }
}
