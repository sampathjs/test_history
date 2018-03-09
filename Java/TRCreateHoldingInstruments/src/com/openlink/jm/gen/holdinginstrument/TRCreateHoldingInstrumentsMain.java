package com.openlink.jm.gen.holdinginstrument;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Debug;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.market.EnumIdxGroup;
import com.olf.openrisk.market.Index;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumSettleType;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranType;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-01-26 - V0.1 - jwaechter - Initial Version
 * 2015-01-28 - V1.0 - jwaechter - Release version
 *                               - added comments
 * 2015-02-05 - V1.1 - jwachter  - added more info level logging
 */

/**
 * Based on AVS script TR_CreateFutureHoldingIns_P.mls
 * Description: <br/>
 * This script creates holdings instruments based on any existing holding instrument
 * already present in the database.
 * It base itself on the instrument market curve to retrieve the contract dates.
 * <br/> <br/>  
 * This script is designed to run as part of the EOD / EOM.
 * <br/>  <br/> 
 * Assumptions: There is already a few holding instruments in the database to replicate.
 * @author jwaechter
 * @version 1.1
 * @see {@link TRCreateHoldingInstrumentsParam}
 */
@ScriptCategory( { EnumScriptCategory.Generic })
public class TRCreateHoldingInstrumentsMain extends AbstractGenericScript {	
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "CreateHoldingInstruments"; // subcontext of constants repository

	private Table errorTable=null;
	@SuppressWarnings("unused")
	private Debug debug = null;
	private TableFactory tf;
	private IOFactory iof;
	private StaticDataFactory sdf;
	private CalendarFactory cf;
	private Date tradingDate;
	private TradingFactory tdf;

	/**
	 * @param context
	 *            the current script context
	 * @param table
	 *            the input parameters
	 * @return a <code>Table</code> or null
	 */
	public Table execute(final Context context, final ConstTable argt) {
		tf = context.getTableFactory();
		iof = context.getIOFactory();
		sdf = context.getStaticDataFactory();
		cf = context.getCalendarFactory();
		tdf = context.getTradingFactory();
		debug = context.getDebug();
		tradingDate = context.getTradingDate();

		init (context);
		try {
			PluginLog.info(this.getClass().getSimpleName() + " started\n"); 
			if (checkArgt (argt)) {
				Table latestHoldIns = getLatestHoldingIns ();
				if (latestHoldIns != null) {
					Table indexList = argt.getTable("index_list", 0);
					latestHoldIns.select(indexList, "index_id", "IN.index_name == OUT.index_name");
					removeIDZeroes (latestHoldIns, "index_id");
					latestHoldIns.sort("index_id", true);
				}
				if (latestHoldIns.getRowCount() < 1) {
					PluginLog.info("Cannot determine any indexes to process!");
				}
				for (int row=0; row < latestHoldIns.getRowCount(); row++) {
					Index index = null;
					Table underlying = null;
					Table gptDefs = null;
					String optDateSeq = latestHoldIns.getString("opt_date_seq", row);
					String indexName = latestHoldIns.getString("index_name", row);
					int insNum = latestHoldIns.getInt("ins_num", row);
					int insType = latestHoldIns.getInt("ins_type", row);
					int commodityIdxGroup = latestHoldIns.getInt("commodity_idx_group", row);
					int settlementType = latestHoldIns.getInt("settlement_type", row);
					int projIndex = latestHoldIns.getInt("proj_index", row);
					int toolset = latestHoldIns.getInt("toolset", row);
					@SuppressWarnings("unused")
					int ret;

					if (!doesHoldingInsExistForIndex (latestHoldIns, insType, projIndex)) {
						PluginLog.warn("No holding instrument exist for index " + indexName);
						continue;
					}
					if (toolset == EnumToolset.ComOptFut.getValue()) {
						underlying = getUnderlyingComFuts (projIndex);
					}

					index = context.getMarket().getIndex(projIndex);
					gptDefs = index.getGridPoints().asTable(); 

					for (int gptRow = gptDefs.getRowCount()-1; gptRow >= 0; gptRow-- ) {
						ret = createHoldInsForGridpoint (indexName, projIndex, insNum, insType, 
								toolset, optDateSeq, commodityIdxGroup, settlementType, gptDefs, 
								gptRow, underlying, argt, context.getMarket());
					}
					if (underlying != null) {
						underlying.dispose();
					}
				}
			}        
			PluginLog.info(this.getClass().getSimpleName() + " ended\n"); 
			return argt.asTable();
		} catch (RuntimeException ex) {
			PluginLog.error(ex.toString());
			throw ex;
		}
	}

