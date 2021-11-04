package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating that the number of legs is insufficient.
 * @author jwaechter 
 * @version 1.0
 */

public class MissingLegException extends ResponseStatusException  {
	private final Class clazz;
	private final String attributeName;
	private final int providedLegCount;
	private final int requiredLegCount;
	
	public MissingLegException (Class clazz, String attributeName, int providedLegCount, int requiredLegCount) {
		super (HttpStatus.BAD_REQUEST, "The number of legs for  " 
				+ clazz.getName() + "." + attributeName + 
				" is insufficient. There are " + providedLegCount + " legs provided but " 
				+ requiredLegCount + " required.");
		this.attributeName = attributeName;
		this.clazz = clazz;
		this.providedLegCount = providedLegCount;
		this.requiredLegCount = requiredLegCount;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public Class getClazz() {
		return clazz;
	}

	public int getProvidedLegCount() {
		return providedLegCount;
	}

	public int getRequiredLegCount() {
		return requiredLegCount;
	}
}
