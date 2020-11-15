package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealData;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.util.MarginFactory;
import com.olf.jm.advancedPricingReporting.util.MarginFactory.EnumPricingType;
import com.olf.jm.advancedPricingReporting.util.MarginFactory.MarginResults;
import com.olf.jm.advancedPricingReporting.util.MathUtils;
import com.olf.jm.advancedPricingReporting.util.PriceFactory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;

import java.util.Date;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DeferredPricingDeals. Generates the data needed for  the deferred pricing section. 
 */
public class DeferredPricingDeals extends ItemBase {

	
	/**
	 * Instantiates a new deferred pricing deals item.
	 *
	 * @param currentContext the current context
	 */
	public DeferredPricingDeals(Context currentContext) {
		super(currentContext);
	}
	
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		
		// Load all the metals that have activity for the given reporting date
		Table metalsToProcess = loadMetalsToProcess(reportParameters.getExternalBu(), reportParameters.getReportDate());
		
		if(metalsToProcess == null || metalsToProcess.getRowCount() == 0) {
			// Add dummy row
			toPopulate.addRows(1);
			
			Logging.info("No data to process for customer " + reportParameters.getExternalBu()
					+ " reporting date " + reportParameters.getReportDate());
			return;
		}
		
		// Loop over each metal
		int rowCount = metalsToProcess.getRowCount();
		for(int row = 0;  row < rowCount; row++ ) {
			toPopulate.addRows(1);
			
			toPopulate.setString(EnumDeferredPricingSection.METAL_NAME.getColumnName(), row, metalsToProcess.getString("description", row));
			toPopulate.setString(EnumDeferredPricingSection.METAL_SHORT_NAME.getColumnName(), row, metalsToProcess.getString("name", row));		
			
			Table metalSectionDetails = getReportOutput(reportParameters,metalsToProcess.getInt("id_number", row));
			
			toPopulate.setTable(EnumDeferredPricingSection.REPORT_DATA.getColumnName(), row, metalSectionDetails);
			
			// Set the current market price
			double marketPrice = setMarketData(toPopulate, row, metalsToProcess.getInt("id_number", row));
					
			// Calculate the total dispatched settle amount
			double totalDispSettleAmount  = getTotalDispatchSettleValue(metalSectionDetails);
			toPopulate.setDouble(EnumDeferredPricingSection.TOTAL_DISPATCHED.getColumnName(), row, totalDispSettleAmount);
			
			// Calculate the total unfixed settle value
			double totalUnfixedSettleAmount  = getTotalUnfixedWeight(metalSectionDetails) * marketPrice * -1.0;
			toPopulate.setDouble(EnumDeferredPricingSection.TOTAL_UNFIXED.getColumnName(), row, totalUnfixedSettleAmount);
			
			
			// Calculate the total weight
			int columnId = metalSectionDetails.getColumnId(EnumDeferredPricingData.VOLUME_IN_TOZ.getColumnName());
			double totalWeight = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumDeferredPricingSection.TOTAL_WEIGHT_TOZ.getColumnName(), row, totalWeight);
			
		
			// Load the HK conversion factor
			double hkConversionFactor = MathUtils.getHkTozToGmsConversionFactor();
			double totalWeightGms = hkConversionFactor * MathUtils.round(totalWeight,TableColumnHelper.TOZ_DECIMAL_PLACES);
			toPopulate.setDouble(EnumDeferredPricingSection.TOTAL_WEIGHT_GMS.getColumnName(), row, MathUtils.gmsRounding(totalWeightGms, 2));			
			
			// total dp value
			double totalDpValue = MathUtils.round(totalWeight,TableColumnHelper.TOZ_DECIMAL_PLACES) * marketPrice * -1.0;
			toPopulate.setDouble(EnumDeferredPricingSection.TOTAL_DP_VALUE.getColumnName(), row, totalDpValue);
			
			// calculate tier margin 
			calculateTierValues(reportParameters, metalsToProcess.getString("name", row), MathUtils.gmsRounding(totalWeight,3), marketPrice, toPopulate, row);
			
			
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#format(com.olf.openrisk.table.Table)
	 */
	public Table format(Table reportSectionToFormat) {
		TableColumnHelper<EnumDeferredPricingSection> columnHelper = new TableColumnHelper<>();
		
		columnHelper.formatTableForOutput(EnumDeferredPricingSection.class, reportSectionToFormat);
		
		for(int row = 0; row < reportSectionToFormat.getRowCount(); row++) {

		Table subReport = reportSectionToFormat.getTable(EnumDeferredPricingSection.REPORT_DATA.getColumnName(), row);
			if(subReport != null && subReport.getRowCount() > 0) {
				reportSectionToFormat.setTable(EnumDeferredPricingSection.REPORT_DATA.getColumnName(), row, formatSubReport(subReport));
			}
		}
		
		return reportSectionToFormat;
	}
	
