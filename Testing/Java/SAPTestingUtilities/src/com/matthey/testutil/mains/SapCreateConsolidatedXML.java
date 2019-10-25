package com.matthey.testutil.mains;

import com.matthey.testutil.CreateConsolidatedXML;
import com.matthey.testutil.common.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

public class SapCreateConsolidatedXML extends CreateConsolidatedXML implements IScript
{
	@Override
	public void execute(IContainerContext iContainerContext) throws OException
	{
		Table argumentTable = iContainerContext.getArgumentsTable();
		String directoryPath = argumentTable.getString("directory_path", 1);
		String rootElement;

		Util.setupLog();
		PluginLog.info("Started executing " + this.getClass().getSimpleName());
		PluginLog.debug("Directory path: " + directoryPath);

		rootElement = argumentTable.getString("root_element", 1);		
		createXML(directoryPath, rootElement);
	} 
}
