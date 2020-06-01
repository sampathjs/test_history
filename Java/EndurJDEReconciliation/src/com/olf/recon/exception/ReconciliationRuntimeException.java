package com.olf.recon.exception;

import com.olf.jm.logging.Logging;

public class ReconciliationRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	public ReconciliationRuntimeException(String message)
	{
		super(message);

		Logging.info(message);
		Logging.error(message);
	}
	
	public ReconciliationRuntimeException(String message, Throwable throwable) 
	{
        super(message, throwable);
        
        Logging.info(message);
        Logging.error(message);
    }

	public ReconciliationRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		Logging.info(message);
		Logging.error(message);
		Logging.error(e.getMessage());
	}
}
