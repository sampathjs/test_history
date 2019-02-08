package com.jm.utils;

import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.openlink.util.logging.PluginLog;

/**
 * Helper class with misc static functions
 */
public class Util 
{	
	/**
	 * Initialise a new logger instance
	 * 
	 * @throws OException
	 */
	public static void initialiseLog(String logFileName) throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		
		String logDir = abOutDir;
		String logLevel = "INFO";
		String logFile = logFileName;

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
			String errMsg = "Failed to initialize logging module.";
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
	}
}
