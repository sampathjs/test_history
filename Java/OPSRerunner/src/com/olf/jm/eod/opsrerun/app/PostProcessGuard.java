package com.olf.jm.eod.opsrerun.app;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OP_SERVICES_LOG_STATUS;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
/* 
* History: 
* 2016-10-07	V1.0 jwaechter - initial version
* 2016-10-10	V1.1 jwaechter - now defaulting log directory to "AB_OUTDIR"
*                              - added retryCountIgnore
* 2016-10-13	V1.2 jwaechter - Removed filter for OPS entries older than today
*/ 

/** 
* EOD Script 
* Checks for pending op service jobs for a specific op service definition of today or before
* 
* @author jwaechter
* @version 1.2
**/ 

public class PostProcessGuard implements IScript
{	
	private int timespanPerTry = 30;      /*sec*/ // determines the sleep time

	private void initLogging() throws OException {
		ConstRepository constRepo = new ConstRepository ("Ops", "PostProcessGuard");
		String debugLevel = constRepo.getStringValue("logLevel", "Info");
		String logFile    = constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log");
		String logDir    = constRepo.getStringValue("logDir", Util.getEnv("AB_OUTDIR") + "\\error_logs");
		
		try
		{
			Logging.init(this.getClass(), "Ops", "PostProcessGuard");
		}
		catch (Throwable t)
		{
			Logging.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
		}
	}

