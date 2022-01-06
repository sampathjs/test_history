package com.matthey.pmm.tradebooking.processors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;

import ch.qos.logback.classic.Logger;

public class RunProcessor {
	private static final String USER_TABLE_RUN_LOG = "USER_jm_builder_run_log";
	private static final String USER_TABLE_PROCESS_LOG = "USER_jm_builder_process_log";
	
	private static final String COL_RUN_ID = "run_id";
	private static final String COL_CLIENT = "client";
	private static final String COL_START_DATE = "start_date";
	private static final String COL_END_DATE = "end_date";
	private static final String COL_STATUS = "status";

	private static final String COL_DEAL_COUNTER = "deal_counter";
	private static final String COL_INPUT_FILE_PATH = "input_file_path";
	private static final String COL_OVERALL_STATUS = "overall_status";
	private static final String COL_DEAL_TRACKING_NUM = "deal_tracking_num";
	private static final String COL_LAST_UPDATE = "last_update";

	
	private final Logger logger;
	private final Session session;
	private final List<String> filesToProcess;
	private final String client;
	private final int runId;
	private final ConstRepository constRepo;
	private final Table runLogTable;
	private Table processLogTable;
		
	public RunProcessor (final Session session, final Logger logger, final ConstRepository constRepo,
			final String client, final List<String> filesToProcess) {
		this.session = session;
		this.logger = logger;
		this.constRepo = constRepo;
		this.client = client;
		this.filesToProcess = new ArrayList<>(filesToProcess);
		runId = getMaxCurrentRunId() + 1;
		runLogTable = setupRunLogTable();
		processLogTable = setupProcessLogTable();
	}

	public void processRun () {
		try (UserTable runLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_RUN_LOG);
			 UserTable processLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_PROCESS_LOG)) {
			runLogUserTable.insertRows(runLogTable);
			processLogUserTable.insertRows(processLogTable);
			int dealCounter = 0;
			boolean overallSuccess = true;			
			
	    	for (String fileNameToProcess : filesToProcess) {
	    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter);
				runLogUserTable.updateRows(runLogTable, COL_RUN_ID);
				processLogTable.setString(COL_OVERALL_STATUS, dealCounter, "Processing");
				processLogUserTable.updateRows(processLogTable, COL_RUN_ID + ", " + COL_DEAL_COUNTER);
				FileProcessor fileProcessor = new FileProcessor(session, constRepo, logger, runId, dealCounter);
	    		logger.info("Now processing file' " + fileProcessor + "'");
	    		boolean success = fileProcessor.processFile(fileNameToProcess);
	    		overallSuccess &= success;
	    		if (success) {
		    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter + " finished successfully");
	    			logger.info("Processing of ' " + fileNameToProcess + "' finished successfully");
	    			processLogTable.setString(COL_OVERALL_STATUS, dealCounter, "Finished Successfully");
	    			processLogTable.setInt(COL_DEAL_TRACKING_NUM, dealCounter, fileProcessor.getLatestDealTrackingNum());
	    		} else {
		    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter + " failed");
	    			processLogTable.setString(COL_OVERALL_STATUS, dealCounter, "Failed");
	    			logger.error("Processing of ' " + fileNameToProcess + "' failed");	    			
	    		}
	    		processLogTable.setDate(COL_LAST_UPDATE, dealCounter, new Date());
				runLogUserTable.updateRows(runLogTable, COL_RUN_ID);
				processLogUserTable.updateRows(processLogTable, COL_RUN_ID + ", " + COL_DEAL_COUNTER);
				dealCounter++;
	    	}
	    	if (overallSuccess) {
	    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run successfully");
	    	} else {
	    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run. Some deals failed to be booked.");	    		
	    	}
	    	runLogTable.setDate(COL_END_DATE, 0, new Date());
			runLogUserTable.updateRows(runLogTable, COL_RUN_ID);	    	
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
				runLogTable.setString(COL_OVERALL_STATUS, rowNum, "Not Started");
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
