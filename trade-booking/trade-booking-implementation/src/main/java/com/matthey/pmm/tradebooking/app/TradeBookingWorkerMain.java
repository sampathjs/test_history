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
import com.matthey.pmm.tradebooking.processors.RunWorkerProcessor;
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
    		RunWorkerProcessor processor = new RunWorkerProcessor(session, constRepo, ""); 
    		processor.processRun();
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
