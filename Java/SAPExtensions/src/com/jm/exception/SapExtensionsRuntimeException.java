package com.jm.exception;

import com.openlink.util.logging.PluginLog;

public class SapExtensionsRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	public SapExtensionsRuntimeException(String message)
	{
		super(message);

		PluginLog.info(message);
		PluginLog.error(message);
	}
	
	public SapExtensionsRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        PluginLog.info(message);
        PluginLog.error(message);
    }

	public SapExtensionsRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		PluginLog.info(message);
		PluginLog.error(message);
		PluginLog.error(e.getMessage());
	}
}
