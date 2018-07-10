package com.olf.jm.SapInterface.messageValidator;

public class SqlInjectionException extends Exception {

	public SqlInjectionException(String errorMessage) {
		super(errorMessage);
	}

}
