package com.olf.jm.coverage.businessObjects;


/**
 * Exception class for errors detected during the coverage process.
 */
public class CoverageRequestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4767859822133528148L;

	/**
	 * Instantiates a new coverage request exception.
	 *
	 * @param errorMessage the error message
	 */
	public CoverageRequestException(final String errorMessage) {
		super(errorMessage);
	}
}
