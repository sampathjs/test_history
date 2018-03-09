package com.matthey.openlink;

import com.olf.openrisk.internal.OpenRiskException;

public class AvailableServicesException extends OpenRiskException {

	private final String reason;
	
	
	private final int cause;

	public AvailableServicesException(String reason, int cause, String message) {
		super(message);
		this.reason = reason;
		this.cause = cause;
	}
	
	public AvailableServicesException(String reason, int cause, String message, Throwable throwable) {
		super(message, throwable);
		this.reason = reason;
		this.cause = cause;
	}

	public AvailableServicesException(String message) {
		this("FAIL", 99999, message);
	}

	public AvailableServicesException(String message, Throwable cause) {
		this("FAIL", 99999, message, cause);
	}
	
	@Override
	public String getLocalizedMessage() {
		return String.format("CAUSE:%d:REASON>%s<%s", cause, reason, super.getLocalizedMessage());
	}
	

}
