package com.olf.recon.exception;

import com.openlink.util.logging.PluginLog;

public class ReconciliationRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	public ReconciliationRuntimeException(String message)
	{
		super(message);

		PluginLog.info(message);
		PluginLog.error(message);
	}
	
	public ReconciliationRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        PluginLog.info(message);
        PluginLog.error(message);
    }

	public ReconciliationRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		PluginLog.info(message);
		PluginLog.error(message);
		PluginLog.error(e.getMessage());
	}
}
