package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information
  * 2018-03-21 - V0.3 - scurran - add column zero tolerance check
  */

public enum EnumFinalBalanceSection implements TableColumn {

	DOLLAR_BALANCE("dollar_balance", EnumColType.Double, EnumFormatType.FMT_2DP),	
	DEPOSIT_USD("deposit_usd", EnumColType.Double, EnumFormatType.FMT_2DP),
	DEPOSIT_HKD("deposit_hkd", EnumColType.Double, EnumFormatType.FMT_2DP),	
	SQUARED_METAL_POSITION("squared_metal_position", EnumColType.Table, EnumFormatType.FMT_NONE),
	COLLECTED_DP_METAL("collected_dp_metal", EnumColType.Double, EnumFormatType.FMT_2DP),
	COLLECTED_AP_METAL("collected_ap_metal", EnumColType.Double, EnumFormatType.FMT_2DP),	
	DAILY_INTEREST("daily_interest", EnumColType.Double, EnumFormatType.FMT_2DP),
	TODAYS_BALANCE("todays_balance", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_VALUE("tier1_percent_value ", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_VALUE("tier2_percent_value ", EnumColType.Double, EnumFormatType.FMT_2DP),
	DEFERRED_PRICING_SHORT("deferred_pricing_short", EnumColType.Table, EnumFormatType.FMT_NONE),	
	TOTAL_MARGIN("total_margin", EnumColType.Double, EnumFormatType.FMT_2DP),
	DIFFERENCE("difference", EnumColType.Double, EnumFormatType.FMT_2DP),
	AP_METAL_NAME("ap_metal_name",EnumColType.String, EnumFormatType.FMT_NONE),
	DP_METAL_NAME("dp_metal_name",EnumColType.String, EnumFormatType.FMT_NONE);
	
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
    EnumFinalBalanceSection(final String name, final EnumColType type, EnumFormatType format) {
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
