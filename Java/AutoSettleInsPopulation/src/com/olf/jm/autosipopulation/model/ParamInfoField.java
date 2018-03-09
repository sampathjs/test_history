package com.olf.jm.autosipopulation.model;

/*
 * History:
 * 2015-06-09	V1.0	jwaechter 	- initial version
 */

/**
 * Enum containing relevant tran info fields (on parameter level) for the 
 * settlement assignment instructions. 
 * @author jwaechter
 * @version 1.0
 */
public enum ParamInfoField {
	FormPhys("Form-Phys");
	
	private final String name;
	
	private ParamInfoField (String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