	/**
	 * Creates a new holding instrument for a single grid point
	 * in case there is no already existing holding instrument.
	 * @param indexName
	 * @param projIndexId
	 * @param insNum
	 * @param insType
	 * @param toolset
	 * @param optDateSeq
	 * @param commodityIdxGroup
	 * @param settlementType
	 * @param gptDefs
	 * @param gptRow
	 * @param underlying
	 * @param argt
	 * @param market
	 * @return
	 */
	private int createHoldInsForGridpoint(String indexName, int projIndexId,
			int insNum, int insType, int toolset, String optDateSeq,
			int commodityIdxGroup, int settlementType, Table gptDefs,
			int gptRow, Table underlying, ConstTable argt, Market market) {
		PluginLog.info("createHoldInsForGridpoint start");
		int ret=0;
		
		PluginLog.info("Retrieving index and holiday schedule");
		Index projIndex = market.getIndex(projIndexId);
		SymbolicDate sd = cf.createSymbolicDate();    	
		sd.setHolidaySchedules(projIndex.getHolidaySchedules(true));
		PluginLog.info("Retrieved index and holiday schedule");
		
		int endDate = argt.getInt("end_date", 0);
		
		String group = gptDefs.getString("Label", gptRow);
		sd.setExpression(group);
		Date date = sd.evaluate(tradingDate);

		PluginLog.info("Retrieving Grid Point Data");		
		String curveDate = gptDefs.getDisplayString(gptDefs.getColumnId("Start Date"), gptRow);
		sd.setExpression(curveDate);
		Date defDate = sd.evaluate(tradingDate);
		PluginLog.info("Retrieved Grid Point Data");		

		String gStartDate = gptDefs.getString("Start Date Expr", gptRow);
		sd.setExpression(gStartDate);
		Date gptStartDate = sd.evaluate(tradingDate);

		String refOpt = indexName.substring(4) + "-" + formatDate (date, "MMMYY");
		String refFut = indexName + "_" + formatDate (date, "MMM-YY");

		errorTable.clearData();

		if (endDate > 0 && cf.getJulianDate(defDate) <= endDate) {
			String ref = (toolset == EnumToolset.ComOptFut.getValue())?(refOpt):(refFut);
			if (!doesHoldInsAlreadyExistForIndexAndRef(insType, projIndex, ref)) {
				PluginLog.info("Creating new Holding Instrument");
				Instrument copy = tdf.cloneInstrument(insNum);
				Transaction tran = copy.getTransaction();
				PluginLog.info("New Holding Instrument created");
				
				if (toolset == EnumToolset.ComOptFut.getValue()) {
					ret = setHoldInsFieldsComOptFut (tran, underlying, defDate, projIndex,
							indexName, optDateSeq, refFut, refOpt, group);
				} else {
					ret = setHoldInsFields (tran, commodityIdxGroup, settlementType,
							refFut, defDate, gptStartDate);
				}
				if (ret >= 0) {
					copy.process(EnumTranStatus.Validated);
					ret = tran.getTransactionId();
				} 
				String insName = sdf.getName(EnumReferenceTable.Instruments, insType);
				if (ret < 1) {
					PluginLog.error("Failed to create new " + insName + " ref " + ref
							+ " for index " + indexName);
				} else {
					PluginLog.info("Created " + insName + " # " + ret + " ref " + ref
							+ " for index" + indexName);
				}
			}
		}    	   
		PluginLog.info("createHoldInsForGridpoint end");
		return ret;
	}

