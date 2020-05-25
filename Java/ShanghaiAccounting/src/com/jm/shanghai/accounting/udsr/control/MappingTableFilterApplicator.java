package com.jm.shanghai.accounting.udsr.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jm.shanghai.accounting.udsr.model.mapping.ColumnIndex;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableColumnConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableIndex;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableRowConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/*
 * History:
 * 2018-11-24		V1.0	jwaechter		- Initial Version
 */

/**
 * Contains the application of the filter logic to select the right account.
 * The class takes the metadata (e.g. column descriptions )and the derived data
 * (e.g. the indexes created above filter columns) for a complete mapping table
 * and provides {@link #apply(Table, TableRow)} to find matching mapping table
 * columns for a provided row of the runtime table.
 * @author jwaechter
 * @version 1.0
 */
public class MappingTableFilterApplicator {
	private MappingTableIndex index;
	
	public MappingTableFilterApplicator (final Map<String, RetrievalConfiguration> retrievalConfigByMappingColName, 
			final Map<String, MappingTableColumnConfiguration> mappingTableColConfig,
			final RetrievalConfigurationColDescriptionLoader colLoader) {
		List<ColumnIndex> indexes = new ArrayList<>();
		for (MappingTableColumnConfiguration colConfig : mappingTableColConfig.values()) {
			ColumnIndex ci = new ColumnIndex(colConfig);
			indexes.add(ci);
		}
		this.index = new MappingTableIndex(indexes, retrievalConfigByMappingColName, colLoader);
	}
	
	/**
	 * Returns the matching mapping table row configuration or null if no such
	 * row configuration exists. In case if several rows match, the matching rows
	 * those weight is minimal is returned.
	 * @param runtimeTableRow 
	 * @param runtimeTable 
	 * @return
	 */
	public List<MappingTableRowConfiguration> apply(Table runtimeTable, TableRow runtimeTableRow) {
		Map<MappingTableRowConfiguration, Integer> allMatchingRows = new HashMap<>();
		int minWeight = Integer.MAX_VALUE;
		int rowNum = runtimeTableRow.getInt("row_id");
		Set<MappingTableRowConfiguration> candidatesForMatch = 
				index.filter(runtimeTable, runtimeTableRow.getNumber());
		for (MappingTableRowConfiguration mtrc : candidatesForMatch) {
			int weight = mtrc.match(runtimeTable, runtimeTableRow.getNumber());
			if (weight > 0) {
				if (weight < minWeight) {
					minWeight = weight;
				}
				allMatchingRows.put(mtrc, weight);
			}
		}
		List<MappingTableRowConfiguration> minWeightMatches = new ArrayList<>();
		for (Entry<MappingTableRowConfiguration, Integer> e : allMatchingRows.entrySet()) {
			if (((int)e.getValue()) == minWeight) {
				minWeightMatches.add(e.getKey());				
			}
		}
		return minWeightMatches;
	}
}
