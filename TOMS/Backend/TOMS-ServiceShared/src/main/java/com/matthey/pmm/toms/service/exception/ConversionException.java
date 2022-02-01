package com.matthey.pmm.toms.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception indicating problems during one of the conversions.
 * @author jwaechter 
 * @version 1.0
 */

public class ConversionException extends ResponseStatusException  {	
	public ConversionException (String message) {
		super (HttpStatus.BAD_REQUEST, message);
	}
}
