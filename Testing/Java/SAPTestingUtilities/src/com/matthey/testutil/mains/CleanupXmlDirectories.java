package com.matthey.testutil.mains;

import java.io.File;

import com.matthey.testutil.BaseScript;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.openlink.util.logging.PluginLog;

/**
 * Move garbage XML files to their according archive folders
 * @author KailaM01
 */
public class CleanupXmlDirectories extends BaseScript
{
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		setupLog();
		
		String sapDirectory = com.olf.openjvs.SystemUtil.getEnvVariable("AB_OUTDIR") + "\\reports\\sap\\";
		
		String[] folders = new String[] { "Invoices", "Confirmations\\Deals", "Confirmations\\Transfers" };
		
		for (String folder : folders)
		{
			String outputDir = sapDirectory + folder;
			
			File destinationFolder = new File(outputDir + "\\archive");

		    if (!destinationFolder.exists())
		    {
		        destinationFolder.mkdirs();
		    }
			
			File dir = new File(outputDir);
			File[] directoryListing = dir.listFiles();
			if (directoryListing != null) 
			{
				for (File child : directoryListing) 
				{
					/* Move files to destination folder */
	                child.renameTo(new File(destinationFolder + "\\" + child.getName()));
	                
	                PluginLog.debug(child.getName() + " moved to :" + destinationFolder);
				}
			}
		}
	}	
}
