package com.olf.jm.interfaces.lims.util.app;

import com.olf.jm.interfaces.lims.model.RelevantUserTables;
import com.olf.jm.interfaces.lims.util.model.OperationMode;
import com.olf.jm.interfaces.lims.util.model.OperationModeType;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.openlink.util.misc.TableUtilities;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class LimUtilParam implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
        try {
        	Table argt = context.getArgumentsTable();
        	setupArgt (argt);
        	askUser (argt);
        } catch (OException ex) {
        	
        } catch (RuntimeException ex) {
        	
        } catch (Throwable t) {
        	
        }
    }

	private void askUser(Table argt) throws OException {
		Table askTable = Table.tableNew("Ask");
		Table opModeTable = createOperationModeTable ();
		Table userTableNames = createUserTableNamesTable ();
		int ret;

		ret  = Ask.setAvsTable(askTable,
				opModeTable,
				"Mode of Operation",
				opModeTable.getColNum("operation_mode"),
				ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(),
				opModeTable.getColNum("operation_mode"),
				null,
				"Please select the mode of operation",
				1);
		
		if(Ask.viewTable (askTable,"LIM Utility","" +
				"Please Complete the Following Fields") == 0)
		{
			return;
		}
		
		Table selectedOpModes = askTable.getTable( "return_value", 1);
		argt.getTable("selected_modes", 1).select(selectedOpModes, 
				"return_value(name)", "return_value GT -1");

		TableUtilities.destroy(askTable);
		askTable = Table.tableNew("Ask");
		int rowUserTable=-1;
		int rowExternalSystem=-1;
		
		if (isOperationOfType(argt, OperationModeType.LOCAL_DB_SCHEMA)) {
			ret  = Ask.setAvsTable(askTable,
					userTableNames,
					"User Table",
					userTableNames.getColNum("name"),
					ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(),
					userTableNames.getColNum("name"),
					null,
					"Please select the user table(s) to process (for local operations)",
					1);	
			rowUserTable = 1;
		}
		
		if (rowExternalSystem != -1 || rowUserTable != -1) {
			if(Ask.viewTable (askTable,"LIM Utility","" +
					"Please Complete the Following Fields") == 0)
			{
				// error case
			}
			else
			{	
				if (rowUserTable != -1) {
					Table selectedUserTables = askTable.getTable( "return_value", rowUserTable);
					argt.getTable("selected_tables", 1).select(selectedUserTables, 
							"return_value(name)", "return_value GT -1");
				}
				
				if (rowExternalSystem != -1) {
					Table selectedEs = askTable.getTable( "return_value", rowExternalSystem);
					argt.getTable("selected_external_systems", 1).select(selectedEs, 
							"return_value(name)", "return_value GT -1");
				}
				
				argt.setInt("param_succeeded", 1, 1);
			}			
		} else {
			argt.setInt("param_succeeded", 1, 1);
		}
		TableUtilities.destroy(askTable);
		TableUtilities.destroy(opModeTable);
		TableUtilities.destroy(userTableNames);
	}
	
	private boolean isOperationOfType(Table argt, OperationModeType mt) throws OException{
		Table opModeTable = argt.getTable("selected_modes", 1);
		for (int row=opModeTable.getNumRows(); row >= 1; row--) {
			String modeName = opModeTable.getString("name", row);
			OperationMode mode = OperationMode.valueOf(modeName);
			if (mode.getType() == mt) {
				return true;
			}
		}
		return false;
	}

	private Table createOperationModeTable () throws OException {
		Table opModes = Table.tableNew("Operation Modes (Table Create)");
		opModes.addCol("id", COL_TYPE_ENUM.COL_INT);
		opModes.addCol ("operation_mode", COL_TYPE_ENUM.COL_STRING);
		opModes.addCol ("description", COL_TYPE_ENUM.COL_STRING);
		for (OperationMode mode: OperationMode.values()) {
			int row = opModes.addRow();
			opModes.setInt("id", row, mode.ordinal());
			opModes.setString("operation_mode", row, mode.toString());
			opModes.setString("description", row, mode.getMenuText() + " - " 
							  + mode.getDescription());
		}
		opModes.colHide("operation_mode");
		return opModes;
	}
	
	private Table createUserTableNamesTable () throws OException {
		Table userTables = Table.tableNew("User Tables (Table Create)");
		
		userTables.addCol ("id", COL_TYPE_ENUM.COL_INT);
		userTables.addCol ("name", COL_TYPE_ENUM.COL_STRING);
		userTables.addCol ("db_name", COL_TYPE_ENUM.COL_STRING);
		userTables.addCol ("description", COL_TYPE_ENUM.COL_STRING);
		
		for (RelevantUserTables table : RelevantUserTables.values()) {
			int row = userTables.addRow();
			userTables.setInt("id", row, table.ordinal());
			userTables.setString("name", row, table.toString());
			userTables.setString("description", row, table.getDescription());
			userTables.setString("db_name", row, table.getName());
		}
		userTables.colHide("name");
		return userTables;
	}
	

	
	private void setupArgt(Table argt) throws OException{
		int row = argt.addRow();

		argt.addCol("param_succeeded", COL_TYPE_ENUM.COL_INT);
		argt.addCol("selected_tables", COL_TYPE_ENUM.COL_TABLE);
		
		Table selectedTables = Table.tableNew("Selected Tables");
		selectedTables.addCol("name", COL_TYPE_ENUM.COL_STRING);
		argt.setTable("selected_tables", row, selectedTables);
		
		argt.addCol("selected_modes", COL_TYPE_ENUM.COL_TABLE);
		Table selectedModes = Table.tableNew("Selected Modes");
		selectedModes.addCol("name", COL_TYPE_ENUM.COL_STRING);
		argt.setTable("selected_modes", row, selectedModes);
	}
}
