package com.matthey.utilities.enums;

/**
 * 
 * Description:
 * This Enum is used to label results in comparing 2 csv Files
 * Revision History:
 * 07.05.20  GuptaN02  initial version
 *  
 */

public enum ValidationResults {
	ORPHAN_IN_ACTUAL_TABLE("Cancelled",5),
	ORPHAN_IN_EXPECTED_TABLE("New",6),
	MATCHING("Matching",1),
	MATCHING_WITH_TOLERANCE("Matching With Tolerance",2),
	NOT_MATCHING("Impacted",0),
	AMENDED("Amended",4);

	private final String value;
	private final int code;


	public String getValue() {
		return value;
	}

	public int getCode() {
		return code;
	}


	ValidationResults(String value, int code)
	{
		this.value=value;
		this.code=code;
	}
}
