package com.jm.archivepurgeutilities.exception;

import com.olf.jm.logging.Logging;

/**
 * @author SharmV03
 *
 */
public class ArchivePurgeUtilitiesRuntimeException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param message
	 */
	public ArchivePurgeUtilitiesRuntimeException(String message)
	{
		super(message);

		Logging.error(message);
	}
	
	/**
	 * @param message
	 * @param e
	 */
	public ArchivePurgeUtilitiesRuntimeException(String message, Exception e) 
	{
		super(message, e);
		
		Logging.error(message);
		Logging.error(e.getMessage());
	}
	
	/**
	 * @param message
	 * @param e
	 */
	public ArchivePurgeUtilitiesRuntimeException(String message, Throwable e) 
	{
		super(message, e);
		
		Logging.error(message);
		Logging.error(e.getMessage());
	}
}
