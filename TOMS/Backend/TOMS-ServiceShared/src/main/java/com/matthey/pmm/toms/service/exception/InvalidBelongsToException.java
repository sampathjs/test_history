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

public class InvalidBelongsToException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameterManager;
	private final String parameterManaged;
	private final long idManaged;
	private final String idsKnownToManager;
	
	public InvalidBelongsToException (Class clazz, String method,
			String parameterManager, String parameterManaged, long idManaged,  String idsKnownToManager) {
		super (HttpStatus.BAD_REQUEST, "The entity '" + parameterManager
			+ "' does not manage entity '" + 
			parameterManaged + "' having ID #" + idManaged + ". Known IDs of the same type managed by '" + parameterManager + "' are the following: '" + idsKnownToManager + "'."
					+ "\nOriginal class.method called ='" +  clazz.getName() + "'.'" + method + "'"
			);
		this.clazz = clazz;
		this.method = method;
		this.parameterManager = parameterManager;
		this.parameterManaged = parameterManaged;
		this.idsKnownToManager = idsKnownToManager;
		this.idManaged = idManaged;
	}

	public Class getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	public String getParameterManager() {
		return parameterManager;
	}

	public String getParameterManaged() {
		return parameterManaged;
	}

	public String getIdsKnownToManager() {
		return idsKnownToManager;
	}

	public long getIdManaged() {
		return idManaged;
	}
}
