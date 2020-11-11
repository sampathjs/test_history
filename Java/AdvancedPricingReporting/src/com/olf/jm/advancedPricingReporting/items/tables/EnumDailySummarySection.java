package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information
 * 2018-03-21 - V0.3 - scurran - add column zero tolerance check
 */

public enum EnumDailySummarySection implements TableColumn {

	CUSTOMER_NAME("customer_name", EnumColType.String, EnumFormatType.FMT_NONE),
	CUSTOMER_ID("customer_id", EnumColType.Int, EnumFormatType.FMT_NONE),
	AG_AP_TOZ("ag_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	AU_AP_TOZ("au_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	IR_AP_TOZ("it_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	OS_AP_TOZ("os_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	PD_AP_TOZ("pd_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	PT_AP_TOZ("pt_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	RH_AP_TOZ("rh_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	RU_AP_TOZ("ru_ap_toz", EnumColType.Double, EnumFormatType.FMT_2DP),	
	TIER_1_AP_MARGIN_CALL_PERCENT("tier1_ap_margin_call_percentage", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_AP_MARGIN_CALL("tier1_ap_margin_call", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_AP_MARGIN_CALL_PERCENT("tier2_ap_margin_call_percentage", EnumColType.Double, EnumFormatType.FMT_2DP),	
	TIER_2_AP_MARGIN_CALL("tier2_ap_margin_call", EnumColType.Double, EnumFormatType.FMT_2DP),	
	AG_DP_TOZ("ag_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	AU_DP_TOZ("au_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	IR_DP_TOZ("it_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	OS_DP_TOZ("os_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	PD_DP_TOZ("pd_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	PT_DP_TOZ("pt_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	RH_DP_TOZ("rh_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	RU_DP_TOZ("ru_dp_toz", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_DP_MARGIN_CALL_PERCENT("tier1_dp_margin_call_percentage", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_1_DP_MARGIN_CALL("tier1_dp_margin_call", EnumColType.Double, EnumFormatType.FMT_2DP),
	TIER_2_DP_MARGIN_CALL_PERCENT("tier2_dp_margin_call_percentage", EnumColType.Double, EnumFormatType.FMT_2DP),	
	TIER_2_DP_MARGIN_CALL("tier2_dp_margin_call", EnumColType.Double, EnumFormatType.FMT_2DP),	
	END_CASH_BALANCE("end_cash_balance", EnumColType.Double, EnumFormatType.FMT_2DP),
	AU_DP_SETTLE_VALUE("au_dp_settle_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	IR_DP_SETTLE_VALUE("ir_dp_settle_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	PD_DP_SETTLE_VALUE("pd_dp_settle_value", EnumColType.Double, EnumFormatType.FMT_2DP),
	PT_DP_SETTLE_VALUE("pt_dp_settle_value", EnumColType.Double, EnumFormatType.FMT_2DP);

	

    /** The column formatting type */
    private final EnumFormatType formatType;


    /** The column name. */
    private final String columnName;
    
    /** The column type. */
    private final EnumColType columnType;
    

    /**
     * Instantiates a new enum transfer data.
     *
     * @param name the column name
     * @param type the column type
     * @param format the column type
     */
    EnumDailySummarySection(final String name, final EnumColType type, EnumFormatType format) {
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
