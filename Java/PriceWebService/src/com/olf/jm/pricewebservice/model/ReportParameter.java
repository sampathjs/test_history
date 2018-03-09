package com.olf.jm.pricewebservice.model;

/*
 * History:
 * 2015-04-22	V1.0	jwaechter - initial version
 */

/**
 * Contains names of report parameters used in the price web report builder report send out via email.
 * @author jwaechter
 * @version 1.0
 *
 */
public enum ReportParameter {
	DATASET_TYPE ("DatasetType"),
	OUTPUT_FILENAME ("OUTPUT_FILENAME"),
	START_DATE ("StartDate"),
	END_DATE ("EndDate"),
	INDEX_NAME ("IndexName")
	;
	
	private final String parameterName;
	
	private ReportParameter (String parameterName) {
		this.parameterName = parameterName;
	}

	public String getName() {
		return parameterName;
	}
}
