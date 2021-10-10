package com.olf.jm.credit.udsr;

/**********************************************************************************************************************
 * File Name:                  JMCreditPFEPVaRAndPAUdsr.java
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
 * 20-Aug-2021  GanapP02   EPI-xxxx     Initial version - UDSR to get Party Agreement, Base MTM, tran Gpt Delta by leg
 *                                       and VaR Gpt Raw data required for Credit PFE calculations
 *********************************************************************************************************************/

import java.util.Date;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.jm.logging.Logging;
import com.olf.jm.util.SimUtil;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedules;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.market.EnumIdxPurpose;
import com.olf.openrisk.simulation.Configuration;
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
public class JMCreditPFEPVaRAndPAUdsr extends AbstractSimulationResult2 {

	IOFactory iof;
	SimulationFactory sf;
	CalendarFactory cf;
	TableFactory tf;

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;

	private SimUtil simUtil = new SimUtil();

	private static final String SIM_PARTY_AFREEMENT_UDSR_NAME = "JM Credit PFE Party Agreement";
	private static final String CONST_REPO_CONTEXT = "JM Credit PFE";
	private static final String CONST_REPO_SUBCONTEXT = "JM Credit PFE UDSR";
	private static final String CR_VAR_SIM_RES_GROUP = "SimResultGroup";
	private static final String CR_VAR_SIM_ATTR_COL_CALL_FREQ = "CollateralCallFreq";
	private static final String CR_VAR_SIM_ATTR_PFE_CALC_HORIZON = "PFECalcHorizon";
	private static String SIM_ATTR_COL_CALL_FREQ_NAME = "";
	private static String SIM_ATTR_PFE_CALC_HORIZON_NAME = "";
	private static String SIM_RES_GROUP = "";
	private static String SIM_ATTR_COL_CALL_FREQ_VALUE = "";
	private static String SIM_ATTR_PFE_CALC_HORIZON_VALUE = "";

	@Override
	public void calculate(Session session, Scenario scenario, RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) {

		try {
			initialize(session);
			Logging.info("JM Credit PFE VaR and Party Agreement UDSR - Start");

			Table creditPFEPAData = createOutputTable();

			addTransactionDetails(creditPFEPAData, transactions);

			addBaseMtmData(creditPFEPAData, prerequisites);

			addPMCurrencyTable(creditPFEPAData);

			populateSimResultAttributes(revalResult);

			addPayRec(transactions, creditPFEPAData);

			addPartyAgreementFields(creditPFEPAData);

			addCollateralCallDates(scenario, creditPFEPAData);

			addhaircut(creditPFEPAData);

			addMtmExposure(creditPFEPAData);

			addTranGptDeltaByLeg(creditPFEPAData, prerequisites);

			addVaRGridPointRawData(creditPFEPAData, prerequisites);

			scaleGreeksColumns(creditPFEPAData);

			filterOutPayNonNettingRows(creditPFEPAData);

			revalResult.setTable(creditPFEPAData);

		} catch (OpenRiskException e) {
			Logging.error(e.getMessage());
			throw new OpenRiskException(e.getMessage());
		}

		Logging.info("JM Credit PFE VaR and Party Agreement UDSR - Complete");
		Logging.close();

	}

	@Override
	public void format(final Session session, final RevalResult revalResult) {

		Table result = revalResult.getTable();
		TableFormatter formatter = result.getFormatter();

		formatter.setColumnTitle("deal_num", "Deal Number");
		formatter.setColumnTitle("tran_num", "Tran Number");
		formatter.setColumnTitle("ins_num", "ins Number");
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

	}

	private void addTransactionDetails(Table creditPFEPAData, Transactions transactions) {

		EnumTransactionFieldId[] fields = { 
				EnumTransactionFieldId.ExternalLegalEntity,
				EnumTransactionFieldId.BuySell,
				EnumTransactionFieldId.InstrumentId,
				EnumTransactionFieldId.InstrumentType,
				EnumTransactionFieldId.TransactionType,
				EnumTransactionFieldId.PartyAgreement };

		Table tranList = simUtil.getTranList(transactions, fields);
		creditPFEPAData.select(tranList, "deal_num, tran_num, ins_num, ins_type, external_lentity, tran_type, party_agreement_id",
				"[IN.deal_num] > -1");
	}

