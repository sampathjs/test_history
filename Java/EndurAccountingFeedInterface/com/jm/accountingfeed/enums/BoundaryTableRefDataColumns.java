package com.jm.accountingfeed.enums;

public enum BoundaryTableRefDataColumns 
{
	EXTRACTION_ID("extraction_id"),
	PARTY_ID("party_id"),
	REGION("region"),
	TIME_IN("time_in"),
	LAST_UPDATE("last_update"),
	PAYLOAD("payload"),
	PROCESS_STATUS("process_status"),
	ERROR_MSG("error_msg");
	
	private String value;

    private BoundaryTableRefDataColumns(String argValue) 
    {
        this.value = argValue;
    }

    @Override
    public String toString() 
    {
        return value;
    }
}
