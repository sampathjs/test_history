package com.olf.jm.autosipopulation.model;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-05-27	V1.1	jwaechter 	- Added tran group
 * 2016-06-01	V1.2	jwaechter	- added OFFSET_TRAN_TYPE
 */

public enum EnumClientDataCol {
	OFFSET_TRAN_TYPE("offset_tran_type", EnumColType.String),
	TRANNUM("tran_num", EnumColType.Int),
	LEG_NUM("leg_num", EnumColType.Int),
	CCY_ID ("ccy_id", EnumColType.Int),
	INT_EXT ("int_ext", EnumColType.Int), 
	BUSINESS_UNIT_ID ("ext_bunit_id", EnumColType.Int),
	SETTLE_ID ("settle_id", EnumColType.Int)
	
	;
	
	private final String colName;	
	private final EnumColType colType;
	
	private EnumClientDataCol (final String colName, final EnumColType colType) {
		this.colName = colName;
		this.colType = colType;
	}

	public String getColName() {
		return colName;
	}

	public EnumColType getColType() {
		return colType;
	}
}
