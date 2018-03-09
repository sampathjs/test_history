package com.olf.jm.interfaces.lims.model;

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
