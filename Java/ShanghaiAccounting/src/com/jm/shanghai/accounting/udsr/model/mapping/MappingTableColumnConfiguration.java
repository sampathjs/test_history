package com.jm.shanghai.accounting.udsr.model.mapping;

import java.util.ArrayList;
import java.util.List;

import com.jm.shanghai.accounting.udsr.model.mapping.predicate.KleeneStar;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.StringAlternatives;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2018-11-21	V1.0		jwaechter		- Initial Version
 */

/**
 * Contains the structural description of the mapping table.
 * @author jwaechter
 * @version 1.0
 */
public class MappingTableColumnConfiguration {
	private final MappingConfigurationColType mappingColType;
	private final String colName;
	private final EnumColType colType;
	private final List<MappingTableCellConfiguration> cells;

	/**
	 * Contains the information if this column can be indexed using {@link ColumnIndex}.
	 */
	private boolean indexable;
	
	public MappingTableColumnConfiguration (final MappingConfigurationColType mappingColType,
			final String colName, EnumColType colType) {
		this.mappingColType = mappingColType;
		this.colName = colName;
		this.colType = colType;
		this.indexable = true;
		this.cells = new ArrayList<>(1000);
	}

	public MappingConfigurationColType getMappingColType() {
		return mappingColType;
	}

	public String getColName() {
		return colName;
	}

	public EnumColType getColType() {
		return colType;
	}

	public boolean isIndexable() {
		return indexable;
	}

	public void setIndexable(boolean indexable) {
		this.indexable = indexable;
	}
	
	public void addCell (MappingTableCellConfiguration cell) {
		cells.add(cell);
		if (   cell.getPredicate() instanceof KleeneStar
			|| cell.getPredicate() instanceof StringAlternatives) {
			indexable &= true;
		} else {
			indexable = false;
		}
	}

	public List<MappingTableCellConfiguration> getCells() {
		return cells;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colName == null) ? 0 : colName.hashCode());
		result = prime * result + ((colType == null) ? 0 : colType.hashCode());
		result = prime * result
				+ ((mappingColType == null) ? 0 : mappingColType.hashCode());
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
		MappingTableColumnConfiguration other = (MappingTableColumnConfiguration) obj;
		if (colName == null) {
			if (other.colName != null)
				return false;
		} else if (!colName.equals(other.colName))
			return false;
		if (colType != other.colType)
			return false;
		if (mappingColType != other.mappingColType)
			return false;
		return true;
	}

	
	@Override
	public String toString() {
		return "MappingTableColumnConfiguration [mappingColType="
				+ mappingColType + ", colName=" + colName + ", colType="
				+ colType + "]";
	}
}
