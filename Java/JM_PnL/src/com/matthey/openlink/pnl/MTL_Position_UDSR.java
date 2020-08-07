package com.matthey.openlink.pnl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.Instrument;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.PFOLIO_RESULT_TYPE;
import com.olf.openjvs.enums.RESULT_CLASS;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.olf.openjvs.enums.VALUE_STATUS_ENUM;
import com.openlink.util.misc.TableUtilities;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-MM-DD	V1.0	mtsteglov	- Initial Version
 * 2016-07-14	V1.1	jwaechter   - added nested execution of simulation for 
 *                                    Call Notice Deals
 * 2016-09-22	V1.2	jwaechter	- skipping execution of nested sim run 
 *                                    in case there are no relevant deals.
 * 2016-09-23	V1.3	jwaechter	- now retrieving cash flow date from fixed side of the
 *                                    com swap deal for metal rows.
 * 2016-10-10	V1.4	jwaechter	- logic of V1.3 is now applied as well for
 *                                    the non USD leg case.
 * 2016-10-17	V1.5	jwaechter	- now using fixed leg pymt date 
 *                                    for USD currency legs
 * 2016-10-18	V1.6	jwaechter	- added special logic for the following case: 
 *                                    the (floating) financial sides of a com swap 
 *                                    have been settled but the (fixed) metal 
 *                                    side has not. 
 * 2016-10-20	V1.7	jwaechter	- extended special logic to always run in case
 *                                    the fixed side has not been settled.
 * 2017-02-28	V1.8	jwaechter	- Added throwing of exceptions to catch methods
 * 2017-04-12	V1.9	jwaechter	- removing non USD currency rows from deals of 
 *                                    LoanDep toolset.
 * 2017-08-23	V1.10	lma	     	- idxCcySpd fix           
 * 2017-09-28 	V1.11	mstseglov	- Added "CB Rate" support     
 * 2017-10-04	V1.12	mstseglov	- fixed "Metal Price Spread" issue               
 * 2020-02-18   V1.13    agrawa01 	- memory leaks & formatting changes     
 * 2020-07-06   V1.14   jwaechter   - CallNot fixes:
 *                                    -> data retrievel for metal currency CallNot deals
 *                                       from Current Notional replace with querying table
 *                                    -> now retrieving positions from cancellations
 */

