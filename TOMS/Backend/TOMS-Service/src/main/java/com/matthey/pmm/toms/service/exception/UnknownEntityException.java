package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating an invalid ID has been provided
 * the expected format.
 * @author jwaechter 
 * @version 1.0
 */
public class UnknownEntityException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String providedId;
	private final String entityName;
	
	public UnknownEntityException (Class clazz, String method,
			String parameter, String providedId, String entityName) {
		super (HttpStatus.BAD_REQUEST, "The method " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "' has to designate '" + 
			entityName + "' but there is no entity of that type having the provided ID '" + providedId + "'"
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.providedId = providedId;
		this.entityName = entityName;
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

	public String getEntityName() {
		return entityName;
	}
}
