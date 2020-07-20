package com.matthey.openlink.purge;

import java.io.PrintWriter;

import com.matthey.utilities.Utils;
import com.olf.openjvs.Afs;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.DataMaint;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ARCHIVE_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * This class purges tables specified in the USER_jm_purge_config.  
 * It will purge data based on a datetime column in a table and the specified number of days from the config
 */
public class PurgeHistoryData implements IScript
{
	// must have this security priv
	private static final int PURGE_SECURITY_PRIVILEGE = 22062; // 22062 - Purge Archive Data Types
	private static final int PURGE_TPM_SECURITY_PRIVILEGE = 54008; // 22062 - Purge Archive Data Types

	// whether to run in data gathering mode
	private boolean runInDataGatheringMode = false;

	// whether to purge the SQL tables
	private boolean purgeSQLTables = true;

	// whether to purge the OL archive types via the OL API
	private boolean purgeOLArchiveTypes = true;

	// whether to look for special purge functions
	private boolean purgeOLUsingFunctions = true;

	// whether to purge the AFS tables
	private boolean purgeAFSFiles = true;

	// whether to email the results
	private boolean emailResults = true;

	// whether to only run on the specified list
	boolean overridePurgeNameList = false;

	// category to run
	private String purgeCategory = "Daily";

	// whether to check the category, if manually run via a task then we should not unless a category is specified
	private boolean checkCategory = true;

	// the list of purge names to purge and the days to retain
	Table purgeNamesToPurge = Util.NULL_TABLE;

	// the map of purge names against table names
	Table purgeToTableMap = Util.NULL_TABLE;

	// the selected subset of tables from the argt (optional)
	Table selectedPurges = Util.NULL_TABLE;

	// report status from the purge
	String reportText = "";

	// email addresses
	Table emailUserList = Util.NULL_TABLE;

	private ConstRepository constantsRepo;
	

