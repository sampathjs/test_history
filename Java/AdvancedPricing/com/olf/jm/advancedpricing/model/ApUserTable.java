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
public enum ApUserTable {
	USER_TABLE_ADVANCED_PRICING_LINK("USER_jm_ap_buy_sell_link"),
	USER_TABLE_ADVANCED_PRICING_SELL_DEALS("USER_jm_ap_sell_deals"),
	USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS("USER_jm_ap_buy_dispatch_deals")
	;
	
	private final String name;
	
	private ApUserTable (String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
