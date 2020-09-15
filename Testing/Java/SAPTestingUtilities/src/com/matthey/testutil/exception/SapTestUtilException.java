package com.matthey.testutil.exception;

import com.olf.jm.logging.Logging;

/**
 * Customised checked exception for the project
 * @author SharmV04
 *
 */
public class SapTestUtilException extends Exception 
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param message
	 */
	public SapTestUtilException(String message)
	{
		super(message);

		Logging.info(message);
		Logging.error(message);
	}
	
	/**
	 * @param message
	 * @param throwable
	 */
	public SapTestUtilException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        Logging.info(message);
        Logging.error(message);
    }

	/**
	 * @param message
	 * @param e
	 */
	public SapTestUtilException(String message, Exception e) 
	{
		super(message, e);
		
		Logging.info(message);
		Logging.error(message);
		Logging.error(e.getMessage());
	}
}