	/**
	 * For a new holding transaction of toolset ComFut
	 * this methods sets up the tran fields.
	 * @param tran
	 * @param commodityIdxGroup
	 * @param settlementType
	 * @param refFut
	 * @param defDate
	 * @param gptStartDate
	 * @return
	 */
	private int setHoldInsFields(Transaction tran, int commodityIdxGroup,
			int settlementType, String refFut, Date defDate, Date gptStartDate) {
		tran.getLeg(0).setValue(EnumLegFieldId.MaturityDate, defDate);
		tran.getLeg(0).setValue(EnumLegFieldId.StartDate, defDate);
		if ((commodityIdxGroup == EnumIdxGroup.Freight.getValue() || 
				commodityIdxGroup == EnumIdxGroup.Crude.getValue()) &&
				settlementType == EnumSettleType.Cash.getValue()) {
			tran.getLeg(0).setValue(EnumLegFieldId.LastDeliveryDate, defDate);			
			tran.getLeg(0).setValue(EnumLegFieldId.FirstDeliveryDate, defDate);
		}	
		tran.setValue (EnumTransactionFieldId.ReferenceString, refFut);
		tran.setValue (EnumTransactionFieldId.Ticker, refFut);
		return 1;
	}

	/**
	 * For a new holding transaction of toolset ComOptFut
	 * this methods sets up the tran fields and the
	 * underlying transaction.
	 * @param tran
	 * @param underlying
	 * @param defDate
	 * @param projIndex
	 * @param indexName
	 * @param optDateSeq
	 * @param refFut
	 * @param refOpt
	 * @param group
	 * @return
	 */
	private int setHoldInsFieldsComOptFut(Transaction tran, Table underlying,
			Date defDate, Index projIndex, String indexName, String optDateSeq,
			String refFut, String refOpt, String group) {
		int ret = 1;
		PluginLog.info("Setting fields in new holding instrument for ComOptFut Toolset");

		SymbolicDate sd = cf.createSymbolicDate();
		sd.setHolidaySchedules(projIndex.getHolidaySchedules(true));
		sd.setExpression("-2(" + optDateSeq + ")");
		Date date = sd.evaluate(defDate);

		int row = findTicker (underlying, refFut);
		if (row >= 0) {
			int ulTranNum = underlying.getInt("tran_num", row);
			Transaction ulTran = tdf.retrieveTransactionById(ulTranNum);
			tran.getInstrument().setUnderlying(ulTran.getInstrument());

		} else {
			PluginLog.error("Cannot find underlying tran for " + group 
					+ " index " + indexName);
			ret = 0;
		}
		if (ret == 1) {
			tran.getLeg(0).setValue(EnumLegFieldId.ExpirationDate, date);
			tran.getLeg(0).setValue(EnumLegFieldId.FirstTradeDate, tradingDate);
			tran.getLeg(0).setValue(EnumLegFieldId.BeginExerciseDate, tradingDate);
		}
		PluginLog.info("fields in new holding instrument for ComOptFut Toolset are set");

		return ret;
	}

	/**
	 * Returns the row of the table underlying containing 
	 * so that this row has a ticker that equal to refFut
	 * (ignoring case)
	 * @param underlying
	 * @param refFut
	 * @return
	 */
	private int findTicker(Table underlying, String refFut) {
		for (int row = underlying.getRowCount()-1; row >= 0; row--) {
			String ticker = underlying.getString("ticker", row);
			if (ticker.equalsIgnoreCase(refFut)) {
				return row;
			}
		}
		return -1;
	}

	private boolean doesHoldInsAlreadyExistForIndexAndRef(int insType,
			Index projIndex, String ref) {
		Table matches = null;
		boolean ret = false;
		try {
			String sql = "\nSELECT\n"
				+ "\n ab.ins_num,\n"
				+ "\n ab.ins_type,\n"
				+ "\n ab.reference,\n"
				+ "\n pa.proj_index\n"               
				+ "\nFROM\n" 
				+ "\n ab_tran ab\n" 
				+ "\n INNER JOIN parameter pa ON (pa.ins_num = ab.ins_num AND pa.param_seq_num = 0)\n" 
				+ "\nWHERE\n"        
				+ "\n ab.tran_type = " + EnumTranType.Holding.getValue() + "\n" 
				+ "\n AND ab.tran_status = " + EnumTranStatus.Validated.getValue() + "\n" 
				+ "\n AND ab.ins_type = " + insType + "\n" 
				+ "\n AND ab.reference LIKE '%" + ref + "'\n" 
				+ "\n AND pa.proj_index = " + projIndex.getId()
				;
			matches = iof.runSQL(sql);
			if (matches == null || matches.getColumnCount() == 0) {
				ret = false;
				PluginLog.error("Cannot load holding instruments!");
			} else if (matches.getRowCount() > 0 && matches.getInt("ins_num", 0) > 0) {
				PluginLog.info("Holding instrument #" + matches.getInt("ins_num", 0) + " exists"
						+ " for instrument " + sdf.getName(EnumReferenceTable.InsType, insType));
				ret = true;
			}
		} finally {
			if (matches != null) {
				matches.dispose();
			}
		}
		return ret;

	}

