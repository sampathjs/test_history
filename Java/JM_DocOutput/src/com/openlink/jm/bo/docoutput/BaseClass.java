package com.openlink.jm.bo.docoutput;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

abstract class BaseClass implements IScript
{
	protected final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue();

	public void execute(IContainerContext context) throws OException
	{
		process(context);
	}

	abstract protected void process(IContainerContext context) throws OException;

	protected final void tryOprint(String s, boolean newLine)
	{
		try { OConsole.oprint(s); if (newLine) OConsole.oprint("\n"); } catch (Throwable t) {}
	}
}
