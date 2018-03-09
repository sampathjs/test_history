package com.olf.jm.SapInterface.messageMapper.fieldMapper;

/**
 * The Class FieldMappingException. Exception class defining errors that occur when mapping 
 * from input message to output message
 */
public class FieldMappingException extends Exception {
	
	/** */
	private static final long serialVersionUID = -7121522562827906462L;

	/**
	 * Instantiates a new field mapping exception.
	 *
	 * @param errorMessage the error message
	 */
	public FieldMappingException(final String errorMessage) {
		super(errorMessage);
	}
}
