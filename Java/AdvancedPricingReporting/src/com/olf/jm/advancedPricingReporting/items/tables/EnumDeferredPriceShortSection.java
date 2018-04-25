package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-03-21 - V0.2 - scurran - add column zero tolerance check
 */

public enum EnumDeferredPriceShortSection implements TableColumn {

	METAL_SHORT_NAME("metal_short_name", EnumColType.String, EnumFormatType.FMT_NONE),
	DEFERRED_SHORT("deferred_short", EnumColType.Double, EnumFormatType.FMT_2DP);


	
    /** The column name. */
    private String columnName;
    
    /** The column type. */
    private EnumColType columnType;
    
    /** The column formatting type */
    private EnumFormatType formatType;
    
    /**
     * Instantiates a new enum transfer data.
     *
     * @param name the column name
     * @param type the column type
     * @param format the column type
     */
    EnumDeferredPriceShortSection(final String name, final EnumColType type, EnumFormatType format) {
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
