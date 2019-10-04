package com.olf.jm.metalstatements;

import static com.olf.jm.metalstatements.EOMMetalStatementsShared.SYMBOLICDATE_1LOM;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.Display;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumPartyStatus;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-MM-DD	<unknown>	V1.0	- Initial Version
 * 2016-07-13 	jwaechter	V1.1	- Now sorting party list
 *                                  - Check if the metal statements have
 *                                    been generated is now done on account 
 *                                    level and not on party level.
 *                                  - if no external BU is specified it blocks
 *                                    in case there is already a single generated
 *                                    metal statement
 * 2016-11-09	jwaechter	V1.2    - added error check for legal entity of
 *                                    external business unit
 * 
 */


/**
 * Parameter plugin for {@link EOMMetalStatements}.
 * Shows internal and external parties to select from to the user. Blocks input if 
 * the metal statements have been run already.
 * @author <unknown>
 * @version 1.2
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class EOMMetalStatementsParam extends AbstractGenericScript {
    private JComboBox<String> intBUList;
    private JComboBox<String> extBUList;
    private JButton button;
	private ConstRepository constRep = null;
	private static Map<String, Set<String>> allowedLocationsForInternalBu = null;
    
	@Override
	public Table execute(Context context, ConstTable table) {
		int secondsPastMidnight = 0;
		int timeTaken = 0;
		
		try {
			constRep = new ConstRepository(EOMMetalStatementsShared.CONTEXT, EOMMetalStatementsShared.SUBCONTEXT);
			String abOutDir = context.getSystemSetting("AB_OUTDIR") + "\\error_logs";
			EOMMetalStatementsShared.init (constRep, abOutDir);
			secondsPastMidnight = Util.timeGetServerTime();
			PluginLog.info("Started EOM Metal Statements Param");
			
			allowedLocationsForInternalBu = EOMMetalStatementsShared.getAllowedLocationsForInternalBu(context);
			
		} catch (OException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        Table partyList = createPartyList(context);
   	 	final Table returnt = context.getTableFactory().createTable("returnt");
		final Display display = context.getDisplay();
        createGUI(context, display, partyList, returnt);
		partyList.dispose();

		try {
			timeTaken = Util.timeGetServerTime() - secondsPastMidnight ;
		} catch (OException e) {
			timeTaken = secondsPastMidnight;
		}
		
		PluginLog.info("Ended EOM Metal Statements Param " + EOMMetalStatementsShared.getTimeTakenDisplay(timeTaken));
        return returnt;
	}

	protected Table createPartyList(Context context) {
		String sqlString = "SELECT p.* FROM party p INNER JOIN party_function pf ON (pf.party_id = p.party_id)\n" +
				"  WHERE p.party_class = 1 AND p.party_status = " + EnumPartyStatus.Authorized.getValue() + "\n" +
				"  AND pf.function_type = 1" ;

		Table partyList = context.getIOFactory().runSQL(sqlString);
        partyList.sort("short_name");
		return partyList;
	}

	private void createGUI(Context context, final Display display, Table partyList, final Table returnt) {
        //Create and set up the window. 
        final JFrame frame = new JFrame("Metal Statement Reports");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 220)); 

        JLabel label1 = new JLabel("Internal Business Unit"); 
        JLabel label2 = new JLabel("External Business Unit"); 
        intBUList = new JComboBox<String>();
        extBUList = new JComboBox<String>();
        button = new JButton("Process");
        
		addListeners(context, display, returnt, frame, intBUList, extBUList, button, allowedLocationsForInternalBu);
		populatePartyLists(partyList, intBUList, extBUList);
		addComponenets(label1, label2, intBUList, extBUList, button, frame.getContentPane());
        frame.pack();
        frame.setVisible(true);

        display.block();
	}

	private void populatePartyLists(Table partyList, final JComboBox<String> intBUList, final JComboBox<String> extBUList) {
		intBUList.removeAllItems();
		extBUList.removeAllItems();
        extBUList.addItem("");
		for (TableRow row : partyList.getRows()){			
			String party = row.getString("short_name");
			if (row.getInt("int_ext") == 1){
				extBUList.addItem(party);
			} 
		}
		for (TableRow row : partyList.getRows()){			
			String party = row.getString("short_name");
			if (row.getInt("int_ext") == 0){
				intBUList.addItem(party);
			} 
		}

		//partyList.dispose();
	}
	
	private void populateExtBUList(Set<String> parties, final JComboBox<String> extBUList) {
		extBUList.removeAllItems();
        extBUList.addItem("");
		for (String party : parties){			
			extBUList.addItem(party);
		}
		if (parties.size() == 0) {
			extBUList.addItem("All Accounts have Metal Statements Executed for the Current Period. See User_JM_Monthly_Metal_Statements.");			
		}
	}


	private void addListeners(final Context context, final Display display, final Table returnt, final JFrame frame, final JComboBox<String> intBUList, 
			final JComboBox<String> extBUList, JButton button, final Map<String, Set<String>> allowedLocationsForInternalBu) {
		    intBUList.addItemListener(new ItemListener() {
		    @Override
		    public void itemStateChanged(ItemEvent event) {
		    	Table parties = createPartyList (context);
		    	extBUList.removeAllItems();
		    	parties.sort("short_name");
				for (TableRow row : parties.getRows()){			
					String party = row.getString("short_name");
					if (row.getInt("int_ext") == 0){
					} else {
						extBUList.addItem(party);
					}
				}

		    	if (event.getStateChange() != ItemEvent.SELECTED) {
		    		return;
		    	}
				Set<String> unprocessedBUs = new TreeSet<>();
				String intBUName = (String)event.getItem();
				Date eomDate = context.getCalendarFactory().createSymbolicDate(SYMBOLICDATE_1LOM).evaluate();
				StaticDataFactory sdf = context.getStaticDataFactory();
				int intBU = sdf.getId(EnumReferenceTable.Party, intBUName);
				String date = new SimpleDateFormat("dd-MMM-yyyy").format(eomDate);

				String sqlCountMetalStatement = "\nSELECT * "
					     + "\nFROM USER_jm_monthly_metal_statement "
					     + "\nWHERE metal_statement_production_date = '" + date + "' AND "
					     + "    internal_bunit = " + intBU
					     ;

				Table userTableContent = context.getIOFactory().runSQL(sqlCountMetalStatement);
				Table usedAccounts = EOMMetalStatementsShared.getUsedAccounts(context);
				// Changes related to Problem-1925
				try{
				HashMap<String, Integer> refAccountHolder = EOMMetalStatementsShared.refDataAccountHolder(context);
				refAccountHolder = EOMMetalStatementsShared.filterRefAccountHolderMap(usedAccounts, refAccountHolder);
				usedAccounts = EOMMetalStatementsShared.enrichAccountData(usedAccounts, refAccountHolder);
				}
				catch(OException e)
				{
				PluginLog.error("Accounts which have single deal with BU other than holder might have missed");	
				}
				Table accountsForHolder = EOMMetalStatementsShared.getAccountsForHolder(usedAccounts, intBU);
			
				for (int i=0; i < extBUList.getItemCount(); i++) {
					String curExtBUName = extBUList.getItemAt(i);
					if (curExtBUName.isEmpty()) {
						continue;
					}
					int extBU = sdf.getId(EnumReferenceTable.Party, curExtBUName);
					BusinessUnit bu = (BusinessUnit) sdf.getReferenceObject(EnumReferenceObject.BusinessUnit, curExtBUName);
					if (!EOMMetalStatementsShared.hasDefaultAuthorizedLegalEntity(context, bu)) {
						PluginLog.warn("Business Unit '" + bu.getName() 
								+	"' does not have a default legal entity assigned" 
								+   " or the default LE is not authorized. Skipping it.");
						continue;
					}
					int extLE = bu.getDefaultLegalEntity().getId();
					Table accountsForBU = EOMMetalStatementsShared.removeAccountsForWrongLocations(context, intBU, extBU, accountsForHolder, allowedLocationsForInternalBu);
					accountsForBU.addColumn("account_id_processed", EnumColType.Int);
					accountsForBU.select(userTableContent, "account_id->account_id_processed", 
							"[In.account_id] == [Out.account_id] AND [In.external_lentity] == " + extLE);
					for (int row=accountsForBU.getRowCount()-1; row >= 0; row--) {
						int accountIdProcessed = accountsForBU.getInt("account_id_processed", row);
						if (accountIdProcessed == 0) {
							unprocessedBUs.add(curExtBUName);							
						}
					}
					accountsForBU.dispose();
				}
				populateExtBUList(unprocessedBUs, extBUList);
			}
		});
						
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
           	 	String intBUName = intBUList.getSelectedItem().toString();
           	 	String extBUName = extBUList.getSelectedItem().toString();
           	 	if (alreadyRun(context, intBUName, extBUName)){
           	 		String message = "You've alreay run EOM report for External Business Unit: " + extBUName
           	 				+ " or the legal entity for the selected external business unit is"
           	 				+ " either not set or not authorized";
           	 		JOptionPane.showMessageDialog(null, message);
           	 		return;
           	 	}
           	 	returnt.addColumn("internal_bunit", EnumColType.String);
           	 	returnt.addColumn("external_bunit", EnumColType.String);
           	 	returnt.addRow();
				returnt.setString("internal_bunit", 0, intBUName);
				returnt.setString("external_bunit", 0, extBUName);
           	 	frame.dispose();
           	 	display.unblock();
			}

			private boolean alreadyRun(Context context, String intBUName, String extBUName) {
				Date eomDate = context.getCalendarFactory().createSymbolicDate(SYMBOLICDATE_1LOM).evaluate();
				StaticDataFactory sdf = context.getStaticDataFactory();
				int intBU = sdf.getId(EnumReferenceTable.Party, intBUName);
				String date = new SimpleDateFormat("dd-MMM-yyyy").format(eomDate);
				
				String sqlCountMetalStatement = "\nSELECT * "
					     + "\nFROM USER_jm_monthly_metal_statement "
					     + "\nWHERE metal_statement_production_date = '" + date + "' AND "
					     + "    internal_bunit = " + intBU
					     ;
				if (!extBUName.isEmpty()) {
					BusinessUnit bu = (BusinessUnit) sdf.getReferenceObject(EnumReferenceObject.BusinessUnit, extBUName);
					if (!EOMMetalStatementsShared.hasDefaultAuthorizedLegalEntity(context, bu)) {
						PluginLog.warn("Business Unit '" + bu.getName() 
							+	"' does not have a default legal entity assigned" 
							+   " or the default LE is not authorized");
						//return true to skip processing of this business unit
						bu.dispose();
						return true;
					} else {
						int extLE = bu.getDefaultLegalEntity().getId();						
						bu.dispose();
						sqlCountMetalStatement += " AND external_lentity = " + extLE;
					}
				}
				Table userTableContent = context.getIOFactory().runSQL(sqlCountMetalStatement);
				userTableContent.addColumn("account_id_valid", EnumColType.Int);
				Table usedAccounts = EOMMetalStatementsShared.getUsedAccounts(context);
				Table accountsForHolder = EOMMetalStatementsShared.getAccountsForHolder(usedAccounts, intBU);
				// Changes related to Problem 1925
				
				try {
					HashMap<String, Integer> refAccountHolder = EOMMetalStatementsShared.refDataAccountHolder(context);
					refAccountHolder=EOMMetalStatementsShared.filterRefAccountHolderMap(usedAccounts,refAccountHolder);
					usedAccounts=EOMMetalStatementsShared.enrichAccountData(usedAccounts,refAccountHolder);
				} catch (OException e) {
					PluginLog.error("Accounts which have single deal with BU other than holder might have missed");	
				}
				
			
				int countAccountsToProcess=0;
				if (!extBUName.isEmpty()) {
					int extBU = sdf.getId(EnumReferenceTable.Party, extBUName);
					Table accountsForBU = EOMMetalStatementsShared.removeAccountsForWrongLocations(context, intBU, extBU, accountsForHolder, allowedLocationsForInternalBu);
					countAccountsToProcess =  accountsForBU.getRowCount();
					userTableContent.select(accountsForBU, "account_id->account_id_present", "[In.account_id] == [Out.account_id]");
					accountsForBU.dispose();
				} else {
					for (int i=0; i < extBUList.getItemCount(); i++) {
						String curExtBUName = extBUList.getItemAt(i);
						int extBU = sdf.getId(EnumReferenceTable.Party, curExtBUName);
						
						Table accountsForBU = EOMMetalStatementsShared.removeAccountsForWrongLocations(context, intBU, extBU, accountsForHolder, allowedLocationsForInternalBu);
						countAccountsToProcess +=  accountsForBU.getRowCount();
						userTableContent.select(accountsForBU, "account_id->account_id_present", 
								"[In.account_id] == [Out.account_id] AND [In.party_id] == " + extBU);
						accountsForBU.dispose();
					}
				}
				for (int row=userTableContent.getRowCount()-1; row >= 0; row--) {
					int accountIdPresent = userTableContent.getInt("account_id_present", row);
					if (accountIdPresent == 0) {
						userTableContent.removeRow(row);
					}
				}
				int countMetalStatements = userTableContent.getRowCount();
				if (countMetalStatements < countAccountsToProcess) {
					userTableContent.dispose();
					return false;
				}
				if (countMetalStatements == 0 && countAccountsToProcess == 0) {
					userTableContent.dispose();
					return false;
				}
				return true;
			}
		});
		
        // Add window event listener that will unblock the display after this window closes.
        frame.addWindowListener(new WindowAdapter() {
             public void windowClosing(WindowEvent e) {
                 display.unblock();
             }
        });
	}

	private void addComponenets(JLabel label1, JLabel label2, final JComboBox<String> intBUList, 
			final JComboBox<String> extBUList, JButton button,  Container pane) {
		GroupLayout layout = new GroupLayout(pane);
        pane.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        	.addComponent(label1)
        	.addComponent(intBUList)
        	.addComponent(label2)
        	.addComponent(extBUList)
        	.addComponent(button)
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addComponent(label1)
        	.addComponent(intBUList)
        	.addComponent(label2)
        	.addComponent(extBUList)
        	.addComponent(button)
        );
	}
}
