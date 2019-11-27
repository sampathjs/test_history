package com.jm.shanghai.accounting.udsr.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class GroupingSelectionDialog extends JDialog {
	private final List<String> colNameGrouping;
	private final List<String> colNameAll;
	private final List<String> colNameUnselected;
	
	private DefaultListModel<String> unselectedColumnsModel;
	private JList<String> unselectedColumns;
	private DefaultListModel<String> selectedColumnsModel;
	private JList<String> selectedColumns;
	private JButton selectButton;
	private JButton deselectButton;
	private JButton moveSelectionUp;
	private JButton moveSelectionDown;

	private JButton okButton;
	private JButton cancelButton;
	
	private boolean cancelled=true;

	
	public GroupingSelectionDialog(MappingTablePane mappingTablePane,
			boolean modal, List<String> colNameGrouping, List<String> colNameAll) {
		super(mappingTablePane.getMainDialog(), modal);
		this.setTitle("Select the grouping for the rule tree");
		this.colNameGrouping = new ArrayList<>(colNameGrouping);
		this.colNameAll = new ArrayList<>(colNameAll);
		colNameUnselected = createColNameUnselected();
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		JPanel mainPanel = new JPanel();
		
		unselectedColumnsModel = new DefaultListModel<>();
		for (int i=0; i < colNameUnselected.size(); i++) {
			unselectedColumnsModel.addElement(colNameUnselected.get(i));
		}
		unselectedColumns = new JList<String>(unselectedColumnsModel);
		unselectedColumns.setSize(300, 500);
		JScrollPane scrollPaneUnselectedColumns = new JScrollPane(unselectedColumns);
		scrollPaneUnselectedColumns.setSize(330, 550);
		scrollPaneUnselectedColumns.setPreferredSize(new Dimension(330, 550));
		scrollPaneUnselectedColumns.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(scrollPaneUnselectedColumns);

		deselectButton = new JButton ("<");
		deselectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doDeselect();
			}
		});
		mainPanel.add(deselectButton);

		selectButton = new JButton (">");
		selectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doSelect();
			}
		});
		mainPanel.add(selectButton);
		
		selectedColumnsModel = new DefaultListModel<>();
		for (int i=0; i < colNameGrouping.size(); i++) {
			selectedColumnsModel.addElement(colNameGrouping.get(i));
		}
		selectedColumns = new JList<String>(selectedColumnsModel);
		selectedColumns.setSize(300, 500);
		JScrollPane scrollPaneSelectedColumns = new JScrollPane(selectedColumns);
		scrollPaneSelectedColumns.setSize(330, 550);
		scrollPaneSelectedColumns.setPreferredSize(new Dimension(330, 550));
		scrollPaneSelectedColumns.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(scrollPaneSelectedColumns);
		
		moveSelectionUp = new JButton("Up");
		moveSelectionUp.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMoveUp();
			}
		});
		moveSelectionDown = new JButton("Down");
		moveSelectionDown.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMoveDown();
			}
		});
		
		JPanel moveUpAndDownPane = new JPanel();
		moveUpAndDownPane.setLayout(new BoxLayout(moveUpAndDownPane, BoxLayout.Y_AXIS));
		moveUpAndDownPane.add(moveSelectionUp);
		moveUpAndDownPane.add(moveSelectionDown);
		mainPanel.add(moveUpAndDownPane);
		getContentPane().add(mainPanel);
		
		JPanel okAndCancelPanel = new JPanel();
		okAndCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doOk();				
			}
		});
		okAndCancelPanel.add(okButton);
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doCancel();
			}
		});
		okAndCancelPanel.add(cancelButton);
		getContentPane().add(okAndCancelPanel);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(1024, 768);
		setVisible(true);
		toFront();
	}

	protected void doSelect() {
		if (!unselectedColumns.isSelectionEmpty()) {
			int unselectedIndices[] = unselectedColumns.getSelectedIndices();
			List<String> columns = unselectedColumns.getSelectedValuesList();
			for (int i=unselectedIndices.length-1; i >= 0; i--) {
				selectedColumnsModel.addElement(columns.get(i));
				unselectedColumnsModel.remove(i);
			}
		}	
	}

	protected void doDeselect() {
		if (!selectedColumns.isSelectionEmpty()) {
			int selectedIndices[] = selectedColumns.getSelectedIndices();
			List<String> columns = selectedColumns.getSelectedValuesList();
			selectedColumns.clearSelection();
			for (int i=selectedIndices.length-1; i >= 0; i--) {
				int indexToInsert = calculateIndexToInsert(columns.get(i));
				unselectedColumnsModel.insertElementAt(columns.get(i), indexToInsert);
				selectedColumnsModel.remove(selectedIndices[i]);
			}
		}
	}

	private int calculateIndexToInsert(String colName) {
		int indexAllColNames = colNameAll.indexOf(colName);
		if (indexAllColNames == 0) {
			return 0;
		}
		int indexSelectedColumnList = selectedColumnsModel.indexOf(colName);
		int indexToInsert = indexAllColNames;
		for (int i=indexSelectedColumnList-1; i >= 0; i--) {
			String selectedColumnValue = selectedColumnsModel.get(i);
			if (colNameAll.indexOf(selectedColumnValue) < indexAllColNames) {
				indexToInsert--;
			}
		}
		return indexToInsert;
	}

	protected void doMoveDown() {
		if (!selectedColumns.isSelectionEmpty()) {
			int selectedIndices[] = selectedColumns.getSelectedIndices();
			if (selectedIndices[selectedIndices.length-1] == selectedColumnsModel.getSize()-1) {
				return;
			}
			for (int i=0; i < selectedIndices.length; i++) {
				swapListElements(selectedColumnsModel, selectedIndices[i]+1, selectedIndices[i]);
				selectedIndices[i]++;
			}
			selectedColumns.setSelectedIndices(selectedIndices);
		}
	}

	protected void doMoveUp() {
		if (!selectedColumns.isSelectionEmpty()) {
			int selectedIndices[] = selectedColumns.getSelectedIndices();
			if (selectedIndices[0] == 0) {
				return;
			}
			for (int i=0; i < selectedIndices.length; i++) {
				swapListElements(selectedColumnsModel, selectedIndices[i]-1, selectedIndices[i]);
				selectedIndices[i]--;
			}
			selectedColumns.setSelectedIndices(selectedIndices);
		}		
	}
	
	private void swapListElements(DefaultListModel<String> listModel, int a, int b) {
		String elementA = listModel.getElementAt(a);
		String elementB = listModel.getElementAt(b);
		listModel.set(a, elementB);
		listModel.set(b, elementA);
	}

	protected void doOk() {
		List<String> colNameNewSelected = getAllValuesFromJList(selectedColumns);
		if (colNameNewSelected.size() == 0) {
			JOptionPane.showMessageDialog(this, "At least one grouping criterium has to be selected.");
			return;
		}
		colNameGrouping.clear();
		colNameGrouping.addAll(colNameNewSelected);
		cancelled=false;
		this.dispose();
	}

	private List<String> getAllValuesFromJList(
			JList<String> jList) {
		List<String> allValues = new ArrayList<String>(jList.getModel().getSize());
		for (int i=0; i < jList.getModel().getSize(); i++) {
			allValues.add(jList.getModel().getElementAt(i));
		}
		return allValues;
	}

	protected void doCancel() {
		this.dispose();		
	}

	private List<String> createColNameUnselected() {
		List<String> colNames = new ArrayList<>(colNameAll);
		for (String colName : colNameGrouping) {
			colNames.remove(colName);
		}
		return colNames;
	}

	public List<String> getColNameGrouping() {
		return colNameGrouping;
	}

	public boolean isCancelled() {
		return cancelled;
	}
}
