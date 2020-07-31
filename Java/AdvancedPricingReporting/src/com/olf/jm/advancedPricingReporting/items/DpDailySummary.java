package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.reports.ApDpReportParameters;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */


/**
 * The Class DpDailySummary.
 */
public class DpDailySummary extends ItemBase {

	/**
	 * Instantiates a new dp daily summary item. Populates the columns with DP data. 
	 *
	 * @param context the context
	 */
	public DpDailySummary(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {
				EnumDailySummarySection.AG_DP_TOZ.getColumnType(),
				EnumDailySummarySection.AU_DP_TOZ.getColumnType(),
				EnumDailySummarySection.IR_DP_TOZ.getColumnType(),
				EnumDailySummarySection.OS_DP_TOZ.getColumnType(),
				EnumDailySummarySection.PD_DP_TOZ.getColumnType(),
				EnumDailySummarySection.PT_DP_TOZ.getColumnType(),
				EnumDailySummarySection.RH_DP_TOZ.getColumnType(),
				EnumDailySummarySection.RU_DP_TOZ.getColumnType(),
				EnumDailySummarySection.TIER_1_DP_MARGIN_CALL_PERCENT.getColumnType(),
				EnumDailySummarySection.TIER_1_DP_MARGIN_CALL.getColumnType(),
				EnumDailySummarySection.TIER_2_DP_MARGIN_CALL_PERCENT.getColumnType(),
				EnumDailySummarySection.TIER_2_DP_MARGIN_CALL.getColumnType(),
		    	EnumDailySummarySection.AU_DP_SETTLE_VALUE.getColumnType(),
		    	EnumDailySummarySection.IR_DP_SETTLE_VALUE.getColumnType(),
		    	EnumDailySummarySection.PD_DP_SETTLE_VALUE.getColumnType(),
		    	EnumDailySummarySection.PT_DP_SETTLE_VALUE.getColumnType(),		
		};
		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {
				EnumDailySummarySection.AG_DP_TOZ.getColumnName(),
				EnumDailySummarySection.AU_DP_TOZ.getColumnName(),
				EnumDailySummarySection.IR_DP_TOZ.getColumnName(),
				EnumDailySummarySection.OS_DP_TOZ.getColumnName(),
				EnumDailySummarySection.PD_DP_TOZ.getColumnName(),
				EnumDailySummarySection.PT_DP_TOZ.getColumnName(),
				EnumDailySummarySection.RH_DP_TOZ.getColumnName(),
				EnumDailySummarySection.RU_DP_TOZ.getColumnName(),
				EnumDailySummarySection.TIER_1_DP_MARGIN_CALL_PERCENT.getColumnName(),
				EnumDailySummarySection.TIER_1_DP_MARGIN_CALL.getColumnName(),
				EnumDailySummarySection.TIER_2_DP_MARGIN_CALL_PERCENT.getColumnName(),
				EnumDailySummarySection.TIER_2_DP_MARGIN_CALL.getColumnName(),
		    	EnumDailySummarySection.AU_DP_SETTLE_VALUE.getColumnName(),
		    	EnumDailySummarySection.IR_DP_SETTLE_VALUE.getColumnName(),
		    	EnumDailySummarySection.PD_DP_SETTLE_VALUE.getColumnName(),
		    	EnumDailySummarySection.PT_DP_SETTLE_VALUE.getColumnName(),		
		};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		for(int row = 0; row < toPopulate.getRowCount(); row++) {
			PluginLog.info("Processing customer " + toPopulate.getString(EnumDailySummarySection.CUSTOMER_NAME.getColumnName(), row));
			int customerId = toPopulate.getInt(EnumDailySummarySection.CUSTOMER_ID.getColumnName(), row);
			
			try(Table customerData = getCustomerData(reportParameters.getInternalBu(), customerId, reportParameters.getReportDate())) {
				// Populate the data into the return table
				for(int customerRow = 0; customerRow < customerData.getRowCount(); customerRow++) {

					String metal = customerData.getString( EnumDeferredPricingSection.METAL_SHORT_NAME.getColumnName(),customerRow );
					
					if(metal == null || metal.length() == 0) {
						PluginLog.info("No data to process, skipping customer.");
						continue;
					}
					
					EnumDailySummarySection column = null;
					EnumDailySummarySection columnSettleValue = null;
					
					switch (metal) {
						case "XAG":
							column = EnumDailySummarySection.AG_DP_TOZ;
							break;
						case "XIR":
							column = EnumDailySummarySection.IR_DP_TOZ;
							columnSettleValue = EnumDailySummarySection.IR_DP_SETTLE_VALUE;
							break;
						case "XOS":
							column = EnumDailySummarySection.OS_DP_TOZ;
							break;
						case "XRU":
							column = EnumDailySummarySection.RU_DP_TOZ;
							break;
						case "XPT":
							column = EnumDailySummarySection.PT_DP_TOZ;
							columnSettleValue = EnumDailySummarySection.PT_DP_SETTLE_VALUE;
							break;
						case "XPD":
							column = EnumDailySummarySection.PD_DP_TOZ;
							columnSettleValue = EnumDailySummarySection.PD_DP_SETTLE_VALUE;
							break;		
						case "XAU":
							column = EnumDailySummarySection.AU_DP_TOZ;
							columnSettleValue = EnumDailySummarySection.AU_DP_SETTLE_VALUE;
							break;	
						case "XRH":
							column = EnumDailySummarySection.RH_DP_TOZ;
							break;	
						default:
							String errorMessage = "Error setting DP weight. Metal " + metal + " is not supported.";
							PluginLog.error(errorMessage);
							throw new RuntimeException(errorMessage);
					}
					toPopulate.setDouble(column.getColumnName(), row, 
							customerData.getDouble( EnumDeferredPricingSection.TOTAL_WEIGHT_TOZ.getColumnName(),customerRow ));					
					if (columnSettleValue != null) {
						toPopulate.setDouble(column.getColumnName(), row, 
								customerData.getDouble( EnumDeferredPricingSection.TOTAL_DP_VALUE.getColumnName(),customerRow ));											
					}
					
					toPopulate.setDouble(EnumDailySummarySection.TIER_1_DP_MARGIN_CALL.getColumnName(), row, 
							customerData.getDouble( EnumDeferredPricingSection.TIER_1_DP_VALUE.getColumnName(),customerRow ));

					toPopulate.setDouble(EnumDailySummarySection.TIER_1_DP_MARGIN_CALL_PERCENT.getColumnName(), row, 
							customerData.getDouble( EnumDeferredPricingSection.TIER_1_DP_PERCENTAGE.getColumnName(),customerRow ));
					
					toPopulate.setDouble(EnumDailySummarySection.TIER_2_DP_MARGIN_CALL.getColumnName(), row,
							customerData.getDouble( EnumDeferredPricingSection.TIER_2_DP_VALUE.getColumnName(),customerRow ));
					
					toPopulate.setDouble(EnumDailySummarySection.TIER_2_DP_MARGIN_CALL_PERCENT.getColumnName(), row,
							customerData.getDouble( EnumDeferredPricingSection.TIER_2_DP_PERCENTAGE.getColumnName(),customerRow ));					
				}
			}
			
		}
	}
	
	/**
	 * Gets the customer data. Used the DeferredPricingDeals item to calculate the data for the specified customer
	 *
	 * @param internalBU the internal bu
	 * @param externalBU the external bu
	 * @param reportDate the report date
	 * @return the customer data
	 */
	private Table getCustomerData(int internalBU, int externalBU, Date reportDate) {
			
			DeferredPricingDeals deferredPricingDeals = new DeferredPricingDeals(context);
			
			Table customerData = buildDataTable(deferredPricingDeals);
			
			ReportParameters reportParameters = new ApDpReportParameters(internalBU, externalBU, reportDate);
			
			deferredPricingDeals.addData(customerData, reportParameters);
			
			return customerData;	
		
	}
	
	/**
	 * Builds the data table.
	 *
	 * @param deferredPricingDeals the deferred pricing deals
	 * @return the table
	 */
	private Table buildDataTable(DeferredPricingDeals deferredPricingDeals) {
		TableFactory tf = context.getTableFactory();
		
		Table data = tf.createTable();
		
		data.addColumns(deferredPricingDeals.getColumnNames(), deferredPricingDeals.getDataTypes());
		
		return data;
	}

}
