package com.jm.accountingfeed.enums;

public enum ReconciliationTableDataColumns {
	EXTRACTION_ID("extraction_id"),    //
	INTERFACE_MODE ("interface_mode"), //
	REGION("region"), //
	INTERNAL_BUNIT("internal_bunit"), //
	DEAL_NUM("deal_num"), //
	DOCUMENT_NUM("endur_doc_num"), //
	TRADE_DATE("trade_date"), //
	METAL_VALUE_DATE("metal_value_date"), //
	VALUE_DATE("cur_value_date"), //
	ACCOUNT_NUM("account_num"), // 
	QTY_TOZ("qty_toz"), //
	LEDGER_AMOUNT("ledger_amount"), //
	TAX_AMOUNT("tax_amount"), //
	DEBIT_CREDIT("debit_credit"), //
	LEDGER_TYPE("ledger_type"), // 
	TIME_IN("time_in"), // 
	DOC_DATE("doc_date"), //
	CURRENCY("currency"), //
	;
	
	private String value;

    private ReconciliationTableDataColumns(String argValue) 
    {
        this.value = argValue;
    }

    @Override
    public String toString() 
    {
        return value;
    }
}
