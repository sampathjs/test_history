package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.matthey.pmm.toms.enums.DefaultReferenceType;

/**
 * Exception indicating an invalid ID has been provided
 * the expected format.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalValueException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedValue;
	private final String expectedValue; 
	
	public IllegalValueException (Class clazz, String method,
			String parameter, String expectedValue, String providedValue) {
		super (HttpStatus.BAD_REQUEST, "Illegal value provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' expects value '" + 
			expectedValue + "' but the value '" + providedValue + "' was provided."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.expectedValue = expectedValue;
		this.providedValue = providedValue;
	}

	public Class getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	public String getParameter() {
		return parameter;
	}

	public String getProvidedValue() {
		return providedValue;
	}

	public String getExpectedValue() {
		return expectedValue;
	}
}
