package com.matthey.testutil.common;

/**
 * Result types for comparison
 * @author SharmV03
 *
 */
public enum ValidationResults
{
	MATCHING("Matching"),
	NOT_MATCHING("Not_Matching"),
	TEST_REF_NOT_AVAILABLE_IN_ACTUAL_TABLE("test_reference not available in actual table"),
	TEST_REF_NOT_AVAILABLE_IN_EXPECTED_TABLE("test_reference not available in expected table");

	private final String value;

    private ValidationResults( String argValue) 
    {
        this.value = argValue;
    }

    /**
     * Returns value of validation result
     * @return
     */
    public String getValue() 
    {
        return value;
    }
}