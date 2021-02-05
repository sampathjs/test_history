/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information
 * 2018-03-21 - V0.3 - scurran - add column zero tolerance check
 */

public enum EnumDeferredPricingSection implements TableColumn {

	METAL_NAME("metal_name", EnumColType.String, EnumFormatType.FMT_NONE),
	METAL_SHORT_NAME("metal_short_name", EnumColType.String, EnumFormatType.FMT_NONE),
	MARKET_PRICE("market_price", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_DP_VALUE("tier1_dp_percent_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_DP_VALUE("tier2_dp_percent_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	TOTAL_DISPATCHED("total_dispatched", EnumColType.Double, EnumFormatType.FMT_2DP),
	TOTAL_UNFIXED("total_unfixed", EnumColType.Double, EnumFormatType.FMT_2DP),
	TOTAL_WEIGHT_TOZ("total_weight_toz", EnumColType.Double, EnumFormatType.FMT_3DP, true),
	TOTAL_WEIGHT_GMS("total_weight_gms", EnumColType.Double, EnumFormatType.FMT_2DP, true),
	TOTAL_DP_VALUE("total_dp_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_DP_PERCENTAGE("tier1_dp_percent", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_DP_PERCENTAGE("tier2_dp_percent", EnumColType.Double, EnumFormatType.FMT_2DP),	
	TIER_1_DP_MIN_VOL("tier1_dp_min_vol", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_DP_MAX_VOL("tier1_dp_max_vol", EnumColType.Double, EnumFormatType.FMT_2DP),	
	TIER_2_DP_MIN_VOL("tier2_dp_min_vol", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_DP_MAX_VOL("tier2_dp_max_vol", EnumColType.Double, EnumFormatType.FMT_2DP),	
	TIER_1_DP_VOL_USED("tier1_dp_vol_used", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_DP_VOL_USED("tier2_dp_vol_used", EnumColType.Double, EnumFormatType.FMT_2DP),	
	REPORT_DATA("report_data", EnumColType.Table, EnumFormatType.FMT_NONE);

	
    /** The column name. */
    private final String columnName;
    
    /** The column type. */
    private final EnumColType columnType;
    
    /** The column formatting type */
    private final EnumFormatType formatType;
    
    /** Apply zero tolerance check to value */
    private final boolean toleranceCheck;
    
    /**
     * Instantiates a new enum transfer data.
     *
     * @param name the column name
     * @param type the column type
     * @param format the column type
    */
    EnumDeferredPricingSection(final String name, final EnumColType type, EnumFormatType format) {
        this.columnName = name;
        this.columnType = type;
        this.formatType = format;
        this.toleranceCheck = false;
    }

    EnumDeferredPricingSection(final String name, final EnumColType type, EnumFormatType format, boolean toleranceCheck) {
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
