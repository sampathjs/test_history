package com.jm.accountingfeed.rb.datasource.salesledger;

/**
 * POJO for missing tran event info.
 * 
 */
public class MissingEventInfo {
	private long eventNum;
	private int valueDate;
	private String taxCode;

	public long getEventNumber() {
		return eventNum;
	}

	public void setEventNumber(long eventNum) {
		this.eventNum = eventNum;
	}

	public int getValueDate() {
		return valueDate;
	}

	public void setValueDate(int valueDate) {
		this.valueDate = valueDate;
	}

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	@Override
	public String toString() {
		return String.format("[Event Mumber: %d, Value Date:%d, Tax Code: %s]", eventNum, valueDate, taxCode);
	}
}