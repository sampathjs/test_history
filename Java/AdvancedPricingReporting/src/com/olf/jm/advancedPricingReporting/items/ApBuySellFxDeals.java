package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealSection;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.util.MathUtils;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApBuySellFxDeals. Generates the data needed for section containing information
 * about advanced pricing buy / sell FX deals. 
 */
public class ApBuySellFxDeals extends ItemBase {

	
	/**
	 * Instantiates a new ap buy / sell FX deals section object.
	 *
	 * @param currentContext the script context
	 */
	public ApBuySellFxDeals(Context currentContext) {
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
			
			TableColumnHelper<EnumFxDealSection> columnHelper = new TableColumnHelper<EnumFxDealSection>();
			Table dummy = columnHelper.buildTable(context, EnumFxDealSection.class, "FXBuySellDeal");
			toPopulate.setTable(EnumFxDealSection.REPORT_DATA.getColumnName(), 0, dummy);
			Logging.info("No data to process for customer " + reportParameters.getExternalBu()
					+ " reporting date " + reportParameters.getReportDate());
			return;
		}
		
		// Loop over each metal
		int rowCount = metalsToProcess.getRowCount();
		for(int row = 0;  row < rowCount; row++ ) {
			toPopulate.addRows(1);
			
			toPopulate.setString(EnumFxDealSection.METAL_NAME.getColumnName(), row, metalsToProcess.getString("description", row));
			toPopulate.setString(EnumFxDealSection.METAL_SHORT_NAME.getColumnName(), row, metalsToProcess.getString("name", row));		
			
			Table metalSectionDetails = getReportOutput(reportParameters,metalsToProcess.getInt("metal_type", row));
			
			toPopulate.setTable(EnumFxDealSection.REPORT_DATA.getColumnName(), row, metalSectionDetails);
			
			// total buy settle amount
			int columnId = metalSectionDetails.getColumnId(EnumFxDealData.BUY_SETTLE_VALUE.getColumnName());
			double totalBuySettleAmount = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumFxDealSection.TOTAL_BUY.getColumnName(), row, totalBuySettleAmount);
			
