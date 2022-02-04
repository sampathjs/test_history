package com.matthey.pmm.tradebooking.app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

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

import com.matthey.pmm.tradebooking.processors.RunRemoteProcessor;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;


@ScriptCategory({ EnumScriptCategory.Generic })
public class TradeBookingMain extends AbstractGenericScript {
    private static Logger logger = null;
	
	public static final String CONST_REPO_CONTEXT = "DealBooking";
	public static final String CONST_REPO_SUBCONTEXT = "MainScript";	
	
	private ConstRepository constRepo;
	
	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(TradeBookingMain.class);
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
        	if (!checkParamTable(params)) {
        		return null;
        	}
        	List<String> files = params.getTable("Files", 0).getRows().stream().map(x -> x.getString("filename")).collect(Collectors.toList());
        	getLogger().info("Processing the following files: " + files);
        	RunRemoteProcessor runProcessor = new RunRemoteProcessor(session, constRepo, params.getString("Client", 0), files);
        	runProcessor.processRun();
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
    
	private boolean checkParamTable(ConstTable table) {
		if (table.getRowCount() != 1) {
			getLogger().error("Row count on parameter table is " + table.getRowCount() + " expected is exactly 1");
			return false;
		}
		if (table.getColumnCount() != 3) {
			getLogger().error("Column count on parameter table is " + table.getColumnCount() + " expected is exactly 3");
			return false;
		}
		if (!table.isValidColumn("Succeeded")) {
			getLogger().error("Column 'Succeeded' on parameter table is not valid");
			return false;
		}
		
		if (!table.isValidColumn("Files")) {
			getLogger().error("Column 'Files' on parameter table is not valid");
			return false;
		}

		if (!table.isValidColumn("Client")) {
			getLogger().error("Column 'Client' on parameter table is not valid");
			return false;
		}
		
		if (table.getColumn("Succeeded").getType() != EnumColType.Int) {
			getLogger().error("Column 'Succeeded' on parameter table is not of type Int");			
			return false;
		}

		if (table.getColumn("Files").getType() != EnumColType.Table) {
			getLogger().error("Column 'Files' on parameter table is not of type Table");			
			return false;
		}

		if (table.getColumn("Client").getType() != EnumColType.String) {
			getLogger().error("Column 'Client' on parameter table is not of type String");			
			return false;
		}

		if (table.getInt("Succeeded", 0) != 1) {
			getLogger().info("Nothing to process as param script did not succeed or was cancelled");
			return false;
		}

		if (!table.getTable("Files", 0).isValidColumn("filename")) {
			getLogger().error("Column 'filename' on subtable 'Files' on the parameter table is not valid");			
			return false;
		}

		if (table.getTable("Files", 0).getColumn("filename").getType() != EnumColType.String) {
			getLogger().error("Column 'filename' on subtable 'Files' on the parameter table is not of type String");			
			return false;
		}
		
		if (table.getTable("Files", 0).getRowCount() == 0) {
			getLogger().info("Nothing to process as no files to process have been specifiedin the param script");			
			return false;
		}
		getLogger().info("Check of parameter table succeeded without issues found");
		return true;
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
