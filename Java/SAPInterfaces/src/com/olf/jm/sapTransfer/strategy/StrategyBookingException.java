package com.olf.jm.sapTransfer.strategy;


/**
 * The Class StrategyBookingException. Exception thrown if errors occure during booking of strategy deals.
 */
public class StrategyBookingException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6098620155419486042L;

	/**
	 * Instantiates a new strategy booking exception.
	 *
	 * @param errorMessage the error message
	 */
	public StrategyBookingException(final String errorMessage) { 
		super(errorMessage);
	}
}
