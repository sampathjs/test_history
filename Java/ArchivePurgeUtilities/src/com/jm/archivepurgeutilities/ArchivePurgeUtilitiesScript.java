package com.jm.archivepurgeutilities;

import static com.jm.enums.PurgeProcedureArgumentsTableColumns.RETAIN_DAYS_ARGUMENT_TABLE_COLUMN;

import java.util.ArrayList;
import java.util.List;

import com.jm.archivepurgeutilities.exception.ArchivePurgeUtilitiesRuntimeException;
import com.jm.archivepurgeutilities.interfaces.ArchivePurgeUtilitiesInterface;
import com.jm.archivepurgeutilities.util.ConstRepoConfig;
import com.jm.data.LogTableData;
import com.jm.enums.ArchiveProcedureArgumentsTableColumns;
import com.jm.enums.PurgeProcedureArgumentsTableColumns;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/**
 * A utility to archive or purge data in Endur database as per user configuration. This utility archives data as per configuration in USER_archive_config and purges data as per configuration in USER_purge_config. USER_sp_archive_data and
 * USER_sp_purge_data procedures are used to archive and purge data.
 * 
 * @author SharmV03
 * 
 */
public abstract class ArchivePurgeUtilitiesScript implements IScript, ArchivePurgeUtilitiesInterface
{
	private ODateTime retainDateODateTime;
	private String emailReceipients;
	String subject;
	private List<String> emailBodyList = new ArrayList<String>();
	protected Table configurationTable;

	@Override
	public void execute(IContainerContext context) throws OException
	{
		Table argumentTableForStoredProcedure = null;
		try
		{
			int startSeconds;

			com.jm.archivepurgeutilities.util.Util.setupLog();
			PluginLog.info("Started execute() method of " + this.getClass().getSimpleName());

			fillConfigurationData();

			int numberOfRows = configurationTable.getNumRows();
			emailReceipients = ConstRepoConfig.getValue("emailReceipients", "");

			/*
			 * Iterate through data from USER_ARCHIVE_CONFIG to archive table as per configuration in USER_ARCHIVE_CONFIG
			 */
			for (int row = 1; row <= numberOfRows; row++)
			{
				startSeconds = (int) (System.currentTimeMillis() / 1000);
				argumentTableForStoredProcedure = generateStoredProcedureInputArgs(row);
				if (null != argumentTableForStoredProcedure && argumentTableForStoredProcedure.getNumRows() != 0)
				{
					performOperation(startSeconds, argumentTableForStoredProcedure, row);
				}
				else
				{
					PluginLog.error("Data in not valid in " + getConfigurationUserTableName() + " at row " + row);
				}
			}
			sendReportsToCommonReceipients();
			PluginLog.info("Completed execution() method of " + this.getClass().getSimpleName());
		}
		catch (Throwable throwable)
		{
			String message = "Exception occurred: " + throwable.getMessage();
			PluginLog.error(message);
			throw new ArchivePurgeUtilitiesRuntimeException(message, throwable);
		}
		finally
		{
			if (Table.isTableValid(configurationTable) == 1)
			{
				configurationTable.destroy();
			}
			if (ODateTime.isNull(retainDateODateTime) == 0)
			{
				retainDateODateTime.destroy();
			}
		}
	}

	/**
	 * 
	 * Runs procedure and records status.
	 * 
	 * @param startSeconds
	 * @param argumentTableForStoredProcedure
	 * @param row
	 * @throws OException
	 */
	private void performOperation(int startSeconds, Table argumentTableForStoredProcedure, int row) throws OException
	{
		String eMailBody;
		String tableName;
		String query;
		int remainingRows;
		Table deletedData = null;
		Table resultTable = Util.NULL_TABLE;

		try
		{
			tableName = argumentTableForStoredProcedure.getString(ArchiveProcedureArgumentsTableColumns.TABLE_NAME_ARGUMENT_TABLE_COLUMN.toString(), 1);
			deletedData = getDataToBeDeleted(argumentTableForStoredProcedure);

			process(argumentTableForStoredProcedure);
			PluginLog.debug("Processed rows:" + deletedData.getNumRows());

			int endSeconds = (int) (System.currentTimeMillis() / 1000);
			int elapsedTimeInSeconds = endSeconds - startSeconds;

			LogTableData logTableData = getLogTableData(startSeconds, argumentTableForStoredProcedure, deletedData, elapsedTimeInSeconds);
			insertAuditLog(logTableData);
			query = "SELECT COUNT(*) AS number_of_rows FROM " + tableName;
			resultTable = Table.tableNew();
			DBaseTable.execISql(resultTable, query);
			remainingRows = resultTable.getInt("number_of_rows", 1);

			eMailBody = "Successfully processed " + tableName + ".<br>";
			eMailBody = eMailBody + "Retain date=" + retainDateODateTime + ".<br>";
			eMailBody = eMailBody + "Rows removed=" + deletedData.getNumRows() + ".<br>";
			eMailBody = eMailBody + "Remaining rows=" + remainingRows + ".<br>";
			eMailBody = eMailBody + "Time elapsed=" + elapsedTimeInSeconds + " seconds.<br>";
		}
		catch (OException oException)
		{
			eMailBody = "Unable to process " + argumentTableForStoredProcedure.getString(ArchiveProcedureArgumentsTableColumns.TABLE_NAME_ARGUMENT_TABLE_COLUMN.toString(), 1);
			eMailBody = eMailBody + ".Exception details:" + oException.getMessage();
			PluginLog.error(oException.getMessage());
		}
		finally
		{
			if (Table.isTableValid(argumentTableForStoredProcedure) == 1)
			{
				argumentTableForStoredProcedure.destroy();
			}
			if (Table.isTableValid(resultTable) == 1)
			{
				resultTable.destroy();
			}
		}

		createReport(row, eMailBody, deletedData);
	}

