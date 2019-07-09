
package com.olf.jm.SapInterface.messageValidator.fieldValidator;


import com.olf.jm.SapInterface.messageValidator.ValidatorException;


/**
 * The Interface IFieldValidator. Validate a single field in the inbound mesage.
 */
public interface ITwoFieldValidator {
	
	/**
	 * Gets the field name of the field validator applies to.
	 *
	 * @return the field name
	 */
	String getFieldName();
	

	/**
	 * Gets the field name of the field validator applies to.
	 *
	 * @return the field name
	 */
	String getOtherFieldName();
	/**
	 * Validate the field data.
	 *
	 * @param value the value to validate.
	 * @throws ValidatorException if there is an error with the the field value.
	 */
	 void validate(String firstValue, String secondValue) throws ValidatorException;
	
	/**
	 * Validate the data in the field against a trade that has already been booked in the db.
	 *
	 * @param value the value to validate
	 * @param trade the existing trade in the database to validate against
	 * @throws ValidatorException if there is an error with the the field value.
	 */

}
