package com.matthey.pmm.tradebooking.processors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;

public class RunRemoteProcessor {
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

	
	public static Logger logger = null;
	public final Session session;
	public final List<String> filesToProcess;
	public final String client;
	public final int runId;
	public final ConstRepository constRepo;
	public final Table runLogTable;
	public Table processLogTable;
	public int timeoutInSeconds = 300;
	
	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(RunRemoteProcessor.class);
		}
		return logger;
	}
		
	public RunRemoteProcessor (final Session session, final ConstRepository constRepo,
			final String client, final List<String> filesToProcess) {
		this.session = session;
		this.constRepo = constRepo;
		this.client = client;
		this.filesToProcess = new ArrayList<>(filesToProcess);
		runId = getMaxCurrentRunId() + 1;
		runLogTable = setupRunLogTable();
		processLogTable = setupProcessLogTable();
		try {
			timeoutInSeconds = Integer.parseInt(constRepo.getStringValue("timeout", "300"));
		} catch (NumberFormatException e) {
			String msg = "ConstRepo entry " + constRepo.getContext() + "/" + constRepo.getSubcontext() + "/timeout is not a number."
					+ " Using default value of " + timeoutInSeconds + " instead";
			getLogger().error(msg);
		} catch (OException e) {
			String msg = "Error retrieving value from ConstRepo entry " + constRepo.getContext() + "/" + constRepo.getSubcontext() + "/timeout"
					+". Using default value of " + timeoutInSeconds + " instead";
			getLogger().error(msg);
		}
	}
	
	public boolean processRun () {
		try (UserTable runLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_RUN_LOG);
			 UserTable processLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_PROCESS_LOG)) {
			runLogUserTable.insertRows(runLogTable);
			// the following line initialises the table for the task doing the actual job.
			processLogUserTable.insertRows(processLogTable);
			int noOpenDeals = 0;
    		runLogTable.setString (COL_STATUS, 0, "Processing");
			runLogUserTable.updateRows(runLogTable, COL_RUN_ID);	
			Date startTime = new Date();
			do {
				noOpenDeals = retrieveNumberOfOpenDealsForRun (); 
			} while (noOpenDeals > 0 && (new Date().getTime() - startTime.getTime()) < timeoutInSeconds * 1000);
			int failedDealCounter = retrieveNumberOfFailedDealsForRun ();
			int succeededDealCounter = retrieveNumberOfSuccededDealsForRun ();
			if (succeededDealCounter + failedDealCounter ==  processLogTable.getRowCount()) {
				// no timeout
				if (failedDealCounter == 0) {
		    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run successfully");
		    	} else {
		    		if (failedDealCounter < processLogTable.getRowCount()) {
			    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run. " + failedDealCounter + " of "
			    			+ processLogTable.getRowCount() + " deals failed to be booked.");
		    		} else {
			    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run. All deals failed to be booked.");	    			    				    			
		    		}
		    	}
			} else {
	    		runLogTable.setString (COL_STATUS, 0, "Timeout while monitoring the deal booking process."
	    			+ " Current timeout is " + timeoutInSeconds + "(s)");
			}
	    	runLogTable.setDate(COL_END_DATE, 0, new Date());
			runLogUserTable.updateRows(runLogTable, COL_RUN_ID);	
			retrieveFinalProcessLogTable ();
			
			return failedDealCounter==0;
		}
	}
	
	private void retrieveFinalProcessLogTable() {
		processLogTable.close();
		String sql = 
				"\nSELECT *"
			+	"\nFROM " + USER_TABLE_PROCESS_LOG 
			+	"\nWHERE " + COL_RUN_ID + " = " + runId
				;
		processLogTable = session.getIOFactory().runSQL(sql);
	}

	private int retrieveNumberOfOpenDealsForRun() {
		String sql = 
				"\nSELECT COUNT(*) "
			+	"\nFROM " + USER_TABLE_PROCESS_LOG 
			+	"\nWHERE " + COL_RUN_ID + " = " + runId
			+   "\n  AND " + COL_OVERALL_STATUS + " = '" + JOB_LOG_INIT_STATUS + "'";
				;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			return sqlResult.getInt(0, 0);
		}
	}

	private int retrieveNumberOfFailedDealsForRun() {
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

	private int retrieveNumberOfSuccededDealsForRun() {
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
	
	private Table setupProcessLogTable() {
		try (UserTable userTable = session.getIOFactory().getUserTable(USER_TABLE_PROCESS_LOG);) {
			Table runLogTable = userTable.getTableStructure();
			int dealCounter = 0;
			for (String filename : filesToProcess) {
				int rowNum = runLogTable.addRow().getNumber();
				runLogTable.setInt(COL_RUN_ID, rowNum, runId);
				runLogTable.setInt(COL_DEAL_COUNTER, rowNum, dealCounter++);
				runLogTable.setString(COL_INPUT_FILE_PATH, rowNum, filename);
				runLogTable.setString(COL_OVERALL_STATUS, rowNum, JOB_LOG_INIT_STATUS);
				runLogTable.setInt(COL_DEAL_TRACKING_NUM, rowNum, -1);
				runLogTable.setDate(COL_LAST_UPDATE, rowNum, new Date());
			}
			return runLogTable;
		}
	}
	
	private Table setupRunLogTable() {
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
	
	private int getMaxCurrentRunId() {
		String sql = "\nSELECT MAX (" + COL_RUN_ID + ") FROM " + USER_TABLE_RUN_LOG;
		try (Table sqlResult = session.getIOFactory().runSQL(sql);) {
			return sqlResult.getInt(0, 0);
		}
	}
}
