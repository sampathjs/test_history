package com.jm.shanghai.accounting.udsr.model.fixed;

import java.util.ArrayList;
import java.util.List;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationTableCols;

/*
 * History:
 * 2018-11-20	V1.0	jwaechter	- initial version
 */

/**
 * Contains user tables relevant for the implementation of the Shanghai Accounting Feed implementation.
 * @author jwaechter
 * @version 1.0
 */
public enum RelevantUserTables implements UserTable {
	RETRIEVAL_CONFIGURATION_TABLE (ConfigurationItem.RETRIEVAL_CONFIG_TABLE_NAME.getValue(), RetrievalConfigurationTableCols.values(), "Contains the data for country, metal, product and test")
	;
	
	private final List<UserTableColumn> columns;
	private final String tableName;
	private final String description;

	private RelevantUserTables (String name, UserTableColumn[] columns, String desc) {
		tableName = name;
		this.columns = new ArrayList<>();
		for (UserTableColumn col : columns) {
			this.columns.add(col);			
		}
		this.description = desc;
	}
	
	@Override
	public String getName() {
		return tableName;
	}
	
	@Override
	public List<UserTableColumn> getColumns() {
		return columns;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}
}