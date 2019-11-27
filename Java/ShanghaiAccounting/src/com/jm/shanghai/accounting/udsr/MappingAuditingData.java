package com.jm.shanghai.accounting.udsr;

import java.util.List;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.olf.openrisk.table.Table;

public class MappingAuditingData {
	private Table runtimeTableBeforeMapping;
	private Table runtimeTableAfterMapping;
	private ColNameProvider colNameProvider;
	
	public Table getRuntimeTableBeforeMapping() {
		return runtimeTableBeforeMapping;
	}
	public void setRuntimeTableBeforeMapping(Table runtimeTableBeforeMapping) {
		this.runtimeTableBeforeMapping = runtimeTableBeforeMapping;
	}
	public Table getRuntimeTableAfterMapping() {
		return runtimeTableAfterMapping;
	}
	public void setRuntimeTableAfterMapping(Table runtimeTableAfterMapping) {
		this.runtimeTableAfterMapping = runtimeTableAfterMapping;
	}

	public ColNameProvider getColNameProvider() {
		return colNameProvider;
	}
	public void setColNameProvider(ColNameProvider colNameProvider) {
		this.colNameProvider = colNameProvider;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((runtimeTableAfterMapping == null) ? 0
						: runtimeTableAfterMapping.hashCode());
		result = prime
				* result
				+ ((runtimeTableBeforeMapping == null) ? 0
						: runtimeTableBeforeMapping.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MappingAuditingData other = (MappingAuditingData) obj;
		if (runtimeTableAfterMapping == null) {
			if (other.runtimeTableAfterMapping != null)
				return false;
		} else if (!runtimeTableAfterMapping
				.equals(other.runtimeTableAfterMapping))
			return false;
		if (runtimeTableBeforeMapping == null) {
			if (other.runtimeTableBeforeMapping != null)
				return false;
		} else if (!runtimeTableBeforeMapping
				.equals(other.runtimeTableBeforeMapping))
			return false;
		return true;
	}
}
