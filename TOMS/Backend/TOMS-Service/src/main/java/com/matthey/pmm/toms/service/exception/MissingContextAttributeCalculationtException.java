package com.matthey.pmm.toms.service.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating the request to calculate the (default) value for an attribute is not possible because at least
 * one dependent attribute necessary for the calculation is missing or has invalid value.
 * @author jwaechter 
 * @version 1.0
 */

public class MissingContextAttributeCalculationtException extends ResponseStatusException  {
	private final Class clazz;
	private final String attributeName;
	private final List<String> dependentAttributeNames;
	
	public MissingContextAttributeCalculationtException (Class clazz, String attributeName,
			List<String> dependentAttributeNames) {
		super (HttpStatus.BAD_REQUEST, "The calculation of the attribute " 
				+ clazz.getName() + "." + attributeName + 
				" is not possible because of the attributes used to calculate the value is missing or having an incorrect value."
				+ " Dependent attributes are: " + dependentAttributeNames);
		this.attributeName = attributeName;
		this.clazz = clazz;
		this.dependentAttributeNames = new ArrayList<>(dependentAttributeNames);
	}

	public String getAttributeName() {
		return attributeName;
	}

	public Class getClazz() {
		return clazz;
	}

	public List<String> getDependentAttributeNames() {
		return dependentAttributeNames;
	}
}
