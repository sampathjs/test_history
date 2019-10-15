package com.jm.shanghai.accounting.udsr.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2019-04-30	V1.0	jwaechter	- Initial Version
 */

public class TransactionPanel extends Panel {
	private static final String SIM_NAME_JM_SHANGHAI_RAW_ACCOUNTING_GL = "JM Shanghai Raw Accounting GL";
	private static final String SIM_NAME_JM_SHANGHAI_RAW_ACCOUNTING_SL = "JM Shanghai Raw Accounting SL";

	private static final long serialVersionUID = -3408692107123006516L;
	private final DataPanel dataPanel;
	private final MappingTablePane mappingTablePane;
	private Panel buttonArea;
	private JButton loadDealButton;
	private JButton loadTransactionButton;
	private JRadioButton filterMode;
	private JRadioButton highlightMode;
	private ButtonGroup modeGroup;
	private JList<String> simulationName;
	private JButton changeGroupingButton;
	private EndurBackedTable runtimeTable;	
	private JScrollPane scrollPaneForRuntimeTable;
	private JLabel runtimeDataLabel;
	
	
	public TransactionPanel (final DataPanel dataPanel, final MappingTablePane mappingTablePane) {
		super();
		this.dataPanel = dataPanel;
		this.mappingTablePane = mappingTablePane;
		simulationName = new JList<>(new Vector<>(Arrays.asList(SIM_NAME_JM_SHANGHAI_RAW_ACCOUNTING_GL, SIM_NAME_JM_SHANGHAI_RAW_ACCOUNTING_SL)));
		simulationName.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				if (!arg0.getValueIsAdjusting()) {
					return;
				}
				if (mappingTablePane.getMainDialog().getTransactions() != null) {
					mappingTablePane.runSimulation();
					adaptRuleTableFilterAndColors(-1);
					dataPanel.getMainDialog().notifySeletedSimChanged(simulationName.getSelectedIndex());
				}
			}
		});
		simulationName.setSelectedIndex(0);
		loadDealButton = new JButton("Load By Deal #");
		loadDealButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				loadDeal();
				adaptRuleTableFilterAndColors(-1);
			}		
		});
		loadTransactionButton = new JButton("Load By Tran #");
		loadTransactionButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				loadTransaction();
				adaptRuleTableFilterAndColors(-1);
			}		
		});

		filterMode = new JRadioButton("Show matching rules");
		filterMode.addActionListener( new ActionListener () {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedRow = runtimeTable.getSelectedRow();
				adaptRuleTableFilterAndColors(selectedRow);				
			}
		});
		highlightMode = new JRadioButton("Highlight matching columns");
		highlightMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedRow = runtimeTable.getSelectedRow();
				adaptRuleTableFilterAndColors(selectedRow);
			}
		});
		modeGroup = new ButtonGroup();
		modeGroup.add(filterMode);
		modeGroup.add(highlightMode);
		filterMode.setSelected(true);
		
		changeGroupingButton = new JButton("Change Grouping");
		changeGroupingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				changeGrouping();
			}
		});
		
		this.setLayout(new GridBagLayout());
		
		buttonArea = new Panel ();
		buttonArea.setLayout(new FlowLayout(FlowLayout.LEFT));
		buttonArea.add(loadDealButton);
		buttonArea.add(loadTransactionButton);
		buttonArea.add(filterMode);
		buttonArea.add(highlightMode);
		buttonArea.add(simulationName);
		buttonArea.add(changeGroupingButton);
		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.VERTICAL;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets = new Insets(10/*top*/, 10 /*left*/, 10 /*bottom*/, 10 /* right */);
		gc.weightx = 1;
		gc.weighty = 0;
		add(buttonArea, gc);

		gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.gridy = 1;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets = new Insets(5/*top*/, 5 /*left*/, 5 /*bottom*/, 5 /* right */);
		gc.weightx = 1;
		gc.weighty = 0;
		runtimeDataLabel = new JLabel("Runtime Data Table"); 
