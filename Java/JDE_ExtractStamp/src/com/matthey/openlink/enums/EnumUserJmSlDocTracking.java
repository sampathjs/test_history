package com.matthey.openlink.enums;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2016-MM-DD	V1.0		- Initial Version 
 * 2018-01-10   V1.1    sma     - add logic of column sap_status  
 */
public enum EnumUserJmSlDocTracking implements ITableColumn {

	/** The document num. */
	DOCUMENT_NUM("document_num", EnumColType.Int, true),
	
	/** The SL extract status for this document. */
	SL_STATUS("sl_status", EnumColType.String, true),
	
	/** The SAP extract status for this document. */
	SAP_STATUS("sap_status", EnumColType.String, true),
	
	LAST_UPDATE("last_update", EnumColType.Int, true),
	
	PERSONNEL_ID("personnel_id", EnumColType.Int, true),
	
	DOC_STATUS("doc_status", EnumColType.Int, true),
	
	LAST_DOC_STATUS("last_doc_status", EnumColType.Int, true),
	
	DOC_VERSION("doc_version", EnumColType.Int, true),
	
	STLDOC_HDR_HIST_ID("stldoc_hdr_hist_id", EnumColType.Int, true);
	
	
	/** The column name. */
	private String columnName;

	/** The column type. */
	private EnumColType columnType;
	
	/** Indicates that the field is required. */
	private boolean requiredColumn;
	
	/**
	 * Instantiates a JM Interest PNL result table columns.
	 *
	 * @param newColumnName            the column name
	 * @param newColumnType            the  column type
	 * @param newRequiredField 			flag to indicate the field is required
	 */
	EnumUserJmSlDocTracking(final String newColumnName,
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
