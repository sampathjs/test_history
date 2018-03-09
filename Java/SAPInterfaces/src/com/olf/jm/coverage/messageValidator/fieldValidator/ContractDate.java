package com.olf.jm.coverage.messageValidator.fieldValidator;

import java.util.Calendar;
import java.util.Date;

import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.SapInterface.util.DateUtils;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.openlink.util.logging.PluginLog;


/**
 * The Class ContractDate. Validate the input message field ContractDate.
 */
public class ContractDate extends FieldValidatorBase {
	
	/** The Constant TRAN_ERROR_CODE. */
	private static final int TRAN_ERROR_CODE = 1006;
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2007;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Contract date is Invalid";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation contract date does not match request.";
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.CONTRACT_DATE.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		super.dateCheck(value);

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator#validate(
	 * java.lang.String, com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	public final void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		if (existingTrade.isValid()) {
			
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;

			Date quotationDate = coverageTrade.getQuotationContractDate();
			Calendar quotationCal = Calendar.getInstance(); 
			quotationCal.setTime(quotationDate);
			
			Date inputDate = DateUtils.getDate(value);
			Calendar inputCal = Calendar.getInstance(); 
			inputCal.setTime(inputDate);


			// Do long comparison to due to timezone issues
			if (inputCal.get(Calendar.YEAR) != quotationCal.get(Calendar.YEAR)) {
				PluginLog.error("ContractDate validation failed. Trade Date " + quotationDate + " message date " + inputDate);
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,	TRAN_ERROR_DESCRIPTION));
			}
			if (inputCal.get(Calendar.MONTH) != quotationCal.get(Calendar.MONTH)) {
				PluginLog.error("ContractDate validation failed. Trade Date " + quotationDate + " message date " + inputDate);
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,	TRAN_ERROR_DESCRIPTION));
			}
			if (inputCal.get(Calendar.DAY_OF_MONTH) != quotationCal.get(Calendar.DAY_OF_MONTH)) {
				PluginLog.error("ContractDate validation failed. Trade Date " + quotationDate + " message date " + inputDate);
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,	TRAN_ERROR_DESCRIPTION));
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
