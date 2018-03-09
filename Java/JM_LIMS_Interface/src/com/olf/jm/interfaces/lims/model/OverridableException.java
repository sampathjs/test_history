package com.olf.jm.interfaces.lims.model;

/*
 * 	History: 
 * 	2015-12-09	V1.0	jwaechter	- initial version 
 */

/**
 * Class representing an exception the user can override in the OPS window.
 * @author jwaechter
 * @version 1.0
 */
public class OverridableException extends RuntimeException {
	public OverridableException (String message) {
		super (message);
	}
}
