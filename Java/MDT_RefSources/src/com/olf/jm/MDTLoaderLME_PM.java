package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class MDTLoaderLME_PM implements IScript {

	public void execute(IContainerContext context) throws OException
	{
	    String loadGroup1;
	
	    loadGroup1 = "RefSource LME PM";
	
	    Table argt = context.getArgumentsTable();
	
	    argt.addCol("args", COL_TYPE_ENUM.COL_STRING);
	
	    argt.addRow();
	    argt.setString(1, 1, loadGroup1);
	
	
	}

}
