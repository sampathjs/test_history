package com.matthey.openlink.testalertbroker;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class TestAlertBrokerParam implements IScript {
	
	public static final String CONST_REPO_CONTEXT="Support";
	public static final String CONST_REPO_SUBCONTEXT="Utilities";

	
	private String defaultAlertBrokerID = "EOD-RSE-001";


	/** The Constant CONTEXT used to identify entries in the const repository. */
 
	
	@Override
	public void execute(IContainerContext context) throws OException {


		Table alertBrokerIDList = Util.NULL_TABLE;		
		Table defaultAlertBrokerList = Util.NULL_TABLE;

		try {
			
			init();
			PluginLog.info("Processing AlertBroker:" );
			
			
			alertBrokerIDList = getAlertBrokerIDs();

			defaultAlertBrokerList = getDefaultTable(alertBrokerIDList, defaultAlertBrokerID);

			if (Util.canAccessGui() == 1) {
				PluginLog.info("Testing Logging Messages and Objects:" );
				// GUI access prompt the user for the process date to run for
				Table tAsk = Table.tableNew ("Alert Broker Test");
				 // Convert the found symbolic date to a julian day.
				
				Ask.setAvsTable(tAsk , alertBrokerIDList.copyTable(), "Select Alert ID" , 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.jvsValue(),  1, defaultAlertBrokerList, "Select Log Level");
				Ask.setTextEdit (tAsk ,"Set Message" ,"Message sent from code layer" ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Please select display message" ,1);

				
				/* Get User to select parameters */
				if(Ask.viewTable (tAsk,"Testing Alert Broker","Testing Testing Testing") == 0) {
					String errorMessages = "The Adhoc Ask has been cancelled.";
					Ask.ok ( errorMessages );
					PluginLog.info(errorMessages );

					tAsk.destroy();
					throw new OException( "User Clicked Cancel" );
				}

				/* Verify Start and End Dates */
				 
				String alertID = tAsk.getTable( "return_value", 1).getString(1, 1);
				String message = tAsk.getTable( "return_value", 2).getString("return_value", 1);
				 
				AlertBroker.sendAlert(alertID, message);
				tAsk.destroy();
				
				
				PluginLog.info("Processing Logging Framework Finished" );
				
			} else {
				PluginLog.info("Processing Logging Framework Setting to defaults" );
	 				
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			String msg = e.getMessage();
			throw new OException(msg);
		} finally {
 
			if (Table.isTableValid(alertBrokerIDList)!=0){
				alertBrokerIDList.destroy();
			}
			if (Table.isTableValid(defaultAlertBrokerList)!=0){
				defaultAlertBrokerList.destroy();
			}
						 
			PluginLog.exitWithStatus();
		}
		
		
	}
	
	private Table getAlertBrokerIDs() throws OException {
		Table locationList = Table.tableNew("Alert Broker IDs");
		String sql = "SELECT uam.msg_id 'Message ID', uam.msg_txt 'Message Text', uam.proj_name 'Project Name' \n" + 
					 " FROM USER_alert_messages uam\n" + 
					 " ORDER BY uam.msg_id" ; 

		DBaseTable.execISql(locationList, sql);
		return locationList;
	}
	
	 
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		ConstRepository constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "Debug";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = Util.getEnv("AB_OUTDIR") + "\\error_logs"; //  
		
		try {
			String defaultLogLevel = constRep.getStringValue("logLevel", logLevel);
			String defaultOutputFile = constRep.getStringValue("logFile", logFile);
			String defaultOutputDir = constRep.getStringValue("logDir", logDir);

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
