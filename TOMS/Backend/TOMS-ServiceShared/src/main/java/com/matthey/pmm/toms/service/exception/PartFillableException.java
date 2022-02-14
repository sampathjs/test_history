package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating one of the constraints regarding orders
 * that are not partially fillable has been violated.

 * @author jwaechter 
 * @version 1.0
 */

public class PartFillableException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final String reason;
	
	public PartFillableException (Class clazz, String method,
			String parameter, String reason) {
		super (HttpStatus.BAD_REQUEST, "Part Fillable Constraint violation calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'. Reason: " + reason 
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
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

	public String getReason() {
		return reason;
	}
}
