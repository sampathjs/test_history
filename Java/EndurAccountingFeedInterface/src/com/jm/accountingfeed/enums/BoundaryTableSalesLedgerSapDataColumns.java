package com.jm.accountingfeed.enums;

public enum BoundaryTableSalesLedgerSapDataColumns 
{
	EXTRACTION_ID("extraction_id"),
	ENDUR_DOC_NUM("endur_doc_num"),
	ENDUR_DOC_STATUS("endur_doc_status"),
	REGION("region"),
	TIME_IN("time_in"),
	LAST_UPDATE("last_update"),
	PAYLOAD("payload"),
	PROCESS_STATUS("process_status"),
	ERROR_MSG("error_msg"),
	MESSAGE_KEY_1("message_key_1"),
	MESSAGE_KEY_2("message_key_2"),
	MESSAGE_KEY_3("message_key_3");

	private String value;

    private BoundaryTableSalesLedgerSapDataColumns(String argValue) 
    {
        this.value = argValue;
    }

    @Override
    public String toString() 
    {
        return value;
    }
}
