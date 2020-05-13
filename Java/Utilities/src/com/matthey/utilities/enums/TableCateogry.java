package com.matthey.utilities.enums;

/**
 * 
 * Description:
 * This enum is used to differentiate table while creating its structure dynamically.
 * Revision History:
 * 07.05.20  GuptaN02  initial version
 *  
 */

public enum TableCateogry {
	DATASOURCE("Data Source"),
	VALIDATION("Validation");

	private String value="";
	
	public String getValue() {
		return value;
	}

	private TableCateogry(String value)
	{
		this.value=value;
	}

}
    