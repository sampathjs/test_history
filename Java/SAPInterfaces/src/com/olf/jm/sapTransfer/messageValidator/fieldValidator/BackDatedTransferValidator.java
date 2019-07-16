/**
 * This lass is ised to validated the SAP request for back dated transfers.
 * Transfers are considered back dated if the value date is less than contract date 
 * and Metal statement has been run for that month
 * */

package com.olf.jm.sapTransfer.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.ITwoFieldValidator;
import com.olf.jm.SapInterface.util.Utility;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/**
 * The Class ValueDate. Validate the input message field ValueDate.
 */
public class BackDatedTransferValidator extends FieldValidatorBase implements
		ITwoFieldValidator {

	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 3012;

	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Comparison between Value Date and Approval Date failed.";

	public BackDatedTransferValidator(Context context) {
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
		return EnumSapTransferRequest.VALUE_DATE.getColumnName();
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
		//No implementation needed for this method, as backdated trabsfers can be validated
		//for an existing quote only.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator#
	 * validate(java.lang.String,
	 * com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	public final void validate(final String value,
			final ISapEndurTrade existingTrade) throws ValidatorException {
		//No Implementation is needed.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase
	 * #getFieldErrorCode()
	 */
	@Override
	protected final int getFieldErrorCode() {
		return FIELD_ERROR_CODE;
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
		return FIELD_ERROR_DESCRIPTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator#
	 * validate(java.lang.String,
	 * com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	public void validate(String firstValue, String secondValue) throws ValidatorException {

		try {
			int metalStmtRun = getLastMetalStmtRunDate();
			if (metalStmtRun != 0) {
				isBackdated(firstValue, secondValue, metalStmtRun);
			}

		} catch (Exception exp) {
			String message = "Error validating backdated transfers " + exp.getMessage();
			PluginLog.error(message);
			throw new RuntimeException(message, exp.getCause());
		}

	}

	/**
	 * Method to check if a transfer is backdated. A transfer is backdated if
	 * valueDate is less than than Approval date and Metal transfers statement
	 * has been run for that month.
	 * 
	 * @param String
	 *            the value Date
	 * @param String
	 *            the Approval Date
	 * @param Table
	 *            table containing the latest month for which Metal statement
	 *            was run
	 * 
	 */
	private void isBackdated(String valueDate, String ApprovalDate, int jdStmtRunDate) throws ValidatorException, OException {

		int jdValueDate = OCalendar.parseString(valueDate);
		int jdApprovalDate = OCalendar.parseString(ApprovalDate);
		if ((jdValueDate <= jdApprovalDate)) {
			int valueDateSOM = OCalendar.getSOM(jdValueDate);
			int stmtRunDateSOM = OCalendar.getSOM(jdStmtRunDate);
			if (valueDateSOM <= stmtRunDateSOM) {
				PluginLog.error("Value Date is  " + getFieldName() + valueDate + " less than Approval Date " + getOtherFieldName() + ApprovalDate
						+ " not allowed when Metal statement has been run for the month \n");
				throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
			}
		}

	}

	/**
	 * Method to retrieve the latest Month for whichi Metal transfers statement
	 * has been run.
	 * 
	 * @return Table - containing the Month and Year for which latest metal
	 *         transfer statements has been run.
	 * 
	 */
	private int getLastMetalStmtRunDate() {
		Table metalStmtRun = null;
		int jdStmtRunDate = 0;
		try {
			String sql = " SELECT TOP 1 statement_period" + " FROM USER_jm_monthly_metal_statement" + " ORDER BY metal_statement_production_date  DESC";
			PluginLog.debug("Running SQL \n. " + sql);
			metalStmtRun = Utility.runSql(sql);
			if (metalStmtRun.getRowCount() < 1) {
				String message = "\n could not retrieve latest metal statement run date from USER_jm_monthly_metal_statment";
				PluginLog.error(message);
				throw new RuntimeException(message);
			}

			String StmtRunDate = metalStmtRun.getString(0, 0);
			jdStmtRunDate = OCalendar.parseString(StmtRunDate);
			PluginLog.info("\n Latets Metal Statement Run date " + jdStmtRunDate);

		} catch (Exception exp) {
			String message = "Error While loading data from USER_jm_monthly_metal_statment" + exp.getMessage();
			PluginLog.error(message);
			throw new RuntimeException(message, exp.getCause());
		}
		return jdStmtRunDate;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.coverage.messageValidator.fieldValidator.FieldValidatorBase
	 * #getOtherFieldName()
	 */
	public String getOtherFieldName() {
		return EnumSapTransferRequest.APPROVAL_DATE.getColumnName();
	}

}
