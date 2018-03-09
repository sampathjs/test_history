package com.jm.accountingfeed.enums;

/**
 * Desk location set up in Endur
 */
public enum PartyRegion 
{
	UK("United Kingdom"),
	HK("Hong Kong"),
	US("United States");
	
	private final String name;

    private PartyRegion(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
