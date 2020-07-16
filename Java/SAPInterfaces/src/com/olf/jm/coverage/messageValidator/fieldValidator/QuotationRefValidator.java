
package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.jm.logging.Logging;


/**
 * The Class QuoationRefValidator. Validates the field quotation reference in the 
 */
public class QuotationRefValidator extends FieldValidatorBase {
	
	/** The Constant TRAN_ERROR_CODE. Error code for errors when validating against an existing trade. */
	private static final int TRAN_ERROR_CODE = 1008;
	
	/** The Constant FIELD_ERROR_CODE. Error code for errors with the field contents. */
	private static final int FIELD_ERROR_CODE = 2010;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. Error description for errors when validating against an existing trade.*/
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid quotation ref";
	
	/** The Constant TRAN_ERROR_DESCRIPTION. Error description for errors with the field contents. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation reference does not match a valid deal.";
	
	private String sapInstrumentID;
	

	
	/**
	 * Instantiates a new quotation ref validator.
	 *
	 * @param currentContext the current context
	 * @param currentTemplateData 
	 */
	public QuotationRefValidator(final Context currentContext, ISapTemplateData currentTemplateData) {
		super(currentContext);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.QUOTE_REFERENCE_ID.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public void validate(final String value) throws ValidatorException {
		// No validation required
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#
	 * validate(java.lang.String, com.olf.jm.coverage.businessObjects.ICoverageTrade)
	 */
	@Override
	public final void validate(final String value, final ISapEndurTrade existingTrade)
 throws ValidatorException {
		
			if (existingTrade.isValid()) {
				int dealTrackingNumber = existingTrade.getDealTrackingNumber();

				if (value != null && value.length() > 0) {
					int messageDealTrackingNumber = new Integer(value)
							.intValue();

					if (messageDealTrackingNumber != dealTrackingNumber) {
						Logging.error("Deal tracking number in message does not match deal in database.");
						throw new ValidatorException(buildErrorMessage(
								TRAN_ERROR_CODE, TRAN_ERROR_DESCRIPTION));
					}
					// Unlikly that the trader will set this when manually
					// booking so remove check
					// ICoverageTrade coverageTrade = (ICoverageTrade)
					// existingTrade;
					// if (!coverageTrade.isCoverage()) {
					// PluginLog.error("Deal in database is not a coverage trade.");
					// throw new
					// ValidatorException(buildErrorMessage(TRAN_ERROR_CODE,
					// TRAN_ERROR_DESCRIPTION));
					// }

					if (existingTrade.getTradeStatus() != EnumTranStatus.Pending) {
						Logging.error("Deal in databaseis in a invalid status, deal is in a validated status.");
						throw new ValidatorException(buildErrorMessage(
								TRAN_ERROR_CODE, TRAN_ERROR_DESCRIPTION));
					}

				} else {
					// SAP coverrage id exists in database but message deal id
					// is not populated
					Logging.error("Deal found in db with matching sap id but message does not contain the deal tracking number.");
					throw new ValidatorException(
							buildErrorMessage(4100,
									"CoverageInstructionNo already exists in the Openlink system."));

				}

			} else {
				if (value != null && value.length() > 0) {
					Logging
							.error("SAP Order Id does not exist in the database but the message contains a deal number.");
					throw new ValidatorException(
							buildErrorMessage(
									4101,
									"SAP Order Id does not exist in the database but the message contains a deal number."));

				}
			}
		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_DESCRIPTION;
	}

}
