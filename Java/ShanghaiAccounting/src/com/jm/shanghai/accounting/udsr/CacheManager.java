package com.jm.shanghai.accounting.udsr;

import java.util.HashMap;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableColumnConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2019-12-18	V1.0	jwaechter 	- Initial Version
 */

/**
 * 
 * @author jwaechter
 * @version 1.0
 */
public class CacheManager {
	// contains the content of the mapping tables
	private final Map<String, Table> mappingTableCache = new HashMap<>();
	
	// cache for the mapping table column metadata
	private final Map<String, Map<String, MappingTableColumnConfiguration>> mappingTableDescriptions = new HashMap<>();
	
	private RetrievalConfigurationColDescriptionLoader colLoader;
	
	public void init (Session session) {
		colLoader = new RetrievalConfigurationColDescriptionLoader(session);
	}
	
	public Table retrieveMappingTable(final Session session, String userTableName) {
		Table mappingTable = mappingTableCache.get(userTableName);
		if (mappingTable != null) {
			return mappingTable;
		}
		UserTable mappingUserTable = session.getIOFactory().getUserTable(userTableName, false);
		if (mappingUserTable == null) {
			String errorMessage = "The mandatory table " + userTableName + " can't be retrieved. "
					+ ". Please check database access rights, confirm the existence of the table in the database ";
			throw new RuntimeException (errorMessage);
		}
		mappingTable = mappingUserTable.retrieveTable();
		mappingTableCache.put(userTableName, mappingTable);
		return mappingTable;
	}
	
	public Map<String, MappingTableColumnConfiguration> getMappingTableDescription (final String mappingTableName) {
		return mappingTableDescriptions.get(mappingTableName);
	}
	
	public void clearCache() {
		mappingTableCache.clear();
		mappingTableDescriptions.clear();
	}

	public Map<String, Table> getMappingTableCache() {
		return mappingTableCache;
	}

	public Map<String, Map<String, MappingTableColumnConfiguration>> getMappingTableDescriptions() {
		return mappingTableDescriptions;
	}

	public RetrievalConfigurationColDescriptionLoader getColLoader() {
		return colLoader;
	}
}
