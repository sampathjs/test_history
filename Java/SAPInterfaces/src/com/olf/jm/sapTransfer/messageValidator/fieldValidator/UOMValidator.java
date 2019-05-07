package com.olf.jm.sapTransfer.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openrisk.staticdata.EnumReferenceTable;


/**
 * The Class UOMValidator. Validate the UOM field in the inbound massage.
 */
public class UOMValidator extends FieldValidatorBase {

	/** The Constant TRAN_ERROR_CODE. */
	//private static final int TRAN_ERROR_CODE =3013;
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 3013;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid Unit of Measure.";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. */
	//private static final String TRAN_ERROR_DESCRIPTION = "Quota UOM does not match request.";
	
	
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
		return EnumSapTransferRequest.UOM.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		super.refTableCheck(value, EnumReferenceTable.IdxUnit);
	}

	@Override
	public void validate(String value, ISapEndurTrade existingTrade)
			throws ValidatorException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int getFieldErrorCode() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_CODE;
	}

	@Override
	protected String getFieldErrorDesc() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_DESCRIPTION;
	}

/*	*//**
	 * Validate.
	 *
	 * @param value the value
	 * @param existingTrade the existing trade
	 * @throws ValidatorException the validator exception
	 *//*
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
	
	 (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 
	@Override
	protected final int getFieldErrorCode() {
		return FIELD_ERROR_CODE;
	}
	
	 (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 
	@Override
	protected final String getFieldErrorDesc() {
		return FIELD_ERROR_DESCRIPTION;
	}
*/
}
