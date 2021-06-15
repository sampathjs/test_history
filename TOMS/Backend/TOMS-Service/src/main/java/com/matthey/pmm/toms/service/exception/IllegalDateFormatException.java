package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.matthey.pmm.toms.enums.DefaultReferenceType;

/**
 * Exception indicating a string that should denote a date  or datetime does not match
 * the expected format.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalDateFormatException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedText;
	private final String expectedFormat; 
	
	public IllegalDateFormatException (Class clazz, String method,
			String parameter, String expectedFormat, String providedText) {
		super (HttpStatus.BAD_REQUEST, "Illegal DateTime value provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' expects format '" + 
			expectedFormat + "' but the text '" + providedText + "' was provided."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.expectedFormat = expectedFormat;
		this.providedText = providedText;
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

	public String getProvidedText() {
		return providedText;
	}

	public String getExpectedFormat() {
		return expectedFormat;
	}
}
