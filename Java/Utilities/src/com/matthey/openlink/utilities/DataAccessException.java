package com.matthey.openlink.utilities;

/**
 * This isolates all our DataAccess exception handling
 * @version $Revision: $
 */
public class DataAccessException extends RuntimeException {

	public DataAccessException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 */
	public DataAccessException(String arg0) {
		super(arg0);
	}

}
