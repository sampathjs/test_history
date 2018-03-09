package com.olf.jm.sapTransfer.messageValidator.fieldValidator;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/**
 * The Class MetalTransferRequestNumberValidator.
 */
public class MetalTransferRequestNumberValidator extends FieldValidatorBase {
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 3005;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Metal Transfer Request Number failed validation.";
	
	/** The Constant SQL_TEMPLATE. */
	private static final String SQL_TEMPLATE = 
			  " SELECT deal_tracking_num, tran_status from ab_tran_info_view abtiv "
			+ " JOIN ab_tran ab ON ab.tran_num = abtiv.tran_num AND ins_type = 66000 AND tran_status in (1,2,3,7) "
			+ " WHERE type_name = 'SAP-MTRNo' " 
			+ " AND value = '%s' ";
	
	/**
	 * Instantiates a new metal transfer request number validator.
	 *
	 * @param currentContext the current context
	 */
	public MetalTransferRequestNumberValidator(final Context currentContext) {
		super(currentContext);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapTransferRequest.METAL_TRANSFER_REQUEST_NUMBER.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		String sql = String.format(SQL_TEMPLATE, value);
		
		try (Table deal = runSql(sql)) {
			if (deal != null && deal.getRowCount() > 0) {
				PluginLog.error("Error validating field " + getFieldName() + " found existing deal for transfer id " + value + ".");
				for (int row = 0; row < deal.getRowCount(); row++) {
					PluginLog.error("Found deal " + deal.getInt("deal_tracking_num", row) + " status " + deal.getInt("tran_status", row));
				}
				throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#
	 * validate(java.lang.String, com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	public void validate(final String value, final ISapEndurTrade existingTrade)
			throws ValidatorException {
		
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
	
	/**
	 * Helper method to run sql statements..
	 *
	 * @param sql the sql to execute
	 * @return the table containing the sql output
	 */
	private Table runSql(final String sql) {
		
		IOFactory iof = context.getIOFactory();
	   
		PluginLog.debug("About to run SQL. \n" + sql);
		
		
		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
				
		return t;
		
	}	

}
