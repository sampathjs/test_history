package com.olf.migration.pricing_fixing.app;

import com.olf.migration.pricing_fixing.model.GroupingCriteria;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EndOfDay;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-05-23	V1.0	jwaechter	- Initial Version
 */

/**
 * This plugin fixes all deals provided by a named query to a default price up to a certain date.
 */
public class Migr_PriceFixingDefault extends Migr_PriceFixing_Common implements IScript
{	
	/**
	 * Name of a saved transaction query to retrieve the transaction to be reset to.
	 */
	public static final String SAVED_QUERY_NAME= "Migr_PriceFixing";
	
	/**
	 * Default value being applied as reset
	 */
	public static final double DEFAULT_VALUE = 1.234d;
	
	/**
	 * The last day the default value is applied to. Format has to be parseable by standard OLF date parser.
	 */
	public static final String END_DATE = "13/05/2016";

	public static final String LOG_LEVEL= "info";
	
	public static final String LOG_FILE = "Migr_PricingFixingDefault.log";
		
	/**
	 * Possible values (see {@link GroupingCriteria}):
	 * NONE, EOY, EOM, PORTFOLIO, CLUSTER_SIZE
	 */
	public static GroupingCriteria GROUPING = GroupingCriteria.EOY;
	
	/**
	 * Size of each cluster if GROUPING == GroupingCriteria.CLUSTER_SIZE. Note that each transaction is finished completely
	 * before a new cluster starts.
	 */
	public static final int CLUSTER_SIZE = 200;
	
	/**
	 * Show the result table for each group to the user?
	 */
	public static boolean VIEW_TABLE=true;

	/**
	 * Should the result table be saved as a CSV file to %AB_OUTDIR%\Migr_PriceFising_<timestamp>_<groups>.csv?
	 */
	public static boolean SAVE_CSV_REPORT=true;
	
	
	/**
	 * Fix unfixed resets only or all.
	 */
	public static boolean UNFIXED_ONLY=true;

	
	private static final String GROUP_COLS[] = { "grouping"};
	
	public void execute(IContainerContext context) throws OException
    {
    	savedQueryId = -1;
    	try {
        	init (); 
        	process();
    	} catch (Throwable ex) {
    		PluginLog.error(ex.toString());
    		for (StackTraceElement ste : ex.getStackTrace()) {
    			PluginLog.error(ste.toString());
    		}
    		throw ex;
    	} finally {
    		if (savedQueryId != -1) {
    			Query.clear(savedQueryId);
    		}
    	}
    }

