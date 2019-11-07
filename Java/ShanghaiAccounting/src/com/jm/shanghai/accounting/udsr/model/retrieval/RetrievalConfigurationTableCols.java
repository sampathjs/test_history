package com.jm.shanghai.accounting.udsr.model.retrieval;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.fixed.UserTableColumn;
import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2018-11-20	V1.0		jwaechter		- Initial Version
 */

/**
 * Enum containing the known columns of the user table {@link ConfigurationItem#RETRIEVAL_CONFIG_TABLE_NAME}
 * @author jwaechter
 * @version 1.0
 */
public enum RetrievalConfigurationTableCols implements UserTableColumn {
	PRIORITY ("priority", "Priority", EnumColType.Int, "Integer"),
	COL_NAME_RUNTIME_TABLE ("col_name_runtime_table", "Column Name in \nRuntime Data Table", EnumColType.String, "String"),
	COL_NAME_MAPPING_TABLE ("col_name_mapping_table", "Column Name in\n Mapping Table", EnumColType.String, "String"),
	COL_NAME_OUTPUT_TABLE ("col_name_report_output", "Column Name in\n Output Table", EnumColType.String, "String"),
	COL_NAME_TAX_TABLE ("col_name_tax_table", "Column Name in\n Tax Mapping Table", EnumColType.String, "String"),
	COL_NAME_MATERIAL_NUMBER_TABLE ("col_name_material_number_table", "Column Name in\n Material Number Mapping Table", EnumColType.String, "String"),
	RETRIEVAL_LOGIC ("retrieval_logic", "Retrieval Logic", EnumColType.String, "String"),
	;
	
	private final String colName;
	private final String colTitle;
	private final EnumColType colType;
	private final String colTypeName;
	
	private RetrievalConfigurationTableCols (String name, String title, EnumColType type, String colTypeName)	{
		colName = name;
		colTitle = title;
		colType = type;
		this.colTypeName = colTypeName;
		
	}
	
	@Override
	public String getColName() {
		return colName;
	}
	
	@Override
	public EnumColType getColType() {
		return colType;
	}
	
	@Override
	public String getColTitle() {
		return colTitle;
	}

	@Override
	public String getColTypeName() {
		return colTypeName;
	}
}
