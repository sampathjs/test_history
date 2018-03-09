package com.olf.jm.sapTransfer.messageValidator.fieldValidator;

import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;


/**
 * The Class ValueDate. Validate the input message field ValueDate.
 */
public class ValueDateValidator extends FieldValidatorBase {
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 3008;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Value date is Invalid";
	
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapTransferRequest.VALUE_DATE.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		super.dateCheck(value);

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator#
	 * validate(java.lang.String, com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	public final void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		return FIELD_ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return FIELD_ERROR_DESCRIPTION;
	}

}
