package com.matthey.openlink.stamping;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public abstract class StampLedger extends AbstractGenericScript {

    protected static final String CONST_REPO_CONTEXT = "BackOffice";
    protected Context context = null;
    protected String taskName = null;
    protected ConstRepository constRepo =  null;

    @SuppressWarnings("serial")
    protected static final Map<LedgerStatus, LedgerStatus> CancelLedger = Collections.unmodifiableMap(
            new HashMap<LedgerStatus, LedgerStatus>() {
                {
                    put(LedgerStatus.PENDING_SENT, LedgerStatus.NOT_SENT);
                    put(LedgerStatus.SENT, LedgerStatus.PENDING_CANCELLED);
                }
            });
    
    /**
     * Initialise task name.
     */
    private void initialiseTaskName() {
        taskName = context.getTaskName();
        if(null == taskName || taskName.isEmpty()) {
            throw new StampingException(String.format("Task name could not be determined. The plugin [%s] should be called from a task." 
                    , this.getClass().getSimpleName()));
        }
    }

    /**
     * Initialise constant repository.
     */
    private void initialiseConstRepository() {
    	try {
    		if(null == constRepo) {
    			constRepo = new ConstRepository(CONST_REPO_CONTEXT, taskName);
    		}
    	} catch (OException oe) {
    		throw new StampingException(String.format("Failed to initialise constant repository [%s,%s]. An exception occured - %s"
    				,CONST_REPO_CONTEXT ,taskName ,oe.getMessage()));
    	}
    }
    
    /**
     * Initialise plugin log.
     */
    private void initialisePluginLog() {
        try {
            String logLevel = constRepo.getStringValue("logLevel", "Info"); 
            String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
            String abOutdir = context.getSystemSetting("AB_OUTDIR");
            String logDir = constRepo.getStringValue("logDir", abOutdir);
            PluginLog.init(logLevel, logDir, logFile);

        } catch (Exception ex) {
            throw new StampingException(String.format("An exception occurred while initialising plugin log. %s", ex.getMessage()), ex);
        }       
    }
    
    /**
     * Set the current context and initialise.
     *
     * @param context the context
     */
    protected void initialise(Context context) {
    	this.context = context;
    	initialiseTaskName();
    	initialiseConstRepository();
    	initialisePluginLog();
    }
    
    
	/**
	 * Gets the string config value from the constant repository.
	 *
	 * @param variableName the variable name
	 * @return the string config
	 */
	protected String getStringConfig(String variableName) {
		String fieldValue;
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, taskName);
			fieldValue = constRepo.getStringValue(variableName);
		} catch (OException oe) {
			throw new StampingException(String.format("Failed to retrieve %s value from constant repository. %s", variableName, oe.getMessage()));
		}
		return fieldValue; 
	}
	
	/**
	 * Gets the date time config value formatted for Db access from the constant repository.
	 *
	 * @param variableName the variable name
	 * @return the string config
	 */
	protected String getDateTimeConfig(String variableName) {
		String fieldValue;
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, taskName);
			fieldValue = constRepo.getDateTimeValue(variableName).formatForDbAccess();
		} catch (OException oe) {
			throw new StampingException(String.format("Failed to retrieve %s value from constant repository. %s", variableName, oe.getMessage()));
		}
		return fieldValue; 
	}
	
	/**
	 * Gets the output file name.
	 *
	 * @param subContext the sub context
	 * @return the output file name
	 */
	private String addDateTimePrefix(String fileName) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
		return String.format("%s-%s",formatter.format(new Date()),fileName);
	}
	
	/**
	 * Write output log to a file.
	 *
	 * @param directoryName the directory name
	 * @param fileName the file name
	 * @param fileContent the file content
	 */
	protected void writeOutputLog(String directoryName, String fileName, String fileContent ) {
		fileName = addDateTimePrefix(fileName);
		PluginLog.info("Started generating output file " + fileName + " under directory " + directoryName);

		directoryName = directoryName.trim();
		File directory = new File(directoryName);
		if (!directory.exists())
		{
			PluginLog.info("Directory does not exist. Path: " + directory);

			if (directory.mkdirs())
			{
				PluginLog.debug("Directories created successfully. Path: " + directory.toPath());
			}
			else
			{
				String errorMessage = "Directories not created. Path: " + directory.toPath();
				PluginLog.error(errorMessage);
				throw new StampingException(errorMessage);
			}
		}
		
		File file = new File(directoryName + File.separator + fileName);
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(fileContent);
		} catch (IOException e) {
			throw new StampingException("An exception occured while writing to file.", e);
		}
		
		PluginLog.info("Completed generating output file.");
	}
}