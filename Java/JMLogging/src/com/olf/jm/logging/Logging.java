package com.olf.jm.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.olf.openjvs.Ref;
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
public class Logging  extends LoggingBase {

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static HashMap<Integer, Logging> logMap = new HashMap<>();
    private static int level;
    
    /**
     * Private no-arg constructor
     */
    private  Logging() {}
    
	public static void init(Class<?> plugin, String context, String subcontext) { 
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
    public static void init(Session session, Class<?> plugin, String context, String subcontext) {
        // Levels handle multiple initialisations via nested plugins
        // Each level gets it's own logger which logs to it's own log file
        // Each initialisation increments the level and a close decrements the level
        level++;
        Logging logger = new Logging();
        logMap.put(level, logger);

        String logLevel = "Error";
        String logFile = plugin.getSimpleName() + ".log";
        String logDir = null;
 
		try {
    		ConstRepository constRep = new ConstRepository(context, subcontext);
    
    		logLevel = constRep.getStringValue("logLevel", logLevel);
    		logFile = constRep.getStringValue("logFile", logFile);
    		logDir = constRep.getStringValue("logDir", session.getIOFactory().getReportDirectory());
    	} catch (Exception e) {
    		throw new RuntimeException("Error initialising logging. ", e);
		}
 
		logger.setServerName (session.getHostName());
        logger.setSessionID (session.getSessionId());
        logger.setUserName(session.getUser().getName());
        logger.setProcessID(session.getProcessId());
        
        logger.setFilePathName(logDir + '/' + logFile);

        logMap.get(level).secondsPastMidnight = System.currentTimeMillis();
        logger.setThreadName (Thread.currentThread().getName());

        
        info("Process started.");
    }

   
}