	private void addBaseMtmData(Table creditPFEPAData, RevalResults prerequisites) {

		ConstTable baseMtM = simUtil.getTranResults(prerequisites, sf.getResultType(EnumResultType.BaseMtm));
//		creditPFEPAData.select(baseMtM,
//				"deal_leg->param_seq_num, currency_id->param_currency, " + EnumResultType.BaseMtm.getValue() + "->base_mtm",
//				"[IN.deal_num] == [OUT.deal_num] AND [IN.ins_num] == [OUT.ins_num]");
		
		ConstTable baseMtMNonCommSwap = baseMtM.createConstView("*", "ins_type !=" + EnumInsType.MetalSwap.getValue())
				.createConstView("*", "ins_type !=" + EnumInsType.MetalBasisSwap.getValue());
		ConstTable baseMtMCommSwap = baseMtM.createConstView("*",
				"ins_type ==" + EnumInsType.MetalSwap.getValue() + " OR ins_type ==" + EnumInsType.MetalBasisSwap.getValue());

		creditPFEPAData.select(baseMtMNonCommSwap,
				"deal_leg->param_seq_num, currency_id->param_currency, " + EnumResultType.BaseMtm.getValue() + "->base_mtm",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.ins_num] == [OUT.ins_num]");
		
		creditPFEPAData.select(baseMtMCommSwap, "deal_leg->param_seq_num, currency_id->param_currency",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.ins_num] == [OUT.ins_num]");
		
		Table commSwap = tf.createTable();
		commSwap.selectDistinct(baseMtMCommSwap, "deal_num", "[IN.deal_num] > 0");
		commSwap.addColumn("param_seq_num", EnumColType.Int);
		commSwap.select(baseMtMCommSwap, EnumResultType.BaseMtm.getValue() + "->base_mtm", "[IN.deal_num] == [OUT.deal_num]",
				"SUM(base_mtm)");
		creditPFEPAData.select(commSwap, "base_mtm", "[IN.deal_num] == [OUT.deal_num] AND [IN.param_seq_num] == [OUT.param_seq_num]");
	}

	private void addPMCurrencyTable(Table creditPFEPAData) {

		Table pmCurrency = iof.runSQL("SELECT id_number AS currency_id, 1 AS metal_leg FROM currency WHERE precious_metal = 1");
		creditPFEPAData.select(pmCurrency, "metal_leg", "[IN.currency_id] == [OUT.param_currency]");
	}

	private void populateSimResultAttributes(RevalResult revalResult) {

		Table pfolioResult = null;
		try {
			// ResultAttributeGroup.getAttributes throws an exception so getting the values directly from db tables
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT prt.name result_name, prt.id_number, pragi.res_attr_grp_id, pragi.res_attr_id");
			sql.append("\n   , prat.res_attr_name, pragi.value");
			sql.append("\n FROM pfolio_result_type prt ");
			sql.append("\n JOIN pfolio_result_cfg_map prcm ON prcm.result_type = prt.id_number");
			sql.append("\n JOIN pfolio_result_attr_grp_items pragi ON pragi.res_attr_grp_id = prcm.res_attr_grp_id");
			sql.append("\n JOIN pfolio_result_attr_types prat ON prat.res_attr_id = pragi.res_attr_id");
			sql.append("\n WHERE prt.name = '").append(revalResult.getName()).append("'");
			pfolioResult = iof.runSQL(sql.toString());

			if (pfolioResult.getRowCount() <= 0) {
				throw new OpenRiskException("Failed to fetch simulation result attributes");
			}
			SIM_ATTR_COL_CALL_FREQ_VALUE = getValueFromTable(pfolioResult, SIM_ATTR_COL_CALL_FREQ_NAME);
			SIM_ATTR_PFE_CALC_HORIZON_VALUE = getValueFromTable(pfolioResult, SIM_ATTR_PFE_CALC_HORIZON_NAME);

		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to get simulation result attribute: " + e.getMessage());
		}
	}

	private String getValueFromTable(Table pfolioResult, String name) throws OpenRiskException {

		String value = "";
		int row = pfolioResult.find(pfolioResult.getColumnId("res_attr_name"), name, 0);
		if (row >= 0) {
			value = pfolioResult.getString("value", row);
		} else {
			throw new OpenRiskException("Failed to fetch value for simulation result attribute : " + name);
		}
		return value;
	}

