package com.matthey.openlink.purge;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * This class purges tables specified in the USER_jm_purge_config.  
 * It will purge data based on a datetime column in a table and the specified number of days from the config
 */
public class PurgeOpenPositionTables implements IScript
{
	 

	private static final String STORED_PROC_PURGE = "USER_purge_jm_open_tp";
	private static final String STORED_PROC_ARCHIVE = "USER_archive_jm_open_tp";
	private static final String STORED_PROC_BACKUP = "USER_backup_jm_open_tp";
	
	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Purge_OpenPositionTables";



	@Override
	public void execute(IContainerContext context) throws OException {

		// Setting up the log file.
		setupLog();
		boolean hasSecurity = doesUserHaveSecurity() ;
		if (hasSecurity){
			// Setting up for main table.
			String tableName = "USER_JM_Open_trading_position";
			String archiveTableName = "USER_jm_dailysnapshot_otp";
			String backupTableName = "USER_jm_backup_otp"; 
			purgeOpenTable(tableName, archiveTableName, backupTableName);
			
			// Setting up for CN table.
			tableName = "USER_JM_Open_trading_position_cn";
			archiveTableName = "USER_jm_dailysnapshot_otp_cn";
			backupTableName = "USER_jm_backup_otp_cn"; 
			purgeOpenTable(tableName, archiveTableName, backupTableName);
		}
		Util.exitSucceed();
	}

	private boolean doesUserHaveSecurity() throws OException {
		int PURGE_SECURITY_PRIVILEGE = 22062; // 22062 - Purge Archive Data Types
		if (Util.userCanAccess(PURGE_SECURITY_PRIVILEGE) != 1) {
			String message = "User " + Ref.getUserName() + " is not allowed to run purge task. Security Right " + PURGE_SECURITY_PRIVILEGE + " is missing";
			throw new OException(message);
		}

		
		return true;
	}

	private void purgeOpenTable(String tableName , String archiveTableName, String backupTableName) throws OException {
		
		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, tableName);

		Table dataSetsToProcess = Util.NULL_TABLE;
		try {
			PluginLog.info("Start  " + getClass().getSimpleName());
			long startTime = System.currentTimeMillis();
			long previousTime = startTime;
			
			String weeksToKeepFullStr = constRepo.getStringValue("Weeks_to_Keep_Full", "-2w"); // 2
			String weeksToKeepDailyStr = constRepo.getStringValue("Weeks_to_Keep_Daily", "-6m"); // 6 months
			
			int lastExtractDate = constRepo.getIntValue("Last_Extract_Date",0);
			int lastBackupExtractDate = constRepo.getIntValue("Last_Backup_Extract_Date",0);
			int lastBackupExtractTime = constRepo.getIntValue("Last_Backup_Extract_Time", 0); 
						
			int weeksToKeepFull = OCalendar.parseString(weeksToKeepFullStr);
			int weeksToKeepDaily = OCalendar.parseString(weeksToKeepDailyStr);
			
			dataSetsToProcess = getDataSetsToPurge(weeksToKeepFull, tableName);
			int totalRows = dataSetsToProcess.getNumRows();
			Table argumentTableForStoredProcedure = Table.tableNew();
			
			argumentTableForStoredProcedure.addCol("table_name", COL_TYPE_ENUM.COL_STRING);
			argumentTableForStoredProcedure.addCol("extract_date", COL_TYPE_ENUM.COL_INT);
			argumentTableForStoredProcedure.addCol("extract_time", COL_TYPE_ENUM.COL_INT);
			argumentTableForStoredProcedure.addCol("archive_table_name", COL_TYPE_ENUM.COL_STRING);
			
			PluginLog.info("Purging Table: " + tableName + " Found Rows: " + totalRows);
			
			for (int iLoop = 1; iLoop<=totalRows;iLoop++){
				
				int extractDate = dataSetsToProcess.getInt("extract_date", iLoop);
				int extractTime = dataSetsToProcess.getInt("extract_time", iLoop);
				int rowsToDelete = dataSetsToProcess.getInt("rows_to_del", iLoop);
				String USER_StoredProcedureName = STORED_PROC_PURGE;
				argumentTableForStoredProcedure.clearRows();

				PluginLog.info("Purging Table: " + tableName + " dataset:" + iLoop + " of " + totalRows + " extractDate: " + extractDate + " extractTime: " + extractTime + " rowsToDelete: " + rowsToDelete);
				argumentTableForStoredProcedure.addRow();
				
				argumentTableForStoredProcedure.setString("table_name", 1, tableName);
				argumentTableForStoredProcedure.setInt("extract_date", 1, extractDate);
				argumentTableForStoredProcedure.setInt("extract_time", 1, extractTime);
				argumentTableForStoredProcedure.setString("archive_table_name", 1, archiveTableName);
				
				int returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);
				
				long endTime = System.currentTimeMillis();
				PluginLog.info("Purging Table: " + tableName + " retVal: " + returnValue + " Time elapsed (milliseconds): " + (endTime-previousTime));
				previousTime = endTime;  //iLoop=totalRows;

			}
			