	@Override
	public void execute(IContainerContext context) throws OException {
		constantsRepo = new ConstRepository("Purge", "PurgeUtility");
		initPluginLog(constantsRepo);
		
		Table tblArgt = context.getArgumentsTable();

		try {
			PurgeUtil.printWithDateTime("START Running PurgeHistoryData");

			initialisePrerequisites(tblArgt);

			String reportFileName = Util.reportGetDirForToday().replace("/", "\\") + "\\Purge_Report_" + OCalendar.formatDateInt(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + "_" + Util.timeGetServerTime() + ".csv";

			PurgeUtil.printWithDateTime("Data Gathering Mode = " + runInDataGatheringMode);
			PurgeUtil.printWithDateTime("Purging SQL tables = " + purgeSQLTables);
			PurgeUtil.printWithDateTime("Purging OL Archive types = " + purgeOLArchiveTypes);
			PurgeUtil.printWithDateTime("Purging AFS tables = " + purgeAFSFiles);
			PurgeUtil.printWithDateTime("Overriding default Purge Config = " + overridePurgeNameList);
			PurgeUtil.printWithDateTime("Email Results = " + emailResults);
			PurgeUtil.printWithDateTime("Checking Category = " + checkCategory);
			PurgeUtil.printWithDateTime("Category = " + purgeCategory);

			purgeToTableMap = GetListOfTablesForPurgeNames();
			purgeNamesToPurge = GetListOfPurgeNames();

			if (overridePurgeNameList) {
				ValidatePurgeableListAgainstSelectedPurgeNames(purgeNamesToPurge, selectedPurges);
			}

			if ( runInDataGatheringMode ) {
				reportText = GatherStatsAboutTablesInPurge(purgeNamesToPurge, purgeToTableMap);

				reportText = reportText + "\r\nDATA GATHERING COMPLETE\r\n";
			} else {
				if ( purgeSQLTables ) {				  
					reportText = PurgeSQLTables(purgeNamesToPurge, purgeToTableMap, checkCategory, purgeCategory);
				}

				if ( purgeOLArchiveTypes ) {
					reportText = PurgeOLArchiveTypes(purgeNamesToPurge, purgeToTableMap, checkCategory, purgeCategory, reportText);
				}

				if ( purgeOLUsingFunctions ) {
					reportText += PurgeOLUsingFunctions(purgeNamesToPurge, purgeToTableMap, checkCategory, purgeCategory);
				}

				if ( purgeAFSFiles ) {
					reportText = reportText + PurgeAFSFiles();
				}
			}

			reportText += "PURGE COMPLETE\r\n";

			SaveReportToFile(reportFileName, reportText);

			if (emailResults) {			  
				PurgeUtil.printWithDateTime("Calling Emailer");
				emailReport(reportFileName);
			}

			PurgeUtil.printWithDateTime("FINISHED Running PurgeHistoryData");
		} catch (Exception ex) {
			String message = "Script failed with the following error(s): " + ex.getMessage();
			PurgeUtil.printWithDateTime(message);
			Util.exitFail(message);
		} finally {
			Logging.close();
			if ( Table.isTableValid(purgeNamesToPurge) != 0 ){
				purgeNamesToPurge.destroy();
			}

			if ( Table.isTableValid(purgeToTableMap) != 0 ){
				purgeToTableMap.destroy();
			}
		}

		if ( reportText.contains("!!!ERROR!!!") ){ 
			Util.exitFail();
		}

		Util.exitSucceed();
	}
	
 

	private void initPluginLog(ConstRepository constRepo) {
		try {
			String abOutdir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			 
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", "PurgeHistoryData.log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				Logging.init(this.getClass(), constRepo.getContext(), constRepo.getSubcontext());
			} catch (Exception e) {
				throw new RuntimeException("Error initializing PluginLog", e);
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error initializing the ConstRepo", ex);
		}
	}
	
	private void initialisePrerequisites(Table argt) throws OException {

		if (Util.userCanAccess(PURGE_SECURITY_PRIVILEGE) != 1) {
			PurgeUtil.printWithDateTime("User does not have privilege to run this utility.  Missing security priv: " + PURGE_SECURITY_PRIVILEGE);
			Util.exitFail();
		}

		int numArgtRows = argt.getNumRows();	
		if ( numArgtRows == 1 ) {
			// if the argt is populated then set the check to false, it will be reset to true if a category is set
			checkCategory = false;

			if ( argt.getColNum("data_gathering_mode") > 0 ){
				runInDataGatheringMode = argt.getInt("data_gathering_mode", 1) == 1 ? true : false;
			}

			if ( argt.getColNum("purge_sql_tables") > 0 ){
				purgeSQLTables = argt.getInt("purge_sql_tables", 1) == 1 ? true : false;
			}

			if ( argt.getColNum("purge_ol_archive_types") > 0 ){
				purgeOLArchiveTypes = argt.getInt("purge_ol_archive_types", 1) == 1 ? true : false;
			}

			if ( argt.getColNum("purge_ol_using_functions") > 0 ){
				purgeOLArchiveTypes = argt.getInt("purge_ol_using_functions", 1) == 1 ? true : false;
			}

			if ( argt.getColNum("purge_afs_files") > 0 ){	
				purgeAFSFiles = argt.getInt("purge_afs_files", 1) == 1 ? true : false;
			}

			if ( argt.getColNum("purge_names") > 0 ){
				
				selectedPurges = argt.getTable("purge_names", 1);
				if ( Table.isTableValid(selectedPurges) != 0 && selectedPurges.getNumRows() > 0 ) {
					if ( selectedPurges.getColNum("purge_name") > 0 ){
						overridePurgeNameList = true;
					} else {
						String msg = "Missing purge_name column from the passed in table";
						PurgeUtil.printWithDateTime(msg);
						Util.exitFail(msg);
					}
				}
			}

			if ( argt.getColNum("email_results") > 0 ){	
				emailResults = argt.getInt("email_results", 1) == 1 ? true : false;
			}

			if ( argt.getColNum("email_user_list") > 0 ) {
				emailUserList = argt.getTable("email_user_list", 1);
				if ( Table.isTableValid(emailUserList) == 0) {
					String msg = "Missing email configuration from the passed in table";
					PurgeUtil.printWithDateTime(msg);
					Util.exitFail(msg);
				}
			} else {
				ConstRepository constantsRepo = new ConstRepository("Purge", "PurgeUtility");
				emailUserList = constantsRepo.getMultiStringValue("Email User", Util.NULL_TABLE);
			}

			if ( argt.getColNum("category") > 0 ) {	
				purgeCategory = argt.getString("category", 1);
				checkCategory = true;
			}
		} else {
			ConstRepository constantsRepo = new ConstRepository("Purge", "PurgeUtility");
			emailUserList = constantsRepo.getMultiStringValue("Email User", Util.NULL_TABLE);
		}
	}

	/**
	 * Function to filter down the purgeable list
	 * @param purgeNames universe of purge names
	 * @param selectedPurges selected purges
	 */
	private void ValidatePurgeableListAgainstSelectedPurgeNames(Table purgeNames, Table selectedPurges) throws OException {
		selectedPurges.sortCol("purge_name");

		int numPurges = purgeNames.getNumRows();
		for (int iRow = numPurges; iRow >= 1; iRow--) {
			String purgeName = purgeNames.getString("purge_name", iRow);
			int foundRow = selectedPurges.findString("purge_name", purgeName, SEARCH_ENUM.FIRST_IN_GROUP);
			if (foundRow < 1) {
				purgeNames.delRow(iRow);
				continue;
			}
		}
	}

	/**
	 * Function to get list of purge names and config
	 * @return list of purge names and config
	 */
	private Table GetListOfPurgeNames() throws OException	{
		PurgeUtil.printWithDateTime("Getting list of Purge Names");

		Table purgeNamesToPurge = Table.tableNew();

		String what  = "purge_name, category, sort_order, purge_type, stored_procedure, days_to_retain";    
		String from  = "USER_jm_purge_config";    
		String where = "active_flag = 1"; 
		int iRetVal = DBaseTable.loadFromDbWithSQL(purgeNamesToPurge, what, from, where);  
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			throw new OException("Failed to run load tables to purge from USER_jm_purge_config");
		}

		purgeNamesToPurge.sortCol("sort_order");

		return purgeNamesToPurge;
	}

