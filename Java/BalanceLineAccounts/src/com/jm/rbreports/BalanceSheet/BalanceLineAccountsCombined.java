package com.jm.rbreports.BalanceSheet;

/*
 * History:
 * 2016-07-19	V1.0	jwaechter	- Initial version 
 */

/**
 * Another version of the BalanceLineAccounts report that is using a different user table and 
 * account info field. Unfortunately it's not possible to pass over parameters to the
 * drill down report, for that reason we have to create two different classes and data sources.
 * @author jwaechter
 * @version 1.0
 */
public class BalanceLineAccountsCombined extends BalanceLineAccountsUK {
	private static final String SUBCONTEXT = "AccountBalanceReport - Combined";
	
	@Override
	protected String getSubcontext() {
		return SUBCONTEXT;
	}
}
