package com.jm.shanghai.accounting.udsr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.retrieval.JavaTable;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;

/*
 * History:
 * 2019-07-DD		V1.0	jwaechter	- Initial Version
 */

/**
 * Instances of this class containing a log / history of the runtime table before and after the
 * mapping of the mapping tables take place. This allows the Operators GUI to display the 
 * mapping steps to the user.
 * The different steps of the mapping are tracked in {@link #mappingAuditingData}. 
 * In addition to the pure mapping data, the initial content and table structure that
 * had been generated while merging the event tables for the different transactions
 * is stored in {@link #runtimeTable}.
 * @author jwaechter
 * @version 1.0
 */
public class RuntimeAuditingData {	
	private final Map<RetrievalConfigurationColDescription, MappingAuditingData> mappingAuditingData;
	private List<RetrievalConfiguration> retrievalConfig;
	private JavaTable runtimeTable;

	public RuntimeAuditingData () {
		this.mappingAuditingData = new HashMap<>();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((mappingAuditingData == null) ? 0 : mappingAuditingData
						.hashCode());
		return result;
	}

	public Map<RetrievalConfigurationColDescription, MappingAuditingData> getMappingAuditingData() {
		return mappingAuditingData;
	}

	public List<RetrievalConfiguration> getRetrievalConfig() {
		return retrievalConfig;
	}

	public void setRetrievalConfig(List<RetrievalConfiguration> retrievalConfig) {
		this.retrievalConfig = retrievalConfig;
	}

	public JavaTable getRuntimeTable() {
		return runtimeTable;
	}

	public void setRuntimeTable(JavaTable runtimeTable) {
		this.runtimeTable = runtimeTable;
	}
}
