package com.olf.jm.SapInterface.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * The Class DateUtils. Utility functions used for 
 * data manipulation. 
 */
public final class DateUtils {
	
	/**
	 * Hide constructor as class only contains static methods.
	 */
	private DateUtils() {
		
	}
	
	/** The Constant DATE_FORMATS. Contains a list of the supported data formats. */
	static final List<String> DATE_FORMATS = Arrays.asList(
			    "yyyy-MM-dd",
			    "yyyy-MMM-dd",
				"yyyyMMdd",
				"dd-MM-yyyy", 
				"dd-MMM-yyyy");    

	/** The Constant DATE_PATTERNS. contains the date formats supported by the interface */
	static final List<String> DATE_PATTERNS = Arrays.asList(
			"^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$", 
			"^[0-9][0-9][0-9][0-9]-[a-zA-Z][a-zA-Z][a-zA-Z]-[0-9][0-9]$", 
			"^[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]$",
			"^[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]$",
			"^[0-9][0-9]-[a-zA-Z][a-zA-Z][a-zA-Z]-[0-9][0-9][0-9][0-9]$");    

	/**
	 * Construct a data object from a string containing a date in a supported format. 
	 * Null string returned is format is not supported.
	 *
	 * @param value string containing the date to parse
	 * @return the date
	 */
	public static Date getDate(final String value) {
		Date inputDate = null;
		
		for (int i = 0; i < DATE_PATTERNS.size(); i++) {
			if (value.matches(DATE_PATTERNS.get(i))) {
				SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMATS.get(i));
				try {
					inputDate = sdf.parse(value);
					break;
				} catch (ParseException e) {
					// intentionally empty
				}				
			}
		}

		
		return inputDate;
	}
}
