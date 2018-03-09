package com.matthey.openlink.utilities.picklists;

/**
 * D422(4.12) Dispatch workflow
 * 
 * @version $Revision: $
 */
public class DispatchConsigneeException extends RuntimeException {
	
	private final int cause;
	private String reason;
	
	public DispatchConsigneeException(String message, int cause) {
		super(message);
		this.cause = cause;
	}
	
	public DispatchConsigneeException(String message, Throwable cause) {
		super(message, cause);
		this.cause=0;
	}
	
	public DispatchConsigneeException(String reason, int errorCode, String message) {
		this(message, errorCode);
		this.reason = reason;
	}
	public String getReason() {
		return this.reason;
	}

	public int getId() {
		return this.cause;
	}

}
