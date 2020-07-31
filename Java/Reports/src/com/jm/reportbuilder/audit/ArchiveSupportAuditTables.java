package com.jm.reportbuilder.audit;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
/**
 * This class purges tables specified in the USER_jm_purge_config.  
 * It will purge data based on a datetime column in a table and the specified number of days from the config
 */
public class ArchiveSupportAuditTables implements IScript
{
	 

	private static final String STORED_PROC_ARCHIVE_E = "USER_archive_audit_table";  // with Explanation

	private static final String STORED_PROC_ARCHIVE_PER_AUDIT = "USER_archive_per_audit_table";			
	
	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Archive";
	protected static final String REPO_SUBCONTEXT = "SupportAudit";



	@Override
	public void execute(IContainerContext context) throws OException {

		// Setting up the log file.
		setupLog();
		boolean hasSecurity = doesUserHaveSecurity() ;
		if (hasSecurity){
			// Purging USER_supportpersonnelanalysis_h
			String tableName = "USER_support_personnel_analysis";
			String archiveTableName = "USER_supportpersonnelanalysis_h";
			String columnName = "per_modified_date";
			//purgeAuditTable(tableName, archiveTableName,columnName);

			// Purging USER_support_change_audit
			tableName = "USER_support_change_audit";
			archiveTableName = "USER_support_change_audit_h";
			columnName = "modified_date";
			purgeAuditTable(tableName, archiveTableName,columnName);
			
			// Purging USER_support_personnel_audit_h
			tableName = "USER_support_personnel_audit";
			archiveTableName = "USER_support_personnel_audit_h";
			columnName = "per_modified_date";
			//purgeAuditTable1(tableName, archiveTableName, columnName);

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

	private void purgeAuditTable(String tableName , String archiveTableName, String column_name) throws OException {
		
		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, REPO_SUBCONTEXT);

		Table argumentTableForStoredProcedure = Util.NULL_TABLE;
		try {
			Logging.info("Start  " + getClass().getSimpleName());
			long startTime = System.currentTimeMillis();
			long previousTime = startTime;
			
			String weeksToKeepFullStr = constRepo.getStringValue("Records_To_Keep", "-1m"); // 2
			
			Logging.info("Start  " + getClass().getSimpleName() + " TableName:" + tableName + " Const: " + weeksToKeepFullStr); 		
			int weeksToKeepFull = OCalendar.parseString(weeksToKeepFullStr);
			int iToday = OCalendar.today();
			int daysToDrop = iToday - weeksToKeepFull;  
			
			argumentTableForStoredProcedure = Table.tableNew();
			
			argumentTableForStoredProcedure.addCol("table_name", COL_TYPE_ENUM.COL_STRING);
			argumentTableForStoredProcedure.addCol("archive_table", COL_TYPE_ENUM.COL_STRING);
			argumentTableForStoredProcedure.addCol("retain_days", COL_TYPE_ENUM.COL_INT);
			argumentTableForStoredProcedure.addCol("column_name", COL_TYPE_ENUM.COL_STRING);
			

				
			int returnValue = 0;
				

			String USER_StoredProcedureName = STORED_PROC_ARCHIVE_E;
			argumentTableForStoredProcedure.clearRows();


			argumentTableForStoredProcedure.addRow();					
			argumentTableForStoredProcedure.setString("table_name", 1, tableName);
			argumentTableForStoredProcedure.setInt("retain_days", 1, daysToDrop);
			argumentTableForStoredProcedure.setString("archive_table", 1, archiveTableName);
			argumentTableForStoredProcedure.setString("column_name", 1, column_name);
			
			returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);
			
			long endTime = System.currentTimeMillis();
			Logging.info("Archive Table: " + tableName + " retVal: " + returnValue + " Time elapsed (milliseconds): " + (endTime-previousTime));
			previousTime = endTime;

			

			endTime = System.currentTimeMillis();
			Logging.info(String.format("Plugin execution completed. Time elapsed (milliseconds): %d",endTime-startTime));
		} catch (Exception ex) {
			String message = "Script failed with the following error(s): " + ex.getMessage();
			Logging.error(message);
			Util.exitFail(message);
		} finally {
			argumentTableForStoredProcedure.destroy(); 
		}

	}
	@SuppressWarnings("unused")
	private void purgeAuditTable1(String tableName , String archiveTableName,String column_name) throws OException {
		
		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, REPO_SUBCONTEXT);

		Table argumentTableForStoredProcedure = Util.NULL_TABLE;
		try {
			
			long startTime = System.currentTimeMillis();
			long previousTime = startTime;
			
			String weeksToKeepFullStr = constRepo.getStringValue("Records_To_Keep", "-1m"); // 2
			
			Logging.info("Start  " + getClass().getSimpleName() + " TableName:" + tableName + " Const: " + weeksToKeepFullStr);		
			int weeksToKeepFull = OCalendar.parseString(weeksToKeepFullStr);
			int iToday = OCalendar.today();
			int daysToDrop = iToday - weeksToKeepFull;  
			
			argumentTableForStoredProcedure = Table.tableNew();
			
			argumentTableForStoredProcedure.addCol("table_name", COL_TYPE_ENUM.COL_STRING);
			argumentTableForStoredProcedure.addCol("archive_table", COL_TYPE_ENUM.COL_STRING);
			argumentTableForStoredProcedure.addCol("retain_days", COL_TYPE_ENUM.COL_INT);
			argumentTableForStoredProcedure.addCol("column_name", COL_TYPE_ENUM.COL_STRING);
			
 
				
			int returnValue = 0;
				
	
			String USER_StoredProcedureName = STORED_PROC_ARCHIVE_PER_AUDIT;
			argumentTableForStoredProcedure.clearRows();
	
	
			argumentTableForStoredProcedure.addRow();					
			argumentTableForStoredProcedure.setString("table_name", 1, tableName);
			argumentTableForStoredProcedure.setInt("retain_days", 1, daysToDrop);
			argumentTableForStoredProcedure.setString("archive_table", 1, archiveTableName);
			argumentTableForStoredProcedure.setString("column_name", 1, column_name);
			
	
			returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);
			
			long endTime = System.currentTimeMillis();
			Logging.info("Archive Table: " + tableName + " retVal: " + returnValue + " Time elapsed (milliseconds): " + (endTime-previousTime));
			previousTime = endTime;

				


			

			endTime = System.currentTimeMillis();
			Logging.info(String.format("Plugin execution completed. Time elapsed (milliseconds): %d",endTime-startTime));
		} catch (Exception ex) {
			String message = "Script failed with the following error(s): " + ex.getMessage();
			Logging.error(message);
			Util.exitFail(message);
		} finally {
			argumentTableForStoredProcedure.destroy(); 
		}

	}

	


	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException {
		try {
			Logging.init(this.getClass(), REPO_CONTEXT, REPO_SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}         

	


	
}


