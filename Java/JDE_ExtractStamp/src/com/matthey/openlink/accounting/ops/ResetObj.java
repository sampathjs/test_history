package com.matthey.openlink.accounting.ops;

import java.io.Serializable;

public class ResetObj implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int startDate;
	private int endDate;
	private double resetSpread;
	private double resetNotnl;
	private int resetDate;
	private int riStartDate;
	private int riEndDate;
	private double value;
	private int valStatus;
	private double accrualDCF;
	private double compoundingFactor;
	
	public ResetObj() {
	}
	
	public ResetObj(int startDate, int endDate, double resetSpread, double resetNotnl, int resetDate, 
			int riStartDate, int riEndDate, double value, int valStatus, double accrualDCF, double compoundingFactor) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.resetSpread = resetSpread;
		this.resetNotnl = resetNotnl;
		this.resetDate = resetDate;
		this.riStartDate = riStartDate;
		this.riEndDate = riEndDate;
		this.value = value;
		this.valStatus = valStatus;
		this.accrualDCF = accrualDCF;
		this.compoundingFactor = compoundingFactor;
	}

	public int getStartDate() {
		return startDate;
	}

	public void setStartDate(int startDate) {
		this.startDate = startDate;
	}

	public int getEndDate() {
		return endDate;
	}

	public void setEndDate(int endDate) {
		this.endDate = endDate;
	}

	public double getResetSpread() {
		return resetSpread;
	}

	public void setResetSpread(double resetSpread) {
		this.resetSpread = resetSpread;
	}

	public double getResetNotnl() {
		return resetNotnl;
	}

	public void setResetNotnl(double resetNotnl) {
		this.resetNotnl = resetNotnl;
	}

	public int getResetDate() {
		return resetDate;
	}

	public void setResetDate(int resetDate) {
		this.resetDate = resetDate;
	}

	public int getRiStartDate() {
		return riStartDate;
	}

	public void setRiStartDate(int riStartDate) {
		this.riStartDate = riStartDate;
	}

	public int getRiEndDate() {
		return riEndDate;
	}

	public void setRiEndDate(int riEndDate) {
		this.riEndDate = riEndDate;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public int getValStatus() {
		return valStatus;
	}

	public void setValStatus(int valStatus) {
		this.valStatus = valStatus;
	}

	public double getAccrualDCF() {
		return accrualDCF;
	}

	public void setAccrualDCF(double accrualDCF) {
		this.accrualDCF = accrualDCF;
	}

	public double getCompoundingFactor() {
		return compoundingFactor;
	}

	public void setCompoundingFactor(double compoundingFactor) {
		this.compoundingFactor = compoundingFactor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(accrualDCF);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(compoundingFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + endDate;
		result = prime * result + resetDate;
		temp = Double.doubleToLongBits(resetNotnl);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(resetSpread);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + riEndDate;
		result = prime * result + riStartDate;
		result = prime * result + startDate;
		result = prime * result + valStatus;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		ResetObj other = (ResetObj) obj;
		if (Double.doubleToLongBits(accrualDCF) != Double
				.doubleToLongBits(other.accrualDCF))
			return false;
		if (Double.doubleToLongBits(compoundingFactor) != Double
				.doubleToLongBits(other.compoundingFactor))
			return false;
		if (endDate != other.endDate)
			return false;
		if (resetDate != other.resetDate)
			return false;
		if (Double.doubleToLongBits(resetNotnl) != Double
				.doubleToLongBits(other.resetNotnl))
			return false;
		if (Double.doubleToLongBits(resetSpread) != Double
				.doubleToLongBits(other.resetSpread))
			return false;
		if (riEndDate != other.riEndDate)
			return false;
		if (riStartDate != other.riStartDate)
			return false;
		if (startDate != other.startDate)
			return false;
		/*if (valStatus != other.valStatus)
			return false;*/
		if (valStatus == 1 && other.valStatus == 1 && Double.doubleToLongBits(value) != Double
				.doubleToLongBits(other.value))
			return false;
		return true;
	}

}
