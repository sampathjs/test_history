package com.matthey.pmm.toms.service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.OrderTo;

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
