package com.olf.jm.interfaces.lims.util.model;

import com.olf.jm.interfaces.lims.model.RelevantUserTables;

/*
 * History: 
 * 2015-08-20 - V1.0 - initial version
 */

/**
 * Contains the different operating modes of the {@link LimUtilParam} task.
 * 
 * @author jwaechter
 * @version 1.0
 */
public enum OperationMode {
	LOCAL_CREATE ("Local Create", "Creates the selected user tables (locally)",
			OperationModeType.LOCAL_DB_SCHEMA), 
	LOCAL_DROP ("Local Drop", "Drops the selected user tables (locally)",
			OperationModeType.LOCAL_DB_SCHEMA),
	LOCAL_CLEAR ("Local Clear", "Clears all datea from all selected user tables" 
			+ "(locally)", OperationModeType.LOCAL_DB_SCHEMA),
	LOCAL_ADD_TEST_DATA ("Add Test Data",  
			"Fills " + RelevantUserTables.JM_METAL_PRODUCT_TEST 
			+ " with test data (locally)",
			OperationModeType.LOCAL_DB_DATA),
	LOCAL_REMOVE_CONFIG_REPOSITORY_DATA ("Remove Constants Repository Data",  
					"Removes all LIMS related ConstantsRepo	sitory",
					OperationModeType.LOCAL_DB_DATA),
	LOCAL_ADD_DEFAULT_CONFIG_REPOSITORY_DATA ("Add Default Constants Repository Data",  
			"Adds all default values to the ConstantsRepository",
			OperationModeType.LOCAL_DB_DATA),
	REMOTE_TEST_QUERY_SAMPLE ("Run Test Query for Sample",  
			"Executes a query on the remote system for sample data",
			OperationModeType.EXTERNAL_READ),
	REMOTE_TEST_QUERY_RESULT ("Run Test Query for Result",  
			"Executes a query on the remote system for result data",
			OperationModeType.EXTERNAL_READ),
	;
	
	private final String menuText;
	
	private final String description;
	
	private final OperationModeType type;
	
	private OperationMode (String menuText, String description, 
			OperationModeType type) {
		this.menuText = menuText;
		this.description = description;
		this.type = type;
	}

	public String getMenuText() {
		return menuText;
	}

	public String getDescription() {
		return description;
	}

	public OperationModeType getType() {
		return type;
	}
}
