/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * Description: 
 * 		Data type representing EOD regions
 * 
 * Project : Metals balance sheet
 * Customer : Johnson Matthey Plc. 
 * last modified date : 4/November/2015
 * 
 * @author:  Douglas Connolly /OpenLink International Ltd.
 * @modified by :
 * @version   1.0 // Initial Release
 * @version   1.1 // Added China as a region 
 */

package com.jm.eod.common;

public enum RegionEnum {
	
	NONE("Unknown Region"),
	HONGKONG("HK"),
	UK("UK"),
	USA("US"),
	CHINA("CN");
	
	private final String description;
	
	RegionEnum(String desc){
		this.description = desc;
	}
	
	public String description() {
		return description;
	}
}
