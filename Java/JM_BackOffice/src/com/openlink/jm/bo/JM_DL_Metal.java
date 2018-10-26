package com.openlink.jm.bo;

/**
 * Project:  D377 - JM-Invoices
 * 
 * Description:
 * - This script loads Invoice related additional data for Metal into the event table (BO blotter).
 * 
 * Revision History:
 *  13.11.15  jbonetzk  initial version
 *  10.05.16  jwaechter fixed data retrieval and join in first SQL
 *                      added retrieval of tran_unit from volume unit of the leg of the event  for certain toolsets
 *  16.09.16  jneufert  change the retrieval of Tax Type and Tax Sub Type from tran level to event level
 *  14.05.18  sma       For metal account remove ins_para_seq_num from where match criteria
 */


import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_DL_Metal implements IScript {
	
	final String ACCT_CLASS_METAL = "Metal Account"; // TODO ask ConstRepo

	protected final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue();

	// default log level - optionally overridden by ContRepo value
	private final String defaultLogLevel = "warn";

	private final String
	  CONST_REPO_CONTEXT        = "BackOffice"
	, CONST_REPO_SUBCONTEXT     = "Dataload Metal"
	, CONST_REPO_VAR_LOGLEVEL   = "logLevel"
	, CONST_REPO_VAR_LOGFILE    = "logFile"
	, CONST_REPO_VAR_LOGDIR     = "logDir"
	, CONST_REPO_VAR_VIEWTABLES = "viewTablesInDebugMode"
	, ARGT_COL_NAME_TRAN_UNIT   = "tran_unit"
	, ARGT_COL_NAME_DEAL_UNIT   = "deal_unit"
	, ARGT_COL_NAME_EXT_METAL_ACCOUNT  = "ext_metal_account"
	, ARGT_COL_NAME_INT_METAL_ACCOUNT  = "int_metal_account"
	, ARGT_COL_TITLE_EXT_METAL_ACCOUNT = "CP Metal\nAccount"
	, ARGT_COL_TITLE_DEAL_UNIT   = "Deal\nUnit"
	, ARGT_COL_TITLE_INT_METAL_ACCOUNT = "Our Metal\nAccount"
	, ARGT_COL_NAME_TAX_TYPE     = "tax_tran_type"
	, ARGT_COL_NAME_TAX_SUBTYPE  = "tax_tran_subtype"
	, ARGT_COL_TITLE_TAX_TYPE     = "Tax Type"
	, ARGT_COL_TITLE_TAX_SUBTYPE  = "Tax Subtype"
	, ARGT_COL_NAME_FX_RATE       = "event_info_type_20005"
	, ARGT_COL_NAME_APPLY_EXT_FX_RATE = "stldoc_info_type_20002"
	, TRAN_INFO_JM_FX_RATE_NAME       = "JM FX Rate"
	;

	// frequently used constants:
	ConstRepository _constRepo = null;
	boolean _viewTables = false;
	String _logLevel = "info"
	     , _logFile = getClass().getSimpleName()+".log"
	     , _logDir = null
	     ;

	public void execute(IContainerContext context) throws OException {
		
		// measure execution time
		long start = System.currentTimeMillis();

		// repository
		_constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		// logging
		tryRetrieveLogSettingsFromConstRepo(defaultLogLevel, null, getClass().getSimpleName() + ".log");
		try { 
			PluginLog.init(_logLevel, _logDir, _logFile); 
		} catch (Exception e) { 
			OConsole.oprint(e.getMessage()); 
		}

		// main process
		try { 
			process(context); 
		} catch (Exception e) { 
			PluginLog.error("Exception: " + e.getMessage()); PluginLog.exitWithStatus(); 
		} finally { 
			PluginLog.info("Done in " + (System.currentTimeMillis()-start) + " ms"); 
		}
	}

	boolean tryRetrieveLogSettingsFromConstRepo(String logLevel, String logDir, String logFile) {
		
		try {
			boolean viewTablesInDebugMode = false;
			if (_constRepo != null) {
				logLevel = _constRepo.getStringValue(CONST_REPO_VAR_LOGLEVEL, logLevel);
				logFile  = _constRepo.getStringValue(CONST_REPO_VAR_LOGFILE, logFile);
				logDir   = _constRepo.getStringValue(CONST_REPO_VAR_LOGDIR, logDir);
				viewTablesInDebugMode = _constRepo.getStringValue(CONST_REPO_VAR_VIEWTABLES, "no").equalsIgnoreCase("yes");
			}
			_logLevel = logLevel;
			_logFile  = logFile;
			_logDir   = logDir;
			_viewTables = viewTablesInDebugMode && PluginLog.LogLevel.DEBUG.equalsIgnoreCase(_logLevel);
			return true;
		} catch (Exception e) { 
			return false; 
		}
	}

	private void process(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();

		final COL_TYPE_ENUM COL_TYPE_STRING = COL_TYPE_ENUM.COL_STRING;
		argt.addCol(ARGT_COL_NAME_INT_METAL_ACCOUNT, COL_TYPE_STRING, ARGT_COL_TITLE_INT_METAL_ACCOUNT);
		argt.addCol(ARGT_COL_NAME_EXT_METAL_ACCOUNT, COL_TYPE_STRING, ARGT_COL_TITLE_EXT_METAL_ACCOUNT);

		argt.addCol(ARGT_COL_NAME_TAX_TYPE, ARGT_COL_TITLE_TAX_TYPE, SHM_USR_TABLES_ENUM.TAX_TRAN_TYPE_TABLE);
		argt.addCol(ARGT_COL_NAME_TAX_SUBTYPE, ARGT_COL_TITLE_TAX_SUBTYPE, SHM_USR_TABLES_ENUM.TAX_TRAN_SUBTYPE_TABLE);
		argt.addCol(ARGT_COL_NAME_DEAL_UNIT, ARGT_COL_TITLE_DEAL_UNIT, SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

		int numRowsArgt = argt.getNumRows();
		for (int row=numRowsArgt; row >= 1; row--) {
			int tranUnit = argt.getInt(ARGT_COL_NAME_TRAN_UNIT, row);
			argt.setInt(ARGT_COL_NAME_DEAL_UNIT, row, tranUnit);
		}
		
		if (numRowsArgt > 0) {
			
			int acm = Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE, ACCT_CLASS_METAL);
			int qid = Query.tableQueryInsert(argt, "tran_num");
			String sql, qtbl = Query.getResultTableForId(qid);
			Table tbl;

			sql = "select distinct ate.tran_num"
				+ ", acc1.account_number "+ARGT_COL_NAME_INT_METAL_ACCOUNT
				+ ", acc2.account_number "+ARGT_COL_NAME_EXT_METAL_ACCOUNT
				+ " from ab_tran_event ate"
				+ " join "+qtbl+" qr on ate.tran_num=qr.query_result and qr.unique_id="+qid
				+ " join ab_tran_event_settle ates on ate.event_num=ates.event_num"
				+ " left join account acc1 on acc1.account_id=ates.int_account_id and acc1.account_status=1 and acc1.account_class="+acm
				+ " left join account acc2 on acc2.account_id=ates.ext_account_id and acc2.account_status=1 and acc2.account_class="+acm
				+ " join parameter p_same on p_same.ins_num = ate.ins_num and ate.ins_para_seq_num = p_same.param_seq_num"
				+ " join parameter p_all on p_all.ins_num = ate.ins_num and p_all.param_group = p_same.param_group"
				+ " where ate.event_type in (14,98)"
				+ " and ate.unit<>0"
				;
			tbl = Table.tableNew("queried");
			DBaseTable.execISql(tbl, sql);
			if (tbl.getNumRows() > 0) {
//				argt.select(tbl, ARGT_COL_NAME_INT_METAL_ACCOUNT+","+ARGT_COL_NAME_EXT_METAL_ACCOUNT, 
//						"tran_num EQ $tran_num AND ins_para_seq_num EQ $ins_para_seq_num");
				//For metal account remove ins_para_seq_num from where match criteria 
				argt.select(tbl, ARGT_COL_NAME_INT_METAL_ACCOUNT+","+ARGT_COL_NAME_EXT_METAL_ACCOUNT, 
						"tran_num EQ $tran_num");
			}
				
			tbl.destroy();

			sql = "select distinct att.tran_num"			
				+ ",att.tax_tran_type "   +ARGT_COL_NAME_TAX_TYPE
				+ ",att.tax_tran_subtype "+ARGT_COL_NAME_TAX_SUBTYPE
				+ ",att.tranf_group,att.param_seq_num,att.seq_num_2"
				+ " from (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 1 param_seq_num, att.seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and at.ins_type = 26001 and att.tranf_group = 1 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type = 14"		//FX Cash Settlement events
				+ " union"
				+ " (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 1 param_seq_num, 0 seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and at.ins_type = 26001 and att.tranf_group = 1 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type = 98)"		//FX Tax Settlement events 
				+ " union"
				+ " (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 1 param_seq_num, 1 seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and at.ins_type = 26001 and att.tranf_group = 1 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type = 98)"		//FX Tax Settlement events 
				+ " union"
				+ " (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, att.seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and at.ins_type = 27001 and att.tranf_group = 1 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type = 14)"		//CASH: Cash Settlement events
				+ " union"				
				+ " (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, 0 seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and at.ins_type = 27001 and att.tranf_group = 1 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type = 98)"		//CASH: Tax Settlement events
				+ " union"				
				+ " (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, 1 seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and at.ins_type = 27001 and att.tranf_group = 1 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type = 98)"		//CASH: Tax Settlement events
				+ " union"				
				+ " (select att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, att.seq_num_2, at.ins_type, ate.event_num"				
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate  where att.tran_num = at.tran_num and tranf_group = 3 "	
				+ " 				and at.tran_num = ate.tran_num and ate.event_type in (14, 98))"		//other: Cash and Tax Settlement events
				+ " union"
				+ " (select distinct att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, param_seq_num, att.seq_num_2, at.ins_type, ate.event_num"
				+ " from ab_tran_tax att, ab_tran at, ab_tran_event ate where att.tran_num = at.tran_num and tranf_group = 16 "
				+ " 				and at.tran_num = ate.tran_num and ate.event_type in (14, 98))"		//COMM-PHYS: Cash and Tax Settlement events
				+ " 				) att"
				+ " join "+qtbl+" qr on att.tran_num=qr.query_result and qr.unique_id="+qid

				;
			tbl = Table.tableNew("queried");
			DBaseTable.execISql(tbl, sql);
			
			//tbl.viewTable();
			
			if (tbl.getNumRows() > 0) {
				
				argt.select(tbl, ARGT_COL_NAME_TAX_TYPE, ARGT_COL_NAME_TAX_SUBTYPE+" EQ -1 AND tran_num EQ $tran_num AND param_seq_num EQ $ins_para_seq_num AND seq_num_2 EQ $ins_seq_num");
				argt.select(tbl, ARGT_COL_NAME_TAX_SUBTYPE, ARGT_COL_NAME_TAX_TYPE+" EQ -1 AND tran_num EQ $tran_num AND param_seq_num EQ $ins_para_seq_num AND seq_num_2 EQ $ins_seq_num");
//				argt.select(tbl, ARGT_COL_NAME_TAX_TYPE, ARGT_COL_NAME_TAX_SUBTYPE+" EQ -1 AND tran_num EQ $tran_num AND param_seq_num EQ $ins_para_seq_num");
//				argt.select(tbl, ARGT_COL_NAME_TAX_SUBTYPE, ARGT_COL_NAME_TAX_TYPE+" EQ -1 AND tran_num EQ $tran_num AND param_seq_num EQ $ins_para_seq_num");
//				argt.select(tbl, ARGT_COL_NAME_TAX_TYPE, ARGT_COL_NAME_TAX_SUBTYPE+" EQ -1 AND tran_num EQ $tran_num");
//				argt.select(tbl, ARGT_COL_NAME_TAX_SUBTYPE, ARGT_COL_NAME_TAX_TYPE+" EQ -1 AND tran_num EQ $tran_num");

			}
			tbl.destroy();
			
			sql =  " select tran_num, value as " + ARGT_COL_NAME_FX_RATE + ", 'Yes' as  " + ARGT_COL_NAME_APPLY_EXT_FX_RATE + " from ab_tran_info_view "
				  + " join "+qtbl+" qr on tran_num=qr.query_result and qr.unique_id="+qid 
				  + " where type_name = '" + TRAN_INFO_JM_FX_RATE_NAME + "'";
			tbl = Table.tableNew("queried");
			DBaseTable.execISql(tbl, sql);
			if (tbl.getNumRows() > 0) {
				
				argt.select(tbl, ARGT_COL_NAME_FX_RATE + ", " + ARGT_COL_NAME_APPLY_EXT_FX_RATE, "tran_num EQ $tran_num");
			}
			tbl.destroy();			

			sql =  "\nSELECT abe.event_num, p.unit " + ARGT_COL_NAME_DEAL_UNIT
				+  "\nFROM " + qtbl + " qr"
				+  "\n  INNER JOIN ab_tran_event abe"
				+  "\n     ON abe.tran_num = qr.query_result"
				+  "\n  INNER JOIN ab_tran ab ON ab.tran_num = qr.query_result"
				+  "\n    AND ab.toolset IN (SELECT t.id_number FROM toolsets t WHERE t.name IN ('MetalSwap', 'ComSwap'))"
				+  "\n  INNER JOIN parameter p ON p.ins_num = abe.ins_num AND p.param_seq_num = abe.ins_para_seq_num"
				+  "\nWHERE qr.unique_id  = " + qid 
					;
			tbl = Table.tableNew("queried");
			DBaseTable.execISql(tbl, sql);
			if (tbl.getNumRows() > 0) {
				
				argt.select(tbl, ARGT_COL_NAME_DEAL_UNIT, "event_num EQ $event_num");
			}
			tbl.destroy();
			
			Query.clear(qid);
		}
	}
}
