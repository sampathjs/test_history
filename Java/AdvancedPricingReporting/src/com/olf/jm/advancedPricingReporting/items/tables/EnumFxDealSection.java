package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information
 * 2018-03-21 - V0.3 - scurran - add column zero tolerance check
 */

public enum EnumFxDealSection implements TableColumn {

	METAL_NAME("metal_name", EnumColType.String, EnumFormatType.FMT_NONE),
	METAL_SHORT_NAME("metal_short_name", EnumColType.String, EnumFormatType.FMT_NONE),
	TOTAL_SELL("total_sell", EnumColType.Double, EnumFormatType.FMT_2DP),
	TOTAL_BUY("total_buy", EnumColType.Double, EnumFormatType.FMT_2DP),
	LOSS_GAIN("loss_gain", EnumColType.Double, EnumFormatType.FMT_2DP),
	TOTAL_WEIGHT_TOZ("total_weight_toz", EnumColType.Double, EnumFormatType.FMT_3DP, true),
	TOTAL_WEIGHT_GMS("total_weight_gms", EnumColType.Double, EnumFormatType.FMT_2DP, true),	
	REPORT_DATA("report_data", EnumColType.Table, EnumFormatType.FMT_NONE);

	
    /** The column name. */
    private String columnName;
    
    /** The column type. */
    private EnumColType columnType;
    
    /** The column formatting type */
    private EnumFormatType formatType;
    
    /** Apply zero tolerance check to value */
    private boolean toleranceCheck;    
    
    /**
     * Instantiates a new enum transfer data.
     *
     * @param name the column name
     * @param type the column type
     * @param format the column type
     */
    EnumFxDealSection(final String name, final EnumColType type, EnumFormatType format) {
        this.columnName = name;
        this.columnType = type;
        this.formatType = format;
        this.toleranceCheck = false;
    }

	@Override
	public String getColumnName() {
		return columnName;
	}

    EnumFxDealSection(final String name, final EnumColType type, EnumFormatType format,  boolean toleranceCheck) {
        this.columnName = name;
        this.columnType = type;
        this.formatType = format;
        this.toleranceCheck = toleranceCheck;
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
		return toleranceCheck;
	}	
}
