/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * Description: 
 * 		Data type respresenting type of data request
 * 
 * Project : Metals balance sheet
 * Customer : Johnson Matthey Plc. 
 * last modified date : 29/October/2015
 * 
 * @author:  Douglas Connolly /OpenLink International Ltd.
 * @modified by :
 * @version   1.0 // Initial Release
 */

package com.jm.rbreports.BalanceSheet;

public enum ReportCallTypeEnum {
	
	NONE("Unknown request"),
	COLLECT_METADATA("Collect metedata request"),
	DRILL_DOWN_REPORT("Drill Down request"),
	STANDARD_REPORT("Standard report request");
	
	private final String description;
	
	ReportCallTypeEnum(String desc){
		this.description = desc;
	}
	
	public String description() {
		return description;
	}
}
