package com.matthey.testutil.exception;

import com.olf.jm.logging.Logging;

/**
 * Customised run time exception
 * @author SharmV04
 */
public class SapTestUtilRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param message
	 */
	public SapTestUtilRuntimeException(String message)
	{
		super(message);

		Logging.info(message);
		Logging.error(message);
	}
	
	/**
	 * @param message
	 * @param throwable
	 */
	public SapTestUtilRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        Logging.info(message);
        Logging.error(message);
    }

	/**
	 * @param message
	 * @param e
	 */
	public SapTestUtilRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		Logging.info(message);
		Logging.error(message);
		Logging.error(e.getMessage());
	}
}
