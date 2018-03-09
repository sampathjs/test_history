package com.olf.jm.SapInterface.businessObjects.enums;



/**
 * The Enum EnumAuxDataTables.
 */
public enum EnumAuxDataTables {

	/** The coverage aux data. */
	COVERAGE_AUX_DATA("CoverageAuxData"),
	
	/** The transfer aux data. */
	TRANSFER_AUX_DATA("TransferAuxData");
	
	
	/** The table name. */
	private String tableName;

	

	/**
	 * Instantiates a new  aux data tables enum.
	 *
	 * @param newTableName the new table name
	 */
	EnumAuxDataTables(final String newTableName) {
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
