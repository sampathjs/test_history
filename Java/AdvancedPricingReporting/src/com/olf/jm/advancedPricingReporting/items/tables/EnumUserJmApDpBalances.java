package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information
*/

public enum EnumUserJmApDpBalances implements TableColumn {


	CUSTOMER_ID("customer_id", EnumColType.Int, EnumFormatType.FMT_NONE),	
	OPEN_DATE("open_date", EnumColType.DateTime, EnumFormatType.FMT_DATE),
	CLOSE_DATE("close_date", EnumColType.DateTime, EnumFormatType.FMT_DATE),
	DOLLAR_BALANCE("prevdaydollar_balance", EnumColType.Double, EnumFormatType.FMT_2DP),
	TOTAL_BALANCE("todaysdollar_balance", EnumColType.Double, EnumFormatType.FMT_2DP);
	
    /** The column name. */
    private final String columnName;
    
    /** The column type. */
    private final EnumColType columnType;
    
    /** The column formatting type */
    private final EnumFormatType formatType;
    
    /**
     * Instantiates a new enum transfer data.
     *
     * @param name the column name
     * @param type the column type
     * @param format the column type
     */
    EnumUserJmApDpBalances(final String name, final EnumColType type, EnumFormatType format) {
        this.columnName = name;
        this.columnType = type;
        this.formatType = format;
    }

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public EnumColType getColumnType() {
		return columnType;
	}
	
	@Override
	public EnumFormatType getFormatType() {
		return formatType;
	}

	@Override
	public boolean applyToleranceCheck() {
		return false;
	}	
}
