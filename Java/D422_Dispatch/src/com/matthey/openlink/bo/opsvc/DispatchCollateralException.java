package com.matthey.openlink.bo.opsvc;

public class DispatchCollateralException extends RuntimeException {

	private final int cause;
	private String reason;
	
	public DispatchCollateralException(String message, int cause) {
		super(message);
		this.cause = cause;
	}
	
	public DispatchCollateralException(String message, Throwable cause) {
		super(message, cause);
		this.cause=0;
	}
	
	public DispatchCollateralException(String reason, int errorCode, String message) {
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
