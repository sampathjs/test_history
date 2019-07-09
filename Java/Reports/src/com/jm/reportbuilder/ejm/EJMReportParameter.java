package com.jm.reportbuilder.ejm;

import java.util.Map;
import java.util.TreeMap;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public class EJMReportParameter {
	
	private Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	public EJMReportParameter(Table pluginParameters) throws OException {
		int numRows = pluginParameters.getNumRows();
		for(int rowNum=1; rowNum <= numRows; rowNum++ ) {
			parameters.put(pluginParameters.getString(1, rowNum), pluginParameters.getString(2, rowNum));
		}
	}

	public String getStringValue(String paramName) {
		String ret = "";
		if(parameters.size() > 0 && parameters.containsKey(paramName)) {
			ret = parameters.get(paramName);
		}
		return ret;
	}
}
