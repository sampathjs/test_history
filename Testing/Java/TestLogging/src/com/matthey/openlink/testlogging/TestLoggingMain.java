package com.matthey.openlink.testlogging;

import com.olf.jm.logging.LoggingJVS;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
//import com.openlink.util.logging.PluginLog;




import standard.include.JVS_INC_Standard;


public class TestLoggingMain implements IScript {
//	 private TestLoggingSwapMethods data = new TestLoggingSwapMethods();
	public ConstRepository constRep;

	@Override
	public void execute(IContainerContext context) throws OException {
		 
		try {
			Table argt = context.getArgumentsTable();
			
			if (argt.getNumRows()==1){
				
				String objectType = argt.getString(TestLoggingConstants.COL_Logging_Object, 1);
				String message = argt.getString(TestLoggingConstants.COL_Message, 1);
				String logLevel = argt.getString(TestLoggingConstants.COL_log_level, 1);
				
				String outputDir = argt.getString(TestLoggingConstants.COL_output_dir, 1);
				String outputFile = argt.getString(TestLoggingConstants.COL_output_file, 1);
				
				if (objectType != null && objectType.length()>0){
					if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_PLUGIN_LOG)){
						runPluginLog(message , logLevel, outputDir, outputFile);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_OConsole)){
						runOConsole(message , logLevel);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_SystemPrint)){
						runSystemPrint(message , logLevel);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_JMLogging)){
						runJMLogging(message , logLevel, outputDir, outputFile);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_JVS_Standard)){
						runJVSStandard(message , logLevel );
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_NEW_PLUGIN_LOG)){
						runNewPluginLog(message , logLevel, outputDir, outputFile);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_LOGGER_LOG)){
						runLoggerLog(message , logLevel);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_NEW_LOGGER_LOG)){
						runNewLoggerLog(message , logLevel);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_SWAP_LOGGER_LOG)){
						TestLoggingSwapMethods.runSwapLoggerLog(message , logLevel, this);
					} else if (objectType.equalsIgnoreCase(TestLoggingConstants.OBJECT_SWAP_PLUGIN_LOG)){
						TestLoggingSwapMethods.runSwapPluginLog(message , logLevel, outputDir, outputFile, getClass().getSimpleName());
					}

					
					
					
				}
			}

			
		} catch (Exception e) {
			OConsole.print("Ooopsie Error: \n" + e.getLocalizedMessage() + e.getStackTrace());
		}
		
	}

	
	private void runLoggerLog(String message, String logLevel) {
//		import com.openlink.endur.utilities.logger.LogCategory;
//		import com.openlink.endur.utilities.logger.LogLevel;
//		import com.openlink.endur.utilities.logger.Logger;
		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			try {
				com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.FATAL, com.openlink.endur.utilities.logger.LogCategory.General, this, message);
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.FATAL,  com.openlink.endur.utilities.logger.LogCategory.General, this, message, e);
			}

		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			try {
				com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.ERROR, com.openlink.endur.utilities.logger.LogCategory.General, this, message);	
