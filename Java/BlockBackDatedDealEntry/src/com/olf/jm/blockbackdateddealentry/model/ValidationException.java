package com.olf.jm.blockbackdateddealentry.model;

public class ValidationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9159728199950127805L;

	public ValidationException(String errorMessage) {
		super(errorMessage);
	}
}
