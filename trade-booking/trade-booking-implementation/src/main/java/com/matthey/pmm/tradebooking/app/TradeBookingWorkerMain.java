package com.matthey.pmm.tradebooking.app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import com.matthey.pmm.tradebooking.processors.FileProcessor;
import com.matthey.pmm.tradebooking.processors.RunRemoteProcessor;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;


@ScriptCategory({ EnumScriptCategory.Generic })
public class TradeBookingWorkerMain extends AbstractGenericScript {
    private static Logger logger = null;
	
	public static final String CONST_REPO_CONTEXT = "DealBooking";
	public static final String CONST_REPO_SUBCONTEXT = "WorkerScript";	
	
	private ConstRepository constRepo;
	
	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(TradeBookingWorkerMain.class);
		}
		return logger;
	}
	
    /**
     * @param context
     *            the current script context
     * @param params
     *            the input parameters
     * @return a <code>Table</code> or null
     */
	@Override
    public Table execute(final Session session, final ConstTable params) {
    	init(session);
    	try {
    		String sql = getSqlForUnprocessedProcessLogRows();
    		try (UserTable runLogUserTable = session.getIOFactory().getUserTable(RunRemoteProcessor.USER_TABLE_RUN_LOG);
    			 UserTable processLogUserTable = session.getIOFactory().getUserTable(RunRemoteProcessor.USER_TABLE_PROCESS_LOG);
    			 Table itemToProcess = session.getIOFactory().runSQL(sql)) {
    			for (int row=itemToProcess.getRowCount()-1; row >= 0; row--) {
    				int runId = itemToProcess.getInt(RunRemoteProcessor.COL_RUN_ID, row);
    				int dealCounter = itemToProcess.getInt(RunRemoteProcessor.COL_DEAL_COUNTER, row);
    				String fullPath = itemToProcess.getString(RunRemoteProcessor.COL_INPUT_FILE_PATH, row);
    				try (Table runLogTable = getRunLogTable(session, runId);) {
    					FileProcessor fileProcessor = new FileProcessor(session, constRepo, runId, dealCounter);
        	    		getLogger().info("Processing file '" + fullPath + "' now.");
        	    		int failedDealCounter = getFailedDealCountForRun(session, runId);
        	    		int openDealsForRun = getOpenDealsForRun (session, runId);
        	    		boolean success;
        	    		String failReason = "";
        	    		
        	    		try {
        		    		success = fileProcessor.processFile(fullPath);
        	    		} catch (Throwable t) {
        	    			success = false;
        	    			getLogger().error("Error while processing file '" + fullPath + "': " + t.toString());
        	        		StringWriter sw = new StringWriter(4000);
        	        		PrintWriter pw = new PrintWriter(sw);
        	        		t.printStackTrace(pw);
        	        		getLogger().error(sw.toString());
        	        		failReason = t.toString();
        	    		}
        	    		boolean overallSuccess = success && (failedDealCounter == 0);
        	    		if (overallSuccess) {
        		    		runLogTable.setString (RunRemoteProcessor.COL_STATUS, 0, "Processing deal #" + dealCounter + " finished successfully");
        	    			getLogger().info("Processing of ' " + fullPath + "' finished successfully");
        	    			itemToProcess.setString(RunRemoteProcessor.COL_OVERALL_STATUS, row, "Finished Successfully");
        	    			itemToProcess.setInt(RunRemoteProcessor.COL_DEAL_TRACKING_NUM, row, fileProcessor.getLatestDealTrackingNum());
        	    		} else {
        	    			failedDealCounter++;
        		    		runLogTable.setString (RunRemoteProcessor.COL_STATUS, 0, "Processing deal #" + dealCounter + " failed");
        		    		itemToProcess.setString (RunRemoteProcessor.COL_OVERALL_STATUS, row, "Failed. " + failReason);
        	    			getLogger().error("Processing of ' " + fullPath + "' failed");
        	    		}
        	    		if (openDealsForRun == 1) {
        	    			getLogger().error("All deals for run #" + runId + " have been processed.");
        	    	    	if (overallSuccess) {
        	    	    		runLogTable.setString (RunRemoteProcessor.COL_STATUS, 0, "Finished processing of all deals of run successfully");
        	    	    	} else {
        	    	    		if (failedDealCounter < dealCounter) {
        	    		    		runLogTable.setString (RunRemoteProcessor.COL_STATUS, 0, "Finished processing of all deals of run. " + failedDealCounter + " of "
        	    		    			+ dealCounter + " deals failed to be booked.");
        	    	    		} else {
        	    		    		runLogTable.setString (RunRemoteProcessor.COL_STATUS, 0, "Finished processing of all deals of run. All deals failed to be booked.");	    			    				    			
        	    	    		}
        	    	    	}
        	    		}
        		    	runLogTable.setDate(RunRemoteProcessor.COL_END_DATE, 0, new Date());
        				runLogUserTable.updateRows(runLogTable, RunRemoteProcessor.COL_RUN_ID);	
        	    		itemToProcess.setDate(RunRemoteProcessor.COL_LAST_UPDATE, row, new Date());
        				runLogUserTable.updateRows(runLogTable, RunRemoteProcessor.COL_RUN_ID);
        				processLogUserTable.updateRows(itemToProcess, RunRemoteProcessor.COL_RUN_ID + ", " + RunRemoteProcessor.COL_DEAL_COUNTER);
    				}
    			}
    		}
    		return null;    		
    	} catch (Exception ex) {
    		getLogger().error("Deal Booking Process Failed: " + ex.toString() + "\n " + ex.getMessage());
    		StringWriter sw = new StringWriter(4000);
    		PrintWriter pw = new PrintWriter(sw);
    		ex.printStackTrace(pw);
    		logger.error(sw.toString());
    		throw ex;
    	}
    }	
	private int getOpenDealsForRun(Session session, int runId) {
		String sql = "\nSELECT COUNT (l." + RunRemoteProcessor.COL_OVERALL_STATUS + ")"
				+ "\nFROM " + RunRemoteProcessor.USER_TABLE_PROCESS_LOG + " l"
				+ "\nWHERE l." + RunRemoteProcessor.COL_RUN_ID + " = " + runId
				+ "\n AND l." + RunRemoteProcessor.COL_OVERALL_STATUS + " = '" + RunRemoteProcessor.JOB_LOG_INIT_STATUS + "'"
				;
		getLogger().info("Executing SQL to number of onprocessed deals for run: " + sql);
		try (Table sqlResult =  session.getIOFactory().runSQL(sql)) {
			getLogger().info ("Number of unprocessed rows: " + sqlResult.getInt(0, 0));
			return sqlResult.getInt(0, 0);
		}
	}

	private int getFailedDealCountForRun(Session session, int runId) {
		String sql = "\nSELECT COUNT (l." + RunRemoteProcessor.COL_OVERALL_STATUS + ")" 
				+ "\nFROM " + RunRemoteProcessor.USER_TABLE_PROCESS_LOG + " l"
				+ "\nWHERE l." + RunRemoteProcessor.COL_RUN_ID + " = " + runId
				+ "\n AND l." + RunRemoteProcessor.COL_OVERALL_STATUS + " LIKE 'Failed.%'" 
				;
		getLogger().info("Executing SQL to get number of failed bookings for run: " + sql);
		try (Table sqlResult =  session.getIOFactory().runSQL(sql)) {
			getLogger().info ("Number of failed runs: " + sqlResult.getInt(0, 0));
			return sqlResult.getInt(0, 0);
		}
	}

	private Table getRunLogTable(Session session, int runId) {
		String sql = "\nSELECT * FROM " + RunRemoteProcessor.USER_TABLE_RUN_LOG 
				+ "\nWHERE " + RunRemoteProcessor.COL_RUN_ID + " = " + runId 
				;
		getLogger().info("Executing SQL to get run log table row for run: " + sql);
		return session.getIOFactory().runSQL(sql);
	}

	private String getSqlForUnprocessedProcessLogRows() {
		String sql = "\nSELECT j." + RunRemoteProcessor.COL_RUN_ID
				+	 "\n  ,j." + RunRemoteProcessor.COL_DEAL_COUNTER
				+    "\n  ,j." + RunRemoteProcessor.COL_INPUT_FILE_PATH
				+    "\n  ,j." + RunRemoteProcessor.COL_OVERALL_STATUS
				+    "\n  ,j." + RunRemoteProcessor.COL_DEAL_TRACKING_NUM
				+    "\n  ,j." + RunRemoteProcessor.COL_LAST_UPDATE
				+    "\nFROM " + RunRemoteProcessor.USER_TABLE_PROCESS_LOG + " j"
				+    "\nWHERE j." + RunRemoteProcessor.COL_OVERALL_STATUS + " = '" + RunRemoteProcessor.JOB_LOG_INIT_STATUS + "'"
				+    "\n  AND j." + RunRemoteProcessor.COL_DEAL_TRACKING_NUM + " = -1"
				+    "\nORDER BY j." + RunRemoteProcessor.COL_RUN_ID + ", j." + RunRemoteProcessor.COL_DEAL_COUNTER
				;
		return sql;
	}

	/**
	 * Inits plugin log by retrieving logger settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
		try {
			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);			
		} catch (OException e) {
			throw new RuntimeException (e);
		}		


		String abOutdir = session.getSystemVariable("AB_OUTDIR");

		initLog4J(abOutdir);
		
		getLogger().info("\n\n********************* Start of new run ***************************");

		try {
			UIManager.setLookAndFeel( // for dialogs that are used in pre process runs
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException (e);
		} catch (InstantiationException e) {
			throw new RuntimeException (e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException (e);
		} catch (UnsupportedLookAndFeelException e) {
			throw new RuntimeException (e);
		}
	}

	private void initLog4J(String abOutdir) {	
		ConfigurationBuilder< BuiltConfiguration > builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		
		builder.setStatusLevel( Level.INFO);
		builder.setConfigurationName("RollingBuilder");
		// create a console appender
		AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
		    ConsoleAppender.Target.SYSTEM_OUT);
		appenderBuilder.add(builder.newLayout("PatternLayout")
		    .addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss.SSS}{UTC} [%t] %-5level %logger{36} - %msg%n"));
		builder.add( appenderBuilder );
		// create a rolling file appender
		LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
		    .addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss.SSS}{UTC} [%t] %-5level %logger{36} - %msg%n");
		ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
			    .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "50M"));
		
		appenderBuilder = builder.newAppender("rolling", "RollingFile")
		    .addAttribute("fileName", abOutdir + "/Logs/trade-booking.log")
		    .addAttribute("filePattern", abOutdir + "/Logs/trade-booking.%i.log.zip")
		    .add(layoutBuilder)
		    .addComponent(triggeringPolicy);
		builder.add(appenderBuilder);
		// create the new logger
		builder.add( builder.newLogger( "com.matthey.pmm.tradebooking", Level.INFO )
		    .add( builder.newAppenderRef( "rolling" ) )
		    .addAttribute( "additivity", false ) );
		
		builder.add( builder.newRootLogger( Level.INFO )
		    .add( builder.newAppenderRef( "rolling" ) ) );
		LoggerContext ctx = Configurator.initialize(builder.build());
		Configurator.setLevel("com.matthey.pmm.tradebooking", Level.INFO);
	}
}
