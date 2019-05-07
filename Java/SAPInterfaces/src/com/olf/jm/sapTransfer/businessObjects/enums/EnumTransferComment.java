
package com.olf.jm.sapTransfer.businessObjects.enums;

/**
 * The Enum EnumTransferAuxSubTables representing the aux data sub table holding
 * deal comments.
 */
public enum EnumTransferComment {

	/** From Account Comment. */
	FROM_ACCOUNT("From Account"),

	/** To Account Comment. */
	TO_ACCOUNT("To Account");

	/**
	 * Instantiates a new sap coverage trade columns.
	 * 
	 * @param newColumnName
	 *            the column name
	 * @param newColumnType
	 *            the column type
	 * @param newRequiredField
	 *            is the field required
	 */
	EnumTransferComment(final String commentType) {
		type = commentType;
	}

	/** The type. */
	private String type;

	/**
	 * Gets the type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}
}
