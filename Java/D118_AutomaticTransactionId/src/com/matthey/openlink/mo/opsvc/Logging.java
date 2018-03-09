package com.matthey.openlink.mo.opsvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.openrisk.application.Session;
import com.openlink.util.constrepository.ConstRepository;

/**
 * 
 * Class used for logging.
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
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class Logging {

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static Logging _instance = new Logging();
    private String userName;
    private String className;
    private String logfile;
    
    /**
     * Private no-arg constructor
     */
    private Logging() {}
    
    /**
     * Initialise logging which should be done by the main entry class.
     * 
     * @param session Plugin session
     * @param plugin Class of main entry plugin
     * @param Constants repository context
     * @param Constants repository sub-context
     */
    public static void init(Session session, Class<?> plugin, String context, String subcontext) {
        if (_instance != null) {
            _instance = new Logging();

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
 
            _instance.userName = session.getUser().getName();
            _instance.className = plugin.getSimpleName();
            _instance.logfile = logDir + '/' + logFile;
            
            Logging.info("Process started.");
        }
    }

    /**
     * Log info message.
     * 
     * @param message Log message
     */
    public static void info(String message) {
        _instance.logMessage("INFO", message, null);
    }

    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     */
    public static void error(String message, Throwable exc) {
        _instance.logMessage("ERROR", message, exc);
    }

    /**
     * Log message to log file.
     * 
     * @param level Log level
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     */
    private void logMessage(String level, String message, Throwable exc) {

    	String stackTrace = null;
        if (exc != null) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 PrintStream ps = new PrintStream(baos)) {
                   exc.printStackTrace(ps);
                   stackTrace = baos.toString();
               } catch (IOException e) {
                   e.printStackTrace();
               }
        }

    	try {
            Files.write(Paths.get(logfile), (getPrefix(level) + message + '\n').getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (stackTrace != null) {
                Files.write(Paths.get(logfile), stackTrace.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
     */
    private String getPrefix(String level) {
        return sdf.format(new Date()) + " | " + className + " | " + level + " | " + userName + " | ";
    }
}
