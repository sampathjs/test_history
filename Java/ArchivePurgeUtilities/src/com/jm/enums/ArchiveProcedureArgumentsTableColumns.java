/**
 * 
 */
package com.jm.enums;

/**
 * This enum contains arguments for archiving procedure
 * @author SharmV03
 * 
 */
public enum ArchiveProcedureArgumentsTableColumns
{
	TABLE_NAME_ARGUMENT_TABLE_COLUMN("table_name"),
	ARCHIVE_TABLE_ARGUMENT_TABLE_COLUMN("archive_table"),
	RETAIN_DAYS_ARGUMENT_TABLE_COLUMN("retain_days"),
	TIME_COMPARISON_COLUMN("time_comparison_column");

	private final String value;

	private ArchiveProcedureArgumentsTableColumns(String argValue)
	{
		this.value = argValue;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
