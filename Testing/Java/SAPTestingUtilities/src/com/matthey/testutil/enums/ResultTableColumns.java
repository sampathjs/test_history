package com.matthey.testutil.enums;

/**
 * This enum contains columns for general ledger result table 
 * @author SharmV03
 *
 */
public enum ResultTableColumns
{
	TRADE_REF("tradeRef"),
	TEST_REFERENCE("test_reference"),
	TRADE_TYPE("tradeType"),
	ACTUAL_TRADEREF("actual_tradeRef"),
	VALIDATION_RESULT("validation_result"),
	COLUMN_NAME("column_name"),
	EXPECTED_VALUE("expected_value"),
	ACTUAL_VALUE("actual_value");

	private final String value;

    /**
     * @param argValue
     */
    private ResultTableColumns( String argValue) 
    {
        this.value = argValue;
    }

    /**
     * Returns value of general ledger result table column enum
     * @return
     */
    public String getValue() 
    {
        return value;
    }
}
