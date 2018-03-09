package com.olf.jm.sapTransfer.strategy;


/**
 * The Class StrategyValidationException. Exception class thrown during the validation of the strategy data. 
 */
public class StrategyValidationException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6098620155419486042L;

	/**
	 * Instantiates a new strategy validation exception.
	 *
	 * @param errorMessage the error message
	 */
	public StrategyValidationException(final String errorMessage) { 
		super(errorMessage);
	}
}
