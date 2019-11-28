package com.jm.shanghai.accounting.udsr.model.retrieval;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;

/*
 * History:
 * 2018-11-27	V1.0	jwaechter	- Initial Version
 * 2019-02-11	V1.1	jwaechter	- removed unused "select" method
 */

/**
 * A java only table that is a shallow mirror of the OpenLink in memory table.
 * It can only add rows and remove single rows, can add columns but can't
 * reorder rows or columns or remove columns.
 * On the other hand, it is much faster compared to the OpenLink table
 * @author jwaechter
 * @version 1.1
 *
 */
public class JavaTable {
	private final List<JavaTableColumn> colList;
	private final Map<String, JavaTableColumn> colNameMap;
	private final Map<Integer, JavaTableRow> rows;
	
	public JavaTable () {
		colList = new ArrayList<>(100);
		rows = new HashMap<>(10000);
		colNameMap = new HashMap<>(400);
	}
	
	public void mergeAddEndurTable (Table endurTable) {
		for (TableColumn tc : endurTable.getColumns()) {
			addColumn (tc);
		}
		for (int i = endurTable.getRowCount()-1; i >= 0; i--) {
			addRow (endurTable, i);
		}
	}
	
	public JavaTableRow addRow (Table endurTable, int rowNum) {
		// assumption: all columns already exist
		List<Object> rowData = new ArrayList<> ();
		List<String> displayStrings = new ArrayList<> ();
		for (JavaTableColumn col : colList) {
			// TODO: do not execute isValidColumn on every row, just one time per column!
			if (endurTable.isValidColumn(col.getColName())) {
				rowData.add(endurTable.getValue(col.getColName(), rowNum));
				displayStrings.add(endurTable.getDisplayString(
						endurTable.getColumnId(col.getColName()), rowNum));
			} else {
				rowData.add(col.getDefaultValue());
				displayStrings.add(col.getDefaultValue().toString());
			}
		}
		JavaTableRow newRow = new JavaTableRow(rows.size(), colList, rowData, displayStrings);
		rows.put(rows.size(), newRow);
		return newRow;
	}
	
	public JavaTableColumn addColumn (String colName, EnumColType colType) {
		if (colNameMap.containsKey(colName)) {
			if (colNameMap.get(colName).getColType() != colType) {
				throw new RuntimeException ("Can't add new column '" + colName + "'."
						+ " It does already exist and has a different col type: "
						+ colNameMap.get(colName).getColType() + " vs. " + colType);
			}
			return colNameMap.get(colName);
		}
		// complete new column
		Object defaultValue = getDefaultValueForColType (colType);
		JavaTableColumn newColDef = new JavaTableColumn(colName, colType, defaultValue, colList.size());
		colList.add(newColDef);
		colNameMap.put(colName, newColDef);
		for (Entry<Integer, JavaTableRow> row : rows.entrySet()) {
			row.getValue().addColumn(newColDef, defaultValue, defaultValue.toString());
		}
		return newColDef;
	}
	
	public int getRowCount() {
		return rows.size();
	}
	
	public JavaTableColumn addColumn (TableColumn endurTableColumn) {
		String colName = endurTableColumn.getName();
		if (colNameMap.containsKey(colName)) {
			if (colNameMap.get(colName).getColType() != endurTableColumn.getType()) {
				throw new RuntimeException ("Can't add new column '" + colName + "'."
						+ " It does already exist and has a different col type: "
						+ colNameMap.get(colName).getColType() + " vs. " + endurTableColumn.getType());
			}
			return colNameMap.get(colName);
		}
		// complete new column
		Object defaultValue = getDefaultValueForColType (endurTableColumn.getType());
		JavaTableColumn newColDef = new JavaTableColumn(endurTableColumn, defaultValue, colList.size());
		colList.add(newColDef);
		colNameMap.put(colName, newColDef);
		for (Entry<Integer, JavaTableRow> row : rows.entrySet()) {
			row.getValue().addColumn(newColDef, defaultValue, defaultValue.toString());
		}
		return newColDef;
	}
	
	private Object getDefaultValueForColType(EnumColType type) {
		switch (type) {
		case Date:
			return new Date();
		case DateTime:
			return new Date();
		case Double:
			return new Double(0);
		case Int:
			return new Integer(0);
		case Long:
			return new Long(0);
		case String:
			return "";
		default:
			throw new RuntimeException ("Column type " + type + " does not have a default value yet");
		}
	}

	public String getDisplayString (String colName, int rowNum) {
		return rows.get(rowNum).getDisplayString(colName);
	}
	
	public Object getValue (String colName, int rowNum) {
		return rows.get(rowNum).getValue(colName);
	}

	public void setDisplayString (String colName, int rowNum, String displayString) {
		rows.get(rowNum).setDisplayString(colName, displayString);
	}
	
	public void setValue (String colName, int rowNum,Object value) {
		rows.get(rowNum).setValue(colName, value);
	}

	public void mergeIntoEndurTable(Table endurTable) {
		// assumption: endurTable columns order matches the one of this java table 
		// but does not have rows
		for (JavaTableColumn col : colList) {
			if (!endurTable.isValidColumn(col.getColName())) {
				endurTable.addColumn(col.getColName(), col.getColType());
			}
		}
		endurTable.addRows(rows.size());
		for (JavaTableRow row : rows.values()) {
			endurTable.setRowValues(row.getRowId(), row.getValues());
		}
	}

	public int getInt(String colName, int row) {
		JavaTableColumn jtc = colNameMap.get(colName);
		if (jtc.getColType() ==  EnumColType.Int) {
			return (Integer)rows.get(row).getValue(colName);
		}
		throw new RuntimeException ("Error: illegal columns type: tried to retrieve an int from column " + jtc.toString());
	}

	public boolean isValidColumn(String retrievalLogic) {
		return colNameMap.containsKey(retrievalLogic);
	}

	public void renameColumn(String oldColName, String newColName) {
		JavaTableColumn jtc = colNameMap.get(oldColName);
		jtc.setColName (newColName);
		colNameMap.remove(oldColName);
		colNameMap.put(newColName, jtc);
		for (JavaTableRow row : rows.values()) {
			row.renameColumn(oldColName, newColName);
		}
	}

	public JavaTableRow getRow(int i) {
		return rows.get(i);
	}

	public EnumColType getColumnType(String colName) {
		JavaTableColumn jtc = colNameMap.get(colName);
		return jtc.getColType();
	}
}
