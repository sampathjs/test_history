package com.openlink.esp.materialamendments.app.gui;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

/*
 * History:
 * 1.0   - 21.01.2013 eikesass    - initial version
 */

/**
 * This script is the entry point for the UTM logic for USER_MATERIAL_CHECK_RULES.
 * 
 * @author eikesass
 * @version 1.0
 * @category none
 * 
 */

public class RulesUtmHandler extends Common implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		try {
			handleEvent(context, "material_check_rules", "Rules"); 
		}
		catch (Throwable t) {
			OConsole.oprint(t.toString()); 
		}
	}
}
