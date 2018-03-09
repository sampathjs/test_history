package com.olf.jm.SapInterface.businessObjects.enums;

import com.olf.openrisk.table.EnumColType;


/** Enum defining the columns in the comment table. */
public enum EnumCommentColumns {
	/** Comment text column. */
	COMMENT_TEXT("comment_text", EnumColType.String, true),
	
	/** Comment type column. */
	COMMENT_TYPE("comment_type", EnumColType.String, true);	
	
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
	 * @param newRequiredField         is the field required
	 */
	 EnumCommentColumns(final String newColumnName,
			final EnumColType newColumnType, final boolean newRequiredField) {
		columnName = newColumnName;
		columnType = newColumnType;
		requiredColumn = newRequiredField;
		
		
	}



	/**
	 * Gets the column name.
	 *
	 * @return the column name
	 */
	public String getColumnName() {
		return columnName;
	}



	/**
	 * Gets the column type.
	 *
	 * @return the column type
	 */
	public EnumColType getColumnType() {
		return columnType;
	}


	/**
	 * Get flag indicating if the field is required.
	 *
	 * @return true, if the field is required
	 */
	public boolean isRequiredField() {
		return requiredColumn;
	}
}
