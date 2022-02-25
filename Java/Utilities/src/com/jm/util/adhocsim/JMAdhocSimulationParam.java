package com.jm.util.adhocsim;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

@ScriptCategory({ EnumScriptCategory.Generic })
public class JMAdhocSimulationParam extends AbstractGenericScript {

	public Table execute(Context context, EnumScriptCategory category, ConstTable table) {

		buildFrame(context);

		return null;
	}

	private void buildFrame(Context context) {

		JFrame frame = new JFrame();
		JPanel panel = (JPanel) frame.getContentPane();

		JLabel SimDefLabel = new JLabel("Simulation Defination : ");
		JLabel savedQueryLabel = new JLabel("Saved Query : ");

		JComboBox<String> simDefComboBox = new JComboBox<String>();
		JComboBox<String> savedQueryComboBox = new JComboBox<String>();

		JButton button = new JButton("Run Simulation");

		panel.add(SimDefLabel);
		SimDefLabel.setBounds(70, 23, SimDefLabel.getPreferredSize().width, SimDefLabel.getPreferredSize().height);

		Table batchSim = retrieveBatchSim(context);
		populateBatchSim(batchSim, simDefComboBox);
		panel.add(simDefComboBox);
		simDefComboBox.setBounds(230, 20, simDefComboBox.getPreferredSize().width,
				simDefComboBox.getPreferredSize().height);

		panel.add(savedQueryLabel);
		savedQueryLabel.setBounds(70, 185, savedQueryLabel.getPreferredSize().width,
				savedQueryLabel.getPreferredSize().height);

		JList<String> savedQueryList = new JList<String>(savedQueryComboBox.getModel());
		savedQueryList.setFixedCellWidth(200);
		savedQueryList.setVisibleRowCount(20);
		savedQueryList.setFixedCellHeight(15);
		savedQueryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JScrollPane savedQueryPane = new JScrollPane(savedQueryList);
		panel.add(savedQueryPane);
		savedQueryPane.setBounds(230, 60, savedQueryPane.getPreferredSize().width,
				savedQueryPane.getPreferredSize().height);

		panel.add(button);
		button.setBounds(200, 400, button.getPreferredSize().width, button.getPreferredSize().height);

		simDefComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {

				String selectedDef = simDefComboBox.getSelectedItem().toString();
				Table savedQuery = retrieveSavedQuery(context, selectedDef);
				populateSavedQuery(savedQuery, savedQueryComboBox);
			}
		});

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (simDefComboBox.getSelectedItem().toString().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Select the Batch Sim Defination.");
					return;
				}

				if (savedQueryList == null || savedQueryList.getSelectedValuesList().size() == 0) {
					JOptionPane.showMessageDialog(null, "Select the Saved Queries.");
					return;
				}

				Table argt = context.getTableFactory().createTable();
				argt.addColumn("bsim_def", EnumColType.String);
				argt.addColumn("queries", EnumColType.Table);

				TableRow row = argt.addRow();
				argt.setString("bsim_def", row.getNumber(), simDefComboBox.getSelectedItem().toString());

				List<String> selectedQueries = savedQueryList.getSelectedValuesList();
				Table selectedQueriesTable = context.getTableFactory().createTable();
				selectedQueriesTable.addColumn("query_name", EnumColType.String);

				for (String queryName : selectedQueries) {
					row = selectedQueriesTable.addRow();
					selectedQueriesTable.setString("query_name", row.getNumber(), queryName);
				}
				
				argt.setTable("queries", 0, selectedQueriesTable);
				
				context.getControlFactory().runScript("com.jm.util.adhocsim.JMAdhocSimulationMain", argt);
				
				frame.dispose();
				argt.dispose();
				selectedQueriesTable.dispose();
			}
		});

		Border border = new LineBorder(Color.GRAY, 4, true);
		panel.setBorder(border);
		panel.setBackground(Color.lightGray);
		panel.setLayout(null);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(550, 500);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setFocusable(true);
		frame.setLocationRelativeTo(null);
	}

	private void populateSavedQuery(Table savedQuery, JComboBox<String> savedQueryComboBox) {
		savedQueryComboBox.removeAllItems();
		for (TableRow row : savedQuery.getRows()) {
			savedQueryComboBox.addItem(row.getString("query_name"));
		}
	}

	private void populateBatchSim(Table batchSim, JComboBox<String> comboBox) {
		comboBox.removeAllItems();
		comboBox.addItem("");
		for (TableRow row : batchSim.getRows()) {
			comboBox.addItem(row.getString("title"));
		}
	}

	private Table retrieveBatchSim(Context context) {
		
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT title ");
		sql.append(" FROM batch_sim");
		sql.append(" WHERE title NOT LIKE '%Backup%' ");
		
		Table batchSim = context.getIOFactory().runSQL(sql.toString());
		batchSim.sort("title");

		return batchSim;
	}

	private Table retrieveSavedQuery(Context context, String selectedDef) {
		
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT query_name ");
		sql.append(" FROM query_view qv ");
		sql.append(" JOIN batch_sim_defn bds ON qv.query_id = bds.query_db_id ");
		sql.append(" JOIN batch_sim bs ON bs.bsim_id = bds.bsim_id ");
		sql.append(" WHERE bs.title like '").append(selectedDef).append("'");
				
		Table savedQuery = context.getIOFactory().runSQL(sql.toString());
		savedQuery.sort("query_name");
		
		return savedQuery;
	}
}
