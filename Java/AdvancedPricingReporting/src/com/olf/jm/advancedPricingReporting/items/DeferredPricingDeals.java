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
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.Date;
import java.util.HashSet;
import java.util.function.Consumer;

public class DeferredPricingDeals extends ItemBase {

	public DeferredPricingDeals(Context currentContext) {
		super(currentContext);
	}
	
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		double hkConversionFactor = MathUtils.getHkTozToGmsConversionFactor();
		Table dispatchDeals = loadDispatchDeals(reportParameters.getExternalBu(), reportParameters.getReportDate(), hkConversionFactor);
		Table fxDeals = loadFxDetails(reportParameters.getExternalBu(), reportParameters.getReportDate(), hkConversionFactor);
		
		HashSet<Currency> metals = new HashSet<>();
		StaticDataFactory staticDataFactory = context.getStaticDataFactory();
		Consumer<TableRow> metalRetriever = row -> metals.add(staticDataFactory.getReferenceObject(Currency.class, row.getInt("metal")));
		dispatchDeals.getRows().forEach(metalRetriever);
		fxDeals.getRows().forEach(metalRetriever);
		if (metals.isEmpty()) {
			toPopulate.addRows(1);
			Logging.info("No data to process for customer " + reportParameters.getExternalBu()
						 + " reporting date " + reportParameters.getReportDate());
			return;
		}

