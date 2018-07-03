package com.matthey.openlink.inventory;

public class VostroInventoryExceptionCode extends RuntimeException {

	private final String reason;
	
	
	private final int cause;
	public VostroInventoryExceptionCode(String reason, int cause, String message) {
		super(message);
		this.reason = reason;
		this.cause = cause;
	}
	
	public VostroInventoryExceptionCode(String message) {
		this("FAIL", 99999, message);
	}

	@Override
	public String getLocalizedMessage() {
		return String.format("CAUSE:%d:REASON>%s<%s", cause, reason, super.getLocalizedMessage());
	}
	
	
}
