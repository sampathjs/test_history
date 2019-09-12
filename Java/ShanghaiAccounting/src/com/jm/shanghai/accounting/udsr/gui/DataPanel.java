package com.jm.shanghai.accounting.udsr.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

/*
 * History:
 * 2019-04-30	V1.0		jwaechter		- Initial Version
 */
/**
 * This class contains the dialog elements shown to the user. It has three elements:
 * <ul> 
 *   <li>
 *     A tree view on the left showing the accounting rules split according to the defined 
 *     grouping criteria
 *   </li>
 *   <li>
 *      A table on the upper right containing all rules matching the selection of the tree view 
 *   </li>
 *   <li> 
 *    A panel on the lower right containing a button to load a transaction and a table showing
 *    the content of the runtime table for all events of the loaded transaction
 *   </li> 
 * </ul>
 * 
 * @author jwaechter
 * @version 1.0
 */
public class DataPanel extends Panel {
	private final MappingTablePane mappingTablePane;
	
	private GridBagLayout layoutManager;
	private JTree ruleGroupingSelectionTree;
	private JScrollPane scrollPaneForRuleGroupingSelection;
	private RuleTable ruleTable;
	private JScrollPane scrollPaneForRuleTable;
	private TransactionPanel transactionPanel;
	private DefaultMutableTreeNode topNode;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4233741609579412157L;

	public DataPanel (final MappingTablePane mappingTablePane) {
		this.mappingTablePane = mappingTablePane;
		layoutManager = new GridBagLayout();
		setLayout(layoutManager);
		topNode = new DefaultMutableTreeNode("Accounting Rules");
		ruleGroupingSelectionTree = new JTree(topNode);
		scrollPaneForRuleGroupingSelection = new JScrollPane(ruleGroupingSelectionTree);
		scrollPaneForRuleGroupingSelection.setPreferredSize(new Dimension(300, 700));
		ruleTable = new RuleTable(mappingTablePane.getRuleTable(), null, null, mappingTablePane.getMappingTableConfig(), mappingTablePane.getMainDialog().getColLoader(), this);

		scrollPaneForRuleTable = new JScrollPane(ruleTable);
		JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPaneForRuleGroupingSelection, scrollPaneForRuleTable);

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets = new Insets(10/*top*/, 10 /*left*/, 10 /*bottom*/, 10 /* right */);
		gc.weightx = 1;
		gc.weighty = 1;		
		add(splitPane1, gc);	
		
		transactionPanel = new TransactionPanel(this, mappingTablePane);
		ruleTable.setApplyPredicates(new BooleanFunction() {
			@Override
			public boolean isTrue() {
				return transactionPanel.getHighlightMode().isSelected();
			}
		});
		
		gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.gridy = 1;
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets = new Insets(10/*top*/, 10 /*left*/, 10 /*bottom*/, 10 /* right */);
		gc.weightx = 1;
		gc.weighty = 0.3;		
		add(transactionPanel, gc);
	}

	public JTree getRuleGroupingSelectionTree() {
		return ruleGroupingSelectionTree;
	}

	public DefaultMutableTreeNode getTopNode() {
		return topNode;
	}
	
	public RuleTable getRuleTable() {
		return ruleTable;
	}

	public TransactionPanel getTransactionPanel() {
		return transactionPanel;
	}

	public MainDialog getMainDialog() {
		return mappingTablePane.getMainDialog();
	}

	public MappingTablePane getMappingTablePane() {
		return mappingTablePane;
	}
}
