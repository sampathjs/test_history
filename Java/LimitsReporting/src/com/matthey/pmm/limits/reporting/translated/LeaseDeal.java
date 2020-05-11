package com.matthey.pmm.limits.reporting.translated;

import org.joda.time.LocalDateTime;

public class LeaseDeal {
    private final String tranNum;
    private final LocalDateTime startDate;
    private final double notnl;
    private final double currencyFxRate;
    
    public LeaseDeal (  final String tranNum,
    					final LocalDateTime startDate,
    					final double notnl,
    					final double currencyFxRate) {
    	this.tranNum = tranNum;
    	this.startDate = startDate;
    	this.notnl = notnl;
    	this.currencyFxRate = currencyFxRate;
    }

	public String getTranNum() {
		return tranNum;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public double getNotnl() {
		return notnl;
	}

	public double getCurrencyFxRate() {
		return currencyFxRate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(currencyFxRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(notnl);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
		result = prime * result + ((tranNum == null) ? 0 : tranNum.hashCode());
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
		LeaseDeal other = (LeaseDeal) obj;
		if (Double.doubleToLongBits(currencyFxRate) != Double.doubleToLongBits(other.currencyFxRate))
			return false;
		if (Double.doubleToLongBits(notnl) != Double.doubleToLongBits(other.notnl))
			return false;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		if (tranNum == null) {
			if (other.tranNum != null)
				return false;
		} else if (!tranNum.equals(other.tranNum))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LeaseDeal [tranNum=" + tranNum + ", startDate=" + startDate + ", notnl=" + notnl + ", currencyFxRate="
				+ currencyFxRate + "]";
	}
}
