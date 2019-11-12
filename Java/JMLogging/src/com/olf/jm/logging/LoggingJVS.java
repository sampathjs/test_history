package com.olf.jm.logging;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
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
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class LoggingJVS extends LoggingBase{


	  
    /**
     * Private no-arg constructor
     */
    

	 public static void init(Session session, Class<?> plugin, String context, String subcontext) {
		throw new RuntimeException("Error initialising logging. Using incorrect loader");
		
	}




	
    
    /**
     * Initialise logging which should be done by the main entry class.
     * 
     * @param session Plugin session
     * @param plugin Class of main entry plugin
     * @param Constants repository context
     * @param Constants repository sub-context
     */
	 
	public static void init(Class<?> plugin, String context, String subcontext) { 
        // Levels handle multiple initialisations via nested plugins
        // Each level gets it's own logger which logs to it's own log file
        // Each initialisation increments the level and a close decrements the level
        level++;
        LoggingJVS logger = new LoggingJVS();
        logMap.put(level, logger);

        String logLevel = "Error";
        String logFile = plugin.getSimpleName() + ".log";
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
            logger.setSessionID (sessionID);
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

	
	


	
	


}
