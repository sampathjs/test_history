package com.olf.jm.SapInterface.businessObjects.enums;

import com.olf.openrisk.table.EnumColType;


/**
 * The Enum EnumSapResponse. The response message sent back to SAP.
 */
public enum EnumSapResponse implements ITableColumn {
	
	// Only output tradeReferenceId as error code returned back via a exception and status
	// code is redundant.
	//STATUS_CODE("cdres:StatusCode", EnumColType.String, true),
	/** The trade reference id. */
	TRADE_REFERENCE_ID("TradeReferenceID", EnumColType.String, false);
	//REJECTED_REASON_TEXT("RejectedReasonText", EnumColType.String, false);	
	

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
	 * @param newColumnType            the  column type
	 * @param newRequiredField 			flag to indicate the field is required
	 */
	EnumSapResponse(final String newColumnName,
			final EnumColType newColumnType, final boolean newRequiredField) {
		columnName = newColumnName;
		columnType = newColumnType;
		requiredColumn = newRequiredField;
		
		
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.tables.ITableColumns#getColumnName()
	 */
	@Override
	public String getColumnName(final String nameSpace) {
		String qualifiedColumnName = nameSpace;
		if (!qualifiedColumnName.endsWith(":")) { 
			qualifiedColumnName = qualifiedColumnName + ":";
		}
		return qualifiedColumnName + columnName;
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

}
