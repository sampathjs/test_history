package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.trading.EnumToolset;

/**
 * The Class Buy Sell Validator. Validate the buy sell field in the inbound message.
 */
public class BuySellFlagValidator extends FieldValidatorBase {
	
	/** The Constant TRAN_ERROR_CODE the error number of validation errors when checking against
	 * an existing trade. */
	private static final int TRAN_ERROR_CODE = 1014;
	
	/** The Constant FIELD_ERROR_CODE the error number when checking for formatting errors. */
	private static final int FIELD_ERROR_CODE = 2014;
	
	/** The Constant FIELD_ERROR_DESCRIPTION the error text when validating against the field contents. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid Buy Sell Flag";
	
	/** The Constant TRAN_ERROR_DESCRIPTION the error text when validating against an existing trade. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation Buy Sell flag doesn't match request.";
	
	
	/**
	 * Instantiates a new UOM validator.
	 *
	 * @param currentContext the current context
	 */
	public BuySellFlagValidator(final Context currentContext) {
		super(currentContext);
	}
	
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.BUY_SELL_FLAG.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		refTableCheck(value, EnumReferenceTable.BuySell);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.
	 * FieldValidatorBase#validate(java.lang.String, com.olf.jm.coverage.businessObjects.ICoverageTrade)
	 */
	@Override
	public final void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		// If an existing trade exists and the deal is a FX deal then validate the price, otherwise do nothing.		
		if (existingTrade.isValid() && existingTrade.getToolset() == EnumToolset.Fx) {
			
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			String buysellFlag = coverageTrade.getBuySellFlag();
		
			
		
			if (value.compareToIgnoreCase(buysellFlag) != 0) {
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE, TRAN_ERROR_DESCRIPTION));
			}			
		}
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
