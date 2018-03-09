package com.matthey.openlink.utilities.stub;

import com.olf.embedded.application.Context;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.EnumOlfDebugType;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Variable;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
 
@ScriptCategory({EnumScriptCategory.TpmStep})
public class RetrieveTEST extends AbstractGenericScript {
 
    @Override
    public Table execute(Context context, ConstTable table) {
 
        Process process = null;
 
        try {
	    String instanceId = table.getString("ProcessInstanceId", 0);
	    long tpmInstanceId = Long.parseLong(instanceId);
 
	    process = context.getTpmFactory().getProcess(tpmInstanceId);
	} catch (Exception e)
        {
	    throw new RuntimeException("No TPM Process Instance available", e);
	}
 
        ConstTable tbl;
        try {
            Variable var = process.getVariables().getVariable("DbResult");
            tbl = process.getVariables().asTable();
 
            // Display the table if Opencomponents debug level is at least Medium
            context.getDebug().viewTable(tbl, "DbResult");
            //context.getDebug().viewTable(tbl, "DbResult", EnumDebugLevel.Medium, EnumOlfDebugType.Opencomponent.getValue());
	} catch (RuntimeException e)
        {
            // Save exception message into LastError variable and save it.
            Variable var = process.getVariable("LastError");
            var.setValue(e.getLocalizedMessage());
            process.setVariable(var);
 
            // Re-throw exception to ensure TPM knows that script failed.
	    throw e;
	}
 
        return tbl.cloneData();
    }
}