	/**
	 * Converts a date to a string using the Java built in class 
	 * "{@link SimpleDateFormat}".
	 * @param date The date to be converted to a string
	 * @param pattern The pattern as described in SimpleDateFormat
	 * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">Oracle JavaDoc for SimpleDateFormat</a>
	 * @return Date formated according to pattern.
	 */
	private String formatDate(Date date, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(date);		
	}

	/**
	 * Get underlying COM-FUT holding instruments when booking COM-OPT deals
	 * @param projIndex index id
	 * @param iof used execute SQL
	 * @return table (tran_num, ins_num, ticker, proj_index)
	 */
	private Table getUnderlyingComFuts(int projIndex) {
		String sql = 
			"\nSELECT a.tran_num, a.ins_num, h.ticker, p.proj_index" 
			+	"\nFROM ab_tran a, parameter p, header h"
			+	"\nWHERE a.tran_type = " + EnumTranType.Holding.getValue()
			+   "\n  AND a.tran_status = " + EnumTranStatus.Validated.getValue()
			+   "\n  AND a.toolset = " + EnumToolset.ComFut.getValue()
			+   "\n  AND a.ins_num = p.ins_num"
			+   "\n  AND p.ins_num = h.ins_num"
			+   "\n  AND p.proj_index = " + projIndex
			+   "\nORDER BY h.ticker"
			;
		Table underlyingComFuts = iof.runSQL(sql);
		if (underlyingComFuts == null || underlyingComFuts.getColumnCount() == 0) {
			PluginLog.error("Cannot get underlying holding instruments");
		}
		return underlyingComFuts;
	}

	/**
	 * Checks if holding instrument exists for this index?
	 * @param latestHoldIns table containing holding instruments
	 * @param insType instrument type id
	 * @param projIndex index id
	 * @param tf 
	 * @return true if there is a holding instrument for projIndex, false if not 
	 */
	private boolean doesHoldingInsExistForIndex(Table latestHoldIns,
			int insType, int projIndex) {
		boolean ret=false;

		for (int row=latestHoldIns.getRowCount()-1; row >= 0; row--) {
			int pi = latestHoldIns.getInt("proj_index", row);
			int it = latestHoldIns.getInt("ins_type", row);

			if (pi == projIndex && it == insType) {
				ret = true;
				break;
			}
		}

		if (ret != true) {		
			PluginLog.warn("No holding instrument exists for " + 
					sdf.getName(EnumReferenceTable.InsType, insType) +
					" for index " + sdf.getName(EnumReferenceTable.Index, projIndex));
		}
		return ret;
	}


	/**
	 * Removes all rows that have a values of column colName are 0.
	 * Column colName has to be of type {@link EnumColType#Int}.
	 * @param latestHoldIns table that is to be processed
	 * @param colName name of the columns those values are supposed 
	 * to be checked for occurrences of 0
	 */
	private void removeIDZeroes(Table latestHoldIns, String colName) {
		for (int row = latestHoldIns.getRowCount()-1; row >= 0; row-- ) {
			if (latestHoldIns.getInt (colName, row) == 0) {
				latestHoldIns.removeRow(row);
			}
		}
	}

