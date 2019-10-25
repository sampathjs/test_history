package com.matthey.testutil.exception;

import com.openlink.util.logging.PluginLog;

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

		PluginLog.info(message);
		PluginLog.error(message);
	}
	
	/**
	 * @param message
	 * @param throwable
	 */
	public SapTestUtilRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        PluginLog.info(message);
        PluginLog.error(message);
    }

	/**
	 * @param message
	 * @param e
	 */
	public SapTestUtilRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		PluginLog.info(message);
		PluginLog.error(message);
		PluginLog.error(e.getMessage());
	}
}
