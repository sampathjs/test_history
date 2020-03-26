package com.olf.jm.fixGateway.messageProcessor;

import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptorException;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface MessageProcessor.
 */
public interface MessageProcessor {

	/**
	 * Process the fix message building the trade builder table.
	 *
	 * @param message the fix message to process
	 * @return populated trade builder message
	 * @throws MessageMapperException the message mapper exception
	 */
	Table processMessage(Table message) throws MessageMapperException;
	
	/**
	 * Accept message. Check is this is a valid fix message that should be processed.
	 * 
	 * Message are skipped if the order status is not valid or the instrument is not supported.
	 * 
	 *
	 * @param message the fix message to check
	 * @return true, if message should be processed, false if the message should be skipped
	 * @throws MessageAcceptorException 
	 */
	boolean acceptMessage(Table message) throws MessageAcceptorException;
}
