package com.jm.archivepurgeutilities;

import com.jm.archivepurgeutilities.util.Constants;
import com.jm.enums.PurgeProcedureArgumentsTableColumns;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * 	
 * A utility to archive and purge data in Endur database as per user configuration.
 * This utility purges data as per configuration in USER_purge_config. To update this table a reference CSV file is available in "CSV files for updating user tables" folder.
 *  User can refer this file to update USER_purge_config table using Endur utility to update user tables. 
 *  After retain_days time is passed data will be deleted and e-mail will be sent with deleted data in CSV format, to ids configured in email_recipients column.
 * 
 * PurgeData class responsible for 
 * 1. Deleting data from source table as per configuration in USER_purge_config 
 * 2. Moving data to purge table as per configuration in USER_purge_config 
 * 3. Putting deleted data in a CSV file and e-mail CSV file with status to recipients as per configuration in USER_purge_config
 * 
 * @author SharmV03
 * 
 */
public class PurgeData extends ArchivePurgeUtilitiesScript
{
	@Override
	public Table generateStoredProcedureInputArgs(int row) throws OException
	{
		boolean isDataValid = true;

		int retainDays;

		String tableName;
		String timeComparisionColumn;
		final String USER_PURGE_CONFIG = "USER_purge_config";
		Table argumentTableForStoredProcedure =	null;
		
		PluginLog.info("Started generating stored procedure arguemnts");
		isDataValid = true;

		tableName = configurationTable.getString("tablename", row);
		if(isEmpty(tableName))
		{
			PluginLog.error("tablename is null or empty in " + USER_PURGE_CONFIG + " in row " + row);
			isDataValid = false;
		}

		retainDays = configurationTable.getInt("retain_days", row);
		if (retainDays <= 0)
		{
			PluginLog.error("retain_days is 0 or negative in " + USER_PURGE_CONFIG + " in row " + row);
			isDataValid = false;
		}

		timeComparisionColumn = configurationTable.getString("time_comparison_column", row);
		if(isEmpty(timeComparisionColumn))
		{
			PluginLog.info("time_comparison_column is null or empty in " + USER_PURGE_CONFIG + " in row " + row);
		}

		/* If configuration data is valid then start archiving */
		if (isDataValid)
		{

			argumentTableForStoredProcedure = setupStoredProcedureArgsTableStructure();
			argumentTableForStoredProcedure.clearDataRows();
			argumentTableForStoredProcedure.addRow();

			argumentTableForStoredProcedure.setString(PurgeProcedureArgumentsTableColumns.TABLE_NAME_ARGUEMNT_TABLE_COLUMN.toString(), 1, tableName);
			argumentTableForStoredProcedure.setInt(PurgeProcedureArgumentsTableColumns.RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString(), 1, retainDays);
			argumentTableForStoredProcedure.setString(PurgeProcedureArgumentsTableColumns.TIME_COMPARISON_COLUMN.toString(), 1, timeComparisionColumn);
		}
		PluginLog.info("Completed generating stored procedure arguemnts");
		return argumentTableForStoredProcedure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.customer.purgepurgeutilities.PurgePurgeUtilitiesScript#
	 * addColumnsInArgumentTableForStoredProcedure(com.olf.openjvs.Table)
	 */
	@Override
	public Table setupStoredProcedureArgsTableStructure() throws OException
	{
		Table argumentTableForStoredProcedure = Table.tableNew();
		PluginLog.info("Started setting up stored procedure arguments");
		argumentTableForStoredProcedure.addCol(PurgeProcedureArgumentsTableColumns.TABLE_NAME_ARGUEMNT_TABLE_COLUMN.toString(), COL_TYPE_ENUM.COL_STRING);
		argumentTableForStoredProcedure.addCol(PurgeProcedureArgumentsTableColumns.RETAIN_DAYS_ARGUMENT_TABLE_COLUMN.toString(), COL_TYPE_ENUM.COL_INT);
		argumentTableForStoredProcedure.addCol(PurgeProcedureArgumentsTableColumns.TIME_COMPARISON_COLUMN.toString(), COL_TYPE_ENUM.COL_STRING);
		PluginLog.info("Completed setting up stored procedure arguments");
		return argumentTableForStoredProcedure;
	}

	@Override
	public String getConfigurationUserTableName()
	{
		return "USER_purge_config";
	}

	@Override
	public String getStoredProcedureName()
	{
		return Constants.PURGE_DATA_STORED_PROCEDURE;
	}

	@Override
	public String getLogUserTableName()
	{
		return Constants.PURGE_LOG_USER_TABLE;
	}
	
	@Override
	public String createCSVFile(Table argDeletedData) throws OException
	{
		final String PAYLOAD = "payload"; 
		if(argDeletedData.getColNum(PAYLOAD) >= 1)
		{
			argDeletedData.delCol(PAYLOAD);
		}
		String filePath = com.jm.archivepurgeutilities.util.Util.exportDataToFile(argDeletedData, "PurgeData");
		return filePath;
	}

	@Override
	public String getEmailSubject()
	{
		return "Purge Status";
	}
}
