package com.jm.eod.mature;

import standard.include.JVS_INC_Standard;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.jm.eod.common.Utils;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks, CSV report output & formatting changes
 */

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JM_MatureTradesByQid implements IScript {

	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "MatureTradesByQid";
	private ConstRepository constRep = null;
	private JVS_INC_Standard m_INCStandard;
	
	public JM_MatureTradesByQid() {
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException {
		String sFileName = "JM_MatureTradesByQid";
		String sReportTitle = "Mature Trades By Query";
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);
		
		try {
			init();
		} catch (Exception e) {
			Logging.error("Error initilising class, " + e.getMessage());
		}

		int  loop, numDeals, intRetVal, numInst, ins_type, exit_fail;
		String err_msg;
		Table tradesToMature = Util.NULL_TABLE;
		Table tOutput = Util.NULL_TABLE, temp_table, tDistInst;

		try {
			Logging.info("*** Start of " +  this.getClass().getName() + " script ***");
			
			//Table argt = context.getArgumentsTable();
			//m_INCStandard.STD_InitRptMgrConfig(error_log_file,argt);
			
			exit_fail = 0;
			err_msg = "";

			tOutput = Table.tableNew();
			tOutput.addCol( "tran_num",    COL_TYPE_ENUM.COL_INT);
			tOutput.addCol( "version_num", COL_TYPE_ENUM.COL_INT);
			tOutput.addCol( "ins_type",    COL_TYPE_ENUM.COL_INT);
			
			try {
				tradesToMature = loadTrades();
			} catch (OException oex) {
				Logging.error("Error in running database query, " + oex.getMessage());
			}
			
			numDeals = tradesToMature.getNumRows();
			for (loop = 1; loop <= numDeals; loop++) {
				int tranNum = tradesToMature.getInt( "tran_num", loop);
				int versionNum = tradesToMature.getInt( "version_number", loop);
				int insType = tradesToMature.getInt( "ins_type", loop);
				
				Logging.debug("About to matured trade tran num: " + tranNum + " VersionNum: " + versionNum + " InsType: " + insType);
				intRetVal = Transaction.mature(tranNum, versionNum);
				if (intRetVal == 1) {
					tradesToMature.copyRowAdd( loop, tOutput);
					Logging.info("Matured trade tran num: " + tranNum );
				} else {
					Logging.error("Matured trade API returned with error - tran num: " + tranNum + " VersionNum: " + versionNum + " InsType: " + insType);
				}
			}
			tradesToMature.clearRows();

			if (tOutput.getNumRows() <= 0) {
				Logging.info("Number of deals matured = 0" );
			} else {
				Logging.info("Number of deals matured = " + tOutput.getNumRows() );
				tDistInst = Table.tableNew();
				temp_table = Table.tableNew();
				
				try {
					tDistInst.select( tOutput, "DISTINCT, ins_type", "ins_type GT 0");
					numInst = tDistInst.getNumRows();
					for (loop = 1; loop <= numInst; loop++) {
						ins_type = tDistInst.getInt( "ins_type", loop);
						temp_table.select( tOutput, "tran_num", "ins_type EQ " + ins_type);
						
						Logging.info( "Number of deals matured for instrument type " + Table.formatRefInt(ins_type, SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE) + " = " + temp_table.getNumRows());
						temp_table.clearRows();
					}
				} finally {
					if (Table.isTableValid(tDistInst) == 1) {
						tDistInst.destroy();
					}
					if (Table.isTableValid(temp_table) == 1) {
						temp_table.destroy();
					}
				}
			}

			tOutput.setColFormatAsRef( "ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
			tOutput.formatSetTableWidth( 30);
			tOutput.groupFormatted( "ins_type, tran_num, version_num");

			/*** Dump to CSV ***/
			tOutput.printTableDumpToFile(sFileName + ".csv");

			tOutput.setColTitle( "tran_num", "Tran Number");
			tOutput.setColTitle( "version_num", "Version");
			tOutput.groupFormatted( "ins_type, tran_num, version_num");
			tOutput.groupTitleAbove( "ins_type");
			tOutput.colHide( "ins_type");

			/*** Create Report Viewer EXC File ***/
			if (m_INCStandard.report_viewer != 0) {
				m_INCStandard.STD_PrintTextReport(tOutput, sFileName + ".exc", sReportTitle, sReportTitle + "\nCurrent Date: " + OCalendar.formatDateInt(OCalendar.today()), error_log_file);
			}

			Logging.info(  "*** End of " + sFileName + " script ***");
			if (exit_fail != 0) {
				Logging.error(err_msg);
				throw new OException( err_msg );
			}
			return;
			
		} finally {
			Logging.close();
			if (Table.isTableValid(tradesToMature) == 1) {
				tradesToMature.destroy();
			}
			if (Table.isTableValid(tOutput) == 1) {
				tOutput.destroy();
			}
		}
	}
	
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(Utils.class, CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
	
    private int getQry(String qryName) throws OException {
		int qid = Query.run(qryName);
		if (qid < 1){
			String msg = "Run Query failed: " + qryName;
			throw new OException(msg);
		}
		return qid;
    }
	
	private Table loadTrades() throws ConstantTypeException, ConstantNameException, OException {
		String queryName = "MatureTrades";
		queryName = constRep.getStringValue("MatureTradesQuery", queryName);
		
		if (queryName == null || queryName.length() == 0) {
			String errorMessage = "Invalid query name, check the const repository MatureTradesQuery entry";
			Logging.error(errorMessage);
			throw new OException(errorMessage);
		} else {
			String errorMessage = "Using query: " + queryName;
			Logging.debug(errorMessage);
		}
		
		String today = OCalendar.formatJdForDbAccess(OCalendar.today());
		Table resultTable = Table.tableNew();
		int qid = getQry(queryName);
		
		String what = " SELECT Distinct ab.tran_num, ab.version_number, ab.ins_type \n";
		String from = " FROM ab_tran ab, ab_tran_event abe, " + Query.getResultTableForId(qid) + " qr \n";
		String where = " WHERE ab.tran_num = abe.tran_num \n " +
				       " AND ab.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + " \n" +
				       " AND abe.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CLOSE.toInt()  + " \n" +
				       " AND abe.event_date <= '" + today + "' \n" +
				       " AND qr.unique_id = " + qid + "\n" +
		               " AND ab.tran_num = qr.query_result\n";
		try {
			String sql = what + " " + from + " " + where;
			Logging.debug("About to run sql:\n " + sql);
			DBaseTable.execISql(resultTable, sql);
			
		} catch (Exception e) {
			String errorMessage = "Error loading the trades to mature. " + e.getMessage();
			Logging.error(errorMessage);			
			throw e;
		} finally {
			if (qid > 0) {
				Query.clear(qid);				
			}
		}

		String errorMessage = "Found no deals to action: " + resultTable.getNumRows();
		Logging.info(errorMessage);
		return resultTable;
	}

}
