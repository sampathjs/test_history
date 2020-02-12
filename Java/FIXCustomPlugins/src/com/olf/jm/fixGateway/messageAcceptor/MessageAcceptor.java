package com.olf.jm.fixGateway.messageAcceptor;

import com.olf.openjvs.Table;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * The Interface MessageAcceptor. Test is a message is valid for processing or should be skipped. 
 * Allows unsupported messages to be ignored without generating an error
 */
public interface MessageAcceptor {

	/**
	 * Accept message. Test if the message should be procesed or skipped
	 *
	 * @param message the inbound message to test
	 * @return true, if message should be processed false otherwise
	 * @throws MessageAcceptorException the message is not valid or contains errors
	 */
	boolean acceptMessage(Table message) throws MessageAcceptorException;
}