			dataSetsToProcess.destroy();
			dataSetsToProcess = Util.NULL_TABLE;
			dataSetsToProcess = getDataSetsToArchive(tableName);
			totalRows = dataSetsToProcess.getNumRows();
			
			PluginLog.info("Archiving Table: " + tableName + " Found Rows: " + totalRows);
			int extractDate = 0; 
			for (int iLoop = 1; iLoop<=totalRows;iLoop++){
				
				extractDate = dataSetsToProcess.getInt("extract_date", iLoop);
				int extractTime = dataSetsToProcess.getInt("extract_time", iLoop);
				int returnValue = 0;
				if (extractDate>lastExtractDate){
					
					int rowsToDelete = dataSetsToProcess.getInt("rows_to_del", iLoop);
					String USER_StoredProcedureName = STORED_PROC_ARCHIVE;
					argumentTableForStoredProcedure.clearRows();
	
					PluginLog.info("Archive Dataset: " + tableName + " dataset:" + iLoop + " of " + totalRows + " extractDate: " + extractDate + " extractTime: " + extractTime + " rowsToDelete: " + rowsToDelete);
					argumentTableForStoredProcedure.addRow();					
					argumentTableForStoredProcedure.setString("table_name", 1, tableName);
					argumentTableForStoredProcedure.setInt("extract_date", 1, extractDate);
					argumentTableForStoredProcedure.setInt("extract_time", 1, extractTime);
					argumentTableForStoredProcedure.setString("archive_table_name", 1, archiveTableName);
					
					returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);
					
					long endTime = System.currentTimeMillis();
					PluginLog.info("Archive Table: " + tableName + " retVal: " + returnValue + " Time elapsed (milliseconds): " + (endTime-previousTime));
					previousTime = endTime;

				} else {
					returnValue=1;
				}
				
