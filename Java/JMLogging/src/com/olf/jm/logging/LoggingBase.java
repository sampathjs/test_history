package com.olf.jm.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openrisk.application.Session;
import com.openlink.util.constrepository.ConstRepository;

/**
 * 
 * Class used for logging. Logger must be initialised via the {@link #init(Session, Class, String, String)} method and, once finished
 * with,  be closed via the {@link #close()} method.
 *  
 * @author G. Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 05-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 13-Nov-2015 |               | G. Moore        | Converted to use PluginLog.                                                     |
 * | 003 | 20-Nov-2015 |               | G. Moore        | Moved away from PluginLog as logging was not consistent.                        |
 * | 004 | 26-Nov-2015 |               | G. Moore        | Now in it's own library project and further enhancements.                       |
 * | 005 | 26-Nov-2019 |            
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public abstract class LoggingBase {
	 
//	public abstract static void internalInit(String context, String subcontext) throws Exception ;
	
	public final static String LOG_LEVEL_FATAL = "FATAL";
	public final static String LOG_LEVEL_ERROR = "ERROR";
	public final static String LOG_LEVEL_WARN = "WARN";
	public final static String LOG_LEVEL_INFO = "INFO";
	public final static String LOG_LEVEL_DEBUG = "DEBUG";
	
	
    private  static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    protected  static HashMap<Integer, LoggingBase> logMap = new HashMap<>();
    protected  static int level;
    private String serverName;
    private String userName;
    private String sessionId;
    private String processId;
    private String logfile;
    private String callingClassName;
    private String callingPackageName;
    private String threadName;
    private String callerMethodName = "";
    private String callerLineNumber = "";

    protected  LogLevelType logLevel;
    protected long secondsPastMidnight;     
    protected int countError=0;
    protected boolean closedAlready =false;
    protected boolean initRan =false;
    
    private static DATE_FORMAT _dateFormat = DATE_FORMAT.DATE_FORMAT_MINIMAL;
    private static DATE_LOCALE _dateLocale = DATE_LOCALE.DATE_LOCALE_US;
  
    public enum LogLevelType {
    	
    	FATAL_TYPE(LOG_LEVEL_FATAL,1)		{		},
    	ERROR_TYPE(LOG_LEVEL_ERROR,2)		{		},
		WARN_TYPE(LOG_LEVEL_WARN,  3)		{		},
		INFO_TYPE(LOG_LEVEL_INFO,  4)		{		},
		DEBUG_TYPE(LOG_LEVEL_DEBUG,5)		{		};
		
		private String _logLevelTypeName;
		private int _logLevelID;

		private LogLevelType(String logLevelTypeName, int logLevelTypeID) {
			_logLevelTypeName = logLevelTypeName;
			_logLevelID = logLevelTypeID;
		}

		public String getLogLevel() {
			return _logLevelTypeName;
		}
		public int getLogLevelID() {
			return _logLevelID;
		}


	}
	
    /**
     * Private no-arg constructor
     */
    public LoggingBase() {}

    protected void setServerName(String thisServerName){
    	serverName = String.format("%1$-16.16s",thisServerName); 
    }
    protected void setSessionID(int thisSessionID){
    	sessionId = String.format("S%1$-5.5s",thisSessionID); 
    }
    protected void setUserName(String thisUserName){
    	userName= String.format("%1$-10.10s",thisUserName); 
    }
    protected void setProcessID(int thisprocessId){
    	processId = String.format("P%1$-5.5s",thisprocessId);   
    }
    protected void setThreadName(String thisThreadName){
    	threadName= String.format("%1$-8.8s",thisThreadName); 
    }
    protected void setFilePathName(String thisPathName){
    	logfile = thisPathName; 
    }
    protected String getFilePathName(){
    	return logfile; 
    }
    protected String getCallingClassName(){
    	return callingClassName; 
    }
    
    

    /**
     * Log info message.
     * 
     * @param message Log message
     * @param args Arguments for formatted message
     */
    public static void fatal(String message, Object...args) {
    	logMap.get(level).countError++;
    	logMap.get(level).logMessage(LogLevelType.FATAL_TYPE, message, null, args);
    }


	/**
     * Log info message.
     * 
     * @param message Log message
     * @param args Arguments for formatted message
     */
    public static void error(String message, Object...args) {
    	logMap.get(level).countError++;
    	logMap.get(level).logMessage(LogLevelType.ERROR_TYPE, message, null, args);
    }
    
    /**
     * Log info message.
     * 
     * @param message Log message
     * @param args Arguments for formatted message
     */
    public static void warn(String message, Object...args) {
    	logMap.get(level).logMessage(LogLevelType.WARN_TYPE, message, null, args);
    }

 
	
    /**
     * Log info message.
     * 
     * @param message Log message
     * @param args Arguments for formatted message
     */
    public static void info(String message, Object...args) {
    	logMap.get(level).logMessage(LogLevelType.INFO_TYPE, message, null, args);
    }
    
    /**
     * Log info message.
     * 
     * @param message Log message
     * @param args Arguments for formatted message
     */
    public static void debug(String message, Object...args) {
    	logMap.get(level).logMessage(LogLevelType.DEBUG_TYPE, message, null, args);
    }
    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    public static void fatal(String message, Throwable exc, Object...args) {
    	logMap.get(level).countError++;
    	logMap.get(level).logMessage(LogLevelType.FATAL_TYPE, message, exc, args);
    }    
    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    public static void error(String message, Throwable exc, Object...args) {
    	logMap.get(level).countError++;
    	logMap.get(level).logMessage(LogLevelType.ERROR_TYPE, message, exc, args);
    }

    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    public static void warn(String message, Throwable exc, Object...args) {
    	logMap.get(level).logMessage(LogLevelType.WARN_TYPE, message, exc, args);
    }


    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    public static void info(String message, Throwable exc, Object...args) {
    	logMap.get(level).logMessage(LogLevelType.INFO_TYPE, message, exc, args);
    }

    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    public static void debug(String message, Throwable exc, Object...args) {
    	logMap.get(level).logMessage(LogLevelType.DEBUG_TYPE, message, exc, args);
    }
