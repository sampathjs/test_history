package com.matthey.pmm.tradebooking.processors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.openlink.util.constrepository.ConstRepository;

public class RunProcessor extends AbstractRunProcessor {
	private final List<String> filesToProcess;
			
	public RunProcessor (final Session session, final ConstRepository constRepo,
			final String client, final List<String> filesToProcess) {
		super(session, constRepo, client);
		this.filesToProcess = new ArrayList<>(filesToProcess);
		processLogTable = setupProcessLogTable();
	}

	public boolean processRun () {
		try (UserTable runLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_RUN_LOG);
			 UserTable processLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_PROCESS_LOG)) {
			runLogUserTable.insertRows(runLogTable);
			processLogUserTable.insertRows(processLogTable);
			int dealCounter = 0;
			boolean overallSuccess = true;
			int failedDealCounter = 0;
			
	    	for (String fileNameToProcess : filesToProcess) {
	    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter);
				runLogUserTable.updateRows(runLogTable, COL_RUN_ID);
				processLogTable.setString(COL_OVERALL_STATUS, dealCounter, "Processing");
				processLogUserTable.updateRows(processLogTable, COL_RUN_ID + ", " + COL_DEAL_COUNTER);
				FileProcessor fileProcessor = new FileProcessor(session, constRepo, runId, dealCounter);
	    		getLogger().info("Processing file ' " + fileNameToProcess + "' now.");
	    		boolean success;
	    		String failReason = "";
	    		try {
		    		success = fileProcessor.processFile(fileNameToProcess);
	    		} catch (Throwable t) {
	    			success = false;
	    			getLogger().error("Error while processing file '" + fileNameToProcess + "': " + t.toString());
	        		StringWriter sw = new StringWriter(4000);
	        		PrintWriter pw = new PrintWriter(sw);
	        		t.printStackTrace(pw);
	        		logger.error(sw.toString());
	        		failReason = t.toString();
	    		}
	    		overallSuccess &= success;
	    		if (success) {
		    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter + " finished successfully");
	    			getLogger().info("Processing of ' " + fileNameToProcess + "' finished successfully");
	    			processLogTable.setString(COL_OVERALL_STATUS, dealCounter, "Finished Successfully");
	    			processLogTable.setInt(COL_DEAL_TRACKING_NUM, dealCounter, fileProcessor.getLatestDealTrackingNum());
	    		} else {
	    			failedDealCounter++;
		    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter + " failed");
	    			processLogTable.setString(COL_OVERALL_STATUS, dealCounter, "Failed. " + failReason);
	    			getLogger().error("Processing of ' " + fileNameToProcess + "' failed");
	    		}
	    		processLogTable.setDate(COL_LAST_UPDATE, dealCounter, new Date());
				runLogUserTable.updateRows(runLogTable, COL_RUN_ID);
				processLogUserTable.updateRows(processLogTable, COL_RUN_ID + ", " + COL_DEAL_COUNTER);
				dealCounter++;
	    	}
	    	if (overallSuccess) {
	    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run successfully");
	    	} else {
	    		if (failedDealCounter < dealCounter) {
		    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run. " + failedDealCounter + " of "
		    			+ dealCounter + " deals failed to be booked.");
	    		} else {
		    		runLogTable.setString (COL_STATUS, 0, "Finished processing of all deals of run. All deals failed to be booked.");	    			    				    			
	    		}
	    	}
	    	runLogTable.setDate(COL_END_DATE, 0, new Date());
			runLogUserTable.updateRows(runLogTable, COL_RUN_ID);	
			return overallSuccess;
		}
	}

	@Override
	protected List<String> fileNameSupplier() {
		return filesToProcess;
	}
}
