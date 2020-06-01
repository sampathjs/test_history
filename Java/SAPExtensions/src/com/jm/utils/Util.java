package com.jm.utils;

import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.jm.logging.Logging;

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
        	 Logging.init(Util.class,"SAPExtensions",logFileName);
        } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
	}
}
