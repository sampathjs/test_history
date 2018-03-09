package com.olf.jm.blockbackdateddealentry.model;
 
/*
 * History: 
 * 2015-07-01	V1.0	jwaechter 	- Initial version 
 */

/**
 * Enum containing metadata about user tables relevant for the BlockBackDatedDealEntry.
 * @author jwaechter
 * @version 1.0
 */
public enum UserTables {
	MonthlyMetalStatementGen ("USER_jm_monthly_metal_statement", UTMonthlyMtlStmtGenCols.values());
	
	private final String name;
	private final UserTableCols cols[];
	
	private UserTables (final String name, final UserTableCols cols[]) {
		this.name = name;
		this.cols = cols;
	}

	public String getName() {
		return name;
	}

	public UserTableCols[] getCols() {
		return cols;
	}
}
