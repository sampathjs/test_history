package com.jm.shanghai.accounting.udsr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;

public class RuntimeAuditingData {	
	private final Map<RetrievalConfigurationColDescription, MappingAuditingData> mappingAuditingData;
	private List<RetrievalConfiguration> retrievalConfig;

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
}
