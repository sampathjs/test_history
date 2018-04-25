package com.olf.jm.advancedPricingReporting;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedPricingReporting.items.tables.EnumArgumentTable;
import com.olf.jm.advancedPricingReporting.items.tables.EnumArgumentTableBuList;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumPartyStatus;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class AdvancedPricingReportParam.
 * 
 * Capture the parameters from the user if there is a GUI session or default to the current business day and active 
 * business units if no display. 
 * 
 * If running in debug mode the user is also asked for a run date in normal operation this defaults to business date.
 * 
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AdvancedPricingReportParam extends AbstractGenericScript {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Reporting";
	
	/** The current context. */
	private Context currentContext; 
	
	/** Flag to indicate if the script is running in debug mode */
	private boolean debugMode = false;
    
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, ConstTable table) {
		
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException("Error initilising logging. " + e.getLocalizedMessage());
		}

		currentContext = context;
		
		Table returnT = buildReturnTable();
	
		
		if(context.hasDisplay()) {
			// Prompt the use  have they run the matching process
			confirmMatchingRun();
			
			Table userResponse = displayDialog();
			processAskResponse(userResponse, returnT);
			
			

		} else {
			Table buList = createFilteredPartyList(context.getBusinessDate());

			returnT.setTable(EnumArgumentTable.BU_LIST.getColumnName(), 0, buList);
			returnT.setDate(EnumArgumentTable.RUN_DATE.getColumnName(), 0, context.getBusinessDate());
		}
		
		
		return returnT;

	}  
	
	private void confirmMatchingRun() {
		try {
			int response = Ask.okCancel("Did you run the matching process ?");
			
			if(response == 0) {
				String errorMsg = "User cancelled operation.";
				PluginLog.error(errorMsg);
				throw new RuntimeException(errorMsg);				
			}
		} catch (OException e) {
			String errorMsg = "Error displaying the matching process dialog. " + e.getMessage();
			PluginLog.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
	
	/**
	 * Process the user response response.
	 *
	 * @param response the output from the ask window
	 * @param returnT the table to be returned by the parameters script.
	 */
	private void processAskResponse(Table response, Table returnT) {
	
		
		try(Table selectedBUList = response.getTable ("return_value", 0)) {
			processBUList(selectedBUList, returnT);
		}
		
		if(debugMode) {
			try(Table selectedProcessDate = response.getTable ("return_value", 1)) {
				processRunDate(selectedProcessDate,  returnT);
			}
		} else {
			returnT.setDate(EnumArgumentTable.RUN_DATE.getColumnName(), 0, currentContext.getBusinessDate());
		}
	}
	
	/**
	 * Process the business units the user has selected, adds the id's to the return table.
	 *
	 * @param selectedBUList table containing the business units the user has selected
	 * @param returnT the table to be returned by the parameters script.
	 */
	private void processBUList(Table selectedBUList, Table returnT) {
		if(selectedBUList == null || selectedBUList.getRowCount() < 1) {
			String errorMsg = "Error processing the selected run date.";
			PluginLog.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
		
		Table buList = buildExtBuTable();
		buList.select(selectedBUList, "return_val->"+EnumArgumentTableBuList.BU_ID.getColumnName(), "[IN.return_val] > 0");
		
		returnT.setTable(EnumArgumentTable.BU_LIST.getColumnName(), 0, buList);
		
	}
	
	/**
	 * Build and display the ask dialog, if running in debug mode the run date is enabled and selectable.
	 * 
	 *
	 * @return a table containing the data the user selected. 
	 */
	private Table displayDialog() {
		com.olf.openjvs.Table askTable = null;
		com.olf.openjvs.Table selectableDealsJVS = null;
		try {
			askTable = com.olf.openjvs.Table.tableNew ("Advanced and Deferred Pricing Exposure Reporting");
			
			selectableDealsJVS = currentContext.getTableFactory().toOpenJvs(createPartyList(), true);
			
			Ask.setAvsTable(askTable, selectableDealsJVS, "External BU", 1,
					ASK_SELECT_TYPES.ASK_MULTI_SELECT.jvsValue(), 1);
			
			if(debugMode){
				Date businessDate = currentContext.getBusinessDate();
				Ask.setTextEdit (askTable
					,"Reporting Date"
					,OCalendar.formatDateInt (currentContext.getCalendarFactory().getJulianDate(businessDate))
					,ASK_TEXT_DATA_TYPES.ASK_DATE
					,"Please select processing date"
					,1);
			}
			if(Ask.viewTable (askTable,"Advanced and Deferred Pricing Exposure Reporting","Please select the processing parameters.") == 0) {
				String errorMessage = "User cancelled the dialog";
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}


		} catch (OException e) {
			String errorMessage = "Error displaying dialog. " + e.getLocalizedMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		} finally {
			try {
				// Do not dispose as needed in returnT
				//if(com.olf.openjvs.Table.isTableValid(askTable) == 1) {
				//	askTable.destroy();
				//}
				if(com.olf.openjvs.Table.isTableValid(selectableDealsJVS) == 1) {
					selectableDealsJVS.destroy();
				}				
			} catch (OException e) {

			}
		}
		return currentContext.getTableFactory().fromOpenJvs(askTable, true);
	}

	/**
	 * Process run date select by the user in the ask dialog, only active in debug mode.
	 *
	 * @param processDate the table containing the process data populated by the ask dialog
	 * @param returnT the table to be returned by the parameters script.
	 */
	private void processRunDate(Table processDate, Table returnT) {
		
		if(processDate == null || processDate.getRowCount() != 1) {
			String errorMsg = "Error processing the selected run date.";
			PluginLog.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
		String selectedDateStr = processDate.getString("return_value", 0);
		
		DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
		
		Date selectedDate;
		try {
			selectedDate = df.parse(selectedDateStr);
		} catch (ParseException e) {
			String errorMsg = "Error processing the selected run date. " + selectedDateStr + " is not a valid format.";
			PluginLog.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
		
		returnT.setDate(EnumArgumentTable.RUN_DATE.getColumnName(), 0, selectedDate);
	}
	
	/**
	 * Build a table with the correct structure that is returned by thsi script.
	 *
	 * @return the table
	 */
	private Table buildReturnTable() {
		
		TableColumnHelper<EnumArgumentTable> tableHelper = new TableColumnHelper<EnumArgumentTable>();
		Table returnT = tableHelper.buildTable(currentContext,EnumArgumentTable.class);
		
		returnT.addRows(1);
		
		return returnT;
	}
	
	/**
	 * Builds a table with the structure needed to store the selected bu's.
	 *
	 * @return the table
	 */
	private Table buildExtBuTable() {
		TableColumnHelper<EnumArgumentTableBuList> tableHelper = new TableColumnHelper<EnumArgumentTableBuList>();
		Table extBu = tableHelper.buildTable(currentContext,EnumArgumentTableBuList.class);
		
		return extBu;
	}
	
	/**
	 * Creates the party list. If running in debug mode all active external parties are returned, if running in a non
	 * debug mode the list is filtered to only parties which have AP / DP activity. 
	 *
	 * @return a table containing the parties the user can select.
	 */
	private Table createPartyList() {

		String sqlString = "\nSELECT p.party_id, p.short_name, p.long_name FROM party p INNER JOIN party_function pf ON pf.party_id = p.party_id "
				+    "\nWHERE p.party_class = 1 AND p.party_status = " + EnumPartyStatus.Authorized.getValue()
				+	 "  AND pf.function_type = 1 and int_ext = 1"
				;
		PluginLog.info("About to run SQL: " + sqlString.toString());
		
		
		
		if(!debugMode) {
			Table activeBus = createFilteredPartyList(currentContext.getBusinessDate());
			
			int[] ids = activeBus.getColumnValuesAsInt(EnumArgumentTableBuList.BU_ID.getColumnName());
			
			StringBuilder sb = new StringBuilder();
			for (int id : ids) { 
			    if (sb.length() > 0) sb.append(',');
			    sb.append(id);
			}

			sqlString = sqlString + " AND p.party_id in (" + sb.toString() + ")";
		}
		
		Table partyList = currentContext.getIOFactory().runSQL(sqlString);
		
        partyList.sort("short_name");
		return partyList;
	}	

	/**
	 * Creates the filtered party list containing only parties with AP / DP activity.
	 *
	 * @param runDate the  date the report is being run for. 
	 * @return a table containing the parties the user can select.
	 */
	private Table createFilteredPartyList(Date runDate) {
		StringBuffer sql = new StringBuffer();
		
		String matchDateString = currentContext.getCalendarFactory().getDateDisplayString(runDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT customer_id as ").append(EnumArgumentTableBuList.BU_ID.getColumnName()).append("\n");
		sql.append(" FROM   user_jm_ap_sell_deals\n"); 
		sql.append(" WHERE  match_status IN ( 'N', 'P' )\n"); 
		sql.append(" UNION\n"); 
		sql.append(" SELECT customer_id\n"); 
		sql.append(" FROM   user_jm_ap_buy_dispatch_deals ap\n"); 
		sql.append(" WHERE  match_date = '").append(matchDateString).append("'\n"); 
		sql.append(" UNION \n");
		sql.append(" SELECT external_bunit\n"); 
		sql.append(" FROM   ab_tran_info_view tiv\n"); 
		sql.append("        JOIN ab_tran ab\n"); 
		sql.append("          ON ab.tran_num = tiv.tran_num\n"); 
		sql.append("             AND ( tran_status = 1\n"); 
		sql.append("                    OR ( tran_status = 3\n"); 
		sql.append("                         AND trade_date = '").append(matchDateString).append("' ) )\n"); 
		sql.append("             AND ins_type = 26001 \n");
		sql.append(" WHERE  tiv.type_name = 'Pricing Type'\n"); 
		sql.append("        AND tiv.value = 'DP' 		\n");
		
		IOFactory ioFactory = currentContext.getIOFactory();
		
		PluginLog.info("About to run SQL: " + sql.toString());
		
		Table results = ioFactory.runSQL(sql.toString());
		
		return results;		
	}
	
	/**
	 * Initilise the logging framwork and set the debug flag.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
			
			// Enable debug mode so the date selector is enabled
			if(logLevel.compareToIgnoreCase("debug") == 0) {
				debugMode = true;
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	

}
