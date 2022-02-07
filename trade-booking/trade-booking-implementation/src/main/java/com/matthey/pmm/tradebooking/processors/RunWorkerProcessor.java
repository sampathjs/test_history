package com.matthey.pmm.tradebooking.processors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;

public class RunWorkerProcessor extends AbstractRunProcessor {
			
	public RunWorkerProcessor (final Session session, final ConstRepository constRepo,
			final String client) {
		super(session, constRepo);
	}

	public void processRun() {
		try (UserTable runLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_RUN_LOG);
			 UserTable processLogUserTable = session.getIOFactory().getUserTable(USER_TABLE_PROCESS_LOG)) {
			for (int row=processLogTable.getRowCount()-1; row >= 0; row--) {
				int runId = processLogTable.getInt(COL_RUN_ID, row);
				int dealCounter = processLogTable.getInt(COL_DEAL_COUNTER, row);
				String fullPath = processLogTable.getString(COL_INPUT_FILE_PATH, row);
				try (Table runLogTable = getRunLogTable(runId);) {
					FileProcessor fileProcessor = new FileProcessor(session, constRepo, runId, dealCounter);
		    		getLogger().info("Processing file '" + fullPath + "' now.");
		    		int failedDealCounter = getFailedDealCountForRun(runId);
		    		int openDealsForRun = retrieveNumberOfOpenDealsForRun (runId);
		    		boolean success;
		    		String failReason = "";
		    		
		    		try {
			    		success = fileProcessor.processFile(fullPath);
		    		} catch (Throwable t) {
		    			success = false;
		    			getLogger().error("Error while processing file '" + fullPath + "': " + t.toString());
		        		StringWriter sw = new StringWriter(4000);
		        		PrintWriter pw = new PrintWriter(sw);
		        		t.printStackTrace(pw);
		        		getLogger().error(sw.toString());
		        		failReason = t.toString();
		    		}
		    		boolean overallSuccess = success && (failedDealCounter == 0);
		    		if (overallSuccess) {
			    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter + " finished successfully");
		    			getLogger().info("Processing of ' " + fullPath + "' finished successfully");
		    			processLogTable.setString(COL_OVERALL_STATUS, row, "Finished Successfully");
		    			processLogTable.setInt(COL_DEAL_TRACKING_NUM, row, fileProcessor.getLatestDealTrackingNum());
		    		} else {
		    			failedDealCounter++;
			    		runLogTable.setString (COL_STATUS, 0, "Processing deal #" + dealCounter + " failed");
			    		processLogTable.setString (COL_OVERALL_STATUS, row, "Failed. " + failReason);
		    			getLogger().error("Processing of ' " + fullPath + "' failed");
		    		}
		    		if (openDealsForRun == 1) {
		    			getLogger().error("All deals for run #" + runId + " have been processed.");
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
		    		}
			    	runLogTable.setDate(COL_END_DATE, 0, new Date());
					runLogUserTable.updateRows(runLogTable, COL_RUN_ID);	
		    		processLogTable.setDate(COL_LAST_UPDATE, row, new Date());
					runLogUserTable.updateRows(runLogTable, COL_RUN_ID);
					processLogUserTable.updateRows(processLogTable, COL_RUN_ID + ", " + COL_DEAL_COUNTER);
				}
			}
		}
	}	


	@Override
	protected List<String> fileNameSupplier() {
		return new ArrayList<>(); // file names are going to be processed differently
	}
}
