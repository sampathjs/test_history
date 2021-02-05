/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
import com.olf.jm.advancedPricingReporting.reports.ApDpReportParameters;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDailySummary. Populates at AP columns in the daily summary report
 */
public class ApDailySummary extends ItemBase {

	/**
	 * Instantiates a new ap daily summary.
	 *
	 * @param context the context
	 */
	public ApDailySummary(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {
				EnumDailySummarySection.AG_AP_TOZ.getColumnType(),
				EnumDailySummarySection.AU_AP_TOZ.getColumnType(),
				EnumDailySummarySection.IR_AP_TOZ.getColumnType(),
				EnumDailySummarySection.OS_AP_TOZ.getColumnType(),
				EnumDailySummarySection.PD_AP_TOZ.getColumnType(),
				EnumDailySummarySection.PT_AP_TOZ.getColumnType(),
				EnumDailySummarySection.RH_AP_TOZ.getColumnType(),
				EnumDailySummarySection.RU_AP_TOZ.getColumnType(),
				EnumDailySummarySection.TIER_1_AP_MARGIN_CALL_PERCENT.getColumnType(),
				EnumDailySummarySection.TIER_1_AP_MARGIN_CALL.getColumnType(),
				EnumDailySummarySection.TIER_2_AP_MARGIN_CALL_PERCENT.getColumnType(),
				EnumDailySummarySection.TIER_2_AP_MARGIN_CALL.getColumnType()};

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {
				EnumDailySummarySection.AG_AP_TOZ.getColumnName(),
				EnumDailySummarySection.AU_AP_TOZ.getColumnName(),
				EnumDailySummarySection.IR_AP_TOZ.getColumnName(),
				EnumDailySummarySection.OS_AP_TOZ.getColumnName(),
				EnumDailySummarySection.PD_AP_TOZ.getColumnName(),
				EnumDailySummarySection.PT_AP_TOZ.getColumnName(),
				EnumDailySummarySection.RH_AP_TOZ.getColumnName(),
				EnumDailySummarySection.RU_AP_TOZ.getColumnName(),
				EnumDailySummarySection.TIER_1_AP_MARGIN_CALL_PERCENT.getColumnName(),
				EnumDailySummarySection.TIER_1_AP_MARGIN_CALL.getColumnName(),
				EnumDailySummarySection.TIER_2_AP_MARGIN_CALL_PERCENT.getColumnName(),
				EnumDailySummarySection.TIER_2_AP_MARGIN_CALL.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		for(int row = 0; row < toPopulate.getRowCount(); row++) {
			Logging.info("Processing customer " + toPopulate.getString(EnumDailySummarySection.CUSTOMER_NAME.getColumnName(), row));
			int customerId = toPopulate.getInt(EnumDailySummarySection.CUSTOMER_ID.getColumnName(), row);
			
			try(Table customerData = getCustomerData(reportParameters.getInternalBu(), customerId, reportParameters.getReportDate())) {
				// Populate the data into the return table
				int colIdTotalWeightToz = customerData.getColumnId(EnumDispatchDealSection.TOTAL_WEIGHT_TOZ.getColumnName());
				int colIdTTier1ApValue = customerData.getColumnId(EnumDispatchDealSection.TIER_1_AP_VALUE.getColumnName());
				int colIdTTier1ApPercent = customerData.getColumnId(EnumDispatchDealSection.TIER_1_AP_PERCENTAGE.getColumnName());
				int colIdTTier2ApValue = customerData.getColumnId(EnumDispatchDealSection.TIER_2_AP_VALUE.getColumnName());
				int colIdTTier2ApPercent = customerData.getColumnId(EnumDispatchDealSection.TIER_2_AP_PERCENTAGE.getColumnName());

				
				for(int customerRow = 0; customerRow < customerData.getRowCount(); customerRow++) {

					String metal = customerData.getString( EnumDispatchDealSection.METAL_SHORT_NAME.getColumnName(),customerRow );
					
					if(metal == null || metal.length() == 0) {
						Logging.info("No data to process, skipping customer.");
						continue;
					}
					
					EnumDailySummarySection column;
					switch (metal) {
					case "XAG":
						column = EnumDailySummarySection.AG_AP_TOZ;
						break;
					case "XIR":
						column = EnumDailySummarySection.IR_AP_TOZ;
						break;
					case "XOS":
						column = EnumDailySummarySection.OS_AP_TOZ;
						break;
					case "XRU":
						column = EnumDailySummarySection.RU_AP_TOZ;
						break;
					case "XPT":
						column = EnumDailySummarySection.PT_AP_TOZ;
						break;
					case "XPD":
						column = EnumDailySummarySection.PD_AP_TOZ;
						break;		
					case "XAU":
						column = EnumDailySummarySection.AU_AP_TOZ;
						break;	
					case "XRH":
						column = EnumDailySummarySection.RH_AP_TOZ;
						break;
						default:
							String errorMessage = "Error setting AP weight. Metal " + metal + " is not supported.";
							Logging.error(errorMessage);
							throw new RuntimeException(errorMessage);
					}
					
					toPopulate.setDouble(column.getColumnName(), row, 
							customerData.getDouble( colIdTotalWeightToz, customerRow));
					
					toPopulate.setDouble(EnumDailySummarySection.TIER_1_AP_MARGIN_CALL_PERCENT.getColumnName(), row, 
							customerData.getDouble (colIdTTier1ApPercent, customerRow  ));
					
					toPopulate.setDouble(EnumDailySummarySection.TIER_2_AP_MARGIN_CALL_PERCENT.getColumnName(), row,
							customerData.getDouble( colIdTTier2ApPercent, customerRow  ));
				}
				toPopulate.setDouble(EnumDailySummarySection.TIER_1_AP_MARGIN_CALL.getColumnName(), row, 
						customerData.calcAsDouble( colIdTTier1ApValue, EnumColumnOperation.Sum ));
				
				toPopulate.setDouble(EnumDailySummarySection.TIER_2_AP_MARGIN_CALL.getColumnName(), row,
						customerData.calcAsDouble( colIdTTier2ApValue,EnumColumnOperation.Sum ));
			}
			
		}
		
	}
	
	/**
	 * Gets the customer data. Uses the ApDispatchedDeals item to generate the data
	 *
	 * @param internalBU the internal bu
	 * @param externalBU the external bu
	 * @param reportDate the report date
	 * @return the customer data
	 */
	private Table getCustomerData(int internalBU, int externalBU, Date reportDate) {
		
		ApDispatchedDeals apDispatchedDeals = new ApDispatchedDeals(context);
		
		Table customerData = buildDataTable(apDispatchedDeals);
		
		ReportParameters reportParameters = new ApDpReportParameters(internalBU, externalBU, reportDate);
		
		apDispatchedDeals.addData(customerData, reportParameters);
		
		return customerData;
	
	}
	
	/**
	 * Builds the data table.
	 *
	 * @param apDispatchedDeals the ap dispatched deals
	 * @return the table
	 */
	private Table buildDataTable(ApDispatchedDeals apDispatchedDeals) {
		TableFactory tf = context.getTableFactory();
		
		Table data = tf.createTable();
		
		data.addColumns(apDispatchedDeals.getColumnNames(), apDispatchedDeals.getDataTypes());
		
		return data;
	}

}
