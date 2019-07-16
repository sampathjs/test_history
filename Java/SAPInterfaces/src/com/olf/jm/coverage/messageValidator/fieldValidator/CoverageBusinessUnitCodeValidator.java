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
 * The Class BusinessUnitCodeValidator. 
 * 
 * Validates that SAP business unit code. Field needs to be present and
 * contain a valid value as defined in the party info field Ext Business Unit Code
 */
public class CoverageBusinessUnitCodeValidator extends FieldValidatorBase {

	/** Error code if validation fails. */
	static final int ERROR_CODE = 2003;
	
	/** The Constant FIELD_ERROR_CODE. */
	static final int FIELD_ERROR_CODE = 1013;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	static final String FIELD_ERROR_DESC = "Quotation External Business Unit doesnot match request";
	
	/** The party data loaded from the DB. */
	private ISapPartyData partyData;
	
	/**
	 * Instantiates a new business unit code validator.
	 *
	 * @param currentPartyData the current party data
	 */
	public CoverageBusinessUnitCodeValidator(final ISapPartyData currentPartyData, Context context) {
		super(context);
		partyData = currentPartyData;
		
	}
	
	public CoverageBusinessUnitCodeValidator(Context context) {
		super(context);
	}
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.BUSINESSUNIT_CODE.getColumnName();
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
		
		if (value.equals(partyData.getExternalParty().getInputSapId())) {
			if (partyData.getExternalParty().getLegalEntity() == null ||  partyData.getExternalParty().getLegalEntity().length() == 0) {
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
		String message = "Error validating field " + getFieldName() + " Quotation External Business Unit doesn't match request";
		try {
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			if (coverageTrade.isValid()) {
				String extBUOnTrade = coverageTrade.getQuotationExtBU();
				if (null != extBUOnTrade && !extBUOnTrade.isEmpty()) {

					String sql = "SELECT pi.value" + " FROM party_info pi \n" + " JOIN party p ON pi.party_id = p.party_id \n" + " WHERE pi.type_id = 20015"
							+ " AND p.short_name = '" + extBUOnTrade + "' " + "AND pi.value = '" + value + "'";

					PluginLog.debug("About to run SQL. \n" + sql);
					party = Utility.runSql(sql);
					if (party.getRowCount() <= 0) {
						PluginLog.error(message);
						throw new ValidatorException(buildErrorMessage(getExistingFieldErrorCode(), getExistingFieldErrorDesc()));

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
		return "Invalid business unit code / account number combination.";
	}
	
	/**
	 * Method to return Error code
	 * 
	 * @return int the error code
	 */
	protected final int getExistingFieldErrorCode() {
		return FIELD_ERROR_CODE;
	}

	/**
	 * Method to return Error message
	 * 
	 * @return String the error message
	 */
	protected final String getExistingFieldErrorDesc() {
		return FIELD_ERROR_DESC;
	}


}
