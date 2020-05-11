package com.matthey.pmm.limits.reporting.translated;

public class LiquidityLimit {
    private final String metal;
    private final int lowerLimit;
    private final int upperLimit;
    private final int maxLiability;
    
    public LiquidityLimit (    final String metal,
    		final int lowerLimit,
    		final int upperLimit,
    		final int maxLiability ) {
    	this.metal = metal;
    	this.lowerLimit = lowerLimit;
    	this.upperLimit = upperLimit;
    	this.maxLiability = maxLiability;
    }

	public String getMetal() {
		return metal;
	}

	public int getLowerLimit() {
		return lowerLimit;
	}

	public int getUpperLimit() {
		return upperLimit;
	}

	public int getMaxLiability() {
		return maxLiability;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lowerLimit;
		result = prime * result + maxLiability;
		result = prime * result + ((metal == null) ? 0 : metal.hashCode());
		result = prime * result + upperLimit;
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
		LiquidityLimit other = (LiquidityLimit) obj;
		if (lowerLimit != other.lowerLimit)
			return false;
		if (maxLiability != other.maxLiability)
			return false;
		if (metal == null) {
			if (other.metal != null)
				return false;
		} else if (!metal.equals(other.metal))
			return false;
		if (upperLimit != other.upperLimit)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LiquidityLimits [metal=" + metal + ", lowerLimit=" + lowerLimit + ", upperLimit=" + upperLimit
				+ ", maxLiability=" + maxLiability + "]";
	}
}