	private void calculateTierValues(ReportParameters reportParameters, String metal, Double weight, Double price, Table toPopulate, int row) {
		MarginFactory marginFactory = new MarginFactory(context, reportParameters.getReportDate(), reportParameters.getExternalBu());
		
		MarginResults[] results = marginFactory.calculateMargin(EnumPricingType.DP, metal, weight, price);
	
		if(results == null) {
			String errorMessage = "Error calculating the tiered pricing, no results returned";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		if(results.length != 2) {
			String errorMessage = "Error calculating the tiered pricing, expecting 2 entries but found " + results.length;
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_1_DP_VALUE.getColumnName(), row, results[0].getValue() * -1.0);
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_1_DP_PERCENTAGE.getColumnName(), row, results[0].getPercentage());
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_1_DP_MIN_VOL.getColumnName(), row, results[0].getMinVol());
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_1_DP_MAX_VOL.getColumnName(), row, results[0].getMaxVol());
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_1_DP_VOL_USED.getColumnName(), row, results[0].getVolumeUsed());
		
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_2_DP_VALUE.getColumnName(), row, results[1].getValue() * -1.0);
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_2_DP_PERCENTAGE.getColumnName(), row, results[1].getPercentage());
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_2_DP_MIN_VOL.getColumnName(), row, results[1].getMinVol());
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_2_DP_MAX_VOL.getColumnName(), row, results[1].getMaxVol());	
		toPopulate.setDouble(EnumDeferredPricingSection.TIER_2_DP_VOL_USED.getColumnName(), row, results[1].getVolumeUsed());
	}
	
	
	/**
	 * Format sub report.
	 *
	 * @param reportSectionToFormat the report section to format
	 * @return the formatted sub report
	 */
	private Table formatSubReport(Table reportSectionToFormat) {

		
		TableColumnHelper<EnumDeferredPricingData> columnHelper = new TableColumnHelper<>();
		
		columnHelper.formatTableForOutput(EnumDeferredPricingData.class, reportSectionToFormat);

		// Sort by deal number ascending
		reportSectionToFormat.sort(EnumDeferredPricingData.DEAL_NUM.getColumnName(), true);
		
		return reportSectionToFormat;
	}		
	
	/**
	 * Gets the total dispatch settlement value.
	 *
	 * @param metalSectionDetails the metal section details
	 * @return the total  settlement value
	 */
	private double getTotalDispatchSettleValue(Table metalSectionDetails) {
		
		ConstTable dispatchData = metalSectionDetails.createConstView("*", 
				"[" + EnumDeferredPricingData.TYPE.getColumnName() + "] == 'Dispatch'"); 
		
		int columnId = metalSectionDetails.getColumnId(EnumDeferredPricingData.SETTLEMENT_VALUE.getColumnName());
		
		return dispatchData.calcAsDouble(columnId, EnumColumnOperation.Sum);
	}
	
	/**
	 * Gets the total unfixed weight.
	 *
	 * @param metalSectionDetails the metal section details
	 * @return the total unfixed weight
	 */
	private double getTotalUnfixedWeight(Table metalSectionDetails) {
		
		ConstTable dispatchData = metalSectionDetails.createConstView("*", 
				"[" + EnumDeferredPricingData.TYPE.getColumnName() + "] == 'FX Unmatched'"); 
		
		int columnId = metalSectionDetails.getColumnId(EnumDeferredPricingData.VOLUME_IN_TOZ.getColumnName());
		
		return dispatchData.calcAsDouble(columnId, EnumColumnOperation.Sum);
	}	

	/**
	 * Sets the market data in the report table for the given metal.
	 *
	 * @param toPopulate the table to to populate
	 * @param row the row
	 * @param metal the metal
	 * @return the double
	 */
	private double setMarketData(Table toPopulate, int row, int metal) {
		PriceFactory priceFactory = new PriceFactory(context);
		
		double marketPrice = priceFactory.getSpotRate(metal);
		
		toPopulate.setDouble(EnumDeferredPricingSection.MARKET_PRICE.getColumnName(), row, marketPrice);
		
		return marketPrice;
	}


	/**
	 * Gets the report output.
	 *
	 * @param reportParameters the parameters controlling the data to be extracted
	 * @param metal the metal to report from
	 * @return the report output
	 */
	private Table getReportOutput(ReportParameters reportParameters, int metal) {
		Table sectionData = context.getTableFactory().createTable("Report_Output");
		for(EnumDeferredPricingData column : EnumDeferredPricingData.values()) {
			sectionData.addColumn(column.getColumnName(), column.getColumnType());
		}

		// Load the HK conversion factor
		double hkConversionFactor = MathUtils.getHkTozToGmsConversionFactor();
		
		try(Table dispatchDeals = loadDispatchDeals(reportParameters.getExternalBu(), reportParameters.getReportDate(), metal, hkConversionFactor)) {
			sectionData.appendRows(dispatchDeals);
		}

		try( Table fxSellDeals = loadFxDetails(reportParameters.getExternalBu(), reportParameters.getReportDate(), metal, hkConversionFactor)) {
			sectionData.appendRows(fxSellDeals);
		}
		
		for(int row = 0; row < sectionData.getRowCount(); row++) {
			sectionData.setDouble(EnumDispatchDealData.VOLUME_IN_GMS.getColumnName(), row, 
					MathUtils.gmsRounding(sectionData.getDouble((EnumDispatchDealData.VOLUME_IN_GMS.getColumnName()), row), 2));
		}
		
		
		sectionData.setName("DeferredPriceData");
		return sectionData;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		
		TableColumnHelper<EnumDeferredPricingSection> columnHelper = new TableColumnHelper<>();

		return columnHelper.getColumnTypes(EnumDeferredPricingSection.class);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		TableColumnHelper<EnumDeferredPricingSection> columnHelper = new TableColumnHelper<>();

		return columnHelper.getColumnNames(EnumDeferredPricingSection.class);
	}
	
	/**
	 * Load metals to process.
	 *
	 * @param customerId the customer id
	 * @return the table
	 */
	private Table loadMetalsToProcess(int customerId, Date matchDate) {
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);
		String sql = " SELECT id_number,\n" +
					 "  		name,\n" +
					 "  		description\n" +
					 " FROM   currency c\n" +
					 " WHERE  id_number IN\n" +
					 " (\n" +
					 " SELECT metal_type\n" +
					 " FROM user_jm_ap_sell_deals ap\n" +
					 " JOIN ab_tran ab \n" +
					 "  ON ab.deal_tracking_num = ap.deal_num \n" +
					 "     AND tran_status = 3 \n" +
					 "     AND current_flag = 1 \n" +
					 "     AND trade_date <= '" + matchDateString + "' \n" +
					 " JOIN ab_tran_info_view abt \n" +
					 "  ON abt.tran_num = ab.tran_num \n" +
					 "     AND type_name = 'Pricing Type' \n" +
					 "     AND value = 'DP' \n" +
					 " WHERE  customer_id = " +
					 customerId +
					 "\n" +
					 " UNION\n" +
					 " SELECT metal_type\n" +
					 " FROM   user_jm_ap_buy_dispatch_deals ap\n" +
					 " JOIN ab_tran ab \n" +
					 "  ON ab.deal_tracking_num = ap.deal_num \n" +
					 "     AND current_flag = 1 \n" +
					 "     AND trade_date <= '" + matchDateString + "' \n" +
					 " JOIN ab_tran_info_view abt \n" +
					 "  ON abt.tran_num = ab.tran_num \n" +
					 "     AND type_name = 'Pricing Type' \n" +
					 "     AND value = 'DP' \n" +
					 " WHERE  customer_id = " +
					 customerId +
					 "\n" +
					 " )\n";
		return runSQL(sql);
	}
	

	/**
	 * Load dispatch deals.
	 *
	 * @param customerId the customer id
	 * @param matchDate the match date
	 * @param metalType the metal type
	 * @return the table
	 */
	private Table loadDispatchDeals(int customerId, Date matchDate, int metalType, double hkUnitConversion) {
		StringBuilder sql = new StringBuilder();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT trade_date, \n");
		sql.append("        deal_tracking_num                                          AS deal_num, \n");
		sql.append("        reference, ");
		sql.append("        p.notnl                                                    AS volume_in_toz, \n");
		sql.append("        ROUND(p.notnl, " + TableColumnHelper.TOZ_DECIMAL_PLACES + ") * ").append(hkUnitConversion).append("       AS volume_in_gms, \n");
 		sql.append("        Cast(Isnull(dp_price.value, 0.0) AS FLOAT)                 AS trade_price, \n");
 		sql.append("        trade_date                                                 AS fixed_date, \n");
 		sql.append("        p.notnl * Cast(Isnull(dp_price.value, 0.0) AS FLOAT)       AS settlement_value, \n");
 		sql.append("        'Dispatch' AS type\n");
		sql.append(" FROM   ab_tran_info_view tiv \n");
		sql.append("        JOIN ab_tran ab \n");
		sql.append("          ON ab.tran_num = tiv.tran_num \n");
		sql.append("             AND tran_status in (1, 2, 3, 7) \n");
		sql.append("             AND trade_date <= '").append(matchDateString).append("' \n");
		sql.append("             AND ins_type = 48010 \n");
		sql.append("             AND external_bunit = ").append(customerId).append("\n ");
		sql.append("        JOIN parameter p \n");
		sql.append("          ON ab.ins_num = p.ins_num \n");
		sql.append("             AND settlement_type = 2 \n");
		sql.append("        JOIN idx_def idx \n");
		sql.append("          ON p.proj_index = idx.index_id \n");
		sql.append("             AND db_status = 1 \n");
		sql.append("        JOIN idx_subgroup idxs \n");
		sql.append("          ON idxs.id_number = idx.idx_subgroup \n");
		sql.append("        JOIN currency c \n");
		sql.append("          ON c.NAME = code \n");
		sql.append("             AND c.id_number = ").append(metalType).append(" \n");
		sql.append("        LEFT JOIN (SELECT ins_num, \n");
		sql.append("                          param_seq_num, \n");
		sql.append("                          value \n");
		sql.append("                   FROM   ins_parameter_info ipi \n");
		sql.append("                          JOIN tran_info_types tit \n");
		sql.append("                            ON ipi.type_id = tit.type_id \n");
		sql.append("                               AND type_name = 'DP Price') dp_price \n");
		sql.append("               ON dp_price.ins_num = p.ins_num \n");
		sql.append("                  AND dp_price.param_seq_num = p.param_seq_num \n");
		sql.append(" WHERE  tiv.type_name = 'Pricing Type' \n");
		sql.append("        AND tiv.value = 'DP' \n");
		
		return runSQL(sql.toString());
	}
	

	/**
	 * Load fx details.
	 *
	 * @param customerId the customer id
	 * @param matchDate the match date
	 * @param metalType the metal type
	 * @return the table
	 */
	private Table loadFxDetails(int customerId, Date matchDate, int metalType, double hkUnitConversion) {
		StringBuilder sql = new StringBuilder();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT trade_date, \n");
		sql.append("    deal_tracking_num as deal_num, \n");
		sql.append("    reference, \n");
		sql.append("    vol.deal_vol * IIF(tran_status = 1, 1.0, -1.0) AS volume_in_toz, \n");
		sql.append("    ROUND(vol.deal_vol, " + TableColumnHelper.TOZ_DECIMAL_PLACES + ") * ").append(hkUnitConversion).append(" * IIF(tran_status = 1, 1.0, -1.0) AS volume_in_gms, \n");
		sql.append("    Cast(Isnull(abt.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0)  AS  trade_price, \n");
		sql.append("    IIF(tran_status = 1, Cast('1900-01-01 00:00:00.000' AS DATETIME), trade_date) AS fixed_date, \n");
		sql.append("    vol.deal_vol * IIF(tran_status = 1, -1.0, 1.0) * (Cast( Isnull(abt.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) AS settlement_value, \n");
		sql.append("    IIF(tran_status = 1, 'FX Unmatched', 'FX Matched') AS type \n");
		sql.append(" FROM   ab_tran_info_view tiv \n");
		sql.append("    JOIN ab_tran ab \n");
		sql.append("      ON ab.tran_num = tiv.tran_num \n");
		sql.append("         AND ( tran_status = 1 \n"); // Quote
		sql.append("                OR   \n");
		sql.append("               ( tran_status = 3 \n"); // Validated
		sql.append("                 AND trade_date <= '").append(matchDateString).append("' ) ) \n");
		sql.append("         AND ins_type = 26001  \n"); //FX
		sql.append("         AND external_bunit = ").append(customerId).append(" \n");
		sql.append("    JOIN (SELECT IIF(unit1 = 0, unit2, unit1) AS deal_unit, \n");
		sql.append("                 IIF(unit1 = 0, c_amt, d_amt) AS deal_vol, \n");
		sql.append("                 IIF(unit1 = 0, ccy2, ccy1) AS metal, \n");
	    sql.append("                 tran_num \n");
		sql.append("          FROM   fx_tran_aux_data fx) AS vol \n");
		sql.append("      ON vol.tran_num = ab.tran_num AND vol.metal = ").append(metalType).append(" \n");
		sql.append("    LEFT JOIN ab_tran_info_view abt \n");
		sql.append("           ON abt.tran_num = ab.tran_num \n");
		sql.append("              AND abt.type_name = 'Trade Price' \n");
		sql.append("    LEFT JOIN unit_conversion uc2 \n");
		sql.append("           ON uc2.src_unit_id = \n");
		sql.append("           		(SELECT IIF(unit1 != 0, unit1, unit2) AS  deal_unit \n");
		sql.append("           		FROM   fx_tran_aux_data  \n");
		sql.append("           		WHERE  tran_num = ab.tran_num)   AND uc2.dest_unit_id = 55 \n");

		sql.append(" WHERE  tiv.type_name = 'Pricing Type' \n");
		sql.append("    AND tiv.value = 'DP' 	 \n");	
		

		
		return runSQL(sql.toString());
	}


}
