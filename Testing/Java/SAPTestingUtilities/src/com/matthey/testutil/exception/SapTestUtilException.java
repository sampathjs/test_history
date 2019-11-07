package com.matthey.testutil.exception;

import com.openlink.util.logging.PluginLog;

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

		PluginLog.info(message);
		PluginLog.error(message);
	}
	
	/**
	 * @param message
	 * @param throwable
	 */
	public SapTestUtilException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        PluginLog.info(message);
        PluginLog.error(message);
    }

	/**
	 * @param message
	 * @param e
	 */
	public SapTestUtilException(String message, Exception e) 
	{
		super(message, e);
		
		PluginLog.info(message);
		PluginLog.error(message);
		PluginLog.error(e.getMessage());
	}
}
