package com.jm.shanghai.accounting.udsr.control;

import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

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
	private final RetrievalConfigurationColDescriptionLoader colLoader;
	
	public OutputTableRetrievalApplicator (RetrievalConfiguration rc, 
			RetrievalConfigurationColDescriptionLoader colLoader) {
		this.rc = rc;
		this.colLoader = colLoader;
	}
		
	public void apply(Table resultTable, Table runtimeTable, StringBuilder columnNames, Map<String, String> outputColNames) {
		String colNameReportOutput = rc.getColumnValue(colLoader.getReportOutput());
		if (colNameReportOutput == null || colNameReportOutput.trim().isEmpty()) {
			Logging.debug("Retrieval table configuration row '" + rc.toString() + "' does not add to  "
					+ " UDSR output table. Skipping it.");
			return;
		}
		if (outputColNames.values().contains(colNameReportOutput)) {
			String errorMessage = "Can't apply output table retrieval on retrieval configuration "
					+ rc.toString() + "\n because the column '" + colNameReportOutput + "'"
					+ " does already exist in the output table";
			Logging.error(errorMessage);
			throw new RuntimeException (errorMessage);
		}
		if (columnNames.length() > 0) {
			columnNames.append(",");
		}
		outputColNames.put(rc.getColumnValue(colLoader.getRuntimeDataTable()),
				colNameReportOutput);
		Logging.debug("Successfully added column " + rc.getColumnValue(colLoader.getRuntimeDataTable()) + " \nto the output table");
	}
}
