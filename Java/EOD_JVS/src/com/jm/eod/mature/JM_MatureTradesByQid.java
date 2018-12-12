package com.jm.eod.mature;


import standard.include.JVS_INC_Standard;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Query;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.openlink.util.logging.PluginLog;


@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class JM_MatureTradesByQid implements IScript {

	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "MatureTradesByQid";
	 
	
	private ConstRepository constRep = null;
	
	private JVS_INC_Standard m_INCStandard;
	
	public JM_MatureTradesByQid(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException {
		String sFileName = "JM_MatureTradesByQid";
		String sReportTitle = "Mature Trades By Query";
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);
		
		 
         
        
		try {
			init();
		} catch (Exception e) {
			PluginLog.error("Error initilising class, " + e.getMessage());
		}

		int  loop, numDeals, intRetVal, numInst, ins_type, exit_fail;
		String err_msg;
		Table tOutput, temp, temp_table, tDistInst;

		PluginLog.info("*** Start of " +  this.getClass().getName() + " script ***");
		
		
		Table argt = context.getArgumentsTable();
		m_INCStandard.STD_InitRptMgrConfig(error_log_file,argt);
		
		exit_fail = 0;
		err_msg = "";



		tOutput = Table.tableNew();
		tOutput.addCol( "tran_num",    COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "version_num", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "ins_type",    COL_TYPE_ENUM.COL_INT);

		Table tradesToMature = null;
		try{
			tradesToMature = loadTrades();
		} catch( OException oex ){
			PluginLog.error("OException - unsuccessful database query, " + oex.getMessage());
		}
		numDeals = tradesToMature.getNumRows();

		for(loop = 1; loop <= numDeals; loop++) {
			int tranNum = tradesToMature.getInt( "tran_num", loop);
			int versionNum = tradesToMature.getInt( "version_number", loop);
			int insType = tradesToMature.getInt( "ins_type", loop);
			PluginLog.debug("About to matured trade tran num: " + tranNum + " VersionNum: " + versionNum + " InsType: " + insType);
			intRetVal = Transaction.mature(tranNum, versionNum);
			if(intRetVal == 1) {
				tradesToMature.copyRowAdd( loop, tOutput);
				PluginLog.info("Matured trade tran num: " + tranNum );
			} else {
				PluginLog.error("Matured trade API returned with error - tran num: " + tranNum + " VersionNum: " + versionNum + " InsType: " + insType);
				
			}
		}
		tradesToMature.clearRows();
		tradesToMature.destroy();

		if(tOutput.getNumRows() <= 0) {
			PluginLog.info("Number of deals matured = 0" );
		} else {
			PluginLog.info("Number of deals matured = " + tOutput.getNumRows() );
			tDistInst = Table.tableNew();
			tDistInst.select( tOutput, "DISTINCT, ins_type", "ins_type GT 0");
			numInst = tDistInst.getNumRows();
			temp_table = Table.tableNew();
			for(loop = 1; loop <= numInst; loop++) {
				ins_type = tDistInst.getInt( "ins_type", loop);
				temp_table.select( tOutput, "tran_num", "ins_type EQ " + ins_type);
				PluginLog.info( "Number of deals matured for instrument type " + Table.formatRefInt(ins_type, SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE) + " = " + temp_table.getNumRows());
				temp_table.clearRows();
			}
			tDistInst.destroy();
			temp_table.destroy();
		}


		tOutput.setColFormatAsRef( "ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		tOutput.formatSetTableWidth( 30);
		tOutput.groupFormatted( "ins_type, tran_num, version_num");



		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0){
			m_INCStandard.STD_PrintTableDumpToFile(tOutput, sFileName, sReportTitle, error_log_file);
		}

		/*** Create USER Table ***/
		if(m_INCStandard.user_table != 0){
			temp = Table.tableNew();
			temp.select( tOutput, "*", "tran_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_EOD_Mature_Trades_Ins", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				err_msg += "Error value returned from m_INCStandard.STD_SaveUserTable()\n";
				exit_fail = 1;
			}
			temp.destroy();
		}

		tOutput.setColTitle( "tran_num", "Tran Number");
		tOutput.setColTitle( "version_num", "Version");
		tOutput.groupFormatted( "ins_type, tran_num, version_num");
		tOutput.groupTitleAbove( "ins_type");
		tOutput.colHide( "ins_type");

		/*** View Table ***/
		if(m_INCStandard.view_table != 0){
			m_INCStandard.STD_ViewTable(tOutput, sReportTitle, error_log_file);
		}


		/*** Create Report Viewer EXC File ***/
		if(m_INCStandard.report_viewer != 0){
			m_INCStandard.STD_PrintTextReport(tOutput, sFileName + ".exc", sReportTitle, sReportTitle + "\nCurrent Date: " + OCalendar.formatDateInt(OCalendar.today()), error_log_file);
		}

		tOutput.destroy();

		PluginLog.info(  "*** End of " + sFileName + " script ***");

		if(exit_fail != 0){
			PluginLog.error(err_msg);
			throw new OException( err_msg );
		}
		return;
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

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
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
		
		if(queryName == null || queryName.length() == 0) {
			String errorMessage = "Invalid query name, check the const repository MatureTradesQuery entry";
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		} else {
			String errorMessage = "Using query: " + queryName;
			PluginLog.debug(errorMessage);
		}
		
		String today = OCalendar.formatJdForDbAccess(OCalendar.today());

		Table resultTable = Table.tableNew();
		
		String what = " SELECT Distinct ab.tran_num, ab.version_number, ab.ins_type \n";
		
		int qid = getQry(queryName);
		String from = " FROM ab_tran ab, ab_tran_event abe, " + Query.getResultTableForId(qid) + " qr \n";
		
		String where = " WHERE ab.tran_num = abe.tran_num \n " +
				       " AND ab.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + " \n" +
				       " AND abe.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CLOSE.toInt()  + " \n" +
				       " AND abe.event_date <= '" + today + "' \n" +
				       " AND qr.unique_id = " + qid + "\n" +
		               " AND ab.tran_num = qr.query_result\n";
		

		try {
			String sql = what + " " + from + " " + where;
			PluginLog.debug("About to run sql:\n " + sql);
			DBaseTable.execISql(resultTable, sql);
		} catch (Exception e) {
			String errorMessage = "Error loading the trades to mature. " + e.getMessage();
			PluginLog.error(errorMessage);			
			throw e;
		} finally {
			Query.clear(qid);
		}

		String errorMessage = "Found no deals to action: " + resultTable.getNumRows();
		PluginLog.debug(errorMessage);
		
		return resultTable;
				
		
	}

}
