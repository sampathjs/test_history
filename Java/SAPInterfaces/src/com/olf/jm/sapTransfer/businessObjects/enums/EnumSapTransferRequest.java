package com.olf.jm.sapTransfer.businessObjects.enums;

import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.openrisk.table.EnumColType;

// TODO: Auto-generated Javadoc
/** Enum representing the columns in the transfer request message. */
public enum EnumSapTransferRequest implements ITableColumn {
	
	
	/** The trading desk id column. */
	TRADING_DESK_ID("mtreq:TradingDeskID", EnumColType.String, true),
	
	/** The metal transfer request number column. */
	METAL_TRANSFER_REQUEST_NUMBER("mtreq:MetalTransferRequestNumber", EnumColType.String, true),
	
	/** The element code column. */
	ELEMENT_CODE("mtreq:ElementCode", EnumColType.String, true),
	
	/** The weight column. */
	WEIGHT("mtreq:Weight", EnumColType.String, true),
	
	/** The unit of measure column. */
	UOM("mtreq:UOM", EnumColType.String, true),
	
	/** The document date column. */
	DOCUMENT_DATE("mtreq:DocumentDate", EnumColType.String, false),
	
	/** The value date column. */
	VALUE_DATE("mtreq:ValueDate", EnumColType.String, true),
	
	/** The from account number column. */
	FROM_ACCOUNT_NUMBER("mtreq:FromAccountNumber", EnumColType.String, true),
	
	/** The from company code column. */
	FROM_COMPANY_CODE("mtreq:FromCompanyCode", EnumColType.String, true),
	
	/** The from segment column. */
	FROM_SEGMENT("mtreq:FromSegment", EnumColType.String, true),
	
	/** The to account number column. */
	TO_ACCOUNT_NUMBER("mtreq:ToAccountNumber", EnumColType.String, false),
	
	/** The to company code column. */
	TO_COMPANY_CODE("mtreq:ToCompanyCode", EnumColType.String, false),
	
	/** The to segment column. */
	TO_SEGMENT("mtreq:ToSegment", EnumColType.String, false),
	
	/** The third party instructions text column. */
	THIRD_PARTY_INSTRUCTIONS_TEXT("mtreq:ThirdPartyInstructionsText", EnumColType.String, false),
	
	/** The third party reference text column. */
	THIRD_PARTY_REFERENCE_TEXT("mtreq:ThirdPartyReferenceText", EnumColType.String, false),
	
	/** The comment text column. */
	COMMENT_TEXT("mtreq:CommentText", EnumColType.String, false),
	
	/** The creation date column. */
	CREATION_DATE("mtreq:CreationDate", EnumColType.String, true),
	
	/** The creation time column. */
	CREATION_TIME("mtreq:CreationTime", EnumColType.String, true),
	
	/** The approval date column. */
	APPROVAL_DATE("mtreq:ApprovalDate", EnumColType.String, true),
	
	/** The transfer destination type column. */
	TRANSFER_DESTINATION_TYPE("mtreq:TransferDestinationType", EnumColType.String, true),
	
	/** The approval user column. */
	APPROVAL_USER("mtreq:ApprovalUser", EnumColType.String, true);

	/** The column name. */
	private String columnName;

	/** The column type. */
	private EnumColType columnType;
	
	/** Indicates that the field is required. */
	private boolean requiredColumn;

	/**
	 * Instantiates a new sap coverage trade columns.
	 *
	 * @param newColumnName            the new column name
	 * @param newColumnType            the new column type
	 * @param newRequiredField         indicating if the column is required 
	 */
	EnumSapTransferRequest(final String newColumnName,
			final EnumColType newColumnType, final boolean newRequiredField) {
		columnName = newColumnName;
		columnType = newColumnType;
		requiredColumn = newRequiredField;
		
		
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.tables.ITableColumns#getColumnName()
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
