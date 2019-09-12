package com.jm.shanghai.accounting.udsr.gui;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.olf.openrisk.table.Table;

/*
 * History:
 * 2019-04-30	V1.0	jwaechter	- Initial Version
 */

/**
 * 
 * @author jwaechter
 * @version 1.0
 */
public class EndurBackedTable extends JTable {
	private static final long serialVersionUID = -2499384946394164577L;
	protected Table endurTable;
	protected int colCount;
	protected int rowCount;
	protected String[] columnNames;
	protected String[][] tableContent;
	
	public EndurBackedTable (Table endurTable) {
		super();
		this.endurTable = endurTable;
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		updateTable();
	}
			
	protected void updateTable() {
		colCount = endurTable.getColumnCount();
		rowCount = endurTable.getRowCount();
		columnNames = new String[colCount];
		for (int i=0; i < colCount; i++) {
			columnNames[i] = endurTable.getColumn(i).getName();
		}
		tableContent = new String[rowCount][];
		for (int row = 0; row < rowCount; row++) {
			tableContent[row] = new String[colCount];
			for (int col = 0; col < colCount; col++) {
				tableContent[row][col] = endurTable.getDisplayString(col, row);
			}
		}
		setModel(createTableModel());
		
		TableColumn column = null;
		for (int i = 0; i < colCount; i++) {
		    column = getColumnModel().getColumn(i);
	        column.setPreferredWidth(columnNames[i].length()*8); 
		}
	}

	protected javax.swing.table.TableModel createTableModel() {
		return new TableModel();
	}

	public void changeEndurTable(Table endurTable) {
		this.endurTable = endurTable;
		this.clearSelection();
		this.setVisible(false);
		updateTable();
		this.invalidate();
		this.setVisible(true);
	}

	protected class TableModel extends AbstractTableModel {
		private static final long serialVersionUID = 4247146574676440035L;

		@Override
		public int getColumnCount() {
			return colCount;
		}

		@Override
		public int getRowCount() {
			return rowCount;
		}

		@Override
		public String getValueAt(int row, int col) {
			return tableContent[row][col];
		}
		
		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}
		
		@Override
		public void setValueAt(Object value, int row, int col) {
			super.setValueAt(value, row, col);
			switch (endurTable.getColumnType(col)) {
			case String:
				endurTable.setValue(col, row, value);
				break;
			case Int:
				int valueAsInt = Integer.parseInt((String)value);
				endurTable.setValue(col, row, valueAsInt);
				break;
			case Double:
				double valueAsDouble = Double.parseDouble((String)value);
				endurTable.setValue(col, row, valueAsDouble);
				break;
			default:
				throw new RuntimeException("Conversion from String to" +
					endurTable.getColumnType(col) + " not implemented yet.");
			}
			tableContent[row][col] = (String)value;
		}
	}
}
