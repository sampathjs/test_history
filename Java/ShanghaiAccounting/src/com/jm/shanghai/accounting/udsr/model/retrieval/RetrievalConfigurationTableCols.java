package com.jm.shanghai.accounting.udsr.model.retrieval;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.fixed.UserTableColumn;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2018-11-20	V1.0		jwaechter		- Initial Version
 * 2019-07-14	V1.1		jwaechter		- Added column for customer and company mapping table
 * 2018-07-24	V1.2		jwaechter		- Refactored enum to be a class.
 */

/**
 * Class containing the known columns of the user table {@link ConfigurationItem#RETRIEVAL_CONFIG_TABLE_NAME}
 * @author jwaechter
 * @version 1.2
 */
public class RetrievalConfigurationTableCols implements UserTableColumn, RetrievalConfigurationColDescription {	
	private final String colName;
	private final String colTitle;
	private final EnumColType colType;
	private final String colTypeName;
	private final ColumnSemantics usageType;
	private final int mappingTableEvaluationOrder;

	/**
	 * In case of {@link ColumnSemantics#MAPPER_COLUMN} this attribute contains the name
	 * of the user table storing the mapping data.
	 * Empty otherwise.
	 */
	private final String mappingTableName;
	
	public RetrievalConfigurationTableCols (String name, String title, EnumColType type, 
			String colTypeName, ColumnSemantics usageType, int mappingTableEvaluationOrder,
			String mappingTableName)	{
		colName = name;
		colTitle = title;
		colType = type;
		this.colTypeName = colTypeName;
		this.usageType = usageType;
		this.mappingTableEvaluationOrder = mappingTableEvaluationOrder;
		this.mappingTableName = mappingTableName;
	}
	
	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getColName()
	 */
	@Override
	public String getColName() {
		return colName;
	}
	
	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getColType()
	 */
	@Override
	public EnumColType getColType() {
		return colType;
	}
	
	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getColTitle()
	 */
	@Override
	public String getColTitle() {
		return colTitle;
	}

	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getColTypeName()
	 */
	@Override
	public String getColTypeName() {
		return colTypeName;
	}

	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getUsageType()
	 */
	@Override
	public ColumnSemantics getUsageType() {
		return usageType;
	}

	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getMappingTableEvaluationOrder()
	 */
	@Override
	public int getMappingTableEvaluationOrder() {
		return mappingTableEvaluationOrder;
	}

	/* (non-Javadoc)
	 * @see com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription#getMappingTableName()
	 */
	@Override
	public String getMappingTableName() {
		return mappingTableName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colName == null) ? 0 : colName.hashCode());
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
		RetrievalConfigurationTableCols other = (RetrievalConfigurationTableCols) obj;
		if (colName == null) {
			if (other.colName != null)
				return false;
		} else if (!colName.equals(other.colName))
			return false;
		return true;
	}	
}
