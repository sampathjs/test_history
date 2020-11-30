package com.jm.usertable.gui;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

public class UserTableGUI_JMTransferChargesCriteria extends Common implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		try { handleEvent(context, "jm_transfercharges_criteria", "JM TransferCharges Criteria"); }
		catch (Throwable t) { OConsole.oprint(t.toString()); }
		
	}

}
