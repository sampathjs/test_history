package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.openlink.util.logging.PluginLog;



/**
 * The Class TimeCodeValidator. Validate the time code in the inbound message.
 */
public class TimeCodeValidator extends FieldValidatorBase {
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2210;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid timecode";
	
	/** Template date loaded from the db. */
	private ISapTemplateData templateData;

	/**
	 * Instantiates a new instrument id validator.
	 *
	 * @param currentContext the current context
	 * @param currentTemplateData the current template data
	 */
	public TimeCodeValidator(final Context currentContext, final ISapTemplateData currentTemplateData) {
		super(currentContext);
		templateData = currentTemplateData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.TIME_CODE.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		
			switch (templateData.getInsType()) {
			case "FX":
				// No validation required
				break;
				
			case "METAL-SWAP":
				if (value == null || value.length() == 0) {
					PluginLog.error("timecode not defined in input message"); 
					throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));	
				}				
				
				if (templateData.getRefSource() == null || templateData.getRefSource().length() == 0) {
					PluginLog.error("Error validating the time code, no data found for id " + value); 
					throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));	
				}				
				break;

			default:
				String errorMessage = "Instrument type " + templateData.getInsType() + " is not supported.";
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
	}

	/**
	 * Validate.
	 *
	 * @param value the value
	 * @param existingTrade the existing trade
	 * @throws ValidatorException the validator exception
	 */
	@Override
	public void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		return FIELD_ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return FIELD_ERROR_DESCRIPTION;
	}

}
