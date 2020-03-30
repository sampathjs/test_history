package com.olf.jm.metalstransfer.transfercharges;

import com.olf.openjvs.OException;

import com.olf.openjvs.SystemUtil;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2020-03-25	V1.1	AgrawA01	- memory leaks, remove console print & formatting changes
 */

public class Utils {
	
	public static void initialiseLog(String logFileName) throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;
		String logLevel = "INFO";
		String logFile = logFileName;

		try {
			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
			
		} catch (Exception e) {
			String errMsg = "Failed to initialize logging module.";
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
	}
}