	public void execute(IContainerContext context) throws OException
	{
		try {
			initLogging();
			PostProcessGuardArgt args = new PostProcessGuardArgt(context.getArgumentsTable());
			if (!args.isCancelled()) {
				int retryCount = args.getRetryCount();
				int retryCountIgnore = args.getRetryCountIgnore();
				boolean pendingPostProcessesLeft=false;
				for (Item item : args.getItems()) {
					String opsName = item.getOpsName();
					String query = item.getAdditionalQuery();
					Logging.info("Starting rerun for '"  + opsName + "' using query '" + query + "'"
						+	" with a timespan of " + timespanPerTry + " and " + retryCount + " retries");
					pendingPostProcessesLeft |= rerunService(context, opsName, query, retryCount, retryCountIgnore);			
				}
				if (pendingPostProcessesLeft) {
					throw new OException ("At least one pending post process is left over");
				}
			}
		} catch (Throwable e) {
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}			
			throw e;
		}finally{
			Logging.close();
		}
	}

	private boolean rerunService(IContainerContext context, String opServiceName, 
			String additionalQuery, int retryCount, int retryCountIgnore) throws OException {
		Table argt = context.getArgumentsTable();		
		/* time in milliseconds to wait before looking again for pending post-process scripts to run */
		int numPendingJobs;
		int queryId=-1;
		Table pendingJobs = null;
		
		String opServiceType = getOpServiceTypeForDef (opServiceName);
		boolean pendingPostProcessesLeft = false;
		try {
			do {
				try {
					if (!additionalQuery.trim().equals("None")) {
						queryId = Query.run(additionalQuery);						
					} else {
						queryId = -1;
					}
					pendingJobs = retrievePendingJobs(opServiceName, queryId, retryCountIgnore);
				} catch (OException e) {
					Logging.error ("Error checking pending jobs of Op Service '" + opServiceName + "'. Script execution will be terminated.");
					
				}
				numPendingJobs = pendingJobs.getNumRows();

				// stop if everything's fine/
				if (numPendingJobs == 0) {
					break;
				}
				if (retryCount > 1) {
					PostProcessRestarter restarter = 
							new PostProcessRestarter (opServiceName, opServiceType, queryId);
					restarter.restart();
					Debug.sleep(timespanPerTry * 1000);
				}
			} while (--retryCount > 0);

			if (numPendingJobs > 0) {
				int runId;
				String scriptName;
				ODateTime rowCreation;
				int itemNum;

				Logging.info ("" + numPendingJobs + " jobs of Op Service '" + opServiceName + "' are pending: ");
				for (int row = numPendingJobs; row >= 1; row--) {
					runId = pendingJobs.getInt("op_services_run_id", row);
					itemNum = pendingJobs.getInt("item_num", row);
					scriptName = pendingJobs.getString("script_name", row);
					rowCreation = pendingJobs.getDateTime("row_creation", row);
					Logging.info ("Run Id: " + runId + " - Item Num: " + itemNum + " - Script Name: " + scriptName + " - Row Creation: " + rowCreation.formatForDbAccess());
				}
				pendingJobs.viewTable();
				Logging.info ("Script will terminate execution in status 'failed'.");
				pendingPostProcessesLeft = true;
			}
		} finally {
			if (queryId != -1) {
				Query.clear(queryId);
			}
		}
		return pendingPostProcessesLeft;
	}

	private String getOpServiceTypeForDef(String opServiceName) throws OException {
		Table sqlResult = Table.tableNew("Op Service type for " + opServiceName);
		String sql = "SELECT t.name AS ops_type"
				+ "\n FROM "
				+ "\n      rsk_exposure_defn r"
				+ "\n    , ops_service_type t"
				+ "\n WHERE r.defn_name = '" + opServiceName + "'"
				+ "\n   AND r.defn_subtype = t.id_number"
				;		
		int ret = DBaseTable.execISql(sqlResult, sql);
		String typeName = sqlResult.getString(1, 1);
		sqlResult.destroy();
		return typeName;
	}

	/**
	 * @param queryId Id of a query. All results returned will be log entry items
	 * for transactions contained in this query.
	 * 
	 */
	private Table retrievePendingJobs (String opsDefName, int queryId, int retryCountIgnore) throws OException
	{
		int numLogEvents;

		Logging.info ("Check Op Service " + opsDefName + " for pending jobs");

		/* Load all unprocessed entries for a given op_service_type based on initial parameters */
		Table logTable = loadPostProcessingLog(opsDefName, queryId);
		numLogEvents = logTable.getNumRows();

		// filter by status
		OP_SERVICES_LOG_STATUS status;
		for (int row = numLogEvents; row >= 1; row--) {
			status = OP_SERVICES_LOG_STATUS.fromInt(logTable.getInt("log_status", row));
			int runCount = logTable.getInt("call_count", row);
			switch (status) {
				case OP_SERVICES_LOG_STATUS_ABORTED:
				case OP_SERVICES_LOG_STATUS_FAILED:
				case OP_SERVICES_LOG_STATUS_NA:
				case OP_SERVICES_LOG_STATUS_NEEDS_TO_RUN:
				case OP_SERVICES_LOG_STATUS_RUNNING:
					Logging.debug ("Job found in status " + status.toString());
					break;
				case OP_SERVICES_LOG_STATUS_SUCCEEDED:
					Logging.debug ("Job in status " + status.toString() + " will be ignored.");
					logTable.delRow(row);
					break;
			}
			if (runCount > retryCountIgnore) {
				Logging.debug ("Job in status " + status.toString() + " will be ignored due to retryCount.");
				logTable.delRow(row);				
			}
		}
		return logTable;
	}

	/*
	 * Note that the following method works with the following assumption: each item id 
	 * of the post process ops log entry is also found in the query result of queryId
	 * */
	private Table loadPostProcessingLog(String opServiceDefName, int queryId) throws OException {
		Table logTable = Table.tableNew("Post Processing Log (" + opServiceDefName + ")");
		int today = OCalendar.today();
		
		// select all pending jobs for 
		// - the requested op service 
		// - of today (= tomorrow midnight) or before
		String sql = "SELECT d.*, dir.node_name script_name, d.item_num, t.name AS ops_type, c.call_count"
				+ "\n FROM op_services_log_detail d"
				+ "\n INNER JOIN op_services_log l ON l.op_services_run_id = d.op_services_run_id"
				+ "\n INNER JOIN rsk_exposure_defn r ON d.defn_id = r.exp_defn_id"
				+ "\n INNER JOIN dir_node dir ON d.script_id = dir.client_data_id"
				+ "\n INNER JOIN ops_service_type t ON r.defn_subtype = t.id_number"
				+ "\n INNER JOIN (SELECT op_services_run_id, count(*) AS call_count"
				+ "\n             FROM op_services_log_detail_history"
				+ "\n             WHERE log_status = 4"
				+ "\n             GROUP BY op_services_run_id) c ON c.op_services_run_id = d.op_services_run_id"
				+ ((queryId != -1)?("\n INNER JOIN query_result qr ON d.item_num = qr.query_result"):"")
				+ "\n WHERE "
				+ "\n   r.defn_name = '" + opServiceDefName + "'"
				+ "\n   AND dir.node_type = 7"
				+ ((queryId != -1)?("\n   AND qr.unique_id = " + queryId):"")				
				;
		
		int ret = DBaseTable.execISql(logTable, sql);
		
		return logTable;
	}

}
