

package com.olf.jm.SapInterface.util;



import com.olf.embedded.application.Context;

import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/**
 * The Class Utility. Utility functions used for 
 * running SQL. 
 */
public final class Utility {
	
	/**
	 * Hide constructor as class only contains static methods.
	 */
	public Utility(Context context) {
		Utility.context = context;
	}
	private static Context context = null;
	
	/**
	 * Helper method to run sql statements..
	 *
	 * @param sql the sql to execute
	 * @return the table containing the sql output
	 */
	public static Table runSql(final String sql) {
		
		IOFactory iof = context.getIOFactory();
	   
		Logging.debug("About to run SQL. \n" + sql);
		
		
		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
				
		return t;
		
	}
}
