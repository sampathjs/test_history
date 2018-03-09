package com.olf.jm.interfaces.lims.model;

import com.olf.openjvs.enums.COL_TYPE_ENUM;

/**
 * Enum containing the known columns of the user table "USER_jm_metal_product_test_result".
 * @author jwaechter
 *
 */
public enum MetalProductTestTableCols implements UserTableColumn {
	COUNTRY ("country", "Country", COL_TYPE_ENUM.COL_STRING),
	METAL ("metal", "Metal", COL_TYPE_ENUM.COL_STRING),
	PRODUCT ("product", "Product", COL_TYPE_ENUM.COL_STRING),
	RESULT ("test_result", "Result of Test", COL_TYPE_ENUM.COL_STRING)
	;
	
	private final String colName;
	private final String colTitle;
	private final COL_TYPE_ENUM colType;
	
	private MetalProductTestTableCols (String name, String title, COL_TYPE_ENUM type)	{
		colName = name;
		colTitle = title;
		colType = type;
	}
	
	@Override
	public String getColName() {
		return colName;
	}
	
	@Override
	public COL_TYPE_ENUM getColType() {
		return colType;
	}
	
	@Override
	public String getColTitle() {
		return colTitle;
	}
}
