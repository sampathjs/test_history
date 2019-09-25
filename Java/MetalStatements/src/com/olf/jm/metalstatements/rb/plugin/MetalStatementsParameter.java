package com.olf.jm.metalstatements.rb.plugin;

import java.util.HashMap;
import java.util.Map;

import com.olf.openrisk.table.Table;

public class MetalStatementsParameter {

	private Map<String, String> parameters = new HashMap<>();

	public MetalStatementsParameter(Table pluginParameters) throws Exception {
		int numRows = pluginParameters.getRowCount();
		
		for (int rowNum = 0; rowNum < numRows; rowNum++) {
			this.parameters.put(pluginParameters.getString(0, rowNum), pluginParameters.getString(1, rowNum));
		}
	}

	public String getStringValue(String paramName) {
		String value = "";
		
		if (this.parameters.size() > 0 && this.parameters.containsKey(paramName)) {
			value = this.parameters.get(paramName);
		}
		return value;
	}
}
