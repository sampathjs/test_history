package com.jm.shanghai.accounting.udsr;

import java.util.HashMap;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableColumnConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2019-12-18	V1.0	jwaechter 	- Initial Version
 */

/**
 * The CacheManager is being used to store data that is either being retrieved or the result of an process but used in more than once.
 * The purpose is to reduce the number of calls to the database and the computation time.
 * 
 * 
 * @author jwaechter
 * @version 1.0
 */
public class CacheManager {
	/**
	*  Contains the content of the mapping tables. The mapping is from table as in the databse to the table content.
	*  Null values indicate a table has not yet been retrieved from the database. 
	*/
	private final Map<String, Table> mappingTableCache = new HashMap<>();
	
	/**
	 *  Cache for the mapping table column metadata. Maps from the name of the user table to the mapping table configurations for each column
	 *  (another map from column name to the details about that column). 
	 */
	private final Map<String, Map<String, MappingTableColumnConfiguration>> mappingTableDescriptions = new HashMap<>();
	
	/**
	 * The cache containing meta data about the column in the "USER_jm_acc_retrieval_config" table.
	 */
	private RetrievalConfigurationColDescriptionLoader colLoader;
	
	public void init (Session session) {
		colLoader = new RetrievalConfigurationColDescriptionLoader(session);
	}
	
	/**
	 * Retrieves the content of the table having name userTableName from either the cache
	 * or the DB in case the content has not yet been retrieved from the DB before. 
	 * @param session
	 * @param userTableName
	 * @return
	 */
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
	
	/**
	 * Clears the cache in those areas where to expect changes. 
	 */
	public void clearCache() {
		mappingTableCache.clear();
		mappingTableDescriptions.clear();
	}

	/**
	 * Retrieves the cache for the {@link RetrievalConfigurationColDescription}.
	 * @return
	 */
	public RetrievalConfigurationColDescriptionLoader getColLoader() {
		return colLoader;
	}
}
