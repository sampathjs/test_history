package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
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
import com.openlink.util.constrepository.ConstRepository;

import java.util.Date;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDispatchedDeals. Generates the data needed for dispatch deal section  in the advanced pricing report. 
 */
public class ApDispatchedDeals extends ItemBase {

	
	/**
	 * Instantiates a new ap dispatched deals object.
	 *
	 * @param currentContext the current context
	 */
	public ApDispatchedDeals(Context currentContext) {
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
			
			toPopulate.setString(EnumDispatchDealSection.METAL_NAME.getColumnName(), row, metalsToProcess.getString("description", row));
			toPopulate.setString(EnumDispatchDealSection.METAL_SHORT_NAME.getColumnName(), row, metalsToProcess.getString("name", row));		
			
			Table metalSectionDetails = getReportOutput(reportParameters,metalsToProcess.getInt("id_number", row));
			
			toPopulate.setTable(EnumDispatchDealSection.REPORT_DATA.getColumnName(), row, metalSectionDetails);
			
			// Set the current market price
			double marketPrice = setMarketData(toPopulate, row, metalsToProcess.getInt("id_number", row));
			
			// Calculate the total weight
			int columnId = metalSectionDetails.getColumnId(EnumDispatchDealData.VOLUME_IN_TOZ.getColumnName());
			double totalWeight = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumDispatchDealSection.TOTAL_WEIGHT_TOZ.getColumnName(), row, totalWeight);

			
			// Load the HK conversion factor
			double hkConversionFactor = MathUtils.getHkTozToGmsConversionFactor();
			double totalWeightGms = hkConversionFactor * MathUtils.round(totalWeight,TableColumnHelper.TOZ_DECIMAL_PLACES);
			toPopulate.setDouble(EnumDispatchDealSection.TOTAL_WEIGHT_GMS.getColumnName(), row, MathUtils.gmsRounding(totalWeightGms, 2));			

			// Calculate the total settlement value
			
			if(MathUtils.round(totalWeight,TableColumnHelper.TOZ_DECIMAL_PLACES) == 0.0) {
				toPopulate.setDouble(EnumDispatchDealSection.TOTAL_AP_DEAL.getColumnName(), row, 0.0);
			} else {
				columnId = metalSectionDetails.getColumnId(EnumDispatchDealData.SETTLEMENT_VALUE.getColumnName());
				double totalSettlementAmount = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
				toPopulate.setDouble(EnumDispatchDealSection.TOTAL_AP_DEAL.getColumnName(), row, totalSettlementAmount);
			}

			
			// calculate and set average price using weights average due to FX and Dispatch deals netting out to 0 volume
			double averagePrice = MathUtils.round(calculateWeightedAverage(metalSectionDetails), 2);
			toPopulate.setDouble(EnumDispatchDealSection.AVERAGE_PRICE.getColumnName(), row, averagePrice);
			
			// calculate and set loss / gain
			double priceDiff = marketPrice - averagePrice;
			toPopulate.setDouble(EnumDispatchDealSection.PRICE_DIFF.getColumnName(), row, priceDiff);
			
			double lossGain = priceDiff * totalWeight * -1.0;
			toPopulate.setDouble(EnumDispatchDealSection.LOSS_GAIN.getColumnName(), row, lossGain);
			
