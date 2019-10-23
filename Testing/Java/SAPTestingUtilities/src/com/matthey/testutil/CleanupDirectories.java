package com.matthey.testutil;

import java.io.File;

import com.matthey.testutil.common.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

public class CleanupDirectories implements IScript
{

	@Override
	public void execute(IContainerContext context) throws OException
	{
		String directoryPath;
		String sourceDirectoryPaths[];

		File sourceDirectory;
		File destinationDirectory;
		File[] directoryListing;

		Table argumenTable;

		Util.setupLog();

		PluginLog.info("Started executing " + this.getClass().getSimpleName());
		argumenTable = context.getArgumentsTable();
		Util.printTableOnLogTable(argumenTable);
		directoryPath = argumenTable.getString("directory_path", 1);
		PluginLog.debug("Paths of directories to clean: " + directoryPath);

		directoryPath = Util.getAbsolutePath(directoryPath);

		sourceDirectoryPaths = directoryPath.split(",");

		for (String directoryToClean : sourceDirectoryPaths)
		{
			PluginLog.debug("Cleaning files from directory: " + directoryToClean);
			destinationDirectory = new File(directoryToClean + "\\archive");

			if (!destinationDirectory.exists())
			{
				destinationDirectory.mkdirs();
			}

			sourceDirectory = new File(directoryToClean);
			directoryListing = sourceDirectory.listFiles();
			if (directoryListing != null)
			{
				for (File child : directoryListing)
				{
					PluginLog.debug("Moving file: " + child.getAbsolutePath());
					/* Move files to destination folder */
					child.renameTo(new File(destinationDirectory + "\\" + child.getName()));

					PluginLog.debug(child.getName() + " moved to :" + destinationDirectory);
				}
			}
		}
		PluginLog.info("Completed executing " + this.getClass().getSimpleName());
	}

}
