package com.jm.shanghai.accounting.udsr.control;

import java.util.List;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2018-11-19	V1.0 	jwaechter	- Initial version
 */


/**
 * Class containing the logic to apply the retrieval logic for the output table of the UDSR. 
 * @author jwaechter
 * @version 1.0
 */
public class OutputTableRetrievalApplicator {
	private RetrievalConfiguration rc;
		
	public OutputTableRetrievalApplicator (RetrievalConfiguration rc) {
		this.rc = rc;
	}
		
	public void apply(Table resultTable, Table runtimeTable, StringBuilder columnNames, Map<String, String> outputColNames) {
		if (rc.getColNameReportOutput() == null || rc.getColNameReportOutput().trim().isEmpty()) {
			PluginLog.debug("Retrieval table configuration row '" + rc.toString() + "' does not add to  "
					+ " UDSR output table. Skipping it.");
			return;
		}
		if (outputColNames.values().contains(rc.getColNameReportOutput())) {
			String errorMessage = "Can't apply output table retrieval on retrieval configuration "
					+ rc.toString() + "\n because the column '" + rc.getColNameReportOutput() + "'"
					+ " does already exist in the output table";
			PluginLog.error(errorMessage);
			throw new RuntimeException (errorMessage);
		}
		if (columnNames.length() > 0) {
			columnNames.append(",");
		}
//		columnNames.append(rc.getColNameRuntimeTable()).append("->").append(rc.getColNameReportOutput());
		outputColNames.put(rc.getColNameRuntimeTable(), rc.getColNameReportOutput());
		PluginLog.debug("Successfully added column " + rc.toString() + " \nto the output table");
	}
}
