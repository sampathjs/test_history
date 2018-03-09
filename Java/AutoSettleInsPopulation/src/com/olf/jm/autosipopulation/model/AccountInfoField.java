package com.olf.jm.autosipopulation.model;

import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2015-06-16	V1.0	jwaechter	- initial version 
 * 2016-01-28	V1.1	jwaechter	- Added "AUTO_SI_SHORTLIST"
 * 2016-05-10	V1.2	jwaechter	- Added "VAT_AND_CASH"
 */

/**
 * Enum containing relevant account info fields (on parameter level) for the 
 * settlement assignment instructions. 
 * @author jwaechter
 * @version 1.2
 */
public enum AccountInfoField {
	Loco("Loco"), Form ("Form"), AllocationType ("Allocation Type"), AUTO_SI_SHORTLIST ("Auto SI Shortlist"),
	VAT_AND_CASH ("VAT and Cash"), ;
	
	private final String name;
	
	private String defaultValue=null;
	
	private AccountInfoField (String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public String getDefault (IOFactory factory) {
		if (defaultValue == null) {
			Table sqlResult=null;
			try {
				String sql = "SELECT ISNULL(default_value,'') FROM account_info_type WHERE type_name = '" + name + "'";
				sqlResult = factory.runSQL(sql);
				defaultValue = sqlResult.getString(0, 0);
			} finally {
				if (sqlResult != null) {
					sqlResult.dispose();
				}
			}
		}
		return defaultValue;
	}
}
