package com.jm.data;

import com.olf.openjvs.ODateTime;

/**
 * This class contains data to update in archive purge log table
 * @author SharmV03
 *
 */
public class LogTableData
{
	private String tableName;
	private int retainDays;
	private String timeComparisonColumn;
	private ODateTime runDate;
	private int rowsRemoved;
	private int elapsedTimeInSeconds;
	/**
	 * @return the tableName
	 */
	public String getTableName()
	{
		return tableName;
	}
	/**
	 * @param argTableName the tableName to set
	 */
	public void setTableName(String argTableName)
	{
		this.tableName = argTableName;
	}

	/**
	 * @return the retainDays
	 */
	public int getRetainDays()
	{
		return retainDays;
	}
	/**
	 * @param argRetainDays the retainDays to set
	 */
	public void setRetainDays(int argRetainDays)
	{
		this.retainDays = argRetainDays;
	}
	/**
	 * @return the timeComparisionColumn
	 */
	public String getTimeComparisonColumn()
	{
		return timeComparisonColumn;
	}
	/**
	 * @param argTimeComparisonColumn the timeComparisionColumn to set
	 */
	public void setTimeComparisonColumn(String argTimeComparisonColumn)
	{
		this.timeComparisonColumn = argTimeComparisonColumn;
	}
	/**
	 * @return the runDate
	 */
	public ODateTime getRunDate()
	{
		return runDate;
	}
	/**
	 * @param argRunDate the runDate to set
	 */
	public void setRunDate(ODateTime argRunDate)
	{
		this.runDate = argRunDate;
	}
	/**
	 * @return the rowsRemoved
	 */
	public int getRowsRemoved()
	{
		return rowsRemoved;
	}
	/**
	 * @param argRowsRemoved the rowsRemoved to set
	 */
	public void setRowsRemoved(int argRowsRemoved)
	{
		this.rowsRemoved = argRowsRemoved;
	}
	/**
	 * @return the elapsedTimeInSeconds
	 */
	public int getElapsedTimeInSeconds()
	{
		return elapsedTimeInSeconds;
	}
	/**
	 * @param argElapsedTimeInSeconds the elapsedTimeInSeconds to set
	 */
	public void setElapsedTimeInSeconds(int argElapsedTimeInSeconds)
	{
		this.elapsedTimeInSeconds = argElapsedTimeInSeconds;
	}
	
	
}
