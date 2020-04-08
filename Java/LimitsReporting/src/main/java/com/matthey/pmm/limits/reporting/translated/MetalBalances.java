package com.matthey.pmm.limits.reporting.translated;


import com.google.common.collect.Table;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class MetalBalances {
	private final Table<String, String, String> rawData; 
	
	public static final Map<String, String> metalNames;

	static {
		metalNames = new HashMap<String, String>();
		metalNames.put("XPT", "Platinum");
		metalNames.put("XPD", "Palladium");
		metalNames.put("XRH", "Rhodium");
		metalNames.put("XAU", "Gold");
		metalNames.put("XAG", "Silver");
		metalNames.put("XIR", "Iridium");
		metalNames.put("XOS", "Osmium");
		metalNames.put("XRU", "Ruthenium");
	}
	
	public MetalBalances (final Table<String, String, String> rawData) {
		this.rawData = rawData;
	}
	
	
	
    public int getBalance(final String lineTitle, final String metal) {
        String metalName = metalNames.get(metal);
        String rawBalance = rawData.get(lineTitle, metalName + "\\nActual");
        if (rawBalance == null) {
        	throw new RuntimeException ("No raw Data found for metal '" + metal + "'");
        }
        try {
			return NumberFormat.getInstance().parse(rawBalance).intValue();
		} catch (ParseException e) {
			throw new RuntimeException ("Error parsing balance of metal '" + metal + "': '" + rawBalance + "'");
		} 
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rawData == null) ? 0 : rawData.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetalBalances other = (MetalBalances) obj;
		if (rawData == null) {
			if (other.rawData != null)
				return false;
		} else if (!rawData.equals(other.rawData))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MetalBalances [rawData=" + rawData + "]";
	}
}
