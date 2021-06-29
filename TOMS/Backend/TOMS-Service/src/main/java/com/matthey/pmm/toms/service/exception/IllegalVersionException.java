package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating an invalid ID has been provided
 * the expected format.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalVersionException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedVersion;
	private final String expectedVersion;
	
	public IllegalVersionException (Class clazz, String method,
			String parameter, String expectedVersion, String providedVersion) {
		super (HttpStatus.BAD_REQUEST, "Illegal version provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' expects version '" + 
			expectedVersion + "' but the version '" + providedVersion + "' was provided. This is usually caused by concurrent modifications to the data instance."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.expectedVersion = expectedVersion;
		this.providedVersion = providedVersion;
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
		return providedVersion;
	}

	public String getExpectedValue() {
		return expectedVersion;
	}
}
