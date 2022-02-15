package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information
 * 2018-03-21 - V0.3 - scurran - add column zero tolerance check
*/

public enum EnumDeferredPricingData implements TableColumn {

	
	
	
	TRADE_DATE("trade_date",EnumColType.DateTime, EnumFormatType.FMT_DATE),
	DEAL_NUM("deal_num",  EnumColType.Int, EnumFormatType.FMT_NONE),
	REFERENCE("reference", EnumColType.String, EnumFormatType.FMT_NONE),
	VOLUME_IN_TOZ("volume_in_toz", EnumColType.Double, EnumFormatType.FMT_3DP, true),
	VOLUME_IN_GMS("volume_in_gms", EnumColType.Double, EnumFormatType.FMT_2DP, true),
	TRADE_PRICE("trade_price", EnumColType.Double, EnumFormatType.FMT_2DP),
	FIXED_DATE("fixed_date", EnumColType.DateTime, EnumFormatType.FMT_DATE),
	SETTLEMENT_VALUE("settlement_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	TYPE("type", EnumColType.String, EnumFormatType.FMT_NONE);
	
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
     */
    EnumDeferredPricingData(final String name, final EnumColType type, EnumFormatType format) {
        this.columnName = name;
        this.columnType = type;
        this.formatType = format;
        this.toleranceCheck = false;
    }
    
    EnumDeferredPricingData(final String name, final EnumColType type, EnumFormatType format, boolean toleranceCheck) {
        this.columnName = name;
        this.columnType = type;
        this.formatType = format;
        this.toleranceCheck = toleranceCheck;
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
		return toleranceCheck;
	}	
	
}
