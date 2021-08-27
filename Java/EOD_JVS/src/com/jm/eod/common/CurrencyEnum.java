package com.jm.eod.common;

public enum CurrencyEnum {
	
	USD("USD"),
	GBP("GBP"),
	ZAR("ZAR");

	private final String name;

    private CurrencyEnum(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