			// calculate the total dispatch deal value
			try(ConstTable dispatchDeals = metalSectionDetails.createConstView(EnumDispatchDealData.SETTLEMENT_VALUE.getColumnName(), "[" +EnumDispatchDealData.TYPE.getColumnName() + "] == 'DISPATCH'")) {
				double totalDealValue = dispatchDeals.calcAsDouble(0, EnumColumnOperation.Sum);
				toPopulate.setDouble(EnumDispatchDealSection.TOTAL_DISP_AP_DEAL.getColumnName(), row, totalDealValue);
			} catch (Exception e) {
				String errorMessage = "Error calculating the total dispatch deal value. " + e.getLocalizedMessage();
				Logging.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
			
			// Calculate the tiered margin
			calculateTierValues(reportParameters, metalsToProcess.getString("name", row), totalWeight, averagePrice, toPopulate, row);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#format(com.olf.openrisk.table.Table)
	 */
	@Override
	public Table format(Table reportSectionToFormat) {
		TableColumnHelper<EnumDispatchDealSection> columnHelper = new TableColumnHelper<>();
		
		columnHelper.formatTableForOutput(EnumDispatchDealSection.class, reportSectionToFormat);
		
		for(int row = 0; row < reportSectionToFormat.getRowCount(); row++) {
			Table subReport = reportSectionToFormat.getTable(EnumDispatchDealSection.REPORT_DATA.getColumnName(), row);
			if(subReport != null && subReport.getRowCount() > 0) {
				reportSectionToFormat.setTable(EnumDispatchDealSection.REPORT_DATA.getColumnName(), row, formatSubReport(subReport));
			}
		}
		
		return reportSectionToFormat;
	}
	
	private void calculateTierValues(ReportParameters reportParameters, String metal, Double weight, Double price, Table toPopulate, int row) {
		MarginFactory marginFactory = new MarginFactory(context, reportParameters.getReportDate(), reportParameters.getExternalBu());
		
		MarginResults[] results = marginFactory.calculateMargin(EnumPricingType.AP, metal, weight, price);
	
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
		
		toPopulate.setDouble(EnumDispatchDealSection.TIER_1_AP_VALUE.getColumnName(), row, results[0].getValue() * -1.0);
		toPopulate.setDouble(EnumDispatchDealSection.TIER_1_AP_PERCENTAGE.getColumnName(), row, results[0].getPercentage());
		toPopulate.setDouble(EnumDispatchDealSection.TIER_1_AP_MIN_VOL.getColumnName(), row, results[0].getMinVol());
		toPopulate.setDouble(EnumDispatchDealSection.TIER_1_AP_MAX_VOL.getColumnName(), row, results[0].getMaxVol());
		toPopulate.setDouble(EnumDispatchDealSection.TIER_1_AP_VOL_USED.getColumnName(), row, results[0].getVolumeUsed());
		
		toPopulate.setDouble(EnumDispatchDealSection.TIER_2_AP_VALUE.getColumnName(), row, results[1].getValue() * -1.0);
		toPopulate.setDouble(EnumDispatchDealSection.TIER_2_AP_PERCENTAGE.getColumnName(), row, results[1].getPercentage());
		toPopulate.setDouble(EnumDispatchDealSection.TIER_2_AP_MIN_VOL.getColumnName(), row, results[1].getMinVol());
		toPopulate.setDouble(EnumDispatchDealSection.TIER_2_AP_MAX_VOL.getColumnName(), row, results[1].getMaxVol());
		toPopulate.setDouble(EnumDispatchDealSection.TIER_2_AP_VOL_USED.getColumnName(), row, results[1].getVolumeUsed());
	}
	
	/**umP
	 * Format the sub report report.
	 *
	 * @param reportSectionToFormat the report section to format
	 * @return the formatted report
	 */
	private Table formatSubReport(Table reportSectionToFormat) {

		
		TableColumnHelper<EnumDispatchDealData> columnHelper = new TableColumnHelper<>();
		
		columnHelper.formatTableForOutput(EnumDispatchDealData.class, reportSectionToFormat);

		// Sort by deal number ascending
		reportSectionToFormat.sort(EnumDispatchDealData.DEAL_NUM.getColumnName(), true);

		return reportSectionToFormat;
	}

	/**
	 * Calculate weighted average. used the absolute price and weight to avoid the situation where the total weight is 0
	 */
	private double calculateWeightedAverage(Table metalSectionDetails)  {
		
		if(metalSectionDetails.getRowCount() == 0) {
			return 0;
		}
		
		int columnId = metalSectionDetails.getColumnId(EnumDispatchDealData.VOLUME_IN_TOZ.getColumnName());
		double totalWeight = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
		
		columnId = metalSectionDetails.getColumnId(EnumDispatchDealData.SETTLEMENT_VALUE.getColumnName());
		double totalSettleValue = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
		double toleranceThreshold;
		try {
			ConstRepository constRep = new ConstRepository("Warehouse", "ContainerWeightConverter");

			String value = constRep.getStringValue("zeroToleranceLevel", "0.01");
			
			toleranceThreshold = Double.parseDouble(value);
		} catch (Exception e) {
			Logging.error("Error reading the tolerance threshold." + e.getMessage());
			throw new RuntimeException("Error reading the tolerance threshold." + e.getMessage());
		}
		
		if(totalWeight<= toleranceThreshold &&  totalWeight >= (toleranceThreshold * -1.0)) {
			return 0.0;
		}
		return Math.abs(totalSettleValue) / Math.abs(totalWeight);
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
		
		toPopulate.setDouble(EnumDispatchDealSection.MARKET_PRICE.getColumnName(), row, marketPrice);
		
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
		for(EnumDispatchDealData column : EnumDispatchDealData.values()) {
			sectionData.addColumn(column.getColumnName(), column.getColumnType());
		}
		
		// Load the HK conversion factor
		double hkConversionFactor = MathUtils.getHkTozToGmsConversionFactor();
		
		try(Table dispatchDeals = loadDispatchDeals(reportParameters.getExternalBu(), reportParameters.getReportDate(), metal, hkConversionFactor)) {
			aggregateDispatchData(dispatchDeals, sectionData);
		}
		
		try( Table fxSellDeals = loadFxUnmatchedDetails(reportParameters.getExternalBu(),metal, hkConversionFactor)) {
			sectionData.appendRows(fxSellDeals);
		}

		try( Table fxSellDeals = loadFXMatchedDetails(reportParameters.getExternalBu(), reportParameters.getReportDate(), metal, hkConversionFactor)) {
			sectionData.appendRows(fxSellDeals);
		}
		
		sectionData.setName("DispatchData");
		
		for(int row = 0; row < sectionData.getRowCount(); row++) {
			sectionData.setDouble(EnumDispatchDealData.VOLUME_IN_GMS.getColumnName(), row, 
					MathUtils.gmsRounding(sectionData.getDouble((EnumDispatchDealData.VOLUME_IN_GMS.getColumnName()), row), 2));
		}
		return sectionData;
	}

	/**
	 * Aggregate dispatch data. Dispatch deals can contain multiple entries in the table, combine these into a single entry based on the deal 
	 * number, calculate the average price based on the aggregated totals.
	 */
	private void aggregateDispatchData(Table dispatchDeals, Table sectionData) {
		Table dealNumbers = dispatchDeals.getDistinctValues(EnumDispatchDealData.DEAL_NUM.getColumnName());
		
		for(int i = 0; i < dealNumbers.getRowCount(); i++) {
			int dealNumber = dealNumbers.getInt(0, i);
			
			ConstTable dealData = dispatchDeals.createConstView("*", "[" + EnumDispatchDealData.DEAL_NUM.getColumnName() + "] == " + dealNumber);
			
			int columnId = dealData.getColumnId(EnumDispatchDealData.VOLUME_IN_TOZ.getColumnName());
			double totalVolumeToz = dealData.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
			columnId = dealData.getColumnId(EnumDispatchDealData.VOLUME_IN_GMS.getColumnName());
			double totalVolumeGms = dealData.calcAsDouble(columnId, EnumColumnOperation.Sum);

			
			columnId = dealData.getColumnId(EnumDispatchDealData.SETTLEMENT_VALUE.getColumnName());
			double totalSettleValue = dealData.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
			int newRow = sectionData.addRows(1);
			
			sectionData.setRowValues(newRow, new Object[] {
					dealData.getInt(EnumDispatchDealData.DEAL_NUM.getColumnName(), 0),
					dealData.getDate(EnumDispatchDealData.TRADE_DATE.getColumnName(), 0),
					dealData.getString(EnumDispatchDealData.REFERENCE.getColumnName(), 0),
					dealData.getDate(EnumDispatchDealData.MATCH_DATE.getColumnName(), 0),
					totalVolumeToz,
					totalVolumeGms,
					totalSettleValue / totalVolumeToz,
					totalSettleValue,
					"DISPATCH"
			});
			
		}		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		
		TableColumnHelper<EnumDispatchDealSection> columnHelper = new TableColumnHelper<>();

		return columnHelper.getColumnTypes(EnumDispatchDealSection.class);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		TableColumnHelper<EnumDispatchDealSection> columnHelper = new TableColumnHelper<>();

		return columnHelper.getColumnNames(EnumDispatchDealSection.class);
	}
	
	/**
	 * Load metals to process.
	 *
	 * @param customerId the customer id
	 * @param matchDate the match date
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
					 " JOIN ab_tran_info_view abt \n" +
					 "  ON abt.tran_num = ab.tran_num \n" +
					 "     AND type_name = 'Pricing Type' \n" +
					 "     AND value = 'AP' \n" +
					 " WHERE  customer_id = " +
					 customerId +
					 "\n" +
					 " AND    match_status IN ('N' , 'P')\n" +
					 " UNION\n" +
					 " SELECT metal_type\n" +
					 " FROM   user_jm_ap_buy_dispatch_deals ap\n" +
					 " JOIN ab_tran ab \n" +
					 "  ON ab.deal_tracking_num = ap.deal_num \n" +
					 "     AND tran_status = 3 \n" +
					 "     AND current_flag = 1 \n" +
					 " JOIN ab_tran_info_view abt \n" +
					 "  ON abt.tran_num = ab.tran_num \n" +
					 "     AND type_name = 'Pricing Type' \n" +
					 "     AND value = 'AP' \n" +
					 " WHERE  customer_id = " +
					 customerId +
					 "\n" +
					 " AND    match_status = 'M'\n" +
					 " AND    match_date = '" +
					 matchDateString +
					 "'\n" +
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
		
		sql.append(" SELECT apb.deal_num, \n");
		sql.append("    trade_date, \n");
		sql.append("    reference, \n");
		sql.append("    apl.match_date, \n");
		sql.append("    match_volume                                                    AS volume_in_toz,\n"); 
		sql.append("     ROUND( match_volume, " + TableColumnHelper.TOZ_DECIMAL_PLACES + " ) * ").append(hkUnitConversion).append("                                    AS volume_in_gms, \n");
		sql.append("   ( Cast(Isnull(pt.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) AS trade_price, \n");
		sql.append("   match_volume * -1.0 * ( Cast(Isnull(pt.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) AS settlement_value, \n");
		sql.append("   'DISPATCH'                                                      AS type\n"); 
		sql.append(" FROM   user_jm_ap_buy_dispatch_deals apb \n");
		sql.append("    JOIN user_jm_ap_buy_sell_link apl \n");
		sql.append("     ON apl.buy_deal_num = apb.deal_num \n");
		sql.append("    JOIN USER_jm_ap_sell_deals aps \n");
		sql.append("     ON apl.sell_deal_num = aps.deal_num AND aps.match_status != 'E' \n");
		sql.append("    LEFT JOIN ab_tran_info_view tp \n");
		sql.append("          ON tp.tran_num = (SELECT tran_num\n");
		sql.append("                             FROM   ab_tran \n");
		sql.append("                             WHERE  deal_tracking_num = apl.sell_deal_num\n");
		sql.append("                                    AND current_flag = 1 \n");
		sql.append("                                    AND tran_status = 3)\n");
		sql.append("             AND tp.type_name = 'Trade Price'\n");
		sql.append("    JOIN ab_tran_info_view pt \n");
		sql.append("          ON pt.tran_num = (SELECT tran_num\n");
		sql.append("                             FROM   ab_tran \n");
		sql.append("                             WHERE  deal_tracking_num = apl.sell_deal_num\n"); 
		sql.append("                                    AND current_flag = 1 \n");
		sql.append("                                    AND tran_status = 3)\n"); 
		sql.append("             AND pt.type_name = 'Pricing Type'\n");
		sql.append("             AND pt.value = 'AP'\n");
		sql.append("   LEFT JOIN unit_conversion uc2 \n");
		sql.append("        ON uc2.src_unit_id = (SELECT Iif(unit1 != 0, unit1, unit2) AS\n"); 
		sql.append("                                      deal_unit \n");
		sql.append("                               FROM   fx_tran_aux_data \n");
		sql.append("                               WHERE  tran_num = (SELECT tran_num\n"); 
		sql.append("                                                  FROM   ab_tran \n");
		sql.append("                                                   WHERE \n");
		sql.append("                                       deal_tracking_num = \n");
		sql.append("                                      apl.sell_deal_num \n");
		sql.append("                                      AND current_flag = 1 \n");
		sql.append("                                      AND tran_status = 3))\n"); 
		sql.append("           AND uc2.dest_unit_id = 55\n"); 
		sql.append("  JOIN ab_tran ab \n");
		sql.append("     ON ab.deal_tracking_num = apb.deal_num\n"); 
		sql.append("       AND current_flag = 1 \n");
		sql.append("       AND tran_status = 3 \n");
		sql.append("       AND ins_type = 48010 \n");
		sql.append(" WHERE  apb.match_status = 'M' \n");
		sql.append("    AND apb.metal_type = ").append(metalType).append("\n");
		sql.append("    AND apb.match_date = '").append(matchDateString).append("'\n");
		sql.append("    AND apb.customer_id = ").append(customerId).append("\n");
	     
		return runSQL(sql.toString());
	}
	
	/**
	 * Load fx sell deals with volume unmatched.
	 *
	 * @param customerId the customer id
	 * @param metalType the metal type
	 * @return the table
	 */
	private Table loadFxUnmatchedDetails(int customerId, int metalType, double hkUnitConversion) {
		String sql = " SELECT deal_num,\n" +
					 "    trade_date,\n" +
					 "    reference,\n" +
					 "    cast('1900-01-01 00:00:00.000' as DATETIME)        AS match_date,\n" +
					 "    volume_left_in_toz  * -1.0              AS volume_in_toz,\n" +
					 "    (ROUND(volume_left_in_toz, " +
					 TableColumnHelper.TOZ_DECIMAL_PLACES +
					 ") * " +
					 hkUnitConversion +
					 ")  * -1.0     AS volume_in_gms,\n" +
					 "    (Cast(Isnull(tp.value, 0.0) AS FLOAT)  / Isnull(uc2.factor,1.0))  AS trade_price,\n" +
					 "    volume_left_in_toz * (Cast(Isnull(tp.value, 0.0) AS FLOAT)  / Isnull(uc2.factor,1.0))  AS settlement_value,\n" +
					 "    'FX UnMatched' as type\n" +
					 " FROM   user_jm_ap_sell_deals aps\n" +
					 "    JOIN ab_tran ab\n" +
					 "      ON ab.deal_tracking_num = aps.deal_num\n" +
					 "         AND current_flag = 1\n" +
					 "         AND tran_status = 3\n" +
					 "       LEFT JOIN ab_tran_info_view tp \n" +
					 "              ON tp.tran_num = ab.tran_num \n" +
					 "                 AND tp.type_name = 'Trade Price' \n" +
					 "       JOIN ab_tran_info_view pt \n" +
					 "              ON pt.tran_num = ab.tran_num \n" +
					 "                 AND pt.type_name = 'Pricing Type' \n" +
					 "                 AND pt.value = 'AP' \n" +
					 "    LEFT JOIN unit_conversion uc2 \n" +
					 "           ON uc2.src_unit_id = (\n" +
					 "              SELECT IIF( unit1 != 0 , unit1, unit2) AS deal_unit \n" +
					 "              FROM fx_tran_aux_data WHERE tran_num = ab.tran_num )\n" +
					 "              AND uc2.dest_unit_id = 55  \n" +
					 " WHERE  match_status IN ( 'N', 'P' )\n" +
					 "    AND metal_type = " +
					 metalType +
					 "\n" +
					 "    AND aps.customer_id = " +
					 customerId +
					 "\n";
		return runSQL(sql);
	}
	
	
	/**
	 * Load fx sell deal with matched volume.
	 *
	 * @param customerId the customer id
	 * @param matchDate the match date
	 * @param metalType the metal type
	 * @return the table
	 */
	Table loadFXMatchedDetails(int customerId, Date matchDate, int metalType, double hkUnitConversion) {
		StringBuilder sql = new StringBuilder();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT aps.deal_num,\n");
		sql.append("    trade_date,\n");
		sql.append("    reference,\n");
		sql.append("    link.match_date								                       AS match_date,\n");
		sql.append("    link.match_volume * -1.0                                           AS volume_in_toz,\n");
		sql.append("    (ROUND(link.match_volume, " + TableColumnHelper.TOZ_DECIMAL_PLACES + ") * ").append(hkUnitConversion).append(") * -1.0  AS volume_in_gms,\n");
		sql.append("    ( Cast(Isnull(tp.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) )    AS trade_price,\n");
		sql.append("    link.match_volume * ( Cast(Isnull(tp.value, 0.0) AS FLOAT) / \n");
		sql.append("                          Isnull(uc2.factor, 1.0) )                    AS settlement_value,\n");
		sql.append("    'FX Matched'                                                       AS type\n");
		sql.append(" FROM   user_jm_ap_sell_deals aps\n");
		sql.append("    JOIN ab_tran ab\n");
		sql.append("      ON ab.deal_tracking_num = aps.deal_num\n");
		sql.append("         AND current_flag = 1\n");
		sql.append("         AND tran_status = 3\n");
		sql.append("       LEFT JOIN ab_tran_info_view tp \n");
		sql.append("              ON tp.tran_num = ab.tran_num \n");
		sql.append("                 AND tp.type_name = 'Trade Price' \n");
		sql.append("       JOIN ab_tran_info_view pt \n");
		sql.append("              ON pt.tran_num = ab.tran_num \n");
		sql.append("                 AND pt.type_name = 'Pricing Type' \n");
		sql.append("                 AND pt.value = 'AP' \n");
		sql.append("    LEFT JOIN unit_conversion uc2\n");
		sql.append("           ON uc2.src_unit_id = (SELECT Iif(unit1 != 0, unit1, unit2) AS\n");
		sql.append("                                        deal_unit\n");
		sql.append("                                 FROM   fx_tran_aux_data\n");
		sql.append("                                 WHERE  tran_num = ab.tran_num)\n");
		sql.append("              AND uc2.dest_unit_id = 55\n");
		sql.append("    JOIN (SELECT sell_deal_num,\n");
		sql.append("                 match_volume,\n");
		sql.append("                 apb.match_date\n");
		sql.append("          FROM   user_jm_ap_buy_dispatch_deals apb\n");
		sql.append("                 JOIN user_jm_ap_buy_sell_link link\n");
		sql.append("                   ON link.buy_deal_num = apb.deal_num AND apb.metal_type = link.metal_type\n");
	    sql.append("                 JOIN ab_tran   ab1 \n");
	    sql.append("                   ON ab1.deal_tracking_num =  link.buy_deal_num  \n");
	    sql.append("                     AND ab1.current_flag = 1 \n");
	    sql.append("                     AND ab1.tran_status = 3 \n");
	    sql.append("                     AND ab1.ins_type != 26001 \n");
		
		sql.append("          WHERE  match_status = 'M'\n");
		sql.append("                 AND apb.metal_type = ").append(metalType).append("\n");
		sql.append("                 AND apb.match_date = '").append(matchDateString).append("'\n");
		sql.append("                 AND apb.customer_id = ").append(customerId).append(") link\n");
		sql.append("      ON aps.deal_num = link.sell_deal_num 	\n");	
		
		return runSQL(sql.toString());
	}
}
