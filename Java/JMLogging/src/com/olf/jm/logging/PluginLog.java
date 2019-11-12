package com.olf.jm.logging;

import java.nio.file.Paths;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;

public class PluginLog extends LoggingBase{

	public static void init() throws Exception { 
		init(null, null, null); 
	}


	public static void init(String logLevel) throws Exception { 
		init(logLevel, null, null); 
	}

	public static void init(String logLevel, String logDir, String logFile) throws Exception {
		
		// Levels handle multiple initialisations via nested plugins
        // Each level gets it's own logger which logs to it's own log file
        // Each initialisation increments the level and a close decrements the level
        level++;
        PluginLog logger = new PluginLog();
        logMap.put(level, logger);

        if( logLevel ==null || logLevel.length()==0){
        	logLevel = "Error";
        }
        if( logFile ==null || logFile.length()==0){
        	logger.setCallingClassName();
        	logFile = logger.getCallingClassName() + ".log";
        }        
        if( logDir ==null || logDir.length()==0){
        	logDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
        }
        
		
 
        try {
        	Table tblInfo = com.olf.openjvs.Ref.getInfo();
        	String hostName = tblInfo.getString("hostname", 1);
        	int sessionID = tblInfo.getInt("session_sequence", 1);
        	logger.setServerName (hostName);
        	logger.setSessionID (sessionID);
            
        	logger.setUserName(Ref.getUserName());        	
	        logger.setProcessID(Ref.getProcessId());	
	        if (tblInfo != null) {
				tblInfo.destroy();	
			}
	        
	        logger.secondsPastMidnight = System.currentTimeMillis();
	        logger.setThreadName (Thread.currentThread().getName());
	        
		} catch (OException e) {
			 
			e.printStackTrace();
		}
        
        logger.setFilePathName(logDir + '\\' + logFile);
        
        logger.setLogLevel (logLevel.toUpperCase()); 
        
        if (logger.getThisLogLevel()>= LogLevelType.INFO_TYPE.getLogLevelID()){
        	OConsole.print("\n");
        	String startString = getStartString();
	        info("Process started. " + startString);
        } else {
        	OConsole.print("\n");
        }
        logMap.get(level).initRan = true;
	}


	
	  
 	  public static String getLogPath() { 
		  return Paths.get(logMap.get(level).getFilePathName()).toString().replace("\\", "/"); 
	  }


 	  
	  public static String getLogDir() { 
		  return Paths.get(logMap.get(level).getFilePathName()).getParent().toString().replace("\\", "/");
	  }
	  
	  
//	public static void init(String logLevel, String logDir, String logFile) throws Exception {
//		boolean lastInitialized = _initialized;
//		String lastLogDir = _logDir;
//		String lastLogFile = _logFile;
//		String lastLogPath = _logPath;
//		String lastLogLevel = _logLevel;
//		String lastExecuter = _executer;
//		boolean lastError = _error;
//
//
//		boolean revertChanges = false;
//
//
//
//
//		try {
//			_error = false;
//			_initialized = true;
//
//			logStart(_executer);
//		}  catch (IOException ioe) {
//
//			revertChanges = true;
//			tryOPrint(ioe.getMessage());
//			throw new Exception(ioe.getMessage());
//		} finally {
//
//			if (revertChanges) {
//				_initialized = lastInitialized;
//				_logDir = lastLogDir;
//				_logFile = lastLogFile;
//				_logPath = lastLogPath;
//				_logLevel = lastLogLevel;
//				_executer = lastExecuter;
//				_error = lastError;
//				tryOPrint("Caused by errors during initialization all changes had been reverted.");
//			} else {
//
//				String friendlyLogPath = _logPath;
//				if (friendlyLogPath != null && (friendlyLogPath = friendlyLogPath.trim()).length() > 0) {
//					friendlyLogPath = friendlyLogPath.replace('/', '\\');
//				}
//				if (_logLevel.equalsIgnoreCase("debug") || _logLevel.equalsIgnoreCase("info")) {
//					tryOPrint("Logging with level \"" + _logLevel + "\" to file: " + friendlyLogPath);
//				}
//
//				if (logLevel == null || logLevel.trim().length() == 0) {
//					tryOPrint("PluginLog: 'logLevel' should not be null or empty. Default log level will be used."); 
//				} 
//				if (logLevel != null && !logLevel.trim().equalsIgnoreCase(_logLevel)) {
//					tryOPrint("PluginLog: Couldn't map argument 'logLevel': '" + logLevel + "' to any supported level. Default log level will be used.");
//				}
//			} 
//		} 
//	}


}
