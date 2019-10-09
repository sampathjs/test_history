package com.matthey.utilities.enums;


/**
 * Desk location set up in Endur
 */
public enum Region 
{
	UK("United Kingdom"),
	HK("Hong Kong"),
	US("United States"),
	CN("China");
	
	private final String name;

    private Region(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
