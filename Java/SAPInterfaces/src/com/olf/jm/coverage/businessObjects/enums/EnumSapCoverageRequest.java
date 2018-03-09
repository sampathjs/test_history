package com.olf.jm.coverage.businessObjects.enums;

import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.openrisk.table.EnumColType;


/**
 * The Enum EnumSapCoverageRequest. Defines the elements of a coverage request message.
 */
public enum EnumSapCoverageRequest implements ITableColumn {
	
	/** The coverage instruction number column. */
	COVERAGE_INSTRUCTION_NO("cdreq:CoverageInstructionNo", EnumColType.String, true),

	/** The element code column. */
	ELEMENT_CODE("cdreq:ElementCode", EnumColType.String, true),
	
	/** The company code column. */
	COMPANY_CODE("cdreq:CompanyCode", EnumColType.String, true),

	/** The business unit code column. */
	BUSINESSUNIT_CODE("cdreq:BusinessUnitCode", EnumColType.String, true),

	/** The trading desk id column. */
	TRADINGDESK_ID("cdreq:TradingDeskId", EnumColType.String, true),
	
	/** The account number column. */
	ACCOUNT_NUMBER("cdreq:AccountNumber", EnumColType.String, true),

	/** The instrument id column. */
	INSTRUMENT_ID("cdreq:InstrumentId", EnumColType.String, true),

	/** The time code column. */
	TIME_CODE("cdreq:TimeCode", EnumColType.String, false),

	/** The contract date column. */
	CONTRACT_DATE("cdreq:ContractDate", EnumColType.String, true),
	
	/** The value date column. */
	VALUE_DATE("cdreq:ValueDate", EnumColType.String, true),
	
	/** The weight column. */
	WEIGHT("cdreq:Weight", EnumColType.String, true),

	/** The weight unit of measure column. */
	WEIGHT_UOM("cdreq:WeightUOM", EnumColType.String, true),

	/** The buy sell flag column. */
	BUY_SELL_FLAG("cdreq:BuySellFlag", EnumColType.String, true),

	/** The currency code column. */
	CURRENCY_CODE("cdreq:CurrencyCode", EnumColType.String, true),

	/** The quote price column. */
	QUOTE_PRICE("cdreq:QuotePrice", EnumColType.String, false),

	/** The quote reference id column. */
	QUOTE_REFERENCE_ID("cdreq:QuoteReferenceId", EnumColType.String, false),

	/** The submission user id column. */
	SUBMISSION_USER_ID("cdreq:SubmissionUserId", EnumColType.String, true),

	/** The submission user name column. */
	SUBMISSION_USER_NAME("cdreq:SubmissionUserName", EnumColType.String, true),

	/** The submission date column. */
	SUBMISSION_DATE("cdreq:SubmissionDate", EnumColType.String, true),
	
	/** The submission time column. */
	SUBMISSION_TIME("cdreq:SubmissionTime", EnumColType.String, true),
	
	/** The comment text column. */
	COMMENT_TEXT("cdreq:CommentText", EnumColType.String, false),

	/** The counter party name column. */
	COUNTER_PARTY_NAME("cdreq:CounterPartyName", EnumColType.String, true);	
	
	
	

	/** The column name. */
	private String columnName;

	/** The column type. */
	private EnumColType columnType;
	
	/** Indicates that the field is required. */
	private boolean requiredColumn;

	/**
	 * Instantiates a new sap coverage trade columns.
	 *
	 * @param newColumnName            the column name
	 * @param newColumnType            the column type
	 * @param newRequiredField 		   is the field required
	 */
	EnumSapCoverageRequest(final String newColumnName,
			final EnumColType newColumnType, final boolean newRequiredField) {
		columnName = newColumnName;
		columnType = newColumnType;
		requiredColumn = newRequiredField;
		
		
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.enums.ITableColumn#getColumnName()
	 */
	@Override
	public String getColumnName() {
		return columnName;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.tables.ITableColumns#getColumnType()
	 */
	@Override
	public EnumColType getColumnType() {
		return columnType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.tables.ITableColumns#isRequiredField()
	 */
	@Override
	public boolean isRequiredField() {
		return requiredColumn;
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.enums.ITableColumn#getColumnName(java.lang.String)
	 */
	@Override
	public String getColumnName(final String nameSpace) {
		throw new RuntimeException("Error column names already contain name space.");
	}

}
