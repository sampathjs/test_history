package com.olf.recon.enums;

public enum ReportingDeskName 
{
	UK("United Kingdom"),
	US("United States"),
	HK("Hong Kong");
	
	private final String name;

    private ReportingDeskName(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
