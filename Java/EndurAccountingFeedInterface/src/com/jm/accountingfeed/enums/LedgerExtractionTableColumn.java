/**
 * 
 */
package com.jm.accountingfeed.enums;


/**
 * @author SharmV03
 *
 */
public enum LedgerExtractionTableColumn {
	EXTRACTION_ID("extraction_id"),
	ROW_CREATION("row_creation"),
	REGION("region"),
	EXTRACTION_START_TIME("extraction_start_time"),
	EXTRACTION_END_TIME("extraction_end_time"),
	LEDGER_TYPE_NAME("ledger_type_name"),
	EXTRACTION_USER("extraction_user"),
	NUM_ROWS_EXTRACTED("num_rows_extracted"),
	ENDUR_TRADING_DATE("endur_trading_date"),
	ENDUR_BUSINESS_DATE("endur_business_date");
	
	
	private String value;

    private LedgerExtractionTableColumn(String argValue) 
    {
        this.value = argValue;
    }

    @Override
    public String toString() 
    {
        return value;
    }

}
