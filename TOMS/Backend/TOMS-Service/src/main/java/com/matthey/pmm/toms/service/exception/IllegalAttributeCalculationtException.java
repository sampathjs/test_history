package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating the request to calculate the (default) value for an attribute is not supported.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalAttributeCalculationtException extends ResponseStatusException  {
	private final Class clazz;
	private final String attributeName;
	
	public IllegalAttributeCalculationtException (Class clazz, String attributeName) {
		super (HttpStatus.BAD_REQUEST, "The calculation of the attribute " 
				+ clazz.getName() + "." + attributeName + " is not supported or there are multiple possible calculations.");
		this.attributeName = attributeName;
		this.clazz = clazz;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public Class getClazz() {
		return clazz;
	}
}
