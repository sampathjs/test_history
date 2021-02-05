/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.jm.advancedPricingReporting.sections.ApDispatchDealSection;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
import com.olf.jm.advancedPricingReporting.util.PriceFactory;
import com.olf.openrisk.table.ColumnFormatterAsDouble;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class MetalPricesForToday. Sets the metal price based on the metals used in the deferred and advanced pricing sections.  
 */
public class MetalPricesForToday extends ItemBase {

	 
	/**
	 * Instantiates a new metal prices for today.
	 *
	 * @param context the context
	 * @param report the report
	 */
	public MetalPricesForToday(Context context, Report report) {
		super(context, report);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {EnumColType.Table, EnumColType.Table, EnumColType.Date};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {"dp_prices", "ap_prices", "date"};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);
		
		validateReportStructure();
		
		PriceFactory priceFactory = new PriceFactory(context);
		
		
		validateReportStructure();
		
		ConstTable reportData = report.getReportData();
		Table deferredDeals = reportData.getTable(DeferredPricingSection.sectionName(), 0);
		
		if(deferredDeals == null) {
			String errorMessage = "Error calculating the metal prices for today field. The required section "
					+ DeferredPricingSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(deferredDeals.getRowCount() > 0) {
			Table dpPrices = getPriceTable("dp_prices");
			toPopulate.setTable("dp_prices", 0, dpPrices);
			
			String[] metals = deferredDeals.getColumnValuesAsString(EnumDeferredPricingSection.METAL_SHORT_NAME.getColumnName());
			
			String[] unique = new HashSet<>(Arrays.asList(metals)).toArray(new String[0]);
			
			for(String metal : unique) {
				double dpPrice = priceFactory.getSpotRate(metal);
				
				int newRow = dpPrices.addRows(1);
				
				dpPrices.setString("metal", newRow, metal);
				dpPrices.setDouble("price", newRow, dpPrice);
			}
		}


		Table dispatchDeals = reportData.getTable(ApDispatchDealSection.sectionName(), 0);
		
		if(dispatchDeals == null ) {
			String errorMessage = "Error calculating the metal prices for today field. The required section "
					+ ApDispatchDealSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(dispatchDeals.getRowCount()> 0) {
			
			Table apPrices = getPriceTable("ap_prices");
			toPopulate.setTable("ap_prices", 0, apPrices);
			
			String[] metals = dispatchDeals.getColumnValuesAsString(EnumDispatchDealSection.METAL_SHORT_NAME.getColumnName());
			
			String[] unique = new HashSet<>(Arrays.asList(metals)).toArray(new String[0]);
			
			for(String metal : unique) {
				double dpPrice = priceFactory.getSpotRate(metal);
				
				int newRow = apPrices.addRows(1);
				
				apPrices.setString("metal", newRow, metal);
				apPrices.setDouble("price", newRow, dpPrice);
			}
	
		}		
		
		toPopulate.setDate("date", 0, new Date());
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#format(com.olf.openrisk.table.Table)
	 */
	public Table format(Table reportSectionToFormat) {
		
		Table dpPriceData = reportSectionToFormat.getTable("dp_prices", 0);
		reportSectionToFormat.setTable("dp_prices", 0, formatPriceTable(dpPriceData));
		
		Table apPriceData = reportSectionToFormat.getTable("ap_prices", 0);
		reportSectionToFormat.setTable("ap_prices", 0, formatPriceTable(apPriceData));
		
		
		reportSectionToFormat.convertColumns(new String[]{ "date"}, new EnumColType[] {EnumColType.String});
		
		return reportSectionToFormat;
	}
	
	private Table formatPriceTable(Table priceData) {
		
		TableFormatter tableFormatterDp = priceData.getFormatter();
		
		ColumnFormatterAsDouble columnFormatter = tableFormatterDp.createColumnFormatterAsDouble(EnumFormatDouble.Double, 2, 2);
		
		tableFormatterDp.setColumnFormatter("price", columnFormatter);
		
		priceData.convertColumns(new String[]{ "metal", "price"}, new EnumColType[] {EnumColType.String, EnumColType.String});
		
		return priceData;
	}

	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the metal prices. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the deferred metal field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the deferred metal field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}	
		
		if(!reportData.getColumnNames().contains(ApBuySellFxDealSection.sectionName())) {
			String errorMessage = "Error calculating the advanced metal field. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}			
	}	
	
	private Table getPriceTable(String priceType) {
		Table prices = context.getTableFactory().createTable(priceType);
		
		prices.addColumns(new String[] {"metal",  "price"}, new EnumColType[] {EnumColType.String, EnumColType.Double});
		
		return prices;
	}
}
