package com.olf.jm.SapInterface.messageValidator.fieldValidator;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.util.DateUtils;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.Table;



/**
 * The Class FieldValidatorBase. Base class for field validators.
 */
public abstract class FieldValidatorBase implements IFieldValidator {
	
	/** The context the script is running in. */
	protected Context context = null;
		
	/**
	 * Instantiates a new field validator base.
	 */
	public FieldValidatorBase() {
		Logging.init(getClass(), "FieldValidatorBase", "FieldValidatorBase");
	}
	
	/**
	 * Instantiates a new field validator base.
	 *
	 * @param currentContext the current context
	 */
	public FieldValidatorBase(final Context currentContext) {
		context = currentContext;
		Logging.init(getClass(), "FieldValidatorBase", "FieldValidatorBase");
	}	
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator#getFieldName()
	 */
	@Override
	public abstract String getFieldName();

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator#validate(java.lang.String)
	 */
	@Override
	public abstract void validate(String value) throws ValidatorException;

	/**
	 * Validate.
	 *
	 * @param value the value
	 * @param existingTrade the existing trade
	 * @throws ValidatorException the validator exception
	 */
	@Override
	public abstract void validate(String value, ISapEndurTrade existingTrade) throws ValidatorException;
	
	/**
	 * Gets the field error code.
	 *
	 * @return the field error code
	 */
	protected abstract int getFieldErrorCode();
	
	/**
	 * Gets the field error desc.
	 *
	 * @return the field error desc
	 */
	protected abstract String getFieldErrorDesc();
	
	/**
	 * Builds the error message.
	 *
	 * @param errorId the error id
	 * @param errorDescription the error description
	 * @return the string
	 */
	protected final String buildErrorMessage(final int errorId, final String errorDescription) {
		
		StringBuilder errorString = new StringBuilder();
		
		errorString.append("Status Code [");
		errorString.append(errorId);
		errorString.append("] Message Text [");
		errorString.append(errorDescription);
		errorString.append("]");
		return errorString.toString();
	}
	
	/**
	 * Validate data that the field contains a valid double number.
	 *
	 * @param value the value
	 * @throws ValidatorException the validator exception
	 */
	protected final void doubleCheck(final String value) throws ValidatorException {
		// Validate that the field is present
		if (value == null || value.length() == 0) {
			Logging.error("Error validating field " + getFieldName() + " data is missing or empty.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		
		// Check a valid double value
		try {
			double convertedValue = Double.parseDouble(value);
			
			if (convertedValue <= 0.0) {
				Logging.error("Error validating field " + getFieldName() + " value [" + value + "] needs to be greater than zero.");
				throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
				
			}
		} catch (Exception e) {
			Logging.error("Error validating field " + getFieldName() + " value [" + value + "] is not a valid.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
			
		}		
	}
	
	/**
	 * Check that the value is a valid entry in a reference table.
	 *
	 * @param value the value
	 * @param refTable the ref table
	 * @throws ValidatorException the validator exception
	 */
	protected final void refTableCheck(final String value, final EnumReferenceTable refTable) throws ValidatorException {
		// Validate that the field is present
		if (value == null || value.length() == 0) {
			Logging.error("Error validating field " + getFieldName() + " data is missing or empty.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		
		if (context == null) {
			throw new ValidatorException("Initialisation error, context is not set.");
		}
		
		Table refData = context.getStaticDataFactory().getTable(refTable);

		int rowFound = refData.find(refData.getColumnId("label"), value, 0);
		
		if (rowFound < 0) {
			Logging.error("Error validating field " + getFieldName() + " value [" + value + "] is not a valid.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}	
	}
	
	/**
	 * Check that the value contains a valid date value.
	 *
	 * @param value the value
	 * @throws ValidatorException the validator exception
	 */
	protected final void dateCheck(final String value) throws ValidatorException {
		// Validate that the field is present
		if (value == null || value.length() == 0) {
			Logging.error("Error validating field " + getFieldName() + " data is missing or empty.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		
		// Check a valid date value
		try {
			//CalendarFactory cf = context.getCalendarFactory();
			
			Date inputDate = DateUtils.getDate(value);
			
			if (inputDate == null) {
				Logging.error("Error validating field " + getFieldName() + " value [" + value + "] is not a supported date format.");
				throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
			}
		} catch (Exception e) {
			Logging.error("Error validating field " + getFieldName() + " value [" + value + "] is not a valid.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
			
		}		
	}

}
