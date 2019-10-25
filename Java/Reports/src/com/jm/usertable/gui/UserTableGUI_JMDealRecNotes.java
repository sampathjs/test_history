package com.jm.usertable.gui;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_TRADE_INPUT)
public class UserTableGUI_JMDealRecNotes extends Common implements IScript {
	
	
	
	public void execute(IContainerContext context) throws OException {
		try { handleEvent(context, "jm_deal_rec_notes", "JM Deal Rec Notes"); }
		catch (Throwable t) { OConsole.oprint(t.toString()); }
	}
}
