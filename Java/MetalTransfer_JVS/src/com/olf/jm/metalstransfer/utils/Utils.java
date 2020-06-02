package com.olf.jm.metalstransfer.utils;

import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.jm.logging.Logging;

public class Utils
{
public static void initialiseLog(String logFileName) throws OException
{
	String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
	
	String logDir = abOutDir;
	String logLevel = "INFO";
	String logFile = logFileName;

    try
    {
    	Logging.init(Utils.class,"MetalTransfer", logFileName);
    } 
	catch (Exception e) 
	{
		String errMsg = "Failed to initialize logging module.";
		com.olf.openjvs.Util.exitFail(errMsg);
		throw new RuntimeException(e);
	}
}
}