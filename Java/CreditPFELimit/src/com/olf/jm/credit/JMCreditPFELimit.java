package com.olf.jm.credit;

/**********************************************************************************************************************
 * File Name:                  JMCreditPFELimit.java
 *  
 * Author:                     Prashanth Ganapathi
 * 
 * Date Of Last Revision:
 * 
 * Script Type:                Main - Process
 * Parameter Script:           None
 * Display Script:             None
 * 
 * Toolsets script applies:    FX, ComSwap, LoanDep
 * 
 * Type of Script:             Credit batch, deal, update or ad-hoc report
 * 
 * History
 * 20-Aug-2021  GanapP02   EPI-xxxx     Initial version
 *********************************************************************************************************************/

import java.util.Date;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.jm.logging.Logging;
import com.olf.jm.util.SimUtil;
import com.olf.openjvs.OException;
import com.olf.openjvs.ValueAtRisk;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedules;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.limits.ConstField;
import com.olf.openrisk.limits.EnumRiskCriteria;
import com.olf.openrisk.limits.ExposureArgument;
import com.olf.openrisk.limits.ExposureLine;
import com.olf.openrisk.simulation.Configuration;
import com.olf.openrisk.simulation.EnumConfiguration;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultTypes;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.Scenarios;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.limits.Field;
import com.olf.openrisk.market.MarketFactory;
import com.olf.openrisk.market.VaRDefinition;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;

