package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating an operation applied to an entity in a wrong state.
 * @author jwaechter 
 * @version 1.0
 */

public class IllegalLegRemovalException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String entity;
	private final String leg;
	private final String reason;
	
	public IllegalLegRemovalException (Class clazz, String method,
			String parameter, String entity, String leg, String reason) {
		super (HttpStatus.BAD_REQUEST, "Illegal Leg Removal Operation applied when calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter 
			+ "'. Entity: '" + entity + "', when trying to remove leg: " + leg + ", reason: " + reason);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.leg = leg;
		this.entity = entity;
		this.reason = reason;
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

	public String getEntity() {
		return entity;
	}

	public String getLeg() {
		return leg;
	}

	public String getReason() {
		return reason;
	}
}
