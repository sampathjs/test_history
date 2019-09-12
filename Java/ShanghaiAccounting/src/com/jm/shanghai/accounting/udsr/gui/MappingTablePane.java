package com.jm.shanghai.accounting.udsr.gui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.jm.shanghai.accounting.udsr.AbstractShanghaiAccountingUdsr;
import com.jm.shanghai.accounting.udsr.ColNameProvider;
import com.jm.shanghai.accounting.udsr.RuntimeAuditingData;
import com.jm.shanghai.accounting.udsr.control.MappingTableFilterApplicator;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableColumnConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableRowConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.ColumnSemantics;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.EnumQueryType;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

public class MappingTablePane extends JPanel implements TreeSelectionListener, ActionListener{
	private final Session session;
	private final MainDialog mainDialog;
	private final DataPanel dataPanel;
	private final Table ruleTable;
	private final List<String> colNameGrouping;
	private List<MappingTableRowConfiguration> matchingRows=null;
	private Map<DefaultMutableTreeNode, Table> nodeToTableMap;
	private final RetrievalConfigurationColDescription mappingTableConfig;
	
	public MappingTablePane (
			final MainDialog mainDialog,
			final Session session,
			final RetrievalConfigurationColDescription mappingTableConfig) {
		this.session = session;
		this.mainDialog = mainDialog;
		this.mappingTableConfig = mappingTableConfig;
		ruleTable = session.getIOFactory().getUserTable(mappingTableConfig.getMappingTableName()).retrieveTable();
		if (mappingTableConfig.getColName().equals(mainDialog.getColLoader().getMappingTable().getColName())) {
			colNameGrouping = new ArrayList<>(Arrays.asList("int_bu", "mode", "ins_type", "buy_sell", "from_currency"));			
		} else if (mappingTableConfig.getColName().equals(mainDialog.getColLoader().getTaxTable().getColName())) {
			colNameGrouping = new ArrayList<>(Arrays.asList("int_bu", "tax_type", "tax_subtype"));
		} else {
			colNameGrouping = new ArrayList<>(Arrays.asList(ruleTable.getColumnName(0)));
		}
		AbstractShanghaiAccountingUdsr.generateUniqueRowIdForTable(ruleTable);
		dataPanel = new DataPanel(this);
		dataPanel.getRuleGroupingSelectionTree().addTreeSelectionListener(this);
		recreateTree(false);
		this.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets = new Insets(10/*top*/, 10 /*left*/, 10 /*bottom*/, 10 /* right */);
		gc.weightx = 1;
		gc.weighty = 0.95;
		
		dataPanel.setBounds(10, 10, mainDialog.getSize().width-20, mainDialog.getSize().height-20);

		add(dataPanel, gc);
		{ 
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

			gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.FIRST_LINE_START;
			gc.gridx = 0;
			gc.gridy = 1;
			gc.fill = GridBagConstraints.BOTH;
			gc.gridwidth = 1;
			gc.gridheight = 1;
			gc.insets = new Insets(10/*top*/, 10 /*left*/, 10 /*bottom*/, 10 /* right */);
			gc.weightx = 1;
			gc.weighty = 0.05;

			add(buttonPane, gc);
			{ 
				JButton saveButton = new JButton("Save");
				saveButton.setActionCommand("Save");
				saveButton.addActionListener(this);
				buttonPane.add(saveButton);
			}
			{
				JButton abortButton = new JButton("Abort");
				abortButton.addActionListener(this);
				abortButton.setActionCommand("Abort");
				buttonPane.add(abortButton);
			}
		}

		
	}

