/*
 * File updated 05/02/2021, 17:53
 */

package com.olf.jm.advancedpricing.model;

/*
 * History:
 * 2017-07-04	V1.0	sma	- initial version
 */

/**
 * Enum containing relevant tran info fields (on tran level/tran leg level)
 * @author sma
 * @version 1.0
 */
public enum TranInfoField {
	PRICING_TYPE("Pricing Type"),
	MATCHED_STATUS("Matched Status"),
	DP_PRICE("DP Price"), //Param info
	NOTNL_DP_SWAP("NotnldpSwap"), //Param info
	MATCHED_DEAL_NUM("Matched Deal Num"), //Param info
	TRADE_PRICE("Trade Price");
	;
	
	private final String name;
	
	private TranInfoField (String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
