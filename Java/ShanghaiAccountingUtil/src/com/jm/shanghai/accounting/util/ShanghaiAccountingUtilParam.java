package com.jm.shanghai.accounting.util;

import com.jm.shanghai.accounting.util.model.OperationMode;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

/*
 * History:
 * 2018-11-20		V1.0		jwaechter	- Initial Version
 */

/**
 * Parameter plugin for the deployment and config helper of the Shanghai Accounting Feed.
 * @author jwaechter
 * @version 1.0
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class ShanghaiAccountingUtilParam implements IScript
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
		
		if(Ask.viewTable (askTable,"Shanghai Accounting Utility","" +
				"Please Complete the Following Fields") == 0) {
			return;
		}
		
		Table selectedOpModes = askTable.getTable( "return_value", 1);
		argt.getTable("selected_modes", 1).select(selectedOpModes, 
				"return_value(name)", "return_value GT -1");

		if (askTable != null) {
			askTable.destroy();
		}

		argt.setInt("param_succeeded", 1, 1);
		requestFileNames(argt);
	}
	

	private void requestFileNames(Table argt) throws OException {
		Table modeTable = argt.getTable("selected_modes", 1);
	    try {
			for (int row = modeTable.getNumRows(); row >= 1; row--) {
				String selMode = modeTable.getString("name", row);
				OperationMode opMode = OperationMode.valueOf(selMode);
				if (opMode.isUsingFilename()) {
					Table askTable = Table.tableNew("Ask");
					int ret  = Ask.setTextEdit(askTable, 
							"Select filename to " + opMode.getMenuText(),
							"*.csv", 
							ASK_TEXT_DATA_TYPES.ASK_FILENAME,
							opMode.getMenuText(), 
							1);
					
					if(Ask.viewTable (askTable,"Shanghai Accounting Utility","" +
							"Please Complete the Following Fields") == 0) {
						argt.setInt("param_succeeded", 1, 0);
						return;
					}
					Table returnValueTable = askTable.getTable("return_value", 1);
					String filename = returnValueTable.getString("return_value", 1);
					modeTable.setString("filename", row, filename);
				}
			}	    	
	    } catch (Throwable t) {
			argt.setInt("param_succeeded", 1, 0);
	    }
	}

	private Table createOperationModeTable () throws OException {
		Table opModes = Table.tableNew("Operation Modes (Table Create)");
		opModes.addCol("id", COL_TYPE_ENUM.COL_INT);
		opModes.addCol ("operation_mode", COL_TYPE_ENUM.COL_STRING);
		opModes.addCol ("description", COL_TYPE_ENUM.COL_STRING);
		opModes.addCol ("uses_filename", COL_TYPE_ENUM.COL_INT);
		for (OperationMode mode: OperationMode.values()) {
			int row = opModes.addRow();
			opModes.setInt("id", row, mode.ordinal());
			opModes.setString("operation_mode", row, mode.toString());
			opModes.setString("description", row, mode.getMenuText() + " - " 
							  + mode.getDescription());
			opModes.setInt("uses_filename", row, mode.isUsingFilename()?1:0);
		}
		opModes.colHide("operation_mode");
		opModes.colHide("uses_filename");
		return opModes;
	}
		
	private void setupArgt(Table argt) throws OException{
		int row = argt.addRow();

		argt.addCol("param_succeeded", COL_TYPE_ENUM.COL_INT);	
		argt.addCol("selected_modes", COL_TYPE_ENUM.COL_TABLE);
		
		Table selectedModes = Table.tableNew("Selected Modes");
		selectedModes.addCol("name", COL_TYPE_ENUM.COL_STRING);
		// optional filename for import and export operation modes
		selectedModes.addCol("filename", COL_TYPE_ENUM.COL_STRING);
		argt.setTable("selected_modes", row, selectedModes);
	}
}
