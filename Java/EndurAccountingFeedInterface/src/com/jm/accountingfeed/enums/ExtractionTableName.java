package com.jm.accountingfeed.enums;

import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;

/**
 * Boundary table extraction table names for the interfaces messages to get stored
 * for diagnostics and stamping
 */
public enum ExtractionTableName 
{
	LEDGER_EXTRACTION("USER_jm_ledger_extraction"),
	SALES_LEDGER("USER_jm_bt_out_sl"),
	GENERAL_LEDGER("USER_jm_bt_out_gl"),
	METALS_LEDGER("USER_jm_bt_out_ml"),
	REF_DATA_EXTRACT("USER_jm_bt_out_ref");
	
	private final String name;

    private ExtractionTableName(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
        
    public static ExtractionTableName fromString(String val) throws AccountingFeedRuntimeException 
    {
        for (ExtractionTableName e : ExtractionTableName.values()) 
        {
            if (e.name.equalsIgnoreCase(val)) 
            {
                return e;
            }
        }
        
        throw new AccountingFeedRuntimeException("Invalid ExtractionTableName: " + val); 
    }

}
