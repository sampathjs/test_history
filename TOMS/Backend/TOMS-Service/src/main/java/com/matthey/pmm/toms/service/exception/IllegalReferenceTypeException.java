package com.matthey.pmm.toms.service.exception;

import com.matthey.pmm.toms.enums.DefaultReferenceType;

/**
 * Exception indicating a wrong reference type has been used in a method, e.g.
 * {@link DefaultReferenceType#OrderType} while querying for a party.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalReferenceTypeException extends RuntimeException {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedReferenceType;
	private final String expectedReferenceType; 
	
	public IllegalReferenceTypeException (Class clazz, String method,
			String parameter, String expectedReferenceType, String providedReferenceType) {
		super ("Illegal Reference Type provided calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'expects type '" + 
				expectedReferenceType + "' but a value of type '" + providedReferenceType + "' was provided."
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.providedReferenceType = providedReferenceType;
		this.expectedReferenceType = expectedReferenceType;
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

	public String getProvidedReferenceType() {
		return providedReferenceType;
	}

	public String getExpectedReferenceType() {
		return expectedReferenceType;
	}
}
