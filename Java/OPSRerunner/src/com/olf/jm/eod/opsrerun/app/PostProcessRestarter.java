package com.olf.jm.eod.opsrerun.app;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-10-07 	V1.0	jwaechter 	- initial version
 * 2016-10-10	V1.1	jwaechter 	- fixed issues with the query retrieval
 */

/**
 * Library class containing a single method to rerun post process log services 
 * of a given type and name.
 * @author jwaechter
 * @version 1.0
 */
public class PostProcessRestarter 
{
	private final String opServiceName;
	private final String opServiceType;
	private final List<Long> itemIds;

	public PostProcessRestarter(String opServiceName, String opServiceType, int queryId) throws OException {

		this.opServiceName = opServiceName;
		this.opServiceType = opServiceType;
		if (queryId > 0) {
			this.itemIds = new ArrayList<>();
			Table items = Table.tableNew();
			String colName = "";
			Logging.info(Query.getResultTableForId(queryId));
			if (Query.getResultTableForId(queryId).indexOf("64") > -1) {
				items.addCol("item_no_long", COL_TYPE_ENUM.COL_INT64);
				colName = "item_no_long";
			} else {
				items.addCol("item_no", COL_TYPE_ENUM.COL_INT);
				colName = "item_no";
			}
			int ret = Query.tableLoadQueryResult(items, colName, queryId);
			for (int row=items.getNumRows(); row >= 1; row--) {
				long itemNo;
				if ("item_no_long".equals(colName)) {
					itemNo = items.getInt64(colName, row);
				} else {
					itemNo = items.getInt(colName, row);
				}
				itemIds.add(itemNo);
			}
			items.destroy();
		} else {
			this.itemIds = null;			
		}
	}		

	public void restart() throws OException
	{
		initLogging();
		Logging.info("Starting restart() method in PostProcessRestarter");
		// Use this script to process unprocessed Operation Services entries - one re-run per item
		// The script should be used when entries for a given definition DO NOT HAVE THE SAME log status

		// ***Specify initial parameters ***
		// time in Math.min (3 days = 4320) which represents entries that are too old for processing
		int staleEventName = 4320*30;

		// time in Math.min (5 Math.min minimum) which represents minimum time between an item having a status
		// of Needs to Run and a request for Running
		int minProcessTime = 5;

		// Number of requests (items) to process at a given run (oldest first), if set to 0 - process all
		int chunkSize = 0;

		// Service Type
		int opServiceTypeId = 
				Ref.getValue(SHM_USR_TABLES_ENUM.OPS_SERVICE_TYPE_TABLE, opServiceType);
		Logging.info("opServiceTypeId=" + opServiceTypeId + " opsServiceName=" + opServiceType);

		// *********************************

		int ret, row, numRows, status;
		Table all = Table.tableNew("ALL"); // used for TABLE_Destroy() on exit
		Table paramTable;
		Table logTable;
		String logServiceName;

		OP_SERVICES_LOG_STATUS runStatus;

		all.addCol("t", COL_TYPE_ENUM.COL_TABLE);

		// Put initial parameters in the parameter table
		paramTable = createTable("Param", 0, all);
		paramTable.addCol("op_service_type", COL_TYPE_ENUM.COL_INT);
		paramTable.addCol("stale_event_time", COL_TYPE_ENUM.COL_INT);
		paramTable.addCol("min_process_time", COL_TYPE_ENUM.COL_INT);
		paramTable.addCol("chunk_size", COL_TYPE_ENUM.COL_INT);
		paramTable.addCol("by_defn", COL_TYPE_ENUM.COL_INT); // 0 for this script

		row = paramTable.addRow();
		paramTable.setInt("op_service_type", row, opServiceTypeId);
		paramTable.setInt("stale_event_time", row, staleEventName);
		paramTable.setInt("min_process_time", row, minProcessTime);
		paramTable.setInt("chunk_size", row, chunkSize);

		// Load all unprocessed entries for a given op_service_type based on initial parameters
		// The number of rows in this table will be equal to chunk_size
		logTable = createTable("PostProcessLog", 0, all);
		ret = OpService.loadPostProcessingLog(logTable, paramTable);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			Logging.warn("Error loading post processing log");
			cleanUp(all, ret);
		} 

		numRows = logTable.getNumRows();
		Logging.info("Found " + numRows + " rows in the post processing log");
		int noProcessedRows = 0;
		for (row = 1; row <= numRows; row++){
			status = logTable.getInt("log_status", row);
			logServiceName = logTable.getString("defn_name", row);
			long itemNum = logTable.getInt("item_num", row);
			if (!logServiceName.equalsIgnoreCase(opServiceName)) {
				continue;
			}
			if (itemIds != null && itemIds.size() > 0 && !itemIds.contains(itemNum)) {
				continue;
			}
			// Process entries based on status
			runStatus = OP_SERVICES_LOG_STATUS.fromInt(status);

			switch (runStatus) {
			case OP_SERVICES_LOG_STATUS_ABORTED:
			case OP_SERVICES_LOG_STATUS_NA:
			case OP_SERVICES_LOG_STATUS_NEEDS_TO_RUN:
			case OP_SERVICES_LOG_STATUS_FAILED:
				noProcessedRows++;
				logTable.setInt("log_status", row, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_NEEDS_TO_RUN.toInt());
				ret = OpService.runPostProcess(opServiceTypeId, logTable, row, Util.NULL_TABLE);
				if (ret != 1) {
					Logging.error("Could not restart pending post process log for " + logServiceName );					
				}
				break;
			case OP_SERVICES_LOG_STATUS_RUNNING:
			case OP_SERVICES_LOG_STATUS_SUCCEEDED:
				break;
			}	
		}
		Logging.info("Restarted " + noProcessedRows + " of " + numRows + " items in the OPS Post Process log");
		cleanUp(all, ret);
		Logging.info("restart() method in PostProcessRestarter ends sucessfully");
		Logging.close();
	}

	private Table createTable(String name, int subTable, Table all) throws OException
	{
		Table new_table;

		// sub_table flag will be set when the table created will be part of another table (besides all)
		new_table = Table.tableNew(name);
		if (subTable == 0){
			all.setTable(1, all.addRow(), new_table);
		}
		return new_table;
	}

	private void cleanUp(Table all, int ret) throws OException
	{
		all.destroy();
	}

	private void initLogging() throws OException {
		ConstRepository constRepo = new ConstRepository ("EOD", "PostProcessGuard");
		String debugLevel = constRepo.getStringValue("logLevel", "Info");
		String logFile    = constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log");
		String logDir    = constRepo.getStringValue("logDir", null);

		try
		{
			Logging.init(this.getClass(), "EOD", "PostProcessGuard");
		}
		catch (Exception e)
		{
			OConsole.oprint(e.getMessage());
		}
	}

}