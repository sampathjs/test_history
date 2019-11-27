package com.jm.shanghai.accounting.udsr.model.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * History:
 * 2018-11-27		V1.0	jwaechter	- Initial Version
 */

/**
 * Contains the data of a row of the java table. Can add columns but cannot be reordered 
 * within other rows.
 * @author jwaechter
 * @version 1.0
 */
public class JavaTableRow {
	private int rowId;
	private final Map<String, JavaTableCell> columnNamesToCell; 
	private final Map<Integer, JavaTableCell> columnIdsToCell;
	
	public JavaTableRow (final int rowId) {
		this.rowId = rowId;
		columnNamesToCell = new HashMap<String, JavaTableCell>(400);
		columnIdsToCell = new HashMap<Integer, JavaTableCell>(400);
	}
	
	public JavaTableRow (final int rowId, 
			final List<JavaTableColumn> colDefinitions, 
			final List<Object> rowData,
			final List<String> displayStrings) {
		this(rowId);
		if (colDefinitions.size() != rowData.size() ||
				displayStrings.size() != colDefinitions.size()) {
			throw new RuntimeException ("Error when creating table row: number of column definitions"
					+ " does not match number of row data objects");
		}
		for (int i=0; i < colDefinitions.size(); i++) {
			if (colDefinitions.get(i).getColId() != i) {
				throw new RuntimeException ("Error when creating table row: column definition "
						+ " does not match column order (" + colDefinitions.get(i).getColId() + "," + i +")");				
			}
			JavaTableCell cell = new JavaTableCell(displayStrings.get(i), rowData.get(i),
					colDefinitions.get(i));
			columnNamesToCell.put(colDefinitions.get(i).getColName(), cell);
			columnIdsToCell.put(i, cell);
		}		
	}

	public void addColumn (JavaTableColumn colDef, Object data, String displayString) {
		int currentColumnCount = columnNamesToCell.size();
		if (colDef.getColId() != currentColumnCount) {
			throw new RuntimeException ("Error when creating table row: column definition "
					+ " does not match column order (" + colDef.getColId() + "," + currentColumnCount +")");
		}
		JavaTableCell cell = new JavaTableCell(displayString, data, colDef);
		columnNamesToCell.put(colDef.getColName(), cell);
		columnIdsToCell.put(currentColumnCount, cell);
	}
	
	public String getDisplayString (String colName) {
		return columnNamesToCell.get(colName).getDisplayString();
	}
	
	public Object getValue (String colName) {
		return columnNamesToCell.get(colName).getValue();
	}

	public void setDisplayString (String colName, String displayString) {
		columnNamesToCell.get(colName).setDisplayString(displayString);
	}
	
	public void setValue (String colName, Object value) {
		columnNamesToCell.get(colName).setValue(value);
	}

	public void setValueAndDisplayString (String colName, Object value, String displayString) {
		JavaTableCell cell = columnNamesToCell.get(colName);
		cell.setValue(value);
		cell.setDisplayString(displayString);
	}

	
	public int getRowId() {
		return rowId;
	}

	public void setRowId(int rowId) {
		this.rowId = rowId;
	}

	public Object[] getValues() {
		Object[] values = new Object[columnNamesToCell.size()];
		for (int i=0; i < columnIdsToCell.size(); i++) {
			values[i] = columnIdsToCell.get(i).getValue();
		}
		return values;
	}	

	public void renameColumn(String oldColName, String newColName) {
		JavaTableCell cell = columnNamesToCell.remove(oldColName);
		columnNamesToCell.put(newColName, cell);
	}

	public JavaTableRow duplicate(int size, List<JavaTableColumn> colDefinitions) {
		List<Object> rowData = new ArrayList<>(colDefinitions.size());
		List<String> displayStrings = new ArrayList<>(colDefinitions.size());
		for (int i=columnIdsToCell.size()-1; i >= 0; i--) {
			rowData.add(columnIdsToCell.get(i).getValue());
			displayStrings.add(columnIdsToCell.get(i).getDisplayString());
		}
		return new JavaTableRow(rowId, colDefinitions, rowData, displayStrings);
	}
}
