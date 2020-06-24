package com.jm.shanghai.accounting.udsr.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

import com.jm.shanghai.accounting.udsr.model.mapping.MappingConfigurationColType;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableCellConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableRowConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.AbstractPredicate;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/*
 * History:
 * 2019-08-02	V1.0	jwaechter		- 	Initial Version
 */

/**
 * This displays a rule table of the accounting configuration.
 * @author jwaechter
 * @version 1.0
 */
public class RuleTable extends EndurBackedTable implements MouseListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1644853434332457602L;

	protected AbstractPredicate[][] predicates;
	protected volatile RetrievalConfiguration[] retrievalConfigs;
	protected BooleanFunction applyPredicates;
	protected Map<String, String> columnNameMappingTable2ColumnNameRuntimeTable;
	protected Map<Integer, Integer> rowIdToUniqueRowId;
	protected Table runtimeTable;
	protected final Table rootRuleTable;
	protected DataPanel dataPanel=null;
	protected final RetrievalConfigurationColDescriptionLoader colLoader;
	protected JPopupMenu contextMenu;
	protected JMenuItem newRowMenuItem;
	protected JMenuItem deleteRowMenuItem;
	protected JMenuItem duplicateRowMenuItem;

	private RetrievalConfigurationColDescription retrievalConfigurationColDescription;

	public RuleTable(final Table endurTable, final List<MappingTableRowConfiguration> mappingRows,
			final List<RetrievalConfiguration> allRetrievalConfigs,
			final RetrievalConfigurationColDescription retrievalConfigurationColDescription,
			final RetrievalConfigurationColDescriptionLoader colLoader,
			final DataPanel dataPanel) {
		super(endurTable);
		this.dataPanel = dataPanel;
		this.retrievalConfigurationColDescription = retrievalConfigurationColDescription;
		updatePredicates(mappingRows);
		updateRetrievalConfigs(allRetrievalConfigs);
		this.colLoader = colLoader;
		this.addMouseListener(this);
		this.setAutoCreateRowSorter(true);
		this.rootRuleTable = endurTable;
		this.contextMenu = new JPopupMenu();
		this.newRowMenuItem = new JMenuItem ("Add new row");
		this.newRowMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				rootRuleTable.addRow();
				dataPanel.getMappingTablePane().recreateTree(true);
			}
		});
		this.duplicateRowMenuItem = new JMenuItem ("Duplicate selected rows");
		this.duplicateRowMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedRows[] = getSelectedRows();
				RuleTableModel rtm = (RuleTableModel) getModel();
				for (int i = 0; i < selectedRows.length; i++) {
					int selectedRow = selectedRows[i];
					int selectedRowModel = convertRowIndexToModel(selectedRow);
					int colNumRowId = rtm.findColumn("row_id");
					int rowId = Integer.parseInt(rtm.getValueAt(selectedRowModel, colNumRowId));
					int rowInRootTable = rootRuleTable.findSorted(rootRuleTable.getColumnId("row_id"), rowId, 0);
					TableRow newRow = rootRuleTable.addRow();
					rootRuleTable.copyRowData(rowInRootTable, newRow.getNumber());
				}
				dataPanel.getMappingTablePane().recreateTree(true);
			}
		});
		this.deleteRowMenuItem = new JMenuItem ("Delete selected rows");
		this.deleteRowMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedRows[] = getSelectedRows();
				RuleTableModel rtm = (RuleTableModel) getModel();
				for (int i = selectedRows.length-1; i >= 0; i--) {
					int selectedRow = selectedRows[i];
					int selectedRowModel = convertRowIndexToModel(selectedRow);
					int colNumRowId = rtm.findColumn("row_id");
					int rowId = Integer.parseInt(rtm.getValueAt(selectedRowModel, colNumRowId));
					int rowInRootTable = rootRuleTable.findSorted(rootRuleTable.getColumnId("row_id"), rowId, 0);
					rootRuleTable.removeRow(rowInRootTable);
				}
				dataPanel.getMappingTablePane().recreateTree(true);
			}
		});
		this.contextMenu.add(newRowMenuItem);
		this.contextMenu.add(duplicateRowMenuItem);
		this.contextMenu.add(deleteRowMenuItem);
		this.contextMenu.setLightWeightPopupEnabled(false);
		this.addMouseListener(new PopupListener());
		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//		this.setComponentPopupMenu(contextMenu);
	}

	@Override
	public void updateTable() {
		super.updateTable();
		setDefaultRenderer(Object.class, new RuleResultRenderer());
	}

	@Override
	public String getToolTipText(MouseEvent me)  {
		int colNum = columnAtPoint(me.getPoint());
		if (retrievalConfigs == null) {
			return null;
		}
		RetrievalConfiguration rc = retrievalConfigs[colNum];
		if (rc == null) {
			return null;
		}
		String colNameRuntimeTable = rc.getColumnValue(colLoader.getRuntimeDataTable());
		String retrievalLogic = rc.getColumnValue(colLoader.getRetrievalLogic());
		String colNameReportOutput = rc.getColumnValue(colLoader.getReportOutput());
		String colName = getColumnName(colNum);
		return "" 
		+	escapeHTML("Column name (mapping table): '" + colName) 
		+ escapeHTML("'      ,Column name (runtime table): '" + colNameRuntimeTable)
		+ escapeHTML("'      ,Column name (report output): '" + colNameReportOutput)
		+ escapeHTML("'      ,has retrieval logic '" + retrievalLogic + "'")
		+ ""
		;
	}

	private static String escapeHTML(String s) {
		StringBuilder out = new StringBuilder(Math.max(16, s.length()));
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
				out.append("&#");
				out.append((int) c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	public void setRuntimeTable(final Table runtimeTable) {
		this.runtimeTable = runtimeTable;
	}

	public void setApplyPredicates(final BooleanFunction applyPredicates) {
		this.applyPredicates = applyPredicates;
	}

	public void updateRetrievalConfigs(
			List<RetrievalConfiguration> allRetrievalConfigs) {
		if (allRetrievalConfigs != null) {
			columnNameMappingTable2ColumnNameRuntimeTable = new HashMap<String, String>();
			retrievalConfigs = new RetrievalConfiguration[columnNames.length];
			for (RetrievalConfiguration rc : allRetrievalConfigs) {
				String colNameMappingTable = rc.getColumnValue(retrievalConfigurationColDescription);
				if (colNameMappingTable != null && !colNameMappingTable.isEmpty()) {
					int columnIndex = findColumnName(colNameMappingTable);
					if (columnIndex >= 0) {
						retrievalConfigs[columnIndex] = rc;
					}
					columnNameMappingTable2ColumnNameRuntimeTable.put(colNameMappingTable, rc.getColumnValue(colLoader.getRuntimeDataTable()));
				}
			}
		} else {
			columnNameMappingTable2ColumnNameRuntimeTable = null;			
			retrievalConfigs = null;
		}
	}

	private int findColumnName(String colNameMappingTable) {
		for (int col = columnNames.length-1; col >= 0; col--) {
			if (columnNames[col].equals(colNameMappingTable)) {
				return col;
			}
		}
		return -1;
	}

	public void updatePredicates(List<MappingTableRowConfiguration> mappingRows) {
		rowIdToUniqueRowId = new HashMap<Integer, Integer>();
		if (mappingRows != null) {
			predicates = new AbstractPredicate[mappingRows.size()][];
			for (int row=mappingRows.size()-1; row >= 0; row--) {
				MappingTableRowConfiguration mtrc = mappingRows.get(row);
				predicates[mtrc.getUniqueRowId()-1] = new AbstractPredicate[columnNames.length];
				for (int col = mtrc.getCellConfigurations().size()-1; col >= 0; col--) {
					MappingTableCellConfiguration mtcc = mtrc.getCellConfigurations().get(col);
					if (mtcc.getColConfig().getMappingColType() != MappingConfigurationColType.MAPPING_LOGIC) {
						continue;
					}
					String colName = mtcc.getColConfig().getColName();
					AbstractPredicate predicate = mtcc.getPredicate();
					int colMapped = endurTable.getColumnId(colName);
					predicates[mtrc.getUniqueRowId()-1][colMapped] = predicate;
					rowIdToUniqueRowId.put(row, mtrc.getUniqueRowId()-1);
				}
			}
		} else {
			predicates = null;
		}
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return true;
	}

	private class RuleResultRenderer extends JLabel implements TableCellRenderer {		
		public RuleResultRenderer() {
			setOpaque(true); //MUST do this for background to show up.			
		}

		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object content,
				boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText((String)content);
			if (isSelected) {
				setBackground(Color.BLUE);				
				return this;
			}
			int rowModel = table.convertRowIndexToModel(row);
			int colNumUniqueRowNum = table.getColumn("row_id").getModelIndex();
			int uniqueRowId = Integer.parseInt((String)table.getModel().getValueAt(rowModel, colNumUniqueRowNum /*table.getColumnCount()-1*/))-1;
			int uniqueRowIdConverted = uniqueRowId;//table.convertRowIndexToView(uniqueRowId);
			int columnConverted = table.convertColumnIndexToModel(column);
			if (applyPredicates != null && applyPredicates.isTrue() 
					&& predicates != null && retrievalConfigs != null 
					&& runtimeTable != null) {
				if (predicates[uniqueRowIdConverted][columnConverted] != null) {
					// get name of column in runtime data table
					String colNameRuntimeTable = columnNameMappingTable2ColumnNameRuntimeTable.get(columnNames[columnConverted]);
					int colIdRuntimeTable = runtimeTable.getColumnId(colNameRuntimeTable);
					if (predicates[uniqueRowIdConverted][columnConverted].evaluate(runtimeTable.getDisplayString(colIdRuntimeTable, 0))) {
						setBackground(Color.GREEN);
					} else {
						setBackground(Color.RED);
					}
				} else {
					setBackground(Color.WHITE);						
				}
			} else {
				setBackground(Color.WHITE);						
			}
			return this;
		}
	}

	public List<String> getAllColumnNames() {
		List<String> allColumnNames = new ArrayList<>();
		for (int i=0; i < getColumnCount(); i++) {
			allColumnNames.add(getColumnName(i));
		}
		return allColumnNames;
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		int colNum = columnAtPoint(me.getPoint());
		int colNumModel = convertColumnIndexToModel(colNum);
		if (retrievalConfigs == null) {
			return;
		}
		RetrievalConfiguration rc = retrievalConfigs[colNum];
		if (rc == null) {
			return;
		}
		String colNameRuntimeTable = rc.getColumnValue(colLoader.getRuntimeDataTable());
		EndurBackedTable jRuntimeTable = dataPanel.getTransactionPanel().getRuntimeTable();
		int selectedRow = jRuntimeTable.getSelectedRow();
		int column = jRuntimeTable.getColumn(colNameRuntimeTable).getModelIndex();
		jRuntimeTable.scrollRectToVisible(new Rectangle(jRuntimeTable.getCellRect(selectedRow, column, true)));
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	protected javax.swing.table.TableModel createTableModel () {
		return new RuleTableModel();
	}

	protected class RuleTableModel extends TableModel {
		@Override
		public void setValueAt(Object value, int row, int col) {
			super.setValueAt(value, row, col);
			if (col < 0 || row < 0) {
				return;
			}
			int colNumRowId = getColumnModel().getColumnIndex("row_id");
			int rowId = Integer.parseInt(getValueAt(row, colNumRowId));
			int rowInRootTable = rootRuleTable.findSorted(rootRuleTable.getColumnId("row_id"), rowId, 0);
			rootRuleTable.setValue(convertColumnIndexToModel(col), rowInRootTable, value);
			dataPanel.getMappingTablePane().recreateTree(true);
		}

	}

	private class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				contextMenu.show(e.getComponent(),
						e.getX(), e.getY());
			}
		}
	}
}
