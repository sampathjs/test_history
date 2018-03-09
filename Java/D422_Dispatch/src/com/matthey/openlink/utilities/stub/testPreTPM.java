package com.matthey.openlink.utilities.stub;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.tpm.Process;

@ScriptCategory({ EnumScriptCategory.TpmPreProcess })
public class testPreTPM extends AbstractGenericScript {
	@Override
	public com.olf.openrisk.table.Table execute(com.olf.embedded.application.Context context, com.olf.openrisk.table.ConstTable table) {
	
    Process process = null;
    System.out.print("STARTing,,,,");
    try {
    String instanceId = table.getString("ProcessInstanceId", 0);
    long tpmInstanceId = Long.parseLong(instanceId);

    process = context.getTpmFactory().getProcess(tpmInstanceId);
} catch (Exception e)
    {
    throw new RuntimeException("No TPM Process Instance available", e);
}
	return null;
	}
}
