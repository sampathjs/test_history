package com.jm.shanghai.accounting.udsr.model.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jm.shanghai.accounting.udsr.model.mapping.predicate.KleeneStar;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.StringAlternatives;

/*
 * History:
 * 2018-12-28		V1.0	jwaechter		 - Initial Version
 */

/**
 * Class containing an index over all rows for a certain column of the mapping table.
 * A column index can be applied if and only if all rows of a certain column contain 
 * predicates of type {@link StringAlternatives} only.
 * @author jwaechter
 * @version 1.0
 */
public class ColumnIndex {
	/**
	 * Assigns each string alternative the rows that contain it. 
	 */
	private final Map<String, Set<MappingTableRowConfiguration>> alternativesToMappingRows;
	private final Set<MappingTableRowConfiguration> kleeneStarRows;
	private final MappingTableColumnConfiguration column;
	
	public ColumnIndex (final MappingTableColumnConfiguration column) {
		this.column = column;
		alternativesToMappingRows = new HashMap<String, Set<MappingTableRowConfiguration>>(column.getCells().size()*5);
		kleeneStarRows = new HashSet<>();
		for (MappingTableCellConfiguration cell : column.getCells()) {
			if (cell.getPredicate() instanceof StringAlternatives) {
				StringAlternatives stringAlternatives = (StringAlternatives)cell.getPredicate();
				for (String alternative : stringAlternatives.getListOfAlternatives()) {
					Set<MappingTableRowConfiguration> rows = alternativesToMappingRows.get(alternative);
					if (rows == null) {
						rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
						alternativesToMappingRows.put(alternative, rows);
					} 
					rows.add(cell.getRowConfig());
				}
			} else {
				if (cell.getPredicate() instanceof KleeneStar) {
					kleeneStarRows.add(cell.getRowConfig());
				} else {
					throw new RuntimeException ("The column '" + column.getColName() + "' contains"
							+ " the predicate '" + cell.getPredicate() + "' that is not of type "
							+ " '" + StringAlternatives.class.getName() + "' and therefore not indexable");
				}
			}
		}
	}

	public Map<String, Set<MappingTableRowConfiguration>> getAlternativesToMappingRows() {
		return alternativesToMappingRows;
	}
	
	public Set<MappingTableRowConfiguration> getMappingTableRows(String alternative) {
		return alternativesToMappingRows.get(alternative);
	}

	public Set<MappingTableRowConfiguration> getKleeneStarRows() {
		return kleeneStarRows;
	}

	public MappingTableColumnConfiguration getColumn() {
		return column;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((alternativesToMappingRows == null) ? 0
						: alternativesToMappingRows.hashCode());
		result = prime * result + ((column == null) ? 0 : column.hashCode());
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
		ColumnIndex other = (ColumnIndex) obj;
		if (alternativesToMappingRows == null) {
			if (other.alternativesToMappingRows != null)
				return false;
		} else if (!alternativesToMappingRows
				.equals(other.alternativesToMappingRows))
			return false;
		if (column == null) {
			if (other.column != null)
				return false;
		} else if (!column.equals(other.column))
			return false;
		return true;
	}
}
