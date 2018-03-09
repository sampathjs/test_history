package com.olf.jm.SapInterface.messageMapper.RefMapping;

/**
 * The Class RefMapException. Represents an error during the application of the reference mappings.
 */
public class RefMapException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 7436870920165666454L;

	/**
	 * Instantiates a new ref map exception.
	 *
	 * @param errorMessage the error message
	 */
	public RefMapException(final String errorMessage) {
		super(errorMessage);
	}
}
