package com.jm.accountingfeed.enums;

/**
 * This represents a status that an interface message can transition through in the
 * user_jm_bt_out* tables
 */
public enum AuditRecordStatus 
{
	NEW("N"),
	PROCESSED("P"),
	ERROR("E");
	
	private final String name;

    private AuditRecordStatus(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
