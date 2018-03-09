/**
 * 
 */
package com.jm.enums;

/**
 * This enum contains arguments for purging procedure
 * @author SharmV03
 * 
 */
public enum PurgeProcedureArgumentsTableColumns
{
	TABLE_NAME_ARGUEMNT_TABLE_COLUMN("table_name"),
	RETAIN_DAYS_ARGUMENT_TABLE_COLUMN("retain_days"),
	TIME_COMPARISON_COLUMN("time_comparison_column");

	private final String value;

	private PurgeProcedureArgumentsTableColumns(String argValue)
	{
		this.value = argValue;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