				if (returnValue==1){
					if (weeksToKeepDaily >extractDate){
						argumentTableForStoredProcedure.clearRows();
						int rowsToDelete = dataSetsToProcess.getInt("rows_to_del", iLoop);
						PluginLog.info("Purge Old Daily Snapshot Dataset: " + tableName + " dataset:" + iLoop + " of " + totalRows + " extractDate: " + extractDate + " extractTime: " + extractTime + " rowsToDelete: " + rowsToDelete);
						argumentTableForStoredProcedure.addRow();						
						argumentTableForStoredProcedure.setString("table_name", 1, tableName);
						argumentTableForStoredProcedure.setInt("extract_date", 1, extractDate);
						argumentTableForStoredProcedure.setInt("extract_time", 1, extractTime);
						argumentTableForStoredProcedure.setString("archive_table_name", 1, archiveTableName);

						String USER_StoredProcedureName = STORED_PROC_PURGE;
						returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);
						
						long endTime = System.currentTimeMillis();
						PluginLog.info("Purge Old Daily Snapshot Table: " + tableName + " retVal: " + returnValue + " Time elapsed (milliseconds): " + (endTime-previousTime));
						previousTime = endTime;


					}

				}
			}

			dataSetsToProcess.destroy();
			dataSetsToProcess = Util.NULL_TABLE;
			dataSetsToProcess = getMaxDateTime(tableName);
			totalRows = dataSetsToProcess.getNumRows();

			int maxExtractDate = dataSetsToProcess.getInt("extract_date", 1);
			int maxExtractTime = dataSetsToProcess.getInt("extract_time", 1);
			
			PluginLog.info("Backing Latest data : " + tableName + " Latest Extract Date: " + maxExtractDate + " Latest Extract Time: " + maxExtractTime );
			
			for (int iLoop = 1; iLoop<=totalRows;iLoop++){
				
			
				String USER_StoredProcedureName = STORED_PROC_BACKUP;
				argumentTableForStoredProcedure.clearRows();

				PluginLog.info("Running Backup Data: " + tableName + " dataset:" + iLoop + " of " + totalRows + " lastBackupExtractDate: " + lastBackupExtractDate + " lastBackupExtractTime: " + lastBackupExtractTime );
				argumentTableForStoredProcedure.addRow();
				
				argumentTableForStoredProcedure.setString("table_name", 1, tableName);
				argumentTableForStoredProcedure.setInt("extract_date", 1, lastBackupExtractDate);
				argumentTableForStoredProcedure.setInt("extract_time", 1, lastBackupExtractTime);
				archiveTableName = backupTableName;
				argumentTableForStoredProcedure.setString("archive_table_name", 1, archiveTableName);
				
				int returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);
				
				long endTime = System.currentTimeMillis();
				PluginLog.info("Backup Table: " + tableName + " retVal: " + returnValue + " Time elapsed (milliseconds): " + (endTime-previousTime));
				previousTime = endTime;
				
			}

			
			if (maxExtractDate>0){
				updateConstRepo(tableName,extractDate,  maxExtractDate,maxExtractTime);
			}

			long endTime = System.currentTimeMillis();
			PluginLog.info(String.format("Plugin execution completed. Time elapsed (milliseconds): %d",endTime-startTime));
		} catch (Exception ex) {
			String message = "Script failed with the following error(s): " + ex.getMessage();
			PurgeUtil.printWithDateTime(message);
			Util.exitFail(message);
		} finally {
			dataSetsToProcess.destroy(); 
		}

	}

	private Table getDataSetsToArchive(String tableName) throws OException {
		String sqlCommand = "SELECT ujotp.extract_date, ujotp.extract_time , COUNT (ujotp.extract_time ) rows_to_del  , ujotp1.count_record max_records\n" + 
							 "FROM " + tableName + " ujotp \n" +
							 "JOIN  (\n" +
							 "SELECT u.extract_date, max (u.extract_time) extract_time , u.count_record  FROM\n" + 
							 "(SELECT i.extract_date, i.extract_time  , COUNT (*)  count_record FROM " + tableName + " i GROUP BY i.extract_date , i.extract_time ) u\n" + 
							 "JOIN (\n" +
							 "   SELECT i.extract_date ,  MAX (i.count_record) max_records_found\n" +   
							 "   FROM ( \n" +
							 "       SELECT i.extract_date, i.extract_time  , COUNT (*)  count_record from " + tableName + " i\n" +
							 "       GROUP BY i.extract_date , i.extract_time  ) i\n" +
							 "   GROUP BY i.extract_date) m_user ON (m_user.extract_date = u.extract_date AND m_user.max_records_found = u.count_record)\n" + 
							 "GROUP BY u.extract_date, u.count_record \n" +
							 ")  ujotp1 ON ( ujotp1.extract_date= ujotp.extract_date AND  ujotp1.extract_time =  ujotp.extract_time)\n" +
							 "GROUP BY ujotp.extract_date, ujotp.extract_time , ujotp1.count_record\n" +
//							 "HAVING   ujotp.extract_date > " + lastRunDate +"\n" +
							 "ORDER BY ujotp.extract_date, ujotp.extract_time";

		Table tblResultsData = Table.tableNew();
		DBaseTable.execISql(tblResultsData, sqlCommand);
	
		
		return tblResultsData;
	}

	private Table getMaxDateTime(String tableName) throws OException {
		String sqlCommand = "SELECT MAX(ujotp.extract_date) extract_date, MAX(ujotp.extract_time)extract_time\n" +
							"FROM " + tableName + " ujotp\n" +
							"JOIN (SELECT MAX(extract_date) extract_date FROM " + tableName + ")m_ujotp\n" + 
							" ON (m_ujotp.extract_date=ujotp.extract_date)";
		
	
		Table tblResultsData = Table.tableNew();
		DBaseTable.execISql(tblResultsData, sqlCommand);
	
		
		return tblResultsData;
	}

	private Table getDataSetsToPurge(int weeksToKeep, String tableName) throws OException {
		
		String sqlCommand = "SELECT ujotp.extract_date, ujotp.extract_time , COUNT (ujotp.extract_time ) rows_to_del  , ujotp1.count_record max_records\n" + 
							 "FROM " + tableName + " ujotp \n" +
							 "JOIN  (\n" +
							 "SELECT u.extract_date, max (u.extract_time) extract_time , u.count_record  FROM\n" + 
							 "(SELECT i.extract_date, i.extract_time  , COUNT (*)  count_record FROM " + tableName + " i GROUP BY i.extract_date , i.extract_time ) u\n" + 
							 "JOIN (\n" +
							 "   SELECT i.extract_date ,  MAX (i.count_record) max_records_found\n" +   
							 "   FROM ( \n" +
							 "       SELECT i.extract_date, i.extract_time  , COUNT (*)  count_record from " + tableName + " i\n" +
							 "       GROUP BY i.extract_date , i.extract_time  ) i\n" +
							 "   GROUP BY i.extract_date) m_user ON (m_user.extract_date = u.extract_date AND m_user.max_records_found = u.count_record)\n" + 
							 "GROUP BY u.extract_date, u.count_record \n" +
							 ")  ujotp1 ON ( ujotp1.extract_date= ujotp.extract_date AND  ujotp1.extract_time !=  ujotp.extract_time)\n" +
							 "GROUP BY ujotp.extract_date, ujotp.extract_time , ujotp1.count_record\n" +
							 "HAVING   ujotp.extract_date < " + weeksToKeep +"\n" +
							 "ORDER BY ujotp.extract_date, ujotp.extract_time";

		Table tblResultsData = Table.tableNew();
		DBaseTable.execISql(tblResultsData, sqlCommand);

		
		return tblResultsData;
	}

	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException {
		
		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT);

		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";


		String logLevel = constRepo.getStringValue("logLevel","DEBUG");
		String logFile = constRepo.getStringValue("logFile", "Purge_OpenPositionTables.log");
		String logDir = constRepo.getStringValue("logDir", abOutDir);

		try {

			PluginLog.init(logLevel, logDir, logFile);

		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}         

	

	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	private void updateConstRepo(String tableName,int lastExtractDate, int lastBackupExtractDate, int lastBackupExtractTime) throws OException {

		PluginLog.info("Updating the constant repository with the latest run details lastBackupExtractDate:" + lastBackupExtractDate + " lastBackupExtractTime:" + lastBackupExtractTime);

		Table updateTime = Table.tableNew();
		int retVal = 0;
		


		try {
			updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);

			updateTime.addRow();
			updateTime.addRow();
			updateTime.addRow();

			updateTime.setColValString("context", REPO_CONTEXT );
			updateTime.setColValString("sub_context", tableName );
			ODateTime dt = ODateTime.getServerCurrentDateTime();
			updateTime.setColValDateTime("date_value", dt);
			
			updateTime.setString("name",1, "Last_Extract_Date");
			updateTime.setString("name",2, "Last_Backup_Extract_Date");
			updateTime.setString("name",3, "Last_Backup_Extract_Time");
			updateTime.setInt("int_value", 1, lastExtractDate);
			updateTime.setInt("int_value", 2, lastBackupExtractDate);
			updateTime.setInt("int_value" ,3, lastBackupExtractTime);
			
			
			updateTime.setTableName("USER_const_repository");

			updateTime.group("context,sub_context,name");

			try {
				// Update database table
				retVal = DBUserTable.update(updateTime);
				if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
				}
			} catch (OException e) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
				throw new OException(e.getMessage());
			}

		} catch (OException e) {

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		} finally {
			if (Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
			}
		}

	}

	
}


