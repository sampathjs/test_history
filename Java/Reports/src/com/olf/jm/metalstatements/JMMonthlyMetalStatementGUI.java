package com.olf.jm.metalstatements;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

public class JMMonthlyMetalStatementGUI extends Common implements IScript {
	
	
	
	public void execute(IContainerContext context) throws OException {
		try { handleEvent(context, "jm_monthly_metal_statement", "JM Monthly Metal Statement"); }
		catch (Throwable t) { OConsole.oprint(t.toString()); }
	}
}
