package com.olf.jm.fixGateway.messageMapper;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */

public class MessageMapperException extends Exception {

	private static final long serialVersionUID = 6877844069753665375L;
	
	public MessageMapperException() {
		super();
	}

	public MessageMapperException(String errorMessage) {
		super(errorMessage);
	}
}
