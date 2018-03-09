package com.matthey.openlink.utilities;

import java.util.concurrent.TimeUnit;

import com.olf.embedded.application.Context;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

/**
 * The Class encapsulates all our data access requests
 * @version $Revision: $
 */
public class DataAccess {  

	/**
	 * Gets the data from table.
	 *
	 * @param session the session
	 * @param sqlToExecute the sql to execute
	 * @return the data from table
	 */
	public static Table getDataFromTable(final Session session, final String sqlToExecute) {
		
		long started = System.nanoTime();
		Table userControl = null;
		try {
			Logger.log(LogLevel.DEBUG, 
					LogCategory.General, 
					DataAccess.class, 
					String.format("Execute SQL>%s<",sqlToExecute));
			userControl = session.getIOFactory().runSQL(sqlToExecute);
			
		} catch (Exception e) {
			Logger.log(LogLevel.ERROR, 
					LogCategory.General, 
					DataAccess.class, 
					String.format("Execute Error: SQL>%s<\nReason:%s",
							sqlToExecute, e.getLocalizedMessage()), e);
			throw new DataAccessException(String.format("Error executing the following request:%s",sqlToExecute),e);
		} finally {
			Logger.log(LogLevel.DEBUG, LogCategory.General, DataAccess.class, String.format("%s FINISHED in %dms", DataAccess.class, TimeUnit.MILLISECONDS.convert(System.nanoTime()-started, TimeUnit.NANOSECONDS)));			
		}
		return userControl;
	}

 
}