/**
 * Main Plugin for MTL Position UDSR
 * @author msteglov
 * @version 1.14
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class MTL_Position_UDSR implements IScript {
	
	private static final String SERVICE_NAME_REVAL = "Reval";
	private static final String SIM_DEF_NAME_CURRENT_NTNL = "MODIFY_TARGET_PATTERN";

	// Initialise static variable - USD currency is zero always, LBMA ref source will be populated on startup
	private static final int s_USD = 0;
	private static int s_LBMA_RefSource = -1;

	private int m_today = 0;

	private boolean m_histPricesReady = false;
	private Table m_histPricesData = null;

	private int fixedLeg = -1;
	private int fixedCcyConvMethod = -1;
	private int resetCcyConvMethod = -1;

	HashMap<Integer, Table> m_ccyForwardRates = null; 
	private static Table s_currenciesData = null;

	
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		initLogging();

		// Clear historical prices here
		clearHistPrices();

		USER_RESULT_OPERATIONS op = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));
		fixedLeg = Ref.getValue(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, "Fixed");
		fixedCcyConvMethod = Ref.getValue(SHM_USR_TABLES_ENUM.SPOTPX_RESET_TYPE, "Fixed");
		resetCcyConvMethod = Ref.getValue(SHM_USR_TABLES_ENUM.SPOTPX_RESET_TYPE, "Reset Level");
		
		try {
			switch (op) {
			case USER_RES_OP_CALCULATE:
				calculate(argt, returnt);
				break;
			case USER_RES_OP_FORMAT:
				format(argt, returnt);				
				break;
			}
			Logging.info("Plugin " + this.getClass().getName() + " finished successfully");
		} 
		catch (Exception e) 
		{
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			//OConsole.message(e.toString() + "\r\n");
			Logging.error("Plugin " + this.getClass().getName() + " failed");
			throw e;
		} finally {
			Logging.close();
			// Clear historical prices here
			clearHistPrices();			
		}
	}

	private void initLogging() throws OException {	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().equals("")) {
			logFile = this.getClass().getName() + ".log";
		}
		try {
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	
	Table getCurrencyTable() throws OException {
		if ((s_currenciesData == null) || (Table.isTableValid(s_currenciesData) != 1)) {
			try  {			
				s_currenciesData = Table.tableNew();
				int ret;
				do {
					ret = DBaseTable.execISql(s_currenciesData, "SELECT * FROM currency WHERE id_number >= 0");
					if (ret == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
						String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Retryable error exeucting SQL to retrieve currencies");
						Logging.warn (errorMessage);
					}
				} while (ret == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt());
				
				if (ret <= 0) {
					String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error exeucting SQL to retrieve currencies");
					Logging.error(message);
				}
			} catch (OException oe) {
				Logging.error(oe.toString());
				for (StackTraceElement ste : oe.getStackTrace()) {
					Logging.error(ste.toString());
				}
				throw oe;
			}
		}

		return s_currenciesData;
	}


	protected void prepareCurrencyForwardRates() {
		// Prepare currency forward rates
		m_ccyForwardRates = new HashMap<Integer, Table>();		
	}

	protected void clearCurrencyForwardRates() {
		for (Table t : m_ccyForwardRates.values()) {
			try {
				if (Table.isTableValid(t) == 1) {
					t.destroy();
				}
			}
			catch (Exception e) {
				Logging.error(e.toString());
				for (StackTraceElement ste : e.getStackTrace()) {
					Logging.error(ste.toString());
				}
			}
			t = null;
		}

		m_ccyForwardRates.clear();
	}

	protected double getForwardRate(int ccy, int date) throws OException {
		double rate = 0.0;
		try {
			if (!m_ccyForwardRates.containsKey(ccy)) {
				Table ccyData = getCurrencyTable();
				int ccyRow = ccyData.unsortedFindInt("id_number", ccy);

				if (ccyRow < 1)
					return rate;

				int fxIndex = ccyData.getInt("spot_index", ccyRow);
				int convention = ccyData.getInt("convention", ccyRow);

				Table fxIdxData = Index.getOutput(fxIndex, "1cd");
				fxIdxData.addCol("date_int", COL_TYPE_ENUM.COL_INT);
				fxIdxData.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);

				int rows = fxIdxData.getNumRows();
				for (int row = 1; row <= rows; row++) {
					int rowDate = fxIdxData.getDate("Date", row);
					double price = fxIdxData.getDouble("Price (Mid)", row);

					if ((convention == 0) && (price > 0)) {
						price = 1 / price;
					}
					fxIdxData.setInt("date_int", row, rowDate);
					fxIdxData.setDouble("price", row, price);
				}

				fxIdxData.group("date_int");
				m_ccyForwardRates.put(ccy, fxIdxData);
			}

			Table ccyData = m_ccyForwardRates.get(ccy);
			int row = ccyData.findInt("date_int", date, SEARCH_ENUM.FIRST_IN_GROUP);

			if (row > 0)
				rate = ccyData.getDouble("price", row);
			
		} catch (Exception e) {
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}			
			throw e;
		}

		return rate;
	}

	protected void calculate(Table argt, Table returnt) throws OException {
		m_today = OCalendar.today();

		if (s_LBMA_RefSource < 0) {
			s_LBMA_RefSource = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, "LBMA");
		}

		// Retrieve all relevant pre-requisite results
		Table revalSimResults = argt.getTable("sim_results", 1);
		Table genResults = revalSimResults.getTable("result_class",RESULT_CLASS.RESULT_GEN.toInt());		

		Table tranLegResults = revalSimResults.getTable("result_class", RESULT_CLASS.RESULT_TRAN_LEG.toInt());

		Table cflowByDayResults = SimResult.getGenResultTables(genResults, PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT.toInt());
		Table cflowByDayResult = cflowByDayResults.getTable("results", 1);	

		prepareCurrencyForwardRates();

		Table transData = Util.NULL_TABLE;
		Table fxData = Util.NULL_TABLE, comFutData = Util.NULL_TABLE, comSwapData = Util.NULL_TABLE;;
		Table callNotData = Util.NULL_TABLE, currNotTranResultTable = Util.NULL_TABLE, loanDepData = Util.NULL_TABLE; 
		try {
			// Generate transaction-level data columns
			transData = prepareTransactionsData(argt.getTable("transactions", 1));		

			// Process FX toolset deals
			Logging.info("Process FX toolset deals\n");
			fxData = generateFXDataTable(cflowByDayResult, transData);				
			returnt.select(fxData, "*", "deal_num GE 0");

			// Process ComFut toolset deals
			Logging.info("Process ComFut toolset deals\n");
			comFutData = generateComFutDataTable(tranLegResults, transData);				
			returnt.select(comFutData, "*", "deal_num GE 0");

			// Process ComSwap toolset deals
			Logging.info("Process ComSwap toolset deals\n");
			comSwapData = generateComSwapDataTable(tranLegResults, transData);				
			returnt.select(comSwapData, "*", "deal_num GE 0");		

			// PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT: has to be run on a different date
			currNotTranResultTable = runSimForCurrNot(transData);
			if (currNotTranResultTable.getNumRows() > 0) {
				Table revalSimResultsCurrNot = currNotTranResultTable.getTable("scenario_results", 1);               
				Table tranResultsCurrNtnl    = revalSimResultsCurrNot.getTable("result_class", RESULT_CLASS.RESULT_TRAN.toInt());

				// Process CallNot toolset deals
				Logging.info("Process CallNot toolset deals\n");
				callNotData = generateCallNotDataTable(tranResultsCurrNtnl, transData);                                                
				returnt.select(callNotData, "*", "deal_num GE 0");
			}

			// Process LoanDep toolset deals
			Logging.info("Process LoanDep toolset deals\n");
			loanDepData = generateLoanDepDataTable(cflowByDayResult, transData);				
			returnt.select(loanDepData, "*", "deal_num GE 0");		

			// Iterate and set the "Position Type" field
			Logging.info("Iterate and set the Position Type field\n");
			int rows = returnt.getNumRows();
			for (int i = 1; i <= rows; i++) {
				int ccy = returnt.getInt("metal_ccy", i);

				boolean isPreciousMetal = MTL_Position_Utilities.isPreciousMetal(ccy);
				int positionType = isPreciousMetal ? MTL_Position_Enums.PositionType.METAL : MTL_Position_Enums.PositionType.CURRENCY;
				returnt.setInt("position_type", i, positionType);
			}

		} finally {
			clearCurrencyForwardRates();
			
			if (Table.isTableValid(transData) == 1) {
				transData.destroy();
			}
			
			if (Table.isTableValid(fxData) == 1) {
				fxData.destroy();
			}
			
			if (Table.isTableValid(comFutData) == 1) {
				comFutData.destroy();
			}
			
			if (Table.isTableValid(comSwapData) == 1) {
				comSwapData.destroy();
			}
			
			if (Table.isTableValid(callNotData) == 1) {
				callNotData.destroy();
			}
			
			if (Table.isTableValid(loanDepData) == 1) {
				loanDepData.destroy();
			}
			
			if (Table.isTableValid(currNotTranResultTable) == 1) {
				currNotTranResultTable.destroy();
			}
		}
	}

	private Table runSimForCurrNot(Table transData) throws OException {
		Table workData = Table.tableNew("Call notice deal numbers");
		int queryId = -1;
		
		// Retrieve all Call Notice deals
		workData.select(transData, "tran_num", "toolset EQ 35");
		if (workData.getNumRows() == 0) {
			return workData;
		}
					
		try {
			Table simDef = Sim.loadSimulation(SIM_DEF_NAME_CURRENT_NTNL);
			Table scenDef = simDef.getTable("scenario_def", 1);
			Table scenConfig = scenDef.getTable("scenario_config_table", 1);
			Table scenConfigSubTable = scenConfig.getTable("config_sub_table", 1);
			scenConfigSubTable.sortCol("param_id", TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_ASCENDING);
			int modDateRow = scenConfigSubTable.findInt("param_id", 2, SEARCH_ENUM.FIRST_IN_GROUP);
			int dateRow = scenConfigSubTable.findInt("param_id", 1, SEARCH_ENUM.FIRST_IN_GROUP);
			int eodHolId = Ref.getValue(SHM_USR_TABLES_ENUM.HOL_ID_TABLE, "EOD_HOLIDAY_SCHEDULE" );
			int lgbd = OCalendar.parseStringWithHolId("-1d", eodHolId, m_today);
			int lgbdPlus1Cd = OCalendar.parseStringWithHolId("1cd", eodHolId, lgbd);
			String modDate = OCalendar.formatJd(lgbdPlus1Cd);
			String date = OCalendar.formatJd(m_today);
			scenConfigSubTable.setString("param_value", modDateRow, modDate);
			scenConfigSubTable.setString("param_value", dateRow, date);

			queryId = Query.tableQueryInsert(workData, "tran_num");
			Table revalParam = Table.tableNew ("Reval Parameters");
			revalParam = Sim.createRevalTable(revalParam);
			revalParam.setTable("SimulationDef", 1, simDef);
			revalParam.setInt("SimRunId", 1, -1);
			revalParam.setInt("SimDefId", 1, -1);
			revalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
			revalParam.setInt("QueryId", 1, queryId );
			revalParam.setString("ServiceName", 1, SERVICE_NAME_REVAL);
			Table revalTable = Table.tableNew("Reval");
			revalTable.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
			revalTable.addRow();
			revalTable.setTable("RevalParam", 1, revalParam);
			//        revalParam.viewTable();
			Table simResults = Sim.runRevalByParamFixed(revalTable);
			return simResults;
			
		} finally {
			if (queryId > 0) {
				Query.clear(queryId);
			}

			if (Table.isTableValid(workData) == 1) {
				workData.destroy();
			}
		}
	}


	private Table generateFXDataTable(Table cflowByDayResult, Table transData) throws OException
	{		
		Table workData = createOutputTable();	

		// Skip processing if no deal generated Cashflow By Day
		if (cflowByDayResult.getNumRows() < 1)
		{
			return workData;
		}

		cflowByDayResult.select(transData, "toolset, tran_ptr", "deal_num EQ $deal_num");		

		// Retrieve all legs bar USD-payout leg (currency of zero, hence GT in selection below)
		// We only want to retrieve cashflows today or in future, as prior cashflows are part of CallNot already
		workData.select(cflowByDayResult, 
				"deal_num, deal_leg, deal_pdc, cflow(position), currency (metal_ccy), cflow_date, " +
						"cflow_date(pricing_start_date), cflow_date(pricing_end_date), cflow_date(pricing_rfis_start_date), cflow_date(pricing_rfis_end_date), " +
						"tran_ptr", 
						"toolset EQ 9 AND currency GT 0 AND cflow_date GE " + m_today);

		int rows = workData.getNumRows();
		for (int row = 1; row <= rows; row++)
		{
			int ccy = workData.getInt("metal_ccy", row);

			// The projection index for FX trades is the default FX index for the precious metal of the FX deal
			int projIdx = MTL_Position_Utilities.getDefaultFXIndexForCcy(ccy);
			workData.setInt("proj_idx", row, projIdx);

			// Now figure out what trade price to set (expected to be measured in USD)
			// For leg zero, take the Dealt Rate, and adjust if the leg one is non-USD (multiply by forward FX rate of leg one to USD)
			// For leg one, take the forward FX rate of leg one's currency to USD
			Transaction trn = workData.getTran("tran_ptr", row);
			int dealLeg = workData.getInt("deal_leg", row);
			int cflowDate = workData.getInt("cflow_date", row);

			double dealtRate = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0);

			if (trn.getInsSubType() == INS_SUB_TYPE.fx_far_leg.toInt())
			{
				dealtRate = trn.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt());
			}						

			if (dealtRate < 0.0001)
				dealtRate = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 0);			

			int usdCcy = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "USD");
			int legZeroCurrency = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0);
			int legOneCurrency = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 1);

			boolean isLegZeroUSD = (legZeroCurrency == usdCcy);
			boolean isLegOneUSD = (legOneCurrency == usdCcy);

			double tradePrice = 0.0;			
			double fxFactor = 1.0;

			if (!isLegZeroUSD && !isLegOneUSD)
			{
				// This is EUR-GBP or similar deal
				// USD-Equivalent Price on leg zero is the deal price (measured as 1 unit of leg 0 ccy in units of leg 1 ccy) * FX factor between ccy 1 and USD
				// USD-Equivalent Price on leg one is simply the FX factor between ccy 1 and USD
				fxFactor = getForwardRate(legOneCurrency, cflowDate);
				tradePrice = (dealLeg == 0) ? dealtRate * fxFactor : fxFactor;
			}
			else if (!isLegZeroUSD)
			{
				// This is ZAR-USD or similiar deal
				// USD-Equivalent Price on leg zero is the deal price (measured as 1 unit of leg 0 ccy in units of leg 1 ccy)
				tradePrice = dealtRate;
			}
			else if (!isLegOneUSD)
			{
				// This is USD-ZAR or similiar deal
				// USD-Equivalent Price on leg zero is the inverse of deal price (measured as 1 unit of leg 0 ccy in units of leg 1 ccy)
				tradePrice = (dealtRate > 0.001) ? 1 / dealtRate : 0.0;
			}

			workData.setDouble("usd_trade_price", row, tradePrice);

		}

		// Leave the reference source blank for FX deals, as the position is fixed at the time the deal is struck
		// Index.tableColSetIndexRefSource(workData, "proj_idx", "ref_source");

		workData.mathMultCol("usd_trade_price", "position", "usd_trade_value");

		workData.setColValInt("fixed_unfixed", MTL_Position_Enums.FixedUnfixed.FIXED);

		workData.delCol("tran_ptr");

		return workData;
	}

	private Table generateLoanDepDataTable(Table cflowByDayResult, Table transData) throws OException
	{		
		Table workData = createOutputTable();	

		// Skip processing if no deal generated Cashflow By Day
		if (cflowByDayResult.getNumRows() < 1)
		{
			return workData;
		}

		int today = OCalendar.today();

		cflowByDayResult.select(transData, "toolset, tran_ptr", "deal_num EQ $deal_num");		

		// Retrieve all legs bar USD-payout leg (currency of zero, hence GT in selection below)
		// We only want to retrieve cashflows today or in future, as prior cashflows are part of CallNot already
		workData.select(cflowByDayResult, 
				"deal_num, deal_leg, deal_pdc, cflow(position), currency (metal_ccy), cflow_date, " +
						"cflow_date(pricing_start_date), cflow_date(pricing_end_date), cflow_date(pricing_rfis_start_date), cflow_date(pricing_rfis_end_date), " +
						"tran_ptr", 
						"toolset EQ 6 AND currency GT 0 AND cflow_date GE " + m_today);		

		for (int row = workData.getNumRows(); row >= 1; row--)
		{
			int ccy = workData.getInt("metal_ccy", row);
			if (ccy != 0 && !MTL_Position_Utilities.isPreciousMetal(ccy)) {
				workData.delRow(row);
				continue;
			}
			// The projection index for LoanDepo trades is the default FX index for the precious metal of the FX deal
			int projIdx = MTL_Position_Utilities.getDefaultFXIndexForCcy(ccy);
			workData.setInt("proj_idx", row, projIdx);

			// Set trade price from today's metal price			
			if (projIdx > 0)
			{
				double spotPrice = MTL_Position_Utilities.getSpotGptRate(projIdx);
				workData.setDouble("usd_trade_price", row, spotPrice);
			}				
		}

		workData.mathMultCol("usd_trade_price", "position", "usd_trade_value");

		workData.setColValInt("fixed_unfixed", MTL_Position_Enums.FixedUnfixed.FIXED);		
		workData.delCol("tran_ptr");

		return workData;
	}	

	private Table generateComFutDataTable(Table tranLegResults, Table transData) throws OException
	{
		Table workData = createOutputTable();		

		// Retrieve all ComFut deals
		workData.select(transData, "deal_num, fut_expiry_date(cflow_date), tran_ptr", "toolset EQ 17 AND fut_expiry_date GE " + m_today);		

		// Enrich all relevant legs and profile periods with appropriate position
		String tranLegSelectResults = "deal_num, deal_leg, deal_pdc, proj_idx, " + PFOLIO_RESULT_TYPE.SIZE_BY_LEG_RESULT.toInt() + "(position)";
		workData.select(tranLegResults, tranLegSelectResults, "deal_num EQ $deal_num");

		int rows = workData.getNumRows();
		for (int i = 1; i <= rows; i++)
		{
			Transaction trn = workData.getTran("tran_ptr", i);
			int dealLeg = workData.getInt("deal_leg", i);
			int dealPdc = workData.getInt("deal_pdc", i);

			double dealPrice = trn.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt());

			int totalResetPeriods = trn.getNumRows(dealLeg, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
			int firstReset = Integer.MAX_VALUE, firstRFISReset = Integer.MAX_VALUE;
			int lastReset = 0, lastRFISReset = 0;

			for (int j = 0; j < totalResetPeriods; j++)
			{
				int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), 0, null, j);	
				int resetRfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_RFIS_DATE.toInt(), 0, null, j);						
				int blockEnd = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_BLOCK_END.toInt(), 0, null, j);	

				if (blockEnd > 0)
					continue;

				firstReset = Math.min(firstReset, resetDate);
				lastReset = Math.max(lastReset, resetDate);				

				firstRFISReset = Math.min(firstRFISReset, resetRfisDate);
				lastRFISReset = Math.max(lastRFISReset, resetRfisDate);
			}

			workData.setInt("pricing_start_date", i, firstReset);
			workData.setInt("pricing_end_date", i, lastReset);		

			workData.setInt("pricing_rfis_start_date", i, firstRFISReset);
			workData.setInt("pricing_rfis_end_date", i, lastRFISReset);					

			// Set currency from projection index
			int projIdx = workData.getInt("proj_idx", i);			
			int ccy = MTL_Position_Utilities.getCcyForIndex(projIdx);

			int refSource = trn.getFieldInt(TRANF_FIELD.TRANF_REF_SOURCE.toInt(), 0);
			workData.setInt("ref_source", i, refSource);

			workData.setInt("metal_ccy", i, ccy);

			workData.setDouble("usd_trade_price", i, dealPrice);
		}

		workData.delCol("tran_ptr");

		workData.mathMultCol("usd_trade_price", "position", "usd_trade_value");
		workData.setColValInt("fixed_unfixed", MTL_Position_Enums.FixedUnfixed.FIXED);

		return workData;
	}

	static class SwapsData
	{
		int m_dealNum;
		int m_dealLeg;
		int m_dealPdc;
		int m_dealReset;

		int m_resetDate;
		int m_rfisDate;
		double m_position;
		double m_usdTradePrice;

		int m_fixedUnfixedStatus;
		int m_pymtDate;
		int m_projIdx;
		int m_refSource;
		int m_ccy;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + m_ccy;
			result = prime * result + m_dealLeg;
			result = prime * result + m_dealNum;
			result = prime * result + m_dealPdc;
			result = prime * result + m_dealReset;
			result = prime * result + m_fixedUnfixedStatus;
			result = prime * result + m_projIdx;
			result = prime * result + m_pymtDate;
			result = prime * result + m_refSource;
			result = prime * result + m_resetDate;
			result = prime * result + m_rfisDate;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SwapsData other = (SwapsData) obj;
			if (m_ccy != other.m_ccy)
				return false;
			if (m_dealLeg != other.m_dealLeg)
				return false;
			if (m_dealNum != other.m_dealNum)
				return false;
			if (m_dealPdc != other.m_dealPdc)
				return false;
			if (m_dealReset != other.m_dealReset)
				return false;
			if (m_fixedUnfixedStatus != other.m_fixedUnfixedStatus)
				return false;
			if (m_projIdx != other.m_projIdx)
				return false;
			if (m_pymtDate != other.m_pymtDate)
				return false;
			if (m_refSource != other.m_refSource)
				return false;
			if (m_resetDate != other.m_resetDate)
				return false;
			if (m_rfisDate != other.m_rfisDate)
				return false;
			return true;
		}

		
	}

	/**
	 * The ComSwap position is considered as the actual metal (so the total volume and sign should match the fixed side),
	 * but the dates come from pricing dates, which are based on the floating legs. Similiarly, the volume being priced each day
	 * can be different, and should come from the appropriate reset on each day. Finally, we support multiple floating legs, as
	 * some swaps price off an average of two different reference sources, and this requires us to model them as separate legs in Endur. 
	 * 
	 * @param tranLegResults
	 * @param transData
	 * @return
	 * @throws OException
	 */
	private Table generateComSwapDataTable(Table tranLegResults, Table transData) throws OException
	{
		Table workData = createOutputTable();

		// We report everything in TOz
		int reportingUnit =  Ref.getValue(SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE, "TOz");

		Vector<SwapsData> resets = new Vector<SwapsData>();

		int rows = transData.getNumRows();
		for (int row = 1; row <= rows; row++)
		{			
			int dealNum = transData.getInt("deal_num", row);
			int toolset = transData.getInt("toolset", row);

			if (toolset != TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt())
			{
				continue;
			}

			// OConsole.message("Processing deal: " + dealNum +" \n");

			Transaction trn = transData.getTran("tran_ptr", row);

			int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());

			// Calculate floating leg weight - an adjustment applied to all floating legs' notional
			// if it does not equal the fixed leg's notional amount; used as a ratio to adjust the notional
			// of each reset later
			double fixedLegSize = 0.0, floatingLegSize = 0.0, floatingLegWeight = 1.0;
			for (int param = 0; param < numParams; param++)
			{
				int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), param);
				double notional = trn.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), param);
				if (fxFlt == fixedLeg)
				{					
					fixedLegSize += notional;
				}				
				else
				{
					floatingLegSize += notional;
				}
			}				
			if (floatingLegSize > 0.0)
			{
				floatingLegWeight = fixedLegSize / floatingLegSize;
			}

			// For index-pricing deals where payment ccy != index ccy, the core "spread" is in payment currency
			// Where we enter contracts with spread in "index ccy", we have to use a "Tran Info" field
			// E.g. EUR-nominated deals pricing off a USD-based index can have a USD-nominated spread
			double idxCcySpd = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_metalSpreadTranInfoField);

			// EUR-nominated deals can have an FX rate spread applied to the FX rate at which they are converted
			double fxRateSpd = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_usdFXSpreadTranInfoField);			

			// CB Rate adjustment is based on CB Rate and number of days between delivery and pricing
			double cbRate = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_cbRateTranInfoField);
			
			// Now iterate over all floating legs' resets, and create a row for each
			int pymtDateFixed=0;
			int pymtDate=0;
			for (int param = 0; param < numParams; param++) {
				double fixedFXRate = 0.0;

				// Skip fixed leg - all pricing occurs on 
				int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), param);				
				if (fxFlt == fixedLeg) {	
					pymtDateFixed = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), param, "", 0);	
					continue;
				}

				double legSpd = trn.getFieldDouble(TRANF_FIELD.TRANF_RATE_SPD.toInt(), param);
				double legMult = trn.getFieldDouble(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), param);

				int legUnit = trn.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), param);				

				double unitConversionRatio = 1.0;
				if (legUnit != reportingUnit)
				{
					unitConversionRatio = Transaction.getUnitConversionFactor(legUnit, reportingUnit);
				}

				int totalProfilePeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_PROFILE.toInt());
				int totalResetPeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
				int blockEndsPassed = 0;

				int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), param, "", 0, 0);
				int refSource = trn.getFieldInt(TRANF_FIELD.TRANF_REF_SOURCE.toInt(), param);

				// Set FX reference source from core field; if blank, default to base reference source
				int fxRefSource = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, trn.getField(TRANF_FIELD.TRANF_CCY_CONV_REF_SOURCE.toInt(), param));								
				if (fxRefSource < 1) {
					fxRefSource = refSource;
				}
				int legCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), param);
				int ccyConversionMethod = trn.getFieldInt(TRANF_FIELD.TRANF_CCY_CONV_METHOD.toInt(), param);

				if (ccyConversionMethod == fixedCcyConvMethod) {
					fixedFXRate = trn.getFieldDouble(TRANF_FIELD.TRANF_CCY_CONV_RATE.toInt(), param);

					if (fixedFXRate > 0.0) {
						fixedFXRate = 1 / fixedFXRate;
					}
				}

				// We need to consider if the underlying pricing index on a swap is in a non-USD currency 
				// E.g. XAU.EUR - the currency will be EUR, and default FX index will be FX_EUR.USD
				int projIdxCcy = MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx);
				int defaultFXIndex = MTL_Position_Utilities.getDefaultFXIndexForCcy(projIdxCcy);

				for (int j = 0; j < totalResetPeriods; j++) {
					int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), param, "", j);	
					int resetRfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_RFIS_DATE.toInt(), param, "", j);	
					int resetStatus = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), param, "", j);	
					int blockEnd = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_BLOCK_END.toInt(), param, "", j);	
					double resetNotional = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_NOTIONAL.toInt(), param, "", j);
					double resetRawValue = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_RAW_VALUE.toInt(), param, "", j);

					// Skip summary resets (one per each profile period, unless there's only reset per profile period,
					// in which case these don't exist
					if (blockEnd > 0) {
						blockEndsPassed++;
						continue;
					}

					int dealPdc = (totalProfilePeriods == totalResetPeriods) ? j : blockEndsPassed;

					pymtDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), param, "", dealPdc);	
					boolean isPreciousMetal = MTL_Position_Utilities.isPreciousMetal(legCcy);

					// Skip profile periods where payment date is already in the past, as this means the swap
					// has settled, and any metal is now part of the open notional position

					if (((isPreciousMetal || legCcy==0 )?pymtDateFixed:pymtDate) < m_today) {
						continue;
					}		

					int fixedUnfixedStatus = MTL_Position_Enums.FixedUnfixed.UNFIXED;

					if ((resetStatus == VALUE_STATUS_ENUM.VALUE_KNOWN.toInt()) || (resetDate < m_today))
					{
						fixedUnfixedStatus = MTL_Position_Enums.FixedUnfixed.FIXED;
					}
					else if (resetDate == m_today)
					{
						fixedUnfixedStatus = getFixedUnfixedStatus(projIdx, resetDate, resetRfisDate, refSource);
					}

					// If the reset raw value is zero, it is possible it is not being populated by core system 
					// due to being fixed - retrieve historical price from idx_historical_prices instead
					if ((Math.abs(resetRawValue) < 0.001)) {
						resetRawValue = MTL_Position_Utilities.getHistPrice(projIdx, resetDate, resetRfisDate, refSource);
					}

					double fxFactor = 1.0;
					
					// Normally, the "Metal Price Spread" is the same for every reset, but if the projection index
					// is in non-USD, we need to adjust it by the FX rate associated with each individual FX reset
					// so set up a reset-level "Metal Price Spread" variable to store the per-reset value
					double resetIdxCcySpread = idxCcySpd; 

					// If this leg's currency is not USD, set the appropriate FX factor to convert it to USD
					if (legCcy != s_USD) {
						if (projIdxCcy != s_USD) {
							// If the underlying index is not in USD, convert it based on either the historical price of its default FX index
							// (use LBMA reference source, as it is LBMA deals who price off such indexes as XAU.EUR), or just the forward FX
							// rate associated with the reset date
							Logging.info("Processing non-USD index for date: " + OCalendar.formatDateInt(resetDate) + "\n");
							if (resetDate < m_today) {								
								fxFactor = MTL_Position_Utilities.getHistPrice(defaultFXIndex, resetDate, resetRfisDate, s_LBMA_RefSource);							
							} else {								
								fxFactor = getForwardRate(legCcy, resetRfisDate);
							}

							Logging.info("FX Factor: " + fxFactor + ", reset raw value: " + resetRawValue + "\n");

							// We need to normalise the reset raw value to convert to USD
							if (Math.abs(fxFactor) < 0.001) {
								fxFactor = 1.0;
							}

							resetRawValue = resetRawValue * fxFactor;

							// Set the "FX" reference source to be LBMA, since that is what the EUR curve prices off
							fxRefSource = s_LBMA_RefSource;

							// Convert the spread into USD from original currency
							resetIdxCcySpread = idxCcySpd * fxFactor;
							
						} else if (ccyConversionMethod == fixedCcyConvMethod) {
							// Underlying index in USD, fixed conversion ratio from the deal
							fxFactor = fixedFXRate;
						} else if (ccyConversionMethod == resetCcyConvMethod) {
							// Underlying index in USD, each reset converts independently							
							if (resetDate <= m_today) {
								fxFactor = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_SPOT_CONV.toInt(), param, "", j);
								if (fxFactor > 0.0) {
									fxFactor = 1 / fxFactor;
								}
							} else {
								// Retrieve the FX reset RFIS date, as that is the date of the FX conversion to use, if available
								int fxRfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_SPOT_RATE_RFIS_DATE.toInt(), param, "", j);
								if (fxRfisDate > 0) {
									fxFactor = getForwardRate(legCcy, resetRfisDate);
								} else {
									// Fall back on reset date if not found
									fxFactor = getForwardRate(legCcy, resetDate);
								}														
							}							
						} else {
							// Underlying index in USD, single FX conversion based on payment date							
							fxFactor = getForwardRate(legCcy, pymtDate);
						}						
					}

					// Add the "FX rate spread" to the calculated FX rate - note that we add it to the "USD per currency" value
					// which is the inverse of FX factor variable we have here
					if (Math.abs(fxRateSpd) > 0.0001) {
						double backSolveFXFactor = 1 / fxFactor;
						backSolveFXFactor += fxRateSpd;
						fxFactor = 1 / backSolveFXFactor;																		
					}

					// Take raw value (now converted to US dollars), and apply the leg-level index multiplier
					// The core leg spread is in foreign currency, so apply FX factor to it
					// Index currency spread is either in USD, or we have pre-converted it to USD already
					double usdPrice = resetRawValue * legMult + legSpd * fxFactor + resetIdxCcySpread;
					
					if (floatingLegWeight > 0) {
						usdPrice /= floatingLegWeight;
					}
					
					// If "CB Rate" is set, calculate adjustment factor from it, and apply to usdPrice
					if (Math.abs(cbRate) > 0.001) {						
						// Delivery date is the "payment date" on metal (fixed) leg
						int deliveryDate = pymtDateFixed; 
						
						// CB factor is based on rate across 360 days, multiplied by number of days between pricing and delivery
						double cbAdjustmentFactor = 1 + ((cbRate / 100) / 360) * (deliveryDate - resetRfisDate);		
						
						usdPrice *= cbAdjustmentFactor;
					}					
					
					// Create data for the Precious Metal entry
					SwapsData data = new SwapsData();

					data.m_dealNum = dealNum;
					data.m_dealLeg = param;
					data.m_dealPdc = dealPdc;
					data.m_dealReset = j;

					data.m_resetDate = resetDate;
					data.m_rfisDate = resetRfisDate;
					data.m_fixedUnfixedStatus = fixedUnfixedStatus;
					data.m_position = -1 * resetNotional * floatingLegWeight * unitConversionRatio;
					data.m_usdTradePrice = usdPrice;

					data.m_projIdx = projIdx;
					data.m_refSource = refSource;
					int ccy = MTL_Position_Utilities.getCcyForIndex(projIdx);
					data.m_ccy = ccy;
					isPreciousMetal = MTL_Position_Utilities.isPreciousMetal(ccy);
					data.m_pymtDate = (isPreciousMetal)?pymtDateFixed:pymtDate;

					if (data.m_pymtDate >= m_today) {
						resets.add(data);						
					}
					double usdValue = data.m_position * data.m_usdTradePrice;

					if (legCcy != s_USD) {
						// Create data for the non-USD currency payment
						data = new SwapsData();

						data.m_dealNum = dealNum;
						data.m_dealLeg = param;
						data.m_dealPdc = dealPdc;
						data.m_dealReset = j;

						data.m_resetDate = resetDate;
						data.m_rfisDate = resetRfisDate;
						data.m_fixedUnfixedStatus = fixedUnfixedStatus;
						data.m_position = (fxFactor > 0.0) ? -1 * usdValue / fxFactor : 0.0;
						data.m_usdTradePrice = fxFactor;

						data.m_projIdx = MTL_Position_Utilities.getDefaultFXIndexForCcy(legCcy);
						data.m_refSource = fxRefSource;
						data.m_ccy = legCcy;
						boolean isLegCcyPreciousMetal = MTL_Position_Utilities.isPreciousMetal(legCcy);
						data.m_pymtDate = (isLegCcyPreciousMetal)?pymtDateFixed:pymtDate;

						if (data.m_pymtDate >= m_today) {
							resets.add(data);
						}
					}
				}
			}
			pymtDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 0, "", 0);
			if (	pymtDate >= m_today) { // check if we should add the fixed leg				
				applyFixedLogic(trn, 0, dealNum, resets);
			}
		}

		for (SwapsData data : resets) {
			int newRow = workData.addRow();

			workData.setInt("deal_num", newRow, data.m_dealNum);
			workData.setInt("deal_leg", newRow, data.m_dealLeg);
			workData.setInt("deal_pdc", newRow, data.m_dealPdc);
			workData.setInt("deal_reset_id", newRow, data.m_dealReset);

			workData.setInt("pricing_start_date", newRow, data.m_resetDate);
			workData.setInt("pricing_end_date", newRow, data.m_resetDate);

			workData.setInt("pricing_rfis_start_date", newRow, data.m_rfisDate);
			workData.setInt("pricing_rfis_end_date", newRow, data.m_rfisDate);		

			workData.setInt("cflow_date", newRow, data.m_pymtDate);
			workData.setInt("proj_idx", newRow, data.m_projIdx);
			workData.setInt("metal_ccy", newRow, data.m_ccy);	
			workData.setInt("fixed_unfixed", newRow, data.m_fixedUnfixedStatus);

			workData.setDouble("position", newRow, data.m_position);
			workData.setDouble("usd_trade_price", newRow, data.m_usdTradePrice);

			workData.setInt("ref_source", newRow, data.m_refSource);
		}

		workData.mathMultCol("usd_trade_price", "position", "usd_trade_value");

		workData.delCol("last_past_reset_date");
		workData.delCol("last_past_rfis_date");
		workData.delCol("first_current_reset_date");
		workData.delCol("first_current_rfis_date");
		workData.delCol("cflow_date_d");
		workData.delCol("tran_ptr");

		// Delete anything which is still USD
		workData.deleteWhereValue("metal_ccy", 0);

		return workData;
	}	

	private void applyFixedLogic(final Transaction trn, final int leg, final int dealNum,
			final Vector<SwapsData> resets
			) throws OException {
		
		// For index-pricing deals where payment ccy != index ccy, the core "spread" is in payment currency
		// Where we enter contracts with spread in "index ccy", we have to use a "Tran Info" field
		// E.g. EUR-nominated deals pricing off a USD-based index can have a USD-nominated spread
		double idxCcySpd = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_metalSpreadTranInfoField);

		// EUR-nominated deals can have an FX rate spread applied to the FX rate at which they are converted
		double fxRateSpd = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_usdFXSpreadTranInfoField);			

		// We report everything in TOz
		int reportingUnit =  Ref.getValue(SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE, "TOz");

		double floatingLegWeight = 1.0;
		int pymtDateFixed=trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), leg, "", 0);
		int pymtDate=0;
		int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());
		
		for (int param = leg+1; param < numParams; param++)
		{
			double fixedFXRate = 0.0;

			double legSpd = trn.getFieldDouble(TRANF_FIELD.TRANF_RATE_SPD.toInt(), param);
			double legMult = trn.getFieldDouble(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), param);
			int legUnit = trn.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), param);				

			double unitConversionRatio = 1.0;
			if (legUnit != reportingUnit) {
				unitConversionRatio = Transaction.getUnitConversionFactor(legUnit, reportingUnit);
			}

			int totalProfilePeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_PROFILE.toInt());
			int totalResetPeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
			int blockEndsPassed = 0;

			int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), param, "", 0, 0);
			int refSource = trn.getFieldInt(TRANF_FIELD.TRANF_REF_SOURCE.toInt(), param);

			// Set FX reference source from core field; if blank, default to base reference source
			int fxRefSource = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, trn.getField(TRANF_FIELD.TRANF_CCY_CONV_REF_SOURCE.toInt(), param));
			if (fxRefSource < 1) {
				fxRefSource = refSource;
			}
			int legCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), param);
			int ccyConversionMethod = trn.getFieldInt(TRANF_FIELD.TRANF_CCY_CONV_METHOD.toInt(), param);

			if (ccyConversionMethod == fixedCcyConvMethod) {
				fixedFXRate = trn.getFieldDouble(TRANF_FIELD.TRANF_CCY_CONV_RATE.toInt(), param);

				if (fixedFXRate > 0.0)
				{
					fixedFXRate = 1 / fixedFXRate;
				}
			}

			// We need to consider if the underlying pricing index on a swap is in a non-USD currency 
			// E.g. XAU.EUR - the currency will be EUR, and default FX index will be FX_EUR.USD
			int projIdxCcy = MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx);
			int defaultFXIndex = MTL_Position_Utilities.getDefaultFXIndexForCcy(projIdxCcy);

			for (int j = 0; j < totalResetPeriods; j++) {
				int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), param, "", j);	
				int resetRfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_RFIS_DATE.toInt(), param, "", j);	
				int resetStatus = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), param, "", j);	
				int blockEnd = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_BLOCK_END.toInt(), param, "", j);	
				double resetNotional = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_NOTIONAL.toInt(), param, "", j);
				double resetRawValue = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_RAW_VALUE.toInt(), param, "", j);

				// Skip summary resets (one per each profile period, unless there's only reset per profile period,
				// in which case these don't exist
				if (blockEnd > 0) {
					blockEndsPassed++;
					continue;
				}

				int dealPdc = (totalProfilePeriods == totalResetPeriods) ? j : blockEndsPassed;

				pymtDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), param, "", dealPdc);	

				// Skip profile periods where payment date is already in the past, as this means the swap
				// has settled, and any metal is now part of the open notional position

