package com.olf.jm.blockbackdateddealentry.model;

import com.olf.openrisk.table.EnumColType;

/*
 * History: 
 * 2015-07-01	V1.0	jwaechter 	- Initial version 
 */

/**
 * Enum containing meta data about some columns of the user table USER_jm_monthly_metal_statement
 * @author jwaechter
 * @version 1.0
 */
public enum UTMonthlyMtlStmtGenCols implements UserTableCols {
	REGION ("region", "Region", EnumColType.String),
	BUSINESS_UNIT ("internal_bunit", "Business Unit", EnumColType.String),
	PROD_DATE ("metal_statement_production_date", "Latest Metal Statement Production Date", EnumColType.Int)
	;
	
	private final String colName;
	private final String colTitle;
	private final EnumColType colType;

	private UTMonthlyMtlStmtGenCols (final String colName, final String colTitle, final EnumColType colType) {
		this.colName = colName;
		this.colTitle = colTitle;
		this.colType = colType;
	}

	public String getColName() {
		return colName;
	}

	public String getColTitle() {
		return colTitle;
	}

	public EnumColType getColType() {
		return colType;
	}
}
