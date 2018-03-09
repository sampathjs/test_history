package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.openlink.util.logging.PluginLog;



/**
 * The Class InstrumentIdValidator. Validate the instrument id in the inbound message.
 */
public class InstrumentIdValidator extends FieldValidatorBase {
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2200;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid instrument id";
	
	/** Template date loaded from the db. */
	private ISapTemplateData templateData;

	/**
	 * Instantiates a new instrument id validator.
	 *
	 * @param currentContext the current context
	 * @param currentTemplateData the current template data
	 */
	public InstrumentIdValidator(final Context currentContext, final ISapTemplateData currentTemplateData) {
		super(currentContext);
		templateData = currentTemplateData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.INSTRUMENT_ID.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		if (!value.equalsIgnoreCase(templateData.getSapInstrumentId())) {
			PluginLog.error("Error validating the instrument id, value loaded from db " + templateData.getSapInstrumentId()  
					+ " does not match message value " + value);
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));		
		}
		
		if (templateData.getTemplate() == null || templateData.getTemplate().length() == 0) {
			PluginLog.error("Error validating the instrument id, no data found for id " + value); 
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));	
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
