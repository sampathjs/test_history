package com.matthey.openlink.reporting.runner;

@SuppressWarnings("serial")
public class ReportRunnerException extends RuntimeException {

	
	private final String reason;
	
	@Override
	public String getLocalizedMessage() {
		return reason;
	}

	@Override
	public String getMessage() {
		return reason;
	}

	public ReportRunnerException(String message, Throwable cause) {
		super(message, cause);
		reason =  message;
	}

	public ReportRunnerException(String message) {
		super(message);
		reason = message;
	}
	


}
