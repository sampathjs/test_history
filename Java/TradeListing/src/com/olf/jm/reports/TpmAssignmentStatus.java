package com.olf.jm.reports;

import java.util.HashMap;
import java.util.Map;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;
import com.olf.openrisk.tpm.Task;
import com.olf.openrisk.tpm.Variable;
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 10-Apr-2021 |               | Ryan Rodrigues   | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class TpmAssignmentStatus extends AbstractGenericScript {
	private static final String TPM_DISPATCH = "Dispatch";
	private HashMap<Integer, String> map;
	
	@Override
    public Table execute(Context context, ConstTable table) {
        // Create returnt table
		Table returnt = context.getTableFactory().createTable();

        try {
            Logging.init(context,this.getClass(),"TradeListing", "TpmAssignmentStatus");
            Logging.info("Starting TpmAssignmentStatus");
            
            map = new HashMap<Integer, String>();
            
            //Table tbl = tf.createTable("AssignmentStatusTable");
            returnt.addColumn("tran_num", EnumColType.Int);
            returnt.addColumn("assigned_group", EnumColType.String);
            
            retriveTPMVariables(context);
            convertMapToTable(map, returnt);
//            context.getDebug().viewTable(returnt);
        } catch (Exception e) {
        	Logging.error("TpmAssignmentStatus:" + e.getMessage() );
        }finally{
            Logging.close();
        }
        return returnt;
        
    }
            
		// Get all current instances of the TPM
		public void retriveTPMVariables(Context context) {
            Logging.info("Entering retriveTPMVariables");
			ProcessDefinition tpmWorkflow = context.getTpmFactory().getProcessDefinition(TPM_DISPATCH);
			Process[] processes = tpmWorkflow.getProcesses();
			for (Process process : processes) {
				// Get the tran number value from process variable
				Variable tranNum = process.getVariable("TranNum");
					int taskCount = process.getTasks().length;
					if (taskCount > 0) {
						for (Task task : process.getTasks()) {
							if ("Management Approval Group".equalsIgnoreCase(task.getAssignedGroup().getName())
									&& task.getName() != null && task.getName().indexOf("Approval") > -1) {
								map.put(tranNum.getValueAsInt(), task.getAssignedGroup().getName());
							}
						}
					} 
			}
			Logging.info("Exiting retriveTPMVariables");
		}
		
		public void convertMapToTable(Map<Integer, String> map, Table t){
			Logging.info("Entering convertMapToTable");
			int rowNum = 0;
			for (Map.Entry<Integer, String> entry : map.entrySet()) {
				Integer key = entry.getKey();
				String value = entry.getValue();
				t.addRow();
				t.setInt("tran_num", rowNum, key);
				t.setString("assigned_group", rowNum, value);
				rowNum++;
			}
			Logging.info("Exiting convertMapToTable");
		}
	
}
