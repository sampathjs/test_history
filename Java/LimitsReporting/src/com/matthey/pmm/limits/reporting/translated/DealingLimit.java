package com.matthey.pmm.limits.reporting.translated;

public class DealingLimit {
    private final String limitType;
    private final String desk;
    private final String metal;
    private final int limit;
    
    public DealingLimit (
    	    final String limitType,
    	    final String desk,
    	    final String metal,
    		final int limit) {
    	this.limitType = limitType;
    	this.desk = desk;
    	this.metal = metal;
    	this.limit = limit;
    }

	public String getLimitType() {
		return limitType;
	}

	public String getDesk() {
		return desk;
	}

	public String getMetal() {
		return metal;
	}

	public int getLimit() {
		return limit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((desk == null) ? 0 : desk.hashCode());
		result = prime * result + limit;
		result = prime * result + ((limitType == null) ? 0 : limitType.hashCode());
		result = prime * result + ((metal == null) ? 0 : metal.hashCode());
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
		DealingLimit other = (DealingLimit) obj;
		if (desk == null) {
			if (other.desk != null)
				return false;
		} else if (!desk.equals(other.desk))
			return false;
		if (limit != other.limit)
			return false;
		if (limitType == null) {
			if (other.limitType != null)
				return false;
		} else if (!limitType.equals(other.limitType))
			return false;
		if (metal == null) {
			if (other.metal != null)
				return false;
		} else if (!metal.equals(other.metal))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DealingLimit [limitType=" + limitType + ", desk=" + desk + ", metal=" + metal + ", limit=" + limit
				+ "]";
	}
}