//		runtimeDataLabel.setPreferredSize(new Dimension(200, 15));
		add(runtimeDataLabel, gc);

		
		runtimeTable = new EndurBackedTable(mappingTablePane.getRuntimeTable());
		runtimeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		runtimeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int selectedRow = runtimeTable.getSelectedRow();
				if (selectedRow == -1) {
					return;
				}
				adaptRuleTableFilterAndColors(selectedRow);
			}
		});
		scrollPaneForRuntimeTable = new JScrollPane(runtimeTable);
		gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.gridy = 2;
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets = new Insets(10/*top*/, 10 /*left*/, 10 /*bottom*/, 10 /* right */);
		gc.weightx = 1;
		gc.weighty = 1;
		add(scrollPaneForRuntimeTable, gc);
	}

	protected void changeGrouping() {	
		GroupingSelectionDialog groupingSelectionDialog = new GroupingSelectionDialog(mappingTablePane, true, mappingTablePane.getColNameGrouping(), dataPanel.getRuleTable().getAllColumnNames());
		if (!groupingSelectionDialog.isCancelled()) {
			mappingTablePane.getColNameGrouping().clear();
			mappingTablePane.getColNameGrouping().addAll(groupingSelectionDialog.getColNameGrouping());
			mappingTablePane.recreateTree(false);
			dataPanel.getRuleGroupingSelectionTree().setVisible(false);
			dataPanel.getRuleGroupingSelectionTree().invalidate();
			dataPanel.getRuleGroupingSelectionTree().setVisible(true);
		}
	}

	protected void adaptRuleTableFilterAndColors(int rowToFilterFor) {
		mappingTablePane.runMapping(rowToFilterFor);
		mappingTablePane.updateRuleTable();		
	}
	
	protected void loadTransaction() {
		Transaction tran=null;
		boolean abort = false;
		String msg = "";
		List<Transaction> trans = new ArrayList<>(); 
		do {
			String tranNumAsString = (String)JOptionPane.showInputDialog(
	                mappingTablePane,
	                msg + "Enter a transaction # or any non integer to abort, or a comma separated list of numbers",
	                "Load Transaction Dialog",
	                JOptionPane.PLAIN_MESSAGE,
	                null,
	                null,
	                "425554");
			try {
				String tokens[] = tranNumAsString.split(",");
				for (String token : tokens) {
					int tranNum = Integer.parseInt(token.trim());
					try {
						tran = mappingTablePane.getSession().getTradingFactory().retrieveTransactionById(tranNum);
						runtimeDataLabel.setText("Runtime Data Table for deal #" + tranNumAsString);
						trans.add(tran);
					} catch (Exception ex) {
						msg = "Transaction # " + token + " not found. ";					
					}					
				}
			} catch (NumberFormatException ex) {
				abort = true;
			}
		} while (tran == null && ! abort);
		if (!abort) {
			mappingTablePane.getMainDialog().clearSimCache();
			mappingTablePane.getMainDialog().setTransaction(trans);
			mappingTablePane.runSimulation();
		}
	}

	protected void loadDeal() {
		Transaction tran=null;
		boolean abort = false;
		String msg = "";
		List<Transaction> trans = new ArrayList<>(); 
		do {
			String dealNumAsString = (String)JOptionPane.showInputDialog(
	                mappingTablePane,
	                msg + "Enter a deal # or any non integer to abort or a comma separated list of deal tracking numbers",
	                "Load Deal Dialog",
	                JOptionPane.PLAIN_MESSAGE,
	                null,
	                null,
	                "751172");
			try {
				String tokens[] = dealNumAsString.split(",");
				for (String token : tokens) {
					int dealNum = Integer.parseInt(token.trim());					
					try {
						tran = mappingTablePane.getSession().getTradingFactory().retrieveTransactionByDeal(dealNum);
						runtimeDataLabel.setText("Runtime Data Table for deal #" + dealNumAsString);
						trans.add(tran);
					} catch (Exception ex) {
						msg = "Deal # " + dealNumAsString + " not found.";					
					}
				}
			} catch (NumberFormatException ex) {
				abort = true;
			}
		} while (tran == null && ! abort);
		if (!abort) {
			mappingTablePane.getMainDialog().clearSimCache();
			mappingTablePane.getMainDialog().setTransaction(trans);
			mappingTablePane.runSimulation();
		}		
	}

	public EndurBackedTable getRuntimeTable() {
		return runtimeTable;
	}

	public JList<String> getSimulationName() {
		return simulationName;
	}
	
	public JRadioButton getHighlightMode() {
		return highlightMode;
	}
	
	public JRadioButton getFilterMode() {
		return filterMode;
	}

}
