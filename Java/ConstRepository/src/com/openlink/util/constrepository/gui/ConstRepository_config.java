package com.openlink.util.constrepository.gui;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

public class ConstRepository_config extends Common implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		try { handleEvent(context, "const_repository", "Constants Repository"); }
		catch (Throwable t) { OConsole.oprint(t.toString()); }
	}
}
