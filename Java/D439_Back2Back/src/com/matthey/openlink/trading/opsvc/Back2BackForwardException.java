package com.matthey.openlink.trading.opsvc;

public class Back2BackForwardException extends RuntimeException {

	
	private final String reason;
	private final int cause;
	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public Back2BackForwardException(String reason, int cause, String message) {
		super(message);
		this.reason = reason;
		this.cause=cause;
	}

	/**
	 * @param message
	 * @param cause
	 */
	public Back2BackForwardException(String message, Throwable cause) {
		super(message, cause);
		this.cause=0;
		this.reason="N/A";
	}

	
	public String getReason() {
		return this.reason;
	}

	public int getId() {
		return this.cause;
	}

	
}
