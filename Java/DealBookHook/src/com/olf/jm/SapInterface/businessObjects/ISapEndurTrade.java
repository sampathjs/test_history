package com.olf.jm.SapInterface.businessObjects;

import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;

/**
 * The Interface ISapEndurTrade.
 */
public interface ISapEndurTrade {

	/**
	 * Gets the toolset.
	 *
	 * @return the toolset
	 */
	EnumToolset  getToolset();

	/**
	 * Gets the deal tracking number.
	 *
	 * @return the deal tracking number
	 */
	int getDealTrackingNumber();

	/**
	 * Gets the trade status.
	 *
	 * @return the trade status
	 */
	EnumTranStatus getTradeStatus();

	/**
	 * Checks if a transaction loaded from the database is a valid transaction. Encapsulates
	 * the selection of the data based  on the instrument type.
	 *
	 * @return true, if is valid
	 */
	boolean isValid();

}
