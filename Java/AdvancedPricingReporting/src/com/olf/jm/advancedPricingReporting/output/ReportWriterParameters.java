package com.olf.jm.advancedPricingReporting.output;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface ReportWriterParameters. Defines the parmeters that control the output generation
 */
public interface ReportWriterParameters {

	/**
	 * Gets the template name used to generate the output.
	 *
	 * @return the template name
	 */
	String getTemplateName();
	
	/**
	 * Gets the output location where the output is to be written.
	 *
	 * @return the output location
	 */
	String getOutputLocation();
}
