package com.olf.jm.receiptworkflow.model;

public class FieldValidationException extends RuntimeException {
	private final String messageToUser;
	
	public FieldValidationException (final String messageToUser) {
		this.messageToUser = messageToUser;
	}

	public String getMessageToUser() {
		return messageToUser;
	}
}
