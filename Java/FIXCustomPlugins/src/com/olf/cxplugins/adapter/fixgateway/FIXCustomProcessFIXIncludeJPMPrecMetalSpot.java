package com.olf.cxplugins.adapter.fixgateway;

import com.olf.jm.fixGateway.jpm.messageProcessor.PrecMetalSpotMsgProcessor;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessor;

/**
 * {@link GuardedCustomFixProcessFIXInclude} for JPM Execute messages containing 
 * precious metal FX spot messages. 
 * 
 * @author jwaechter
 * @version 1.0
 */
public class FIXCustomProcessFIXIncludeJPMPrecMetalSpot extends FIXCustomProcessFIXIncludeJPM {
	/**
	 * The expected value of column {@link #SECURITY_SUB_TYPE_COL_NAME} that has to be set 
	 * for {@link FIXCustomProcessFIXIncludeJPMPrecMetalSpot} to process the incoming message.
	 */
	private static final String EXPECTED_SECURITY_SUB_TYPE = "FXSPOT";
		
    public String getExpectedSecuritySubtype() {
	   return EXPECTED_SECURITY_SUB_TYPE; 
	}
    
    public MessageProcessor getMessageProcessor() {
 	   return new PrecMetalSpotMsgProcessor();
    }
}
