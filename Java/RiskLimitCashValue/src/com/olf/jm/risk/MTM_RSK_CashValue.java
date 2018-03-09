package com.olf.jm.risk;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.limits.EnumRiskCriteria;
import com.olf.openrisk.limits.Field;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultTypes;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({EnumScriptCategory.CreditRisk})
public class MTM_RSK_CashValue extends AbstractExposureCalculator2<Table, String> {

	@Override
	public com.olf.embedded.limits.ExposureCalculator2.DealExposure[] calculateDealExposures(Session session, ExposureDefinition definition, Transaction transaction, Table dealCache) {
		double rawExposure = 0.0;
		
		Table cflowByDay = dealCache.getTable("cflow_by_day",0);
		Table pnlDetails = dealCache.getTable("pnl_details",0);
		EnumToolset toolset = transaction.getToolset();
		
		if (cflowByDay == null && toolset != EnumToolset.ComFut) {
    		DealExposure dealExposure = definition.createDealExposure(0.0, transaction);
    		return new DealExposure[] {dealExposure};
		}
		
		if (pnlDetails == null && toolset == EnumToolset.ComFut) {
    		DealExposure dealExposure = definition.createDealExposure(0.0, transaction);
    		return new DealExposure[] {dealExposure};
		}
		
		Table tblWork = null;
		if (toolset != EnumToolset.ComFut) {
			tblWork = cflowByDay.createConstView("*","[deal_num] == " + transaction.getDealTrackingId()).asTable();
		} else {
			tblWork = pnlDetails.createConstView("*","[cflow_type] == " + EnumCashflowType.Interest.getValue()).asTable();
		}

		Field[] fields = definition.getCriteriaFields(transaction);
		int index = 0;
		int originCriteria = 0; 
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getCriteriaType().getId() == EnumRiskCriteria.Ccy.getValue()) {
				index = i;
				originCriteria = fields[i].getValueAsInt();
			}
		}
		
		if (toolset == EnumToolset.Fx) {
//			Table temp = tblWork.calcByGroup("currency", "base_cflow");
//			temp.setColumnName(1, "base_cflow");
			tblWork.calcColumn("base_cflow", "Abs(base_cflow)");
			int count = tblWork.getRowCount();
			DealExposure[] dealExposures = new DealExposure[count];
			for (int row = 0; row < count; row++) {
				int currency = tblWork.getInt("currency", row);
				rawExposure = tblWork.getDouble("base_cflow", row);
				if (currency == originCriteria){
					dealExposures[row] = definition.createDealExposure(rawExposure, transaction);
				} else {
					fields[index].setValue(currency);
					dealExposures[row] = definition.createDealExposure(rawExposure, transaction, fields);
				}
			}	
			
//			temp.dispose();
			return dealExposures;
		} else if (toolset == EnumToolset.Cash) {
			tblWork.calcColumn("base_cflow", "Abs(base_cflow)");
			rawExposure = tblWork.getDouble("base_cflow", 0);
			
			fields[index].setValue(tblWork.getInt("currency", 0));
    		DealExposure dealExposure = definition.createDealExposure(rawExposure, transaction, fields);
    		return new DealExposure[] {dealExposure};
		} else if (toolset == EnumToolset.Loandep) {
			// Get Initial principle
			Table temp = tblWork.createConstView("currency, base_cflow", "[cflow_type] == " + EnumCashflowType.Principal.getValue()).asTable();
			temp.calcColumn("base_cflow", "Abs(base_cflow)");
			fields[index].setValue(temp.getInt("currency", 0));
			rawExposure = temp.getDouble("base_cflow", 0);
			DealExposure dealExposure = definition.createDealExposure(rawExposure, transaction, fields);
			temp.dispose();
			return new DealExposure[]{dealExposure};
		} else if (toolset == EnumToolset.ComSwap) {
			tblWork.calcColumn("base_cflow", "Abs(base_cflow)");
			int row = findLargestCFlowRow(tblWork);
			fields[index].setValue(tblWork.getInt("currency", row));
			rawExposure = tblWork.getDouble("base_cflow", row);
			DealExposure dealExposure = definition.createDealExposure(rawExposure, transaction, fields);
			return new DealExposure[]{dealExposure};
		} else if (toolset == EnumToolset.ComFut) {
			tblWork.calcColumn("base_total_value", "Abs(base_total_value)");
			rawExposure = tblWork.getDouble("base_total_value", 0);
			String contractCode = transaction.getLeg(0).getValueAsString(EnumLegFieldId.ContractCode);
			if (contractCode.equalsIgnoreCase("PL")) {
				fields[index].setValue("XPT");
			} else if (contractCode.equalsIgnoreCase("PA")) {
				fields[index].setValue("XPD");
			} else {
				rawExposure = 0.0;
			}
    		DealExposure dealExposure = definition.createDealExposure(rawExposure, transaction, fields);
    		return new DealExposure[] {dealExposure};
		}
		
		tblWork.dispose();
		DealExposure dealExposure = definition.createDealExposure(0.0, transaction);
		return new DealExposure[] {dealExposure};

	}

	private int findLargestCFlowRow(Table clientData) {
		int row = 0;
		double baseCflow = 0.0;
		for (int tableRow = 0; tableRow < clientData.getRowCount(); tableRow++){
			double temp = clientData.getDouble("base_cflow", tableRow);
			if (temp > baseCflow) {
				row = tableRow;
				baseCflow = temp;
			}
		}
		return row;
	}

	@Override
	public Table createDealCache(Session session, ExposureDefinition definition, Transactions transactions) {
		// If it is a quick credit check for fx swap, then tran_num = 0, first one will be near leg
//		if (transactions.getCount() == 2 && transactions.getTransactionIds()[0] == 0){
//			transactions.get(0).assignTemporaryIds();
//			transactions.get(1).assignTemporaryIds();
//		}
		
		PluginLog.info("Start Running Sim Result");
		SimulationFactory sf = session.getSimulationFactory();
		
		EnumToolset toolset = transactions.get(0).getToolset();
		
		ResultTypes resultTypes = sf.createResultTypes();
		resultTypes.add(EnumResultType.CashflowByDay);
		resultTypes.add(EnumResultType.PnlDetail);
		
		Table returnt = session.getTableFactory().createTable("sim_results");
		returnt.addColumn("cflow_by_day", EnumColType.Table);
		returnt.addColumn("pnl_details", EnumColType.Table);
		try {
			RevalResults scenarioResults = null;
			if (toolset == EnumToolset.ComFut) {
				Simulation sim = sf.createSimulation("Cash Value");
				Scenario s = sf.createScenario("Base");
				s.useMarketPriceIndexes(false);
				s.setResultTypes(resultTypes);
				sim.addScenario(s);
				SimResults results = sim.run(transactions);
				scenarioResults = results.getScenarioResults("Base");
			} else {
		        RevalSession revalSession = sf.createRevalSession(transactions);
				scenarioResults = revalSession.calcResults(resultTypes);
			}
			returnt.addRow();
			if (scenarioResults.contains(EnumResultType.CashflowByDay)) {
				returnt.setTable("cflow_by_day", 0, scenarioResults.getResultTable(EnumResultType.CashflowByDay).asTable());
			}
			
			if (scenarioResults.contains(EnumResultType.PnlDetail)) {
				returnt.setTable("pnl_details", 0, scenarioResults.getResultTable(EnumResultType.PnlDetail).asTable());
			}
			
			scenarioResults.dispose();
		} catch (Exception e) {
			PluginLog.info("Failed to run Sim Result");
			PluginLog.info(e.getMessage());
		}

		PluginLog.info("Finished Running Sim Result");
		return returnt;
	}

	@Override
	public void disposeDealCache(Session session, Table dealCache) {
		dealCache.dispose();
	}

}