//    private static void internalInit() {
//		
//    	try {
//			internalInit ("","");
//		} catch (Exception e) {
//			OConsole.print("Unable to create Log File with details");		// TODO more details 
//			e.printStackTrace();
//		}
//	}
    
//    private static void internalInit(String context, String subcontext) throws Exception { 
//    	// Levels handle multiple initialisations via nested plugins
//        // Each level gets it's own logger which logs to it's own log file
//        // Each initialisation increments the level and a close decrements the level
//        level++;
//        LoggingBase logger = new LoggingBase();
//        logMap.put(level, logger);
//
//        String logLevel = "Error";
//        Caller c = new Caller("execute", new Throwable());
//		
//        logger.setCallingClassName();
//        String logFile = logger.callingClassName + ".log";
//        String logDir = null;
// 
//		try {
//    		ConstRepository constRep = new ConstRepository(context, subcontext);
//    
//    		logLevel = constRep.getStringValue("logLevel", logLevel);
//    		logFile = constRep.getStringValue("logFile", logFile);
//    		logDir = constRep.getStringValue("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs");
//    	} catch (Exception e) {
//    		throw new RuntimeException("Error initialising JVS logging. ", e);
//		}
// 
//        try {
//        	Table tblInfo = com.olf.openjvs.Ref.getInfo();
//        	String hostName = tblInfo.getString("hostname", 1);
//        	int sessionID = tblInfo.getInt("session_sequence", 1);
//        	logger.serverName = String.format("%1$-16.16s", hostName);
//            logger.sessionId = String.format("S%1$-5.5s", sessionID);
//            
//        	logger.userName = String.format("%1$-10.10s", Ref.getUserName());
//	        logger.processId = String.format("P%1$-5.5s", Ref.getProcessId());	
//	        if (tblInfo != null) {
//				tblInfo.destroy();	
//			}
//	        
//	        secondsPastMidnight = System.currentTimeMillis();
//	        logger.threadName = String.format("%1$-8.8s", Thread.currentThread().getName());
//		} catch (OException e) {
//			 
//			e.printStackTrace();
//		}
//        
//        logger.logfile = logDir + '\\' + logFile;
//        logger.setLogLevel (logLevel.toUpperCase()); 
//        
//        if (logger.getLogLevel()>= LogLevelType.INFO_TYPE.getLogLevelID()){
//        	OConsole.print("\n");
//        	String startString = getStartString();
//	        info("Process started. " + startString);
//        } else {
//        	OConsole.print("\n");
//        }
//        initRan = true;
//		
//	}
    
    protected static String getStartString() {
        StringBuffer buffer = new StringBuffer("Business/Trading/Processing/Current date: ");
        try {
			buffer.append(formatJd(Util.getBusinessDate())).append("/");
	        buffer.append(formatJd(Util.getTradingDate())).append("/");
	        buffer.append(formatJd(Util.processingDate())).append("/");
	        buffer.append(formatJd(OCalendar.today()));

		} catch (OException e) {

		}
        
        return buffer.toString();
	}
    
    private static String formatJd(int jd) throws OException { return OCalendar.formatJd(jd, _dateFormat, _dateLocale); }
    
    /**
     * Log message to log file.
     * 
     * @param thisLogLevel Log level
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    protected void logMessage(LogLevelType thisLogLevel, String message, Throwable exc, Object...args) {

    	String stackTrace = null;
        if (exc != null) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 PrintStream ps = new PrintStream(baos)) {
                   exc.printStackTrace(ps);
                   stackTrace = baos.toString();
               } catch (IOException e) {
                   e.printStackTrace();
                   throw new RuntimeException(e);
               }
        }

    	try {
    		if (thisLogLevel._logLevelID<=logLevel._logLevelID){
	    	    String logMessage = message;
	    	    if (args.length > 0) {
	    	        logMessage = String.format(message, args);
	    	    }
	    	    String outputMessage = getPrefix(thisLogLevel.getLogLevel())  + logMessage + '\n';
	            Files.write(Paths.get(logfile), (outputMessage ).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	            OConsole.print(outputMessage);
	            if (stackTrace != null) {
	                Files.write(Paths.get(logfile), stackTrace.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	                OConsole.print(stackTrace + '\n');
	            }
    		}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prefix for each log message consisting of date, time, level and user name.
     * 
     * @param level
     * @return
     * @throws Exception 
     */
    private String getPrefix(String level) {
        String fLevel = String.format("%1$-5s", level).substring(0, 5);
        setCallingClassName();
        try {
			Caller c = new Caller("execute", new Throwable());
			this.callerLineNumber = String.format("LN%1$-5.5s", c.getLineNumber());
			this.callerMethodName = String.format("%1$-40.40s", c.getMethodName());
		} catch (Exception e) {
			 
		}
		
        return sdf.format(new Date()) + " | " + fLevel + " | " + userName + " | " + serverName + " | " + 
        					sessionId + " | " + processId + " | " + threadName + " | " + callingPackageName + " | " + 
        					callingClassName + " | " + callerMethodName + " | " + callerLineNumber + " | ";
    }

    /**
     * @return class and method that is doing logging
     */
    protected void setCallingClassName() {

        String callingClassName = null;
        String packageName = null;
        try {
            StackTraceElement[] ste = new Throwable().fillInStackTrace().getStackTrace();
            for (StackTraceElement element : ste) {
                // If stack trace element is inside an Endur package ignore it and try next class in stack
                if (element.getClassName().startsWith("com.olf") && !element.getClassName().startsWith("com.olf.jm")) {
                    break;
                }

                callingClassName = element.getClassName();

                // If class is an instance of the logging class try the next class in the stack
                if (Class.forName(callingClassName).isInstance(this)) {
                    continue;
                }

                

                packageName = Class.forName(callingClassName).getCanonicalName();
                callingClassName = Class.forName(callingClassName).getSimpleName();
                packageName = packageName.substring(0, packageName.indexOf(callingClassName)-1);
                break;
            }
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.callingClassName =String.format("%1$-30s", callingClassName).substring(0, 30);
        this.callingPackageName=String.format("%1$-80s", packageName).substring(0, 80);
        
        return ;
    }

   
    
    public void setLogLevel(String logLevelVal) {
		
		if (LOG_LEVEL_FATAL.equals(logLevelVal)){
			logLevel = LogLevelType.FATAL_TYPE;
		} else if (LOG_LEVEL_ERROR.equals(logLevelVal)){
			logLevel = LogLevelType.ERROR_TYPE;
		} else if (LOG_LEVEL_WARN.equals(logLevelVal)){
			logLevel = LogLevelType.WARN_TYPE;
		} else if (LOG_LEVEL_INFO.equals(logLevelVal)){
			logLevel = LogLevelType.INFO_TYPE;
		} else if (LOG_LEVEL_DEBUG.equals(logLevelVal)){
			logLevel = LogLevelType.DEBUG_TYPE;
		} else {
			logLevel = LogLevelType.DEBUG_TYPE;
		}
			
	}
    
    public static String getLogLevel(){
    	
    	return logMap.get(level).logLevel.getLogLevel().toLowerCase();
    }
    
    protected int getThisLogLevel(){
    	
    	return logLevel.getLogLevelID();
    }
    
     
 	  public static String getLogPath() { 
		  return Paths.get(logMap.get(level).logfile).toString(); 
	  }

 	  public static String getLogFile() { 
 		 return Paths.get(logMap.get(level).logfile).getFileName().toString(); 
	  }
 	  
	  public static String getLogDir() { 
		  return Paths.get(logMap.get(level).logfile).getParent().toString();
	  }

	  
    private static void displayCloseMessage(String message) {
    	long timeTaken = 0;
    	if (logMap.get(level).secondsPastMidnight>0){
			timeTaken =  (System.currentTimeMillis() - logMap.get(level).secondsPastMidnight)/1000 ;
		}
    	info(String.format(message + " Time(s): %d",timeTaken)); 
	
	}
    
    /**
     * Close the logging session.
     */
    public static void close() {

    	if (!logMap.get(level).closedAlready){
	    	displayCloseMessage ("Process finished.");
	    	
	        removeLevel();
    	}
    }


	private static void removeLevel() {
		logMap.get(level).closedAlready = true;
		logMap.get(level).initRan = false;
		logMap.remove(level);
        level--;
	}
   


	public static void exitWithStatus() {

    	if (logMap.get(level).closedAlready){
    		if (logMap.get(level).countError>0 ) {
    			Util.exitFail();
    		} else {
    			Util.exitSucceed();
    		}
    	} 
    	
    	if (logMap.get(level).countError>0 ) {
    		displayCloseMessage("Exits with status failed."); 
    		removeLevel();
    		Util.exitFail();
    	} else {
    		displayCloseMessage("Exits succeessfully.");
    		removeLevel();
    		Util.exitSucceed();
    	} 
    }






    public static void exitWithStatus(boolean terminate) {
    	if (logMap.get(level).closedAlready){
    		if (terminate) {
        		Util.exitTerminate(logMap.get(level).countError>0 ? 0 : 1);
        	} else {
        		exitWithStatus();
        	} 
    	} 

    	if (terminate) {

    		if (logMap.get(level).countError>0) {
    			displayCloseMessage("Exits with status failed.");
    		} else {
    			displayCloseMessage("Exits with status succeeded.");
    		} 
    		removeLevel();
    		Util.exitTerminate(logMap.get(level).countError>0 ? 0 : 1);
    	} else {
    		if (logMap.get(level).countError>0 ) {
    			removeLevel();
    			Util.exitFail();
    		} else {
    			removeLevel();
    			Util.exitSucceed();
    		}
    	} 
    }
    

    
}
