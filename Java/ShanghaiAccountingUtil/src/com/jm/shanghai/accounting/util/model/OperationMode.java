package com.jm.shanghai.accounting.util.model;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;

/*
 * History: 
 * 2018-11-20 - V1.0 - initial version
 */

/**
 * Contains the different operating modes of the {@link ShanghaiAccountingUtil} task.
 * 
 * @author jwaechter
 * @version 1.0
 */
public enum OperationMode {
	CLEAR_MAPPING_CONFIG_TABLE ("Clear Mapping Config Table", 
			"Clears the " + ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue(), false), 
	EXPORT_MAPPING_CONFIG_TABLE ("Export Mapping Config Table", 
			"Exports the " + ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue() + " to a CSV file", true), 
	IMPORT_MAPPING_CONFIG_TABLE ("Import Mapping Config Table", 
			"Imports the " + ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue() + " from a CSV file", true), 		
	LOCAL_REMOVE_CONFIG_REPOSITORY_DATA ("Remove Constants Repository Data",  
			"Removes all ShanghaiAccounting related ConstantsRepository entries", false),
	LOCAL_ADD_DEFAULT_CONFIG_REPOSITORY_DATA ("Add Default Constants Repository Data",  
			"Adds all default values to the ConstantsRepository", false),
	;
	
	private final String menuText;
	private final String description;
	private final boolean usingFilename;
		
	private OperationMode (final String menuText, 
			final String description, 
			final boolean usingFilename) {
		this.menuText = menuText;
		this.description = description;
		this.usingFilename = usingFilename;
	}

	public String getMenuText() {
		return menuText;
	}

	public String getDescription() {
		return description;
	}

	public boolean isUsingFilename() {
		return usingFilename;
	}
}
