package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.staticdata.EnumReferenceTable;


/**
 * The Class UOMValidator. Validate the UOM field in the inbound massage.
 */
public class UOMValidator extends FieldValidatorBase {

	/** The Constant TRAN_ERROR_CODE. */
	private static final int TRAN_ERROR_CODE = 1003;
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2005;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid weight UOM";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation UOM does not match request.";
	
	
	/**
	 * Instantiates a new UOM validator.
	 *
	 * @param currentContext the current context
	 */
	public UOMValidator(final Context currentContext) {
		super(currentContext);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.WEIGHT_UOM.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		refTableCheck(value, EnumReferenceTable.IdxUnit);
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
			String dealUom = coverageTrade.getQuotationUOM();
		
			if (!value.equals(dealUom)) {
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
