package com.matthey.pmm.tradebooking.processors;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;

public abstract class AbstractRunProcessor {
	public static final String JOB_LOG_INIT_STATUS = "Not Started";
	public static final String USER_TABLE_RUN_LOG = "USER_jm_builder_run_log";
	public static final String USER_TABLE_PROCESS_LOG = "USER_jm_builder_process_log";
	
	public static final String COL_RUN_ID = "run_id";
	public static final String COL_CLIENT = "client";
	public static final String COL_START_DATE = "start_date";
	public static final String COL_END_DATE = "end_date";
	public static final String COL_STATUS = "status";

	public static final String COL_DEAL_COUNTER = "deal_counter";
	public static final String COL_INPUT_FILE_PATH = "input_file_path";
	public static final String COL_OVERALL_STATUS = "overall_status";
	public static final String COL_DEAL_TRACKING_NUM = "deal_tracking_num";
	public static final String COL_LAST_UPDATE = "last_update";		
	
	protected static Logger logger = null;
	protected final Session session;
	protected final String client;
	protected final ConstRepository constRepo;
	protected final int runId;
	protected final Table runLogTable;
	protected Table processLogTable;
	
	protected abstract List<String> fileNameSupplier();
	
	protected static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(AbstractRunProcessor.class);
		}
		return logger;
	}

	public AbstractRunProcessor(final Session session, ConstRepository constRepo, final String client, final int runId) {
		this.session = session;
		this.client = client;
		this.constRepo = constRepo;
		this.runId = runId;
		runLogTable = setupRunLogTable();
		processLogTable = setupProcessLogTable();
	}
	
	public AbstractRunProcessor(final Session session, ConstRepository constRepo, final String client) {
		this.session = session;
		this.client = client;
		this.constRepo = constRepo;
		this.runId = getMaxCurrentRunId()+1;
		runLogTable = setupRunLogTable();
	}
	
	public AbstractRunProcessor(final Session session, ConstRepository constRepo) {
		this.session = session;
		this.constRepo = constRepo;
		runLogTable = setupRunLogTable();
		String sql = getSqlForUnprocessedProcessLogRows();
		processLogTable = session.getIOFactory().runSQL(sql);
		runId = -1;
		client = "";
	}
	
	protected Table setupProcessLogTable() {
		try (UserTable userTable = session.getIOFactory().getUserTable(USER_TABLE_PROCESS_LOG);) {
			Table runLogTable = userTable.getTableStructure();
			int dealCounter = 0;
			for (String filename : fileNameSupplier()) {
				int rowNum = runLogTable.addRow().getNumber();
				runLogTable.setInt(COL_RUN_ID, rowNum, runId);
				runLogTable.setInt(COL_DEAL_COUNTER, rowNum, dealCounter++);
				runLogTable.setString(COL_INPUT_FILE_PATH, rowNum, filename);
				runLogTable.setString(COL_OVERALL_STATUS, rowNum, "Not Started");
				runLogTable.setInt(COL_DEAL_TRACKING_NUM, rowNum, -1);
				runLogTable.setDate(COL_LAST_UPDATE, rowNum, new Date());
			}
			return runLogTable;
		}		
	}
	
	protected Table setupRunLogTable() {
		try (UserTable userTable = session.getIOFactory().getUserTable(USER_TABLE_RUN_LOG);) {
			Table runLogTable = userTable.getTableStructure();
			runLogTable.addRow();
			runLogTable.setInt(COL_RUN_ID, 0, runId);
			runLogTable.setString(COL_CLIENT, 0, client);
			runLogTable.setDate(COL_START_DATE, 0, new Date());			
			runLogTable.setString(COL_STATUS, 0, "Initialising");
			return runLogTable;
		}		
	}
	
	protected int getMaxCurrentRunId() {
		String sql = "\nSELECT MAX (" + COL_RUN_ID + ") FROM " + USER_TABLE_RUN_LOG;
		try (Table sqlResult = session.getIOFactory().runSQL(sql);) {
			return sqlResult.getInt(0, 0);
		}
	}
	
	protected Table getRunLogTable(int runId) {
		String sql = "\nSELECT * FROM " + USER_TABLE_RUN_LOG 
				+ "\nWHERE " + COL_RUN_ID + " = " + runId 
				;
		getLogger().info("Executing SQL to get run log table row for run: " + sql);
		return session.getIOFactory().runSQL(sql);
	}
	
	protected int getFailedDealCountForRun(int runId) {
		String sql = "\nSELECT COUNT (l." + COL_OVERALL_STATUS + ")" 
				+ "\nFROM " + USER_TABLE_PROCESS_LOG + " l"
				+ "\nWHERE l." + COL_RUN_ID + " = " + runId
				+ "\n AND l." + COL_OVERALL_STATUS + " LIKE 'Failed.%'" 
				;
		getLogger().info("Executing SQL to get number of failed bookings for run: " + sql);
		try (Table sqlResult =  session.getIOFactory().runSQL(sql)) {
			getLogger().info ("Number of failed runs: " + sqlResult.getInt(0, 0));
			return sqlResult.getInt(0, 0);
		}
	}
	
	protected void retrieveFinalProcessLogTable() {
		processLogTable.close();
		String sql = 
				"\nSELECT *"
			+	"\nFROM " + USER_TABLE_PROCESS_LOG 
			+	"\nWHERE " + COL_RUN_ID + " = " + runId
				;
		processLogTable = session.getIOFactory().runSQL(sql);
	}
	
	protected int retrieveNumberOfOpenDealsForRun(int runId) {
		String sql = 
				"\nSELECT COUNT(*) "
			+	"\nFROM " + USER_TABLE_PROCESS_LOG 
			+	"\nWHERE " + COL_RUN_ID + " = " + runId
			+   "\n  AND " + COL_OVERALL_STATUS + " = '" + JOB_LOG_INIT_STATUS + "'";
				;
		getLogger().info("Executing SQL to number of onprocessed deals for run: " + sql);
		try (Table sqlResult =  session.getIOFactory().runSQL(sql)) {
			getLogger().info ("Number of unprocessed rows: " + sqlResult.getInt(0, 0));
			return sqlResult.getInt(0, 0);
		}
	}

	protected int retrieveNumberOfOpenDealsForRun() {
		return retrieveNumberOfOpenDealsForRun (runId);
	}

	protected int retrieveNumberOfFailedDealsForRun() {
		String sql = 
				"\nSELECT COUNT(*) "
			+	"\nFROM " + USER_TABLE_PROCESS_LOG 
			+	"\nWHERE " + COL_RUN_ID + " = " + runId
			+   "\n  AND " + COL_OVERALL_STATUS + " LIKE 'Failed.%'";
				;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			return sqlResult.getInt(0, 0);
		}
	}

	protected int retrieveNumberOfSuccededDealsForRun() {
		String sql = 
				"\nSELECT COUNT(*) "
			+	"\nFROM " + USER_TABLE_PROCESS_LOG 
			+	"\nWHERE " + COL_RUN_ID + " = " + runId
			+   "\n  AND " + COL_OVERALL_STATUS + " = 'Finished Successfully'";
				;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			return sqlResult.getInt(0, 0);
		}
	}

	
	public Map<String, Integer> getBookedDealTrackingNums () {
		Map<String, Integer> bookedDealTrackingNums = new HashMap<>();
		processLogTable.getRows().forEach(x -> bookedDealTrackingNums.put(x.getString(COL_INPUT_FILE_PATH), x.getInt(COL_DEAL_TRACKING_NUM)));
		return bookedDealTrackingNums;
	}
	
	private String getSqlForUnprocessedProcessLogRows() {
		String sql = "\nSELECT j." + COL_RUN_ID
				+	 "\n  ,j." + COL_DEAL_COUNTER
				+    "\n  ,j." + COL_INPUT_FILE_PATH
				+    "\n  ,j." + COL_OVERALL_STATUS
				+    "\n  ,j." + COL_DEAL_TRACKING_NUM
				+    "\n  ,j." + COL_LAST_UPDATE
				+    "\nFROM " + USER_TABLE_PROCESS_LOG + " j"
				+    "\nWHERE j." + COL_OVERALL_STATUS + " = '" + JOB_LOG_INIT_STATUS + "'"
				+    "\n  AND j." + COL_DEAL_TRACKING_NUM + " = -1"
				+    "\nORDER BY j." + COL_RUN_ID + ", j." + COL_DEAL_COUNTER
				;
		return sql;
	}

}