package com.olf.jm.SapInterface.messageMapper;


/**
 * The Class MessageMapperException. Error during the field mapping.
 */
public class MessageMapperException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new message mapper exception.
	 *
	 * @param errorMessage the error message
	 */
	public MessageMapperException(final String errorMessage) {
		super(errorMessage);
	}
}
