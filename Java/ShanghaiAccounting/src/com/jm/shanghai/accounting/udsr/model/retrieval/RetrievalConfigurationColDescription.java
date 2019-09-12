package com.jm.shanghai.accounting.udsr.model.retrieval;

import com.olf.openrisk.table.EnumColType;

public interface RetrievalConfigurationColDescription {

	public abstract String getColName();

	public abstract EnumColType getColType();

	public abstract String getColTitle();

	public abstract String getColTypeName();

	public abstract ColumnSemantics getUsageType();

	public abstract int getMappingTableEvaluationOrder();

	public abstract String getMappingTableName();

}