package com.olf.migration.pricing_fixing.app;

import java.util.Date;

import com.olf.migration.pricing_fixing.model.GroupingCriteria;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.jm.logging.Logging;

public abstract class Migr_PriceFixing_Common {
	public abstract String getLogLevel();
	
	public abstract String getLogFile();
	
	public abstract String getEndDateString();
	
	public abstract String getQueryName();
	
	public abstract GroupingCriteria getGrouping();
	
	public abstract int getClusterSize();
	
	public abstract boolean getUnfixedOnly();
	
	protected abstract String [] getGroupingCols(); 	
	
	protected abstract String getSql() throws OException;

	
	
	
	protected int endDate;
	
	protected int savedQueryId;
	
	private int[] currentGroupingConfig=null;

	private int[] maxGroups = null;
	private int[] minGroups = null;
	
	private Date startOfRun = null;
	
	protected void init () throws OException {
		Util.utilRefreshCache("SESSION", "ENTIRE_CACHE");
		initLogging(getLogFile(), getLogLevel());
		endDate = parseEndDate(getEndDateString());
		savedQueryId = getSavedQueryId(getQueryName());
		currentGroupingConfig=null;
		maxGroups = null;
		minGroups = null;
		startOfRun = new Date();
	}
	
	protected Table getNextGroup (Table sqlResult) throws OException {
		String [] groupingCols = getGroupingCols();
		if (maxGroups == null) {
			maxGroups = new int[groupingCols.length];
			for (int i = 0; i < maxGroups.length; i++) {
				int max = getMaxForCol (sqlResult, groupingCols[i]);
				maxGroups[i] = max;
			}
		}
		if (minGroups == null) {
			minGroups = new int[groupingCols.length];
			for (int i = 0; i < minGroups.length; i++) {
				int min = getMinForCol (sqlResult, groupingCols[i]);
				minGroups[i] = min;
			}
		}
		if (currentGroupingConfig == null) {
			currentGroupingConfig = new int[groupingCols.length];
			for (int i=0; i < currentGroupingConfig.length; i++) {
				currentGroupingConfig[i] = minGroups[i];
			}
		} else {
			boolean changed = false;
			for (int i=0; i < groupingCols.length; i++) {
				if (currentGroupingConfig[i] < maxGroups[i]) {
					currentGroupingConfig[i] = currentGroupingConfig[i]+1;
					changed = true;
					break;
				} else {
					currentGroupingConfig[i] = minGroups[i];
				}
			}
			if (!changed) {
				return null;
			}			
		}
		Table group = Table.tableNew("next group");
		StringBuilder where = new StringBuilder ("");
		boolean first = true;
		for (int i=0; i < groupingCols.length; i++) {
			if (!first) {
				where.append(" AND ");
			}
			where.append(groupingCols[i]).append(" EQ ").append(currentGroupingConfig[i]);
			first = false;
		}
		group.select(sqlResult, "*", where.toString());
		return group;
	}

	/**
	 * Expected columns in sqlResult:
	 * reset_date (int), internal_portfolio(int), tran_num(int), <groupingCol>(int)
	 * After execution the grouping columns contains the data clustered by requested grouping criteria.
	 * @param sqlResult
	 * @throws OException
	 */
	protected void applyGroupingToSqlResult(Table sqlResult, String groupingCol) throws OException {
		switch (getGrouping()) {
		case EOM:
			OCalendar.colConvertJdToEOM(sqlResult, "reset_date", groupingCol);
			break;
		case EOY:
			OCalendar.colConvertJdToEOY(sqlResult, "reset_date", groupingCol);
			break;
		case PORTFOLIO:
			sqlResult.copyCol("internal_portfolio", sqlResult, groupingCol);
			break;
		case CLUSTER_SIZE:
			applyClusterSizeGrouping(sqlResult, groupingCol);
			break;
		case NONE:
			break;
		}
	}

	protected String getFileNameForGroup () {
		StringBuilder sb = new StringBuilder("Migr_PriceFixing_");
		sb.append(startOfRun.getTime());
		String [] groupingCols = getGroupingCols();
		for (int i=0; i < groupingCols.length; i++) {
			sb.append("_").append(currentGroupingConfig[i]);
		}
		sb.append(".csv");
		return sb.toString();
	}

	private void applyClusterSizeGrouping(Table sqlResult, String groupingCol)
			throws OException {
		sqlResult.group("tran_num");
		int counter = 0;
		int groupCounter = 0;
		int lastTranNum = -1;
		for (int row=sqlResult.getNumRows(); row >= 1; row--) {
			int tranNum = sqlResult.getInt ("tran_num", row);
			if (counter >= getClusterSize() && tranNum != lastTranNum) {
				groupCounter++;
				counter = 0;
			}
			sqlResult.setInt(groupingCol, row, groupCounter);
			lastTranNum = tranNum;
			counter++;
		}
	}
	
	private void initLogging(String logFile, String logLevel) throws OException {
		try {
			String logDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			Logging.init(this.getClass(), "","");

		} catch (Exception e) {
			throw new OException("Can't init PluginLog");
		}		
	}
	
	private int parseEndDate (String endDate) throws OException {
		try {
			return OCalendar.parseString(endDate);
		} catch (OException ex) {
			Logging.error("Error parsing endDate '" + endDate + "'");
			throw ex;
		}	
	}
	
	private int getSavedQueryId (String savedQueryName) throws OException {
		try {
			return Query.run(savedQueryName);
		} catch (OException ex) {
			Logging.error("Error executing saved query '" + savedQueryName + "'");
			throw ex;
		}   			
	}

	private int getMaxForCol(Table sqlResult, String groupingCol) throws OException {
		int max=0;
		for (int row=sqlResult.getNumRows(); row >= 1; row--) {
			int value = sqlResult.getInt(groupingCol, row);
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	private int getMinForCol(Table sqlResult, String groupingCol) throws OException {
		int min=Integer.MAX_VALUE;
		for (int row=sqlResult.getNumRows(); row >= 1; row--) {
			int value = sqlResult.getInt(groupingCol, row);
			if (value < min) {
				min = value;
			}
		}
		return min;
	}

}
