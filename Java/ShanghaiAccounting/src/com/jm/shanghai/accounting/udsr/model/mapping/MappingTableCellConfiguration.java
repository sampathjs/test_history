package com.jm.shanghai.accounting.udsr.model.mapping;

import java.util.Collection;

import com.jm.shanghai.accounting.udsr.model.mapping.predicate.AbstractPredicate;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2018-11-21		V1.0	jwaechter		- Initial Version
 */

/**
 * Contains the predicates for a cell of a row. The predicate can be used
 * to determine whether a cell from the runtime data table passes through 
 * a filter to the mapping table row.
 * 
 * @author jwaechter
 * @version 1.0
 */
public class MappingTableCellConfiguration {
	private final MappingTableColumnConfiguration colConfig;
	private final AbstractPredicate predicate;
	private MappingTableRowConfiguration rowConfig;
	
	public MappingTableCellConfiguration (
			final MappingTableColumnConfiguration colConfig,
			final AbstractPredicate<Object> predicate) {
		this.colConfig = colConfig;
		this.predicate = predicate;
		this.colConfig.addCell(this);
	}

	public MappingTableRowConfiguration getRowConfig() {
		return rowConfig;
	}

	public void setRowConfig(MappingTableRowConfiguration rowConfig) {
		this.rowConfig = rowConfig;
	}

	public MappingTableColumnConfiguration getColConfig() {
		return colConfig;
	}

	public AbstractPredicate getPredicate() {
		return predicate;
	}

	@Override
	public String toString() {
		return "MappingTableCellConfiguration [colConfig=" + colConfig + ", predicate=" + predicate + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((colConfig == null) ? 0 : colConfig.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
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
		MappingTableCellConfiguration other = (MappingTableCellConfiguration) obj;
		if (colConfig == null) {
			if (other.colConfig != null)
				return false;
		} else if (!colConfig.equals(other.colConfig))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		return true;
	}
}
