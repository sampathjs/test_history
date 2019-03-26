package com.olf.jm.metalutilisationstatement.reportbuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

public class CNYConversionAvgRate extends AbstractGenericScript {
	 private static SimpleDateFormat SDF1 = new SimpleDateFormat("dd-MMM-yyyy");
	    private static SimpleDateFormat SDF2 = new SimpleDateFormat("yyyyMMdd");
	    private static String INDEX_NAME = "FX_USD.CNY";
	    private static String REF_SOURCE_NAME = "BOC";
	    private String indexName="", source="";
	    private double avgPrice;
	
	
	 public Table execute(Session session, ConstTable table) {
	        try {
	        	
	        	Logging.init(session, getClass(), "Metals Utilisation Statement", "CNY conversion rate");
	        	Table parameters = table.getTable("PluginParameters", 0);
	        	// Get start/end date range
	            Calendar start = getDateParameter(parameters, "start_date");
	            Calendar end = getDateParameter(parameters, "end_date");
	            Table account = this.getAccountTable(session);
	            Table rates = this.getRatesTable(start, end, session);
	            //account.select(rates, "index_name,avg_price,source","[In.avg_price] > 0");
	            account.addColumn("index_name", EnumColType.String);
	            account.addColumn("avg_price", EnumColType.Double);
	            account.addColumn("source", EnumColType.String);
	            account.addColumn("toztogms", EnumColType.Double);
	            
	            int rowCounter = account.getRowCount();
	            for (int i = 0; i<rowCounter; i++)
	            {
	            	account.setString("index_name", i, this.indexName);
	            	account.setDouble("avg_price", i, this.avgPrice);
	            	account.setString("source", i, this.source);
	            	account.setDouble("toztogms", i, 31.103476800);
	            }
	            
	            return account;
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
	        return date;
	    }
	 
	 public Table getRatesTable(Calendar start, Calendar end, Session session)
	 {
		 String sql = "SELECT idx_def.index_name, "
		 		+ "idx_hist.index_id,  "
		 		+ "Avg(idx_hist.price)avg_price, "
					+ "ref_source.name as source "
			+ "FROM   idx_historical_prices idx_hist  "
			       + "JOIN idx_def  "
			       + "ON idx_def.index_id = idx_hist.index_id  "
			       + "AND idx_def.index_name = '"+INDEX_NAME+"'  AND idx_def.db_status = 1 "
				   + "JOIN ref_source  "
				   + "ON ref_source.id_number = idx_hist.ref_source  and ref_source.name= '"+REF_SOURCE_NAME+"'	 "		
			+ "WHERE  idx_hist.reset_date >= '"+SDF1.format(start.getTime())+"'  "
			       + "AND idx_hist.reset_date <= '"+SDF1.format(end.getTime())+"'  "
			       + "GROUP  BY idx_hist.index_id,  "
			          + "idx_def.index_name, "
					  + "ref_source.name ";
		 Logging.info("Sql "+sql);
		 Table result = session.getIOFactory().runSQL(sql);
		 if((result != null) && (result.getRowCount()>0))
		 {
			 this.indexName = result.getString("index_name", 0);
         	this.avgPrice = result.getDouble("avg_price", 0);
         	this.source = result.getString("source", 0);
         	
		 }
		 return result;
	 }
	 
	 public Table getAccountTable ( Session session )
	 {
		 String sql = "select account_id from account";
		 Logging.info("SQL  "+sql);
		 Table result = session. getIOFactory().runSQL(sql);
		 return result;
	 }
}
