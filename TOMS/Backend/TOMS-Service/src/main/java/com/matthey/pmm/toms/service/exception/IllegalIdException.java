package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating an invalid ID has been provided
 * the expected format.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalIdException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedId;
	private final String expectedId; 
	
	public IllegalIdException (Class clazz, String method,
			String parameter, String expectedId, String providedId) {
		super (HttpStatus.BAD_REQUEST, "Illegal ID value provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' expects ID '" + 
			expectedId + "' but the ID '" + providedId + "' was provided."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.expectedId = expectedId;
		this.providedId = providedId;
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

	public String getProvidedId() {
		return providedId;
	}

	public String getExpectedId() {
		return expectedId;
	}
}
