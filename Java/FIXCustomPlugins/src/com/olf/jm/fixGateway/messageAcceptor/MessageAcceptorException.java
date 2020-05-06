package com.olf.jm.fixGateway.messageAcceptor;

public class MessageAcceptorException extends Exception {

	/*
	 * History:
	 * 2017-10-10 - V0.1 - scurran - Initial Version
	 */
	
	private static final long serialVersionUID = -8020791000613476072L;

	public MessageAcceptorException(String errorMessage ) {
		super(errorMessage);
	}
}
