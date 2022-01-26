package com.olf.jm.credit.udsr;

/**********************************************************************************************************************
 * File Name:                  JMCreditPFEPartyAgreementUdsr.java
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
 * 20-Aug-2021  GanapP02   EPI-xxxx     Initial version - UDSR to get Party Agreement, Base MTM data required for  
 *                                      Credit PFE calculations
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
import com.olf.openrisk.simulation.Configuration;
import com.olf.openrisk.simulation.EnumConfiguration;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumReceivePay;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class JMCreditPFEPartyAgreementUdsr extends AbstractSimulationResult2 {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;
	
	private SimUtil simUtil = new SimUtil();
	
	private static IOFactory iof;
	private static CalendarFactory cf;
	private static TableFactory tf;

	private static final String CONST_REPO_CONTEXT = "JM Credit PFE";
	private static final String CONST_REPO_SUBCONTEXT = "Party Agreement UDSR";
	
	private static final String CR_VAR_SIM_ATTR_COL_CALL_FREQ = "CollateralCallFreq";
	private static final String CR_VAR_SIM_ATTR_PFE_CALC_HORIZON = "PFECalcHorizon";
	private static final String CR_VAR_LOG_DEBUG_MESSAGES = "logDebugMessages";
	
	private static String SIM_ATTR_COL_CALL_FREQ_NAME = "";
	private static String SIM_ATTR_PFE_CALC_HORIZON_NAME = "";
	private static String SIM_ATTR_COL_CALL_FREQ_VALUE = "";
	private static String SIM_ATTR_PFE_CALC_HORIZON_VALUE = "";
	
	private static String CLASSNAME = "";
	private static boolean LOG_DEBUG_MSG = false;
	private static Date scenarioDate;
	private static Date currentDate;
	private static Currency scenarioCurrency;
	
	JMCreditPFEPartyAgreementUdsr () {
		CLASSNAME = this.getClass().getSimpleName();
	}

	@Override
	public void calculate(Session session, Scenario scenario, RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".calculate() : ";
		try {
			initialize(session, scenario);
			Logging.info(logPrefix + "JM Credit PFE Party Agreement UDSR - Start for scenario id " + scenario.getId());

			Table tranList = getTranList(transactions);
			ConstTable baseMtM = getBaseMtM(prerequisites);
			Table creditPFEPAData = createOutputTable();
			
			updateTranAndBaseMTMData(creditPFEPAData, tranList, baseMtM);
			
			addPMCurrencyTable(creditPFEPAData);

			populateSimResultAttributes(revalResult);

			addPayRec(transactions, creditPFEPAData);

			addPartyAgreementFields(creditPFEPAData);

			addCollateralCallDates(creditPFEPAData);

			addhaircut(creditPFEPAData);

			addMtmExposure(creditPFEPAData);
			
			revalResult.setTable(creditPFEPAData);

		} catch (OpenRiskException e) {
			String message = "Failed to run sim result: " + e.getMessage();
			Logging.error(message);
			throw new OpenRiskException(message);
		} finally {
			Logging.info(logPrefix + "JM Credit PFE Party Agreement UDSR - completed in " + (System.currentTimeMillis() - currentTime) + " ms");
			Logging.close();	
		}
	}

	private void updateTranAndBaseMTMData(Table creditPFEPAData, Table tranList, ConstTable baseMtM) {
		
		creditPFEPAData.select(tranList, "deal_num, tran_num, ins_num, ins_type, ins_sub_type, start_date, end_date, external_lentity"
				+ ", tran_type, party_agreement_id", "[IN.deal_num] >= 0");

		Table baseMtMtemp = creditPFEPAData.cloneData();
		baseMtMtemp.select(baseMtM, "deal_leg->param_seq_num, currency_id->param_currency, base_mtm",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.tran_num] == [OUT.tran_num]");
		
		Table baseMtMNotForNetting = tf.createTable();
		baseMtMNotForNetting.select(baseMtMtemp, "*", "[IN.party_agreement_id] != 0 AND [IN.ins_type] !=" + EnumInsType.MetalSwap.getValue()
				+ " AND [IN.ins_type] !=" + EnumInsType.MetalBasisSwap.getValue());
		creditPFEPAData.select(baseMtMNotForNetting, "param_seq_num, param_currency, base_mtm",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.tran_num] == [OUT.tran_num]");

		Table baseMtMForNetting = tf.createTable("baseMtMForNetting");
		baseMtMForNetting.select(baseMtMtemp, "*", "[IN.party_agreement_id] == 0");
		baseMtMForNetting.select(baseMtMtemp, "*", "[IN.party_agreement_id] != 0 AND [IN.ins_type] ==" + EnumInsType.MetalSwap.getValue());
		baseMtMForNetting.select(baseMtMtemp, "*", "[IN.party_agreement_id] != 0 AND [IN.ins_type] ==" + EnumInsType.MetalBasisSwap.getValue());
		
		Table baseMTMNettedOnZeroLeg = tf.createTable("baseMTMNettedOnZeroLeg");
		baseMTMNettedOnZeroLeg.selectDistinct(baseMtMForNetting, "deal_num", "[IN.deal_num] >= 0");
		baseMTMNettedOnZeroLeg.addColumn("param_seq_num", EnumColType.Int);
		baseMTMNettedOnZeroLeg.select(baseMtMForNetting, "base_mtm", "[IN.deal_num] == [OUT.deal_num]", "SUM(base_mtm)");
		
		creditPFEPAData.select(baseMtMForNetting, "param_seq_num, param_currency",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.tran_num] == [OUT.tran_num]");
		creditPFEPAData.select(baseMTMNettedOnZeroLeg, "base_mtm",
				"[IN.deal_num] == [OUT.deal_num] AND [IN.param_seq_num] == [OUT.param_seq_num]");
	}

	@Override
	public void format(final Session session, final RevalResult revalResult) {

		Table result = revalResult.getTable();
		TableFormatter formatter = result.getFormatter();

		formatter.setColumnTitle("deal_num", "Deal Number");
		formatter.setColumnTitle("tran_num", "Tran Number");
		formatter.setColumnTitle("ins_num", "ins Number");
		formatter.setColumnTitle("ins_type", "Instrument");
		formatter.setColumnTitle("ins_sub_type", "Instrument\nSub Type");
		formatter.setColumnTitle("start_date", "Start Date");
		formatter.setColumnTitle("end_date", "Maturity Date");
		formatter.setColumnTitle("external_lentity", "External\nLegal Entity");
		formatter.setColumnTitle("param_seq_num", "Deal Leg");
		formatter.setColumnTitle("param_currency", "Param\nCurrency");
		formatter.setColumnTitle("pay_receive", "Pay/Receive");
		formatter.setColumnTitle("base_mtm", "Base MtM");
		formatter.setColumnTitle("party_agreement_id", "Party Agreement");
		formatter.setColumnTitle("netting_flag", "Netting");
		formatter.setColumnTitle("collateral_agreement", "Collateral\nAgreement");
		formatter.setColumnTitle("collateral_valuation_date_seq", "Collateral\nValuation\nDate Sequence");
		formatter.setColumnTitle("next_collateral_call_date", "Next\nCollateral\nCall Date");
		formatter.setColumnTitle("time_to_call_date", "Time To Call Date");
		formatter.setColumnTitle("tran_type", "Tran Type");
		formatter.setColumnTitle("haircut", "Haircut");
		formatter.setColumnTitle("mtm_exposure", "MtM Exposure");
		
		formatter.setColumnFormatter("ins_type", formatter.createColumnFormatterAsRef(EnumReferenceTable.Instruments));
		formatter.setColumnFormatter("ins_sub_type", formatter.createColumnFormatterAsRef(EnumReferenceTable.InsSubType));
		formatter.setColumnFormatter("external_lentity", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));
		formatter.setColumnFormatter("param_currency", formatter.createColumnFormatterAsRef(EnumReferenceTable.Currency));
		formatter.setColumnFormatter("pay_receive", formatter.createColumnFormatterAsRef(EnumReferenceTable.RecPay));
		formatter.setColumnFormatter("party_agreement_id", formatter.createColumnFormatterAsRef(EnumReferenceTable.PartyAgreement));
		formatter.setColumnFormatter("tran_type", formatter.createColumnFormatterAsRef(EnumReferenceTable.TransType));

		formatter.setColumnFormatter("base_mtm", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		formatter.setColumnFormatter("time_to_call_date", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		formatter.setColumnFormatter("haircut", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		formatter.setColumnFormatter("mtm_exposure", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 10));
		
		formatter.setColumnFormatter("start_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("end_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));

		formatter.getColumnFormatter("metal_leg").setHidden(true);
	}

	private ConstTable getBaseMtM(RevalResults prerequisites) throws OpenRiskException {

		logDebugMsg(CLASSNAME + ".getBaseMtM() : get base MTM from sim results");
		// BaseMTM results doesn't include tran numbers hence get from transaction results table
		Table transactions = prerequisites.getResultsTable().getTable(0, 0).cloneData();
		transactions.convertColumns("Int[40]");
		transactions.setColumnName(transactions.getColumnId("" + EnumResultType.TranListing.getValue()), "tran_num");
		transactions.setColumnName(transactions.getColumnId("" + EnumResultType.BaseMtm.getValue()), "base_mtm");

		// Work around for FX swap deals in quick credit check mode since Tran listing result is 0 for all rows
		if (transactions.getRowCount() == 4) {
			int[] distinctDealNum = transactions.getDistinctValues("deal_num").getColumnValuesAsInt("deal_num");
			if (distinctDealNum.length == 1 && distinctDealNum[0] == 0) {
				for (TableRow row : transactions.getRows()) {
					int value = (int) Math.ceil((double) row.getInt("sort") / 2);
					row.getCell("tran_num").setInt(value);
				}
			}
		}

		ConstTable baseMtM = transactions
				.createConstView("deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, currency_id, tran_num, base_mtm");
		return baseMtM;
	}

	private Table getTranList(Transactions transactions) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".getTranList() : ";
		logDebugMsg(logPrefix + "method started");
		EnumTransactionFieldId[] fields = { 
				EnumTransactionFieldId.InternalBusinessUnit,
				EnumTransactionFieldId.ExternalBusinessUnit,
				EnumTransactionFieldId.InstrumentType,
				EnumTransactionFieldId.InstrumentSubType,
				EnumTransactionFieldId.ExternalLegalEntity,
				EnumTransactionFieldId.BuySell,
				EnumTransactionFieldId.InstrumentId,
				EnumTransactionFieldId.InstrumentType,
				EnumTransactionFieldId.TransactionType,
				EnumTransactionFieldId.PartyAgreement,
				EnumTransactionFieldId.StartDate,
				EnumTransactionFieldId.MaturityDate
		};
		Table tranList = simUtil.getTranList(transactions, fields);
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");

		updatePartyAgreementForFxFarLeg(tranList);
		
		// Work around for FX swap deals in quick credit check mode
		if (tranList.getRowCount() == 2 && tranList.getInt("tran_num", 0) == 0) {
			if(tranList.getInt("ins_type", 0) == EnumInsType.FxInstrument.getValue()) {
				tranList.setInt("tran_num", 0, tranList.getInt("ins_sub_type", 0) == EnumInsSub.FxNearLeg.getValue() ? 1 : 2);
				tranList.setInt("tran_num", 1, tranList.getInt("ins_sub_type", 1) == EnumInsSub.FxNearLeg.getValue() ? 1 : 2);
			}
		}
		
		return tranList;
	}

	private void updatePartyAgreementForFxFarLeg(Table tranList) {

		Table partyAgreementNearLeg = tf.createTable();
		Table fxFarLegDeals = tf.createTable();
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".updatePartyAgreementForFxFarLeg() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			fxFarLegDeals.selectDistinct(tranList, "*", "[IN.ins_sub_type] == " + EnumInsSub.FxFarLeg.getValue());
			QueryResult qr = iof.createQueryResult();
			qr.add(fxFarLegDeals.getColumnValuesAsInt("tran_num"));
			int queryId = qr.getId();
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT far.tran_num far_tran_num, near.tran_num near_tran_num, atav.party_agreement_id");
			sql.append("\n FROM query_result qr");
			sql.append("\n JOIN ab_tran far ON far.tran_num = qr.query_result AND qr.unique_id = ").append(queryId);
			sql.append("    AND far.ins_sub_type = ").append(EnumInsSub.FxFarLeg.getValue());
			sql.append("\n JOIN ab_tran near ON near.tran_group = far.tran_group");
			sql.append("    AND near.ins_sub_type = ").append(EnumInsSub.FxNearLeg.getValue());
			sql.append("\n JOIN ab_tran_agreement_view atav ON atav.tran_num = near.tran_num");
			sql.append("\n AND atav.party_agreement_id !=0");
			
			partyAgreementNearLeg = iof.runSQL(sql.toString());
			if (partyAgreementNearLeg.getRowCount() <= 0 && tranList.getRowCount() > 1) {
				Logging.error(logPrefix + "Failed to fetch Party Agreement for FX near leg deals");
			}
			tranList.select(partyAgreementNearLeg, "party_agreement_id", "[IN.far_tran_num] == [OUT.tran_num]");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to get simulation result attribute: " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private void addPMCurrencyTable(Table creditPFEPAData) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".addPMCurrencyTable() : ";
		logDebugMsg(logPrefix + "method started");
		Table pmCurrency = iof.runSQL("SELECT id_number AS currency_id, 1 AS metal_leg FROM currency WHERE precious_metal = 1");
		creditPFEPAData.select(pmCurrency, "metal_leg", "[IN.currency_id] == [OUT.param_currency]");
		logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
	}

	private void populateSimResultAttributes(RevalResult revalResult) {

		Table pfolioResult = null;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".populateSimResultAttributes() : ";
		try {
			logDebugMsg(logPrefix + "method started");
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
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
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

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".addPayRec() : ";
		logDebugMsg(logPrefix + "method started");
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
			sql.append("\n          WHEN ab.toolset = ").append(EnumToolset.Cash.getValue());
			sql.append("\n               AND ab.position > 0 THEN 0");
			sql.append("\n          WHEN ab.toolset = ").append(EnumToolset.Cash.getValue());
			sql.append("\n               AND ab.position <=0 THEN 1");
			sql.append("\n          WHEN ifp.fee_def_id > 2000 THEN ifp.pay_rec"); // For Fees
			sql.append("\n          ELSE ip.pay_rec");
			sql.append("\n          END AS pay_rec");
			sql.append("\n  FROM ab_tran ab");
			sql.append("\n  JOIN ins_parameter ip ON ab.ins_num = ip.ins_num");
			sql.append("\n  JOIN instruments i ON ab.ins_type = i.id_number ");
			sql.append("\n  LEFT JOIN ins_fee_param ifp ON ifp.ins_num = ab.ins_num AND ip.param_seq_num>2");
			sql.append("\n          AND ifp.param_seq_num = ip.param_seq_num AND ifp.fee_seq_num = 0");

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
			logDebugMsg(logPrefix + "Pay Rec added to the table.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to load pay rec from database table : " + e.getMessage());
		} finally {
			payRec.dispose();
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private void addPartyAgreementFields(Table creditPFEPAData) {

		Table partyAgreementList = null;
		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".addPartyAgreementFields() : ";
		try {
			logDebugMsg(logPrefix + "method started");
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
			sql.append("\n  LEFT JOIN party_agreement_collateral pac ON pa.party_agreement_id = pac.party_agreement_id");
			sql.append("\n        AND pac.int_ext = 0");
			sql.append("\n WHERE pa.doc_status = 1");
			partyAgreementList = iof.runSQL(sql.toString());

			int rowCount = creditPFEPAData.getRowCount();
			for (int row = 0; row < rowCount; row++) {
				int partyAgreement = creditPFEPAData.getValueAsInt("party_agreement_id", row);
				String netting = "No";
				String valDateSeq = "";
				if (partyAgreement > 0) {
					int partyAgreementRow = partyAgreementList.find(partyAgreementList.getColumnId("party_agreement_id"), partyAgreement, 0);
					if (partyAgreementRow > -1) {
						netting = partyAgreementList.getInt("netting_flag", partyAgreementRow) == 0 ? "No" : "Yes";
						valDateSeq = partyAgreementList.getString("valuation_date_sequence", partyAgreementRow);
					}
				}
				creditPFEPAData.setString("netting_flag", row, netting);
				String collAgreement = partyAgreement <= 0 ? "No" : "Yes";
				creditPFEPAData.setString("collateral_agreement", row, collAgreement);
				valDateSeq = "Yes".equalsIgnoreCase(collAgreement)
						? ("".equalsIgnoreCase(valDateSeq) ? SIM_ATTR_COL_CALL_FREQ_VALUE : valDateSeq) : SIM_ATTR_PFE_CALC_HORIZON_VALUE;
				creditPFEPAData.setString("collateral_valuation_date_seq", row, valDateSeq);
			}
			logDebugMsg(logPrefix + "Party agreement info updated.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to load party agreement netting flag : " + e.getMessage());
		} finally {
			partyAgreementList.dispose();
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private void addCollateralCallDates(Table creditPFEPAData) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".addCollateralCallDates() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			HolidaySchedules hs = cf.createHolidaySchedules();
			hs.addSchedule(scenarioCurrency.getHolidaySchedule());

			int rowCount = creditPFEPAData.getRowCount();
			for (int row = 0; row < rowCount; row++) {
				SymbolicDate valDateSeq = cf.createSymbolicDate(creditPFEPAData.getString("collateral_valuation_date_seq", row));
				valDateSeq.setHolidaySchedules(hs);
				Date nextCallDate = valDateSeq.evaluate(currentDate);
				creditPFEPAData.setDate("next_collateral_call_date", row, nextCallDate);

				double timeToCallDate = (cf.getJulianDate(nextCallDate) - cf.getJulianDate(currentDate)) / 365;
				creditPFEPAData.setDouble("time_to_call_date", row, timeToCallDate);
			}
			logDebugMsg(logPrefix + "Collateral Call dates updated.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add Collateral Call dates : " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private void addhaircut(Table creditPFEPAData) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".addhaircut() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			creditPFEPAData.setColumnValues(creditPFEPAData.getColumnId("haircut"), 1.0);
			logDebugMsg(logPrefix + "Haircut values added.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add haircut values: " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	private void addMtmExposure(Table creditPFEPAData) {

		long currentTime = System.currentTimeMillis();
		String logPrefix = CLASSNAME + ".addMtmExposure() : ";
		try {
			logDebugMsg(logPrefix + "method started");
			int rowCount = creditPFEPAData.getRowCount();
			for (int row = 0; row < rowCount; row++) {
				double haircut = creditPFEPAData.getDouble("haircut", row);
				String netting = creditPFEPAData.getString("netting_flag", row);
				double baseMtm = creditPFEPAData.getDouble("base_mtm", row);
				int payRec = creditPFEPAData.getInt("pay_receive", row);
				int insType = creditPFEPAData.getInt("ins_type", row);
				Date nextCallDate = creditPFEPAData.getDate("next_collateral_call_date", row);
				Date maturityDate = creditPFEPAData.getInt("end_date", row) == 0 ? cf.getDate(creditPFEPAData.getInt("start_date", row))
						: cf.getDate(creditPFEPAData.getInt("end_date", row));
				double mtmExposure = 0;
				if (scenarioDate.after(nextCallDate) && scenarioDate.after(maturityDate)) {
					mtmExposure = 0;
				} else if (insType == EnumInsType.MetalSwap.getValue() || insType == EnumInsType.MetalBasisSwap.getValue()) {
					mtmExposure = Math.max(0.0, haircut * baseMtm);
				} else if ("Yes".equalsIgnoreCase(netting)) {
					mtmExposure = haircut * baseMtm;
				} else if (payRec == EnumReceivePay.Pay.getValue()) {
					mtmExposure = 0.0;
				} else {
					mtmExposure = Math.max(0.0, haircut * baseMtm);
				}
				creditPFEPAData.setDouble("mtm_exposure", row, mtmExposure);
			}
			logDebugMsg(logPrefix + "MTM Exposure values calculated and added.");
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to add MTM Exposure values : " + e.getMessage());
		} finally {
			logDebugMsg(logPrefix + "method completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	protected Table createOutputTable() {
		Table data = tf.createTable("JM Credit PFE Party Agreement Data");

		data.addColumn("deal_num", EnumColType.Int);
		data.addColumn("tran_num", EnumColType.Int);
		data.addColumn("ins_num", EnumColType.Int);
		data.addColumn("ins_type", EnumColType.Int);
		data.addColumn("ins_sub_type", EnumColType.Int);
		data.addColumn("start_date", EnumColType.Int);
		data.addColumn("end_date", EnumColType.Int);
		data.addColumn("external_lentity", EnumColType.Int);
		data.addColumn("param_seq_num", EnumColType.Int);
		data.addColumn("param_currency", EnumColType.Int);
		data.addColumn("pay_receive", EnumColType.Int);
		data.addColumn("base_mtm", EnumColType.Double);
		data.addColumn("party_agreement_id", EnumColType.Int);
		data.addColumn("netting_flag", EnumColType.String);
		data.addColumn("collateral_agreement", EnumColType.String);
		data.addColumn("collateral_valuation_date_seq", EnumColType.String);
		data.addColumn("next_collateral_call_date", EnumColType.Date);
		data.addColumn("time_to_call_date", EnumColType.Double);
		data.addColumn("tran_type", EnumColType.Int);
		data.addColumn("haircut", EnumColType.Double);
		data.addColumn("mtm_exposure", EnumColType.Double);

		return data;
	}

	private void initialize(Session session, Scenario scenario) {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			iof = session.getIOFactory();
			cf = session.getCalendarFactory();
			tf = session.getTableFactory();

			SIM_ATTR_COL_CALL_FREQ_NAME = constRepo.getStringValue(CR_VAR_SIM_ATTR_COL_CALL_FREQ);
			SIM_ATTR_PFE_CALC_HORIZON_NAME = constRepo.getStringValue(CR_VAR_SIM_ATTR_PFE_CALC_HORIZON);
			LOG_DEBUG_MSG = constRepo.getIntValue(CR_VAR_LOG_DEBUG_MESSAGES) == 1 ? true : false;

			Configuration scenarioDateCfg = scenario.getConfigurations().find(EnumConfiguration.Date, "Scenario Date");
			scenarioDate = scenarioDateCfg == null ? cf.createSymbolicDate("0cd").evaluate()
					: scenarioDateCfg.getFields().getField("Current Date").getValueAsDate();
			currentDate = session.getBusinessDate();
			scenarioCurrency = scenario.getCurrency();
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
