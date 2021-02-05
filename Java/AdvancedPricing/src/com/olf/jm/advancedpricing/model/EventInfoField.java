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
public enum EventInfoField {
	MATCHED_DEAL_NUM("Matched Deal Num"),
	MATCHED_POSITION("Matched Position"), 
	METAL_VALUE_DATE("Metal Value Date")
	;
	
	private final String name;
	
	private EventInfoField (String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
