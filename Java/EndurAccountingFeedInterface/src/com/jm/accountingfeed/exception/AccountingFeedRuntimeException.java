package com.jm.accountingfeed.exception;

import com.olf.jm.logging.Logging;

public class AccountingFeedRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	public AccountingFeedRuntimeException(String message)
	{
		super(message);

		Logging.info(message);
		Logging.error(message);
	}
	
	public AccountingFeedRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        Logging.info(message);
        Logging.error(message);
    }

	public AccountingFeedRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		Logging.info(message);
		Logging.error(message);
		Logging.error(e.getMessage());
	}
}
