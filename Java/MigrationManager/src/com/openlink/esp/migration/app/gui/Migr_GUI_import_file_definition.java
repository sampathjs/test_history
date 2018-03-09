package com.openlink.esp.migration.app.gui;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.framework.utm.Common;

public class Migr_GUI_import_file_definition extends Common implements IScript {
	public void execute(IContainerContext context) throws OException {
		try { handleEvent(context, "migr_import_file_def", "Import File Definition"); }
		catch (Throwable t) { OConsole.oprint(t.toString()); }
	}
}
