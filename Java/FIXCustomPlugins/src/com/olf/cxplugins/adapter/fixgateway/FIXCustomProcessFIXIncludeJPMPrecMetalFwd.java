package com.olf.cxplugins.adapter.fixgateway;

import com.olf.jm.fixGateway.jpm.messageProcessor.PrecMetalFwdMsgProcessor;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessor;

/**
 * {@link GuardedCustomFixProcessFIXInclude} for JPM Execute messages containing 
 * precious metal FX forward messages. 
 * 
 * @author jwaechter
 * @version 1.0
 */
public class FIXCustomProcessFIXIncludeJPMPrecMetalFwd extends FIXCustomProcessFIXIncludeJPM {
	/**
	 * The expected value of column {@link #SECURITY_SUB_TYPE_COL_NAME} that has to be set 
	 * for {@link FIXCustomProcessFIXIncludeJPMPrecMetalFwd} to process the incoming message.
	 */
	private static final String EXPECTED_SECURITY_SUB_TYPE = "FXFORWARD";
	   
	public String getExpectedSecuritySubtype() {
	   return EXPECTED_SECURITY_SUB_TYPE; 
	}

	public MessageProcessor getMessageProcessor() {
	   return new PrecMetalFwdMsgProcessor();
    }
}
