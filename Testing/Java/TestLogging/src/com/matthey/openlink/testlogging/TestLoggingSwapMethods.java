package com.matthey.openlink.testlogging;

// New Imports 
//import static com.olf.jm.logging.LogCategory.*;
//import static com.olf.jm.logging.LogLevel.*;
//import com.olf.jm.logging.Logger;
//import com.olf.jm.logging.PluginLog;



// Old Imports
import static com.openlink.endur.utilities.logger.LogCategory.*;
import static com.openlink.endur.utilities.logger.LogLevel.*;
import com.openlink.endur.utilities.logger.Logger;
import com.openlink.util.logging.PluginLog;


import com.openlink.util.constrepository.ConstRepository;

public class TestLoggingSwapMethods {


	private static void initNewPluginLog(String outputDir,String outputFile) throws Exception {
		ConstRepository constRep = new ConstRepository(TestLoggingConstants.CONST_REPO_CONTEXT, TestLoggingConstants.CONST_REPO_SUBCONTEXT);
		
		String logLevel = "Debug"; 

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			if (outputDir == null || outputDir.length()==0){
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, outputDir, outputFile);
			}
		} catch (Exception e) {
			// do something
		}
	}

	
	public static void runSwapPluginLog(String message, String logLevel , String outputDir,String outputFile, String classSimpleName) throws Exception {
		initNewPluginLog( outputDir, outputFile);
		
		PluginLog.info(classSimpleName + " started");


		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			PluginLog.fatal(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			PluginLog.error(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			PluginLog.warn(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			PluginLog.info(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			PluginLog.debug(message);
		}
		PluginLog.debug("getLogDir:" + PluginLog.getLogDir());
		PluginLog.debug("getLogFile:" + PluginLog.getLogFile());
		PluginLog.debug("getLogPath:" + PluginLog.getLogPath());
		PluginLog.debug("getLogLevel:" + PluginLog.getLogLevel());
		
		PluginLog.exitWithStatus(false);
		
	}
	public static void runSwapLoggerLog(String message, String logLevel, Object callingClass) {

		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			try {
				Logger.log(FATAL, General, callingClass, message);
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				Logger.log(FATAL,  General, callingClass, message, e);
			}

		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			try {
				Logger.log(ERROR, General, callingClass, message);	
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				Logger.log(ERROR,  General, callingClass, message, e);
			}
			
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			Logger.log(WARNING, General, callingClass, message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			Logger.log(DEBUG, General, callingClass, message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			Logger.log(INFO, General, callingClass, message);
		}

		
	}

}
