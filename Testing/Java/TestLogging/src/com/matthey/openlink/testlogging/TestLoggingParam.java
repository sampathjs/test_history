package com.matthey.openlink.testlogging;

import static com.matthey.openlink.testlogging.TestLoggingConstants.COL_Logging_Object;
import static com.matthey.openlink.testlogging.TestLoggingConstants.COL_Message;
import static com.matthey.openlink.testlogging.TestLoggingConstants.COL_log_level;
import static com.matthey.openlink.testlogging.TestLoggingConstants.COL_output_dir;
import static com.matthey.openlink.testlogging.TestLoggingConstants.COL_output_file;

import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class TestLoggingParam implements IScript {
	
	private String defaultOutputDir = "";
	private String defaultOutputFile = "";
	private String defaultLogLevel = "";

	/** The const repository used to initialise the logging classes. */
//	private ConstRepository constRep;
	


	/** The Constant CONTEXT used to identify entries in the const repository. */
 
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table loggingObjList = Util.NULL_TABLE;		
		Table defaultLoggingObjList = Util.NULL_TABLE;

		Table logLevelList = Util.NULL_TABLE;		
		Table defaultlogLevelList = Util.NULL_TABLE;

		try {
			
			init();
			PluginLog.info("Processing Logging Framework Started:" );
			
			
			Table argt = context.getArgumentsTable();
			loggingObjList = getLoggingObjects();
			logLevelList = getLogLevelObjects();
			

			String defaultLoggingObject = loggingObjList.getString(1, 1);
			defaultLoggingObjList = getDefaultTable(loggingObjList, defaultLoggingObject);
			
			
			//String defaultLogLevel = logLevelList.getString(1, 2);	// Error
			defaultlogLevelList = getDefaultTable(logLevelList, defaultLogLevel);

			
			 
			argt.addCol(COL_Logging_Object, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_log_level, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Message, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_output_dir, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_output_file, COL_TYPE_ENUM.COL_STRING);
			
			
			if(argt.getNumRows() < 1) {
				argt.addRow();
			}
		   
			
			
			if (Util.canAccessGui() == 1) {
				PluginLog.info("Testing Logging Messages and Objects:" );
				// GUI access prompt the user for the process date to run for
				Table tAsk = Table.tableNew ("Logging Details");
				 // Convert the found symbolic date to a julian day.
				
				Ask.setAvsTable(tAsk , loggingObjList.copyTable(), "Select Logging Object" , 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.jvsValue(), 1, defaultLoggingObjList, "Select Logging Object to use");
				Ask.setAvsTable(tAsk , logLevelList.copyTable(), "Select Log Level" , 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.jvsValue(),  1, defaultlogLevelList, "Select Log Level");
				Ask.setTextEdit (tAsk ,"Set Message" ,"Hello World" ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Please select display message" ,1);
				Ask.setTextEdit (tAsk ,"Set Output Directory" ,defaultOutputDir ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Please select Output Directory" ,1);
				Ask.setTextEdit (tAsk ,"Set Output FileName" ,defaultOutputFile ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Please select Output FileName" ,1);
				
				/* Get User to select parameters */
				if(Ask.viewTable (tAsk,"Testing Logging Framework","Testing various Logging Objects") == 0) {
					String errorMessages = "The Adhoc Ask has been cancelled.";
					Ask.ok ( errorMessages );
					PluginLog.info(errorMessages );

					tAsk.destroy();
					throw new OException( "User Clicked Cancel" );
				}

				/* Verify Start and End Dates */
				 
//				Table logObjRetTable = tAsk.getTable( "return_value", 1);
				String loggingObjectName = tAsk.getTable( "return_value", 1).getString(1, 1);
				String logLevel = tAsk.getTable( "return_value", 2).getString(1, 1);
				String message = tAsk.getTable( "return_value", 3).getString("return_value", 1);
				String outputDir = tAsk.getTable( "return_value", 4).getString(1, 1);
				String outputFile = tAsk.getTable( "return_value", 5).getString(1, 1);
				
				argt.setString(COL_Logging_Object, 1, loggingObjectName);
				argt.setString(COL_log_level, 1, logLevel);
				argt.setString(COL_Message, 1, message);
				argt.setString(COL_output_dir, 1, outputDir);
				argt.setString(COL_output_file, 1, outputFile);
				
				
				tAsk.destroy();
				
				
				PluginLog.info("Processing Logging Framework Finished" );
				
			} else {
				PluginLog.info("Processing Logging Framework Setting to defaults" );
				
				// no gui so default to the current EOD date. 
				argt.setString(COL_Logging_Object, 1, defaultLoggingObject);
				argt.setString(COL_Message, 1, "Hello World");
				argt.setString(COL_log_level, 1, defaultLogLevel);
				argt.setString(COL_output_dir, 1, defaultOutputDir);
				argt.setString(COL_output_file, 1, defaultOutputFile);
				
				
				
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			String msg = e.getMessage();
			throw new OException(msg);
		} finally {
			if (Table.isTableValid(loggingObjList)!=0){
				loggingObjList.destroy();
			}
			 
			if (Table.isTableValid(defaultLoggingObjList)!=0){
				defaultLoggingObjList.destroy();
			}
			 
			PluginLog.exitWithStatus();
		}
		
		
	}
	
	private Table getLogLevelObjects() throws OException {
		Table logLevelList = Table.tableNew("Log Level List");
		logLevelList.addCol("Log Level", COL_TYPE_ENUM.COL_STRING);
		logLevelList.addRow();
		logLevelList.addRow();
		logLevelList.addRow();
		logLevelList.addRow();
		logLevelList.addRow();
		logLevelList.setString(1, 1, TestLoggingConstants.LOG_LEVEL_FATAL);
		logLevelList.setString(1, 2, TestLoggingConstants.LOG_LEVEL_ERROR);
		logLevelList.setString(1, 3, TestLoggingConstants.LOG_LEVEL_WARN);
		logLevelList.setString(1, 4, TestLoggingConstants.LOG_LEVEL_INFO);
		logLevelList.setString(1, 5, TestLoggingConstants.LOG_LEVEL_DEBUG);

		return logLevelList;
	}

	private Table getLoggingObjects() throws OException {
		Table loggingObjectList = Table.tableNew("Logging Object List");
		loggingObjectList.addCol("Logging Object", COL_TYPE_ENUM.COL_STRING);
		loggingObjectList.addRow();
		loggingObjectList.addRow();
		loggingObjectList.addRow();
		loggingObjectList.addRow();
		loggingObjectList.addRow();
		loggingObjectList.addRow();
		loggingObjectList.addRow();  
		loggingObjectList.addRow();  

		loggingObjectList.addRow();  
		loggingObjectList.addRow();  
		
		loggingObjectList.setString(1, 1, TestLoggingConstants.OBJECT_JMLogging);
		loggingObjectList.setString(1, 2, TestLoggingConstants.OBJECT_OConsole);
		loggingObjectList.setString(1, 3, TestLoggingConstants.OBJECT_SystemPrint);
		loggingObjectList.setString(1, 4, TestLoggingConstants.OBJECT_JVS_Standard);
		loggingObjectList.setString(1, 5, TestLoggingConstants.OBJECT_PLUGIN_LOG);
		loggingObjectList.setString(1, 6, TestLoggingConstants.OBJECT_NEW_PLUGIN_LOG);
		loggingObjectList.setString(1, 7, TestLoggingConstants.OBJECT_LOGGER_LOG);
		loggingObjectList.setString(1, 8, TestLoggingConstants.OBJECT_NEW_LOGGER_LOG);
		
		loggingObjectList.setString(1, 9, TestLoggingConstants.OBJECT_SWAP_PLUGIN_LOG);
		loggingObjectList.setString(1, 10, TestLoggingConstants.OBJECT_SWAP_LOGGER_LOG);
		
		return loggingObjectList;
	}

	 
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		ConstRepository constRep = new ConstRepository(TestLoggingConstants.CONST_REPO_CONTEXT, TestLoggingConstants.CONST_REPO_SUBCONTEXT);

		String logLevel = "Debug";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";

		try {
			defaultLogLevel = constRep.getStringValue("logLevel", logLevel);
			defaultOutputFile = constRep.getStringValue("logFile", logFile);
			defaultOutputDir = constRep.getStringValue("logDir", logDir);
			
			if (defaultOutputDir == null) {
				PluginLog.init(defaultLogLevel);
			} else {
				PluginLog.init(defaultLogLevel, defaultOutputDir, defaultOutputFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	
	private Table getDefaultTable(Table coreTable, String preferedDefaultValue) throws OException {
		Table retTable = Table.tableNew();
		retTable = coreTable.cloneTable();
		int foundVal = coreTable.unsortedFindString(1, preferedDefaultValue	, SEARCH_CASE_ENUM.CASE_INSENSITIVE	);
		if (foundVal<=0){
			foundVal = coreTable.unsortedFindString(2, preferedDefaultValue	, SEARCH_CASE_ENUM.CASE_INSENSITIVE	);
		}
		if (foundVal<=0){
			foundVal=coreTable.getNumRows();
		}
		coreTable.copyRowAdd(foundVal, retTable);
		return retTable;
	}
}
