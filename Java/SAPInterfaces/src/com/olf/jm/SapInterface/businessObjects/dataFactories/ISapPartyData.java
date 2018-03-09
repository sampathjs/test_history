package com.olf.jm.SapInterface.businessObjects.dataFactories;

/**
 * The Interface ISapPartyData. Used to map party data between SAP and Endur.
 */
public interface ISapPartyData {
	
	/**
	 * Gets the Endur internal party details.
	 *
	 * @return the internal party
	 */
	IEndurParty getInternalParty();
	

	/**
	 * Gets the Endur external party details.
	 * @return the external party
	 */
	IEndurParty getExternalParty();


	/**
	 * Gets the to account. Used in metal transfers
	 *
	 * @return the to account
	 */
	IEndurAccount getToAccount();
	
	
	/**
	 * Gets the from account. Used in metal transfers
	 *
	 * @return the from account
	 */
	IEndurAccount getFromAccount();
	
	/**
	 * Gets the interface type.
	 *
	 * @return the interface type
	 */
	EnumInterfaceType getInterfaceType();
	
	
}
