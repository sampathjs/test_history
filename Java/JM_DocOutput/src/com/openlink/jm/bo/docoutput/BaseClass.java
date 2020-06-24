package com.openlink.jm.bo.docoutput;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

/*
 * History:
 * 2020-03-25	V1.1	YadavP03	- memory leaks, remove console print & formatting changes
 */

abstract class BaseClass implements IScript
{
	protected final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

	public void execute(IContainerContext context) throws OException
	{
		process(context);
	}

	abstract protected void process(IContainerContext context) throws OException;
}
