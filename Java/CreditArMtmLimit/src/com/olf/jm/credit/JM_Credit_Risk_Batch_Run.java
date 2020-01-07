package com.olf.jm.credit;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openjvs.*;

@ScriptCategory( { EnumScriptCategory.Generic })
public class JM_Credit_Risk_Batch_Run extends AbstractGenericScript {
 
    @Override
    public Table execute(Context context, EnumScriptCategory category, ConstTable table) {
    	
    	Logging.init(context, this.getClass(), "JM_Credit_Risk_Batch_Run", "");
	Logging.info("Start JM_Credit_Risk_Batch_Run");
		String taskName = "";
		try
		{	
			taskName = Ref.getInfo().getString("task_name", 1);
			Logging.info("Executing Credit Batch Task for Definition:" + taskName);
			Credit.runBatchTask(taskName);
		}
		catch (OException e)
		{
			Logging.error( "Error Executing Credit Batch Task for Definition:" + taskName + " : " + e.getMessage(),e);	
		}
		
		Logging.info( "Completed JM_Credit_Risk_Batch_Run.java");
		Logging.close();
		
		return null;
	}
	
}
