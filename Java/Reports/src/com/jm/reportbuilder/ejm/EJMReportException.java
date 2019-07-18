package com.jm.reportbuilder.ejm;

public class EJMReportException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new reporting runtime exception.
	 * 
	 * @param message the message
	 */
	public EJMReportException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new reporting runtime exception.
	 * 
	 * @param exception the exception
	 */
	public EJMReportException(Exception exception) {
		super(exception);
	}

	/**
	 * Instantiates a new reporting runtime exception.
	 * 
	 * @param message the message
	 * @param exception the exception
	 */
	public EJMReportException(String message, Exception exception) {
		super(message, exception);
	}
}
