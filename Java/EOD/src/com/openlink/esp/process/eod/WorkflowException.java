package com.openlink.esp.process.eod;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.alertbroker.AlertBroker;
/*
 * HISTORY
 * 1.0 - 2011.06.23 - dmoebius - initial version
 */
/**
 * Script will be used in case a workflow is failing.
 * Creates an AlertBroker Alert. 
 * It will create a detailed message which workflow/job has been failing
 * @author dmoebius
 * @version 1.0
 */
public class WorkflowException implements IScript{

	public void execute(IContainerContext context) throws OException {
		
		AlertBroker.sendAlert("WEX-001", getDetailedErrorMessage(context));
	}
	
	/**
	 * creates a detailed message out of the arg
	 * @param context given from system
	 * @return error message
	 * @throws OException
	 */
	private static String getDetailedErrorMessage(IContainerContext context) throws OException{
			String message = "Workflow failed:";
			Table argt = context.getArgumentsTable();
			Table failedJob;
			if(argt.getColNum("Failed Job") < 1 || argt.getNumRows() < 1){
				message = "Workflow failed! Can't create detailed message: " +
						"argt has not the correct structure.";
			}else{
				failedJob = argt.getTable("Failed Job", 1);
				if(failedJob.getColNum("workflow_name") < 0
						|| failedJob.getColNum("job_name") < 0 
						|| failedJob.getColNum("start_time") < 0
						|| failedJob.getColNum("end_time") < 0 
						|| failedJob.getColNum("server_node") < 0 
						|| failedJob.getNumRows() < 1){
					
					message = "Workflow failed! Can't create detailed message: " +
							"failedJob table has not the correct structure.";
				}else{
					String workflow = failedJob.getString("workflow_name", 1);
					String job = failedJob.getString("job_name", 1);
					ODateTime startDate = failedJob.getDateTime("start_time", 1);
					ODateTime endTime = failedJob.getDateTime("end_time", 1);
					String serverNode = failedJob.getString("server_node", 1);
					message = "A workflow failed. -- "
								+ "Workflow '" + workflow + "' -- "		
								+ "Job:  '" + job + "' -- "
								+ "Start time: '" + startDate.formatForDbAccess() + "' -- "
								+ "End time: '" + endTime.formatForDbAccess() + "' -- "
								+ "Server Node: '" + serverNode + "'.";
				}
			}
			
			return message;
		}

}
