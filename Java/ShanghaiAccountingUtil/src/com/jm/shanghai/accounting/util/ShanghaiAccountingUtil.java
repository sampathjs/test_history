package com.jm.shanghai.accounting.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.fixed.ConstRepItem;
import com.jm.shanghai.accounting.util.model.OperationMode;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
/*
 * History: 
 * 2018-11-20 - V1.0 - jwaechter - initial version
 * 2019-02-11 -	V1.1 - jwaechter - removed modes not being usable because of missing DB access
 */
import com.openlink.util.misc.TableUtilities;

/**
 * This class is responsible to create all necessary user tables for the
 * Shanghai Accounting Interface
 * This is the main plugin. Refer to details about
 * user input in
 *  {@link ShanghaiAccountingUtilParam}. 
 * @author jwaechter
 * @version 1.1
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class ShanghaiAccountingUtil implements IScript
{
    private static final String LINE_SEPARATOR = "\n";
	private static final String COL_SEPARATOR = ",";
	private static final String USER_CONST_REPOSITORY = "USER_const_repository";

	@Override
	public void execute(IContainerContext context) throws OException
    {
        try {
        	Table argt = context.getArgumentsTable();
        	if (checkArgumentsTable (argt)) {
        		Map<OperationMode, String> modes = getOperationModes(argt);
        		if (modes.containsKey(OperationMode.LOCAL_REMOVE_CONFIG_REPOSITORY_DATA)) {
        			removeConstRepoConfig();
        		}
        		if (modes.containsKey(OperationMode.LOCAL_ADD_DEFAULT_CONFIG_REPOSITORY_DATA)) {
        			addDefaultConfigRepositoryData ();
        		}
        		if (modes.containsKey(OperationMode.EXPORT_MAPPING_CONFIG_TABLE)) {
        			exportMappingConfigTable(modes.get(OperationMode.EXPORT_MAPPING_CONFIG_TABLE));
        		}
        		if (modes.containsKey(OperationMode.CLEAR_MAPPING_CONFIG_TABLE)) {
        			clearMappingTable ();
        		}        	
        		if (modes.containsKey(OperationMode.IMPORT_MAPPING_CONFIG_TABLE)) {
        			importMappingConfigTable (modes.get(OperationMode.IMPORT_MAPPING_CONFIG_TABLE));
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
	
	private void clearMappingTable() {
		Table mappingTable;
		try {
			mappingTable = Table.tableNew(ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue());
			int ret = DBUserTable.clear(mappingTable);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException ("Error while clearing mapping table");
			}
			mappingTable.destroy();
		}  catch (OException e) {
			for (StackTraceElement ste : e.getStackTrace()) {
				try {
					OConsole.oprint(ste.toString());
				} catch (OException e1) {
					e1.printStackTrace();
				}
			}
			throw new RuntimeException (e);
		}
	}

	private void importMappingConfigTable(String inputFilename) {
		Table mappingTable;
		int ret=0;
		try {
			mappingTable = Table.tableNew(ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue());
			ret = DBUserTable.load(mappingTable);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException ("Error while importing mapping table");
			}
			Table headerTbl = mappingTable.cloneTable();
			Table bodyTbl = mappingTable.cloneTable();
			headerTbl.loadTableFromFileWithHeader(bodyTbl, inputFilename);
			ret = bodyTbl.copyRowAddAll(mappingTable);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException ("Error while copying content while importing mapping table");
			}
			DBUserTable.saveUserTable(mappingTable, 0, 0, 0);
			mappingTable.destroy();
			headerTbl.destroy();
			bodyTbl.destroy();
		} catch (Throwable e) {
			for (StackTraceElement ste : e.getStackTrace()) {
				try {
					OConsole.oprint(ste.toString());
				} catch (OException e1) {
					e1.printStackTrace();
				}
			}
			throw new RuntimeException (e);
		}
	}

	private void exportMappingConfigTable(String outputFilename) {
		Table mappingTable;
		try {
			mappingTable = Table.tableNew(ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue());
			int ret = DBUserTable.load(mappingTable);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException ("Error while exporting mapping table");
			}
			File outputFile = new File(outputFilename);
			if (outputFile.exists()) {
				outputFile.delete();
			}
			mappingTable.printTableDumpToFile(outputFilename);
			mappingTable.destroy();
		} catch (Throwable e) {
			for (StackTraceElement ste : e.getStackTrace()) {
				try {
					OConsole.oprint(ste.toString());
				} catch (OException e1) {
					e1.printStackTrace();
				}
			}
			throw new RuntimeException (e);
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


	private boolean checkArgumentsTable(Table argt) throws OException{
		if (argt == null || argt.getNumRows() != 1 
			|| argt.getInt ("param_succeeded", 1) != 1) {
			return false;
		}
		if (argt.getTable("selected_modes", 1) == null ) {
				return false;
		}
		return true;
	}
	
	private Map<OperationMode, String> getOperationModes (Table argt)
			throws OException {
		Table selectedModes = argt.getTable("selected_modes", 1);
		Map<OperationMode, String> modes = 
				new HashMap<>();
		for (int row = selectedModes.getNumRows(); row >= 1; row--) {
			String name = selectedModes.getString("name", row);
			String filename = selectedModes.getString("filename", row);
			modes.put(OperationMode.valueOf(name), filename);
		}
		return modes;
	}
}
