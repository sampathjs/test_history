package com.matthey.testutil;

import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Base class for JVS scripts under Testing Utilities project.
 * @author jains03
 *
 */
public abstract class BaseScript implements IScript
{
	/**
    * Setup a log file
    * 
     * @param logFileName
    * @throws OException
    */
    protected void setupLog() throws OException
    {
    	String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Logs\\";
    	String logDir =  abOutDir;

    	ConstRepository constRepo = new ConstRepository("SAPTestUtil", "");
    	String logLevel = constRepo.getStringValue("logLevel");
    	if(logLevel == null || logLevel.isEmpty())
    	{
    		logLevel = "DEBUG";
    	}
    	String logFile =  "SAPTestingUtilities.log";

    	try
    	{
    		if (logDir.trim().equals("")) 
    		{
    			PluginLog.init(logLevel);
    		}
    		else  
    		{
    			PluginLog.init(logLevel, logDir, logFile);
    		}
    	} 
    	catch (Exception e) 
    	{
    		String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
    		Util.exitFail(errMsg);
    		throw new RuntimeException(e);
    	}

    	PluginLog.info("**********" + this.getClass().getName() + " started **********");
    }
}
