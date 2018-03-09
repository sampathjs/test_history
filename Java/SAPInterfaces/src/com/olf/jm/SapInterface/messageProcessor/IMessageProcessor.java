package com.olf.jm.SapInterface.messageProcessor;

import com.olf.embedded.connex.Request;
import com.olf.embedded.connex.RequestData;
import com.olf.embedded.connex.RequestOutput;


/**
 * The Interface IMessageProcessor.
 */
public interface IMessageProcessor {

	/**
	 * Process request message.
	 *
	 * @param request the request
	 * @param requestData the request data
	 */
	void processRequestMessage(Request request, RequestData requestData);
	
	
	/**
	 * Process response message.
	 *
	 * @param request the request
	 * @param requestOutput the request output
	 */
	void processResponseMessage(Request request, RequestOutput requestOutput);
}