	private void process() throws OException {
		int today = OCalendar.today();
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");

		String sql = getSql();
		Table sqlResult 		 = null;
		Table distinctResetDates = null;
		Table tran				 = null;
		Table resetList			 = null;
		Table fixings            = null;
		
		try {
			sqlResult = Table.tableNew("reset date query result");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error Executing SQL " + sql + "\n");
				throw new OException (message);
			}
			if (sqlResult.getNumRows() == 0) {
				throw new OException ("Could not retrieve any reset dates using SQL " + sql);
			}
			tran = Table.tableNew("Transactions for reset date");
			fixings = EndOfDay.createResetTable();
			sqlResult.colConvertDateTimeToInt("reset_date");
			sqlResult.colConvertDateTimeToInt("start_date");
			sqlResult.colConvertDateTimeToInt("date");
			sqlResult.addCol("grouping", COL_TYPE_ENUM.COL_INT);
			applyGroupingToSqlResult(sqlResult, "grouping");
			
			Table group = getNextGroup(sqlResult);
			distinctResetDates = Table.tableNew("Distinct reset dates");
			while (group != null) {
				distinctResetDates.clearDataRows();
				distinctResetDates.select(group, "DISTINCT, reset_date", "tran_num GT 0");
				distinctResetDates.group("reset_date");
				int rowCount = distinctResetDates.getNumRows();
				resetList = null;			
				
				for (int row=1; row <= rowCount; row++) {
					int resetDate = distinctResetDates.getInt("reset_date", row);
					tran.select(group, "DISTINCT, tran_num", "reset_date EQ " + resetDate);
					if (tran.getNumRows() > 0) {
						ret = Util.setCurrentDate(resetDate);
						fixings.select(group, "DISTINCT, index, start_date, date, source, yield_basis, tenor, projector_id, index_location", "tran_num GT 0");
						fixings.setColValDouble("rate", DEFAULT_VALUE);
						Table result = EndOfDay.resetDealsByTranListTable(tran, fixings, resetDate);
						if (result.getNumRows() > 0) {
							if (resetList == null) {
								resetList = result.copyTable();
							} else {
		 						result.copyRowAddAll(resetList);	
		 						resetList.group("ins_num, tran_num, deal_num, param_seq_num, prh_seq_num, reset_seq_num, profile_seq_num, value, spot_value, info, success, message");
		 						resetList.distinctRows();
							}
						}
						result.destroy();
					}
					tran.clearDataRows();
					fixings.clearDataRows();
				}
				if (resetList.getNumRows() > 0) {
					if (VIEW_TABLE) {
						resetList.viewTable();						
					}
					if (SAVE_CSV_REPORT) {
						resetList.printTableDumpToFile(abOutdir + "\\" + getFileNameForGroup());						
					}
					resetList.clearDataRows();
				}
				group.destroy();
				group = getNextGroup(sqlResult);
			}
		} finally {
			Util.setCurrentDate(today);			
			if (sqlResult != null) {
				sqlResult.destroy();
			}
			if (distinctResetDates != null) {
				distinctResetDates.destroy();
			}
			if (tran != null) {
				tran.destroy();
			}
			if (resetList != null) {
				resetList.destroy();
			}
			if (fixings != null) {
				fixings.destroy();
			}
		}
		
	}

	@Override
	protected String getSql() throws OException {
		String resultTable = Query.getResultTableForId(savedQueryId);
		String sql =       "\nSELECT DISTINCT ab.tran_num"
					+      "\n, r.reset_date"
					+      "\n, i.index_id AS \"index\""
					+      "\n, r.ristart_date AS \"start_date\""
					+      "\n, r.riend_date AS \"date\""
					+      "\n, p.ref_source AS \"source\""
					+      "\n, p.reset_yield_basis AS \"yield_basis\""
					+      "\n, p.proj_index_tenor AS \"tenor\""
					+      "\n, p.index_loc_id AS \"index_location\""
					+      "\n, p.projector_id"
					+	   "\n, ab.internal_portfolio"
					+	   "\nFROM " + resultTable + " qr"
					+      "\n  INNER JOIN ab_tran ab"
					+ 	   "\n     ON ab.tran_num = qr.query_result"
					+      "\n       AND ab.current_flag = 1"
					+      "\n  INNER JOIN param_reset_header p"
					+	   "\n     ON p.ins_num = ab.ins_num"
					+ 	   "\n   INNER JOIN idx_def i"
					+	   "\n     ON i.index_id = p.proj_index"
					+	   "\n       AND i.db_status = 1"
					+	   "\n   INNER JOIN reset r"
					+      "\n     ON r.ins_num = p.ins_num"
					+	   "\n       AND r.param_seq_num = p.param_seq_num"
					+	   "\n       AND r.param_reset_header_seq_num = p.param_reset_header_seq_num"
					+	   "\n       AND r.reset_date <= '" + OCalendar.formatJdForDbAccess(endDate) + "'"
					+	   (getUnfixedOnly()?"\n       AND r.value_status = 2":"") // unknown
					+      "\nWHERE qr.unique_id = " + savedQueryId
					;
		return sql;
	}

	@Override
	public String getLogLevel() {
		return LOG_LEVEL;
	}

	@Override
	public String getLogFile() {
		return LOG_FILE;
	}

	@Override
	public String getEndDateString() {
		return END_DATE;
	}

	@Override
	public String getQueryName() {
		return SAVED_QUERY_NAME;
	}

	@Override
	public GroupingCriteria getGrouping() {
		return GROUPING;
	}

	@Override
	public int getClusterSize() {
		return CLUSTER_SIZE;
	}
	
	@Override
	protected String[] getGroupingCols() {
		return GROUP_COLS;		
	}

	@Override
	public boolean getUnfixedOnly() {
		return UNFIXED_ONLY;
	}
}
