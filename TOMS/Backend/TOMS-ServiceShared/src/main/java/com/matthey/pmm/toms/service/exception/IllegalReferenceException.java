package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Exception indicating a wrong reference type has been used in a method, e.g.
 * {@link DefaultReferenceType#OrderType} while querying for a party.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalReferenceException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedReference;
	private final String expectedReference; 
	
	public IllegalReferenceException (Class clazz, String method,
			String parameter, String expectedReference, String providedReference) {
		super (HttpStatus.BAD_REQUEST, "Illegal Reference provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' expects type '" + 
				expectedReference + "' but a value of reference '" + providedReference + "' was provided."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.providedReference = providedReference;
		this.expectedReference = expectedReference;
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

	public String getProvidedReference() {
		return providedReference;
	}

	public String getExpectedReference() {
		return expectedReference;
	}
}
