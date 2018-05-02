package com.jm.accountingfeed.enums;

public enum BoundaryTableGeneralLedgerDataColumns 
{
	EXTRACTION_ID("extraction_id"),
	DEAL_NUM("deal_num"),
	TRAN_NUM("tran_num"),
	TRAN_STATUS("tran_status"),
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

    private BoundaryTableGeneralLedgerDataColumns(String argValue) 
    {
        this.value = argValue;
    }

    @Override
    public String toString() 
    {
        return value;
    }
}
