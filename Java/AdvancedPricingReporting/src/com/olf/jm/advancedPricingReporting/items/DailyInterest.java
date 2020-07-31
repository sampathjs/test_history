package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealSection;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.jm.advancedPricingReporting.sections.ApDispatchDealSection;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
import com.olf.jm.advancedPricingReporting.util.InterestRateFactory;
import com.olf.jm.advancedPricingReporting.util.PriceFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2020-05-29 - V0.1 - jwaechter - Initial Version
 */

/**
 * The Class DailyInterest. Fills the daily interest column.
 */
public class DailyInterest extends ItemBase {

	private static final String INTEREST_CHARGES_TABLE = "USER_jm_ap_dp_interest_charges";

	/**
	 * Instantiates a new collected deferred pricing metal item.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public DailyInterest(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.DAILY_INTEREST.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.DAILY_INTEREST.getColumnName()};
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {	
		super.addData(toPopulate, reportParameters);

		PriceFactory priceFactory = new PriceFactory(context);
		InterestRateFactory rateFactory = new InterestRateFactory(context);
		Map<String, Double> netOpenPositionDispatchDeals = new HashMap<>();
		Map<String, Double> netOpenPositionFXDeals = new HashMap<>();
		Map<String, Double> netOpenPositionDeferredPricing = new HashMap<>();

		ConstTable reportData = report.getReportData();
		sumUpTotals(netOpenPositionDispatchDeals, netOpenPositionFXDeals,
				netOpenPositionDeferredPricing, reportData);
		double totalOpenExposure = 0;
		for (String metal : netOpenPositionFXDeals.keySet()) {
			double spotRate = priceFactory.getSpotRate(metal);
			PluginLog.info("Market price for metal '" + metal + "': " + spotRate);
			double netOpenPosition = netOpenPositionDispatchDeals.containsKey(metal)?netOpenPositionDispatchDeals.get(metal):0.0d;
			netOpenPosition += netOpenPositionFXDeals.containsKey(metal)?netOpenPositionFXDeals.get(metal):0.0d;
			netOpenPosition += netOpenPositionDeferredPricing.containsKey(metal)?netOpenPositionDeferredPricing.get(metal):0.0d;
			PluginLog.info("Net open position for metal '" + metal + "': " + netOpenPosition);
			PluginLog.info("Exposure for metal '" + metal + "' * spot rate : " + netOpenPosition*spotRate);
			totalOpenExposure += netOpenPosition*spotRate;
		}
		PluginLog.info("Total Open Exposure over all metals: " + totalOpenExposure);
		double interestRate = rateFactory.getRatesFor(reportParameters.getExternalBu(), reportParameters.getReportDate());
		String externalBunit =  context.getStaticDataFactory().getName(EnumReferenceTable.Party, reportParameters.getExternalBu());
		PluginLog.info("interest rate for date '" + context.getCalendarFactory().getSQLString(reportParameters.getReportDate()) + "', external BU " 
				+ externalBunit + ": " + interestRate);
		double dailyInterest = ((interestRate/100.0d) * totalOpenExposure) / 360d;
		PluginLog.info("Daily interest charge for extern bunit '" + externalBunit + "' day '" + reportParameters.getReportDate() + "'((interestRate/100.0d) * totalOpenExposure) / 360d: " + dailyInterest);
		updateInterestChargesUserTable (reportParameters, dailyInterest);
		
		toPopulate.setDouble(EnumFinalBalanceSection.DAILY_INTEREST.getColumnName(), 0, dailyInterest);
	}

	private void updateInterestChargesUserTable(ReportParameters reportParameters, double dailyInterest) {
		String sql = 
				"\nSELECT customer_id, run_date, interest_charge, effective, last_update"
			+   "\nFROM " + INTEREST_CHARGES_TABLE
			+   "\nWHERE "
			+   "\n      customer_id = " + reportParameters.getExternalBu() 
			+   "\n  AND run_date = '" + context.getCalendarFactory().getSQLString(reportParameters.getReportDate()) + "'"
			;
			
		try (Table sqlResult = runSQL(sql);
			 UserTable interestChargeUserTable = context.getIOFactory().getUserTable(INTEREST_CHARGES_TABLE);) {
			sqlResult.setColumnValues("effective", "N");
			sqlResult.setColumnValues("last_update", new Date());
			if (sqlResult.getRowCount() > 0) {				
				interestChargeUserTable.updateRows(sqlResult, "customer_id, run_date, interest_charge");	
			}
		} catch (Exception ex) {
			throw new RuntimeException ("Error deleting old entry in table '" + 
					INTEREST_CHARGES_TABLE + "' for customer_id #" + reportParameters.getExternalBu() +
					" and run_date '" + context.getCalendarFactory().getSQLString(reportParameters.getReportDate()));
		}
		
		try (UserTable interestChargeUserTable = context.getIOFactory().getUserTable(INTEREST_CHARGES_TABLE);
			 Table interestChargeTable = interestChargeUserTable.getTableStructure()) {
			interestChargeTable.addRow();
			interestChargeTable.setInt("customer_id", 0, reportParameters.getExternalBu());
			interestChargeTable.setDate("run_date", 0, reportParameters.getReportDate());
			interestChargeTable.setDouble("interest_charge", 0, dailyInterest);
			interestChargeTable.setString("effective", 0, "Y");
			interestChargeTable.setDate("last_update", 0, new Date());
			interestChargeUserTable.insertRows(interestChargeTable);
		} catch (Exception ex) {
			throw new RuntimeException ("Error creating new entry in table '" + 
					INTEREST_CHARGES_TABLE + "' for customer_id #" + reportParameters.getExternalBu() +
					" and run_date '" + context.getCalendarFactory().getSQLString(reportParameters.getReportDate()));			
		}
	}

	private void sumUpTotals(Map<String, Double> netOpenPositionDispatchDeals,
			Map<String, Double> netOpenPositionFXDeals,
			Map<String, Double> netOpenPositionDeferredPricing,
			ConstTable reportData) {
		Table dispatchDeals = reportData.getTable(ApDispatchDealSection.sectionName(), 0);
		Table fxDeals = reportData.getTable(ApBuySellFxDealSection.sectionName(), 0);
		Table deferredPricing = reportData.getTable(DeferredPricingSection.sectionName(), 0);
		
		// calculate totals for AP DP Dispatch Deals
		for (int row=dispatchDeals.getRowCount()-1; row >= 0; row--) {
			String metal = dispatchDeals.getString("metal_short_name", row);
			double openPosition = dispatchDeals.getDouble("total_weight_toz", row);
			if (netOpenPositionDispatchDeals.containsKey(metal)) {
				openPosition += netOpenPositionDispatchDeals.get(metal);
			}
			netOpenPositionDispatchDeals.put(metal, openPosition);
		}
		for (Map.Entry<String, Double>  metalAndPosition : netOpenPositionDispatchDeals.entrySet()) {
			PluginLog.info("Total position for metal '" + 
					metalAndPosition.getKey() + "' for report '" + ApDispatchDealSection.sectionName() 
					+ "': " + metalAndPosition.getValue());
		}
		// calculate totals for AP DP Buy Sell FX Deals
		for (int row=fxDeals.getRowCount()-1; row >= 0; row--) {
			String metal = fxDeals.getString("metal_short_name", row);
			double openPosition = fxDeals.getDouble("total_weight_toz", row);
			if (netOpenPositionFXDeals.containsKey(metal)) {
				openPosition += netOpenPositionFXDeals.get(metal);
			}
			netOpenPositionDispatchDeals.put(metal, openPosition);
			// ensure netOpenPositionFXDeals contain non null values for all metals
			netOpenPositionFXDeals.put(metal, netOpenPositionFXDeals.get(metal)==null?0:netOpenPositionFXDeals.get(metal));
		}
		for (Map.Entry<String, Double>  metalAndPosition : netOpenPositionFXDeals.entrySet()) {
			PluginLog.info("Total position for metal '" + 
					metalAndPosition.getKey() + "' for report '" + ApBuySellFxDealSection.sectionName() 
					+ "': " + metalAndPosition.getValue());
		}
		// calculate totals for Deferred Pricing
		for (int row=deferredPricing.getRowCount()-1; row >= 0; row--) {
			String metal = deferredPricing.getString("metal_short_name", row);
			double openPosition = deferredPricing.getDouble("total_weight_toz", row);
			if (netOpenPositionDeferredPricing.containsKey(metal)) {
				openPosition += netOpenPositionDeferredPricing.get(metal);
			}
			netOpenPositionDeferredPricing.put(metal, openPosition);
			// ensure netOpenPositionFXDeals contain non null values for all metals
			netOpenPositionFXDeals.put(metal, netOpenPositionFXDeals.get(metal)==null?0:netOpenPositionFXDeals.get(metal));
		}
		for (Map.Entry<String, Double>  metalAndPosition : netOpenPositionDeferredPricing.entrySet()) {
			PluginLog.info("Total position for metal '" + 
					metalAndPosition.getKey() + "' for report '" + DeferredPricingSection.sectionName() 
					+ "': " + metalAndPosition.getValue());
		}
	}
	
	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the collected dp metal field. Unable to access the report data.";
			
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the collected dp metal field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}		
}