//				com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.ERROR, com.openlink.endur.utilities.logger.LogCategory.General, TestLoggingMain.class, message);		// Worse implemetation
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.ERROR,  com.openlink.endur.utilities.logger.LogCategory.General, this, message, e);
			}
			
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.WARNING, com.openlink.endur.utilities.logger.LogCategory.General, this, message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.DEBUG, com.openlink.endur.utilities.logger.LogCategory.General, this, message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			com.openlink.endur.utilities.logger.Logger.log(com.openlink.endur.utilities.logger.LogLevel.INFO, com.openlink.endur.utilities.logger.LogCategory.General, this, message);
		}

		
	}

	private void runNewLoggerLog(String message, String logLevel) {
//		import com.olf.jm.logging.LogCategory;
//		import com.olf.jm.logging.LogLevel;
//		import com.olf.jm.logging.Logger;
		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			try {
				com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.FATAL, com.olf.jm.logging.LogCategory.General, this, message);
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.FATAL,  com.olf.jm.logging.LogCategory.General, this, message, e);
			}

		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			try {
				com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.ERROR, com.olf.jm.logging.LogCategory.General, this, message);	
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.ERROR,  com.olf.jm.logging.LogCategory.General, this, message, e);
			}
			
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.WARNING, com.olf.jm.logging.LogCategory.General, this, message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.DEBUG, com.olf.jm.logging.LogCategory.General, this, message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			com.olf.jm.logging.Logger.log(com.olf.jm.logging.LogLevel.INFO, com.olf.jm.logging.LogCategory.General, this, message);
		}

		
	}

	private void runJVSStandard(String message, String logLevel) throws OException {
		 
		JVS_INC_Standard m_INCStandard;
		m_INCStandard = new JVS_INC_Standard();
		
		String sFileName = getClass().getSimpleName();
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);

		m_INCStandard.Print(error_log_file, "START", "*** Start of Message - " +  getClass().getSimpleName() + " ***");

		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			m_INCStandard.Print(error_log_file, "FATAL", message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			m_INCStandard.Print(error_log_file, "ERROR", message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			m_INCStandard.Print(error_log_file, "WARN", message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			m_INCStandard.Print(error_log_file, "DEBUG", message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			m_INCStandard.Print(error_log_file, "INFO", message);
		}
		
		m_INCStandard.Print(error_log_file, "END", "*** End of Message - " +  getClass().getSimpleName() + " ***\n");
		
	}


	private void runJMLogging(String message, String logLevel,String outputDir, String outputFile) {
		 
		LoggingJVS.init( this.getClass(),TestLoggingConstants.CONST_REPO_CONTEXT, TestLoggingConstants.CONST_REPO_SUBCONTEXT);


		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL) ){
			try {
				LoggingJVS.fatal(message);
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				LoggingJVS.fatal(message,e);
			}
		} else  if( logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			try {
				LoggingJVS.error(message);
				throw new RuntimeException("Help me");
			} catch(Exception e) {
				LoggingJVS.error(message,e);
			}
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			LoggingJVS.warn(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			LoggingJVS.info(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			LoggingJVS.debug(message);
		}
		LoggingJVS.close();
	}


	private void runSystemPrint(String message, String logLevel) {

		System.out.println("Log Level: " + logLevel + " Message: " + message);
  		
	}


	private void runOConsole(String message, String logLevel) throws OException {
		
		OConsole.oprint("Log Level: " + logLevel + " Message: " + message);
  	}


	private void runPluginLog(String message, String logLevel, String outputDir,String outputFile) throws Exception {
		initPluginLog(outputDir, outputFile);
		
		com.openlink.util.logging.PluginLog.info(getClass().getSimpleName() + " started");


		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			com.openlink.util.logging.PluginLog.fatal(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			com.openlink.util.logging.PluginLog.error(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			com.openlink.util.logging.PluginLog.warn(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			com.openlink.util.logging.PluginLog.info(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			com.openlink.util.logging.PluginLog.debug(message);
		}
		com.openlink.util.logging.PluginLog.debug("getLogDir:" + com.openlink.util.logging.PluginLog.getLogDir());
		com.openlink.util.logging.PluginLog.debug("getLogFile:" + com.openlink.util.logging.PluginLog.getLogFile());
		com.openlink.util.logging.PluginLog.debug("getLogPath:" + com.openlink.util.logging.PluginLog.getLogPath());
		com.openlink.util.logging.PluginLog.debug("getLogLevel:" + com.openlink.util.logging.PluginLog.getLogLevel());
		
		com.openlink.util.logging.PluginLog.exitWithStatus(false);
		
	}

	private void runNewPluginLog(String message, String logLevel , String outputDir,String outputFile) throws Exception {
		initNewPluginLog( outputDir, outputFile);
		
		com.olf.jm.logging.PluginLog.info(getClass().getSimpleName() + " started");


		if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_FATAL)){
			com.olf.jm.logging.PluginLog.fatal(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_ERROR)){
			com.olf.jm.logging.PluginLog.error(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_WARN)){
			com.olf.jm.logging.PluginLog.warn(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_INFO)){
			com.olf.jm.logging.PluginLog.info(message);
		} else if(logLevel.equalsIgnoreCase(TestLoggingConstants.LOG_LEVEL_DEBUG)){
			com.olf.jm.logging.PluginLog.debug(message);
		}
		com.olf.jm.logging.PluginLog.debug("getLogDir:" + com.olf.jm.logging.PluginLog.getLogDir());
		com.olf.jm.logging.PluginLog.debug("getLogFile:" + com.olf.jm.logging.PluginLog.getLogFile());
		com.olf.jm.logging.PluginLog.debug("getLogPath:" + com.olf.jm.logging.PluginLog.getLogPath());
		com.olf.jm.logging.PluginLog.debug("getLogLevel:" + com.olf.jm.logging.PluginLog.getLogLevel());
		
		com.olf.jm.logging.PluginLog.exitWithStatus(false);
		
	}
	
	private void initNewPluginLog(String outputDir,String outputFile) throws Exception {
		constRep = new ConstRepository(TestLoggingConstants.CONST_REPO_CONTEXT, TestLoggingConstants.CONST_REPO_SUBCONTEXT);
		
		String logLevel = "Debug"; 

		try
		{
			logLevel = constRep.getStringValue("logLevel", logLevel);
			

			if (outputDir == null || outputDir.length()==0){
				com.olf.jm.logging.PluginLog.init(logLevel);
			} else {
				com.olf.jm.logging.PluginLog.init(logLevel, outputDir, outputFile);
			}
		}
		catch (Exception e)
		{
			// do something
		}
	}
	private void initPluginLog(String outputDir, String outputFile) throws Exception {
		
		constRep = new ConstRepository(TestLoggingConstants.CONST_REPO_CONTEXT, TestLoggingConstants.CONST_REPO_SUBCONTEXT);
		
		String logLevel = "Debug"; 

		try
		{
			logLevel = constRep.getStringValue("logLevel", logLevel);
			if (outputDir == null || outputDir.length() ==0){
				com.openlink.util.logging.PluginLog.init(logLevel);
			} else {
				com.openlink.util.logging.PluginLog.init(logLevel, outputDir, outputFile);
			}
		}  catch (Exception e) {
			// do something
		}
	}
	

	
}
