package com.olf.jm.advancedPricingReporting.util;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/**
 * A factory for creating Margin objects. Used in the calculation of the tier 1 and tier 2 margins
 */
public class MarginFactory {

	/**
	 * The Enum EnumPricingType.
	 */
	public enum EnumPricingType {
		
		/** Advanced Pricing. */
		AP("AP"), 
		
		/** Deferred Pricing */
		DP("DP");
		
		/** The label used in the db */
		private String dbName;
		
		/**
		 * Instantiates a new enum pricing type.
		 *
		 * @param dbName the db name
		 */
		EnumPricingType(String dbName) {
			this.dbName = dbName;
		}
		
		/**
		 * Gets the db name.
		 *
		 * @return the db name
		 */
		public String getDbName() {
			return dbName;
		}
	}
	
	/**
	 * The Class MarginResults. Stores the margin calculation results. 
	 */
	public class MarginResults {
		
		/** The margin calculated. */
		private double value; 
		
		/** The percentage used in the calculation. */
		private double percentage;
		
		/** The min vol. */
		private double minVol;
		
		/** The max vol. */
		private double maxVol;
		
		/** The volume used in calcualtion. */
		private double volumeUsed;
		
		
		/**
		 * Gets the margin value.
		 *
		 * @return the margin value
		 */
		public double getValue() {
			return value;
		}
		
		/**
		 * Gets the percentage used in the calculation.
		 *
		 * @return the percentage
		 */
		public double getPercentage() {
			return percentage;
		}
		
		/**
		 * Gets the tier min vol.
		 *
		 * @return the minVol
		 */
		public double getMinVol() {
			return minVol;
		}
		
		/**
		 * Gets the tier max vol.
		 *
		 * @return the maxVol
		 */
		public double getMaxVol() {
			return maxVol;
		}
		
		/**
		 * Gets the volume used in the calculation.
		 *
		 * @return the volume used
		 */
		public double getVolumeUsed() { 
			return volumeUsed;
		}


	}
	
	/** The margin percentages loaded from the database. */
	private Table marginPercentages;
	
	/**
	 * Instantiates a new margin factory.
	 *
	 * @param context the current script context
	 * @param reportDate the date the report is running for
	 * @param customerId the customer id the report is running for
	 */
	public MarginFactory(Context context, Date reportDate, int customerId) {
		loadMarginData( context,  reportDate,  customerId);		
	}
	
	/**
	 * Calculate the tier 1 and tier 2 margins.
	 *
	 * @param priceType the price type to calculate the margin for AP or DP
	 * @param metal the metal used in calculation
	 * @param weight the total section weight
	 * @param price the average deal price
	 * @return the margin results[] tier1 - tier2
	 */
	public MarginResults[] calculateMargin(EnumPricingType priceType, String metal, double weight, double price) {
		
		 MarginResults[] results = new  MarginResults[2];
		 
		ConstTable filteredData = getMarginPercentages(priceType, metal);
		
		for(int tier = 0; tier < 2; tier++) {
			double lowerLimit = filteredData.getDouble("min_vol_toz", tier);
			double upperLimit = filteredData.getDouble("max_vol_toz", tier);
			double tierWeight = calculateTierWeight(Math.abs(weight), lowerLimit, upperLimit);
			
			double percentage = filteredData.getDouble("percentage", tier);
			
			results[tier] = new MarginResults();
			

			results[tier].value = MathUtils.round(tierWeight * price * (percentage / 100.0),2);
			results[tier].percentage = percentage;
			results[tier].minVol = filteredData.getDouble("min_vol_kgs", tier);
			results[tier].maxVol = filteredData.getDouble("max_vol_kgs", tier);
			results[tier].volumeUsed = tierWeight;
		}
		
		return results;
		
	}
	
	/**
	 * Gets the margin percentages from the data loaded from the DB.
	 *
	 * @param priceType the price type to calculate the margin for AP or DP
	 * @param metal the metal used in calculation
	 * @return the margin percentages for the current section / metal
	 */
	private ConstTable getMarginPercentages(EnumPricingType priceType, String metal) {
		ConstTable filteredData =  marginPercentages.createConstView("*", "[metal] == '" + metal + "' AND [price_type] == '" + priceType.getDbName() + "'" );
		
		if(filteredData.getRowCount() != 2) {
			String errorMessage = "Error loading margin percentages for metal " + metal + " price type " + priceType.getDbName();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		return filteredData;
	}
	
	/**
	 * Calculate tier weight to use in the calcualtion.
	 *
	 * @param totalWeight the total section weight
	 * @param lowerLimit the lower limit
	 * @param upperLimit the upper limit
	 * @return the double
	 */
	private double calculateTierWeight(double totalWeight, double lowerLimit, double upperLimit) {
		
		if(lowerLimit > upperLimit) {
			String errorMessage = "Invalid limit configuration. Upper limit " + upperLimit + " must be greater than lower limit " + lowerLimit;
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		double tierWeight = 0.0;
		
		if(totalWeight < lowerLimit ) {
			//Below threashold
			tierWeight = 0.0;			
		} else if(totalWeight <= upperLimit) {
			tierWeight = totalWeight - lowerLimit;
		} else if(totalWeight > upperLimit) {
			tierWeight = upperLimit - lowerLimit;
		}
		return tierWeight;
	}
	
	/**
	 * Load margin data from the db.
	 *
	 * @param context the context
	 * @param reportDate the report date
	 * @param customerId the customer id
	 */
	private void loadMarginData(Context context, Date reportDate, int customerId) {
		StringBuffer sql = new StringBuffer();
		
		String reportDateString = context.getCalendarFactory().getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);
		
		
		sql.append(" SELECT mp.*, \n");
		sql.append("    round(min_vol_kgs * factor,2) AS min_vol_toz,  \n");
		sql.append("    round(max_vol_kgs * factor,2) AS max_vol_toz  \n");
		sql.append(" FROM   user_jm_ap_dp_margin_percn mp  \n");
		sql.append("    JOIN unit_conversion uc  \n");
		sql.append("      ON src_unit_id = 52  \n");
		sql.append("         AND dest_unit_id = 55  \n");
		sql.append(" WHERE  customer_id = ").append(customerId).append("  \n");
		sql.append("    AND start_date < '").append(reportDateString).append("'  \n");
		sql.append("    AND end_date >= '").append(reportDateString).append("'  \n");
		sql.append(" ORDER BY metal, price_type, min_vol_kgs \n");
		IOFactory ioFactory = context.getIOFactory();
		
		Logging.debug("About to run SQL: " + sql.toString());
		
		marginPercentages = ioFactory.runSQL(sql.toString());		
	}
}
