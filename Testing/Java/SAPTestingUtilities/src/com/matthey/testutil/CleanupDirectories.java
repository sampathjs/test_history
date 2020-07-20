package com.matthey.testutil;

import java.io.File;

import com.matthey.testutil.common.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.jm.logging.Logging;

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

		Logging.info("Started executing " + this.getClass().getSimpleName());
		argumenTable = context.getArgumentsTable();
		Util.printTableOnLogTable(argumenTable);
		directoryPath = argumenTable.getString("directory_path", 1);
		Logging.debug("Paths of directories to clean: " + directoryPath);

		directoryPath = Util.getAbsolutePath(directoryPath);

		sourceDirectoryPaths = directoryPath.split(",");

		for (String directoryToClean : sourceDirectoryPaths)
		{
			Logging.debug("Cleaning files from directory: " + directoryToClean);
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
					Logging.debug("Moving file: " + child.getAbsolutePath());
					/* Move files to destination folder */
					child.renameTo(new File(destinationDirectory + "\\" + child.getName()));

					Logging.debug(child.getName() + " moved to :" + destinationDirectory);
				}
			}
		}
		Logging.info("Completed executing " + this.getClass().getSimpleName());
	}

}
