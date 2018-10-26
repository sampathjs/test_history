package com.openlink.esp.migration.app.gui;



import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

public class BO_GUI_confirm_customer_wording extends Common implements IScript {
	
	public void execute(IContainerContext context) throws OException {
		try { 
			handleEvent(context, "Confirm_Customer_Wording", "Confirm Customer Wording");
		} catch (Throwable t) { 
			OConsole.oprint(t.toString()); 
		}
	}
}
