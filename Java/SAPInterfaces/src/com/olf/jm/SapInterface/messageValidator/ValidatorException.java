
package com.olf.jm.SapInterface.messageValidator;


/**
 * The Class ValidatorException representing errors during field validation.
 */
public class ValidatorException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Instantiates a new exception.
	 * 
	 * @param errorMessage
	 *            the error message
	 */
	public ValidatorException(final String errorMessage) {
		super(errorMessage);
	}

}