	/**
	 * 
	 * Prepares data for e-mail
	 * 
	 * @param row
	 * @param eMailBody
	 * @param deletedData
	 * @throws OException
	 */
	private void createReport(int row, String eMailBody, Table deletedData) throws OException
	{
		String emailRecipientsForTable;
		String filePath = createCSVFile(deletedData);
		try
		{

			emailRecipientsForTable = configurationTable.getString("email_recipients", row);

			subject = getEmailSubject();

			eMailBody = eMailBody + "File path:<a href=\"" + filePath + "\">" + filePath + "</a>";

			if (deletedData.getNumRows() != 0)
			{
				if (isEmpty(emailRecipientsForTable) || emailRecipientsForTable.equals(" "))
				{
					PluginLog.info("email_recipients is null or empty in " + getConfigurationUserTableName() + " in row " + row);
					emailBodyList.add( eMailBody );
				}
				else
				{
					sendReport( emailRecipientsForTable, eMailBody );
				}
			}
		}
		catch (OException oException)
		{
			PluginLog.error("Exception occrred while preparing e-mail." + oException.getMessage());
			throw oException;
		}
		finally
		{
			if (Table.isTableValid(deletedData) == 1)
			{
				deletedData.destroy();
			}
		}
	}

	/**
	 * @param startSeconds
	 * @param argumentTableForStoredProcedure
	 * @param deletedData
	 * @param logTableData
	 * @throws OException
	 */
	private LogTableData getLogTableData(int startSeconds, Table argumentTableForStoredProcedure, Table deletedData, int elapsedTimeInSeconds) throws OException
	{
		LogTableData logTableData = new LogTableData();
		String tableName;
		int retainDays;
		int rowsRemoved;
		String timeComprisonColumn;
		ODateTime serverTime = ODateTime.getServerCurrentDateTime();
		PluginLog.info("Started filling LogTableData");

		tableName = argumentTableForStoredProcedure.getString("table_name", 1);
		retainDays = argumentTableForStoredProcedure.getInt("retain_days", 1);
		timeComprisonColumn = argumentTableForStoredProcedure.getString("time_comparison_column", 1);
		rowsRemoved = deletedData.getNumRows();

		PluginLog.debug("Values to be filled in log table:");
		PluginLog.debug("Table name=" + tableName);
		PluginLog.debug("Retain days=" + retainDays);
		PluginLog.debug("Time comparison column=" + timeComprisonColumn);
		PluginLog.debug("Run date=" + serverTime);
		PluginLog.debug("Rows removed=" + rowsRemoved);
		PluginLog.debug("Elapsed time(In seconds)=" + elapsedTimeInSeconds);

		logTableData.setTableName(tableName);
		logTableData.setRetainDays(retainDays);
		logTableData.setTimeComparisonColumn(timeComprisonColumn);
		logTableData.setRunDate(serverTime);
		logTableData.setRowsRemoved(rowsRemoved);
		logTableData.setElapsedTimeInSeconds(elapsedTimeInSeconds);

		PluginLog.info("Completed filling LogTableData");
		return logTableData;
	}

