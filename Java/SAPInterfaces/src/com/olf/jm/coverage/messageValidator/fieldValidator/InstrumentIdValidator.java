package com.olf.jm.coverage.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.SapInterface.util.Utility;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;



/**
 * The Class InstrumentIdValidator. Validate the instrument id in the inbound message.
 */
public class InstrumentIdValidator extends FieldValidatorBase {
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 2200;
	/** The Constant TRAN_ERROR_CODE. */
	private static final int TRAN_ERROR_CODE = 1009;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid instrument id";
	
	/** Template date loaded from the db. */
	private ISapTemplateData templateData;

	/** The Constant TRAN_ERROR_DESCRIPTION. */
	private static final String TRAN_ERROR_DESCRIPTION = "Quotation Instrument Id does not match request.";
	/**
	 * Instantiates a new instrument id validator.
	 *
	 * @param currentContext the current context
	 * @param currentTemplateData the current template data
	 */
	public InstrumentIdValidator(final Context currentContext, final ISapTemplateData currentTemplateData) {
		super(currentContext);
		templateData = currentTemplateData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapCoverageRequest.INSTRUMENT_ID.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		if (!value.equalsIgnoreCase(templateData.getSapInstrumentId())) {
			PluginLog.error("Error validating the instrument id, value loaded from db " + templateData.getSapInstrumentId()  
					+ " does not match message value " + value);
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));		
		}
		
		if (templateData.getTemplate() == null || templateData.getTemplate().length() == 0) {
			PluginLog.error("Error validating the instrument id, no data found for id " + value); 
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));	
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
	public void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		Table cflow = null;
		String message = "Error validating field " + getFieldName()
				+ " Quotation Instrument ID doesn’t match request";
		try {
			ICoverageTrade coverageTrade = (ICoverageTrade) existingTrade;
			if (coverageTrade.isValid()) {
				String cflowOnTrade = coverageTrade.getQuotationCflowType();
				if (null != cflowOnTrade && !cflowOnTrade.isEmpty()) {

					String sql = "select sap_inst_id"
							+ "  from USER_jm_sap_inst_map"
							+ " where cflow_type = '" + cflowOnTrade + "'"
							+ " AND sap_inst_id = '" + value + "'";

					PluginLog.debug("About to run SQL. \n" + sql);
					Utility util = new Utility(context);
					cflow = util.runSql(sql);
					if (cflow.getRowCount() <= 0) {
						PluginLog.error(message);
						throw new ValidatorException(buildErrorMessage(
								getTranErrorCode(), getTranErrorDescription()));

					}

				}
			}
		} catch (ValidatorException exp) {
			throw exp;
		} catch (Exception exp) {
			PluginLog.error(exp.getMessage());
			throw new ValidatorException(buildErrorMessage(0, exp.getMessage()));
		} finally {
			if (cflow != null)
				cflow.dispose();
		}
	}

	/**
	 * Gets the Trasaction Error Code
	 * 
	 * @return int - the error code
	 */
	public static int getTranErrorCode() {
		return TRAN_ERROR_CODE;
	}

	/**
	 * Gets the Transaction Error Message
	 * 
	 * @return String Error Message
	 */
	public static String getTranErrorDescription() {
		return TRAN_ERROR_DESCRIPTION;
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
