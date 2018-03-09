package com.olf.jm.interfaces.lims.util.app;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.ConstRepItem;
import com.olf.jm.interfaces.lims.model.MetalProductTestTableCols;
import com.olf.jm.interfaces.lims.model.RelevantUserTables;
import com.olf.jm.interfaces.lims.model.UserTableColumn;
import com.olf.jm.interfaces.lims.persistence.ConnectionExternal;
import com.olf.jm.interfaces.lims.util.model.OperationMode;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.misc.TableUtilities;

/*
 * History: 
 * 2015-02-25 - V1.0 - jwaechter - initial version
 * 2015-03-05 - V1.1 - jwaechter - added test data and processing of test data
 *                                 for NTT staging table.
 */

/**
 * This class is responsible to create all necessary user tables for the
 * LIM interface. 
 * This is the main plugin. Refer to details about
 * user input in
 *  {@link LimUtilParam}. 
 * @author jwaechter
 * @version 1.0
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class LimUtil implements IScript
{
    private static final String USER_CONST_REPOSITORY = "USER_const_repository";

	public void execute(IContainerContext context) throws OException
    {
        try {
        	Table argt = context.getArgumentsTable();
        	if (checkArgumentsTable (argt)) {
        		List<OperationMode> modes = getOperationModes(argt);
        		List<RelevantUserTables> tables = getUserTables(argt);
        		if (modes.contains(OperationMode.LOCAL_DROP)) {
        			for (RelevantUserTables tab : tables) {
        				dropUserTable (tab);
        			}
        		}
        		if (modes.contains(OperationMode.LOCAL_CREATE)) {
        			for (RelevantUserTables tab : tables) {
        				createUserTable (tab);
        			}
        		}        		
        		if (modes.contains(OperationMode.LOCAL_CLEAR)) {
        			for (RelevantUserTables tab : tables) {
        				clearUserTable (tab);
        			}
        		}
        		if (modes.contains(OperationMode.LOCAL_ADD_TEST_DATA)) {
        			addTestData ();
        		}
        		if (modes.contains(OperationMode.REMOTE_TEST_QUERY_SAMPLE)) {
        			executeQueryOnSample();
        		}
        		if (modes.contains(OperationMode.REMOTE_TEST_QUERY_RESULT)) {
        			executeQueryOnResult();
        		}
        		if (modes.contains(OperationMode.LOCAL_REMOVE_CONFIG_REPOSITORY_DATA)) {
        			removeConstRepoConfig();
        		}
        		if (modes.contains(OperationMode.LOCAL_ADD_DEFAULT_CONFIG_REPOSITORY_DATA)) {
        			addDefaultConfigRepositoryData ();
        		}        		
        	} else {
        		// error case or user cancellation
        		OConsole.oprint("\nError: Argt is invalid or user cancelled"
        				+ " param plugin  \n");
        	}
        } catch (OException ex) {
    		OConsole.oprint(ex.toString());
        } catch (RuntimeException ex) {
    		OConsole.oprint(ex.toString());
        } catch (Throwable t) {
    		OConsole.oprint(t.toString());        	
        }
    }

	private void removeConstRepoConfig() throws OException {
		Table constRepoTable = null;

		try {
			constRepoTable = Table.tableNew(USER_CONST_REPOSITORY);
			DBUserTable.load(constRepoTable);
			
			for (ConstRepItem cri : ConfigurationItem.values()) {
				String context = cri.getContext();
				String subcontext = cri.getSubContext();
				String variable = cri.getVarName();
				String value = cri.getValue();
				int foundRow= findExistingRow(constRepoTable, context, subcontext,
						variable);
				if (foundRow != -1) {
					constRepoTable.delRow(foundRow);
				}
				OConsole.oprint ("\nRemoved : " + context + ", " + subcontext + ", " + variable + " = " + value);
			}
			DBUserTable.saveUserTable(constRepoTable , 0, 1, 0);
		} finally {
			constRepoTable = TableUtilities.destroy(constRepoTable);
		}
		
	}

	private int findExistingRow(Table constRepoTable, String context,
			String subcontext, String variable) throws OException {
		for (int row=constRepoTable.getNumRows(); row >= 1; row--) {
			String contextRow =  constRepoTable.getString("context", row);
			String subcontextRow =  constRepoTable.getString("sub_context", row);
			String variableRow =  constRepoTable.getString("name", row);
			if (	context.equals(contextRow) 
				 && subcontext.equals(subcontextRow)
				 && variable.equals(variableRow)) {
				return row;
			}
		}
		return -1;
	}

	private void addDefaultConfigRepositoryData() throws OException {
		Table constRepoTable = null;
		Table crNew = null;

		try {
			constRepoTable = Table.tableNew(USER_CONST_REPOSITORY);
			DBUserTable.load(constRepoTable);
			crNew = constRepoTable.cloneTable();
			crNew.setTableName(USER_CONST_REPOSITORY);
			
			for (ConstRepItem cri : ConfigurationItem.values()) {
				String context = cri.getContext();
				String subcontext = cri.getSubContext();
				String variable = cri.getVarName();
				String value = cri.getDefaultValue();
				boolean found= alreadyExists(constRepoTable, context, subcontext,
						variable, value);
				if (!found) {
					int row = crNew.addRow();
					crNew.setString("context", row, context);
					crNew.setString("sub_context", row, subcontext);
					crNew.setString("name", row, variable);
					crNew.setString("string_value", row, value);
					crNew.setInt("type", row, 2);
				}
				OConsole.oprint ("\nAdded : " + context + ", " + subcontext + ", " + variable + " = " + value);
			}
			DBUserTable.insert(crNew);
		} finally {
			constRepoTable = TableUtilities.destroy(constRepoTable);
			crNew = TableUtilities.destroy(crNew);
		}
	}

	private boolean alreadyExists(Table constRepoTable, String context,
			String subcontext, String variable, String value)
			throws OException {
		boolean found = false;
		for (int row=constRepoTable.getNumRows(); row >= 1; row--) {
			String contextRow =  constRepoTable.getString("context", row);
			String subcontextRow =  constRepoTable.getString("sub_context", row);
			String variableRow =  constRepoTable.getString("name", row);
			String valueRow =  constRepoTable.getString("string_value", row);
			if (	context.equals(contextRow) 
				 && subcontext.equals(subcontextRow)
				 && variable.equals(variableRow)
				 && value.equals(valueRow)) {
				found=true;
				break;
			}
		}
		return found;
	}

	private void executeQueryOnResult() throws OException {
		ConnectionExternal conn = new ConnectionExternal(ConfigurationItem.AS400_RESULT_SERVER_NAME_UK.getValue(),
				ConfigurationItem.AS400_RESULT_DB_NAME_UK.getValue(), 
				ConfigurationItem.AS400_RESULT_USER_UK.getValue(), 
				ConfigurationItem.AS400_RESULT_PASSWORD_UK.getValue());
		String query = ConfigurationItem.AS400_RESULT_QUERY_UK.getValue();
		query = String.format(query, "766345", "CALPMMDPD");
		Object[][] table = conn.query(query);
		OConsole.oprint("\nSQL Result for " + query);
		for (Object[] row : table) {
			OConsole.oprint("\n");
			for (Object col : row) {
				OConsole.oprint(String.format("%-8s", col.toString()) );
			}
		}		
	}
	
	private void executeQueryOnSample() throws OException {
		ConnectionExternal conn = new ConnectionExternal(ConfigurationItem.AS400_SAMPLE_SERVER_NAME_UK.getValue(),
				ConfigurationItem.AS400_SAMPLE_DB_NAME_UK.getValue(), 
				ConfigurationItem.AS400_SAMPLE_USER_UK.getValue(), 
				ConfigurationItem.AS400_SAMPLE_PASSWORD_UK.getValue());
		String query = ConfigurationItem.AS400_SAMPLE_QUERY_UK.getValue();
		query = String.format(query, "ZB1110", "'173500'");
		Object[][] table = conn.query(query);
		OConsole.oprint("\nSQL Result for " + query);
		for (Object[] row : table) {
			OConsole.oprint("\n");
			for (Object col : row) {
				OConsole.oprint(String.format("%-5s", col.toString()) );
			}
		}				
	}

	private void clearUserTable(RelevantUserTables tab) throws OException {
		Table tabToBeCleared = Table.tableNew(tab.getName());
		int ret;
		ret = DBUserTable.clear(tabToBeCleared);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			throw new OException ("Error clearing user table " + tab.getName());
		}
		TableUtilities.destroy(tabToBeCleared);
	}

	private ODateTime convertISO8601ToODateTime (String dateTime) throws OException {
		ODateTime dt = ODateTime.dtNew();
		if (dateTime.isEmpty() || dateTime.length() != 19) {
			return dt;
		}
		String year=dateTime.substring(0, 4);
		String month=dateTime.substring(5, 7);
		String day=dateTime.substring(8, 10);
		String hour = dateTime.substring(11, 13);
		String minute = dateTime.substring(14, 16);
		String second = dateTime.substring(17, 19);
		int date = OCalendar.convertYYYYMMDDToJd(year+month+day);
		dt.setDate(date);
		dt.setTime(Integer.parseInt(hour)*60*60 + Integer.parseInt(minute)*60
				+ Integer.parseInt(second));
		return dt;
	}
	
	private void createUserTable(RelevantUserTables tab) throws OException {
		Table tabToBeCreated = Table.tableNew(tab.getName());
		switch (tab) {
		case JM_METAL_PRODUCT_TEST:
			createInterfacesTable (tabToBeCreated, tab); 
			break;
		default:
			throw new OException ("Table structure for " + tab.getName() 
					+ " not implemented");
		}
		int ret = DBUserTable.create(tabToBeCreated);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			throw new OException ("Error creating user table " + tab.getName());
		}
		TableUtilities.destroy(tabToBeCreated);
	}

	private void createInterfacesTable(Table tabToBeCreated, RelevantUserTables tab) throws OException {
		createTableFromTableEnums (tabToBeCreated, tab.getColumns());
	}

	private void dropUserTable(RelevantUserTables tab) throws OException {
		Table userTable = Table.tableNew(tab.getName());
		int ret = DBUserTable.drop(userTable);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			throw new OException ("Error dropping user table " + tab.getName());
		}
		TableUtilities.destroy(userTable);
	}

	private boolean checkArgumentsTable(Table argt) throws OException{
		if (argt == null || argt.getNumRows() != 1 
			|| argt.getInt ("param_succeeded", 1) != 1) {
			return false;
		}
		if (argt.getTable("selected_tables", 1) == null) {
			return false;
		}
		if (argt.getTable("selected_modes", 1) == null ) {
				return false;
		}
		return true;
	}
	
	private List<OperationMode> getOperationModes (Table argt)
			throws OException {
		Table selectedModes = argt.getTable("selected_modes", 1);
		List<OperationMode> modes = 
				new ArrayList<OperationMode> (selectedModes.getNumRows());
		for (int row = selectedModes.getNumRows(); row >= 1; row--) {
			String name = selectedModes.getString("name", row);
			modes.add(OperationMode.valueOf(name));
		}
		return modes;
	}
	
	private List<RelevantUserTables> getUserTables (Table argt)
			throws OException {
		Table selectedTables = argt.getTable("selected_tables", 1);
		List<RelevantUserTables> userTables = 
				new ArrayList<RelevantUserTables> (selectedTables.getNumRows());
		for (int row = selectedTables.getNumRows(); row >= 1; row--) {
			String name = selectedTables.getString("name", row);
			userTables.add(RelevantUserTables.valueOf(name));
		}
		return userTables;
	}
	
	/** 
	 * Adds the all columns taken from the provided metadata 
	 * to the table
	 * @param tab Table to be filled. Assumption: tab is initially empty
	 * @param list The metadata containing the information about the columns 
	 */
	private void createTableFromTableEnums (Table tab, List<UserTableColumn> list) 
			throws OException {
		for (UserTableColumn col : list) {
			tab.addCol(col.getColName(), col.getColType(), col.getColTitle());
		}
	}	
	
	private void addTestData () throws OException {
		Table testData = null;
		try {
			testData = Table.tableNew(RelevantUserTables.JM_METAL_PRODUCT_TEST.getName());
			DBUserTable.structure(testData);
			for (LIMTestData td : LIMTestData.values()) {
				int row = testData.addRow();
				testData.setString(MetalProductTestTableCols.COUNTRY.getColName(), row, td.country);
				testData.setString(MetalProductTestTableCols.METAL.getColName(), row, td.metal);
				testData.setString(MetalProductTestTableCols.PRODUCT.getColName(), row, td.product);
				testData.setString(MetalProductTestTableCols.RESULT.getColName(), row, td.result);
			}
			int ret = DBUserTable.insert(testData);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new OException ("Error inserting data into table " + testData.getTableName());
			}
		} finally {
			testData = TableUtilities.destroy(testData);
		}
	}
	
	private enum LIMTestData {
		IRIDIUM1   ("United Kingdom", "IR", "IR0001", "CALSPECIR"),
		PALLADIUM1 ("United Kingdom", "PD", "1735000", "CALSPEC2"),
		PALLADIUM2 ("United Kingdom", "PD", "173510", "CALSPEC2"),
		PALLADIUM3 ("United Kingdom", "PD", "PD0001", "CALPMMDPD"),
		PLATINUM1  ("United Kingdom", "PT", "PT0001", "CALSPEC4"),
		RHODIUM1   ("United Kingdom", "RH", "204500", "CALSPEC6"),
		RUTHENIUM1  ("United Kingdom", "RU", "RU0001", "CALSPECRU1"),
		RUTHENIUM2  ("United Kingdom", "RU", "RU0002", "CALSPECRU1"),
		RUTHENIUM3  ("United Kingdom", "RU", "RU0003", "CALSPECRU1"),
		RUTHENIUM3b  ("United Kingdom", "RU", "197001", "CAL_PMMDRU"),
		RUTHENIUM4  ("United Kingdom", "RU", "RU0002", "CAL_PMMDRU"),
		RUTHENIUM5  ("United Kingdom", "RU", "RU0003", "CALSPECRU1")
		;
		
		private final String country;
		private final String metal;
		private final String product;
		private final String result;
		
		private LIMTestData (final String country, final String metal, final String product, final String result) {
			this.country = country;
			this.metal = metal;
			this.product = product;
			this.result = result;
		}
	}
}
