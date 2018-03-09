package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.staticdata.EnumReferenceTable;


/**
 * The Class CurrencyValidator. Validate the currency field in the input message
 */
public class CoverageCurrencyValidator extends FieldValidatorBase {
	
	/** The Constant TRAN_ERROR_CODE. */
	private static final int TRAN_ERROR_CODE = 1005;
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2006;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid currency code";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation currency does not match request.";
	
	/**
	 * Instantiates a new currency validator.
	 *
	 * @param currentContext the current context
	 */
	public CoverageCurrencyValidator(final Context currentContext) {
		super(currentContext);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.CURRENCY_CODE.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		refTableCheck(value, EnumReferenceTable.Currency);
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
			String dealCurrency = coverageTrade.getQuotationCurrency();
		
			if (!value.equals(dealCurrency)) {
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE, TRAN_ERROR_DESCRIPTION));
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_DESCRIPTION;
	}

}
