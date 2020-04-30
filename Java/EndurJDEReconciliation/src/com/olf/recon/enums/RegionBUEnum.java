package com.olf.recon.enums;

/***
 * 
 * @author joshig01
 * 
 * This enum will have all the fields for SAP Recon.
 * 
 */

public enum RegionBUEnum 
{
	CHINA("China","20755"),
	UK("United Kingdom", "20006,20008"),
	US("United States","20001");
	
	private final String region;
	private final String listOfBU;
	

    private RegionBUEnum(final String region, final String BUs) 
    {
    	this.region = region;
        this.listOfBU = BUs;
    }

    @Override
    public String toString() 
    {
        return listOfBU;
    }
    
    public String getRegion()
    {
    	return region;
    }
    
    public String getBUs() 
    {
    	return listOfBU;
    }
}