	/**
	 * Function to update list of purgeable tables for DW
	 */
	@SuppressWarnings("unused")
	private void UpdateListofDWTablesForOLPurge() throws OException {
		PurgeUtil.printWithDateTime("Updating list of purgeable tables for DW OL archive type (ADT_PURGE_DW_SIM_RESULTS_DAILY)");

		Table arguments = Table.tableNew();

		int iRetVal = DBase.runProc("USER_purge_update_dw_list", arguments);  
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			throw new OException("Failed to run USER_purge_update_dw_list to update list of DW tables that will be purged");		
		}

		return;
	}

	/**
	 * Function to get list of purgeable tables
	 * @return list of purge names and tables
	 */
	private Table GetListOfTablesForPurgeNames() throws OException {		
		//UpdateListofDWTablesForOLPurge(); // 

		Table tablesToPurge = Table.tableNew();

		String what  = "purge_name, table_name, date_column, where_clause, date_column_type";    
		String from  = "USER_jm_purge_tables";    
		String where = "1 = 1"; 

		PurgeUtil.printWithDateTime("Getting list of tables associated with every purge type");

		int iRetVal = DBaseTable.loadFromDbWithSQL(tablesToPurge, what, from, where);  
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			throw new OException("Failed to run load tables to purge from USER_jm_purge_tables");		
		}

		// Add extra columns to track what was logged
		tablesToPurge.addCol("purged_from_date", COL_TYPE_ENUM.COL_DATE_TIME);
		tablesToPurge.addCol("start_time", COL_TYPE_ENUM.COL_DATE_TIME);
		tablesToPurge.addCol("before_row_count", COL_TYPE_ENUM.COL_INT);
		tablesToPurge.addCol("end_time", COL_TYPE_ENUM.COL_DATE_TIME);
		tablesToPurge.addCol("remaining_rows", COL_TYPE_ENUM.COL_INT);
		tablesToPurge.addCol("number_rows_purged", COL_TYPE_ENUM.COL_INT);
		tablesToPurge.addCol("purge_elapsed_time_secs", COL_TYPE_ENUM.COL_INT);

		tablesToPurge.sortCol("purge_name");
		return tablesToPurge;
	}

	/**
	 * Function to gather stats about tables rather than doing the actual purge
	 * @param purgeNamesToPurge List of purge names to purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 * @return reporting string
	 */
	private String GatherStatsAboutTablesInPurge(Table purgeNamesToPurge, Table purgeToTableMap) throws OException {

		PurgeUtil.printWithDateTime("Gathering stats about tables");

		Table distinctTables = Table.tableNew("unique tables in purge");

		distinctTables.select(purgeToTableMap, "DISTINCT,table_name,date_column,date_column_type", "table_name NE ''");

		distinctTables.addCol("minimum_jddate", COL_TYPE_ENUM.COL_INT);
		distinctTables.addCol("maximum_jddate", COL_TYPE_ENUM.COL_INT);
		distinctTables.addCol("row_count", COL_TYPE_ENUM.COL_INT);

		String messageText = "GATHERING INFO ABOUT TABLES TO BE PURGED";

		String sqlWhere, sqlWhat, sqlFrom;

		messageText = messageText + "\r\nTable,Row Count, Date Column, Date Column Type, Min Date, Max Date";

		int numTables = distinctTables.getNumRows();
		for ( int tableRow = 1; tableRow <= numTables; tableRow++) {
			String tableName = distinctTables.getString("table_name", tableRow);
			String dateColumn = distinctTables.getString("date_column", tableRow);
			String dateColumnType = distinctTables.getString("date_column_type", tableRow);

			if ( tableName.startsWith("dw_") ) {
				sqlWhat = "min(a.row_creation) as minimum_jddate, max(a.row_creation) as maximum_jddate, count(b.extraction_id) as row_count";
				sqlFrom = "dw_extraction_log a, " + tableName + " b";
				sqlWhere = "a.extraction_id = b.extraction_id";				
			} else if ( dateColumn.length() < 2) {
				sqlWhat = "0 as minimum_jddate, 0 as maximum_jddate, count(*) as row_count";
				sqlFrom = tableName;
				sqlWhere = "1 = 1";				
			} else {			
				sqlWhat = "";
				sqlFrom = tableName;
				sqlWhere = "1 = 1";

				if ( dateColumnType.equals("string") ) {
					sqlWhat = "min(cast(" + dateColumn + " as datetime)) as minimum_jddate, " +
							"max(cast(" + dateColumn + " as datetime)) as maximum_jddate, " + 
							"count(*) as row_count";
				} else if ( dateColumnType.equals("datetime") ) {
					sqlWhat = "min(" + dateColumn + ") as minimum_jddate, " +
							"max(" + dateColumn + ") as maximum_jddate, " + 
							"count(*) as row_count";
				} else if ( dateColumnType.equals("int") ) {
					sqlWhat = "min(" + dateColumn + ") as minimum_jddate, " +
							"max(" + dateColumn + ") as maximum_jddate, " + 
							"count(*) as row_count";			
				}
			}
			PurgeUtil.printWithDateTime("Gathering stats about table: " + tableName);
			PurgeUtil.printWithDateTime("What: " + sqlWhat);
			PurgeUtil.printWithDateTime("From: " + sqlFrom);
			PurgeUtil.printWithDateTime("Where: " + sqlWhere);

			Table tableInfo = Table.tableNew();
			try {
				int iRetVal = DBaseTable.loadFromDbWithSQL(tableInfo, sqlWhat, sqlFrom, sqlWhere);  
				if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
					throw new OException("Failed to find min, max, count for table: " + tableName);		
				}
			} catch (OException ex) {
				messageText = messageText + "\r\n!!!ERROR!!! Failed to find min, max, count for table: " + tableName + ". Exception:" + ex.getMessage();
				continue;
			}

			distinctTables.setInt("row_count", tableRow, tableInfo.getInt("row_count", 1));
			distinctTables.setInt("minimum_jddate", tableRow, tableInfo.getInt("minimum_jddate", 1));
			distinctTables.setInt("maximum_jddate", tableRow, tableInfo.getInt("maximum_jddate", 1));

			tableInfo.destroy();

			int rowCount = distinctTables.getInt("row_count", tableRow);

			String msg = "\r\n" + tableName + ", " + rowCount + ", " + dateColumn + ", " + dateColumnType;

			int minDate = distinctTables.getInt("minimum_jddate", tableRow);
			int maxDate = distinctTables.getInt("maximum_jddate", tableRow);
			msg = msg + ", " + OCalendar.formatJd(minDate) + ", " + OCalendar.formatJd(maxDate);

			PurgeUtil.printWithDateTime(msg);

			messageText = messageText + msg;
		}

		return messageText;
	}

	/**
	 * Function to purge OL archive types
	 * @param purgeNamesToPurge List of purge names to purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 * @param checkCategory Whether to check the category of the purge name
	 * @param purgeCategory purge category to check against
	 * @return reporting string
	 */
	private String PurgeOLArchiveTypes(Table purgeNamesToPurge, Table purgeToTableMap, boolean checkCategory, String purgeCategory, String messageText) throws OException {
		messageText = messageText + "Purging OL Archive types\n";

		PurgeUtil.printWithDateTime(messageText);
		if ( checkCategory ){
			messageText = messageText + "\r\n" + "Category = " + purgeCategory;
		}

		messageText = messageText + "\r\n";

		int numPurgeNames = purgeNamesToPurge.getNumRows();

		for ( int row = 1; row <= numPurgeNames; row++) {
			String purgeType = purgeNamesToPurge.getString("purge_type", row);
			if ( !purgeType.equals("OL") ){
				continue;
			}

			String purgeName = purgeNamesToPurge.getString("purge_name", row);
			String category = purgeNamesToPurge.getString("category", row);				
			int numDaysToRetain = purgeNamesToPurge.getInt("days_to_retain", row);

			if ( checkCategory && !purgeCategory.equals(category) ) {
				PurgeUtil.printWithDateTime("Skipping purge: " + purgeName + ", category: " + category + ". Does not match specified category: " + purgeCategory);
				continue;
			}

			String msg = callPurgeOLArchiveType(purgeName, purgeToTableMap, numDaysToRetain);
			messageText = messageText + msg + "\r\n";
		}

		return messageText;
	}

	/**
	 * Warning: StlDoc.purgeHistory does not work
	 * @param purgeNamesToPurge
	 * @param purgeToTableMap
	 * @param checkCategory
	 * @param purgeCategory
	 * @return
	 * @throws OException
	 */
	private String PurgeOLUsingFunctions(Table purgeNamesToPurge, Table purgeToTableMap, boolean checkCategory, String purgeCategory) throws OException {
		
		String messageText = "Purging OL using special functions\n";

		PurgeUtil.printWithDateTime(messageText);

		if ( checkCategory ){
			messageText = messageText + "\r\n" + "Category = " + purgeCategory;
		}

		messageText = messageText + "\r\n";

		int numPurgeNames = purgeNamesToPurge.getNumRows();

		for ( int row = 1; row <= numPurgeNames; row++) {
			String purgeType = purgeNamesToPurge.getString("purge_type", row);
			if ( !purgeType.equalsIgnoreCase("FUNC") ){
				continue;
			}

			String purgeName = purgeNamesToPurge.getString("purge_name", row);
			String category = purgeNamesToPurge.getString("category", row);				
			int numDaysToRetain = purgeNamesToPurge.getInt("days_to_retain", row);

			if ( checkCategory && !purgeCategory.equals(category) ) {
				PurgeUtil.printWithDateTime("Skipping purge: " + purgeName + ", category: " + category + ". Does not match specified category: " + purgeCategory);
				continue;
			}

			PurgeUtil.printWithDateTime("Attempting to process OLF function purge_name: " + purgeName);

			if ("STLDOC_HIST".equalsIgnoreCase(purgeName))  {
				ODateTime start = ODateTime.dtNew();
				start.setDate(0);
				start.setTime(0);
				int today = Util.getBusinessDate();
				ODateTime end = ODateTime.dtNew();
				end.setDate(today - numDaysToRetain);
				end.setTime(0);

				GetBeforeStatsForAffectedTables(purgeName, purgeToTableMap, end);

				try {
					int ret = StlDoc.purgeHistory(start, end); // Note: this function seems broken
					if ( ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ) {
						messageText += "!!!ERROR!!! Failed to purge OL archive type: " + purgeName + ": error code" + ret + "\n";
					}
				} catch (Exception e) {
					messageText += "!!!ERROR!!! Failed to purge OL archive type: " + purgeName + ": " + e.getMessage() + "\n";
				} finally {
					start.destroy();
					end.destroy();
				}

				GetAfterStatsForAffectedTables(purgeName, purgeToTableMap);

				messageText += UpdateLogForAffectedTables(purgeName, purgeToTableMap, numDaysToRetain);
			} else if ("TPM_ORPHAN_WORKFLOWS".equalsIgnoreCase(purgeName) || "TPM_DISCONNECTED_VARIABLES".equalsIgnoreCase(purgeName)) {
				if (Util.userCanAccess(PURGE_TPM_SECURITY_PRIVILEGE) != 1) {
					String message = "User does not have privilege to run purge_name: TPM_ORPHAN_WORKFLOWS, missing security priv: " + PURGE_TPM_SECURITY_PRIVILEGE;
					PurgeUtil.printWithDateTime(message);

					messageText += message;

					continue;
				}

				/* The functions don't take any start/end times, they just outright purge everything.. */
				GetBeforeStatsForAffectedTables(purgeName, purgeToTableMap, null);

				try {
					
					int ret = -1;
					if ("TPM_ORPHAN_WORKFLOWS".equalsIgnoreCase(purgeName)) {
						ret = Tpm.purgeOrphanWorkflows();
					} else if ("TPM_DISCONNECTED_VARIABLES".equalsIgnoreCase(purgeName)) {
						ret = Tpm.purgeDisconnectedVariables();	
					}

					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())  {
						messageText += "!!!ERROR!!! Failed to purge OL archive type: " + purgeName + ": error code" + ret + "\n";
					}
				} catch (Exception e) {
					messageText += "!!!ERROR!!! Failed to purge OL archive type: " + purgeName + ": " + e.getMessage() + "\n";
				}

				GetAfterStatsForAffectedTables(purgeName, purgeToTableMap);

				messageText += UpdateLogForAffectedTables(purgeName, purgeToTableMap, numDaysToRetain);
			}

			PurgeUtil.printWithDateTime("Finished processing OLF function purge_name: " + purgeName);
		}

		return messageText;
	}

	/**
	 * Function to purge OL archive types
	 * @param purgeName Name of the purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 * @param numDaysToRetain number of days to retain
	 * @return reporting string
	 */
	private String callPurgeOLArchiveType(String purgeName, Table purgeToTableMap, int numDaysToRetain) throws OException {
		ODateTime dateToPurgeFrom = ODateTime.dtNew();
		dateToPurgeFrom.setDate(Util.getBusinessDate() - numDaysToRetain);
		dateToPurgeFrom.setTime(0); // midnight
		String purgeDateStr = dateToPurgeFrom.formatForDbAccess();

		int adtType = 0;

		try {
			adtType = ARCHIVE_DATA_TYPES.valueOf(purgeName).toInt();
		} catch (Exception ex) {
			return "Cannot convert purge name: " + purgeName + " to ARCHIVE_DATA_TYPE enum value";
		}

		String messageText = "Purging: " + purgeName + " for all dates from: " + purgeDateStr;

		PurgeUtil.printWithDateTime(messageText);

		String errorMsg = "";

		// gather the before stats
		GetBeforeStatsForAffectedTables(purgeName, purgeToTableMap, dateToPurgeFrom);

		try {
			if ( DataMaint.isPurgeValid(adtType) != 0 ) {
				int retVal = DataMaint.databaseData(adtType, dateToPurgeFrom.getDate());

				if ( retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ){
					errorMsg = "!!!ERROR!!! Failed to purge OL archive type: " + purgeName;
				}
			} else{
				errorMsg = "!!!ERROR!!! Cannot purge OL archive type: " + purgeName;
			}
		} catch (Exception ex) {
			errorMsg = "!!!ERROR!!! Cannot purge OL archive type: " + purgeName + ".   Exception: " + ex.getMessage();			
		}

		messageText = messageText + "\r\n" + errorMsg;

		GetAfterStatsForAffectedTables(purgeName, purgeToTableMap);

		// gather the before stats
		messageText = messageText + UpdateLogForAffectedTables(purgeName, purgeToTableMap, numDaysToRetain);

		dateToPurgeFrom.destroy();

		return messageText;		
	}

	/**
	 * Function to purge SQL data tables
	 * @param purgeNamesToPurge List of purge names to purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 * @param checkCategory Whether to check the category of the purge name
	 * @param purgeCategory purge category to check against
	 * @return reporting string
	 */
	private String PurgeSQLTables(Table purgeNamesToPurge, Table purgeToTableMap, boolean checkCategory, String purgeCategory) throws OException {
		
		String messageText = "Purging SQL tables\n";
		PurgeUtil.printWithDateTime(messageText);
		if ( checkCategory ){
			messageText = messageText + "\r\n" + "Category = " + purgeCategory;
		}

		messageText = messageText + "\r\n";

		int numPurgeNames = purgeNamesToPurge.getNumRows();

		for ( int row = 1; row <= numPurgeNames; row++) {
			String purgeType = purgeNamesToPurge.getString("purge_type", row);
			if ( !purgeType.equals("SQL") ){
				continue;
			}

			String purgeName = purgeNamesToPurge.getString("purge_name", row);
			String storedProcedure = purgeNamesToPurge.getString("stored_procedure", row);
			String category = purgeNamesToPurge.getString("category", row);				
			int numDaysToRetain = purgeNamesToPurge.getInt("days_to_retain", row);

			if ( checkCategory && !purgeCategory.equals(category) ) {
				PurgeUtil.printWithDateTime("Skipping purge: " + purgeName + ", category: " + category + ". Does not match specified category: " + purgeCategory);
				continue;
			}

			// look up the datColumn, dateColumnType and WhereClause from the purgeToTableMap
			int purgeMapEntryRow = purgeToTableMap.findString("purge_name", purgeName, SEARCH_ENUM.FIRST_IN_GROUP);
			String dateColumn = purgeToTableMap.getString("date_column", purgeMapEntryRow);
			String dateColumnType = purgeToTableMap.getString("date_column_type", purgeMapEntryRow);
			String whereClause = purgeToTableMap.getString("where_clause", purgeMapEntryRow);

			String msg = callPurgeStoredProcedure(storedProcedure, purgeName, purgeToTableMap, whereClause, dateColumn, dateColumnType, numDaysToRetain);
			messageText = messageText + msg + "\r\n";
		}

		return messageText;
	}

	/**
	 * Function to purge SQL data tables
	 * @param procedureName Stored procedure to call to execute the purge
	 * @param purgeName Name of the purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 * @param whereClause Where clause to pass into the stored procedure
	 * @param dateColumn date column to check against
	 * @param dateColumnType type of date column
	 * @param numDaysToRetain number of days to retain
	 * @return reporting string
	 */
	private String callPurgeStoredProcedure(String procedureName, String purgeName, Table purgeToTableMap, String whereClause, String dateColumn, String dateColumnType, int numDaysToRetain) throws OException {
		
		ODateTime dateToPurgeFrom = ODateTime.dtNew();
		dateToPurgeFrom.setDate(Util.getBusinessDate() - numDaysToRetain);
		dateToPurgeFrom.setTime(0); // midnight
		String purgeDateStr = dateToPurgeFrom.formatForDbAccess();

		if ( dateColumnType.equals("string") ) {
			int purgeDateInt = getTimId_yyyymmdd(dateToPurgeFrom.getDate());
			purgeDateStr = Integer.toString(purgeDateInt);
		} else if ( dateColumnType.equals("int") ) {
			int purgeDateInt = dateToPurgeFrom.getDate();
			purgeDateStr = Integer.toString(purgeDateInt);			
		}

		String messageText = "Purging: " + purgeName + " for all dates from: " + purgeDateStr;
		if ( whereClause.length() > 1 ){
			messageText = messageText + " with where clause:" + whereClause;
		}

		PurgeUtil.printWithDateTime(messageText);

		// gather the before stats
		GetBeforeStatsForAffectedTables(purgeName, purgeToTableMap, dateToPurgeFrom);

		Table args = Table.tableNew();
		args.addCol("table_name", COL_TYPE_ENUM.COL_STRING);
		args.addCol("where_clause", COL_TYPE_ENUM.COL_STRING);		
		args.addCol("date_column", COL_TYPE_ENUM.COL_STRING);
		args.addCol("date_column_type", COL_TYPE_ENUM.COL_STRING);
		args.addCol("purge_date", COL_TYPE_ENUM.COL_STRING);

		args.addRow();

		args.setString("table_name", 1, purgeName);
		args.setString("where_clause", 1, whereClause);
		args.setString("date_column", 1, dateColumn);
		args.setString("date_column_type", 1, dateColumnType);
		args.setString("purge_date", 1, purgeDateStr);

		String errorMsg = "";
		try {
			DBase.runProc(procedureName, args);
		} catch (Exception e) {		
			errorMsg = "!!!ERROR!!! Purge FAILED !! Check the user table entry.  Exception when running stored procedure: " + procedureName + ", for PurgeName: " + purgeName + ", exception "  + e.toString();
			PurgeUtil.printWithDateTime(errorMsg);
		}

		GetAfterStatsForAffectedTables(purgeName, purgeToTableMap);

		args.destroy();

		messageText = messageText + "\r\n" + errorMsg;

		// gather the before stats
		//purgeToTableMap.viewTable();
		messageText = messageText + UpdateLogForAffectedTables(purgeName, purgeToTableMap, numDaysToRetain);

		dateToPurgeFrom.destroy();

		return messageText;
	}

	/**
	 * Function to gather stats about tables before they are affected by purge
	 * @param purgeName Name of the purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 */
	private void GetBeforeStatsForAffectedTables(String purgeName, Table purgeToTablesMap, ODateTime dateToPurgeFrom) throws OException {
		int firstRow = purgeToTablesMap.findString("purge_name", purgeName, SEARCH_ENUM.FIRST_IN_GROUP);
		int lastRow = purgeToTablesMap.findString("purge_name", purgeName, SEARCH_ENUM.LAST_IN_GROUP);

		ODateTime todayDateTime = ODateTime.dtNew();
		todayDateTime.setDate(Util.getBusinessDate()); 
		todayDateTime.setTime(Util.timeGetServerTime()); 

		for (int tableRow = firstRow; tableRow <= lastRow; tableRow++) {
			String tableName = purgeToTablesMap.getString("table_name", tableRow);

			if (dateToPurgeFrom != null) {
				purgeToTablesMap.setDateTime("purged_from_date", tableRow, dateToPurgeFrom);	
			}
			purgeToTablesMap.setDateTime("start_time", tableRow, todayDateTime);

			int count = GetNumberOfRowsInTable(tableName);			
			purgeToTablesMap.setInt("before_row_count", tableRow, count);
		}

		todayDateTime.destroy();
	}

	/**
	 * Function to get rowcount in a table
	 * @param purgeName Name of the table
	 * @return number of rows in a table
	 */
	private int GetNumberOfRowsInTable(String tableName) throws OException {

		// do a count * - but efficiently
		Table sqlCount = Table.tableNew("count");

		String whatClause = "SUM(b.row_count)";
		String fromClause = "sysobjects a, sys.dm_db_partition_stats b";
		String whereClause = "b.object_id = a.id and a.name = '" + tableName + "' and a.type = 'U' AND (index_id < 2)";

		int iRetVal = DBaseTable.loadFromDbWithSQL(sqlCount, whatClause, fromClause, whereClause);  
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			sqlCount.destroy();
			throw new OException("Failed to do count for table: " + tableName);		
		}
		// ok to cast as I refuse to believe even an OL table can get bigger than 2 billion		
		int count = (int)sqlCount.getInt64(1, 1);	

		sqlCount.destroy();

		return count;
	}

	/**
	 * Function to gather stats about tables after they are affected by purge
	 * @param purgeName Name of the purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 */
	private void GetAfterStatsForAffectedTables(String purgeName, Table purgeToTablesMap) throws OException {
		int firstRow = purgeToTablesMap.findString("purge_name", purgeName, SEARCH_ENUM.FIRST_IN_GROUP);
		int lastRow = purgeToTablesMap.findString("purge_name", purgeName, SEARCH_ENUM.LAST_IN_GROUP);

		ODateTime todayDateTime = ODateTime.dtNew();
		todayDateTime.setDate(Util.getBusinessDate()); 
		todayDateTime.setTime(Util.timeGetServerTime());

		for ( int tableRow = firstRow; tableRow <= lastRow; tableRow++) {
			String tableName = purgeToTablesMap.getString("table_name", tableRow);
			purgeToTablesMap.setDateTime("end_time", tableRow, todayDateTime);

			int count = GetNumberOfRowsInTable(tableName);

			purgeToTablesMap.setInt("remaining_rows", tableRow, count);
			int beforeRowCount = purgeToTablesMap.getInt("before_row_count", tableRow);

			// do the diffs
			int numberRowsPurged = beforeRowCount - count;
			if ( numberRowsPurged < 0 ) {
				// guess this could happen if
				numberRowsPurged = 0;
			}
				

			purgeToTablesMap.setInt("number_rows_purged", tableRow, numberRowsPurged);

			ODateTime startTime = purgeToTablesMap.getDateTime("start_time", tableRow);

			// calc the difference in times
			int numSecsDifference = startTime.computeTotalSecondsInGMTDateRange(todayDateTime);			
			purgeToTablesMap.setInt("purge_elapsed_time_secs", tableRow, numSecsDifference);

		}

		todayDateTime.destroy();

	}

	/**
	 * Function to generate messages about the affected tables
	 * @param purgeName Name of the purge
	 * @param purgeToTableMap Map of purge names to tables that will be purged
	 * @return reporting string
	 */
	private String UpdateLogForAffectedTables(String purgeName, Table purgeToTablesMap, int daysToRetain) throws OException {

		int firstRow = purgeToTablesMap.findString("purge_name", purgeName, SEARCH_ENUM.FIRST_IN_GROUP);
		int lastRow = purgeToTablesMap.findString("purge_name", purgeName, SEARCH_ENUM.LAST_IN_GROUP);

		String messageText = "";

		Table arguments = Table.tableNew();
		arguments.addCol("purge_name", COL_TYPE_ENUM.COL_STRING);
		arguments.addCol("table_name", COL_TYPE_ENUM.COL_STRING);
		arguments.addCol("days_to_retain", COL_TYPE_ENUM.COL_INT);
		arguments.addCol("purged_from_date", COL_TYPE_ENUM.COL_DATE_TIME);
		arguments.addCol("purge_elapsed_time_secs", COL_TYPE_ENUM.COL_INT);
		arguments.addCol("number_rows_purged", COL_TYPE_ENUM.COL_INT);
		arguments.addCol("remaining_rows", COL_TYPE_ENUM.COL_INT);

		for ( int tableRow = firstRow; tableRow <= lastRow; tableRow++) {

			String tableName = purgeToTablesMap.getString("table_name", tableRow);
			int numRowsPurged = purgeToTablesMap.getInt("number_rows_purged", tableRow);
			int numRowsLeft = purgeToTablesMap.getInt("remaining_rows", tableRow);
			int purgeDuration = purgeToTablesMap.getInt("purge_elapsed_time_secs", tableRow);
			ODateTime purgedFromDate = purgeToTablesMap.getDateTime("purged_from_date", tableRow);

			String msg = "Purge name: " + purgeName + ", Table: " + tableName + ", Purged From Date: " + OCalendar.formatJd(purgedFromDate.getDate()) + ", Rows Purged: " + numRowsPurged + ", Num Remaining Rows: " + numRowsLeft + ", Duration(Secs): " + purgeDuration;
			PurgeUtil.printWithDateTime(msg);
			messageText = messageText + msg + "\r\n";

			// update the log
			arguments.clearRows();
			arguments.addRow();
			arguments.setString("purge_name", 1, purgeName);
			arguments.setString("table_name", 1, tableName);
			arguments.setInt("days_to_retain", 1, daysToRetain);
			arguments.setDateTime("purged_from_date", 1, purgedFromDate);
			arguments.setInt("purge_elapsed_time_secs", 1, purgeDuration);
			arguments.setInt("number_rows_purged", 1, numRowsPurged);
			arguments.setInt("remaining_rows", 1, numRowsLeft);

			int iRetVal = DBase.runProc("USER_add_purge_log_entry", arguments);
			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				PurgeUtil.printWithDateTime("Unable to execute USER_add_purge_log_entry");
				PurgeUtil.printWithDateTime(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of USER_add_purge_log_entry failed"));
				throw new OException("Unable to execute USER_add_purge_log_entry");
			}			
		}

		arguments.destroy();
		return messageText;
	}


	/**
	 * Function to purge AFS data files
	 * @return updated reporting string
	 */
	private String PurgeAFSFiles() throws OException {			
		String messageText = "Purging AFS files";
		PurgeUtil.printWithDateTime(messageText);
		messageText = messageText + "\r\n";

		Table afsEntries = retrieveAPMAFSFileNames();

		int numRows = afsEntries.getNumRows();
		for(int iRow = 1; iRow <= numRows; iRow++) {
			String fileName = afsEntries.getString("filename", iRow);
			String msg = "Deleting from AFS file: " + fileName;
			PurgeUtil.printWithDateTime(msg);
			messageText = messageText + msg + "\r\n";
			Afs.deleteTable(fileName, 1); 
		}

		return messageText;
	}

	/**
	 * Saves the report to file
	 * @param fileName filename to save to
	 * @param reportText Reporting string so far
	 */
	private void SaveReportToFile(String fileName, String reportText) throws OException {
		PurgeUtil.printWithDateTime("Saving report to file:" + fileName);

		PrintWriter out = null;
		try {
			out = new PrintWriter(fileName);
			out.println("PURGE Report");
			out.println(reportText);
		} catch (Exception e) {
			PurgeUtil.printWithDateTime("Failed to save file: " + fileName);
			e.printStackTrace();
		} finally {
			if ( out != null ){
				out.close();
			}
		}

		return;
	}

	/**
	 * Utility function used to get the list of AFS APM entries
	 * @return table of the AFS entries
	 */	
	private Table retrieveAPMAFSFileNames() throws OException 	{
		Table afsEntries = Table.tableNew();

		// only purge entries over a week old
		ODateTime todayDateTime = ODateTime.dtNew();
		todayDateTime.setDate(Util.getBusinessDate()-14); 
		todayDateTime.setTime(0); // midnight

		String filesToFind = " (filename like '%APMFailedBatchArgt%' or filename like '%APM_Analyse%' or filename like '%APM_DealUpdates%' or filename like '%APM_ServiceHealth%')";
		String sqlWhereStr = filesToFind + " and last_update < '" + todayDateTime.formatForDbAccess() + "'";
		DBaseTable.loadFromDbWithSQL(afsEntries, "filename", "abacus_file_System", sqlWhereStr);

		return afsEntries;
	}	

	private int getTimId_yyyymmdd(int intDate) throws OException {

		int intMonthMM,
		intDayDD,
		intYear4,
		intResult;


		intMonthMM = OCalendar.getMonth(intDate);

		intDayDD = intDate - OCalendar.getSOM(intDate) + 1;

		intYear4 = OCalendar.getYear(intDate);

		intResult = (intYear4 * 10000) + (intMonthMM * 100) + intDayDD;

		return intResult;
	}

	/**
	 * Email the report to EMAIL_ADDRESS
	 * @param fileName filename to save to
	 */
	private void emailReport(String fileName) throws OException {
		
		PurgeUtil.printWithDateTime("Emailing report:" + fileName);

		String emailBody = "Purge Completed. Please see details in attachment";

		EmailMessage msg = null;

		try {
			msg = EmailMessage.create();
			msg.addSubject("Openlink Purge Report");

			String emailAddresses = "";
			for( int i=1; i<= emailUserList.getNumRows(); ++i ) {
				String user = emailUserList.getString(1, i);
				emailAddresses += user;
				if( i != emailUserList.getNumRows() ) {
					emailAddresses += ";";
				}
			}
			emailAddresses = Utils.convertUserNamesToEmailList(emailAddresses);
			PurgeUtil.printWithDateTime("Emailing recipients: " + emailAddresses);
			msg.addRecipients(emailAddresses);

			msg.addBodyText(emailBody, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			msg.addAttachments(fileName, 0, null);
			msg.send("Mail");
		} catch (Exception e) {
			PurgeUtil.printWithDateTime("Failed to email file: " + fileName);
			e.printStackTrace();
		} finally {
			if ( msg != null) {
				msg.dispose();
			}
		}

		return;
	}
}


