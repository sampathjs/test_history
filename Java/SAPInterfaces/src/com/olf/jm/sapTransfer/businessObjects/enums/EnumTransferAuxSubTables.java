package com.olf.jm.sapTransfer.businessObjects.enums;


/**
 * The Enum EnumTransferAuxSubTables representing the aux data sub table holding deal comments.
 */
public enum EnumTransferAuxSubTables {
	
	/** The comments. */
	COMMENTS("Comments");
	
	/** The table name. */
	private String tableName;

	

	/**
	 * Instantiates a new enum transfer aux sub tables.
	 *
	 * @param newTableName the new table name
	 */
	EnumTransferAuxSubTables(final String newTableName) {
		tableName = newTableName;

	}
	
	/**
	 * Gets the table name.
	 *
	 * @return the table name
	 */
	public String getTableName() {
		return tableName;
	}
}
