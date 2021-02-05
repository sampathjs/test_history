/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items.tables;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-25 - V0.2 - scurran - add column formatting information 
 */

import com.olf.openrisk.table.EnumColType;

	/**
	 * The Interface ITableColumns. Represents a column in a user table defining the column name 
	 * and the column type.
	 */
	public interface TableColumn {
		/**
		 * Gets the column name.
		 *
		 * @return the column name
		 */
		String getColumnName();
		
		/**
		 * Gets the column type.
		 *
		 * @return the column type
		 */
		EnumColType getColumnType();
		
		
		/** 
		 * Get the formatting type.
		 * 
		 * @return the formatting type for the column date 
		 */
		EnumFormatType getFormatType();
		
		/**
		 * Flag to indicate in the tolerance check need to be applied to the value
		 * 
		 * @return ture in the tolerance check needs to be applied to zero values
		 */
		boolean applyToleranceCheck();
	}
