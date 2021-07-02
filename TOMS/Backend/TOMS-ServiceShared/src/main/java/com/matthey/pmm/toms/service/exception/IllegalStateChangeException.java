package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating a status change while updating an entity like a LimitOrder is invalid.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalStateChangeException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String fromState;
	private final String toState;
	private final String allowedStatus;
	
	public IllegalStateChangeException (Class clazz, String method,
			String parameter, String fromState, String toState, String allowedStatus) {
		super (HttpStatus.BAD_REQUEST, "Illegal state change while calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'. Current state: '" + 
			fromState + "'. New State: '" + toState + "'. Allowed new states: " + allowedStatus);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.fromState = fromState;
		this.toState = toState;
		this.allowedStatus = allowedStatus;
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

	public String getFromState() {
		return fromState;
	}

	public String getToState() {
		return toState;
	}

	public String getAllowedStatus() {
		return allowedStatus;
	}
}