//				if (((isPreciousMetal || legCcy==0 )?pymtDateFixed:pymtDate) < m_today) {
//					continue;
//				}

				int fixedUnfixedStatus = MTL_Position_Enums.FixedUnfixed.UNFIXED;

				if ((resetStatus == VALUE_STATUS_ENUM.VALUE_KNOWN.toInt()) || (resetDate < m_today))
				{
					fixedUnfixedStatus = MTL_Position_Enums.FixedUnfixed.FIXED;
				}
				else if (resetDate == m_today)
				{
					fixedUnfixedStatus = getFixedUnfixedStatus(projIdx, resetDate, resetRfisDate, refSource);
				}

				// If the reset raw value is zero, and it is in the past, it is possible it is not being populated
				// by core system - retrieve historical price from idx_historical_prices instead
				if ((Math.abs(resetRawValue) < 0.001) && (resetDate < m_today))
				{
					resetRawValue = MTL_Position_Utilities.getHistPrice(projIdx, resetDate, resetRfisDate, refSource);
				}

				double fxFactor = 1.0;
				
				// Normally, the "Metal Price Spread" is the same for every reset, but if the projection index
				// is in non-USD, we need to adjust it by the FX rate associated with each individual FX reset
				// so set up a reset-level "Metal Price Spread" variable to store the per-reset value
				double resetIdxCcySpread = idxCcySpd; 				

				// If this leg's currency is not USD, set the appropriate FX factor to convert it to USD
				if (legCcy != s_USD) {
					if (projIdxCcy != s_USD) {
						// If the underlying index is not in USD, convert it based on either the historical price of its default FX index
						// (use LBMA reference source, as it is LBMA deals who price off such indexes as XAU.EUR), or just the forward FX
						// rate associated with the reset date
						Logging.info("Processing non-USD index for date: " + OCalendar.formatDateInt(resetDate) + "\n");
						if (resetDate < m_today)
						{								
							fxFactor = MTL_Position_Utilities.getHistPrice(defaultFXIndex, resetDate, resetRfisDate, s_LBMA_RefSource);							
						}
						else
						{								
							fxFactor = getForwardRate(legCcy, resetRfisDate);
						}

						Logging.info("FX Factor: " + fxFactor + ", reset raw value: " + resetRawValue + "\n");

						// We need to normalise the reset raw value to convert to USD
						if (Math.abs(fxFactor) < 0.001)
						{
							fxFactor = 1.0;
						}

						resetRawValue = resetRawValue * fxFactor;

						// Set the "FX" reference source to be LBMA, since that is what the EUR curve prices off
						fxRefSource = s_LBMA_RefSource;

						// Convert the spread into USD from original currency
						resetIdxCcySpread = idxCcySpd * fxFactor; 
					}						
					else if (ccyConversionMethod == fixedCcyConvMethod)
					{
						// Underlying index in USD, fixed conversion ratio from the deal
						fxFactor = fixedFXRate;
					}
					else if (ccyConversionMethod == resetCcyConvMethod)
					{
						// Underlying index in USD, each reset converts independently							
						if (resetDate <= m_today)
						{
							fxFactor = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_SPOT_CONV.toInt(), param, "", j);
							if (fxFactor > 0.0)
							{
								fxFactor = 1 / fxFactor;
							}
						}
						else
						{
							// Retrieve the FX reset RFIS date, as that is the date of the FX conversion to use, if available
							int fxRfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_SPOT_RATE_RFIS_DATE.toInt(), param, "", j);
							if (fxRfisDate > 0) {
								fxFactor = getForwardRate(legCcy, resetRfisDate);
							}
							else {
								// Fall back on reset date if not found
								fxFactor = getForwardRate(legCcy, resetDate);
							}														
						}							
					}
					else {
						// Underlying index in USD, single FX conversion based on payment date							
						fxFactor = getForwardRate(legCcy, pymtDate);
					}						
				}

				// Add the "FX rate spread" to the calculated FX rate - note that we add it to the "USD per currency" value
				// which is the inverse of FX factor variable we have here
				if (Math.abs(fxRateSpd) > 0.0001) {
					double backSolveFXFactor = 1 / fxFactor;
					backSolveFXFactor += fxRateSpd;
					fxFactor = 1 / backSolveFXFactor;																		
				}

				// Take raw value (now converted to US dollars), and apply the leg-level index multiplier
				// The core leg spread is in foreign currency, so apply FX factor to it
				// Index currency spread is either in USD, or we have pre-converted it to USD already
				double usdPrice = resetRawValue * legMult + legSpd * fxFactor + resetIdxCcySpread;
				if (floatingLegWeight > 0) {
					usdPrice /= floatingLegWeight;
				}

				// Create data for the Precious Metal entry
				SwapsData data = new SwapsData();
				data.m_dealNum = dealNum;
				data.m_dealLeg = param;
				data.m_dealPdc = dealPdc;
				data.m_dealReset = j;

				data.m_resetDate = resetDate;
				data.m_rfisDate = resetRfisDate;
				data.m_fixedUnfixedStatus = fixedUnfixedStatus;
				data.m_position = -1 * resetNotional * floatingLegWeight * unitConversionRatio;
				data.m_usdTradePrice = usdPrice;

				data.m_projIdx = projIdx;
				data.m_refSource = refSource;
				int ccy = MTL_Position_Utilities.getCcyForIndex(projIdx);
				data.m_ccy = ccy;
				data.m_pymtDate = pymtDateFixed;
				// it's possible the data above has already been generated, so check to
				// avoid duplicate rows.
				if (!resets.contains(data)) { 
					resets.add(data);						
				}
			} // end of reset periods loop
		} // end of param loop
	}

	// Assume we are only ever going to need to retrieve today's prices for now
	private void initHistPrices() throws OException
	{
		m_histPricesData = Table.tableNew("Historical Prices");

		int ret;
		
		do {
			ret = DBaseTable.execISql(m_histPricesData, "SELECT * FROM idx_historical_prices WHERE reset_date = '" + OCalendar.formatJdForDbAccess(m_today) + "'");	
			if (ret == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Retryable error exeucting SQL to retrieve historical prices");
				Logging.warn (errorMessage);
			}
		} while (ret == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt());

		m_histPricesData.group("index_id, ref_source");

		m_histPricesReady = true;
	}

	private void clearHistPrices() throws OException {
		if (m_histPricesData != null && Table.isTableValid(m_histPricesData) == 1) {
			m_histPricesData.destroy();
			m_histPricesData = null;
			m_histPricesReady = false;
		}
	}

	private int getFixedUnfixedStatus(int projIdx, int resetDate, int resetRfisDate, int refSource) throws OException 
	{		
		int fixedUnfixedStatus = MTL_Position_Enums.FixedUnfixed.UNFIXED;
		if (!m_histPricesReady)
		{
			initHistPrices();
		}

		int firstIdxRow = m_histPricesData.findInt("index_id", projIdx, SEARCH_ENUM.FIRST_IN_GROUP);
		int lastIdxRow = m_histPricesData.findInt("index_id", projIdx, SEARCH_ENUM.LAST_IN_GROUP);

		if (firstIdxRow > 0)
		{
			for (int row = firstIdxRow; row <= lastIdxRow; row++)
			{
				int rowRfisDate = m_histPricesData.getInt("start_date", row);
				int rowRefSource = m_histPricesData.getInt("ref_source", row);

				if ((resetRfisDate == rowRfisDate) && (refSource == rowRefSource))
				{
					fixedUnfixedStatus = MTL_Position_Enums.FixedUnfixed.FIXED;
					break;
				}
			}
		}

		return fixedUnfixedStatus;
	}


	private Table generateCallNotDataTable(Table tranResults, Table transData) throws OException
	{
		Table workData = createOutputTable();		

		// Retrieve all Call Notice deals
		workData.select(transData, "deal_num", "toolset EQ 35");		

		// If no CallNot deals, exit
		if (workData.getNumRows() < 1)
		{
			return workData;
		}

		// Enrich all relevant legs and profile periods with appropriate position
		String tranSelectResults = "deal_num, deal_leg, disc_idx(proj_idx), currency_id(metal_ccy), " + PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT.toInt() + "(position)";
		workData.select(tranResults, tranSelectResults, "deal_num EQ $deal_num");
		int rows = workData.getNumRows();
		StringBuilder dealNums = new StringBuilder ();
		Set<Integer> dealTrackingNumsUnique = new HashSet<>();
		for (int row = 1; row <= rows; row++) {
			int dealTrackingNum = workData.getInt("deal_num", row);
			dealTrackingNumsUnique.add(dealTrackingNum);
			if (dealNums.length() == 0) {
				dealNums.append(dealTrackingNum);
			} else {
				dealNums.append(",").append(dealTrackingNum);				
			}
		}
		
		Table positionTable = retrieveCallNoticePositions(dealNums);
		Table adjustmentsTable = retrieveCallNoticeAdjustments(dealNums);
		int rows2 = positionTable.getNumRows();
		for (int row = 1; row <= rows; row++) {
			int dealNum = workData.getInt("deal_num", row);
			int currencyId = workData.getInt("metal_ccy", row);
			if (!MTL_Position_Utilities.isPreciousMetal(currencyId)) {
				continue;
			}
			for (int row2 = 1; row2 <= rows2; row2++) {
				int dealNumPositionTable = positionTable.getInt("deal_num", row2);
				if (dealNumPositionTable == dealNum) {
					double position = positionTable.getDouble("position", row2);					
					double adjustment = 0.0;
					for (int row3 = adjustmentsTable.getNumRows(); row3 >= 1; row3--) {
						int dealNumAdjustmentTable = adjustmentsTable.getInt("deal_num", row3);
						if (dealNumAdjustmentTable == dealNum) {
							adjustment = adjustmentsTable.getDouble("position", row3);
							break;
						}
					}
					workData.setDouble("position", row, position + adjustment);
					break;
				}
			}
		}

		positionTable = TableUtilities.destroy(positionTable);
		adjustmentsTable = TableUtilities.destroy(adjustmentsTable);
		// Set trade price from the spot rate, set trade value as position X price
	
		
		for (int row = 1; row <= rows; row++)
		{
			int fxIndexID = MTL_Position_Utilities.getDefaultFXIndexForCcy(workData.getInt("metal_ccy", row));
			if (fxIndexID > 0)
			{
				double spotPrice = MTL_Position_Utilities.getSpotGptRate(fxIndexID);
				workData.setDouble("usd_trade_price", row, spotPrice);
			}			
		}		
		
		workData.mathMultCol("usd_trade_price", "position", "usd_trade_value");		

		// Set value to "fixed" for stock
		workData.setColValInt("fixed_unfixed", MTL_Position_Enums.FixedUnfixed.FIXED);

		// Set all dates to current date
		workData.setColValInt("cflow_date", m_today);
		workData.setColValInt("pricing_start_date", m_today);
		workData.setColValInt("pricing_end_date", m_today);
		workData.setColValInt("pricing_rfis_start_date", m_today);
		workData.setColValInt("pricing_rfis_end_date", m_today);

		// Delete anything which is still USD
		workData.deleteWhereValue("metal_ccy", 0);		

		return workData;
	}

	public Table retrieveCallNoticePositions(StringBuilder dealNums) throws OException {
		String sql = 
				"\nSELECT ab.deal_tracking_num AS deal_num,naph.account_id, naph.currency_id, naph.portfolio_id, naph.report_date,  SUM(naph.position) AS position"
			+	"\nFROM nostro_account_position_hist naph"
			+	"\n  INNER JOIN ab_tran ab"
			+	"\n    ON ab.internal_portfolio = naph.portfolio_id AND naph.currency_id = ab.currency"
			+   "\n  INNER JOIN call_notice c"
			+   "\n    ON c.ins_num = ab.ins_num AND naph.account_id = c.account_id"
			+	"\n  INNER JOIN configuration conf"
  			+   "\n    ON naph.report_date < conf.business_date"
			+	"\nWHERE  ab.tran_num IN (" + dealNums.toString() + ")"
			+	"\nGROUP BY ab.deal_tracking_num,naph.account_id, naph.currency_id, naph.portfolio_id, naph.report_date"
			+	"\nORDER BY naph.report_date DESC";
		
		Table positionTable = Table.tableNew(sql);
		try {
			Logging.info("Executing SQL:" + sql);
			int ret = DBaseTable.execISql(positionTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException ("Could not execute SQL " + sql);
			}
			return positionTable;			
		} catch (Exception ex) {
			Logging.error("Could not execute SQL " + sql);
			Logging.error("Exception:" + ex.toString());			
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
			positionTable = TableUtilities.destroy(positionTable);
			throw new RuntimeException ("Could not execute SQL " + sql);
		}
	}
	
	public Table retrieveCallNoticeAdjustments(StringBuilder dealNums) throws OException {
		String sql = 
				"\nSELECT ab.deal_tracking_num AS deal_num,naph.account_id, naph.currency_id, naph.portfolio_id, SUM(naph.position) AS position"
			+	"\nFROM nostro_account_position_adj naph"
			+	"\nINNER JOIN ab_tran ab"
			+	"\nON ab.internal_portfolio = naph.portfolio_id AND naph.currency_id = ab.currency"
			+   "\nINNER JOIN call_notice c"
			+	"\nON c.ins_num = ab.ins_num AND naph.account_id = c.account_id"
			+   "\nINNER JOIN configuration conf"
			+   "\nON naph.official_system_date = conf.business_date"
			+	"\nWHERE  ab.tran_num IN (" + dealNums.toString() +")" // AND naph.reverse_flag = 1"
			+	"\nGROUP BY ab.deal_tracking_num,naph.account_id, naph.currency_id, naph.portfolio_id"
			;
		
		Table positionTable = Table.tableNew(sql);
		try {
			Logging.info("Executing SQL:" + sql);
			int ret = DBaseTable.execISql(positionTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException ("Could not execute SQL " + sql);
			}
			return positionTable;			
		} catch (Exception ex) {
			Logging.error("Could not execute SQL " + sql);
			Logging.error("Exception:" + ex.toString());			
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
			positionTable = TableUtilities.destroy(positionTable);
			throw new RuntimeException ("Could not execute SQL " + sql);
		}
	}

	// Generate a table with Transaction pointers, and any Tran-level data (so that we 
	private Table prepareTransactionsData(Table trans) throws OException
	{
		Table workData = trans.cloneTable();

		workData.addCol("base_ins_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("toolset", COL_TYPE_ENUM.COL_INT);	
		workData.addCol("fx_pricing_date", COL_TYPE_ENUM.COL_INT);	
		workData.addCol("fut_expiry_date", COL_TYPE_ENUM.COL_INT);
		workData.select(trans, "*", "deal_num GE 0");

		int numRows = workData.getNumRows();

		for (int row = 1; row <= numRows; row++) 
		{
			Transaction trn = workData.getTran("tran_ptr", row);

			int baseInsType = Instrument.getBaseInsType(trn.getInsType());
			int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());

			workData.setInt("base_ins_type", row, baseInsType);
			workData.setInt("toolset", row, toolset);

			if (baseInsType == INS_TYPE_ENUM.fx_instrument.toInt())
			{
				int fxPricingDate = trn.getFieldInt(TRANF_FIELD.TRANF_FX_DATE.toInt(), 0);

				if (fxPricingDate < 1)
				{
					String fxPricingDateStr = trn.getField(TRANF_FIELD.TRANF_FX_DATE.toInt(), 0);

					fxPricingDate = OCalendar.parseString(fxPricingDateStr);
				}

				workData.setInt("fx_pricing_date", row, fxPricingDate);
			}
			else if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
			{
				int expiryDate = trn.getFieldInt(TRANF_FIELD.TRANF_EXPIRATION_DATE.toInt(), 0);

				workData.setInt("fut_expiry_date", row, expiryDate);				
			}
		}

		return workData;
	}	

	protected Table createOutputTable() throws OException
	{
		Table workData = new Table("Metal Position UDSR");

		workData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);

		workData.addCol("proj_idx", COL_TYPE_ENUM.COL_INT);

		workData.addCol("position", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("usd_trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("usd_trade_value", COL_TYPE_ENUM.COL_DOUBLE);

		workData.addCol("cflow_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_start_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_end_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_rfis_start_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_rfis_end_date", COL_TYPE_ENUM.COL_INT);		
		workData.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		workData.addCol("position_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("fixed_unfixed", COL_TYPE_ENUM.COL_INT);

		workData.addCol("ref_source", COL_TYPE_ENUM.COL_INT);

		return workData;		
	}

	private static Table s_fixedUnfixed = null;
	private static Table s_positionType = null;

	protected void format(Table argt, Table returnt) throws OException 
	{
		returnt.setColTitle("deal_num", "Deal Num");
		returnt.setColTitle("deal_leg", "Deal Leg");
		returnt.setColTitle("deal_pdc", "Deal Profile");		
		returnt.setColTitle("deal_reset_id", "Deal Reset ID");

		returnt.setColTitle("proj_idx", "Index");

		returnt.setColTitle("position", "Position");
		returnt.setColTitle("usd_trade_price", "USD Trade Price");
		returnt.setColTitle("usd_trade_value", "USD Trade Value");

		returnt.setColTitle("cflow_date", "Settle Date");
		returnt.setColTitle("pricing_start_date", "Pricing Start Date");
		returnt.setColTitle("pricing_end_date", "Pricing End Date");
		returnt.setColTitle("pricing_rfis_start_date", "Pricing RFIS\nStart Date");
		returnt.setColTitle("pricing_rfis_end_date", "Pricing RFIS\nEnd Date");		
		returnt.setColTitle("metal_ccy", "Metal \\ Currency");
		returnt.setColTitle("position_type", "Metal \\ Currency\nType");
		returnt.setColTitle("fixed_unfixed", "Fixed \\ Unfixed");
		returnt.setColTitle("ref_source", "Ref Source");

		returnt.setColFormatAsRef("proj_idx", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsRef("ref_source", SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);

		returnt.setColFormatAsDate("cflow_date");
		returnt.setColFormatAsDate("pricing_start_date");
		returnt.setColFormatAsDate("pricing_end_date");
		returnt.setColFormatAsDate("pricing_rfis_start_date");
		returnt.setColFormatAsDate("pricing_rfis_end_date");		

		returnt.setColFormatAsNotnl("position", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsNotnl("usd_trade_price", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsNotnl("usd_trade_value", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		if (s_fixedUnfixed == null)
		{
			s_fixedUnfixed = new Table("USER_mtl_fixed_unfixed");
			s_positionType = new Table("USER_mtl_position_type");

			DBUserTable.load(s_fixedUnfixed);
			DBUserTable.load(s_positionType);
		}

		returnt.setColFormatAsTable("fixed_unfixed", s_fixedUnfixed);
		returnt.setColFormatAsTable("position_type", s_positionType);

		// Group Table
		returnt.group("deal_num, deal_leg, deal_pdc, proj_idx, deal_reset_id");		
	}
}
