package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;

/**
 * The Class CoverageDealInstructionValidator. Validates the field deal instruction.
 */
public class CoverageDealInstructionValidator extends FieldValidatorBase {

	/** The Constant tran level error code. */
	private static final int TRAN_ERROR_CODE = 2001;

	/** The Constant tran level error description. */
	private static final String TRAN_ERROR_DESCRIPTION = 
			"A firm trade already exists in Openlink with this SAP coverage deal instruction id.";
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.COVERAGE_INSTRUCTION_NO.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public void validate(final String value) throws ValidatorException {
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
		return TRAN_ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return TRAN_ERROR_DESCRIPTION;
	}

}
