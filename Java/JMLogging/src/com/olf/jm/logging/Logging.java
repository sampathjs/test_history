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
public class Logging {

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static HashMap<Integer, Logging> logMap = new HashMap<>();
    private static int level;
    private String serverName;
    private String userName;
    private String sessionId;
    private String processId;
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
      try {
        Logging logger = logInitialiser(plugin, context, subcontext);
        logger.serverName = String.format("%1$-16.16s", session.getHostName());
        logger.userName = String.format("%1$-10.10s", session.getUser().getName());
        logger.sessionId = String.format("S%1$-5.5s", session.getSessionId());
        logger.processId = String.format("P%1$-5.5s", session.getProcessId());
        Logging.info("Process started.");
      } catch (Exception e) {
        throw new RuntimeException("Error initialising logging. ", e);
      }

    }

    /**
     * Initialise logging to support both OpenJVS & OpenComponent
     * 
     *
     * @param plugin
     *            Class of main entry plugin
     * @param Constants
     *            repository context
     * @param Constants
     *            repository sub-context
     */
    public static void init(Class<?> plugin, String context, String subcontext) {
      try {
        Logging logger = logInitialiser(plugin, context, subcontext);
        logger.serverName = String.format("%1$-16.16s", Ref.getInfo().getString("hostname", 1));
        logger.userName = String.format("%1$-10.10s", Ref.getUserName());
        logger.sessionId = String.format("S%1$-5.5s", Ref.getInfo().getInt("session_sequence", 1));
        logger.processId = String.format("P%1$-5.5s", Ref.getProcessId());
        Logging.info("Process started.");
      } catch (Exception e) {
        throw new RuntimeException("Error initialising logging. ", e);
      }
    }

    /**
     * Log info message.
     * 
     * @param message
     *            Log message
     * @param args
     *            Arguments for formatted message
     */
    public static void info(String message, Object...args) {
        logMap.get(level).logMessage("INFO", message, null, args);
    }
	/**
	 * Log debug message.
	 * 
	 * @param message
	 *            Log message
	 * @param args
	 *            Arguments for formatted message
	 */
	public static void debug(String message, Object... args) {
		logMap.get(level).logMessage("DEBUG", message, null, args);
	}

	/**
	 * Log warning message.
	 * 
	 * @param message
	 *            Log message
	 * @param args
	 *            Arguments for formatted message
	 */
	public static void warn(String message, Object... args) {
		logMap.get(level).logMessage("WARN", message, null, args);
	}

    /**
     * Log error message.
     * 
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    public static void error(String message, Throwable exc, Object...args) {
        logMap.get(level).logMessage("ERROR", message, exc, args);
    }

    /**
     * Log message to log file.
     * 
     * @param level Log level
     * @param message Log message
     * @param exc Exception to log stack trace (can be null)
     * @param args Arguments for formatted message
     */
    private void logMessage(String level, String message, Throwable exc, Object...args) {

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
            String logMessage = message;
            if (args.length > 0) {
                logMessage = String.format(message, args);
            }
            Files.write(Paths.get(logfile), (getPrefix(level) + logMessage + '\n').getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
        String fLevel = String.format("%1$-5s", level).substring(0, 5);
        return sdf.format(new Date()) +
                " | " + serverName +
                " | " + sessionId +
                " | " + processId +
                " | " + getCallingClassName() +
                " | " + userName +
                " | " + fLevel +
                " | ";
    }

    /**
     * @return class and method that is doing logging
     */
    private String getCallingClassName() {

        String callingClassName = null;

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

                callingClassName = Class.forName(callingClassName).getSimpleName();
                
                break;
            }
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return String.format("%1$-32s", callingClassName).substring(0, 32);
    }

    /**
     * Close the logging session.
     */
    public static void close() {
        Logging.info("Process finished");
        logMap.remove(level);
        level--;
    }
    private static Logging logInitialiser(Class<?> plugin, String context, String subcontext) {
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
        logDir = constRep.getStringValue("logDir", logDir);
        logger.logfile = logDir + '/' + logFile;
        return logger;
      } catch (Exception e) {
        throw new RuntimeException("Error initialising logging. ", e);
      }

    }
}
