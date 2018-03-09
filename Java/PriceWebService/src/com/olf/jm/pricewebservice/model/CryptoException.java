package com.olf.jm.pricewebservice.model;

import java.util.Arrays;


/*
 * History:
 * 2015-11-02	V1.0	jwaechter	- initial version
 */

/**
 * Class mapping cryptographic related exceptions to a runtime exception
 * @author jwaechter
 * @version 1.0
 */
public class CryptoException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 13123213213321L;

	public CryptoException(String message, Exception source) {
		super (message + ":\n" + Arrays.deepToString(source.getStackTrace()), source );
	}
}
