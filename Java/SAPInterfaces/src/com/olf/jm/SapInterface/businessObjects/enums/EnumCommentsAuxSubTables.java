package com.olf.jm.SapInterface.businessObjects.enums;

/**
 * The Enum EnumCommentsAuxSubTables representing the aux data sub table holding deal comments.
 */
public enum EnumCommentsAuxSubTables {
	
	/** The comments. */
	COMMENTS("Comments");
	
	/** The table name. */
	private String tableName;

	

	/**
	 * Instantiates a new enum comments aux sub tables.
	 *
	 * @param newTableName the new table name
	 */
	EnumCommentsAuxSubTables(final String newTableName) {
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
