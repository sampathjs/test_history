package com.olf.jm.fixGateway.jpm.messageAcceptor;

/*
 * History:
 * 2020-05-14 - V0.1 - jwaechter - Initial Version 
 */


/**
 * Test if the JPM Execute message containing a precious metal FX forward deal is valid and should be processed.
 * 
 * Skip order cancelation and rejection messages
 */
public class PrecMetalFwdMsgAcceptor extends JpmExecuteMessageAcceptor {
	private static final String EXPECTED_SECURITY_SUBTYPE = "FXFORWARD";

	@Override
	public String getExpectedSecuritySubtype() {
		return EXPECTED_SECURITY_SUBTYPE;
	}
}
