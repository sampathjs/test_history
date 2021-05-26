package com.olf.jm.fixGateway.jpm.messageAcceptor;


/*
 * History:
 * 2020-05-15 - V0.1 - jwaechter - Initial Version 
 */


/**
 * Test if the JPM Execute message containing a base metal bullet swap deal is valid and should be processed.
 * 
 * Skip order cancellation and rejection messages
 */
public class BaseMetalSwapMsgAcceptor extends JpmExecuteMessageAcceptor {
	static final String EXPECTED_SECURITY_SUBTYPE = "BULLET SWAP STRIP";
	
	@Override
	public String getExpectedSecuritySubtype() {
		return EXPECTED_SECURITY_SUBTYPE;
	}
}
