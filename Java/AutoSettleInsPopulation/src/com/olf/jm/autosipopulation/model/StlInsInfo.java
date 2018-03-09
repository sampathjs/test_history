package com.olf.jm.autosipopulation.model;

import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial version
 * 2016-01-28	V1.1	jwaechter	- Added
 */

/**
 * Enum containing relevant settlement instruction info fields for the 
 * settlement instructions. 
 * @author jwaechter
 * @version 1.0
 */
public enum StlInsInfo {
	ForPassthrough("For Passthrough");
	
	private final String name;
	
	private String defaultValue=null;
	
	private StlInsInfo (String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public String getDefault (IOFactory factory) {
		if (defaultValue == null) {
			Table sqlResult=null;
			try {
				String sql = "SELECT ISNULL(default_value,'') FROM settle_instruction_info_type WHERE type_name = '" + name + "'";
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
