package com.olf.jm.fixGateway.jpm.messageAcceptor;


/*
 * History:
 * 2020-05-11 - V0.1 - jwaechter - Initial Version 
 */


/**
 * Test if the JPM Execute message containing a precious metal FX spot deal is valid and should be processed.
 * 
 * Skip order cancelation and rejection messages
 * Skip spread orders
 */
public class PrecMetalSpotMsgAcceptor extends JpmExecuteMessageAcceptor {
	private static final String EXPECTED_SECURITY_SUBTYPE = "FXSPOT";
	
	@Override
	public String getExpectedSecuritySubtype() {
		return EXPECTED_SECURITY_SUBTYPE;
	}
}
