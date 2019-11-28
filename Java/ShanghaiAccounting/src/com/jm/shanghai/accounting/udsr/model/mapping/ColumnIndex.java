package com.jm.shanghai.accounting.udsr.model.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jm.shanghai.accounting.udsr.model.mapping.predicate.DoubleGreaterThan;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.DoubleGreaterThanOrEquals;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.DoubleLessThan;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.DoubleLessThanOrEquals;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.IntegerGreaterThan;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.IntegerGreaterThanOrEquals;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.IntegerLessThan;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.IntegerLessThanOrEquals;
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
	private final Map<Double, Set<MappingTableRowConfiguration>> highestNumberIncluded;
	private final Map<Double, Set<MappingTableRowConfiguration>> lowestNumberIncluded;
	private final Map<Double, Set<MappingTableRowConfiguration>> highestNumberNotIncluded;
	private final Map<Double, Set<MappingTableRowConfiguration>> lowestNumberNotIncluded;
	private final Set<MappingTableRowConfiguration> kleeneStarRows;
	private final MappingTableColumnConfiguration column;
	
	public ColumnIndex (final MappingTableColumnConfiguration column) {
		this.column = column;
		alternativesToMappingRows = new HashMap<String, Set<MappingTableRowConfiguration>>(column.getCells().size()*5);
		highestNumberIncluded = new HashMap<Double, Set<MappingTableRowConfiguration>>(column.getCells().size()*5);
		lowestNumberIncluded = new HashMap<Double, Set<MappingTableRowConfiguration>>(column.getCells().size()*5);
		highestNumberNotIncluded = new HashMap<Double, Set<MappingTableRowConfiguration>>(column.getCells().size()*5);
		lowestNumberNotIncluded = new HashMap<Double, Set<MappingTableRowConfiguration>>(column.getCells().size()*5);
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
				} else if (cell.getPredicate().isComparable())  {
					if (cell.getPredicate() instanceof DoubleLessThan) {
						Double threshold = ((DoubleLessThan)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = highestNumberNotIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							highestNumberNotIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} else if (cell.getPredicate() instanceof DoubleLessThanOrEquals) {
						Double threshold = ((DoubleLessThanOrEquals)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = highestNumberIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							highestNumberIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} else if (cell.getPredicate() instanceof DoubleGreaterThanOrEquals) {
						Double threshold = ((DoubleGreaterThanOrEquals)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = lowestNumberIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							lowestNumberIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} else if (cell.getPredicate() instanceof DoubleGreaterThan) {
						Double threshold = ((DoubleGreaterThan)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = lowestNumberNotIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							lowestNumberNotIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} if (cell.getPredicate() instanceof IntegerLessThan) {
						Double threshold = ((IntegerLessThan)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = highestNumberNotIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							highestNumberNotIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} else if (cell.getPredicate() instanceof IntegerLessThanOrEquals) {
						Double threshold = ((IntegerLessThanOrEquals)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = highestNumberIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							highestNumberIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} else if (cell.getPredicate() instanceof IntegerGreaterThanOrEquals) {
						Double threshold = ((IntegerGreaterThanOrEquals)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = lowestNumberIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							lowestNumberIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					} else if (cell.getPredicate() instanceof IntegerGreaterThan) {
						Double threshold = ((IntegerGreaterThan)cell.getPredicate()).getThreshold();
						Set<MappingTableRowConfiguration> rows = lowestNumberNotIncluded.get(threshold);
						if (rows == null) {
							rows = new HashSet<MappingTableRowConfiguration>(column.getCells().size());
							lowestNumberNotIncluded.put(threshold, rows);
						}
						rows.add(cell.getRowConfig());
					}
				} else {
					throw new RuntimeException ("The column '" + column.getColName() + "' contains"
							+ " the predicate '" + cell.getPredicate() + "' that is not indexable");
				}
			}
		}
	}

	public Map<String, Set<MappingTableRowConfiguration>> getAlternativesToMappingRows() {
		return alternativesToMappingRows;
	}
	
	public Set<MappingTableRowConfiguration> getAlternativesMappingTableRows(String value) {
		Set<MappingTableRowConfiguration> matchtingRows = new HashSet<>();
		if (alternativesToMappingRows.get(value) != null) {
			matchtingRows.addAll(alternativesToMappingRows.get(value));
		}
		Double threshold = null;
		try {
			threshold = Double.parseDouble(value);
		} catch (NumberFormatException ex) {
			// do nothing
		}
		if (threshold != null) {
			for (Double highestNumber : highestNumberIncluded.keySet()) {
				if (threshold <= highestNumber ) {
					matchtingRows.addAll(highestNumberIncluded.get(highestNumber));
				}
			}
			for (Double highestNumber : highestNumberNotIncluded.keySet()) {
				if (threshold < highestNumber ) {
					matchtingRows.addAll(highestNumberNotIncluded.get(highestNumber));
				}
			}
			for (Double lowestNumber : lowestNumberNotIncluded.keySet()) {
				if (threshold > lowestNumber ) {
					matchtingRows.addAll(lowestNumberNotIncluded.get(lowestNumber));
				}
			}
			for (Double lowestNumber : lowestNumberIncluded.keySet()) {
				if (threshold >= lowestNumber ) {
					matchtingRows.addAll(lowestNumberIncluded.get(lowestNumber));
				}
			}
		}
		return matchtingRows;
		
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
