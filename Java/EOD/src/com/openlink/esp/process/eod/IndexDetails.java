package com.openlink.esp.process.eod;

import com.olf.openjvs.ODateTime;

/**
 * This is a POJO class for Indexes.
 * 
 * @author YadavP03
 * @version 1.0
 */
public class IndexDetails {
	private final String indexId;
	private final String refSource;
	private final Double price;
	private final ODateTime startDate;
	private final ODateTime resetDate;
	public IndexDetails(String indexId,String refSource, Double price, ODateTime startDate, ODateTime resetDate){
		this.indexId = indexId;
		this.refSource = refSource;
		this.price = price;
		this.startDate = startDate;
		this.resetDate = resetDate;

	}
	public String getIndexId() {
		return indexId;
	}
	public ODateTime getStartDate() {
		return startDate;
	}
	public ODateTime getResetDate() {
		return resetDate;
	}
	public String getRefSource() {
		return refSource;
	}
	public Double getPrice() {
		return price;
	}


}
