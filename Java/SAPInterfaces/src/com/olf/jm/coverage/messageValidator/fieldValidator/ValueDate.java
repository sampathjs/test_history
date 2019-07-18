package com.olf.jm.coverage.messageValidator.fieldValidator;

import java.util.Calendar;
import java.util.Date;

import com.olf.jm.SapInterface.util.DateUtils;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openjvs.OCalendar;


/**
 * The Class ValueDate. Validate the value date field in the inbouund message.
 */
public class ValueDate extends FieldValidatorBase {

	/** The Constant TRAN_ERROR_CODE. */
	private static final int TRAN_ERROR_CODE = 1007;
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2008;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Value date is Invalid";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation value date does not match request.";
	
	/** Template date loaded from the db. */
	private ISapTemplateData templateData;
	
	public ValueDate(ISapTemplateData currentTemplateData) {
		templateData = currentTemplateData;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.VALUE_DATE.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		super.dateCheck(value);
		if(templateData.getSapInsType().equalsIgnoreCase("MKT")){
			updateCflowOnTemplate(value);
		}
			
	}

	/*
	 * This method is called to set the cashflow on the MKT type instruments.
	 * By Default MKT are booked as SPOT but if value date is in future then 
	 * MKT are bokked as Forward. Template default is spot, hence setting it to 
	 * forward in case of future value date.
	 * */
	private void updateCflowOnTemplate(String value) throws ValidatorException {
		try{
			if(OCalendar.parseString(value) > OCalendar.getServerDate()){
			templateData.setCflowType("Forward");
			}
		}catch(Exception exp){
			throw new ValidatorException(buildErrorMessage(FIELD_ERROR_CODE,	FIELD_ERROR_DESCRIPTION));
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
	public final void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		if (existingTrade.isValid()) {

			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			
			Date valueDate = coverageTrade.getQuotationValueDate();
			Calendar valueCal = Calendar.getInstance(); 
			valueCal.setTime(valueDate);
			
			Date inputDate = DateUtils.getDate(value);
			Calendar inputCal = Calendar.getInstance(); 
			inputCal.setTime(inputDate);


			// Do long comparison to due to timezone issues
			if (inputCal.get(Calendar.YEAR) != valueCal.get(Calendar.YEAR)) {
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,	TRAN_ERROR_DESCRIPTION));
			}
			if (inputCal.get(Calendar.MONTH) != valueCal.get(Calendar.MONTH)) {
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,	TRAN_ERROR_DESCRIPTION));
			}
			if (inputCal.get(Calendar.DAY_OF_MONTH) != valueCal.get(Calendar.DAY_OF_MONTH)) {
				throw new ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,	TRAN_ERROR_DESCRIPTION));
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
