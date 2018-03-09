package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;


/**
 * The Class WeightValidator. Validate the weight field in the inbound message
 */
public class WeightValidator extends FieldValidatorBase {
	

	/** The Constant TRAN_ERROR_CODE. */
	private static final int TRAN_ERROR_CODE = 1002;
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2004;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Missing metal weight";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation weight does not match request.";

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {

		return EnumSapCoverageRequest.WEIGHT.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		super.doubleCheck(value);
	}

	/**
	 * Validate.
	 *
	 * @param value the value
	 * @param existingTrade the existing trade
	 * @throws ValidatorException the validator exception
	 */
	@Override
	public final void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		if (existingTrade.isValid()) {
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			double dealWeight = coverageTrade.getQuotationWeight();
		
			double weight = Double.parseDouble(value);
		
			if (dealWeight != weight) {
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE, TRAN_ERROR_DESCRIPTION));
			}
		}
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