	/**
	 * Retrieves a table containing the holding instruments already in database.
	 * @return the {@link Table} containing details of the holding instruments
	 */
	private Table getLatestHoldingIns() {
		int indexDbStatusValidated = sdf.getId(EnumReferenceTable.IdxDbStatus, TRCreateHoldingInstrumentsParam.INDEX_DB_STATUS_VALIDATED);

		String sql = "\nSELECT "
			+ "\n    MAX (a.ins_num) ins_num, "
			+ "\n    a.ins_type, "
			+ "\n    a.toolset, "
			+ "\n    p.proj_index, "
			+ "\n    im.index_name, "
			+ "\n    im.idx_group AS commodity_idx_group, "
			+ "\n    im.opt_date_seq, "
			+ "\n    p.settlement_type, "
			+ "\n    rh.hol_id as hol_schedule, "
			+ "\n    rhi.currency as hol_cur, "
			+ "\n    CASE WHEN a.toolset = " + EnumToolset.ComFut.getValue() + " THEN 1 ELSE 2 END AS sort_order "
			+ "\n FROM "
			+ "\n    ab_tran a "
			+ "\n    JOIN parameter p ON a.ins_num = p.ins_num AND p.param_seq_num = 0 "
			+ "\n    JOIN idx_def im ON im.index_id = p.proj_index AND im.db_status = " + indexDbStatusValidated
			+ "\n    LEFT JOIN reset_holiday rh ON a.ins_num = rh.ins_num AND rh.param_seq_num = 0 AND hol_seq_num = 0 "
			+ "\n    LEFT JOIN idx_def rhi ON rh.hol_id = rhi.index_id AND rhi.db_status = " + indexDbStatusValidated
			+ "\n WHERE "
			+ "\n    a.toolset in (" + EnumToolset.ComFut.getValue() + ", " + EnumToolset.ComOptFut.getValue() + ") "
			+ "\n    AND a.tran_type = " + EnumTranType.Holding.getValue()
			+ "\n    AND a.tran_status = " + EnumTranStatus.Validated.getValue()
			+ "\n GROUP BY "
			+ "\n    p.proj_index, "
			+ "\n    a.ins_type, "
			+ "\n    a.toolset, "
			+ "\n    im.index_name, "
			+ "\n    im.idx_group, "
			+ "\n    im.opt_date_seq,"
			+ "\n    p.settlement_type, "
			+ "\n    rh.hol_id, "
			+ "\n    rhi.currency "
			+ "\n ORDER BY "
			+ "\n    sort_order ";

		Table latestHoldingIns = iof.runSQL (sql);
		return latestHoldingIns;
	}

	/**
	 * Checks whether the argument table follows the expectations. 
	 * @param argt the argument table to check
	 * @return true if argt follows the expected table structure,
	 * false if not.
	 */
	private boolean checkArgt(ConstTable argt) {
		boolean ret = false;
		if (argt.getColumnId("index_list") < 0 ||
				argt.getColumnId("end_date")   < 0	||
				argt.getColumnId("user_exit")  < 0	) {
			PluginLog.error("Input parameter table has incorrect format."
					+ "\nTable must have following columns: "
					+ "\nIndex_list (table)"
					+ "\nend_date (int)"
					+ "\nuser_exit (int)"
					+ "\nProbably this plugin was run without parameter plugin."
					+ "\nTry to run \"NDA_Create_Future_Holding_Ins\" task instead of running this plugin."
					+ "\nIf this doesn't help and you still see this message, please contact your system administrator.");
			return false;
		}

		// did user choose to exit?
		if (argt.getInt("user_exit", 0) == 1) {
			PluginLog.info("User has cancelled the task, exit...");
		}

		Table indexList = argt.getTable("index_list", 0); 
		if (indexList != null && indexList.getRowCount() > 0) { // did the user choose indices?
			if (argt.getInt("end_date", 0) > 0) { // did the user choose an end date?
				ret = true;
			} else {
				PluginLog.info("End date is not set up in param plugin");
			}
		} else {
			PluginLog.info("No indexes were selected in param plugin");
		}
		return ret;
	}

	/**
	 * inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Context context) {
		try {
			String abOutdir = context.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		if (errorTable != null) {
			errorTable.dispose();
		} 
		errorTable = context.getTableFactory().createTable("Errors");
		errorTable.addColumns("String[tran_field],String[value],String[err_msg]");
	}
}