	private void addPayRec(Transactions transactions, Table creditPFEPAData) {

		Table payRec = null;
		try {
			QueryResult qr = iof.createQueryResult();
			qr.add(transactions.getTransactionIds());
			int queryId = qr.getId();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ab.deal_tracking_num deal_num, ab.tran_num, ab.ins_num, ip.param_seq_num");
			sql.append("\n   , CASE WHEN ab.toolset = ").append(EnumToolset.Fx.getValue());
			sql.append("\n               AND ab.buy_sell = 0 AND ip.param_seq_num = 0 THEN 0");
			sql.append("\n          WHEN ab.toolset = ").append(EnumToolset.Fx.getValue());
			sql.append("\n               AND ab.buy_sell = 0 AND ip.param_seq_num = 1 THEN 1");
			sql.append("\n          WHEN ab.toolset = ").append(EnumToolset.Fx.getValue());
			sql.append("\n               AND ab.buy_sell = 1 AND ip.param_seq_num = 0 THEN 1");
			sql.append("\n          WHEN ab.toolset = ").append(EnumToolset.Fx.getValue());
			sql.append("\n               AND ab.buy_sell = 1 AND ip.param_seq_num = 1 THEN 0");
			sql.append("\n          WHEN upper(i.name) like Upper('%nostro%') THEN 0");
			sql.append("\n          WHEN upper(i.name) like Upper('%vostro%') THEN 1");
			sql.append("\n          ELSE ip.pay_rec");
			sql.append("\n          END AS pay_rec");
			sql.append("\n  FROM ab_tran ab");
			sql.append("\n  JOIN ins_parameter ip ON ab.ins_num = ip.ins_num");
			sql.append("\n  JOIN instruments i ON ab.ins_type = i.id_number ");
			sql.append("\n  JOIN query_result qr ON ab.tran_num = qr.query_result AND qr.unique_id = ").append(queryId);
			sql.append("\n WHERE ab.current_flag = 1");
			payRec = iof.runSQL(sql.toString());
			if (payRec.getRowCount() < 0) {
				throw new OpenRiskException("Query failed");
			} else {
				creditPFEPAData.select(payRec, "pay_rec->pay_receive",
						"[IN.deal_num] == [OUT.deal_num] " + "AND [IN.tran_num] == [OUT.tran_num] AND [IN.ins_num] == [OUT.ins_num] "
								+ "AND [IN.param_seq_num] == [OUT.param_seq_num]");
			}
			Logging.info("Pay Rec added to the table.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to load pay rec from database table : " + e.getMessage());
		} finally {
			payRec.dispose();
		}
	}

