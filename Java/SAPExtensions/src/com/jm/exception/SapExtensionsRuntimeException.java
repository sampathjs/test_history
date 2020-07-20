package com.jm.exception;

import com.olf.jm.logging.Logging;

public class SapExtensionsRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	public SapExtensionsRuntimeException(String message)
	{
		super(message);

		Logging.info(message);
		Logging.error(message);
	}
	
	public SapExtensionsRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        Logging.info(message);
        Logging.error(message);
    }

	public SapExtensionsRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		Logging.info(message);
		Logging.error(message);
		Logging.error(e.getMessage());
	}
}