			// total sell settle amount
			columnId = metalSectionDetails.getColumnId(EnumFxDealData.SELL_SETTLE_VALUE.getColumnName());
			double totalSellSettleAmount = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumFxDealSection.TOTAL_SELL.getColumnName(), row, totalSellSettleAmount);
			
			// loss gain calculation
			double lossGain =  (totalBuySettleAmount + totalSellSettleAmount) * -1.0;
			toPopulate.setDouble(EnumFxDealSection.LOSS_GAIN.getColumnName(), row, lossGain);
			
			
			// calculate total weight
			columnId = metalSectionDetails.getColumnId(EnumFxDealData.VOLUME_IN_TOZ.getColumnName());
			double totalWeightToz = metalSectionDetails.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumFxDealSection.TOTAL_WEIGHT_TOZ.getColumnName(), row, totalWeightToz);	
			
			
			// Load the HK conversion factor
			double hkConversionFacort = MathUtils.getHkTozToGmsConversionFactor();
			double totalWeightGms = hkConversionFacort * MathUtils.round(totalWeightToz,3);
			toPopulate.setDouble(EnumFxDealSection.TOTAL_WEIGHT_GMS.getColumnName(), row, MathUtils.gmsRounding(totalWeightGms, 2));			
			
		}
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#format(com.olf.openrisk.table.Table)
	 */
	@Override
	public Table format(Table reportSectionToFormat) {
		TableColumnHelper<EnumFxDealSection> columnHelper = new TableColumnHelper<EnumFxDealSection>();
		
		columnHelper.formatTableForOutput(EnumFxDealSection.class, reportSectionToFormat);
		
		
		for(int row = 0; row < reportSectionToFormat.getRowCount(); row++) {
			Table subReport = reportSectionToFormat.getTable(EnumFxDealSection.REPORT_DATA.getColumnName(), row);
			if(subReport != null && subReport.getRowCount() > 0) {
				reportSectionToFormat.setTable(EnumFxDealSection.REPORT_DATA.getColumnName(), row, formatSubReport(subReport));
			}
		}
		return reportSectionToFormat;
	}
	
	/**
	 * Format the sub report report.
	 *
	 * @param reportSectionToFormat the report section to format
	 * @return the formatted report
	 */
	private Table formatSubReport(Table reportSectionToFormat) {

		
		TableColumnHelper<EnumFxDealData> columnHelper = new TableColumnHelper<EnumFxDealData>();
		
		columnHelper.formatTableForOutput(EnumFxDealData.class, reportSectionToFormat);

		// Sort by deal number ascending
		reportSectionToFormat.sort(EnumFxDealData.DEAL_NUM.getColumnName(), true);
		
		return reportSectionToFormat;
	}	


	/**
	 * Gets the report output containing matched buy / sell FX deals. 
	 *
	 * @param reportParameters the parameters controlling the data to be extracted
	 * @param metal the metal to report from
	 * @return the report output
	 */
	private Table getReportOutput(ReportParameters reportParameters, int metal) {
		Table sectionData = context.getTableFactory().createTable("Report_Output");
		for(EnumFxDealData column : EnumFxDealData.values()) {
			sectionData.addColumn(column.getColumnName(), column.getColumnType());
		}
		
		// Load the HK conversion factor
		double hkConversionFacort = MathUtils.getHkTozToGmsConversionFactor();
		
		try( Table fxSellDeals = loadFxSellDetails(reportParameters.getExternalBu(), reportParameters.getReportDate(), metal, hkConversionFacort)) {
			aggregateSellData(fxSellDeals, sectionData);
		}

		try( Table fxBuyDeals = loadFXBuyDetails(reportParameters.getExternalBu(), reportParameters.getReportDate(), metal, hkConversionFacort)) {
			sectionData.appendRows(fxBuyDeals);
		}
		
		for(int row = 0; row < sectionData.getRowCount(); row++) {
			sectionData.setDouble(EnumDispatchDealData.VOLUME_IN_GMS.getColumnName(), row, 
					MathUtils.gmsRounding(sectionData.getDouble((EnumDispatchDealData.VOLUME_IN_GMS.getColumnName()), row), 2));
		}
			
		sectionData.setName("FXBuySellDeal");
		return sectionData;
	}

	/**
	 * Aggregate sell data. Sell FX deals can contain multiple entries in the table, combine these into a single entry based on the deal 
	 * number, calculate the average price based on the aggregated totals.
	 *
	 * @param sellDeals the sell deals loaded from the database
	 * @param sectionData the section to populate with data
	 */
	private void aggregateSellData(Table sellDeals, Table sectionData) {
		Table dealNumbers = sellDeals.getDistinctValues(EnumFxDealData.DEAL_NUM.getColumnName());
		
		for(int i = 0; i < dealNumbers.getRowCount(); i++) {
			int dealNumber = dealNumbers.getInt(0, i);
			
			ConstTable dealData = sellDeals.createConstView("*", "[" + EnumFxDealData.DEAL_NUM.getColumnName() + "] == " + dealNumber);
			
			int columnId = dealData.getColumnId(EnumFxDealData.VOLUME_IN_TOZ.getColumnName());
			double totalVolumeToz = dealData.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
			columnId = dealData.getColumnId(EnumFxDealData.VOLUME_IN_GMS.getColumnName());
			double totalVolumeGms = dealData.calcAsDouble(columnId, EnumColumnOperation.Sum);

			double totalSettleValue;
			if(MathUtils.round(totalVolumeToz, TableColumnHelper.TOZ_DECIMAL_PLACES) == 0.0) {
				totalSettleValue = 0.0;
			} else {
				columnId = dealData.getColumnId(EnumFxDealData.SELL_SETTLE_VALUE.getColumnName());
				totalSettleValue = dealData.calcAsDouble(columnId, EnumColumnOperation.Sum);
			}
			
			int newRow = sectionData.addRows(1);
			
			sectionData.setRowValues(newRow, new Object[] {
					dealData.getDate(EnumFxDealData.FIXED_DATE.getColumnName(), 0),
					dealData.getInt(EnumFxDealData.DEAL_NUM.getColumnName(), 0),
					totalVolumeToz,
					totalVolumeGms,
					totalSettleValue / totalVolumeToz,
					0.0,
					totalSettleValue,
					"FX Sell",
					dealData.getString(EnumFxDealData.REFERENCE.getColumnName(), 0)
			});
		}		
	}
	

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		
		TableColumnHelper<EnumFxDealSection> columnHelper = new TableColumnHelper<EnumFxDealSection>();

		return columnHelper.getColumnTypes(EnumFxDealSection.class);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		TableColumnHelper<EnumFxDealSection> columnHelper = new TableColumnHelper<EnumFxDealSection>();

		return columnHelper.getColumnNames(EnumFxDealSection.class);
	}
	
	/**
	 * Load metals to process. Returns a list of the metals where there is activity for the given custom and run date. 
	 *
	 * @param customerId the customer id currently being processed
	 * @param matchDate the date the report is being run for
	 * @return the table
	 */
	private Table loadMetalsToProcess(int customerId, Date matchDate) {
		
		StringBuffer sql = new StringBuffer();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(matchDate, EnumDateFormat.DlmlyDash);

		sql.append(" SELECT DISTINCT metal_type, \n");
		sql.append("         name, \n");
		sql.append("         description \n");
		sql.append(" FROM   user_jm_ap_buy_dispatch_deals ap \n");
		sql.append(" JOIN ab_tran ab \n");
		sql.append("  ON ab.deal_tracking_num = ap.deal_num \n");
		sql.append("     AND tran_status = 3 \n");
		sql.append("     AND current_flag = 1 \n");
		sql.append("     AND ins_type = 26001 \n");
		sql.append(" JOIN ab_tran_info_view abt \n");
		sql.append("  ON abt.tran_num = ab.tran_num \n");
		sql.append("     AND type_name = 'Pricing Type' \n");
		sql.append("     AND value = 'AP' \n");
		sql.append(" JOIN currency c \n");
		sql.append("  ON metal_type = c.id_number \n");
		sql.append(" WHERE  match_date = '").append(matchDateString).append("' \n");
		sql.append(" AND customer_id = ").append(customerId).append(" \n");
		       		
		return runSQL(sql.toString());
	}
	
	/**
	 * Load fx sell details that are used in the report.
	 *
	 * @param externalBu the id for the customer currently being processed
	 * @param reportDate the date the report is being run for
	 * @param metal the metal to process
	 * @return the table containing FX Sell deal data.
	 */
	private Table loadFxSellDetails(int externalBu, Date reportDate, int metal, double hkUnitConversion) {
		StringBuffer sql = new StringBuffer();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT match_date                                                      AS fixed_date, \n");
		sql.append("        sell_deal_num                                                   AS deal_num, \n");
		sql.append("        Abs(match_volume) * -1                                          AS volume_in_toz, \n");
		sql.append("        ").append(hkUnitConversion).append(" * ROUND(( Abs(match_volume) * -1 )," + TableColumnHelper.TOZ_DECIMAL_PLACES+ ")  AS volume_in_gms, \n");
		sql.append("        ( Cast(Isnull(tp.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) AS trade_price, \n");
		sql.append("        0.0                                                             AS buy_settle_value, \n");
		sql.append("        ( Cast(Isnull(tp.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) *  Abs(match_volume) AS sell_settle_value, \n");
		sql.append("        'FX Sell'                                                       AS type, \n");
		sql.append("        ab.reference                                                    AS reference \n");
		sql.append(" FROM   user_jm_ap_buy_sell_link \n");

		sql.append("        JOIN ab_tran ab \n");
		sql.append("          ON ab.deal_tracking_num = sell_deal_num \n");
		sql.append("             AND tran_status = 3 \n");
		sql.append("             AND current_flag = 1 \n");
		sql.append("             AND ins_type = 26001 \n");
		sql.append("       LEFT JOIN ab_tran_info_view tp \n");
		sql.append("              ON tp.tran_num = ab.tran_num \n");
		sql.append("                 AND type_name = 'Trade Price' \n");
		sql.append("       JOIN ab_tran_info_view pt \n");
		sql.append("              ON pt.tran_num = ab.tran_num \n");
		sql.append("                 AND type_name = 'Pricing Type' \n");
		sql.append("                 AND value = 'AP' \n");
		sql.append("       LEFT JOIN unit_conversion uc2 \n");
		sql.append("               ON uc2.src_unit_id = (SELECT Iif(unit1 != 0, unit1, unit2) AS \n");
		sql.append("                                            deal_unit \n");
		sql.append("                                     FROM   fx_tran_aux_data \n");
	    sql.append("                                     WHERE  tran_num = ab.tran_num) \n");
	    sql.append("                 AND uc2.dest_unit_id = 55 \n");
	    sql.append(" WHERE  match_date = '").append(matchDateString).append("' \n");
	    sql.append(" AND  buy_deal_num IN (SELECT deal_num \n");
	    sql.append("                   FROM   user_jm_ap_buy_dispatch_deals ap \n");
	    sql.append("                           JOIN ab_tran ab \n");
	    sql.append("                              ON ab.deal_tracking_num = ap.deal_num \n");
	    sql.append("                                 AND tran_status = 3 \n");
	    sql.append("                                 AND current_flag = 1 \n");
	    sql.append("                                 AND ins_type = 26001 \n");
	    sql.append("                     WHERE  match_date = '").append(matchDateString).append("' \n");
	    sql.append("                           AND customer_id = ").append(externalBu).append(" \n");
	    sql.append("                           AND metal_type = ").append(metal).append(") \n");
	    
	    return runSQL(sql.toString());
	}


	/**
	 * Load fx buy details used in the report output.
	 *
	 * @param externalBu the customer the report is being run for
	 * @param reportDate the date the report is being run for
	 * @param metal the metal  to process. 
	 * @return the table
	 */
	private Table loadFXBuyDetails(int externalBu, Date reportDate, int metal, double hkUnitConversion) {

		StringBuffer sql = new StringBuffer();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);

		sql.append(" SELECT ap.match_date  AS fixed_date, \n");
		sql.append("        deal_num, \n");
		sql.append("        match_volume AS volume_in_toz,\n");
		sql.append("        ROUND(match_volume, " + TableColumnHelper.TOZ_DECIMAL_PLACES + ")  * ").append(hkUnitConversion).append(" AS  volume_in_gms, \n");
		sql.append("        ( Cast(Isnull(tp.value, 0.0) AS FLOAT) / Isnull(uc2.factor, 1.0) ) AS  trade_price, \n");
		sql.append("        (match_volume *  ( Cast(Isnull(tp.value, 0.0) AS FLOAT)) / Isnull(uc2.factor, 1.0) ) * -1 AS buy_settle_value, \n");
		sql.append("        0.0 AS sell_settle_value, \n");
		sql.append("       	'FX Buy' AS type, \n");
		sql.append("        ab.reference   AS reference \n");
		sql.append(" FROM   user_jm_ap_buy_dispatch_deals ap \n");
		sql.append("        JOIN ab_tran ab \n");
		sql.append("          ON ab.deal_tracking_num = ap.deal_num \n");
		sql.append("             AND tran_status = 3 \n");
		sql.append("             AND current_flag = 1 \n");
		sql.append("             AND ins_type = 26001 \n");
		sql.append("        LEFT JOIN unit_conversion uc2 \n");
		sql.append("               ON uc2.src_unit_id = (SELECT Iif(unit1 != 0, unit1, unit2) AS \n");
		sql.append("                                            deal_unit \n");
		sql.append("                                     FROM   fx_tran_aux_data \n");
		sql.append("                                     WHERE  tran_num = ab.tran_num)\n");
		sql.append("                  AND uc2.dest_unit_id = 55 \n");
		sql.append("       LEFT JOIN ab_tran_info_view tp \n");
		sql.append("              ON tp.tran_num = ab.tran_num \n");
		sql.append("                 AND tp.type_name = 'Trade Price' \n");
		sql.append("       JOIN ab_tran_info_view pt \n");
		sql.append("              ON pt.tran_num = ab.tran_num \n");
		sql.append("                 AND pt.type_name = 'Pricing Type' \n");
		sql.append("                 AND pt.value = 'AP' \n");
		sql.append("        LEFT JOIN   USER_jm_ap_buy_sell_link link \n");
		sql.append("               ON link.buy_deal_num = deal_num  \n");
		sql.append("               AND ap.match_date = link.match_date  \n");
		sql.append(" WHERE  ap.match_date = '").append(matchDateString).append("' \n");
		sql.append("        AND ap.customer_id = ").append(externalBu).append(" \n");
		sql.append("        AND ap.metal_type = ").append(metal).append("\n");

		return runSQL(sql.toString());
	}
}
