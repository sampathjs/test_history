
package com.olf.jm.sapTransfer.messageValidator.fieldValidator;

import java.util.HashSet;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase;
import com.olf.jm.SapInterface.util.Utility;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/**
 * The Class MetalElementValidator. Validate the metal element code in the inbound message.
 */
public class TransferDestinationTypeValidator extends FieldValidatorBase {
	
	/** The Constant FIELD_ERROR_CODE. */
	private static final int FIELD_ERROR_CODE = 3014;
	
	/** The Constant FIELD_ERROR_DESCRIPTION. */
	private static final String FIELD_ERROR_DESCRIPTION = "Invalid Metal Transfer Type";

	/**
	 * Instantiates a new metal element validator.
	 *
	 * @param currentContext the current context
	 */
	public TransferDestinationTypeValidator(final Context currentContext) {
		super(currentContext);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#getFieldName()
	 */
	@Override
	public final String getFieldName() {
		return EnumSapTransferRequest.TRANSFER_DESTINATION_TYPE.getColumnName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase#validate(java.lang.String)
	 */
	@Override
	public final void validate(final String value) throws ValidatorException {
		if (value == null || value.length() == 0) {
			PluginLog.error("Error validating field " + getFieldName() + " data is missing or empty.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}else if(!validTransferDestType(value)){
			PluginLog.error("Error validating field " + getFieldName() + " Invalid Data.");
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		
		
	}

	/**
	 * Validates the Transfer destination type of request with transfer types
	 * configured in COnnex Mapings
	 * 
	 * @param value
	 * @return boolean
	 * @throws ValidatorException
	 */
	private boolean validTransferDestType(String value) throws ValidatorException {
		boolean isValid = false;
		try {
			HashSet<String> cnxDataMapping = loadConnexDataMapping();
			if (cnxDataMapping.contains(value)) {
				isValid = true;
			}

		} catch (Exception e) {
			String errorMessage = "Error validating Metal Transfer Type " + e.getMessage() + "\n";
			PluginLog.error(errorMessage);
			throw new ValidatorException(buildErrorMessage(getFieldErrorCode(), getFieldErrorDesc()));
		}
		return isValid;
	}

	/**
	 * loads the Transfer destination Type configured in the COnnex Mappings
	 * 
	 * @return Table
	 */
	private HashSet<String> loadConnexDataMapping() throws Exception {
		Table cnxDataMapping = null;
		HashSet<String> cnxDataSet;
		try {
			String sql = "SELECT" + " CASE WHEN ma.name IS NOT NULL" + " THEN ma.name" + " ELSE ts.name" + " END as alias" + " FROM oc_ref_map_data md"
					+ " JOIN oc_ext_ref_data  rd ON (md.ext_ref_id = rd.ext_ref_id )" + " JOIN TRANS_STATUS  ts on (md.int_ref_id = ts.trans_status_id)"
					+ " LEFT JOIN master_alias_table mat ON ( mat.name = 'TRANS_STATUS')"
					+ " LEFT JOIN master_aliases ma ON (ma.id_number = mat.id_number AND ts.trans_status_id = ma.ref_id)" + " WHERE rd.ext_ref_type_id = 20016"
					+ " AND md.ref_map_id = 20034";

			cnxDataMapping = Utility.runSql(sql);
			if (cnxDataMapping.getRowCount() < 1) {
				String errorMessage = "No Connex Mapping has been defined for Metal Transfer Type";
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
			PluginLog.info("Rows in Connex Reference Data Mapping  \n. " + cnxDataMapping.getRowCount());
			cnxDataSet = populateCnxMappingSet(cnxDataMapping);
		} catch (Exception exp) {
			PluginLog.error("Error Loading Connex Reference Data Mapping for Metal Transfer Type " + exp.getMessage());
			throw new RuntimeException("Error Loading Connex Reference Data Mapping for Metal Transfer Type " + exp.getMessage());
		}
		return cnxDataSet;
	}

	private HashSet<String> populateCnxMappingSet(Table cnxDataMapping) throws OException {

		HashSet<String> mappingSet = new HashSet<String>();
		int rowCount = cnxDataMapping.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			mappingSet.add(cnxDataMapping.getString("alias", row));
		}
		if (mappingSet == null || mappingSet.isEmpty()) {
			throw new RuntimeException("\n Error populating connex reference mapping");
		}
		return mappingSet;
	}

	/**
	 * Validate.
	 * 
	 * @param value
	 *            the value
	 * @param existingTrade
	 *            the existing trade
	 * @throws ValidatorException
	 *             the validator exception
	 */
	@Override
	public void validate(final String value, final ISapEndurTrade existingTrade) throws ValidatorException {
		// No Implementation is required.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase
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
	 * com.olf.jm.SapInterface.messageValidator.fieldValidator.FieldValidatorBase
	 * #getFieldErrorDesc()
	 */
	@Override
	protected final String getFieldErrorDesc() {
		return FIELD_ERROR_DESCRIPTION;
	}

}
