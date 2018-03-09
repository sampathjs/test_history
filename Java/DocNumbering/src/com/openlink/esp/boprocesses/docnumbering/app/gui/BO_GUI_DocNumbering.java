package com.openlink.esp.boprocesses.docnumbering.app.gui;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_TRADE_INPUT)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class BO_GUI_DocNumbering extends Common implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		try { handleEvent(context, "bo_doc_numbering", "Document Numbering"); }
		catch (Throwable t) { OConsole.oprint(t.toString()); }
	}
}
