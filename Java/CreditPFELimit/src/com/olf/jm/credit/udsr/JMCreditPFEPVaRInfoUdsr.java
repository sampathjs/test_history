package com.olf.jm.credit.udsr;

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
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.market.EnumIdxPurpose;
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
	
	IOFactory iof;
	TableFactory tf;
	SimulationFactory sf;
	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;
	
	private SimUtil simUtil = new SimUtil();
	
	private static final String SIM_PARTY_AFREEMENT_UDSR_NAME = "JM Credit PFE Party Agreement"; 
	private static final String CONST_REPO_CONTEXT = "JM Credit PFE";
	private static final String CONST_REPO_SUBCONTEXT = "VaR Info UDSR";

	@Override
	public void calculate(Session session, Scenario scenario, RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) {

		try {
			initialize(session);
			Logging.info("JM Credit PFE VaR Info UDSR - Start");

			Table tranList = getTranList(transactions);
			ConstTable varGptRawData = getVaRGridPointRawData(prerequisites);
			ConstTable varCorrMatrix = getVaRCorrelationMatrix(prerequisites);
			ConstTable creditPFEPA = getJMCreditPFEPartyAgreement(prerequisites);
			Table tranGptDeltaByLeg = getTranGptDeltaByLeg(prerequisites).asTable();
			filteroutNonVaRIncedies(tranGptDeltaByLeg);

			Table creditPFEVaRInfo = createOutputTable();
			creditPFEVaRInfo.select(tranList, "deal_num, tran_num, ins_num, toolset, ins_type, external_lentity, buy_sell", "[IN.deal_num] > 0");
			creditPFEVaRInfo.select(creditPFEPA, "param_seq_num, party_agreement_id, time_to_call_date, pay_receive, netting_flag",
					"[IN.deal_num] == [OUT.deal_num] AND [IN.tran_num] == [OUT.tran_num] AND [IN.ins_num] == [OUT.ins_num]");
			creditPFEVaRInfo.select(tranGptDeltaByLeg, "index->index_id, gpt_id, delta, gamma",
					"[IN.deal_num] == [OUT.deal_num] AND [IN.deal_leg] == [OUT.param_seq_num]");
			creditPFEVaRInfo.select(varGptRawData, "gpt_label, gpt_name, gpt_date, gpt_time, gpt_sigma", "[IN.index_id] == [OUT.index_id]"
					+ " AND [IN.vol_id] == [OUT.vol_id] AND [IN.gpt_id] == [OUT.gpt_id]");

			scaleGreeksColumns(creditPFEVaRInfo);

			filterOutPayNonNettingRows(creditPFEVaRInfo);

			revalResult.setTable(creditPFEVaRInfo);

		} catch (OpenRiskException e) {
			logErrorWithException("Failed to run sim result: " + e.getMessage());
		}

		Logging.info("JM Credit PFE VaR Info UDSR - Complete");
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
		
		formatter.setColumnFormatter("ins_type", formatter.createColumnFormatterAsRef(EnumReferenceTable.Instruments));
		formatter.setColumnFormatter("external_lentity", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));
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

	private void filterOutPayNonNettingRows(Table creditPFEVaRInfo) {

		try {
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
			logErrorWithException("Failed to filter out non netting rows: " + e.getMessage());
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
			logErrorWithException("Failed to remove non VaR indices: " + e.getMessage());
		}
	}

	private ConstTable getVaRGridPointRawData(RevalResults prerequisites) throws OpenRiskException {
		try {
			return simUtil.getGenResults(prerequisites, sf.getResultType(EnumResultType.VaRGridPointRawData));
		} catch (Exception e) {
			logErrorWithException("Check if VaR Definition is added to reval: " + e.getMessage());
		}
		return null;
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

	public Table getTranList(Transactions transactions) {

		EnumTransactionFieldId[] fields = { 
				EnumTransactionFieldId.ExternalLegalEntity,
				EnumTransactionFieldId.BuySell,
				EnumTransactionFieldId.InstrumentId,
				EnumTransactionFieldId.Toolset,
				EnumTransactionFieldId.InstrumentType,
				EnumTransactionFieldId.TransactionType };
		return simUtil.getTranList(transactions, fields);
	}
	
	protected Table createOutputTable() {
		Table data = tf.createTable("JM Credit PFE VaR Info");

		data.addColumn("deal_num", EnumColType.Int);
		data.addColumn("tran_num", EnumColType.Int);
		data.addColumn("ins_num", EnumColType.Int);
		data.addColumn("toolset", EnumColType.Int);
		data.addColumn("ins_type", EnumColType.Int);
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

	private void initialize(Session session) {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			this.sf = session.getSimulationFactory();
			this.iof = session.getIOFactory();
			tf = session.getTableFactory();

			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (OpenRiskException e) {
			throw new OpenRiskException("Failed to initialize : " + e.getMessage());
		} catch (OException e) {
			throw new OpenRiskException("Failed to load const repository values : " + e.getMessage());
		}
		Logging.info("********************* Start of new run ***************************");
	}

	private void logErrorWithException(String msg) {
		Logging.error(msg);
		throw new OpenRiskException(msg);
	}

}
