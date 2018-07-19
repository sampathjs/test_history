package com.olf.jm.SapInterface.businessObjects.dataFactories;


/**
 * The Interface IEndurParty. Party information used to book trade.
 */
public interface IEndurParty {

	/**
	 * The Enum EnumPartyType.
	 */
	enum EnumPartyType { 
		/** Enum for the internal party. */
		INTERNAL, 
		/** Enum for the external party. */
		EXTERNAL;
	}
	
	/**
	 * Gets the business unit.
	 *
	 * @return the business unit
	 */
	String getBusinessUnit();
	
	/**
	 * Gets the legal entity.
	 *
	 * @return the legal entity
	 */
	String getLegalEntity();
	
	/**
	 * Gets the protfolio.
	 *
	 * @return the protfolio
	 */
	String getPortfolio();
	
	/**
	 * Get input sap id used to load the data.
	 *
	 * @return the string
	 */
	String getInputSapId();
	
	
	/**
	 * Gets the party type indicating if the data is for 
	 * a internal or external party.
	 *
	 * @return the party type
	 */
	EnumPartyType getPartyType();
	

	/**
	 * Gets the settlement id.
	 *
	 * @return the settlement id
	 */
	int getSettlementId();
	
	/**
	 * Get the flag indicating if the account is included in the auto
	 * SI short list functionality.
	 *
	 * @return the use auto si short list flag
	 */
	String getUseAutoSIShortList();
	
	/**
	 * Gets the loco associated with the account selected.
	 *
	 * @return the account loco
	 */
	String getAccountLoco();
	
	
	/**
	 * Gets the form accociated to the account selected.
	 *
	 * @return the account form
	 */
	String getAccountForm();
	
}
