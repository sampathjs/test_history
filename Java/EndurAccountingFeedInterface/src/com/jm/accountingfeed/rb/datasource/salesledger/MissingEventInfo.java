package com.jm.accountingfeed.rb.datasource.salesledger;

import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;

/**
 * POJO for missing tran event info.
 * 
 */
public class MissingEventInfo {
	private final long eventNum;
	private final int valueDate;
	private final String taxCode;
	
	private MissingEventInfo(MissingEventInfoBuilder builder){
		this.eventNum = builder.eventNum;
		this.valueDate = builder.valueDate;
		this.taxCode = builder.taxCode;
	}

	public long getEventNumber() {
		return eventNum;
	}

	public int getValueDate() {
		return valueDate;
	}

	public String getTaxCode() {
		return taxCode;
	}

	@Override
	public String toString() {
		return String.format("[Event Number: %d, Value Date:%d, Tax Code: %s]", eventNum, valueDate, taxCode);
	}
	
	public static class MissingEventInfoBuilder {
		private long eventNum;
		private int valueDate = -1;
		private String taxCode = "";
		
		public MissingEventInfoBuilder(long eventNum) {
			super();
			if(eventNum <= 0) {
                throw new AccountingFeedRuntimeException("Invalid Event Number: " + eventNum );
            }
			this.eventNum = eventNum;
		}
		
		public MissingEventInfoBuilder ValueDate(int valueDate) {
            this.valueDate = valueDate;
            return this;
        }
		
		public MissingEventInfoBuilder TaxCode(String taxCode) {
            this.taxCode = taxCode;
            return this;
        }
		
		public MissingEventInfo build() {
            return new MissingEventInfo(this);
        } 
	}
}