package com.matthey.openlink.accounting.ops;

import java.io.Serializable;

public class ProfileObj implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int startDate;
	private int endDate;
	private int pymtDate;
	private double notnl;
	private int notnlStatus;
	private int rateDtmnDate;
	private double floatSpread;
	private int accountingDate;
	private int cashflowType;
	private int paymentCalc;
	
	public ProfileObj() {
	}
	
	public ProfileObj(int startDate, int endDate, int pymtDate, double notnl, int notnlStatus, int rateDtmnDate, 
			double floatSpread, int accountingDate, int cashflowType, int paymentCalc) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.pymtDate = pymtDate;
		this.notnl = notnl;
		this.notnlStatus = notnlStatus;
		this.rateDtmnDate = rateDtmnDate;
		this.floatSpread = floatSpread;
		this.accountingDate = accountingDate;
		this.cashflowType = cashflowType;
		this.paymentCalc = paymentCalc;
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

	public int getPymtDate() {
		return pymtDate;
	}

	public void setPymtDate(int pymtDate) {
		this.pymtDate = pymtDate;
	}

	public double getNotnl() {
		return notnl;
	}

	public void setNotnl(double notnl) {
		this.notnl = notnl;
	}

	public int getNotnlStatus() {
		return notnlStatus;
	}

	public void setNotnlStatus(int notnlStatus) {
		this.notnlStatus = notnlStatus;
	}

	public int getRateDtmnDate() {
		return rateDtmnDate;
	}

	public void setRateDtmnDate(int rateDtmnDate) {
		this.rateDtmnDate = rateDtmnDate;
	}

	public double getFloatSpread() {
		return floatSpread;
	}

	public void setFloatSpread(double floatSpread) {
		this.floatSpread = floatSpread;
	}

	public int getAccountingDate() {
		return accountingDate;
	}

	public void setAccountingDate(int accountingDate) {
		this.accountingDate = accountingDate;
	}

	public int getCashflowType() {
		return cashflowType;
	}

	public void setCashflowType(int cashflowType) {
		this.cashflowType = cashflowType;
	}

	public int getPaymentCalc() {
		return paymentCalc;
	}

	public void setPaymentCalc(int paymentCalc) {
		this.paymentCalc = paymentCalc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + accountingDate;
		result = prime * result + cashflowType;
		result = prime * result + endDate;
		long temp;
		temp = Double.doubleToLongBits(floatSpread);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(notnl);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + notnlStatus;
		result = prime * result + paymentCalc;
		result = prime * result + pymtDate;
		result = prime * result + rateDtmnDate;
		result = prime * result + startDate;
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
		ProfileObj other = (ProfileObj) obj;
		if (accountingDate != other.accountingDate)
			return false;
		if (cashflowType != other.cashflowType)
			return false;
		if (endDate != other.endDate)
			return false;
		if (Double.doubleToLongBits(floatSpread) != Double
				.doubleToLongBits(other.floatSpread))
			return false;
		if (Double.doubleToLongBits(notnl) != Double
				.doubleToLongBits(other.notnl))
			return false;
		if (notnlStatus != other.notnlStatus)
			return false;
		if (paymentCalc != other.paymentCalc)
			return false;
		if (pymtDate != other.pymtDate)
			return false;
		if (rateDtmnDate != other.rateDtmnDate)
			return false;
		if (startDate != other.startDate)
			return false;
		return true;
	}
	
}
