/*
 * Parameter script for Task - Party Info Updater.
 * 
 *  This task can be used to update value of any party info field (of Picklist type like Form, GT Active etc) 
 *  for multiple external business units in one go. 
 *  Param fields - External BU (multi-select), Party Info Field (single select), Picklist Values (single select).								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.jm.util.partyinfo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.Display;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.EnumPartyStatus;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

@ScriptCategory({ EnumScriptCategory.Generic })
public class PartyInfoUpdaterParam extends AbstractGenericScript {
	
	private JList<String> extBUList;
	private JComboBox<String> partyInfoList;
	private JComboBox<String> fieldValueList;
    private JButton button;
    
	public Table execute(Context context, ConstTable table) {
		Table partyList = null;
		Table partyInfoFields = null;
		Table returnt = null;
		
		try {
			Logging.init(context, this.getClass(), "", "");
			
			Logging.info("Started PartyInfoUpdater parameter script");
			partyList = retrieveExtParties(context);
			Logging.info("Retrieved " + partyList.getRowCount() + " external parties");
			
			partyInfoFields = retrievePartyInfoFields(context);
			Logging.info("Retrieved " + partyInfoFields.getRowCount() + " party info fields");

			returnt = context.getTableFactory().createTable("returnt");
			final Display display = context.getDisplay();

			createGUI(context, display, partyList, partyInfoFields, returnt);

			Logging.info("Ended PartyInfoUpdater parameter script");

		} catch (Exception e) {
			Logging.error("Error in executing parameter script: " + e.getMessage(), e);
			throw e;
			
		} finally {
			Logging.close();
			if (partyList != null) {
				partyList.dispose();
			}
			if (partyInfoFields != null) {
				partyInfoFields.dispose();
			}
		}
		
		return returnt;
	}

	protected Table retrieveExtParties(Context context) {
		String sqlString = "SELECT p.short_name "
				+ " FROM party p "
				+ " INNER JOIN party_function pf ON (pf.party_id = p.party_id)" 
				+ " WHERE p.party_class = 1 "
				+ " AND p.party_status = " + EnumPartyStatus.Authorized.getValue() 
				+ " AND pf.function_type = 1" ;

		Table partyList = context.getIOFactory().runSQL(sqlString);
        partyList.sort("short_name");
		return partyList;
	}
	
	protected Table retrievePartyInfoFields(Context context) {
		String sqlString = "SELECT pit.type_name "
				+ " FROM party_info_types pit \n" 
				+ " WHERE pit.party_class = 1 AND int_ext = 1 AND pit.pick_list_id > 0" ;

		Table partyInfoFields = context.getIOFactory().runSQL(sqlString);
		partyInfoFields.sort("type_name");
		return partyInfoFields;
	}
	
	protected Table retrieveInfoFieldValues(Context context, String partyInfoField) {
		String sqlString = "SELECT cpl.table_name, cpl.column_name "
				+ " FROM party_info_types pit "
				+ " INNER JOIN client_pick_list cpl ON pit.pick_list_id = cpl.pick_list_id "
				+ " WHERE pit.type_name = '" + partyInfoField + "'" ;

		Table infoFieldValues = null;
		Table picklistTbl = null;
		try {
			picklistTbl = context.getIOFactory().runSQL(sqlString);
			String tableName = picklistTbl.getString(0, 0);
			String columnName = picklistTbl.getString(1, 0);
			
			sqlString = "SELECT " + columnName + " FROM " + tableName;
			Logging.info("Retrieving picklist values, executing SQL query: " + sqlString);
			infoFieldValues = context.getIOFactory().runSQL(sqlString);
			infoFieldValues.sort(columnName);
			
		} finally {
			if (picklistTbl != null) {
				picklistTbl.dispose();
			}
		}
		
		return infoFieldValues;
	}
	
	private void createGUI(Context context, final Display display, Table partyList, Table partyInfoFields, final Table returnt) {
        //Create and set up the window. 
        final JFrame frame = new JFrame("Party Info Fields Updater");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setPreferredSize(new Dimension(640, 600));

        JLabel label1 = new JLabel("External BU"); 
        JLabel label2 = new JLabel("PartyInfo Fields"); 
        JLabel label3 = new JLabel("Picklist Values");
        
        JComboBox<String> extBUComboBox = new JComboBox<String>();
        partyInfoList = new JComboBox<String>();
        fieldValueList = new JComboBox<String>();
        button = new JButton("Update");
        
        label3.setVisible(false);
        this.fieldValueList.setVisible(false);
        
        populatePartyInfoFields(partyInfoFields, partyInfoList);
        populatePartyLists(partyList, extBUComboBox);
        Logging.info("Populated party list & party info fields to GUI objects");
        
        this.extBUList = new JList<String>(extBUComboBox.getModel());
        this.extBUList.setFixedCellWidth(500);
		this.extBUList.setVisibleRowCount(20);
		this.extBUList.setFixedCellHeight(15);
		this.extBUList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
		addListeners(context, display, returnt, frame, this.extBUList, partyInfoList, label3, fieldValueList, button);
		
		addComponents(label1, label2, label3, this.extBUList, partyInfoList, fieldValueList, button, frame.getContentPane());
        frame.pack();
        frame.setVisible(true);
        display.block();
	}
	
	private void populatePartyLists(Table partyList, final JComboBox<String> extBUList) {
		extBUList.removeAllItems();
        extBUList.addItem("");
		for (TableRow row : partyList.getRows()){			
			String party = row.getString("short_name");
			extBUList.addItem(party);
		}
	}
	
	private void populatePartyInfoFields(Table partyInfoFields, final JComboBox<String> partyInfoList) {
		partyInfoList.removeAllItems();
		partyInfoList.addItem("");
		for (TableRow row : partyInfoFields.getRows()){			
			String party = row.getString("type_name");
			partyInfoList.addItem(party);
		}
	}
	
	private void populateInfoFieldValues(ConstTable infoFieldValues, final JComboBox<String> fieldValueList) {
		fieldValueList.removeAllItems();
		fieldValueList.addItem("");
		for (int row = 0; row < infoFieldValues.getRowCount(); row++) {
			Object[] rowObj = infoFieldValues.getRowValues(row);
			String party = (String) rowObj[0];//("type_name");
			fieldValueList.addItem(party);
		}
	}
	
	private void addListeners(final Context context, final Display display, final Table returnt, final JFrame frame,
			final JList<String> extBUList, final JComboBox<String> partyInfoList, final JLabel fieldValLabel,
			final JComboBox<String> fieldValueList, JButton button) {
		
		partyInfoList.addItemListener(new ItemListener() {
		    @Override
		    public void itemStateChanged(ItemEvent event) {
		    	if (event.getStateChange() != ItemEvent.SELECTED) {
		    		return;
		    	}
		    	
		    	fieldValLabel.setVisible(true);
		    	fieldValueList.setVisible(true);
		    	
		    	String partyInfo = partyInfoList.getSelectedItem().toString();
		    	Logging.info("Selected party info field value: " + partyInfo);
		    	ConstTable infoFieldValues = retrieveInfoFieldValues(context, partyInfo);
		    	populateInfoFieldValues(infoFieldValues, fieldValueList);
		    	Logging.info("Populated picklist values to GUI object");
		    }
		});
						
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String message = "";
				String fieldValue = "";
           	 	List<String> buList = extBUList.getSelectedValuesList();
           	 	String partyInfo = partyInfoList.getSelectedItem().toString();
           	 	
           	 	if (fieldValueList.isVisible()) {
           	 		fieldValue = fieldValueList.getSelectedItem().toString();
           	 	}
        	 	
           	 	if (buList == null || buList.size() == 0) {
					message = "No external BUs are selected. Please select atleast one external BU to proceed.";
					Logging.info(message);
					JOptionPane.showMessageDialog(null, message);
					return;
				}
           	 
				if (partyInfo == null || "".equals(partyInfo)) {
					message = "No Party Info field is selected. Please select a party info field to procced.";
					Logging.info(message);
					JOptionPane.showMessageDialog(null, message);
					return;
				}
           	 	
				if (fieldValue == null || "".equals(fieldValue)) {
					message = "No field value is selected. Please select a field value to procced.";
					Logging.info(message);
					JOptionPane.showMessageDialog(null, message);
					return;
				}
				
				returnt.addColumn("external_bunit", EnumColType.String);
           	 	returnt.addColumn("party_info", EnumColType.String);
           	 	returnt.addColumn("field_value", EnumColType.String);
           		
           	 	int row = -1;
				for (String extBUName : buList) {
					row++;
					returnt.addRow();
					
					returnt.setString("external_bunit", row, extBUName);
					returnt.setString("party_info", row, partyInfo);
					returnt.setString("field_value", row, fieldValue);
				}
				
				Logging.info("Added " + buList.size() + " rows to returnt");
           	 	frame.dispose();
           	 	display.unblock();
			}
		});
		
        // Add window event listener that will unblock the display after this window closes.
        frame.addWindowListener(new WindowAdapter() {
             public void windowClosing(WindowEvent e) {
                 display.unblock();
             }
        });
	}
	
	private void addComponents(JLabel label1, JLabel label2, JLabel label3, final JList<String> extBUList,
			final JComboBox<String> partyInfoList, final JComboBox<String> fieldValue, JButton button, Container pane) {
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		
		JPanel panel1 = new JPanel();
		panel1.setAlignmentX(Component.LEFT_ALIGNMENT );
		panel1.add(label1);
		panel1.add(new JScrollPane(this.extBUList));
		pane.add(panel1);

		JPanel panel2 = new JPanel();
		panel2.setAlignmentX(Component.LEFT_ALIGNMENT );
		panel2.add(label2);
		panel2.add(partyInfoList);
		pane.add(panel2);
		
		JPanel panel3 = new JPanel();
		panel3.setAlignmentX(Component.LEFT_ALIGNMENT );
		panel3.add(label3);
		panel3.add(fieldValue);
		pane.add(panel3);
		
		JPanel panel4 = new JPanel();
		panel4.setAlignmentX(Component.LEFT_ALIGNMENT );
		panel4.add(button);
		pane.add(panel4);
	}
}
