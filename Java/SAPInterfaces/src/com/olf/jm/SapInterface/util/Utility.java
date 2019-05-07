

package com.olf.jm.SapInterface.util;



import com.olf.embedded.application.Context;

import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/**
 * The Class Utility. Utility functions used for 
 * running SQL. 
 */
public final class Utility {
	
	/**
	 * Hide constructor as class only contains static methods.
	 */
	public Utility(Context context) {
		this.context = context;
	}
	private Context context = null;
	
	/**
	 * Helper method to run sql statements..
	 *
	 * @param sql the sql to execute
	 * @return the table containing the sql output
	 */
	public Table runSql(final String sql) {
		
		IOFactory iof = context.getIOFactory();
	   
		PluginLog.debug("About to run SQL. \n" + sql);
		
		
		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
				
		return t;
		
	}
}
