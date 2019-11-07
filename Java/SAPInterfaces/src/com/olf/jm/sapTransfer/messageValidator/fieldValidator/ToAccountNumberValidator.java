package com.olf.jm.sapTransfer.messageValidator.fieldValidator;

import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.openlink.util.logging.PluginLog;


/**
 * The Class ToAccountNumberValidator. 
 * 
 * Validates that to account number. 
 */
public class ToAccountNumberValidator extends FieldValidatorBase {

	/** Error code if validation fails. */
	static final int ERROR_CODE = 3010;
	
	/** The party data loaded from the DB. */
	private ISapPartyData partyData;
	
	/**
	 * Instantiates a new account number validator.
	 *
	 * @param currentPartyData the current party data
	 */
	public ToAccountNumberValidator(final ISapPartyData currentPartyData) {
		partyData = currentPartyData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapTransferRequest.TO_ACCOUNT_NUMBER.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		// No validation required
		if (value == null || value.length() == 0) {
			PluginLog.error("Error validating field " + getFieldName() + " data is missing or empty.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		if (!value.equals(partyData.getToAccount().getAccountNumber())) {
			PluginLog.error("Error validating field " + getFieldName() + " data is invalid, no mapping found to Endur BU.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#
	 * validate(java.lang.String, com.olf.jm.coverage.businessObjects.ICoverageTrade)
	 */
	@Override
	public void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		// No validation required
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		return ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return "To accouunt number field validation. Invalid to segment / to company code / to accouunt number";
	}

}