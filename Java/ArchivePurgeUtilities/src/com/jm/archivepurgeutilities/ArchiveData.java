/**
 * 
 */
package com.jm.archivepurgeutilities;

import com.jm.archivepurgeutilities.util.Constants;
import com.jm.enums.ArchiveProcedureArgumentsTableColumns;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * A utility to archive data in Endur database as per user configuration.
 * This utility archives data as per configuration in USER_archive_config. To update this table a reference CSV file is available in "CSV files for updating user tables" folder. 
 * User can refer this file to update USER_archive_config table using Endur utility to update user tables. 
 * After retain_days time is passed data will be archived to archive table provided in configuration and e-mail will be sent with archived data in CSV format, to ids configured in email_recipients column.
 * 
 * ArchivData class responsible for 
 * 1. Deleting data from source table as per configuration in USER_archive_config 
 * 2. Moving data to archive table as per configuration in USER_archive_config 
 * 3. Putting deleted data in a CSV file and e-mail CSV file with status to recipients as per configuration in USER_archive_config
 * 
 * @author SharmV03
 * 
 */
public class ArchiveData extends ArchivePurgeUtilitiesScript
{
	@Override
	public Table generateStoredProcedureInputArgs(int row) throws OException
	{
		boolean isDataValid = true;

		int retainDays;

		String tableName;
		String archiveTable;
		String timeComparisonColumn;
		final String USER_ARCHIVE_CONFIG = getConfigurationUserTableName();
		Table argumentTableForStoredProcedure =	null;
		
		PluginLog.info("Started populating argument table for USER_sp_archive_data procedure.");

		tableName = configurationTable.getString("tablename", row);
		if(isEmpty(tableName))
		{
			PluginLog.error("tablename is null or empty in " + USER_ARCHIVE_CONFIG + " in row " + row);
			isDataValid = false;
		}

		archiveTable = configurationTable.getString("archive_table", row);
		if(isEmpty(archiveTable))
		{
			PluginLog.error("archive_table is null or empty in " + USER_ARCHIVE_CONFIG + " in row " + row);
			isDataValid = false;
		}

		retainDays = configurationTable.getInt("retain_days", row);
		if (retainDays <= 0)
		{
			PluginLog.error("retain_days is 0 or negative in " + USER_ARCHIVE_CONFIG + " in row " + row);
			isDataValid = false;
		}

		timeComparisonColumn = configurationTable.getString("time_comparison_column", row);
		if(isEmpty(timeComparisonColumn))
		{
			PluginLog.info("time_comparison_column is null or empty in " + USER_ARCHIVE_CONFIG + " in row " + row);
		}

		/* If configuration data is valid then start archiving */
		if (isDataValid)
		{

			argumentTableForStoredProcedure = setupStoredProcedureArgsTableStructure();
			argumentTableForStoredProcedure.clearDataRows();
			argumentTableForStoredProcedure.addRow();

			argumentTableForStoredProcedure.setString(ArchiveProcedureArgumentsTableColumns.TABLE_NAME_ARGUMENT_TABLE_COLUMN.toString(), 1, tableName);
			argumentTableForStoredProcedure.setString(ArchiveProcedureArgumentsTableColumns.ARCHIVE_TABLE_ARGUMENT_TABLE_COLUMN.toString(), 1, archiveTable);
			argumentTableForStoredProcedure.setInt(ArchiveProcedureArgumentsTableColumns.RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString(), 1, retainDays);
			argumentTableForStoredProcedure.setString(ArchiveProcedureArgumentsTableColumns.TIME_COMPARISON_COLUMN.toString(), 1, timeComparisonColumn);
			PluginLog.debug("USER_sp_archive_data arguments values:");
			PluginLog.debug(ArchiveProcedureArgumentsTableColumns.TABLE_NAME_ARGUMENT_TABLE_COLUMN.toString()+"="+tableName);
			PluginLog.debug(ArchiveProcedureArgumentsTableColumns.ARCHIVE_TABLE_ARGUMENT_TABLE_COLUMN.toString()+"="+ archiveTable);
			PluginLog.debug(ArchiveProcedureArgumentsTableColumns.RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString()+"="+retainDays);
			PluginLog.debug(ArchiveProcedureArgumentsTableColumns.TIME_COMPARISON_COLUMN.toString()+"="+ timeComparisonColumn);
		}
		
		PluginLog.info("Completed populating argument table for USER_sp_archive_data procedure.");
		
		return argumentTableForStoredProcedure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.customer.archivepurgeutilities.ArchivePurgeUtilitiesScript#
	 * addColumnsInArgumentTableForStoredProcedure(com.olf.openjvs.Table)
	 */
	@Override
	public Table setupStoredProcedureArgsTableStructure() throws OException
	{
		Table argumentTableForStoredProcedure = Table.tableNew();
		argumentTableForStoredProcedure.addCol(ArchiveProcedureArgumentsTableColumns.TABLE_NAME_ARGUMENT_TABLE_COLUMN.toString(), COL_TYPE_ENUM.COL_STRING);
		argumentTableForStoredProcedure.addCol(ArchiveProcedureArgumentsTableColumns.ARCHIVE_TABLE_ARGUMENT_TABLE_COLUMN.toString(), COL_TYPE_ENUM.COL_STRING);
		argumentTableForStoredProcedure.addCol(ArchiveProcedureArgumentsTableColumns.RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString(), COL_TYPE_ENUM.COL_INT);
		argumentTableForStoredProcedure.addCol(ArchiveProcedureArgumentsTableColumns.TIME_COMPARISON_COLUMN.toString(), COL_TYPE_ENUM.COL_STRING);
		return argumentTableForStoredProcedure;
	}

	@Override
	public String getConfigurationUserTableName()
	{
		return "USER_archive_config";
	}

	@Override
	public String getStoredProcedureName()
	{
		return Constants.ARCHIVE_DATA_STORED_PROCEDURE;
	}

	@Override
	public String getLogUserTableName()
	{
		return Constants.ARCHIVE_LOG_USER_TABLE;
	}
	
	@Override
	public String createCSVFile(Table argDeletedData) throws OException
	{
		final String PAYLOAD = "payload"; 
		if(argDeletedData.getColNum(PAYLOAD) >= 1)
		{
			argDeletedData.delCol(PAYLOAD);
		}
		
		String filePath = com.jm.archivepurgeutilities.util.Util.exportDataToFile(argDeletedData, "ArchiveData");
		return filePath;
	}

	@Override
	public String getEmailSubject()
	{
		return "Archive Status";
	}
}
