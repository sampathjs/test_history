package com.matthey.pmm.toms.service.shared;

import org.springframework.web.bind.annotation.ControllerAdvice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;



/**
 * This class converts exceptions thrown by the rest controllers into HTTP return codes and error texts.
 * @author jwaechter
 * @version 1.0
 */
@ControllerAdvice
class GlobalControllerExceptionHandler {	
	
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 400
    @ExceptionHandler(IllegalReferenceTypeException.class)
    public String reportIllegalReferenceType(IllegalReferenceTypeException ex) {
    	return "The provided parameter '" + ex.getParameter() 
    	   + "' is expected to be of type '" + ex.getExpectedReferenceType()
    	   + "' but is of type '" + ex.getProvidedReferenceType() + "'";
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 400
    @ExceptionHandler(IllegalReferenceException.class)
    public String reportIllegalReference(IllegalReferenceException ex) {
    	return "The provided parameter '" + ex.getParameter() 
    	   + "' is expected to be reference '" + ex.getExpectedReference()
    	   + "' but is reference '" + ex.getProvidedReference() + "'";
    }

}