	@Override
	public void fillConfigurationData() throws OException
	{
		int returnValue;
		String message;
		String configurationTableName;

		PluginLog.info("Started filling configuration data");

		configurationTableName = getConfigurationUserTableName();

		PluginLog.debug("Configuration table name=" + configurationTableName + ".");

		configurationTable = Table.tableNew(configurationTableName);

		/*
		 * Get configuration from USER_ARCHIVE_CONFIG for archiving.
		 */
		returnValue = DBUserTable.load(configurationTable);
		if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			message = DBUserTable.dbRetrieveErrorInfo(returnValue, "DBUserTable.load() failed");
			PluginLog.error(message);
			configurationTable.destroy();
			PluginLog.debug("Exiting with failure status.");
			Util.exitFail(message);
		}
		PluginLog.info("Completed filling configuration data");
	}

	@Override
	public boolean isEmpty(String argData)
	{
		boolean flag = false;
		if (null == argData || argData.isEmpty())
		{
			flag = true;
		}
		return flag;
	}

	@Override
	public void insertAuditLog(LogTableData argLogTableData) throws OException
	{
		Table tableToInsert = null;
		ODateTime runDate = null;
		try
		{
			PluginLog.info("Started inserting audit records in log table.");
			if (argLogTableData.getRowsRemoved() != 0)
			{
				int returnValue;
				String logTableName;

				tableToInsert = Table.tableNew();

				logTableName = getLogUserTableName();

				runDate = argLogTableData.getRunDate();

				tableToInsert.setTableName(logTableName);
				returnValue = DBUserTable.structure(tableToInsert);

				if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new ArchivePurgeUtilitiesRuntimeException("Unable to get structure of: " + logTableName);
				}

				tableToInsert.addRow();

				tableToInsert.setString("tablename", 1, argLogTableData.getTableName());
				tableToInsert.setInt("retain_days", 1, argLogTableData.getRetainDays());
				tableToInsert.setString("time_comparison_column", 1, argLogTableData.getTimeComparisonColumn());
				tableToInsert.setDateTime("rundate", 1, runDate);
				tableToInsert.setInt("rows_removed", 1, argLogTableData.getRowsRemoved());
				tableToInsert.setInt("elapsed_time_in_seconds", 1, argLogTableData.getElapsedTimeInSeconds());

				returnValue = DBUserTable.insert(tableToInsert);
				if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(returnValue, "DBUserTable.insert() failed"));
				}
				PluginLog.info("Completed inserting audit records in log table.Log table name=" + logTableName + ".Table name=" + argLogTableData.getTableName() + ".Run date=" + argLogTableData.getRunDate() + ".Rows removed="
						+ argLogTableData.getRowsRemoved() + ".Elapsed time(In seconds)=" + argLogTableData.getElapsedTimeInSeconds() + ".");
			}
			else
			{
				PluginLog.info("Nothing is removed from " + argLogTableData.getTableName() + ".");
			}
		}
		catch (OException oException)
		{
			String message = "Exception occurred while updating archive purge log table." + oException.getMessage();
			PluginLog.error(message);
			throw new ArchivePurgeUtilitiesRuntimeException(message, oException);
		}
		finally
		{
			if (Table.isTableValid(tableToInsert) == 1)
			{
				tableToInsert.destroy();
			}
			if (ODateTime.isNull(runDate) == 0)
			{
				runDate.destroy();
			}
		}
	}

	@Override
	public void process(Table argumentTableForStoredProcedure) throws OException
	{
		int returnValue;
		String message;
		String tableBeingProcessed = argumentTableForStoredProcedure.getString("table_name", 1);

		PluginLog.info("Started processing data in database. Processing table:" + tableBeingProcessed);
		
		Table tblTableToPurge = Table.tableNew();
		tblTableToPurge.setTableName(tableBeingProcessed);
		DBUserTable.structure(tblTableToPurge);
		
		String timeComparisonColumn = argumentTableForStoredProcedure.getString("time_comparison_column", 1);
		int intColType = tblTableToPurge.getColType(timeComparisonColumn); 
		
		tblTableToPurge.destroy();
		
		
		if(intColType == COL_TYPE_ENUM.COL_DATE_TIME.jvsValue()){

			PluginLog.debug("Running procedure " + getStoredProcedureName());
			returnValue = DBase.runProc(getStoredProcedureName(), argumentTableForStoredProcedure);
			PluginLog.debug("Value returned by procedure=" + returnValue);
			if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 		{
				PluginLog.debug("Value returned by procedure=" + returnValue);
				if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					message = "DBase.runProcFillTable() failed. For arguments:" + argumentTableForStoredProcedure.toString();
					PluginLog.error(message);
					throw new OException("Unable to process " + tableBeingProcessed);
				}
			}
		} else if(intColType == COL_TYPE_ENUM.COL_INT.jvsValue()){

			int retainDays = argumentTableForStoredProcedure.getInt(RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString(), 1);
			int purgeFromDate = OCalendar.today() - retainDays;
			argumentTableForStoredProcedure.setInt(RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString(), 1, purgeFromDate);
			
			argumentTableForStoredProcedure.setColName("retain_days", "purge_from");
			
			try{
				
				PluginLog.debug("Running procedure " + getStoredProcedureName()  + "_int");
				returnValue = DBase.runProc(getStoredProcedureName() + "_int", argumentTableForStoredProcedure);
				PluginLog.debug("Value returned by procedure=" + returnValue);
				if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 		{
					PluginLog.debug("Value returned by procedure=" + returnValue);					
					message = "DBase.runProcFillTable() failed. For arguments:" + argumentTableForStoredProcedure.toString();
					PluginLog.error(message);
					throw new OException("Unable to process " + tableBeingProcessed);
					
				}
				
			}catch(Exception e){
				
				message = "DBase.runProcFillTable() failed. For arguments:" + argumentTableForStoredProcedure.toString();
				PluginLog.error(message);
				throw new OException("Unable to process " + tableBeingProcessed);
				
			}
			
		} else {
			message = "Unrecognised Column Table:" + tableBeingProcessed + " Column:"  + timeComparisonColumn;
			PluginLog.debug(message);
		}
		
		PluginLog.info("Completed processing data in database. Processing table:" + tableBeingProcessed);

	}

	@Override
	public void sendReport( String argEmailReceipients, String emailBody )
	{
		try
		{
			String message;
			
			int returnValue = com.jm.archivepurgeutilities.util.Util.sendEmail( argEmailReceipients, subject, emailBody );
			if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				message = "Failure when sending email.";
				PluginLog.error(message);
				com.olf.openjvs.Util.exitFail(message);
			}
		}
		catch (OException oException)
		{
			String message = "Exception occurred while updating archive purge log table." + oException.getMessage();
			PluginLog.error(message);
			throw new ArchivePurgeUtilitiesRuntimeException(message, oException);
		}
	}

	/**
	 * 
	 * Gets data eligible for deletion from user table
	 * 
	 * @param argumentTableForStoredProcedure
	 * @return
	 * @throws OException
	 */
	private Table getDataToBeDeleted(Table argumentTableForStoredProcedure) throws OException
	{
		String tableName = argumentTableForStoredProcedure.getString("table_name", 1);
		String timeComparisonColumn = argumentTableForStoredProcedure.getString("time_comparison_column", 1);
		int retainDays = argumentTableForStoredProcedure.getInt("retain_days", 1);
		int dateFormatInt = 103;
		Table dataToBeDeletedTable = Table.tableNew();
		int businessDateAsInt = Util.getBusinessDate();
		retainDateODateTime = getRetainDate(businessDateAsInt, retainDays);
		String query = "SELECT * FROM " 
				+ tableName 
				+ " WHERE " 
				+ timeComparisonColumn 
				+ "<CONVERT(datetime," 
				+ "\'" 
				+ retainDateODateTime 
				+ "\'," + dateFormatInt + ") AND " 
				+ timeComparisonColumn + " <> \'\'";
		PluginLog.debug("query=" + query);
		PluginLog.debug("tableName=" + tableName);
		dataToBeDeletedTable.setTableTitle(tableName);
		int returnValue = DBaseTable.execISql(dataToBeDeletedTable, query);
		if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new ArchivePurgeUtilitiesRuntimeException("Exception while reading data to be deleted");
		}
		return dataToBeDeletedTable;
	}

	/**
	 * 
	 * Gets retain data in ODateTime format
	 * 
	 * @param businessDateAsInt
	 * @param retainDays
	 * @return
	 * @throws OException
	 */
	private ODateTime getRetainDate(int businessDateAsInt, int retainDays) throws OException
	{
		ODateTime retainDate = ODateTime.dtNew();
		int retainDateAsInt = businessDateAsInt - retainDays;
		String dateAsString = OCalendar.formatDateInt(retainDateAsInt);
		PluginLog.debug("dateAsString=" + dateAsString);
		retainDate = ODateTime.strToDateTime(dateAsString);
		PluginLog.debug("Retain date=" + retainDate);
		return retainDate;
	}
	
	private void sendReportsToCommonReceipients()
	{
		String consolidatedEmailBody = "";
		if (!emailReceipients.isEmpty() && !emailBodyList.isEmpty())
		{
			for (String emailBody : emailBodyList)
			{
				consolidatedEmailBody = consolidatedEmailBody + emailBody+"<br><br><br>";
			}
			sendReport(emailReceipients, consolidatedEmailBody);
		}
	}
}
