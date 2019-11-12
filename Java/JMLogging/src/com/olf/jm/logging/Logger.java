package com.olf.jm.logging;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.openlink.util.constrepository.ConstRepository;



public final class Logger extends LoggingBase {
	



	public static Logger logger = new Logger();
	
	private static void init(String pluginName, String context, String subcontext) { 
        // Levels handle multiple initialisations via nested plugins
        // Each level gets it's own logger which logs to it's own log file
        // Each initialisation increments the level and a close decrements the level
        level++;
        Logger logger = new Logger();
        logMap.put(level, logger);

        String logLevel = "Error";
        String logFile = pluginName + ".log";
        String logDir = null;
 
		try {
    		ConstRepository constRep = new ConstRepository(context, subcontext);
    
    		logLevel = constRep.getStringValue("logLevel", logLevel);
    		logFile = constRep.getStringValue("logFile", logFile);
    		logDir = constRep.getStringValue("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs");
    	} catch (Exception e) {
    		throw new RuntimeException("Error initialising JVS logging. ", e);
		}
 
        try {
        	Table tblInfo = com.olf.openjvs.Ref.getInfo();
        	String hostName = tblInfo.getString("hostname", 1);
        	int sessionID = tblInfo.getInt("session_sequence", 1);
        	logger.setServerName (hostName);
            logger.setSessionID(sessionID);
            
            logger.setUserName(Ref.getUserName());
            logger.setProcessID(Ref.getProcessId());
	        	
	        if (tblInfo != null) {
				tblInfo.destroy();	
			}
	        
	        logMap.get(level).secondsPastMidnight = System.currentTimeMillis();
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


 

  
  public static void log(LogLevel level, LogCategory category, Object callingClass, String message) { 
	  log(level, category, callingClass, message, null); 
  }




  public static void log(LogLevel thislevel, LogCategory category, Object callingClass, String message, Throwable throwable) {

	  
	  if (level==0){
		  init (callingClass.getClass().getSimpleName(), "Logger",category.toString() );
	  }
			  

	  switch (thislevel) {
	  default:
		  info(message, throwable);
		  return;
	  case DEBUG:
		  debug(message, throwable);
		  return;
	  case WARNING:
		  warn(message, throwable);
		  return;
	  case ERROR:
		  error(message, throwable); 
		  return;
	  case FATAL:
		  fatal(message, throwable);
		  break;
	  }  
  }
  
}