@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class JMCreditPFELimit extends AbstractExposureCalculator2<Table, Table> {
	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;
	
	private SimUtil simUtil = new SimUtil();
	
	private static TableFactory tf;
	private static IOFactory iof;
	private static CalendarFactory cf;
	
	private static final String CONST_REPO_CONTEXT = "JM Credit PFE";
	private static final String CONST_REPO_SUBCONTEXT = "Credit PFE Processing Script";
	
	private static final String CR_VAR_PA_UDSR_NAME = "Party Agreement udsr name";
	private static final String CR_VAR_VAR_INFO_UDSR_NAME = "VaR Info udsr name";
	private static final String CR_VAR_VAR_DEFINITION = "VaR Definition";
	private static final String CR_VAR_LOG_DEBUG_MESSAGES = "logDebugMessages";
	private static String SIM_PARTY_AFREEMENT_UDSR_NAME = "JM Credit PFE Party Agreement";
	private static String SIM_VAR_INFO_UDSR_NAME = "JM Credit PFE VaR Info";
	private static String VAR_DEFINITION = "";
	private static boolean LOG_DEBUG_MSG = false;
	
	private static final String CM_EXP_ARG_SAVED_SIM = "Saved Simulation";
	private static final String CM_EXP_ARG_CONFIDENCE_LVL = "Confidence Interval";
	private static final String CM_EXP_ARG_VAR_METHOD = "VaR Method";
	private static final String CM_EXP_ARG_SIM_PERFORMANCE_MODE = "Simulation Performance Mode";	
	private static String CM_EXP_ARG_SAVED_SIM_VALUE = "";
	private static Double CM_EXP_ARG_CONFIDENCE_LVL_VALUE;
	private static int CM_EXP_ARG_VAR_METHOD_VALUE;
	private static int CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE;
	
	private static String CLASSNAME = "";

	JMCreditPFELimit() {
		CLASSNAME = this.getClass().getSimpleName();
	}
	
	private VaRDefinition getDefinition(Session session) {
		MarketFactory mf = session.getMarketFactory();
		return mf.retrieveVaRDefinition(VAR_DEFINITION);
	}
	
	@Override
	public Table createExposureCache(Session session, ExposureDefinition definition) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".createExposureCache() : ";
		Table exposureCache = null;
		try {
			initialize(session, true);
			Logging.info(logPrefix + "method started");

			// Load VaR Definition
			VaRDefinition varDef = getDefinition(session);
			Logging.info(logPrefix + "Loaded VaR Definition '" + varDef.getName()+ "'");

			// Cache Correlation matrix and raw gpt info
			exposureCache = tf.createTable("Exposure Data");
			exposureCache.addColumn("gpt_info", EnumColType.Table);
			exposureCache.addColumn("corr_matrix", EnumColType.Table);
			exposureCache.addColumn("defn_id", EnumColType.Int);
			exposureCache.addRow();
			exposureCache.setTable("gpt_info", 0, varDef.getGridPointDataTable());
			exposureCache.setTable("corr_matrix", 0, varDef.getCorrelationMatrixTable());
			exposureCache.setInt("defn_id", 0, definition.getId());

			Logging.info(logPrefix + "Loaded VaR Grid Point Data and Var Correlation matrix for the VaR Definition");
		} catch (OpenRiskException e) {
			logErrorWithException(logPrefix + "Failed to create Exposure Cache " + e.getMessage());
		} finally {
			Logging.info(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
			Logging.close();
		}
		return exposureCache;
	}

	@Override
	public DealExposure[] calculateDealExposures(Session session, ExposureDefinition definition, Transaction transaction,
			Table revalResults) {

		DealExposure dealExposure = null;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".calculateDealExposures() : ";
		try {
			int dealNum = transaction.getDealTrackingId();
			int tranNum = transaction.getTransactionId();
			int insType = transaction.getInstrumentTypeObject().getId();
			int insSubType = transaction.getToolset() != EnumToolset.Loandep ? transaction.getInstrumentSubType().getValue() : 0;
			logDebugMsg(logPrefix + "Processing for deal : " + dealNum);
			
			SimulationFactory sf = session.getSimulationFactory();
			
			int row = revalResults.find(revalResults.getColumnId("result_type"), EnumResultType.VaRByTrans.getValue(), 0);
			Table vaRByTransResult = revalResults.getTable("result", row).createView("*", "id == " + tranNum).asTable();
			
			row = revalResults.find(revalResults.getColumnId("result_type"), sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME).getId(), 0);
			Table partyAgreementUDSR = revalResults.getTable("result", row)
					.createView("*", "deal_num == " + dealNum + " AND ins_sub_type == " + insSubType).asTable();
			
			row = revalResults.find(revalResults.getColumnId("result_type"), sf.getResultType(SIM_VAR_INFO_UDSR_NAME).getId(), 0);
			Table vaRInfoUDSR = revalResults.getTable("result", row).createView("*", "deal_num == " + dealNum).asTable();
			
			Table clientData = tf.createTable("clientData_" + dealNum);
			clientData.selectDistinct(partyAgreementUDSR, "deal_num, tran_num", "[IN.deal_num] == " + dealNum);
			if(clientData.getRowCount() <0) {
				logDebugMsg(logPrefix + "Sim results not found for deal : " + dealNum);
				return null;
			}
			
			int extLe = partyAgreementUDSR.getInt("external_lentity", 0);
			int overrideLE = 0;
			if(insType == EnumInsType.CallNoticeNostro.getValue() || insType == EnumInsType.CallNoticeMultiLegNostro.getValue() ) {
				overrideLE = getOverrideLEForCallNoticeDeals(session, transaction.getInstrumentId());
				extLe = overrideLE;
			}
			
			clientData.setName("Credid PFE WorkSheet");
			int matDateforDeal = insType == EnumInsType.FxInstrument.getValue()
					? transaction.getField(EnumTransactionFieldId.FxDate).getValueAsInt()
					: transaction.getField(EnumTransactionFieldId.MaturityDate).getValueAsInt();
			double[] rowExposure = getExposureForDeal(session, vaRByTransResult, partyAgreementUDSR, extLe, matDateforDeal);
			clientData.addColumn("mtm_info", EnumColType.Table);
			clientData.addColumn("var_info", EnumColType.Table);
			clientData.addColumn("var_by_trans", EnumColType.Double);
			clientData.setDouble("var_by_trans", 0, rowExposure[1]); 
			clientData.setTable("mtm_info", 0, partyAgreementUDSR);
			clientData.setTable("var_info", 0, vaRInfoUDSR);
			
			Field[] fields = definition.getCriteriaFields(transaction);
			// Assumption: only Legal Entity is added to criteria fields in Exposure definition
			// Assumption: Call notice deals are only of type Nostro and Nostro-ML.
			if(insType == EnumInsType.CallNoticeNostro.getValue() || insType == EnumInsType.CallNoticeMultiLegNostro.getValue() ) {
				// Find the External Legal Entity field
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].getCriteriaType().getId() == EnumRiskCriteria.ExtLentity.getValue()) {
						fields[i].setValue(overrideLE);
					}
				}
			}
			
			dealExposure = definition.createDealExposure(rowExposure[0] + rowExposure[1], transaction, fields);
			if (clientData != null)
				dealExposure.setClientData(clientData);

		} catch (Exception e) {
			logErrorWithException(logPrefix + "Failed to calculate deal exposures " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
		return new DealExposure[] { dealExposure };
	}

	private double[] getExposureForDeal(Session session, Table vaRByTransResult, Table partyAgreementUDSR, int extLe, int matDateforDeal) {

		double exposure = 0.0;
		double mtmExposure = 0.0;
		double vaRExposure = 0.0;
		ConstTable partyAgreementUDSRForDeal = partyAgreementUDSR.createConstView("*", "scenario_date <= " + matDateforDeal);
		ConstTable vaRByTransResultForDeal = vaRByTransResult.createConstView("*", "scenario_date <= " + matDateforDeal);
		Table baseMtmExposure = partyAgreementUDSRForDeal.calcByGroup("deal_num, scenario_id", "mtm_exposure");
		ConstTable distinctScenarios = partyAgreementUDSRForDeal.createConstView("scenario_id").getDistinctValues("scenario_id");
		int numOfScenarios = distinctScenarios.getRowCount();
		for (int scenarioRow = 0; scenarioRow < numOfScenarios; scenarioRow++) {
			int scenarioId = distinctScenarios.getInt("scenario_id", scenarioRow);
			int row = baseMtmExposure.find(baseMtmExposure.getColumnId("scenario_id"), scenarioId, 0);
			double dealMtmExposure = row < 0 ? 0.0 : baseMtmExposure.getDouble("Sum mtm_exposure", row);
			row = vaRByTransResultForDeal.find(vaRByTransResultForDeal.getColumnId("scenario_id"), scenarioId, 0);
			double dealVarExposure = row < 0 ? 0.0 : vaRByTransResultForDeal.getDouble("result", row) * -1;
			if ((dealMtmExposure + dealVarExposure) > exposure) {
				exposure = dealMtmExposure + dealVarExposure;
				mtmExposure = dealMtmExposure;
				vaRExposure = dealVarExposure;
			}
		}
		disposeTable(baseMtmExposure);

		// Set Exposure to 0 if cpty is internal LE
		return isCptyInternalLE(session, extLe) ? new double[] { 0.0, 0.0 } : new double[] { mtmExposure, vaRExposure };
	}

	@Override
	public Table createDealCache(Session session, ExposureDefinition definition, Transactions transactions) {

		SimResults results = null;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".createDealCache() : ";
		Table revalResults = null;
		try {
			initialize(session, true);
			revalResults = tf.createTable();
			Logging.info(logPrefix + "method started");
			loadExposureArguments(definition);
			
			SimulationFactory sf = session.getSimulationFactory();
			Simulation sim = sf.retrieveSimulation(CM_EXP_ARG_SAVED_SIM_VALUE);
			Date collCallDate = getCollCallDate(session, transactions, sim);
			
			results = runSimulation(session, sim, transactions, collCallDate); 
			
			Table partyAgreementUDSR = tf.createTable("Party Agreement Info");
			Table vaRInfoUDSR = tf.createTable("VaR Info");
			Table vaRByTransResult = tf.createTable("VarByTrans result");
			int numOfScenarios = results.getCount();
			for(int scenarioId = 1; scenarioId <= numOfScenarios; scenarioId++) {
				RevalResults scenarioResults = results.getScenarioResults(scenarioId);
				Date scenarioDate = getScenarioDateForScenario(sim, scenarioId); 
				int scenarioDateJd = cf.getJulianDate(scenarioDate);
				
				Table partyAgreementUDSRForScenario = getPartyAgreementUDSR(sf, scenarioResults).asTable();
				partyAgreementUDSRForScenario.addColumn("scenario_id", EnumColType.Int);
				partyAgreementUDSRForScenario.addColumn("scenario_date", EnumColType.Int);
				partyAgreementUDSRForScenario.setColumnValues(partyAgreementUDSRForScenario.getColumnId("scenario_id"), scenarioId);
				partyAgreementUDSRForScenario.setColumnValues(partyAgreementUDSRForScenario.getColumnId("scenario_date"), scenarioDateJd);
				partyAgreementUDSR.select(partyAgreementUDSRForScenario, "*", "scenario_id > -1");
				
				Table vaRInfoUDSRForScenario = getVaRInfoUDSR(sf, scenarioResults).asTable();
				vaRInfoUDSRForScenario.addColumn("scenario_id", EnumColType.Int);
				vaRInfoUDSRForScenario.addColumn("scenario_date", EnumColType.Int);
				vaRInfoUDSRForScenario.setColumnValues(vaRInfoUDSRForScenario.getColumnId("scenario_id"), scenarioId);
				vaRInfoUDSRForScenario.setColumnValues(vaRInfoUDSRForScenario.getColumnId("scenario_date"), scenarioDateJd);
				vaRInfoUDSR.select(vaRInfoUDSRForScenario, "*", "scenario_id > -1");
				
				if (isVaRByTransResultRequiredForScenario(transactions, collCallDate, scenarioDate)) {
					Table vaRByTransResultForScenario = getVaRByTransResultForAllIns(sf, scenarioResults);
					vaRByTransResultForScenario.addColumn("scenario_id", EnumColType.Int);
					vaRByTransResultForScenario.addColumn("scenario_date", EnumColType.Int);
					vaRByTransResultForScenario.setColumnValues(vaRByTransResultForScenario.getColumnId("scenario_id"), scenarioId);
					vaRByTransResultForScenario.setColumnValues(vaRByTransResultForScenario.getColumnId("scenario_date"), scenarioDateJd);
					vaRByTransResult.select(vaRByTransResultForScenario, "*", "scenario_id > -1");
				}
			}
			
			extrapolateResultsForOtherScenarios(sim, transactions, partyAgreementUDSR, vaRInfoUDSR, vaRByTransResult);
			
			revalResults.addColumn("result_type", EnumColType.Int);
			revalResults.addColumn("result", EnumColType.Table);
			int row = revalResults.addRows(1);
			revalResults.setInt("result_type", row, EnumResultType.VaRByTrans.getValue());
			revalResults.setTable("result", row, vaRByTransResult);
			row = revalResults.addRows(1);
			revalResults.setInt("result_type", row, sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME).getId());
			revalResults.setTable("result", row, partyAgreementUDSR);
			row = revalResults.addRows(1);
			revalResults.setInt("result_type", row, sf.getResultType(SIM_VAR_INFO_UDSR_NAME).getId());
			revalResults.setTable("result", row, vaRInfoUDSR);
			
		} catch (OpenRiskException e) {
			logErrorWithException(logPrefix + "Failed to run simulation and create deal cache " + e.getMessage());
		} finally {
			Logging.info(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
		
		return revalResults;
	}

	private Date getScenarioDateForScenario(Simulation sim, int scenarioId) {
		
		Scenario scenario = sim.getScenario(scenarioId);
		Configuration scenarioDateCfg = scenario.getConfigurations().find(EnumConfiguration.Date, "Scenario Date");
		Date scenarioDate = scenarioDateCfg == null ? cf.createSymbolicDate("0cd").evaluate()
				: scenarioDateCfg.getFields().getField("Modified Scenario Date").getValueAsDate();
		return scenarioDate;
	}

	private boolean isVaRByTransResultRequiredForScenario(Transactions transactions, Date collCallDate, Date scenarioDate) {
		return transactions.getCount() <= 2 && CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE == 0 && scenarioDate.after(collCallDate) ? false : true;
	}

	private SimResults runSimulation(Session session, Simulation sim, Transactions transactions, Date collCallDate) {

		SimResults results = null;
		String logPrefix = CLASSNAME + ".runSimulation() : ";
		//In case of FX and Cash toolset remove all scenarios except first '1d' scenario
		//for other toolsets remove Scenarios with scenario date greater tgab Next Collateral Call Date Or Maturity Date 
		Transaction transaction = getTransaction(transactions); 
		long currentTimeSim = System.currentTimeMillis();
		
		if (transactions.getCount() > 2 || CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE == 0) {
			// Running in Batch mode so run default sim definition with all scenarios.
			Logging.info(logPrefix + "Run revaluation for the simulation definition " + sim.getName() + " with "
					+ sim.getScenarios().getCount() + " scenarios");
			results = sim.run(transactions);
		} else {
			removeUnwantedScenarios(session, sim, transactions, collCallDate);
			if ((transaction.getToolset() == EnumToolset.Fx || transaction.getToolset() == EnumToolset.Cash)
					&& CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE == 2) {
				Simulation simCopy = sim.clone();
				removeUnwantedScenariosForFxCash(simCopy, transactions);
				Logging.info(logPrefix + "Run revaluation for the simulation definition " + sim.getName() + " with "
						+ simCopy.getScenarios().getCount() + " scenarios");
				results = simCopy.run(transactions);
				simCopy.dispose();
			} else {
				Logging.info(logPrefix + "Run revaluation for the simulation definition " + sim.getName() + " with "
						+ sim.getScenarios().getCount() + " scenarios");
				results = sim.run(transactions);				
			}
		}
		Logging.info(logPrefix + "Completed running revaluation for the simulation definition " + sim.getName() + "in " 
				+ (System.currentTimeMillis() - currentTimeSim) + " ms");
		
		return results;
	}

	private Date getCollCallDate(Session session, Transactions transactions, Simulation sim) {

		Transaction transaction = getTransaction(transactions);
		int partyAgreementId = transaction.getField(EnumTransactionFieldId.PartyAgreement).getValueAsInt();
		Date matDate = getLastMatDate(session, transactions, sim.getScenario(1).getCurrency());
		// Assumption: all scenarios have same currency.
		Date collCallDate = getCollCallDateFromPA(session, partyAgreementId, sim.getScenario(1).getCurrency());
		collCallDate = matDate == null ? collCallDate
				: (collCallDate == null ? matDate : (matDate.before(collCallDate) ? matDate : collCallDate));
		if (collCallDate == null) {
			Logging.warn("Invalid collateral call date. Credit check will be run for all scenarios");
		}
		return collCallDate;
	}

	private Transaction getTransaction(Transactions transactions) {

		EnumInsSub insSub;
		try {
			insSub = transactions.get(0).getInstrumentSubType();
		} catch (Exception e) {
			insSub = null;
		}
		return insSub == null ? transactions.get(0) : insSub == EnumInsSub.FxFarLeg ? transactions.get(1) : transactions.get(0);
	}

	private void extrapolateResultsForOtherScenarios(Simulation sim, Transactions transactions, Table partyAgreementUDSR, Table vaRInfoUDSR,
			Table vaRByTransResult) {
		
		String logPrefix = CLASSNAME + ".extrapolateResultsForOtherScenarios() : ";
		long currentTime = System.currentTimeMillis();
		Logging.info(logPrefix + "method started");
		Transaction transaction = getTransaction(transactions); 
		if (transactions.getCount() > 2 || (transaction.getToolset() != EnumToolset.Fx && transaction.getToolset() != EnumToolset.Cash)
				|| CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE < 2) {
			return;
		}
		
		Table partyAgreementUDSRScen1 = partyAgreementUDSR.cloneData();
		Table vaRInfoUDSRScen1 = vaRInfoUDSR.cloneData();
		Table vaRByTransResultScen1 = vaRByTransResult.cloneData();
		Table partyAgreementUDSRCopy = null;
		Table vaRInfoUDSRCopy = null;
		Table vaRByTransResultCopy = null;
		
		Double scalingFactorScen1 = 1.0; 
		for (Scenario scenario : sim.getScenarios()) {
			int scenarioId = scenario.getId();
			if(scenarioId == 1) {
				Configuration scenarioDateCfg = scenario.getConfigurations().find(EnumConfiguration.Result, "Parametric VaR Attributes");
				scalingFactorScen1 = scenarioDateCfg.getFields().getField("Input Volatility Scaling Factor").getValueAsDouble();
				continue;
			}
			Date scenarioDate = getScenarioDateForScenario(sim, scenarioId); 
			int scenarioDateJd = cf.getJulianDate(scenarioDate);
			partyAgreementUDSRCopy = partyAgreementUDSRScen1.cloneData();
			partyAgreementUDSRCopy.setColumnValues(partyAgreementUDSRCopy.getColumnId("scenario_id"), scenarioId);
			partyAgreementUDSRCopy.setColumnValues(partyAgreementUDSRCopy.getColumnId("scenario_date"), scenarioDateJd);
			partyAgreementUDSR.appendRows(partyAgreementUDSRCopy);
			partyAgreementUDSRCopy.clear();
			
			Configuration scenarioDateCfg = scenario.getConfigurations().find(EnumConfiguration.Result, "Parametric VaR Attributes");
			Double scalingFactor = scenarioDateCfg.getFields().getField("Input Volatility Scaling Factor").getValueAsDouble();
			scalingFactor = (scalingFactor == 0) ? 1 : scalingFactor;
			
			vaRByTransResultCopy = vaRByTransResultScen1.cloneData();
			vaRByTransResultCopy.setColumnValues(vaRByTransResultCopy.getColumnId("scenario_id"), scenarioId);
			vaRByTransResultCopy.setColumnValues(vaRByTransResultCopy.getColumnId("scenario_date"), scenarioDateJd);
			vaRByTransResultCopy.calcColumn("result", "result * " + scalingFactor);
			int compVarCollId = vaRByTransResultCopy.getColumnId("Component VaR");
			vaRByTransResultCopy.calcColumn(compVarCollId, "result * " + scalingFactor);
			vaRByTransResult.appendRows(vaRByTransResultCopy);
			vaRByTransResultCopy.clear();
			
			vaRInfoUDSRCopy = vaRInfoUDSRScen1.cloneData();
			vaRInfoUDSRCopy.setColumnValues(vaRInfoUDSRCopy.getColumnId("scenario_id"), scenarioId);
			vaRInfoUDSRCopy.setColumnValues(vaRInfoUDSRCopy.getColumnId("scenario_date"), scenarioDateJd);
			vaRInfoUDSRCopy.calcColumn("delta", "delta * " + scalingFactor);
			vaRInfoUDSRCopy.calcColumn("delta", "delta / " + scalingFactorScen1);
			vaRInfoUDSRCopy.calcColumn("gamma", "gamma * " + Math.pow(scalingFactor, 2));
			vaRInfoUDSRCopy.calcColumn("gamma", "gamma / " + Math.pow(scalingFactorScen1, 2));
			vaRInfoUDSR.appendRows(vaRInfoUDSRCopy);
			vaRInfoUDSRCopy.clear();
		}
		
		disposeTable(partyAgreementUDSRScen1);
		disposeTable(vaRInfoUDSRScen1);
		disposeTable(vaRByTransResultScen1);
		disposeTable(partyAgreementUDSRCopy);
		disposeTable(vaRInfoUDSRCopy);
		disposeTable(vaRByTransResultCopy);
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
	}

	private void removeUnwantedScenariosForFxCash(Simulation sim, Transactions transactions) {

		// DO not remove scenarios in Bach mode. Assumption is that in batch mode deal count will always be more than 2.
		// Do not remove scenarios if run mode is 0 or 1
		if (transactions.getCount() > 2 || CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE < 2 ){
			return;
		}
				
		Scenarios scenarios = sim.getScenarios();
		int scenarioCount = scenarios.getCount();
		for(int count = scenarioCount; count > 1; count-- ) {
			Scenario scenario = sim.getScenario(count);
			if(scenario.getId() == 1) {
				continue;
			}
			sim.removeScenario(scenario);			
		}
	}

	private void removeUnwantedScenarios(Session session, Simulation sim, Transactions transactions, Date collCallDate) {

		// DO not remove scenarios in Bach mode. Assumption is that in batch mode deal count will always be more than 2.
		// Do not remove scenarios if run mode is 0
		if (transactions.getCount() > 2 || CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE == 0) {
			return;
		}

		Scenarios scenarios = sim.getScenarios();
		int scenarioCount = scenarios.getCount();
		// Do not remove first scenario. Always have atleast 1 scenario in sim definition.
		for (int count = scenarioCount; count > 1; count--) {
			Scenario scenario = scenarios.get(count - 1);
			Date scenarioDate = getScenarioDateForScenario(sim, scenario.getId());
			if (scenarioDate.after(collCallDate)) {
				sim.removeScenario(scenario);
			}
		}
	}

	private Date getLastMatDate(Session session, Transactions transactions, Currency scenarioCurrency) {

		Date lastMatDate = session.getBusinessDate();
		// Set up the reval
		SimulationFactory sf = session.getSimulationFactory();
		RevalSession reval = sf.createRevalSession(transactions);
		// Set the base currency from the Exposure definition
		reval.setCurrency(scenarioCurrency);

		// Get MTM Detail
		ResultTypes resultTypes = sf.createResultTypes();
		resultTypes.add(EnumResultType.MtmDetail);
		RevalResults results = reval.calcResults(resultTypes);
		Table mtmDetail = session.getTableFactory().createTable("MTM Detail");
		if (results.contains(EnumResultType.MtmDetail)) {
			mtmDetail = results.getResultTable(EnumResultType.MtmDetail).asTable();
			if (mtmDetail.isValidColumn("payment_date")) {
				for (int date : mtmDetail.getColumnValuesAsInt("payment_date")) {
					lastMatDate = cf.getJulianDate(lastMatDate) > date ? lastMatDate : cf.getDate(date);
				}
			}
		} else {
			Logging.warn("No Sim Result Returned for MTM Detail \n");
		}
		return lastMatDate;
	}

	private Date getCollCallDateFromPA(Session session, int partyAgreementId, Currency currency) {
		
		Date collCallDate = null;
		Table partyAgreementList = null;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".getCollCallDateFromPA() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT intbu.party_id internal_bunit, extbu.party_id external_bunit, pa.party_agreement_id");
			sql.append("\n     , netting_flag, haircut_calculator_flag, haircut_method, pac.valuation_date_sequence");
			sql.append("\n  FROM party_agreement pa");
			sql.append("\n  JOIN party_agreement_assignment intbu ON pa.party_agreement_id = intbu.party_agreement_id");
			sql.append("\n       AND intbu.internal_external_flag = 0");
			sql.append("\n  JOIN party_agreement_assignment extbu ON pa.party_agreement_id = extbu.party_agreement_id");
			sql.append("\n       AND extbu.internal_external_flag = 1");
			sql.append("\n  LEFT JOIN party_agreement_collateral pac ON pa.party_agreement_id = pac.party_agreement_id");
			sql.append("\n        AND pac.int_ext = 0");
			sql.append("\n WHERE pa.doc_status = 1");
			sql.append("\n   AND pa.party_agreement_id = ").append(partyAgreementId);
			partyAgreementList = iof.runSQL(sql.toString());
			
			if(partyAgreementList.getRowCount() >1) {
				Logging.info(logPrefix + "More than one party agreement found for the deal. First party agreement "
						+ "will be used to get collateral call date");	
			} else if(partyAgreementList.getRowCount() <= 0) {
				Logging.warn(logPrefix + "Party agreement or collateral call date not found");	
			} else {
				int partyAgreement = partyAgreementList.getValueAsInt("party_agreement_id", 0);
				String collAgreement = partyAgreement <= 0 ? "No" : "Yes";
				String valDateSeq = partyAgreementList.getString("valuation_date_sequence", 0);
				valDateSeq = "Yes".equalsIgnoreCase(collAgreement) ? ("".equalsIgnoreCase(valDateSeq) ? "1m" : valDateSeq) : "1y";
				HolidaySchedules hs = cf.createHolidaySchedules();
				hs.addSchedule(currency.getHolidaySchedule());
				SymbolicDate symbolicDate = cf.createSymbolicDate(valDateSeq);
				symbolicDate.setHolidaySchedules(hs);
				collCallDate = symbolicDate.evaluate(session.getBusinessDate());
			}

			logDebugMsg(logPrefix + "Collateral Call date retrieved.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to load Collateral Call date for party agreement : " + e.getMessage());
		} finally {
			disposeTable(partyAgreementList);
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
		return collCallDate;
	}

	private void loadExposureArguments(ExposureDefinition definition) {

		ExposureArgument[] expArg = definition.getExposureArguments();
		for (ExposureArgument arg : expArg) {
			if (CM_EXP_ARG_SAVED_SIM.equalsIgnoreCase(arg.getName())) {
				CM_EXP_ARG_SAVED_SIM_VALUE = arg.getValueAsString();
			} else if (CM_EXP_ARG_CONFIDENCE_LVL.equalsIgnoreCase(arg.getName())) {
				CM_EXP_ARG_CONFIDENCE_LVL_VALUE = Double.parseDouble(arg.getValueAsString());
			} else if (CM_EXP_ARG_VAR_METHOD.equalsIgnoreCase(arg.getName())) {
				CM_EXP_ARG_VAR_METHOD_VALUE = Integer.parseInt(arg.getValueAsString());
			} else if (CM_EXP_ARG_SIM_PERFORMANCE_MODE.equalsIgnoreCase(arg.getName())) {
				CM_EXP_ARG_SIM_PERFORMANCE_MODE_VALUE = Integer.parseInt(arg.getValueAsString());
			}
		}
	}

	private ConstTable getVaRCorrelationMatrix(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(EnumResultType.VaRCorrelationMatrix));
	}

	private ConstTable getVaRByTransResult(SimulationFactory sf, RevalResults revalResults, int insType) {
		return simUtil.getGenResults(revalResults, sf.getResultType(EnumResultType.VaRByTrans), insType, 0, 0);
	}
	
	private Table getVaRByTransResultForAllIns(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getAllGenResultsForInsType(revalResults, sf.getResultType(EnumResultType.VaRByTrans));
	}

	private ConstTable getPartyAgreementUDSR(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME));
	}

	private ConstTable getVaRInfoUDSR(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(SIM_VAR_INFO_UDSR_NAME));
	}
	
	@Override
	public void disposeDealCache(Session session, Table dealCache) {
		disposeTable(dealCache);
	}

	@Override
	public double aggregateLineExposures(Session session, ExposureLine line, LineExposure[] exposures, Table exposureCache,
			boolean isInquiry) {

		double usage = 0.0;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".aggregateLineExposures() : ";
		Table distinctPartyAgreement = null;
		Table clientDataForLineAndPA = null;
		Table pFEExposure = null;
		try {
			initialize(session, false);
			Logging.info(logPrefix + "method started");
			Logging.info(logPrefix + "Exposure Line: " + line.toString());
			int exposureDefnId = exposureCache.getInt("defn_id", 0);
			ExposureDefinition defn = (ExposureDefinition) session.getLimitsFactory().getExposureDefinition(exposureDefnId); 
			loadExposureArguments(defn);
			
			int extLe = 0;
			ConstField[] fields = line.getCriteriaFields();
			// Find the External Legal Entity field
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].getCriteriaType().getId() == EnumRiskCriteria.ExtLentity.getValue()) {
					extLe = fields[i].getValueAsInt();
				}
			}
			
			//Create Table for PFE Exposure calculation 
			pFEExposure = createPFEExposureTable();
			
			//Get distinct Party Agreement before any calculations
			distinctPartyAgreement = getDistinctPartyAgreement(exposures);
			// for each party Agreement against the cpty
			for(int row =0; row < distinctPartyAgreement.getRowCount(); row++ ) {
				
				int partyAgreementId = distinctPartyAgreement.getInt("party_agreement_id", row);
				clientDataForLineAndPA = getAllClientDataForPA(exposures, partyAgreementId);
				
				Table mtmExposureForAllScenarios = getMtMExposureFORLineByPA(clientDataForLineAndPA);
				Table vaRExposureForAllScenarios = getVarExposureForLineByPA(session, exposureCache, clientDataForLineAndPA);

				int rowPFE = pFEExposure.addRow().getNumber();
				pFEExposure.setInt("exposure_line_id", rowPFE, line.getId());
				pFEExposure.setInt("party_agreement_id", rowPFE, partyAgreementId);
				pFEExposure.setTable("mtm_exposure", rowPFE, mtmExposureForAllScenarios);
				pFEExposure.setTable("var_exposure", rowPFE, vaRExposureForAllScenarios);
				
				disposeTable(clientDataForLineAndPA);
			}
			
			// Once Mtm Exposure and Var Exposure for each cpty for each party agreement is known
			for(TableRow row : pFEExposure.getRows() ) {
				Table exposure = tf.createTable("Exposure by Counterparty and PA");
				exposure = row.getTable("mtm_exposure").cloneData();
				exposure.select(row.getTable("var_exposure"), "var_exposure", "[IN.scenario_id] == [OUT.scenario_id]");
				exposure.addColumn("usage", EnumColType.Double);
				calcUsage(exposure);
				usage += Math.max(arrayMax(exposure.getColumnValuesAsDouble("usage")), 0);
			}

			// Set Exposure to 0 if cpty is internal LE
			usage = isCptyInternalLE(session, extLe) ? 0.0: usage;
			
		} catch (OpenRiskException e) {
			logErrorWithException(logPrefix + "Failed to aggregate line exposures " + e.getMessage());
			String message = "";
			for (StackTraceElement st : e.getStackTrace()) {
				message += "\n" + st.toString();
			}
			Logging.error(message);
			
		} finally {
			Logging.info(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
			Logging.close();
		}
		return usage;
	}

	private double arrayMax(double[] arr) {

		double max = Double.NEGATIVE_INFINITY;
		if (arr == null)
			return 0.0;
		for (double cur : arr)
			max = Math.max(max, cur);
		return max;
	}

	private void calcUsage(Table exposure) {
		
		//exposure.calcColumn("usage", "max(mtm_exposure + var_exposure, 0)");
		//Above API internally converts double to int, when mtm/var exposure is > 2^32, hence calculating in a loop
		for(TableRow row : exposure.getRows() ) {
			double mtmExposure = row.getDouble("mtm_exposure");
			double varExposure = row.getDouble("var_exposure");
			row.getCell("usage").setDouble(mtmExposure + varExposure);
		}
	}
	
	private boolean isCptyInternalLE(Session session, int extLentity) {
		
		boolean isIntLe = false;
		// Client data will have data only for one deal so get ins num from first row.
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT int_ext FROM party WHERE party_status = 1 AND party_id = ").append(extLentity);
		Table intLe = tf.createTable();
		intLe = session.getIOFactory().runSQL(sql.toString());
		if (intLe.getRowCount() <= 0) {
			Logging.error("Failed to get internal external status of party. SQL: " + sql.toString());
		} else {
			isIntLe = intLe.getInt("int_ext",0) == 0;
		}
		return isIntLe;
	}

	private int getOverrideLEForCallNoticeDeals(Session session, int insNum) {
		
		int overrideHolderLE = 0;
		// Client data will have data only for one deal so get ins num from first row.
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT cn.ins_num, acc.account_id, acc.holder_id, pr.legal_entity_id");
		sql.append("\n  FROM call_notice cn");
		sql.append("\n  JOIN account acc ON cn.account_id = acc.account_id");
		sql.append("\n  JOIN party_relationship pr ON pr.business_unit_id = acc.holder_id");
		sql.append("\n WHERE cn.ins_num = ").append(insNum);
		
		Table overrideLE = tf.createTable();
		overrideLE = session.getIOFactory().runSQL(sql.toString());
		if (overrideLE.getRowCount() <= 0) {
			throw new OpenRiskException("Query failed");
		} else {
			overrideHolderLE = overrideLE.getInt("legal_entity_id",0);
		}
		return overrideHolderLE; 
	}

	private Table getVarExposureForLineByPA(Session session, Table exposureCache, Table clientDataForLineAndPA) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".getVarExposureForLineByPA() : ";
		logDebugMsg(logPrefix + "method started");
		if(clientDataForLineAndPA.getRowCount() <=0) {
			return null;
		}
		
		// Get num of Scenarios -> Assuming all deals have same number of scenarios
		ConstTable vaRInfo = clientDataForLineAndPA.getTable("var_info", 0);
		// Summarise the client data for exposure line by index id and grid point id
		Table summary = vaRInfo.calcByGroup("index_id, scenario_id, vol_id, gpt_id, gpt_label, gpt_name, gpt_date, gpt_time", "delta, gamma, gpt_sigma");
		
		com.olf.openjvs.Table corrMatrix = tf.toOpenJvs(exposureCache.getTable("corr_matrix", 0));
		ConstTable distinctScenarios = summary.createConstView("scenario_id").getDistinctValues("scenario_id");
		int scenarioCount = distinctScenarios.getRowCount();
		Table vaRExposureForAllScenarios = tf.createTable("Var Exposure for line by PAby scenario");
		vaRExposureForAllScenarios.addColumn("scenario_id", EnumColType.Int);
		vaRExposureForAllScenarios.addColumn("var_exposure", EnumColType.Double);
		vaRExposureForAllScenarios.addRows(scenarioCount);
		for(int scenarioRow = 0; scenarioRow < scenarioCount ; scenarioRow++){
			int scenarioId = distinctScenarios.getInt("scenario_id", scenarioRow);
			// Get Grid Point info from exposure cache and replace sensitivity values with exposure line's values
			Table gptInfoOC = exposureCache.getTable("gpt_info", 0).cloneData();
			gptInfoOC.select(summary,
					"index_id, vol_id, gpt_id, gpt_label, gpt_name, gpt_date, gpt_time, Sum gpt_sigma->gpt_sigma, Sum delta->gpt_delta, Sum gamma->gpt_gamma",
					"[IN.index_id] == [OUT.index_id] AND [IN.gpt_id] == [OUT.gpt_id] AND [IN.scenario_id] == " + scenarioId);
			com.olf.openjvs.Table gptInfo = tf.toOpenJvs(gptInfoOC);
			double vaRExposure = 0.0;
			try {
				double scale = 1;
				// computeVaR method is not available in OC hence using JVS API
				vaRExposure = ValueAtRisk.computeVaR(corrMatrix, gptInfo, 1.0, scale * -1, CM_EXP_ARG_VAR_METHOD_VALUE, CM_EXP_ARG_CONFIDENCE_LVL_VALUE);
			} catch (OException e) {
				logErrorWithException(logPrefix + "Failed to compute VaR: " + e.getMessage());
			} catch (Exception e ) {
				
			}
			vaRExposureForAllScenarios.setInt("scenario_id", scenarioRow, scenarioId);
			vaRExposureForAllScenarios.setDouble("var_exposure", scenarioRow, vaRExposure);
			disposeTable(gptInfoOC);
		}
		disposeTable(summary);
		
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		return vaRExposureForAllScenarios;
	}

	private Table getMtMExposureFORLineByPA(Table clientDataForLineAndPA) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".getMtMExposureFORLineByPA() : ";
		logDebugMsg(logPrefix + "method started");
		// Summarise the client data for exposure line by
		ConstTable mtmInfo = clientDataForLineAndPA.getTable("mtm_info", 0);
		ConstTable clientDataDistinctValues = mtmInfo.createConstView("deal_num, party_agreement_id")
				.getDistinctValues("deal_num, party_agreement_id");
		if (clientDataDistinctValues.getRowCount() <= 0) {
			return null;
		}
		
		Table mtmExposureForAllScenarios = null;
		mtmExposureForAllScenarios = mtmInfo.calcByGroup("scenario_id", "mtm_exposure");
		mtmExposureForAllScenarios.setColumnName(mtmExposureForAllScenarios.getColumnId("Sum mtm_exposure"), "mtm_exposure");
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		return mtmExposureForAllScenarios;
	}

	private Table getAllClientDataForPA(LineExposure[] exposures, int partyAgreementId) {
		
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".getAllClientDataForPA() : ";
		logDebugMsg(logPrefix + "method started");
		//Gather all the client data tables into a single table for the current exposure line
		Table clientDataForLineAndPA = (exposures.length >0) ? exposures[0].getClientData().cloneStructure() : null;
		clientDataForLineAndPA.setName("Client Data For all deals for Line And PA");
		Table mtmInfo = exposures[0].getClientData().getTable("mtm_info", 0).cloneStructure();
		Table varInfo = exposures[0].getClientData().getTable("var_info", 0).cloneStructure();
		for (LineExposure exposure : exposures) {
			ConstTable mtmInfoForDeal = exposure.getClientData().getTable("mtm_info", 0);
			ConstTable varInfoForDeal = exposure.getClientData().getTable("var_info", 0);
			
			// Since a deal can have only one Party Agreement it is sufficient to check first row.
			int partyAgreementIdCD = mtmInfoForDeal.getInt("party_agreement_id", 0);
			if(partyAgreementId == partyAgreementIdCD) {
				mtmInfo.appendRows(mtmInfoForDeal);
				varInfo.appendRows(varInfoForDeal);
			}
		}
		clientDataForLineAndPA.addRows(1);
		clientDataForLineAndPA.setTable("mtm_info", 0, mtmInfo);
		clientDataForLineAndPA.setTable("var_info", 0, varInfo);
		
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		return clientDataForLineAndPA;
	}

	private Table createPFEExposureTable() {
		
		Table pFEExposure = tf.createTable("PFE Exposure");
		pFEExposure.addColumn("exposure_line_id", EnumColType.Int);
		pFEExposure.addColumn("external_lentity", EnumColType.Int);
		pFEExposure.addColumn("party_agreement_id", EnumColType.Int);
		pFEExposure.addColumn("mtm_exposure", EnumColType.Table);
		pFEExposure.addColumn("var_exposure", EnumColType.Table);
		return pFEExposure;
	}

	private Table getDistinctPartyAgreement(LineExposure[] exposures) {

		Table distinctPartyAgreement = tf.createTable("Distinct Party Agreements");
		for (LineExposure exposure : exposures) {
			ConstTable clientData = exposure.getClientData().getTable("mtm_info", 0);
			distinctPartyAgreement.select(clientData, "party_agreement_id", "[IN.party_agreement_id] > -1");
		}
		distinctPartyAgreement.makeDistinct("party_agreement_id", "party_agreement_id > -1");
		return distinctPartyAgreement;
	}

	private void initialize(Session session, boolean isNewRun) {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			tf = session.getTableFactory();
			iof = session.getIOFactory();
			cf = session.getCalendarFactory();
			
			VAR_DEFINITION = constRepo.getStringValue(CR_VAR_VAR_DEFINITION);
			SIM_PARTY_AFREEMENT_UDSR_NAME = constRepo.getStringValue(CR_VAR_PA_UDSR_NAME, SIM_PARTY_AFREEMENT_UDSR_NAME);
			SIM_VAR_INFO_UDSR_NAME = constRepo.getStringValue(CR_VAR_VAR_INFO_UDSR_NAME, SIM_VAR_INFO_UDSR_NAME);
			LOG_DEBUG_MSG = constRepo.getIntValue(CR_VAR_LOG_DEBUG_MESSAGES) == 1 ? true : false;
			
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to initialize : " + e.getMessage());
		} catch (OException e) {
			throw new OpenRiskException("Failed to load const repository values : " + e.getMessage());
		}
		if(isNewRun) {
			Logging.info("********************* Start of new run ***************************");			
		}
	}

	private void logErrorWithException(String msg) {
		Logging.error(msg);
		throw new OpenRiskException(msg);
	}
	
	private void disposeTable(Table table) {
		if(table != null) {
			table.dispose();
		}
	}
	
	private void logDebugMsg(String message){
		if(LOG_DEBUG_MSG) {
			Logging.debug(message);
		}
	}

}