	private void addPartyAgreementFields(Table creditPFEPAData) {

		Table partyAgreementList = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT intbu.party_id internal_bunit, extbu.party_id external_bunit, pa.party_agreement_id");
			sql.append("\n     , netting_flag, haircut_calculator_flag, haircut_method, ai.ins_type");
			sql.append("\n     , pac.valuation_date_sequence");
			sql.append("\n  FROM party_agreement pa");
			sql.append("\n  JOIN party_agreement_assignment intbu ON pa.party_agreement_id = intbu.party_agreement_id");
			sql.append("\n       AND intbu.internal_external_flag = 0");
			sql.append("\n  JOIN party_agreement_assignment extbu ON pa.party_agreement_id = extbu.party_agreement_id");
			sql.append("\n       AND extbu.internal_external_flag = 1");
			sql.append("\n  JOIN agreement_ins ai ON pa.agreement_id = ai.agreement_id");
			sql.append("\n  LEFT JOIN party_agreement_collateral pac ON pa.agreement_id = pac.party_agreement_id");
			sql.append("\n WHERE pa.doc_status = 1");
			partyAgreementList = iof.runSQL(sql.toString());

			int rowCount = creditPFEPAData.getRowCount();
			for (int row = 0; row < rowCount; row++) {
				int partyAgreement = creditPFEPAData.getValueAsInt("party_agreement_id", row);
				String netting = "No";
				String valDateSeq = "";
				if (partyAgreement > 0) {
					int partyAgrmtRow = partyAgreementList.find(partyAgreementList.getColumnId("party_agreement_id"), partyAgreement, 0);
					if (partyAgrmtRow > 0) {
						netting = partyAgreementList.getInt("netting_flag", partyAgrmtRow) == 0 ? "No" : "Yes";
						valDateSeq = partyAgreementList.getString("valuation_date_sequence", partyAgrmtRow);
					}
				}
				creditPFEPAData.setString("netting_flag", row, netting);
				String collAgreement = partyAgreement <= 0 ? "No" : "Yes";
				creditPFEPAData.setString("collateral_agreement", row, collAgreement);
				valDateSeq = "Yes".equalsIgnoreCase(collAgreement)
						? ("".equalsIgnoreCase(valDateSeq) ? SIM_ATTR_COL_CALL_FREQ_VALUE : valDateSeq) : SIM_ATTR_PFE_CALC_HORIZON_VALUE;
				creditPFEPAData.setString("collateral_valuation_date_seq", row, valDateSeq);
			}
			Logging.info("Party agreement info updated.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to load party agreement netting flag : " + e.getMessage());
		} finally {
			partyAgreementList.dispose();
		}
	}

	private void addCollateralCallDates(Scenario scenario, Table creditPFEPAData) {

		try {
			Configuration scenarioDateCfg = scenario.getConfigurations().find(EnumConfiguration.Date, "Scenario Date");
			Date scenarioDate = scenarioDateCfg == null ? cf.createSymbolicDate("0cd").evaluate()
					: scenarioDateCfg.getFields().getField("Current Date").getValueAsDate();
			HolidaySchedules hs = cf.createHolidaySchedules();
			hs.addSchedule(scenario.getCurrency().getHolidaySchedule());

			int rowCount = creditPFEPAData.getRowCount();
			for (int row = 0; row < rowCount; row++) {
				SymbolicDate valDateSeq = cf.createSymbolicDate(creditPFEPAData.getString("collateral_valuation_date_seq", row));
				valDateSeq.setHolidaySchedules(hs);
				Date nextCallDate = valDateSeq.evaluate(scenarioDate);
				creditPFEPAData.setDate("next_collateral_call_date", row, nextCallDate);

				double timeToCallDate = (cf.getJulianDate(nextCallDate) - cf.getJulianDate(scenarioDate)) / 365;
				creditPFEPAData.setDouble("time_to_call_date", row, timeToCallDate);
			}
			Logging.info("Collateral Call dates updated.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add Collateral Call dates : " + e.getMessage());
		}
	}

	private void addhaircut(Table creditPFEPAData) {
		try {
			creditPFEPAData.setColumnValues(creditPFEPAData.getColumnId("haircut"), 1.0);
			Logging.info("Haircut values added.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add haircut values: " + e.getMessage());
		}
	}

	private void addMtmExposure(Table creditPFEPAData) {

		try {
			int rowCount = creditPFEPAData.getRowCount();
			for (int row = 0; row < rowCount; row++) {
				if(creditPFEPAData.getInt("deal_num", row) == 1509282) {
					Logging.info("Testing.");		
				}
				double haircut = creditPFEPAData.getDouble("haircut", row);
				String netting = creditPFEPAData.getString("netting_flag", row);
				double baseMtm = creditPFEPAData.getDouble("base_mtm", row);
				int payRec = creditPFEPAData.getInt("pay_receive", row);
				double mtmExposure = "Yes".equalsIgnoreCase(netting) ? haircut * baseMtm
						: payRec == EnumReceivePay.Pay.getValue() ? 0.0 : Math.max(0.0, haircut * baseMtm);
				creditPFEPAData.setDouble("mtm_exposure", row, mtmExposure);
			}
			Logging.info("MTM Exposure values calculated and added.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add MTM Exposure values : " + e.getMessage());
		}
	}

	private void addTranGptDeltaByLeg(Table creditPFEPAData, RevalResults prerequisites) {

		Table tranGptDeltaByLeg = getTranGptDeltaByLeg(prerequisites).asTable();
		filteroutNonVaRIncedies(tranGptDeltaByLeg);
		creditPFEPAData.select(tranGptDeltaByLeg, "index->index_id, gpt_id, delta, gamma",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.deal_leg] == [OUT.param_seq_num]");
	}

	private void addVaRGridPointRawData(Table creditPFEPAData, RevalResults prerequisites) {

		ConstTable varGptRawData = getVaRGridPointRawData(prerequisites);
		creditPFEPAData.select(varGptRawData, "gpt_label, gpt_name, gpt_date, gpt_time, gpt_sigma",
				"[IN.index_id] == [OUT.index_id]" + " AND [IN.vol_id] == [OUT.vol_id] AND [IN.gpt_id] == [OUT.gpt_id]");
	}

	private void filterOutPayNonNettingRows(Table creditPFEVaRInfo) {

		try {
			int numOfRows = creditPFEVaRInfo.getRowCount();
			for (int row = numOfRows - 1; row >= 0; row--) {
				if (creditPFEVaRInfo.getInt("pay_receive", row) == EnumReceivePay.Pay.getValue()
						&& "No".equalsIgnoreCase(creditPFEVaRInfo.getString("netting_flag", row))) {
					creditPFEVaRInfo.removeRow(row);
				}
			}
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to filter out non netting rows: " + e.getMessage());
		}
	}

	private void scaleGreeksColumns(Table creditPFEVaRInfo) {

		int rowCount = creditPFEVaRInfo.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			double sigma = creditPFEVaRInfo.getDouble("gpt_sigma", row);
			double timeToCallDate = creditPFEVaRInfo.getDouble("time_to_call_date", row);
			creditPFEVaRInfo.setDouble("gpt_sigma", row, sigma * Math.sqrt(timeToCallDate));

			double delta = creditPFEVaRInfo.getDouble("delta", row);
			creditPFEVaRInfo.setDouble("delta", row, delta * Math.sqrt(timeToCallDate));
			double gamma = creditPFEVaRInfo.getDouble("gamma", row);
			creditPFEVaRInfo.setDouble("gamma", row, gamma * Math.sqrt(timeToCallDate));
		}
	}

	private void filteroutNonVaRIncedies(Table tranGptDeltaByLeg) {

		Table varIndices = null;
		try {
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
		return simUtil.getGenResults(prerequisites, sf.getResultType(EnumResultType.TranGptDeltaByLeg));
	}

	private ConstTable getJMCreditPFEPartyAgreement(RevalResults prerequisites) throws OpenRiskException {
		return simUtil.getGenResults(prerequisites, sf.getResultType(SIM_PARTY_AFREEMENT_UDSR_NAME));
	}

	protected Table createOutputTable() {
		Table data = tf.createTable("JM Credit PFE VaR And PA Info");

		// Transaction Data
		data.addColumn("deal_num", EnumColType.Int);
		data.addColumn("tran_num", EnumColType.Int);
		data.addColumn("ins_num", EnumColType.Int);
		data.addColumn("ins_type", EnumColType.Int);
		data.addColumn("external_lentity", EnumColType.Int);
		data.addColumn("param_seq_num", EnumColType.Int);
		data.addColumn("param_currency", EnumColType.Int);
		data.addColumn("pay_receive", EnumColType.Int);
		data.addColumn("tran_type", EnumColType.Int);
		// Base MTM Data
		data.addColumn("base_mtm", EnumColType.Double);
		data.addColumn("mtm_exposure", EnumColType.Double);
		// VaR Grid Point Raw Data
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
		// Party Agreement Data
		data.addColumn("party_agreement_id", EnumColType.Int);
		data.addColumn("netting_flag", EnumColType.String);
		data.addColumn("collateral_agreement", EnumColType.String);
		data.addColumn("collateral_valuation_date_seq", EnumColType.String);
		data.addColumn("next_collateral_call_date", EnumColType.Date);
		data.addColumn("time_to_call_date", EnumColType.Double);
		data.addColumn("haircut", EnumColType.Double);
		return data;
	}

	private void initialize(Session session) {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			this.iof = session.getIOFactory();
			this.sf = session.getSimulationFactory();
			this.cf = session.getCalendarFactory();
			this.tf = session.getTableFactory();

			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			SIM_RES_GROUP = constRepo.getStringValue(CR_VAR_SIM_RES_GROUP);
			SIM_ATTR_COL_CALL_FREQ_NAME = constRepo.getStringValue(CR_VAR_SIM_ATTR_COL_CALL_FREQ);
			SIM_ATTR_PFE_CALC_HORIZON_NAME = constRepo.getStringValue(CR_VAR_SIM_ATTR_PFE_CALC_HORIZON);
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to initialize : " + e.getMessage());
		} catch (OException e) {
			throw new OpenRiskException("Failed to load const repository values : " + e.getMessage());
		}
		Logging.info("********************* Start of new run ***************************");
	}
}
