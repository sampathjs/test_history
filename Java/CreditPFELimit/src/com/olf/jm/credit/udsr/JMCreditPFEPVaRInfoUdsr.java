package com.olf.jm.credit.udsr;

import java.util.Date;

/**********************************************************************************************************************
 * File Name:                  JMCreditPFEPVaRInfoUdsr.java
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
 * Type of Script:             UDSR
 * 
 * History
 * 20-Aug-2021  GanapP02   EPI-xxxx     Initial version - UDSR to get tran Gpt Delta by leg and VaR Gpt Raw data   
 *                                      required for Credit PFE calculations
 *********************************************************************************************************************/

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.jm.logging.Logging;
import com.olf.jm.util.SimUtil;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.market.EnumIdxPurpose;
import com.olf.openrisk.simulation.Configuration;
import com.olf.openrisk.simulation.ConfigurationField;
import com.olf.openrisk.simulation.EnumConfiguration;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumReceivePay;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class JMCreditPFEPVaRInfoUdsr extends AbstractSimulationResult2 {
	
	private static IOFactory iof;
	private static TableFactory tf;
	private static SimulationFactory sf;
	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;
	
	private SimUtil simUtil = new SimUtil();
	
	private static final String CONST_REPO_CONTEXT = "JM Credit PFE";
	private static final String CONST_REPO_SUBCONTEXT = "VaR Info UDSR";
	
	private static final String CR_VAR_PA_UDSR_NAME = "Party Agreement udsr name";
	private static final String CR_VAR_TRGT_RESULT_SELECTION = "Target Result Selection";
	private static final String CR_VAR_TRGT_RESULT_PARAM_SCALING = "Target Result Parameter Scaling";
	private static final String CR_VAR_LOG_DEBUG_MESSAGES = "logDebugMessages";
	
	private static String SIM_PARTY_AFREEMENT_UDSR_NAME = "JM Credit PFE Party Agreement";
	private static String TARGET_RESULT_SELECTION = "Parametric VaR Attributes";
	private static String TARGET_RESULT_PARAMETER_SCALING = "Input Volatility Scaling Factor";
	private static boolean LOG_DEBUG_MSG = false;
	
	private static Date scenarioDate;
	private static String CLASSNAME = "";

	JMCreditPFEPVaRInfoUdsr() {
		CLASSNAME = this.getClass().getSimpleName();
	}
	
	@Override
	public void calculate(Session session, Scenario scenario, RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".calculate() : ";
		try {
			initialize(session, scenario);
			Logging.info(logPrefix + "JM Credit PFE VaR Info UDSR - Start for scenario id " + scenario.getId());

			Table tranList = getTranList(transactions);
			ConstTable varGptRawData = getVaRGridPointRawData(prerequisites);
			ConstTable varCorrMatrix = getVaRCorrelationMatrix(prerequisites);
			ConstTable creditPFEPA = getJMCreditPFEPartyAgreement(prerequisites);
			Table tranGptDeltaByLeg = getTranGptDeltaByLeg(prerequisites).asTable();
			filteroutNonVaRIncedies(tranGptDeltaByLeg);

			Table creditPFEVaRInfo = createOutputTable();
			creditPFEVaRInfo.select(tranList, "deal_num, tran_num, ins_num, toolset, ins_type, start_date, end_date, external_lentity"
					+ ", buy_sell", "[IN.deal_num] >= 0");
			creditPFEVaRInfo.select(creditPFEPA,
					"param_seq_num, party_agreement_id, time_to_call_date, next_collateral_call_date, pay_receive, netting_flag",
					"[IN.deal_num] == [OUT.deal_num] AND [IN.tran_num] == [OUT.tran_num] AND [IN.ins_num] == [OUT.ins_num]");
			creditPFEVaRInfo.select(tranGptDeltaByLeg, "index->index_id, gpt_id, delta, gamma",
					"[IN.deal_num] == [OUT.deal_num] AND [IN.deal_leg] == [OUT.param_seq_num]");
			creditPFEVaRInfo.select(varGptRawData, "gpt_label, gpt_name, gpt_date, gpt_time, gpt_sigma", "[IN.index_id] == [OUT.index_id]"
					+ " AND [IN.vol_id] == [OUT.vol_id] AND [IN.gpt_id] == [OUT.gpt_id]");

			updateGreekValuesForScenario(session, creditPFEVaRInfo);
			
			scaleGreeksColumns(scenario, creditPFEVaRInfo);

			filterOutPayNonNettingRows(creditPFEVaRInfo);

			revalResult.setTable(creditPFEVaRInfo);

		} catch (OpenRiskException e) {
			String message = "Failed to run sim result: " + e.getMessage();
			Logging.error(message);
			throw new OpenRiskException(message);
		}

		Logging.info(logPrefix + "JM Credit PFE VaR Info UDSR - completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		Logging.close();
	}

	private void updateGreekValuesForScenario(Session session, Table creditPFEVaRInfo) {
		
		try {
			int rowCount = creditPFEVaRInfo.getRowCount();
			CalendarFactory cf = session.getCalendarFactory();
			for (int row = 0; row < rowCount; row++) {
				Date nextCallDate = creditPFEVaRInfo.getDate("next_collateral_call_date", row);
				Date maturityDate = creditPFEVaRInfo.getInt("end_date", row) == 0 ? cf.getDate(creditPFEVaRInfo.getInt("start_date", row))
						: cf.getDate(creditPFEVaRInfo.getInt("end_date", row));
				if (scenarioDate.after(nextCallDate) && scenarioDate.after(maturityDate)) {
					creditPFEVaRInfo.setDouble("delta", row, 0.0);
					creditPFEVaRInfo.setDouble("gamma", row, 0.0);
					creditPFEVaRInfo.setDouble("gpt_sigma", row, 0.0);
				}
			}
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add MTM Exposure values : " + e.getMessage());
		}
	}

	@Override
	public void format(final Session session, final RevalResult revalResult) {

		Table result = revalResult.getTable();
		TableFormatter formatter = result.getFormatter();
		
		formatter.setColumnTitle("deal_num", "Deal Number");
		formatter.setColumnTitle("tran_num", "Tran Number");
		formatter.setColumnTitle("ins_num", "ins Number");
		formatter.setColumnTitle("toolset", "Toolset");
		formatter.setColumnTitle("ins_type", "Instrument");
		formatter.setColumnTitle("start_date", "Start Date");
		formatter.setColumnTitle("end_date", "Maturity Date");
		formatter.setColumnTitle("external_lentity", "External\nLegal Entity");
		formatter.setColumnTitle("buy_sell", "Buy Sell");
		formatter.setColumnTitle("param_seq_num", "Deal Leg");
		formatter.setColumnTitle("party_agreement_id", "Party Agreement");
		formatter.setColumnTitle("index_id", "Index\nName");
		formatter.setColumnTitle("vol_id", "Volatility\nName");
		formatter.setColumnTitle("gpt_id", "Gpt\nID");
		formatter.setColumnTitle("gpt_label", "Gpt\nLabel");
		formatter.setColumnTitle("gpt_name", "Gpt\nName");
		formatter.setColumnTitle("gpt_date", "Gpt\nDate");
		formatter.setColumnTitle("gpt_sigma", "Gpt\nSigma");
		formatter.setColumnTitle("gpt_time", "Gpt Time\n(minutes in day)");
		formatter.setColumnTitle("delta", "Gpt\nDelta");
		formatter.setColumnTitle("gamma", "Gpt\nGamma");
		formatter.setColumnTitle("time_to_call_date", "Time To Call Date");
		formatter.setColumnTitle("pay_receive", "Pay/Receive");
		formatter.setColumnTitle("netting_flag", "Netting");
		
		formatter.setColumnFormatter("toolset", formatter.createColumnFormatterAsRef(EnumReferenceTable.Toolsets));
		formatter.setColumnFormatter("ins_type", formatter.createColumnFormatterAsRef(EnumReferenceTable.Instruments));
		formatter.setColumnFormatter("external_lentity", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));
		formatter.setColumnFormatter("buy_sell", formatter.createColumnFormatterAsRef(EnumReferenceTable.BuySell));
		formatter.setColumnFormatter("pay_receive", formatter.createColumnFormatterAsRef(EnumReferenceTable.RecPay));
		formatter.setColumnFormatter("party_agreement_id", formatter.createColumnFormatterAsRef(EnumReferenceTable.PartyAgreement));
		formatter.setColumnFormatter("index_id", formatter.createColumnFormatterAsRef(EnumReferenceTable.Index));
		formatter.setColumnFormatter("vol_id", formatter.createColumnFormatterAsRef(EnumReferenceTable.Volatility));
		formatter.setColumnFormatter("gpt_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("gpt_time", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Time));
		
		formatter.setColumnFormatter("gpt_sigma", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		formatter.setColumnFormatter("delta", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		formatter.setColumnFormatter("gamma", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		formatter.setColumnFormatter("time_to_call_date", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));

		formatter.setColumnFormatter("start_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("end_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
	}

	private void filterOutPayNonNettingRows(Table creditPFEVaRInfo) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".filterOutPayNonNettingRows() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			int numOfRows = creditPFEVaRInfo.getRowCount();
			for (int row = numOfRows - 1; row >= 0; row--) {
				if (creditPFEVaRInfo.getInt("pay_receive", row) == EnumReceivePay.Pay.getValue()
						&& "No".equalsIgnoreCase(creditPFEVaRInfo.getString("netting_flag", row))
						&& creditPFEVaRInfo.getInt("toolset", row) != EnumToolset.ComSwap.getValue()
						) {
					creditPFEVaRInfo.removeRow(row);
				}
			}
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to filter out non netting rows: " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private void scaleGreeksColumns(Scenario scenario, Table creditPFEVaRInfo) {

		boolean useScalingFromScenario = false;
		double scaling = 1.0;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".scaleGreeksColumns() : ";
		logDebugMsg(logPrefix + "method started");
		ConfigurationField confField = scenario.getConfigurationField(EnumConfiguration.Result, TARGET_RESULT_SELECTION,
				TARGET_RESULT_PARAMETER_SCALING);
		useScalingFromScenario = confField != null;
//		scaling = confField.getValueAsDouble();

		int rowCount = creditPFEVaRInfo.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			double sigma = creditPFEVaRInfo.getDouble("gpt_sigma", row);
			if (!useScalingFromScenario) {
				double timeToCallDate = creditPFEVaRInfo.getDouble("time_to_call_date", row);
				scaling = Math.sqrt(timeToCallDate);
			}
			creditPFEVaRInfo.setDouble("gpt_sigma", row, sigma * scaling);

			double delta = creditPFEVaRInfo.getDouble("delta", row);
			creditPFEVaRInfo.setDouble("delta", row, delta * scaling);
			double gamma = creditPFEVaRInfo.getDouble("gamma", row);
			creditPFEVaRInfo.setDouble("gamma", row, gamma * scaling);
		}
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
	}

	private void filteroutNonVaRIncedies(Table tranGptDeltaByLeg) {

		Table varIndices = null;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".filteroutNonVaRIncedies() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT id.index_id, 1 AS include_flag");
			sql.append("\n FROM idx_def id");
			sql.append("\n WHERE  db_status = 1 AND validated = 1");
			sql.append("\n   AND purpose = ").append(EnumIdxPurpose.ValueAtRisk.getValue());
			varIndices = iof.runSQL(sql.toString());

			if (varIndices.getRowCount() <= 0) {
				throw new OpenRiskException("Failed to load VaR indices from database");
			}
			tranGptDeltaByLeg.select(varIndices, "include_flag", "[IN.index_id] == [OUT.index]");
			int numOfRows = tranGptDeltaByLeg.getRowCount();
			for (int row = numOfRows - 1; row >= 0; row--) {
				if (tranGptDeltaByLeg.getInt("include_flag", row) == 0) {
					tranGptDeltaByLeg.removeRow(row);
				}
			}
			tranGptDeltaByLeg.removeColumn("include_flag");

		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to remove non VaR indices: " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private ConstTable getVaRGridPointRawData(RevalResults prerequisites) throws OpenRiskException {
		try {
			return simUtil.getGenResults(prerequisites, sf.getResultType(EnumResultType.VaRGridPointRawData));
		} catch (Exception e) {
			throw new OpenRiskException("Check if VaR Definition is added to reval: " + e.getMessage());
		}
	}

	private ConstTable getVaRCorrelationMatrix(RevalResults prerequisites) throws OpenRiskException {
		return simUtil.getGenResults(prerequisites, sf.getResultType(EnumResultType.VaRCorrelationMatrix));
	}

	private ConstTable getTranGptDeltaByLeg(RevalResults prerequisites) throws OpenRiskException {
		return simUtil.getGenResults(prerequisites, sf.getResultType(EnumResultType.TranGptDeltaByLeg), false);
	}

	private ConstTable getJMCreditPFEPartyAgreement(RevalResults prerequisites) throws OpenRiskException {
		return simUtil.getGenResults(prerequisites, sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME));
	}

	public Table getTranList(Transactions transactions) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".getTranList() : ";
		logDebugMsg(logPrefix + "method started");
		EnumTransactionFieldId[] fields = { 
				EnumTransactionFieldId.ExternalLegalEntity,
				EnumTransactionFieldId.BuySell,
				EnumTransactionFieldId.InstrumentId,
				EnumTransactionFieldId.Toolset,
				EnumTransactionFieldId.InstrumentType,
				EnumTransactionFieldId.TransactionType,
				EnumTransactionFieldId.StartDate,
				EnumTransactionFieldId.MaturityDate
		};
		Table tranList = simUtil.getTranList(transactions, fields);
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");

		return tranList;
	}
	
	protected Table createOutputTable() {
		Table data = tf.createTable("JM Credit PFE VaR Info");

		data.addColumn("deal_num", EnumColType.Int);
		data.addColumn("tran_num", EnumColType.Int);
		data.addColumn("ins_num", EnumColType.Int);
		data.addColumn("toolset", EnumColType.Int);
		data.addColumn("ins_type", EnumColType.Int);
		data.addColumn("start_date", EnumColType.Int);
		data.addColumn("end_date", EnumColType.Int);
		data.addColumn("external_lentity", EnumColType.Int);
		data.addColumn("buy_sell", EnumColType.Int);
		data.addColumn("param_seq_num", EnumColType.Int);
		data.addColumn("party_agreement_id", EnumColType.Int);
		data.addColumn("index_id", EnumColType.Int);
		data.addColumn("vol_id", EnumColType.Int);
		data.addColumn("gpt_id", EnumColType.Int);
		data.addColumn("gpt_label", EnumColType.String);
		data.addColumn("gpt_name", EnumColType.String);
		data.addColumn("gpt_date", EnumColType.Int);
		data.addColumn("gpt_sigma", EnumColType.Double);
		data.addColumn("gpt_time", EnumColType.Int);
		data.addColumn("delta", EnumColType.Double);
		data.addColumn("gamma", EnumColType.Double);
		data.addColumn("time_to_call_date", EnumColType.Double);
		data.addColumn("pay_receive", EnumColType.Int);
		data.addColumn("netting_flag", EnumColType.String);
		return data;
	}

	private void initialize(Session session, Scenario scenario) {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			
			sf = session.getSimulationFactory();
			iof = session.getIOFactory();
			tf = session.getTableFactory();

			SIM_PARTY_AFREEMENT_UDSR_NAME = constRepo.getStringValue(CR_VAR_PA_UDSR_NAME, SIM_PARTY_AFREEMENT_UDSR_NAME);
			TARGET_RESULT_SELECTION = constRepo.getStringValue(CR_VAR_TRGT_RESULT_SELECTION, TARGET_RESULT_SELECTION);
			TARGET_RESULT_PARAMETER_SCALING = constRepo.getStringValue(CR_VAR_TRGT_RESULT_PARAM_SCALING, TARGET_RESULT_PARAMETER_SCALING);
			LOG_DEBUG_MSG = constRepo.getIntValue(CR_VAR_LOG_DEBUG_MESSAGES) == 1 ? true : false;
			
			Configuration scenarioDateCfg = scenario.getConfigurations().find(EnumConfiguration.Date, "Scenario Date");
			scenarioDate = scenarioDateCfg == null ? session.getCalendarFactory().createSymbolicDate("0cd").evaluate()
					: scenarioDateCfg.getFields().getField("Current Date").getValueAsDate();
			
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to initialize : " + e.getMessage());
		} catch (OException e) {
			throw new OpenRiskException("Failed to load const repository values : " + e.getMessage());
		}
		Logging.info("********************* Start of new run ***************************");
	}

	private void logDebugMsg(String message){
		if(LOG_DEBUG_MSG) {
			Logging.debug(message);
		}
	}
	
}
