package com.openlink.matthey.simresults;

public class SimulationResultsException extends RuntimeException {

	
	private final int cause;
	
	 public SimulationResultsException(String message, Throwable cause, int reasonCode) {
	        super(message, cause);
	        this.cause = reasonCode;
	    }

	    public SimulationResultsException(String message,int reasonCode) {
	        super(message);
	        this.cause = reasonCode;
	    }
	    
	    public int getReasonCode() {
	        return cause;
	    }
	
}
