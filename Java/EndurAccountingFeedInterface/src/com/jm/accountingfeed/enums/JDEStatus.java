package com.jm.accountingfeed.enums;

/**
 * JDE stamping status that is used on a deal or invoice
 */
public enum JDEStatus 
{
	PENDING_SENT("Pending Sent"),
	SENT("Sent"),
	PENDING_CANCELLED("Pending Cancelled"),
	CANCELLED_SENT("Cancelled Sent"),
	NOT_SENT("NOT Sent");
		
	private final String name;

    private JDEStatus(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
