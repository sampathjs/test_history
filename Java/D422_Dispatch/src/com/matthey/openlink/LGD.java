package com.matthey.openlink;

/**
 * Provide the client request URN based on the defined format
 * <br>
 * The format follows the following layout {@value #prefix}:<next value in sequence>
 */
public class LGD {
	private final String prefix="LGD";
	private final int number;

	@SuppressWarnings("unused")
	private LGD() {
		this(Integer.MIN_VALUE);
	};
		
	public LGD(int number) {
		this.number = number;
	}
	
	public String toString() {
		if (isValid())
			return String.format("%s:%d", prefix, number);
		else
			return "";
	}

	private boolean isValid() {
		
		return number>0;
	}
	
	
}
