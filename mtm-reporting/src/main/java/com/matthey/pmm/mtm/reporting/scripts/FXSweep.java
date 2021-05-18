package com.matthey.pmm.mtm.reporting.scripts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.io.EnumQueryType;
import com.olf.openrisk.io.Queries;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.market.EnumBmo;
import com.olf.openrisk.simulation.ConstGeneralResult;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

import ch.qos.logback.classic.Logger;

/*
 * History:
 * 2020-12-17	V1.0	jwaechter	- Initial Version
 */

/**
 * This plugins is supposed to run as a main plugin in a task.
 * It executes the following functionality:
 * <ol>
 *   <li>
 *      Retrieve the variable '{@value #VAR_START_TRADE_DATE}' from the Constants Repository.
 *      --->Do not continue if the current trading date is before the retrieved {@value #VAR_START_TRADE_DATE}.
 *      Verify above.
 *   </li>
 *   <li>
 *      Retrieve the name of a named query defined for variable '{@value #VAR_QUERY}' from 
 *      Constants Repository
 *   </li>
 *   <li>
 *      Execute the named query and retrieve the results removing trades booked in portfolios
 *      not having a defined FX Portfolio and deals having all resets before the one defined in
 *      '{@value #VAR_START_TRADE_DATE}'.
 *   </li>
 *   <li>
 *      Find previously created FX Sweep deals for the deals queried from the named query before.
 *   </li>
 *   <li>
 *      Run the MTL position report for the deals in question.
 *   </li>
 *   <li>
 *   	Group the MTL positions by portfolio, currency, FX date, deal num queried by named query.
 *   </li>
 *   <li>
 *   	Ignore zero position rows showing up after grouping.
 *   </li>
 *   <li>
 *   	Book an FX sweep deal for each deal retrieved by the named query for the open position and
 *      fill the tran info field {@link #TRAN_INFO_HEDGE_SOURCE} with the deal tracking num of the
 *      deal retrieved by the named query.
 *   </li>
 * </ol>
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class FXSweep extends EnhancedGenericScript {
	// Const Repository
	private static final String CONTEXT = "MtmReporting";
	private static final String SUBCONTEXT = "FxSweep";
	private static final String VAR_QUERY = "SavedQueryName";
	private static final String DEFAULT_QUERY = "FX Sweep Input Deals";
	private static final String VAR_START_TRADE_DATE = "StartTradeDate";
	private static final String DEFAULT_START_DATE = "2020-01-01";
	private static final String VAR_TEMPLATE_REFERENCE = "TemplateReference";
	private static final String DEFAULT_TEMPLATE_REFERENCE = "UK Spot & Forward";

	// Tran info fields
	private static final String TRAN_INFO_HEDGE_SOURCE = "Hedge Source";
	private static final String TRAN_INFO_TRADE_PRICE = "Trade Price";
	
	// portfolio info fields
	private static final String PFOLIO_INFO_TARGET_FX_PORTFOLIO = "Target FX Portfolio";
	private static final String PFOLIO_INFO_BASE_CURRENCY = "Portfolio Base Currency";

	// simulation definition names
	private static final String SIM_NAME_MTL_POSITION = "MTL Position";	

	
	private static final Logger logger = EndurLoggerFactory.getLogger(FXSweep.class);
	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private static final double EPSILON = 0.01d;
	
	private ConstRepository constRepo = null;
	private String savedQueryName;
	private String startDate;
	private Date startDateAsDate;
	private int templateTransactionId;
	private String templateReference; 
	
	@Override
	protected void run(Context context, ConstTable table) {
		Table inputDealList = null;
		Table existingFxSweepDealList = null;
		Table mtlPositionData = null;
		Table summaryTable = null;
		try {
			init();
			logger.info("Starting FX Sweep Process");
			retrieveTemplateTransactionId(context);
			logger.info("Retrieved template transaction ID");
			 inputDealList = executeNamedQuery(context);
			if (inputDealList.getRowCount() == 0) {
				logger.info("No input deals found. Exiting");
				return;
			}
			logger.info("Successfully retrieved inputDealList");
			existingFxSweepDealList = findExistingFxSweepDeals (context, inputDealList);
			logger.info("Successfully retrieved existing FX Sweep Deals");
			mtlPositionData = runMtlPositionUdsr (context, existingFxSweepDealList, inputDealList);
			if (mtlPositionData == null) {
				return;
			}
			logger.info("Successfully run MTL Position UDSR");
			summaryTable = createSummary(context, mtlPositionData, existingFxSweepDealList, inputDealList);
			logger.info("Successfully calculated Summary Table");
			createFXSweepDeals (context, summaryTable);
			logger.info("Successfully created FX Sweep deals");			
			logger.info("FX Sweep Process finished successfully");
		} catch (Throwable t) {
			logger.error("Error while executing FXSweep: " + t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				logger.error(ste.toString());
			}
			throw t;
		} finally {
			if (inputDealList != null) {
				inputDealList.dispose();
			}
			if (existingFxSweepDealList != null) {
				existingFxSweepDealList.dispose();
			}
			if (mtlPositionData != null) {
				mtlPositionData.dispose();
			}
			if (summaryTable != null) {
				summaryTable.dispose();
			}
		}		
	}

	

	private void retrieveTemplateTransactionId(Session session) {
		String sql = 
				"\nSELECT ab.tran_num"
			+   "\nFROM ab_tran ab"
			+   "\nWHERE ab.reference = '" + templateReference + "'"
			+   "\n  AND ab.current_flag = 1"
			+   "\n  AND ab.tran_status = " + EnumTranStatus.Template.getValue()
			;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			if (sqlResult.getRowCount() == 0) {
				String errorMessage = "Error retrieving template transaction having reference '" 
						+ templateReference + "' that has to be in status 'Template'";
				logger.error(errorMessage);
				throw new RuntimeException (errorMessage);
			}
			templateTransactionId = sqlResult.getInt(0, 0);
		} catch (Exception ex) {
			String errorMessage = "Error executing SQL:\n" + sql + "\n " + ex.toString();
			logger.error(errorMessage);
			throw ex;
		}
	}

	private void createFXSweepDeals(Session session, Table summaryTable) {
		SymbolicDate sd = session.getCalendarFactory().createSymbolicDate("2d");
		Date businessDate = session.getBusinessDate();
		for (int row=summaryTable.getRowCount()-1; row >= 0; row--) {
			int inputDealTrackingNum = summaryTable.getInt ("input_deal_tracking_num", row);
			String pfolioBaseCurrency = summaryTable.getString("pfolio_base_currency", row);
			String targetFxPortfolioName = summaryTable.getString("target_fx_portfolio_name", row);
			double positionSum = summaryTable.getDouble ("position_sum", row);
			Transaction inputTran = session.getTradingFactory().retrieveTransactionByDeal(inputDealTrackingNum);
			Date settlementDate = summaryTable.getValueAsDate("cflow_date", row);
			String baseCurrency = getBaseCurrency (inputDealTrackingNum, summaryTable);
			String termCurrency = pfolioBaseCurrency;
			String tradePrice = getTradePrice (session, baseCurrency, termCurrency, settlementDate );
			Date tradeDate = businessDate;
			Date tradeDatePlus2Gbd = sd.evaluate(tradeDate);
			Date fxDate = settlementDate;
			String cashFlowType = session.getCalendarFactory().getJulianDate(fxDate) - session.getCalendarFactory().getJulianDate(tradeDatePlus2Gbd)>0?
					"Forward":"Spot";
			String buySell = positionSum >= 0?"Sell":"Buy";
			double amount=Math.abs(positionSum);
			String reference = "FX Sweep";
			String interBu = inputTran.getDisplayString(EnumTransactionFieldId.InternalBusinessUnit);
			String interPfolio = summaryTable.getString("pfolio_name", row);
			String trader = session.getUser().getName();
			String externalBu = inputTran.getDisplayString(EnumTransactionFieldId.InternalBusinessUnit);
			String externalPfolio = targetFxPortfolioName;
			String metalDeal = "" + inputDealTrackingNum;
			try (Transaction sweep = session.getTradingFactory().createTransactionFromTemplate(templateTransactionId)) {
				try {
					sweep.setValue(EnumTransactionFieldId.Ticker, baseCurrency + "/" + termCurrency);					
				} catch (Exception ex) {
					sweep.setValue(EnumTransactionFieldId.Ticker, termCurrency + "/" + baseCurrency);										
				}
				sweep.setValue(EnumTransactionFieldId.CashflowType, cashFlowType);
				sweep.setValue(EnumTransactionFieldId.FxBaseCurrency, baseCurrency);
				sweep.setValue(EnumTransactionFieldId.FxTermCurrency, termCurrency);
				sweep.setValue(EnumTransactionFieldId.BuySell, buySell);
				sweep.setValue(EnumTransactionFieldId.FxDealtAmount, amount);
//				sweep.setValue(EnumTransactionFieldId.TradeDate, businessDate);
				sweep.setValue(EnumTransactionFieldId.FxDate, settlementDate);
				sweep.setValue(EnumTransactionFieldId.SettleDate, settlementDate);
				sweep.setValue(EnumTransactionFieldId.FxTermSettleDate, settlementDate);
				sweep.getField(TRAN_INFO_TRADE_PRICE).setValue(tradePrice);
				sweep.setValue(EnumTransactionFieldId.ReferenceString, reference);
				sweep.setValue(EnumTransactionFieldId.InternalBusinessUnit, interBu);
				sweep.setValue(EnumTransactionFieldId.InternalPortfolio, interPfolio);
				sweep.setValue(EnumTransactionFieldId.InternalContact, trader);
				sweep.setValue(EnumTransactionFieldId.ExternalBusinessUnit, externalBu);
				sweep.setValue(EnumTransactionFieldId.ExternalPortfolio, externalPfolio);
				sweep.getField(TRAN_INFO_HEDGE_SOURCE).setValue(metalDeal);
				try {
					sweep.process(EnumTranStatus.Validated);
					logger.info("Created new FX Sweep deal #" + sweep.getDealTrackingId() 
						+ " for input deal #" + metalDeal);
				} catch (Exception ex) {
					logger.error("Error processing new FX Sweep deal to status validated: " + ex.toString());
					for (StackTraceElement ste : ex.getStackTrace()) {
						logger.error(ste.toString());
					}
				}
			} catch (Exception ex) {
				logger.error("Error setting field on new FX Sweep deal: " + ex.toString());
				for (StackTraceElement ste : ex.getStackTrace()) {
					logger.error(ste.toString());
				}
				throw ex;
			}
		}
	}


	/**
	 * @param session
	 * @param termCurrency 
	 * @param baseCurrency 
	 * @return
	 */
	private String getTradePrice(Session session, String baseCurrency, String termCurrency, Date settleDate) {
		session.getMarket().loadUniversal();
		Currency baseCur = session.getStaticDataFactory().getReferenceObject(Currency.class, baseCurrency);
		Currency termCur = session.getStaticDataFactory().getReferenceObject(Currency.class, termCurrency);
		
		double rate = session.getMarket().getFXRate(baseCur, termCur, settleDate,  EnumBmo.Mid);
		String message = "Retrieved rate for " + baseCurrency + " to " + termCur
				+ " and date" + settleDate + " is: " + rate;
		logger.info(message);
		session.getDebug().logLine(message);
		if (rate == 0.0d) {
			logger.info("Retrieved rate for " + termCur + " to " + baseCur + " is: " + rate);
			rate = 1.0d/session.getMarket().getFXRate(termCur, baseCur, settleDate,  EnumBmo.Mid);			
		} 
		if (termCurrency.equalsIgnoreCase("USD") && !isReversed (session, baseCurrency)) {
			rate = 1.0d/rate;
		}
		
	    BigDecimal bd = new BigDecimal(Double.toString(rate));
	    bd = bd.setScale(6, RoundingMode.HALF_UP);
		return bd.toString();
	}

	private boolean isReversed(Session session, String currency) {
		String sql = 
				"\nSELECT convention FROM currency WHERE name = '" + currency + "'"
				;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			if (sqlResult.getRowCount() == 0) {
				String message = "Currency '" + currency + "' not found ";
				logger.error(message);
				throw new RuntimeException (message);
			}
			return sqlResult.getInt(0, 0) == 1;
		} catch (Exception ex) {
			String message = "\nError executing SQL " + sql + "\n" + ex.toString();
			logger.error(message);
			throw ex;
		}
	}

	private String getBaseCurrency(int inputDealTrackingNum, Table summaryTable) {
		int row = summaryTable.find(summaryTable.getColumnId("input_deal_tracking_num"), inputDealTrackingNum, 0);
		return summaryTable.getDisplayString(summaryTable.getColumnId("metal_ccy"), row);
	}
	
	/**
	 * Calculates the summary table for each input deal hit by the query defined in 
	 * Constants Repository in variable {@value #VAR_QUERY} that are having a non zero
	 * balance taking into account the FX deals already booked.
	 * @param session
	 * @param mtlPositionData The result of the MTL Position UDSR for both Input and FX Sweep deals  
	 * @param existingFxSweepDealList The list of FX Sweep deals that belong to the input deals.
	 * @param inputDealList The list of input deals and additional information.
	 * @return Table having the following columns:
	 * <ol>
	 *   <li>
	 *     input_deal_tracking_num, integer, deal tracking num of the input deal
	 *   </li>
	 *   <li>
	 *     pfolio_base_currency, String, currency name of the portfolio base currency of the portfolio
	 *   </li>
	 *   <li>
	 *     pfolio_name, String, name of the portfolio the deal is being booked in
	 *   </li>
	 *   <li>
	 *     target_fx_portfolio_name, String, name of the portfolio to store the FX deals belong to the deal
	 *     denoted by input_deal_tracking_num
	 *   </li>
	 *   <li>
	 *     cflow_date, int, Julian Date when the settlement for the deal in question happens.
	 *   </li>
	 *   <li>
	 *     metal_ccy, int, Julian Date when the settlement for the deal in question happens.
	 *   </li>
	 *   <li>
	 *     position_sum, double, sum of the balance of the input deal denoted by 
	 *     input_deal_tracking_num and the FX deals belonging to it.
	 *   </li>
	 * </ol>
	 */
	private Table createSummary(Session session, Table mtlPositionData, Table existingFxSweepDealList, Table inputDealList) {
		Table tempTable = mtlPositionData.cloneData();
		tempTable.addColumn("tran_status", EnumColType.Int);
		tempTable.addColumn("input_deal_tracking_num", EnumColType.Int);
		tempTable.addColumn("pfolio_base_currency", EnumColType.String);
		tempTable.addColumn("pfolio_name", EnumColType.String);
		tempTable.addColumn("target_fx_portfolio_name", EnumColType.String);
		tempTable.copyColumnData(tempTable.getColumnId("deal_num"), tempTable.getColumnId("input_deal_tracking_num"));
		
		int colNumDealTrackingNum = inputDealList.getColumnId("deal_tracking_num");
		int colNumDealTrackingNumSweep = existingFxSweepDealList.getColumnId("fx_deal_tracking_num");
		int colNumInputDealTrackingNumSweep = existingFxSweepDealList.getColumnId("input_deal_tracking_num");
		inputDealList.sort(colNumDealTrackingNum, true);
		existingFxSweepDealList.sort(colNumDealTrackingNumSweep, true);

		for (int row=tempTable.getRowCount()-1; row>=0; row--) {
			int dealNum = tempTable.getInt("deal_num", row);
			int rowFxSweep = existingFxSweepDealList.find(colNumDealTrackingNumSweep, dealNum, 0);
			if (rowFxSweep < 0) {
				continue;
			}
			int inputDealTrackingNum = existingFxSweepDealList.getInt ("input_deal_tracking_num", rowFxSweep);
			tempTable.setInt("input_deal_tracking_num", row, inputDealTrackingNum);
			int tranStatus = existingFxSweepDealList.getInt ("fx_tran_status", rowFxSweep);
			tempTable.setInt("tran_status", row, tranStatus);
		}

		existingFxSweepDealList.sort(colNumInputDealTrackingNumSweep, true);
		for (int row=tempTable.getRowCount()-1; row>=0; row--) {
			int fixedUnfixed = tempTable.getInt("fixed_unfixed", row);
			int positionType = tempTable.getInt("position_type", row);
			if (fixedUnfixed != 0 || positionType != 0) { // 0 = fixed / currency
				tempTable.removeRow(row);
				continue;
			}
			int dealTrackingNum = tempTable.getInt("deal_num", row);
			int inputDealTrackingNum = tempTable.getInt("input_deal_tracking_num", row);
			int rowNumInputTable = inputDealList.findSorted(colNumDealTrackingNum, inputDealTrackingNum, 0);
			String fxPortfolioName = inputDealList.getString("target_fx_portfolio_name", rowNumInputTable);
			String ccy = inputDealList.getString("pfolio_base_currency", rowNumInputTable);
			int tranStatus = inputDealList.getInt ("tran_status", rowNumInputTable);

			String dealPortfolioName = inputDealList.getString("pfolio_name", rowNumInputTable);
			if (inputDealTrackingNum != dealTrackingNum) { // we are processing an FX Sweep deal
				int rowNumFxSweepTable = existingFxSweepDealList.find(colNumDealTrackingNumSweep, dealTrackingNum, 0);
				dealPortfolioName = existingFxSweepDealList.getString("pfolio_name", rowNumFxSweepTable);
				tranStatus = existingFxSweepDealList.getInt ("fx_tran_status", rowNumFxSweepTable);
			}
			tempTable.setString("pfolio_base_currency", row, ccy);
			tempTable.setString("target_fx_portfolio_name", row, fxPortfolioName);
			tempTable.setString("pfolio_name", row, dealPortfolioName);
			tempTable.setInt("tran_status", row, tranStatus);
		}
		for (int row=tempTable.getRowCount()-1; row>=0; row--) {
			int tranStatus = tempTable.getInt("tran_status", row);
			if (tranStatus == EnumTranStatus.Cancelled.getValue()) {
				tempTable.setDouble("position", row, 0d);
			}
		}
		
		Table summaryTable = tempTable.calcByGroup("input_deal_tracking_num, pfolio_base_currency, pfolio_name, target_fx_portfolio_name,cflow_date,metal_ccy,tran_status", "position");
		summaryTable.setColumnName(summaryTable.getColumnId("Sum position"), "position_sum");
		// remove zero sum rows
		for (int row=summaryTable.getRowCount()-1; row>=0; row--) {
			double positionSum = summaryTable.getDouble("position_sum", row);
			String pfolioName = summaryTable.getString("pfolio_name", row);
			String targetPfolioName = summaryTable.getString("target_fx_portfolio_name", row);
			if (Math.abs(positionSum) < EPSILON || pfolioName.equalsIgnoreCase(targetPfolioName)) {
				summaryTable.removeRow(row);
			}
		}
		summaryTable.getFormatter().setColumnFormatter("metal_ccy", summaryTable.getFormatter().createColumnFormatterAsRef(EnumReferenceTable.Currency));
		return summaryTable;
	}

	/**
	 * Runs the {@value #SIM_NAME_MTL_POSITION} simulation for the input deals retrieved by the saved query stored in
	 * Constants Repository {@value #VAR_QUERY} and the FX Sweep deals that have already been booked for those deals.
	 * @param session
	 * @param existingFxSweepDealList
	 * @param inputDealList
	 * @return Table containing the MTL position UDSR.
	 */
	private Table runMtlPositionUdsr(Session session, Table existingFxSweepDealList, Table inputDealList) {
		try (QueryResult qr = session.getIOFactory().createQueryResult(EnumQueryType.Transaction)) {
			int[] inputTranNumsList = inputDealList.getColumnValuesAsInt("tran_num");
			int[] fxTranNumsList = existingFxSweepDealList.getColumnValuesAsInt("fx_tran_num");
			if (inputTranNumsList.length == 0 && fxTranNumsList.length == 0) {
				return null;
			}

			qr.add(inputTranNumsList);
			qr.add(fxTranNumsList);
			SimulationFactory sf = session.getSimulationFactory();
			
			try (RevalSession rs = sf.createRevalSession(qr);
				 ResultType rt = sf.getResultType(SIM_NAME_MTL_POSITION);
				 Simulation sim = sf.createSimulation("temp");
				 Scenario scen = sf.createScenario("temp");) {
				scen.getResultTypes().add(rt);
				sim.addScenario(scen);
				try (SimResults simResults = sim.runLocally(qr)) {
					RevalResults rss = simResults.getScenarioResults("temp");
					if (rss.getGeneralResults().size() == 0) {
						logger.info(SIM_NAME_MTL_POSITION + " did not compute any results. Exiting");
						return null;
					}
					ConstGeneralResult mtlPosResult = rss.getGeneralResult(rt);
					return mtlPosResult.getConstTable().cloneData();
				} catch (Exception ex) {
					String errorMessage = "Error while running simulation '" + SIM_NAME_MTL_POSITION + "'";
					logger.error(errorMessage);
					throw new RuntimeException (errorMessage, ex);
				}
			} catch (Exception ex) {
				String errorMessage = "Error while setting up simulation '" + SIM_NAME_MTL_POSITION + "'";
				logger.error(errorMessage);
				throw new RuntimeException (errorMessage, ex);				
			}
		} catch (Exception ex) {
			String errorMessage = "Error while creating query for simulation '" + SIM_NAME_MTL_POSITION + "'";
			logger.error(errorMessage);
			throw new RuntimeException (errorMessage, ex);			
		}
	}
	
	/**
	 * Loads data about the FX Sweep deals belonging to the input deals
	 * being queried by the named query found in the Constants Repository
	 * variable '{@value #VAR_QUERY}'.
	 * @param session
	 * @param inputDealList
	 * @return table containing the following columns:
	 * <ol>
	 *   <li> 
	 *     fx_tran_num - transaction number of an FX Sweep deal referencing one of the input deals
	 *   </li>
	 *   <li> 
	 *     fx_deal_tracking_num - deal tracking number of an FX Sweep deal referencing one of the input deals
	 *   </li>
	 *   <li> 
	 *     fx_tran_status - tran_status of an FX Sweep deal referencing one of the input deals
	 *   </li>
	 *   <li> 
	 *     input_deal_tracking_num - deal tracking number of the input deal referenced by the
	 *     FX Sweep Deal identified by fx_tran_num and deal_tracking_num
	 *   </li>
	 *   <li> 
	 *     pfolio_name - name of the portfolio the FX Sweep deal is booked into.
	 *   </li>
	 * </ol>
	 */
	private Table findExistingFxSweepDeals(Session session, Table inputDealList) {
		int[] inputDealTrackingNumList = inputDealList.getColumnValuesAsInt("deal_tracking_num");		
		try (QueryResult qr = session.getIOFactory().createQueryResult(EnumQueryType.Transaction);) {
			qr.add(inputDealTrackingNumList);
			String sql =
				"\nSELECT DISTINCT ab2.tran_num AS fx_tran_num"
			+	"\n  ,ab2.deal_tracking_num AS fx_deal_tracking_num"
			+	"\n  ,ab2.tran_status AS fx_tran_status"
			+	"\n  ,qr.query_result AS input_deal_tracking_num"
			+   "\n  ,p.name AS pfolio_name"
			+   "\nFROM " + qr.getDatabaseTableName() + " qr"
			+ 	"\n  INNER JOIN portfolio_info_types pit1 "
			+ 	"\n    ON pit1.type_name = '" + PFOLIO_INFO_TARGET_FX_PORTFOLIO + "'"
			+   "\n  INNER JOIN tran_info_types tit"
			+   "\n    ON tit.type_name = '" + TRAN_INFO_HEDGE_SOURCE + "'"
			+   "\n  INNER JOIN ab_tran_info_view tiv"
			+   "\n    ON tiv.value = CONVERT (varchar, qr.query_result)"
			+   "\n      AND tiv.type_id = tit.type_id"
			+   "\n  INNER JOIN ab_tran ab1"
			+   "\n    ON ab1.tran_num = tiv.tran_num"
			+   "\n  INNER JOIN ab_tran ab2"
			+   "\n    ON ab2.deal_tracking_num = ab1.deal_tracking_num"
			+   "\n      AND ab2.current_flag = 1"
			+   "\n      AND ab2.tran_status = " + EnumTranStatus.Validated.getValue() 
			+   "\n  INNER JOIN portfolio p"
			+   "\n    ON p.id_number = ab2.internal_portfolio"
			+   "\n  LEFT OUTER JOIN portfolio_info pi1"
			+   "\n    ON pi1.portfolio_id = p.id_number"
			+   "\n      AND pi1.info_type_id = pit1.type_id"
			+   "\nWHERE qr.unique_id = " + qr.getId()
			+   "\n  AND ISNULL(pi1.info_value, ISNULL (pit1.default_value, '')) != ''"

				;
			try {
				Table sqlResult = session.getIOFactory().runSQL(sql);
				return sqlResult;
			} catch (Exception ex) {
				String errorMessage = "Error executing SQL :" + sql;
				logger.error(errorMessage);
				throw ex;
			}
		}
	}

	/**
	 * Creates a table of input deals based on the saved query {@link #savedQueryName}
	 * containing the following columns:
	 * <ol>
	 *   <li>  
	 *     tran_num  - transaction number of an input deal
	 *   </li>
	 *   <li>  
	 *     deal_tracking_num  - deal tracking number of an input deal
	 *   </li>
	 *   <li>  
	 *     tran_status  - ID of the transaction status of tran_num
	 *   </li>
	 *   <li>  
	 *     pfolio_name  - name of the portfolio the input deal is booked in
	 *   </li>
	 *   <li>  
	 *     pfolio_base_currency  - value of the portfolio info field '{@value #PFOLIO_INFO_BASE_CURRENCY}'
	 *     of the portfolio the input deal is booked in.
	 *   </li>
	 *   <li>  
	 *     target_fx_portfolio_id  - id of the portfolio designated in info field '{@value #PFOLIO_INFO_TARGET_FX_PORTFOLIO}'
	 *     of the portfolio the input deal is booked in.
	 *   </li>
	 *   <li>  
	 *     target_fx_portfolio_name  - name of the portfolio designated in info field '{@value #PFOLIO_INFO_TARGET_FX_PORTFOLIO}'
	 *     of the portfolio the input deal is booked in.
	 *   </li>
	 * </ol>
	 * @param session
	 * @return
	 */
	private Table executeNamedQuery(Session session) {
		try (Queries tranQueries = session.getIOFactory().getQueries(EnumQueryType.Transaction);
 			 Query namedQuery = tranQueries.getQuery(savedQueryName);) {
			if (namedQuery == null) {
				String errorMessage = "The query '" + savedQueryName + "' is either not defined "
						+ "or not of type transaction";
				logger.error(errorMessage);
				throw new RuntimeException (errorMessage);
			} 
			try (QueryResult qr = namedQuery.execute(true);) {
				String sql = 
						"\nSELECT DISTINCT ab.tran_num"
					+   "\n  ,ab.deal_tracking_num"
					+   "\n  ,ab.tran_status"
					+   "\n  ,p.name AS pfolio_name"
					+   "\n  ,ISNULL (pi2.info_value, ISNULL(pit2.default_value, '')) AS pfolio_base_currency"
					+   "\n  ,CONVERT(integer, ISNULL (pi1.info_value, ISNULL(pit1.default_value, '0'))) AS target_fx_portfolio_id"
					+   "\n  ,p2.name AS target_fx_portfolio_name"
					+	"\nFROM " + qr.getDatabaseTableName() + " qr"
					+ 	"\n  INNER JOIN portfolio_info_types pit1 "
					+ 	"\n    ON pit1.type_name = '" + PFOLIO_INFO_TARGET_FX_PORTFOLIO + "'"
					+ 	"\n  INNER JOIN portfolio_info_types pit2 "
					+ 	"\n    ON pit2.type_name = '" + PFOLIO_INFO_BASE_CURRENCY + "'"
					+   "\n  INNER JOIN ab_tran ab"
					+   "\n    ON ab.tran_num = qr.query_result"
					+   "\n  INNER JOIN portfolio p"
					+   "\n    ON p.id_number = ab.internal_portfolio"
					+   "\n  LEFT OUTER JOIN portfolio_info pi1"
					+   "\n    ON pi1.portfolio_id = p.id_number"
					+   "\n      AND pi1.info_type_id = pit1.type_id"
					+   "\n  LEFT OUTER JOIN portfolio_info pi2"
					+   "\n    ON pi2.portfolio_id = p.id_number"
					+   "\n      AND pi2.info_type_id = pit2.type_id"
					+   "\n  LEFT OUTER JOIN reset r "
					+   "\n    ON r.ins_num = ab.ins_num"
					+   "\n  INNER JOIN portfolio p2"
					+   "\n    ON CONVERT (varchar, p2.id_number) = ISNULL (pi1.info_value, ISNULL(pit1.default_value, ''))"
					+   "\n  LEFT OUTER JOIN ins_parameter ip "
					+   "\n    ON ip.ins_num = ab.ins_num"					
					+   "\nWHERE qr.unique_id = " + qr.getId()
					+   "\n  AND ISNULL(pi1.info_value, ISNULL (pit1.default_value, '')) != ''"
					+   "\n  AND (ISNULL (r.reset_date, '" + session.getCalendarFactory().getSQLString(startDateAsDate) + "') >= '" + session.getCalendarFactory().getSQLString(startDateAsDate) + "'"
					+   "\n       OR ab.toolset = " + EnumToolset.Fx.getValue() + ")"
					+   "\n  AND (ab.trade_date >= '" + session.getCalendarFactory().getSQLString(startDateAsDate) + "'"
					+   "\n       OR ab.toolset != " + EnumToolset.Fx.getValue() + ")"
					+   "\n  AND (ISNULL (ip.unit, -1) > 0"
					+   "\n       OR ab.toolset != " + EnumToolset.Fx.getValue() + ")"
						;
				try {
					logger.info(sql);
					Table sqlResult = session.getIOFactory().runSQL(sql);
					return sqlResult;
				} catch (Exception ex) {
					String errorMessage = "Error executing SQL :" + sql;
					logger.error(errorMessage);
					throw ex;
				}
			} catch (Exception ex) {
				String errorMessage = "Error executing saved query '" + savedQueryName + "'";
				logger.error(errorMessage);
				throw ex;
			}
		} catch (Exception ex) {
			String errorMessage = "The query '" + savedQueryName + "' is either not defined "
					+ "or not of type transaction";
			logger.error(errorMessage);
			throw ex;			
		}
	}

	private void init() {
		try {
			constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
		} catch (OException e) {
			String errorMessage = "Error initializing the constants repository '" +
					CONTEXT + "\\" + SUBCONTEXT + "'"; 
			logger.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}
		try {
			savedQueryName = constRepo.getStringValue(VAR_QUERY, DEFAULT_QUERY);
			logger.info("Using saved query name '" + savedQueryName + "'");
		} catch (OException e) {
			String errorMessage = "Error retrieving saved query name from ConstRepo '" +
					CONTEXT + "\\" + SUBCONTEXT + "\\" + VAR_QUERY + "'";
			logger.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}
		try {
			templateReference = constRepo.getStringValue(VAR_TEMPLATE_REFERENCE, DEFAULT_TEMPLATE_REFERENCE);
			logger.info("Using template reference  '" + templateReference + "'");
		} catch (OException e) {
			String errorMessage = "Error retrieving template reference from ConstRepo '" +
					CONTEXT + "\\" + SUBCONTEXT + "\\" + VAR_TEMPLATE_REFERENCE + "'";
			logger.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}

		try {
			startDate = constRepo.getStringValue(VAR_START_TRADE_DATE, DEFAULT_START_DATE);
			logger.info("Using start date '" + startDate + "'");
		} catch (OException e) {
			String errorMessage = "Error retrieving start date from ConstRepo '" +
					CONTEXT + "\\" + SUBCONTEXT + "\\" + VAR_START_TRADE_DATE + "'";
			logger.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
		try {
			startDateAsDate = sdf.parse(startDate);			
		} catch (ParseException ex) {
			String errorMessage = "The provided start date from ConstRepo '"
				+ CONTEXT + "\\" + SUBCONTEXT + "\\" + VAR_START_TRADE_DATE + "'"
				+ " is not in the expected format '" + DATE_PATTERN + "'" 
				;
			logger.error(errorMessage, ex);
			throw new RuntimeException (errorMessage, ex);
		}

	}
}
