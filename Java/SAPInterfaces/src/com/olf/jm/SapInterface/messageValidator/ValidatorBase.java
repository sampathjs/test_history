package com.olf.jm.SapInterface.messageValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.RequestData;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.ITwoFieldValidator;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.jm.sqlInjection.SqlInjectionFilter;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/**
 * The Class ValidatorBase used to validate the fields in a inbound message.
 */
public abstract class ValidatorBase implements IMessageValidator {
	
	/** The Constant STRUCTURE_ERROR. Error code returned if issues found with the message structure.*/
	private static final int STRUCTURE_ERROR = 8001;

	/** The context the script is currently running in. */
	protected Context context;

	/** The validators used to validate the message. */
	protected ArrayList<IFieldValidator> validators;
	
	/** The validators used to validate the message. */
	protected ArrayList<ITwoFieldValidator> twoFieldValidators;
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olf.jm.SapInterface.messageValidator.IMessageValidator#
	 * validate(com.olf.embedded.connex.RequestData,
	 * com.olf.jm.SapInterface.businessObjects.ISapEndurTrade,
	 * com.olf.jm.coverage.businessObjects.dataFactories.ISapPartyData,
	 * com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData)
	 */
	@Override
	public final void validate(final RequestData requestData,
			final ISapEndurTrade trade,
			final ISapPartyData currentSapPartyData,
			final ISapTemplateData currentSapTemplateData)
			throws ValidatorException {

		Table inputData = requestData.getInputTable();
		String columnName = inputData.getColumnNames();
		initValidators(currentSapPartyData, currentSapTemplateData);
		initTwoFieldValidators();
		for (IFieldValidator validator : validators) {

			String valueToCheck = "";
			if (columnName.contains(validator.getFieldName())) {
				valueToCheck = inputData.getString(validator.getFieldName(),0);
			} else {
				Logging.info(buildErrorMessage(STRUCTURE_ERROR, "column " + validator.getFieldName() 
						+ " is not present, setting value to empty string"));
			}

			validator.validate(valueToCheck);

			validator.validate(valueToCheck, trade);

		}
		//Two field validtor can be null for coverage Trades
		if(twoFieldValidators != null){
			for (ITwoFieldValidator twoFieldValidator : twoFieldValidators) {
				String valueToCheck = "";
				String OthervalueToCheck = "";
				if (columnName.contains(twoFieldValidator.getFieldName()) ||
						columnName.contains(twoFieldValidator.getOtherFieldName())) {
					valueToCheck = inputData.getString(twoFieldValidator.getFieldName(),	0);
					OthervalueToCheck = inputData.getString(twoFieldValidator.getOtherFieldName(),	0);
				} else {
					Logging.info(buildErrorMessage(STRUCTURE_ERROR, "column " + twoFieldValidator.getFieldName() 
							+ " or " + twoFieldValidator.getOtherFieldName()
							+ " is not present, setting value to empty string"));
				}

				twoFieldValidator.validate(valueToCheck, OthervalueToCheck);

			}	
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olf.jm.SapInterface.messageValidator.IMessageValidator#
	 * validateMessageStructure(com.olf.embedded.connex.RequestData)
	 */
	@Override
	public final void validateMessageStructure(final RequestData requestData)
			throws ValidatorException {
		Table inputData = requestData.getInputTable();

		// Check that the input table contains a single row
		if (inputData.getRowCount() != 1) {
			throw new ValidatorException(buildErrorMessage(STRUCTURE_ERROR,
					"Input message does not contian and data."));
		}

		SqlInjectionFilter sqlInjectionFilter = new SqlInjectionFilter();

		for (ITableColumn column : getColumns()) {
			String columnName = column.getColumnName();

			if (column.isRequiredField()) {
				try {
					if (inputData.getColumnId(columnName) < 0) {
						throw new ValidatorException(buildErrorMessage(STRUCTURE_ERROR, "Required column "
								+ columnName + " is not present in the message."));
					}

					if (inputData.getString(columnName, 0) == null
							|| inputData.getString(columnName, 0).length() <= 0) {
						throw new ValidatorException(buildErrorMessage(STRUCTURE_ERROR,
								"Required column "
										+ columnName
										+ " is present in the message but contains no data."));
					}
				} catch (Exception e) {
					throw new ValidatorException(buildErrorMessage(STRUCTURE_ERROR,
							"Error validating column "
									+ columnName
									+ ". " + e.getLocalizedMessage()));
				}
			}
		}
		
		// Check for SQL injection
		for(int columnId = 0; columnId < inputData.getColumnCount(); columnId++) {
		try {
			if (inputData.getString(columnId, 0) != null && inputData.getString(columnId, 0).length() > 0) {
				String parameter = inputData.getString(columnId, 0);
				
				sqlInjectionFilter.doFilter(parameter);
			}
		} catch (Exception e) {
			throw new ValidatorException(buildErrorMessage(STRUCTURE_ERROR,
					"Error validating column "
							+ inputData.getColumnName(columnId)
							+ ". " + e.getLocalizedMessage()));				
		}
		}
	}
	
	/**
	 * Builds the error message.
	 *
	 * @param errorId the error id
	 * @param errorDescription the error description
	 * @return the string
	 */
	protected final String buildErrorMessage(final int errorId, final String errorDescription) {
		
		StringBuilder errorString = new StringBuilder();
		
		errorString.append("Status Code [");
		errorString.append(errorId);
		errorString.append("] Message Text [");
		errorString.append(errorDescription);
		errorString.append("]");
		return errorString.toString();
	}

	/**
	 * Initialise the validators used in the validation of the message.
	 *
	 * @param currentSapPartyData            the current sap party data
	 * @param currentTemplateData 			 the current template data
	 */
	protected abstract void initValidators(ISapPartyData currentSapPartyData, ISapTemplateData currentTemplateData);

	/**
	 * Get a list of the columns in the inbound mesasge.
	 * 
	 * @return collection of ITableColumn objects defining the mesasge.
	 */
	protected abstract ITableColumn[] getColumns();

	protected abstract void initTwoFieldValidators();
		// TODO Auto-generated method stub
		
	
}