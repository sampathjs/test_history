package com.matthey.pmm.tradebooking.processors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.openlink.util.constrepository.ConstRepository;

public class RunRemoteProcessor extends AbstractRunProcessor {	
	public final List<String> filesToProcess;
	public int timeoutInSeconds = 300;
			
	public RunRemoteProcessor (final Session session, final ConstRepository constRepo,
			final String client, final List<String> filesToProcess) {
		super(session, constRepo, client);
		this.filesToProcess = new ArrayList<>(filesToProcess);
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
	
	@Override
	protected List<String> fileNameSupplier() {
		return filesToProcess;
	}
}