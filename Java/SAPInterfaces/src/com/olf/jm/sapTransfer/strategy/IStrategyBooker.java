package com.olf.jm.sapTransfer.strategy;

import com.olf.openrisk.table.Table;


/**
 * The Interface IStrategyBooker. Interface to classes that can book strategy deal.
 */
public interface IStrategyBooker {

	/**
	 * Validate. Validates the data being used to book the strategy
	 *
	 * @throws StrategyValidationException the strategy validation exception
	 */
	void validate() throws StrategyValidationException;
	
	/**
	 * Book. Books a new strategy deal.
	 *
	 * @return the int the deal tracking number
	 * 
	 * @throws StrategyBookingException the strategy booking exception
	 */
	int book() throws StrategyBookingException;
	
	/**
	 * Gets the response message.
	 *
	 * @return the response message
	 * @throws StrategyBookingException the strategy booking exception
	 */
	Table getResponseMessage() throws StrategyBookingException;
}
