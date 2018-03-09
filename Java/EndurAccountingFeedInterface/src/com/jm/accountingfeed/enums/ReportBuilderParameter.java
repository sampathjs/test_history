package com.jm.accountingfeed.enums;

/**
 * Report Builder parameters used across the interface report layer
 */
public enum ReportBuilderParameter
{
	REGIONAL_SEGREGATION("regional_segregation"),
	BOUNDARY_TABLE("boundary_table"),
	TARGET_FILENAME("TARGET_FILENAME"),
	TARGET_DIR("TARGET_DIR"),
	EXCLUDE_COUNTERPARTIES("exclude_counterparty"),
	INCLUDE_INTERNAL_LENTITIES("include_internal_lentity");
	
	private String value;

    private ReportBuilderParameter(String argValue) 
    {
        this.value = argValue;
    }

    @Override
    public String toString() 
    {
        return value;
    }
}
