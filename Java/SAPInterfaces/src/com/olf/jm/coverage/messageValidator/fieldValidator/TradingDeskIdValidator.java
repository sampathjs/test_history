package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.SapInterface.util.Utility;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/**
 * The Class TradingDeskIdValidator. 
 * 
 * Validates that SAP business unit code. Field needs to be present and
 * contain a valid value as defined in the party info field Int LE Code
 */
public class TradingDeskIdValidator extends FieldValidatorBase {

	/** Error code if validation fails. */
	static final int ERROR_CODE = 2011;
	
	/** Error code if validation fails. */
	static final int FIELD_ERROR_CODE = 1011;
	
	/** Error message if validation fails. */
	static final String FIELD_ERROR_DESC = "Quotation Trading Desk does not match request";
	
	/** The party data loaded from the DB. */
	private ISapPartyData partyData;
	
	/**
	 * Instantiates a new business unit code validator.
	 *
	 * @param currentPartyData the current party data
	 */
	public TradingDeskIdValidator(final Context currentContext, final ISapPartyData currentPartyData) {
		super(currentContext);
		partyData = currentPartyData;
		
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.TRADINGDESK_ID.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		// Validate that the field is present
		if (value == null || value.length() == 0) {
			PluginLog.error("Error validating field " + getFieldName() + " data is missing or empty.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		
		if (value.equals(partyData.getInternalParty().getInputSapId())) {
			if (partyData.getInternalParty().getLegalEntity() == null ||  partyData.getInternalParty().getLegalEntity().length() == 0) {
				PluginLog.error("Error validating field " + getFieldName() + " data is invalid, no mapping found to Endur BU.");
				throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));				
			}
			
		} else {
			PluginLog.error("Error validating field " + getFieldName() + " data is invalid, no mapping found to Endur BU.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#
	 * validate(java.lang.String, com.olf.jm.coverage.businessObjects.ICoverageTrade)
	 */
	@Override
	public void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		Table party = null;
		String message = "Error validating field " + getFieldName()
				+ " Quotation Trading Desk doesn't match request";
		try {
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			if (existingTrade.isValid()) {
				String intBUOnTrade = coverageTrade.getQuotationIntBU();

				if (null != intBUOnTrade && !intBUOnTrade.isEmpty()) {

					String sql = "select p_bu.short_name AS int_bu"
							+ " FROM party p_bu JOIN party_info_view  piv"
							+ " ON p_bu.party_id = piv.party_id"
							+ " WHERE piv.value = '" + value +"'"
							+ " AND p_bu.short_name = '" + intBUOnTrade + "' "
							+ " AND piv.type_name = 'SAP Desk Location' ";

					PluginLog.debug("About to run SQL. \n" + sql);
					party = Utility.runSql(sql);
					if (party.getRowCount() <= 0) {
						PluginLog.error(message);
						throw new ValidatorException(buildErrorMessage(
								getExistingFieldErrorCode(),
								getExistingFieldErrorDesc()));

					}

				}
			}
		} catch (ValidatorException exp) {
			throw exp;
		} catch (Exception exp) {
			PluginLog.error(exp.getMessage());
			throw new ValidatorException(buildErrorMessage(0, exp.getMessage()));
		} finally {
			if (party != null)
				party.dispose();
		}
	}

	/**
	 * Gets the error message if validation with quotation failed
	 * 
	 * @return String - the error message
	 */
	private String getExistingFieldErrorDesc() {

		return FIELD_ERROR_DESC;
	}

	/**
	 * Gets the error code if validation with quotation failed
	 * 
	 * @return int - the error code
	 */
	private int getExistingFieldErrorCode() {

		return FIELD_ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		return ERROR_CODE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return "Invalid internal business unit code";
	}

}
