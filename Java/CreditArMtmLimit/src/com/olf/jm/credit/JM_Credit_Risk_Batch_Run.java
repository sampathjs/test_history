package com.olf.jm.credit;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

import com.olf.openjvs.*;

@ScriptCategory( { EnumScriptCategory.Generic })
public class JM_Credit_Risk_Batch_Run extends AbstractGenericScript {
 
    @Override
    public Table execute(Context context, EnumScriptCategory category, ConstTable table) {
    	
    	Logger.log(LogLevel.INFO, LogCategory.Credit, this, "Start JM_Credit_Risk_Batch_Run");
		String taskName = "";
		try
		{	
			taskName = Ref.getInfo().getString("task_name", 1);
			Logger.log(LogLevel.INFO, LogCategory.Credit, this, "Executing Credit Batch Task for Definition:" + taskName);
			Credit.runBatchTask(taskName);
		}
		catch (OException e)
		{
			Logger.log(LogLevel.ERROR, LogCategory.Credit, this, "Error Executing Credit Batch Task for Definition:" + taskName + " : " + e.getMessage());	
		}
		
		Logger.log(LogLevel.INFO, LogCategory.Credit, this, "Completed JM_Credit_Risk_Batch_Run.java");
		
		return null;
	}
	
}
