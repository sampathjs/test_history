package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating an invalid ID has been provided
 * the expected format.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalSortColumnException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedPropertyName;
	private final String allowedPropertyNames; 
	
	public IllegalSortColumnException (Class clazz, String method,
			String parameter, String providedPropertyName, String allowedPropertyNames) {
		super (HttpStatus.BAD_REQUEST, "Illegal sort property name provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' expects one of the following '" + 
			allowedPropertyNames + "' but the property name '" + providedPropertyName + "' was provided."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.providedPropertyName = providedPropertyName;
		this.allowedPropertyNames = allowedPropertyNames;
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

	public String getProvidedPropertyName() {
		return providedPropertyName;
	}

	public String getAllowedPropertyNames() {
		return allowedPropertyNames;
	}
}
