package com.jm.accountingfeed.exception;

import com.openlink.util.logging.PluginLog;

public class AccountingFeedRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	public AccountingFeedRuntimeException(String message)
	{
		super(message);

		PluginLog.info(message);
		PluginLog.error(message);
	}
	
	public AccountingFeedRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        PluginLog.info(message);
        PluginLog.error(message);
    }

	public AccountingFeedRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		PluginLog.info(message);
		PluginLog.error(message);
		PluginLog.error(e.getMessage());
	}
}