	public void recreateTree(boolean stayOnSameTreeElement) {
		TreePath oldPath = dataPanel.getRuleGroupingSelectionTree().getSelectionPath();;
		dataPanel.getRuleGroupingSelectionTree().clearSelection();
		dataPanel.getTopNode().removeAllChildren();
		nodeToTableMap = new HashMap<>();
		nodeToTableMap.put(dataPanel.getTopNode(), ruleTable);
		Set<String> uniqueValues = new HashSet<>();
		int firstColId = ruleTable.getColumnId(colNameGrouping.get(0));
		for (int row=ruleTable.getRowCount()-1; row >= 0; row--) {
			uniqueValues.add(ruleTable.getDisplayString(firstColId, row));
		}
		List<String> uniqueValuesSorted = new ArrayList<>(uniqueValues);
		Collections.sort(uniqueValuesSorted);
		for (String value : uniqueValuesSorted) {
			DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(String.format("%s (%s)", value, colNameGrouping.get(0)));
			dataPanel.getTopNode().add(defaultMutableTreeNode);
			List<String> newSelectedValuesList = Arrays.asList(value);
			collectTreeData(ruleTable.cloneData(), defaultMutableTreeNode, newSelectedValuesList);
		}
		((DefaultTreeModel)dataPanel.getRuleGroupingSelectionTree().getModel()).reload();
		dataPanel.getRuleGroupingSelectionTree().setSelectionRow(0);
		if (stayOnSameTreeElement) {
			DefaultMutableTreeNode newNode = dataPanel.getTopNode();
			for (int i=1;i <= oldPath.getPathCount(); i++) {
				Object pathElement = oldPath.getPathComponent(i);
				boolean found=false;
				for (int childIndex = 0; childIndex < newNode.getChildCount(); childIndex++) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) newNode.getChildAt(childIndex);
					if (pathElement.toString().equals(childNode.toString())) {
						newNode = childNode;
						found=true;
						break;
					}
				}
				dataPanel.getRuleGroupingSelectionTree().setSelectionPath(new TreePath(newNode.getPath()));
			}
		}
	}
	
	private void collectTreeData (Table currMatchingRules, DefaultMutableTreeNode currentNode, List<String> selectedValues) {
		Table filteredRuleTable = currMatchingRules.cloneData();
		int currColId = filteredRuleTable.getColumnId(colNameGrouping.get(selectedValues.size()-1));
		// remove all rows that don't match
		for (int row=filteredRuleTable.getRowCount()-1; row >= 0; row--) {
			if (!filteredRuleTable.getDisplayString(currColId, row).equals(selectedValues.get(selectedValues.size()-1))) {
				filteredRuleTable.removeRow(row);
			}
		}
		nodeToTableMap.put(currentNode, filteredRuleTable);
		if (selectedValues.size() < colNameGrouping.size()) {
			int nextColId = filteredRuleTable.getColumnId(colNameGrouping.get(selectedValues.size()));
			Set<String> uniqueValues = new HashSet<>();
			for (int row=filteredRuleTable.getRowCount()-1; row >= 0; row--) {
				uniqueValues.add(filteredRuleTable.getDisplayString(nextColId, row));
			}
			List<String> uniqueValuesSorted = new ArrayList<>(uniqueValues);
			Collections.sort(uniqueValuesSorted);
			for (String value : uniqueValuesSorted) {
				DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(String.format("%s (%s)", value, colNameGrouping.get(selectedValues.size())));
				currentNode.add(defaultMutableTreeNode);
				List<String> newSelectedValuesList = new ArrayList<>(selectedValues);
				newSelectedValuesList.add(value);
				collectTreeData(filteredRuleTable, defaultMutableTreeNode, newSelectedValuesList);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if (action.equals("Save")) {
//			mainDialog.dispose();
			session.getIOFactory().getUserTable(mappingTableConfig.getMappingTableName()).clearRows();
			Table insertionTable = ruleTable.cloneData();
			insertionTable.removeColumn("row_id");
			session.getIOFactory().getUserTable(mappingTableConfig.getMappingTableName()).insertRows(insertionTable);
		}
		if (action.equals("Abort")) {
			mainDialog.setFinished(true);
			//			if (display != null) {
			//				display.unblock();				
			//			}
			mainDialog.dispose();
		}
	}
	
	public Table getRuleTable() {
		return ruleTable;
	}

	public Session getSession() {
		return session;
	}

	public Table getRuntimeTable() {
		Table runtimeTable = session.getTableFactory().createTable("Runtime Table");
//		runtimeTable.addColumn("dummy", EnumColType.String);
//		runtimeTable.addColumn("dummy2", EnumColType.String);
//		runtimeTable.addRow();
//		runtimeTable.setValue("dummy", 0, "value");
//		runtimeTable.setValue("dummy2", 0, "X");
		return runtimeTable;
	}
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		updateRuleTable();
	}

	public void updateRuleTable() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
				dataPanel.getRuleGroupingSelectionTree().getLastSelectedPathComponent();
		if (node == null) {
			return;
		}
		Table filteredEndurTable = nodeToTableMap.get(node);
		if (matchingRows != null && dataPanel.getTransactionPanel().getFilterMode().isSelected()) {
			Table mappedFilteredEndurTable = filteredEndurTable.cloneData();
			for (int row=mappedFilteredEndurTable.getRowCount()-1; row >= 0; row--) {
				boolean found = false;	
				for (MappingTableRowConfiguration rowConfiguration : matchingRows) {
					if (mappedFilteredEndurTable.getInt(AbstractShanghaiAccountingUdsr.ROW_ID, row) == rowConfiguration.getUniqueRowId()) {	
						found = true;
						break;
					}
				}
				if (!found) {
					mappedFilteredEndurTable.removeRow(row);
				}
			}
			dataPanel.getRuleTable().changeEndurTable (mappedFilteredEndurTable);
		} else {
			dataPanel.getRuleTable().changeEndurTable(filteredEndurTable);			
		}
	}

	/**
	 * What is done here? To access the table containing the retrieved data for our 
	 * single transaction, the UDSR is passing a duplicate via session.clientData.
	 * To ensure the client data object is changed in OUR session, we will
	 * have to run the simulation locally. If the simulation is run locally,
	 * we will have to execute it within the main Endur thread, not within this
	 * GUI thread. To do so, we are passing on the sim details to be executed
	 * to the main thread and wait.
	 * @param tran
	 */
	public void runSimulation() {
		Table beforeMapping = null;
		String simName = dataPanel.getTransactionPanel().getSimulationName().getSelectedValue();
		if (mainDialog.getSimNameToSimResult().containsKey(simName + mappingTableConfig.getColName())) {
			beforeMapping = mainDialog.getSimNameToSimResult().get(simName);
		} else {
			SimulationFactory sf = session.getSimulationFactory();
			try (QueryResult qr = session.getIOFactory().createQueryResult(EnumQueryType.Transaction);
					ResultType rt = sf.getResultType(simName);
					Simulation sim = sf.createSimulation("temp");
					Scenario scen = sf.createScenario("temp")) {
				qr.add(mainDialog.getTransaction().getTransactionId());
				scen.getResultTypes().add(rt);
				sim.addScenario(scen);
				mainDialog.getMainThread().setQueryToRun(qr);
				mainDialog.getMainThread().setSimToRun(sim);
				while (mainDialog.getMainThread().getQueryToRun() != null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException (e);
					}
				}
				mainDialog.setRuntimeAuditingData((RuntimeAuditingData) session.getClientData());
				for (RetrievalConfigurationColDescription rccd : mainDialog.getRetrievalConfigurationColDescriptions()) {
					if (rccd.getUsageType() != ColumnSemantics.MAPPER_COLUMN) {
						continue;
					}
					beforeMapping = mainDialog.getRuntimeAuditingData().getMappingAuditingData().get(rccd).getRuntimeTableBeforeMapping();
					mainDialog.getSimNameToSimResult().put(simName + rccd.getColName(), beforeMapping);
				}
			}
		}
		mainDialog.notifySimResultsChanged();
	}

	/**
	 * 
	 * @param rowId the row id of the row from the event data 
	 */
	public void runMapping (int rowId) {
		if (rowId < 0) {
			dataPanel.getRuleTable().setRuntimeTable(null);
			dataPanel.getRuleTable().updatePredicates(null);
//			dataPanel.getRuleTable().updateRetrievalConfigs(null);
			matchingRows = null;
			return;
		}
		Table beforeMapping = null;
		String simName = dataPanel.getTransactionPanel().getSimulationName().getSelectedValue();
		beforeMapping = mainDialog.getSimNameToSimResult().get(simName + mappingTableConfig.getColName());

		
		Table mappingTable = null;
		int[] rowIds = new int[1];
		rowIds[0] = rowId;
		mappingTable = AbstractShanghaiAccountingUdsr.retrieveMappingTable(session, mappingTableConfig.getMappingTableName());
		dataPanel.getRuleTable().updateRetrievalConfigs(mainDialog.getRuntimeAuditingData().getRetrievalConfig());
		Table runtimeTable = beforeMapping.cloneData(rowIds);
		mappingTable = AbstractShanghaiAccountingUdsr.retrieveMappingTable(session, mappingTableConfig.getMappingTableName());
		ColNameProvider colNameProvider = mainDialog.getRuntimeAuditingData().getMappingAuditingData().get(mappingTableConfig).getColNameProvider();

		Map<String, MappingTableColumnConfiguration> mappingTableColConfig = 
				AbstractShanghaiAccountingUdsr.confirmMappingTableStructure (this.mappingTableConfig.getMappingTableName(), colNameProvider, 
						mappingTable, runtimeTable, mainDialog.getRuntimeAuditingData().getRetrievalConfig());
		AbstractShanghaiAccountingUdsr.generateUniqueRowIdForTable(mappingTable);
		Map<String, RetrievalConfiguration> rcByMappingColName = new HashMap<>(mainDialog.getRuntimeAuditingData().getRetrievalConfig().size()*3);
		for (RetrievalConfiguration rc : mainDialog.getRuntimeAuditingData().getRetrievalConfig()) {
			if (colNameProvider.getColName(rc) != null && colNameProvider.getColName(rc).trim().length() > 0) {
				rcByMappingColName.put(colNameProvider.getColName(rc), rc);
			}
		}
		List<MappingTableRowConfiguration> mappingRows = 
				AbstractShanghaiAccountingUdsr.parseMappingTable (colNameProvider, mappingTable, runtimeTable,
						mainDialog.getRuntimeAuditingData().getRetrievalConfig(), mappingTableColConfig);
		dataPanel.getRuleTable().updatePredicates(mappingRows);
		MappingTableFilterApplicator applicator = 
				new MappingTableFilterApplicator (rcByMappingColName, mappingTableColConfig, mainDialog.getColLoader());
		mappingTable.sort(AbstractShanghaiAccountingUdsr.ROW_ID);

		TableRow runtimeTableRow = runtimeTable.getRow(0);
		matchingRows = applicator.apply(runtimeTable, runtimeTableRow);
		dataPanel.getRuleTable().setRuntimeTable(runtimeTable);
	}

	public List<String> getColNameGrouping() {
		return colNameGrouping;
	}	
	
	public MainDialog getMainDialog () {
		return mainDialog;
	}

	public RetrievalConfigurationColDescription getMappingTableConfig() {
		return mappingTableConfig;
	}

	public void notifySimResultChange() {
		Table beforeMapping = null;
		String simName = dataPanel.getTransactionPanel().getSimulationName().getSelectedValue();
		beforeMapping = mainDialog.getSimNameToSimResult().get(simName + mappingTableConfig.getColName());
		dataPanel.getTransactionPanel().getRuntimeTable().changeEndurTable(beforeMapping);
		dataPanel.getTransactionPanel().getRuntimeTable().invalidate();
		dataPanel.getTransactionPanel().getRuntimeTable().repaint();		
	}

	public void notifySimChanged(int selectedIndex) {
		dataPanel.getTransactionPanel().getSimulationName().setSelectedIndex(selectedIndex);
	}

	public void notifyRuntimeAuditingTableChanged() {
		dataPanel.getRuleTable().updateRetrievalConfigs(mainDialog.getRuntimeAuditingData().getRetrievalConfig());
	}
}
