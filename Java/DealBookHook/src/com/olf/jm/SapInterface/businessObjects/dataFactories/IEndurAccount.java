package com.olf.jm.SapInterface.businessObjects.dataFactories;


/**
 * The Interface IEndurAccount.
 */
public interface IEndurAccount {

	/**
	 * Gets the account business unit.
	 *
	 * @return the account business unit
	 */
	String getAccountBusinessUnit();
	
	/**
	 * Gets the account name.
	 *
	 * @return the account name
	 */
	String getAccountName();
	
	/**
	 * Gets the company code.
	 *
	 * @return the company code
	 */
	String getCompanyCode();
	
	/**
	 * Gets the segment.
	 *
	 * @return the segment
	 */
	String getSegment();

	/**
	 * Gets the account number.
	 *
	 * @return the account number
	 */
	String getAccountNumber();
	
	/**
	 * Gets the form associated with the account.
	 *
	 * @return the account form
	 */
	String getAccountForm();
	
	/**
	 * Gets the loco associated with the account.
	 *
	 * @return the account loco
	 */
	String getAccountLoco();
	
}
