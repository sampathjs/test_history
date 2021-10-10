package com.olf.jm.credit;

import java.util.Calendar;
import java.util.Date;

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

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.jm.logging.Logging;
import com.olf.jm.util.SimUtil;
import com.olf.openjvs.OException;
import com.olf.openjvs.ValueAtRisk;
import com.olf.openjvs.enums.VAR_METHODS;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.limits.EnumBatchStatus;
import com.olf.openrisk.limits.EnumRiskCriteria;
import com.olf.openrisk.limits.ExposureArgument;
import com.olf.openrisk.limits.ExposureLine;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultTypes;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.limits.Field;
import com.olf.openrisk.market.MarketFactory;
import com.olf.openrisk.market.VaRDefinition;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;

@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class JMCreditPFELimit extends AbstractExposureCalculator2<RevalResults, Table> {
	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;
	
	private SimUtil simUtil = new SimUtil();
	
	private static TableFactory tf; 
	
	private static final String SIM_PARTY_AFREEMENT_UDSR_NAME = "JM Credit PFE Party Agreement";
	private static final String SIM_VAR_INFO_UDSR_NAME = "JM Credit PFE VaR Info";
	private static final String SIM_VAR_AND_PA_UDSR_NAME = "JM Credit PFE VaR And PA Info";
	private static final String CONST_REPO_CONTEXT = "JM Credit PFE";
	private static final String CONST_REPO_SUBCONTEXT = "Credit PFE Processing Script";
	private static String CR_VARIABLE_VAR_DEFINITION = "VaR Definition";
	private static String VAR_DEFINITION = "";
	private static String CM_EXP_ARG_SAVED_SIM = "Saved Simulation";
	private static String CM_EXP_ARG_SAVED_SIM_VALUE = "";
	private static String CM_EXP_ARG_CONFIDENCE_LVL = "Confidence Interval";
	private static Double CM_EXP_ARG_CONFIDENCE_LVL_VALUE;
	private static String CM_EXP_ARG_VAR_METHOD = "VaR Method";
	private static int CM_EXP_ARG_VAR_METHOD_VALUE;
	private static String SIM_DEF_SCENARIO_NAME = "PFE";
	
	private VaRDefinition getDefinition(Session session) {
		MarketFactory mf = session.getMarketFactory();
		return mf.retrieveVaRDefinition(VAR_DEFINITION);
	}
	
	@Override
	public Table createExposureCache(Session session, ExposureDefinition definition) {

		Table exposureCache = null;
		String logPrefix = this.getClass().getSimpleName() + ".createExposureCache() : ";
		try {
			initialize(session, true);
			Logging.info(logPrefix + "Start Method");

			// Load VaR Definition
			VaRDefinition varDef = getDefinition(session);
			Logging.info(logPrefix + "Loaded VaR Definition '" + varDef.getName()+ "'");

			// Cache Correlation matrix and raw gpt info
			exposureCache = tf.createTable("Exposure Data");
			exposureCache.addColumn("gpt_info", EnumColType.Table);
			exposureCache.addColumn("corr_matrix", EnumColType.Table);
			exposureCache.addRow();
			exposureCache.setTable("gpt_info", 0, varDef.getGridPointDataTable());
			exposureCache.setTable("corr_matrix", 0, varDef.getCorrelationMatrixTable());

			Logging.info(logPrefix + "Loaded VaR Grid Point Data and Var Correlation matrix for the VaR Definition");
		} catch (OpenRiskException e) {
			logErrorWithException("Failed to create Exposure Cache " + e.getMessage());
		} finally {
			Logging.info(logPrefix + "End Method");
			Logging.close();
		}

		return exposureCache;
	}

	@Override
	public DealExposure[] calculateDealExposures(Session session, ExposureDefinition definition, Transaction transaction,
			RevalResults revalResults) {

		DealExposure dealExposure = null;
		String logPrefix = this.getClass().getSimpleName() + ".calculateDealExposures() : ";
		try {
			double rowExposure = 0.0;
			int dealNum = transaction.getDealTrackingId();
			int tranNum = transaction.getTransactionId();
			int insType = transaction.getInstrumentTypeObject().getId();
			Logging.debug(logPrefix + "Processing for deal : " + dealNum);
			if(dealNum == 1498314 || dealNum == 1463888 || dealNum == 1520454 || dealNum == 23152 || dealNum == 23360 ) {
				Logging.debug(logPrefix + "Testing!!!!!     ");	
			}

			SimulationFactory sf = session.getSimulationFactory();
			ConstTable partyAgreementUDSR = getPartyAgreementUDSR(sf, revalResults);
			ConstTable vaRInfoUDSR = getVaRInfoUDSR(sf, revalResults);
			ConstTable vaRAndPAInfoUDSR = getVaRAndPAInfoUDSR(sf, revalResults);
			ConstTable vaRByTransResult = getVaRByTransResult(sf, revalResults, insType);
			// ConstTable vaRCorrelationMatrix = getVaRCorrelationMatrix(sf, revalResults);

			if (dealNum == 1492772) {
				Logging.debug(logPrefix + "Testing!!!!!     ");	
			}
			
			Table clientData = tf.createTable();
			clientData.select(partyAgreementUDSR, "deal_num, tran_num, ins_num, ins_type, external_lentity"
					+ ", param_seq_num, param_currency, pay_receive, base_mtm, mtm_exposure, party_agreement_id"
					+ ", netting_flag, collateral_agreement, collateral_valuation_date_seq, next_collateral_call_date"
					+ ", time_to_call_date, tran_type, haircut", "[deal_num] == " + dealNum);
			
			clientData.select(vaRInfoUDSR, "index_id, vol_id, gpt_id, gpt_label, gpt_name, gpt_date, gpt_sigma"
					+ ", gpt_time, delta,gamma", "[IN.tran_num] == [OUT.tran_num] AND [IN.ins_num] == [OUT.ins_num]"
					+ " AND [IN.param_seq_num] == [OUT.param_seq_num] AND [IN.party_agreement_id] == [OUT.party_agreement_id]");
							
			clientData.select(vaRByTransResult, "result->var_by_trans", "[IN.id] == [OUT.tran_num]");
			
			if (clientData != null && clientData.getRowCount() > 0) {
				clientData.setName("Credid PFE WorkSheet");
				double dealMtmExposure = getMtmExposureForDeal(clientData);
				// Get VaR By Tran Result from sim results instead of client data since client data will have same VaR 
				// value copied over for all grid points
				int row = vaRByTransResult.find(vaRByTransResult.getColumnId("id"), tranNum, 0);
				double dealVarExposure = row < 0 ? 0.0 : vaRByTransResult.getDouble("result", row);
				rowExposure = dealMtmExposure + dealVarExposure;
				
				// Set Exposure to 0 if cpty is internal LE
				rowExposure = isCptyInternalLE(session, clientData.getInt("external_lentity", 0)) ? 0: rowExposure;
			}

			Field[] fields = definition.getCriteriaFields(transaction);
			// Assumption: only Legal Entity is added to criteria fields in Exposure definition
			// Assumption: Call notice deals are only of type Nostro and Nostro-ML.
			if(insType == EnumInsType.CallNoticeNostro.getValue() || insType == EnumInsType.CallNoticeMultiLegNostro.getValue() ) {
//					|| dealNum == 1509282 || dealNum == 1462146 || dealNum == 1448275 ) {
				int overrideLE = getOverrideLEForCallNoticeDeals(session, transaction.getInstrumentId());
				// Find the External Legal Entity field
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].getCriteriaType().getId() == EnumRiskCriteria.ExtLentity.getValue()) {
						fields[i].setValue(overrideLE);
//						fields[i].setValue(21372);
					}
				}
			}
			
			dealExposure = definition.createDealExposure(rowExposure, transaction, fields);
			if (clientData != null)
				dealExposure.setClientData(clientData);

		} catch (Exception e) {
			logErrorWithException("Failed to calculate deal exposures " + e.getMessage());
		}
		return new DealExposure[] { dealExposure };
	}

	private double getMtmExposureForDeal(Table clientData) {
		
		double mtmExposure = 0.0;
		Table baseMtmExposure = tf.createTable();
		baseMtmExposure.selectDistinct(clientData, "deal_num, mtm_exposure", "deal_num >=0");
		for(int row = 0; row < baseMtmExposure.getRowCount(); row++) {
			mtmExposure += baseMtmExposure.getDouble("mtm_exposure", row);
		}
		baseMtmExposure.dispose();
		return mtmExposure;
	}

	@Override
	public RevalResults createDealCache(Session session, ExposureDefinition definition, Transactions transactions) {

		RevalResults revalResults = null;
		String logPrefix = this.getClass().getSimpleName() + ".createDealCache() : ";
		try {
			initialize(session, true);
			loadExposureArguments(definition);
			Logging.info(logPrefix + "Start Method");

			// If it is a quick credit check for fx swap, then tran_num = 0, first one will be near leg
			if (transactions.getCount() == 2 && transactions.getTransactionIds()[0] == 0) {
				transactions.get(0).assignTemporaryIds();
			}
			
			SimulationFactory sf = session.getSimulationFactory();
			ResultTypes resultTypes = sf.createResultTypes();
			resultTypes.add(sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME));
			resultTypes.add(sf.getResultType(SIM_VAR_INFO_UDSR_NAME));
//			resultTypes.add(sf.getResultType(SIM_VAR_AND_PA_UDSR_NAME));
			resultTypes.add(EnumResultType.VaRByTrans);
			// IF Batch status is running then the plugin is running in Batch Mode
			if(definition.getBatchStatus() == EnumBatchStatus.Running) {
				resultTypes.add(EnumResultType.VaRCorrelationMatrix);				
			}
			
			Simulation sim = sf.retrieveSimulation(CM_EXP_ARG_SAVED_SIM_VALUE);
			SimResults results = sim.run(transactions);
			revalResults = results.getScenarioResults(SIM_DEF_SCENARIO_NAME);
				
//			RevalSession reval = sf.createRevalSession(transactions);
//			// Set the base currency from the Exposure definition
//			reval.setCurrency(definition.getCurrency());
//			VaRDefinition vaRDefinition = getDefinition(session);
//			reval.setVaRDefinition(vaRDefinition);
//			// Prepare for reval
//			reval.calcIndexes();
//			reval.calcVolatilities();
//			reval.getVolatilityCollection();
//			Logging.debug(logPrefix + "Run revaluation for the selected results");
//			revalResults = reval.calcResults(resultTypes);
			Logging.debug(logPrefix + "Run revaluation for the selected results - completed");

			if (revalResults == null) {
				throw new OpenRiskException("Simulation run did not return revaluation results");
			}
		} catch (OpenRiskException e) {
			logErrorWithException("Failed to run simulation and create deal cache " + e.getMessage());
		} finally {
			Logging.info(logPrefix + "End Method");
		}
		return revalResults;
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
			}
		}
	}

	private ConstTable getVaRCorrelationMatrix(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(EnumResultType.VaRCorrelationMatrix));
	}
	private ConstTable getVaRByTransResult(SimulationFactory sf, RevalResults revalResults, int insType) {
		return simUtil.getGenResults(revalResults, sf.getResultType(EnumResultType.VaRByTrans), insType, 0, 0);
	}

	private ConstTable getPartyAgreementUDSR(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME));
	}

	private ConstTable getVaRInfoUDSR(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(SIM_VAR_INFO_UDSR_NAME));
	}
	
	private ConstTable getVaRAndPAInfoUDSR(SimulationFactory sf, RevalResults revalResults) {
		return simUtil.getGenResults(revalResults, sf.getResultType(SIM_VAR_AND_PA_UDSR_NAME));
	}
	
	
	@Override
	public void disposeDealCache(Session session, RevalResults dealCache) {
		dealCache.dispose();
	}

	@Override
	public double aggregateLineExposures(Session session, ExposureLine line, LineExposure[] exposures, Table exposureCache,
			boolean isInquiry) {

		double usage = 0.0;
		double vaRExposure = 0.0;
		double mtmExposure = 0.0;
		String logPrefix = this.getClass().getSimpleName() + ".aggregateLineExposures() : ";
		Table distinctPartyAgreement = null;
		Table clientDataForLineAndPA = null;
		Table pFEExposure = null;
		try {
			initialize(session, false);
			Logging.info(logPrefix + "Start Aggegate Line Exposures");
			Logging.info(logPrefix + "Exposure Line: " + line.toString());
			
			//Create Table for PFE Exposure calculation 
			pFEExposure = createPFEExposureTable();
			
			//Get distinct Party Agreement before any calculations
			distinctPartyAgreement = getDistinctPartyAgreement(exposures);
			// for each party Agreement against the cpty
			for(int row =0; row < distinctPartyAgreement.getRowCount(); row++ ) {
				
				int partyAgreementId = distinctPartyAgreement.getInt("party_agreement_id", row);
				clientDataForLineAndPA = getAllClientDataForPA(exposures, partyAgreementId);
				mtmExposure = getMtMExposureFORLineByPA(clientDataForLineAndPA);
				vaRExposure = getVarExposureForLineByPA(session, exposureCache, clientDataForLineAndPA);

				int rowPFE = pFEExposure.addRow().getNumber();
				pFEExposure.setInt("exposure_line_id", rowPFE, line.getId());
				pFEExposure.setInt("party_agreement_id", rowPFE, partyAgreementId);
				pFEExposure.setDouble("mtm_exposure", rowPFE, mtmExposure);
				pFEExposure.setDouble("var_exposure", rowPFE, vaRExposure);
				
				disposeTable(clientDataForLineAndPA);
			}
			
			// Once Mtm Exposure and Var Expore for each cpty for each party agreement is known
			for(TableRow row : pFEExposure.getRows() ) {
				usage += Math.max(row.getDouble("mtm_exposure") + row.getDouble("var_exposure"), 0);
			}

			// Set Exposure to 0 if cpty is internal LE
			usage = isCptyInternalLE(session, exposures[0].getClientData().getInt("external_lentity", 0)) ? 0: usage;
			
			
		} catch (OpenRiskException e) {
			logErrorWithException("Failed to aggregate line exposures " + e.getMessage());
		} finally {
			Logging.info(logPrefix + "End Aggegate Line Exposures");
			Logging.close();
		}
		return usage;
	}

	private boolean isCptyInternalLE(Session session, int extLentity) {
		
		boolean isIntLe = false;
		// Client data will have data only for one deal so get ins num from first row.
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT int_ext FROM party WHERE party_status = 1 AND party_id = ").append(extLentity);
		Table intLe = tf.createTable();
		intLe = session.getIOFactory().runSQL(sql.toString());
		if (intLe.getRowCount() <= 0) {
			throw new OpenRiskException("Query failed");
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
		Logging.info("Override Le fetched for call notice deal with ins num : " + insNum);
		return overrideHolderLE; 
	}

	private double getVarExposureForLineByPA(Session session, Table exposureCache, Table clientDataForLineAndPA) {

		// Summarise the client data for exposure line by index id and grid point id
		Table summary = clientDataForLineAndPA.calcByGroup("index_id, vol_id, gpt_id, gpt_label, gpt_name, gpt_date, gpt_time, gpt_sigma", "delta, gamma");

		// Get Grid Point info from exposure cache and replace sensitivity values with exposure line's values
		Table gptInfoOC = exposureCache.getTable("gpt_info", 0).cloneData();
//		Table gptInfoOC = exposureCache.getTable("gpt_info", 0).cloneStructure();
		gptInfoOC.select(summary, "index_id, vol_id, gpt_id, gpt_label, gpt_name, gpt_date, gpt_time, gpt_sigma, Sum delta->gpt_delta, Sum gamma->gpt_gamma", 
				"[IN.index_id] == [OUT.index_id] AND [IN.gpt_id] == [OUT.gpt_id]");
		
		com.olf.openjvs.Table gptInfo = tf.toOpenJvs(gptInfoOC);
		com.olf.openjvs.Table corrMatrix = tf.toOpenJvs(exposureCache.getTable("corr_matrix", 0));
		double vaRExposure = 0.0;
		try {
			double scale = getScaleForOneYear(session);
			// computeVaR method is not available in OC hence using JVS API
			vaRExposure = ValueAtRisk.computeVaR(corrMatrix, gptInfo, 1.0, scale * -1, CM_EXP_ARG_VAR_METHOD_VALUE,
					CM_EXP_ARG_CONFIDENCE_LVL_VALUE);
		} catch (OException e) {
			logErrorWithException("Failed to compute VaR: " + e.getMessage());
		}
		disposeTable(summary);
		disposeTable(gptInfoOC);
		return vaRExposure;
	}

	private double getScaleForOneYear(Session session) {

		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.YEAR, 1);
		CalendarFactory cf = session.getCalendarFactory();
		int gbd = cf.getHolidaySchedule("GBP").getGoodBusinessDayCount(date, cal.getTime());
		return Math.sqrt(gbd);
	}

	private double getMtMExposureFORLineByPA(Table clientDataForLineAndPA) {
		
		Table mtmExposurePA = tf.createTable();
		//Summarise the client data for exposure line by index id and grid point id
		Table clientDataDistinctValues = tf.createTable();
		clientDataDistinctValues.selectDistinct(clientDataForLineAndPA, "deal_num, param_seq_num, party_agreement_id, mtm_exposure",
				"[IN.deal_num] > 0");
		mtmExposurePA.selectDistinct(clientDataForLineAndPA, "party_agreement_id", "[IN.deal_num] > 0");
		mtmExposurePA.select(clientDataDistinctValues, "mtm_exposure->mtm_exposure", "[IN.party_agreement_id] == [OUT.party_agreement_id]",
				"SUM(mtm_exposure)");
		double mtmExposure = mtmExposurePA.getDouble("mtm_exposure", 0);
		disposeTable(mtmExposurePA);
		disposeTable(clientDataDistinctValues);
		return mtmExposure;
	}

	private Table getAllClientDataForPA(LineExposure[] exposures, int partyAgreementId) {
		
		//Gather all the client data tables into a single table for the current exposure line
		Table clientDataForLineAndPA = (exposures.length >0) ? exposures[0].getClientData().cloneStructure() : null;
		for (LineExposure exposure : exposures) {
			ConstTable clientData = exposure.getClientData();
			int dealNum = clientData.getInt("deal_num", 0);
			if(dealNum == 1498314 || dealNum == 1463888 || dealNum == 1520454 || dealNum == 23152 || dealNum == 23360 ) {
				Logging.debug("Testing!!!!!     ");	
			}
			
			// Since a deal can have only one Party Agreement it is sufficient to check first row.
			int partyAgreementIdCD = clientData.getInt("party_agreement_id", 0);
			if(partyAgreementId == partyAgreementIdCD) {
				clientDataForLineAndPA.appendRows(clientData);
			}
		}
		return clientDataForLineAndPA;
	}

	private Table createPFEExposureTable() {
		
		Table pFEExposure = tf.createTable();
		pFEExposure.addColumn("exposure_line_id", EnumColType.Int);
		pFEExposure.addColumn("external_lentity", EnumColType.Int);
		pFEExposure.addColumn("party_agreement_id", EnumColType.Int);
		pFEExposure.addColumn("mtm_exposure", EnumColType.Double);
		pFEExposure.addColumn("var_exposure", EnumColType.Double);
		return pFEExposure;
	}

	private Table getDistinctPartyAgreement(LineExposure[] exposures) {

		Table distinctPartyAgreement = tf.createTable();
		for (LineExposure exposure : exposures) {
			ConstTable clientData = exposure.getClientData();
			int dealNum = clientData.getInt("deal_num", 0);
			if(dealNum == 1498314 || dealNum == 1463888 || dealNum == 1520454 || dealNum == 23152 || dealNum == 23360 ) {
				Logging.debug("Testing!!!!!     ");	
			}
			distinctPartyAgreement.select(clientData, "party_agreement_id", "[IN.party_agreement_id] > -1");
		}
		distinctPartyAgreement.makeDistinct("party_agreement_id", "party_agreement_id > -1");

		return distinctPartyAgreement;
	}

	private void initialize(Session session, boolean isNewRun) {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			VAR_DEFINITION = constRepo.getStringValue(CR_VARIABLE_VAR_DEFINITION);
			
			tf = session.getTableFactory();

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

}