		for (Currency metal : metals) {
			TableRow row = toPopulate.addRow();
			row.getCell(EnumDeferredPricingSection.METAL_NAME.getColumnName()).setString(metal.getDescription());
			row.getCell(EnumDeferredPricingSection.METAL_SHORT_NAME.getColumnName()).setString(metal.getName());
			
			Table metalSectionDetails = getMetalSectionDetails(dispatchDeals, fxDeals, metal.getId());
			row.getCell(EnumDeferredPricingSection.REPORT_DATA.getColumnName()).setTable(metalSectionDetails);
			
			// Set the current market price
			double marketPrice = new PriceFactory(context).getSpotRate(metal.getId());
			row.getCell(EnumDeferredPricingSection.MARKET_PRICE.getColumnName()).setDouble(marketPrice);
					
			// Calculate the total dispatched settle amount
			double totalDispSettleAmount  = getTotalDispatchSettleValue(metalSectionDetails);
			row.getCell(EnumDeferredPricingSection.TOTAL_DISPATCHED.getColumnName()).setDouble(totalDispSettleAmount);
			
			// Calculate the total unfixed settle value
			double totalUnfixedSettleAmount  = getTotalUnfixedWeight(metalSectionDetails) * marketPrice * -1.0;
			row.getCell(EnumDeferredPricingSection.TOTAL_UNFIXED.getColumnName()).setDouble(totalUnfixedSettleAmount);
			
			// Calculate the total weight
			int columnId = metalSectionDetails.getColumnId(EnumDeferredPricingData.VOLUME_IN_TOZ.getColumnName());
			double totalWeight = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			row.getCell(EnumDeferredPricingSection.TOTAL_WEIGHT_TOZ.getColumnName()).setDouble(totalWeight);
			double totalWeightGms = hkConversionFactor * MathUtils.round(totalWeight,TableColumnHelper.TOZ_DECIMAL_PLACES);
			row.getCell(EnumDeferredPricingSection.TOTAL_WEIGHT_GMS.getColumnName()).setDouble(MathUtils.gmsRounding(totalWeightGms, 2));
			
			// total dp value
			double totalDpValue = MathUtils.round(totalWeight,TableColumnHelper.TOZ_DECIMAL_PLACES) * marketPrice * -1.0;
			row.getCell(EnumDeferredPricingSection.TOTAL_DP_VALUE.getColumnName()).setDouble(totalDpValue);
			
			// calculate tier margin 
			calculateTierValues(reportParameters, metal.getName(), MathUtils.gmsRounding(totalWeight,3), marketPrice, row);
		}
	}

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
	
	private void calculateTierValues(ReportParameters reportParameters, String metal, Double weight, Double price, TableRow row) {
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
		
		row.getCell(EnumDeferredPricingSection.TIER_1_DP_VALUE.getColumnName()).setDouble(results[0].getValue() * -1.0);
		row.getCell(EnumDeferredPricingSection.TIER_1_DP_PERCENTAGE.getColumnName()).setDouble(results[0].getPercentage());
		row.getCell(EnumDeferredPricingSection.TIER_1_DP_MIN_VOL.getColumnName()).setDouble(results[0].getMinVol());
		row.getCell(EnumDeferredPricingSection.TIER_1_DP_MAX_VOL.getColumnName()).setDouble(results[0].getMaxVol());
		row.getCell(EnumDeferredPricingSection.TIER_1_DP_VOL_USED.getColumnName()).setDouble(results[0].getVolumeUsed());
		
		row.getCell(EnumDeferredPricingSection.TIER_2_DP_VALUE.getColumnName()).setDouble(results[1].getValue() * -1.0);
		row.getCell(EnumDeferredPricingSection.TIER_2_DP_PERCENTAGE.getColumnName()).setDouble(results[1].getPercentage());
		row.getCell(EnumDeferredPricingSection.TIER_2_DP_MIN_VOL.getColumnName()).setDouble(results[1].getMinVol());
		row.getCell(EnumDeferredPricingSection.TIER_2_DP_MAX_VOL.getColumnName()).setDouble(results[1].getMaxVol());
		row.getCell(EnumDeferredPricingSection.TIER_2_DP_VOL_USED.getColumnName()).setDouble(results[1].getVolumeUsed());
	}
	
	private Table formatSubReport(Table reportSectionToFormat) {
		TableColumnHelper<EnumDeferredPricingData> columnHelper = new TableColumnHelper<>();
		columnHelper.formatTableForOutput(EnumDeferredPricingData.class, reportSectionToFormat);
		reportSectionToFormat.sort(EnumDeferredPricingData.DEAL_NUM.getColumnName(), true);
		return reportSectionToFormat;
	}		
	
	private double getTotalDispatchSettleValue(Table metalSectionDetails) {
		
		ConstTable dispatchData = metalSectionDetails.createConstView("*", 
				"[" + EnumDeferredPricingData.TYPE.getColumnName() + "] == 'Dispatch'"); 
		
		int columnId = metalSectionDetails.getColumnId(EnumDeferredPricingData.SETTLEMENT_VALUE.getColumnName());
		
		return dispatchData.calcAsDouble(columnId, EnumColumnOperation.Sum);
	}
	
	private double getTotalUnfixedWeight(Table metalSectionDetails) {
		ConstTable dispatchData = metalSectionDetails.createConstView("*",
				"[" + EnumDeferredPricingData.TYPE.getColumnName() + "] == 'FX Unmatched'"); 
		int columnId = metalSectionDetails.getColumnId(EnumDeferredPricingData.VOLUME_IN_TOZ.getColumnName());
		return dispatchData.calcAsDouble(columnId, EnumColumnOperation.Sum);
	}	

	private Table getMetalSectionDetails(Table dispatchDeals, Table fxDeals, int metal) {
		Table sectionData = context.getTableFactory().createTable("DeferredPriceData");
		for(EnumDeferredPricingData column : EnumDeferredPricingData.values()) {
			sectionData.addColumn(column.getColumnName(), column.getColumnType());
		}
		sectionData.select(dispatchDeals, "*", "[In.metal] == " + metal);
		sectionData.select(fxDeals, "*", "[In.metal] == " + metal);
		for (TableRow row : sectionData.getRows()) {
			String volumeCol = EnumDispatchDealData.VOLUME_IN_GMS.getColumnName();
			double volume = MathUtils.gmsRounding(row.getDouble(volumeCol), 2);
			row.getCell(volumeCol).setDouble(volume);
		}
		return sectionData;
	}

	@Override
	public EnumColType[] getDataTypes() {
		TableColumnHelper<EnumDeferredPricingSection> columnHelper = new TableColumnHelper<>();
		return columnHelper.getColumnTypes(EnumDeferredPricingSection.class);
	}

	@Override
	public String[] getColumnNames() {
		TableColumnHelper<EnumDeferredPricingSection> columnHelper = new TableColumnHelper<>();
		return columnHelper.getColumnNames(EnumDeferredPricingSection.class);
	}
	
	private Table loadDispatchDeals(int customerId, Date matchDate, double hkUnitConversion) {
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);
		String customerName = context.getStaticDataFactory().getName(EnumReferenceTable.Party, customerId);
		
		String sql = " SELECT trade_date, \n" +
					 "        deal_tracking_num                                          AS deal_num, \n" +
					 "        reference, " +
					 "        p.notnl                                                    AS volume_in_toz, \n" +
					 "        ROUND(p.notnl, " +
					 TableColumnHelper.TOZ_DECIMAL_PLACES + ") * " + hkUnitConversion + "       AS volume_in_gms, \n" +
					 "        Cast(Isnull(dp_price.value, 0.0) AS FLOAT)                 AS trade_price, \n" +
					 "        trade_date                                                 AS fixed_date, \n" +
					 "        p.notnl * Cast(Isnull(dp_price.value, 0.0) AS FLOAT)       AS settlement_value, \n" +
					 "        'Dispatch' AS type, \n" +
					 "        c.id_number AS metal\n" +
					 " FROM   ab_tran_info_view tiv \n" +
					 "        JOIN ab_tran ab \n" +
					 "          ON ab.tran_num = tiv.tran_num \n" +
					 "             AND tran_status in (1, 2, 3, 7) \n" +
					 "             AND trade_date <= '" + matchDateString + "' \n" +
					 "             AND ins_type = 48010 \n" +
					 "        JOIN ab_tran_info_view consignee \n" +
					 "          ON ab.tran_num = consignee.tran_num \n" +
					 "             AND consignee.value = '" + customerName + "'\n " +
					 "        JOIN parameter p \n" +
					 "          ON ab.ins_num = p.ins_num \n" +
					 "             AND settlement_type = 2 \n" +
					 "        JOIN idx_def idx \n" +
					 "          ON p.proj_index = idx.index_id \n" +
					 "             AND db_status = 1 \n" +
					 "        JOIN idx_subgroup idxs \n" +
					 "          ON idxs.id_number = idx.idx_subgroup \n" +
					 "        JOIN currency c \n" +
					 "          ON c.NAME = code \n" +
					 "        LEFT JOIN (SELECT ins_num, \n" +
					 "                          param_seq_num, \n" +
					 "                          value \n" +
					 "                   FROM   ins_parameter_info ipi \n" +
					 "                          JOIN tran_info_types tit \n" +
					 "                            ON ipi.type_id = tit.type_id \n" +
					 "                               AND type_name = 'DP Price') dp_price \n" +
					 "               ON dp_price.ins_num = p.ins_num \n" +
					 "                  AND dp_price.param_seq_num = p.param_seq_num \n" +
					 " WHERE  tiv.type_name = 'Pricing Type' \n" +
					 "        AND tiv.value = 'DP' \n";
		return runSQL(sql);
	}

	/**
	 * Load fx details.
	 *
	 * @param customerId the customer id
	 * @param matchDate the match date
	 * @return the table
	 */
	private Table loadFxDetails(int customerId, Date matchDate, double hkUnitConversion) {
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);
		
		String sql = " SELECT trade_date, \n" +
					 "    deal_tracking_num as deal_num, \n" +
					 "    reference, \n" +
					 "    vol.deal_vol * IIF(tran_status = 1, 1.0, -1.0) AS volume_in_toz, \n" +
					 "    ROUND(vol.deal_vol, " + TableColumnHelper.TOZ_DECIMAL_PLACES + ") * " + hkUnitConversion + " * IIF(tran_status = 1, 1.0, -1.0) AS volume_in_gms, \n" +
					 "    Cast(Isnull(abt.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0)  AS  trade_price, \n" +
					 "    IIF(tran_status = 1, Cast('1900-01-01 00:00:00.000' AS DATETIME), trade_date) AS fixed_date, \n" +
					 "    vol.deal_vol * IIF(tran_status = 1, -1.0, 1.0) * (Cast( Isnull(abt.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) AS settlement_value, \n" +
					 "    IIF(tran_status = 1, 'FX Unmatched', 'FX Matched') AS type, \n" +
					 "    vol.metal AS metal \n" +
					 " FROM   ab_tran_info_view tiv \n" +
					 "    JOIN ab_tran ab \n" +
					 "      ON ab.tran_num = tiv.tran_num \n" +
					 "         AND ( tran_status = 1 \n" + // Quote
					 "                OR   \n" +
					 "               ( tran_status = 3 \n" + // Validated
					 "                 AND trade_date <= '" + matchDateString + "' ) ) \n" +
					 "         AND ins_type = 26001  \n" + //FX
					 "         AND external_bunit = " + customerId + " \n" +
					 "    JOIN (SELECT IIF(unit1 = 0, unit2, unit1) AS deal_unit, \n" +
					 "                 IIF(unit1 = 0, c_amt, d_amt) AS deal_vol, \n" +
					 "                 IIF(unit1 = 0, ccy2, ccy1) AS metal, \n" +
					 "                 tran_num \n" +
					 "          FROM   fx_tran_aux_data fx) AS vol \n" +
					 "      ON vol.tran_num = ab.tran_num\n" +
					 "    LEFT JOIN ab_tran_info_view abt \n" +
					 "           ON abt.tran_num = ab.tran_num \n" +
					 "              AND abt.type_name = 'Trade Price' \n" +
					 "    LEFT JOIN unit_conversion uc2 \n" +
					 "           ON uc2.src_unit_id = \n" +
					 "           		(SELECT IIF(unit1 != 0, unit1, unit2) AS  deal_unit \n" +
					 "           		FROM   fx_tran_aux_data  \n" +
					 "           		WHERE  tran_num = ab.tran_num)   AND uc2.dest_unit_id = 55 \n" +
					 " WHERE  tiv.type_name = 'Pricing Type' \n" +
					 "    AND tiv.value = 'DP' 	 \n";
		return runSQL(sql);
	}
}
