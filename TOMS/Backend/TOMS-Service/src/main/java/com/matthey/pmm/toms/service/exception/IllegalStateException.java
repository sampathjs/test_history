package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating an operation applied to an entity in a wrong state.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalStateException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String state;
	private final String entity;
	private final String requiredState;
	
	public IllegalStateException (Class clazz, String method,
			String parameter, String state, String entity, String requiredState) {
		super (HttpStatus.BAD_REQUEST, "Illegal Operation applied to entity while calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'. Current State '" + 
			state + "'. Entity: '" + entity + "'. Required State: " + requiredState);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.state = state;
		this.entity = entity;
		this.requiredState = requiredState;
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

	public String getState() {
		return state;
	}

	public String getEntity() {
		return entity;
	}

	public String getRequiredState() {
		return requiredState;
	}
}
