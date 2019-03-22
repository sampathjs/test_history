package com.jm.rbreports.BalanceSheet;

public class BalanceLineAccountsCN extends BalanceLineAccountsUK {
	private static final String SUBCONTEXT = "AccountBalanceReport - CN";
	
	@Override
	protected String getSubcontext() {
		return SUBCONTEXT;
	}
}
