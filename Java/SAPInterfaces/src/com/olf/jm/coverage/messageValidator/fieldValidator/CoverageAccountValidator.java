package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.openlink.util.logging.PluginLog;

/**
 * The Class BusinessUnitCodeValidator.
 * 
 * Validates that SAP business unit code. Field needs to be present and contain
 * a valid value as defined in the party info field Ext Business Unit Code
 */
public class CoverageAccountValidator extends FieldValidatorBase {

	/** The Constant FIELD_ERROR_CODE. */
	static final int FIELD_ERROR_CODE = 1012;

	/** The Constant FIELD_ERROR_DESCRIPTION. */
	static final String FIELD_ERROR_DESC = "Quotation From Account doesnot match request";

	/** The party data loaded from the DB. */
	private ISapPartyData partyData;

	/**
	 * Instantiates a new business unit code validator.
	 * 
	 * @param currentPartyData
	 *            the current party data
	 */
	public CoverageAccountValidator(final ISapPartyData currentPartyData) {

		partyData = currentPartyData;

	}

	public CoverageAccountValidator(Context context) {
		super(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase
	 * #getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.ACCOUNT_NUMBER.getColumnName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase
	 * #validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		// Validate that the field is present

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#
	 * validate(java.lang.String,
	 * com.olf.jm.coverage.businessObjects.ICoverageTrade)
	 */
	@Override
	public void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {

		String message = "Error validating field " + getFieldName()
				+ " Quotation External Business Unit doesn't match request";
		try {
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			if (coverageTrade.isValid()) {
				String formOnTrade = coverageTrade.getQuotationForm();
				String locoOnTrade = coverageTrade.getQuotationLoco();
				if ((null != locoOnTrade && !locoOnTrade.isEmpty())
						&& (null != formOnTrade && !formOnTrade.isEmpty())) {

					String form = partyData.getExternalParty().getAccountForm();
					String loco = partyData.getExternalParty().getAccountLoco();
					if (!form.equalsIgnoreCase(formOnTrade)
							|| !(loco.equalsIgnoreCase(locoOnTrade))) {
						PluginLog.error(message);
						throw new ValidatorException(buildErrorMessage(
								getFieldErrorCode(), getFieldErrorDesc()));

					}

				} else {
					PluginLog.error("Form or Loco on the Quote doesn't exist");
					throw new RuntimeException(
							"Form or Loco on the Quote doesn't exist");
				}
			}
		} catch (ValidatorException exp) {
			throw exp;
		} catch (Exception exp) {
			PluginLog.error(exp.getMessage());
			throw new ValidatorException(buildErrorMessage(0, exp.getMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase
	 * #getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return FIELD_ERROR_DESC;
	}

	@Override
	protected int getFieldErrorCode() {
		// TODO Auto-generated method stub
		return FIELD_ERROR_CODE;
	}

}
