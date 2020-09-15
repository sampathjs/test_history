package com.olf.jm.eod.opsrerun.app;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-10-07	V1.0	jwaechter	 - Initial Version
 * 2016-10-10	V1.1	jwaechter	 - Added retryCountIgnore
 * 2016-10-13	V1.2	jwaechter	 - Removed filter for OPS entries older than today
 */

/**
 * Parameter plugin for the PostProcessGuard
 * @author jwaechter
 * @version 1.2
 */
public class PostProcessGuardParam implements IScript {
	private String defaultOPS;
	private String additionalQuery;
	private int retryCount;
	private int timespan;
	private int retryCountIgnore;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init();
			ask(context);
			Logging.info("Parameter plugin finished");
		} catch (Throwable t) {
			Logging.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			PostProcessGuardArgt newArgt = new PostProcessGuardArgt(true, 0, 2, 0);
			Table argt = newArgt.toTable();
			context.getArgumentsTable().select(argt, "*", PostProcessGuardArgt.ARGT_PARAM_CANCELLED + " GT 1");
			argt.dispose();
			context.getArgumentsTable().viewTable();
			throw t;
		}finally{
			Logging.close();
		}
	}

	private void ask(IContainerContext context) throws OException {
		PostProcessGuardArgt newArgt;
		if (Util.canAccessGui() == 1) {
			newArgt = withGui (context);
			//newArgt = withoutGui (context);
		} else {
			newArgt = withoutGui (context);
		}
		Table argt = newArgt.toTable();
		context.getArgumentsTable().select(argt, "*", PostProcessGuardArgt.ARGT_PARAM_CANCELLED + " LT 99999");
		argt.dispose();
	}

	private PostProcessGuardArgt withoutGui(IContainerContext context) throws OException {
		PostProcessGuardArgt fromCR = new PostProcessGuardArgt (false, retryCount, timespan, retryCountIgnore);
		Table opsList = getDefaultOpsList();		
		for (int row=opsList.getNumRows(); row >= 1; row--) {
			String opsName = opsList.getString(1, row);
			Table queryList = getDefaultQueryList(opsName);
			String queryName = queryList.getString(1, 1);
			fromCR.addItem(opsName, queryName);
			queryList.destroy();
		}		
		opsList.destroy();
		return fromCR;
	}

	private PostProcessGuardArgt withGui(IContainerContext context) throws OException {
		int ret;
		Table opsDefList = getOpsDefList();
		Table defaultOpsList = getDefaultOpsList ();
		Table savedQueryList = getSavedQueryList();
		Table ask1 = Table.tableNew("Ask window");
		ret = Ask.setAvsTable(ask1, opsDefList, "Select OPS definitions to check", 
				1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, defaultOpsList, 
				"Select one or more OPS those pending post processes are to rerun");
		ret = Ask.setTextEdit(ask1, "Retry count",  "" + retryCount, ASK_TEXT_DATA_TYPES.ASK_INT);
		ret = Ask.setTextEdit(ask1, "Timespan(s) per try", "" + timespan, ASK_TEXT_DATA_TYPES.ASK_INT);
		ret = Ask.setTextEdit(ask1, "Ignore items having run in reporting", "" + retryCountIgnore, ASK_TEXT_DATA_TYPES.ASK_INT);
		ret = Ask.viewTable(ask1, "Select the OPS to process", 
				"Select the OPS definitions those pending post processes are supposed to rerun");
		if (ret == 0) { // user cancelled
			PostProcessGuardArgt newArgt = new PostProcessGuardArgt(true, 0, 2, 0);
			return newArgt;
		}
		Table selectedOps = ask1.getTable("return_value", 1);
		selectedOps.setColName(1, PostProcessGuardArgt.ITEM_LIST_OPS_DEF_NAME);
		Table retryCountTable = ask1.getTable("return_value", 2);
		retryCount = Integer.parseInt(retryCountTable.getString("return_value", 1));
		Table timespanTable = ask1.getTable("return_value", 3);
		timespan = Integer.parseInt(timespanTable.getString("return_value", 1));
		Table retryCountIgnoreTable = ask1.getTable("return_value", 4);
		retryCountIgnore = Integer.parseInt(retryCountIgnoreTable.getString("return_value", 1));
		
		Table ask2 = Table.tableNew("Ask window 2");
		for (int row=selectedOps.getNumRows(); row >= 1; row--) {
			String opsName = selectedOps.getString(PostProcessGuardArgt.ITEM_LIST_OPS_DEF_NAME, row);
			Table defaultQueryList = getDefaultQueryList(opsName);
			ret = Ask.setAvsTable(ask2, savedQueryList.copyTable(), "Select saved query for " + opsName,
					1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, defaultQueryList,
					"Select one or more OPS those pending post processes are to rerun");
		}
		ret = Ask.viewTable(ask2, "Select the additional queries",
				"Select the additional queries for the selected OPS");
		if (ret == 0) { // user cancelled
			PostProcessGuardArgt newArgt = new PostProcessGuardArgt(true, 0, 2, 0);
			return newArgt;
		}
		PostProcessGuardArgt newArgt = new PostProcessGuardArgt(false, retryCount, timespan, retryCountIgnore); 
		for (int row=selectedOps.getNumRows(); row >= 1; row--) {
			Table selectedOpsAndQueries = ask2.getTable("return_value", row);
			String opsName = selectedOps.getString(PostProcessGuardArgt.ITEM_LIST_OPS_DEF_NAME, row);
			String queryName = selectedOpsAndQueries.getString(1, 1);
			newArgt.addItem(opsName, queryName);
		}
		savedQueryList.destroy();
		ask1.destroy();
		ask2.destroy();
		return newArgt;
	}

	private Table getDefaultQueryList(String opsName) throws OException {
		Table defQueryList = Table.tableNew("default query list");
		defQueryList.addCol(PostProcessGuardArgt.ITEM_LIST_ADDITIONAL_QUERY, COL_TYPE_ENUM.COL_STRING);
		String opslist[] = defaultOPS.split(",");
		String querylist[] = additionalQuery.split(",");
		for (int i=0; i < opslist.length; i++) {
			String opsNameList = opslist[i].trim();
			if (opsName.equals(opsNameList)) {
				//if (querylist[i].trim().length() > 0) {
				if (i <= querylist.length-1 && querylist[i].trim().length() > 0) {
					int row = defQueryList.addRow();
					defQueryList.setString(0, row,  querylist[i].trim());
				}
				break;
			}
		}
		if (defQueryList.getNumRows() == 0) {
			defQueryList.addRow();
			defQueryList.setString(1, 1, "None");
		}
		return defQueryList;		
	}

	private Table getSavedQueryList() throws OException {
		Table queryList = Table.tableNew("query list");
		String sql = 
				"\nSELECT node_name FROM dir_node WHERE node_type = 3"; // 3 = Query

		int ret = DBaseTable.execISql(queryList, sql);
		int row = queryList.addRow();
		queryList.setString(1, row, "None");
		return queryList;
	}

	private Table getDefaultOpsList() throws OException {
		Table defOps = Table.tableNew("default ops list");
		defOps.addCol(PostProcessGuardArgt.ITEM_LIST_OPS_DEF_NAME, COL_TYPE_ENUM.COL_STRING);
		String opslist[] = defaultOPS.split(",");
		for (String ops : opslist) {
			String opsName = ops.trim();
			if (opsName.length() > 0) {
				int row = defOps.addRow();
				defOps.setString(1, row,  opsName);				
			}
		}
		return defOps;
	}

	private Table getOpsDefList() throws OException {
		Table opsDefList = Table.tableNew("Ops def list");
		int today = OCalendar.today();
		
		// select all pending jobs for 
		// - the requested op service 
		// - of today (= tomorrow midnight) or before
		String sql = "SELECT DISTINCT r.defn_name"
				+ "\n FROM op_services_log_detail d"
				+ "\n    , op_services_log l"
				+ "\n    , rsk_exposure_defn r"
				+ "\n    , dir_node dir "
				+ "\n    , ops_service_type t"
				+ "\n WHERE l.op_services_run_id = d.op_services_run_id"
				+ "\n   AND d.defn_id = r.exp_defn_id "
				+ "\n   AND d.script_id = dir.client_data_id"
				+ "\n   AND dir.node_type = 7"
				;
		
		int ret = DBaseTable.execISql(opsDefList, sql);
		return opsDefList;
	}

	private void init() throws OException {
		ConstRepository constRepo = new ConstRepository ("Ops", "PostProcessGuard");
		String debugLevel = constRepo.getStringValue("logLevel", "Info");
		String logFile    = constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log");
		String logDir    = constRepo.getStringValue("logDir", Util.getEnv("AB_OUTDIR") + "\\error_logs");
		
		defaultOPS = constRepo.getStringValue("opsNames", "Sent to JDE Stamp");
		additionalQuery = constRepo.getStringValue("savedQueries", "");
		retryCount = constRepo.getIntValue("retryCount", 2);
		timespan = constRepo.getIntValue("timespan", 30);
		retryCountIgnore = constRepo.getIntValue("retryCountIgnore", 4);
		
		try
		{
			Logging.init(this.getClass(), "Ops", "PostProcessGuard");
		}
		catch (Exception e)
		{
			OConsole.oprint(e.getMessage());
		}
	}
}
