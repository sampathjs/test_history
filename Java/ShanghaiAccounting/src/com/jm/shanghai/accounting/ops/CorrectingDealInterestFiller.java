package com.jm.shanghai.accounting.ops;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PreProcessingInfo;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.io.EnumQueryType;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.simulation.ConstGeneralResult;
import com.olf.openrisk.simulation.EnumGeneralResultType;
import com.olf.openrisk.simulation.EnumResultClass;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CorrectingDealInterestFiller extends AbstractTradeProcessListener {
    public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
            final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
    	for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
    		Transaction tran = ppi.getTransaction();
    		Field interfaceTranTypeField = tran.getField("Interface_Trade_Type");
    		if (interfaceTranTypeField == null) {
    			continue;
    		}
    		if (!interfaceTranTypeField.getValueAsString().equalsIgnoreCase("Corrections")) {
    			continue;
    		}
    		Field correctingDealField =  tran.getField("Correcting Deal");
    		if (correctingDealField == null) {
    			continue;
    		}
    		if (correctingDealField.getValueAsString() == null || correctingDealField.getValueAsString().isEmpty()) {
    			continue;
    		}
			Field correctingDealInterestField = tran.getField("Correcting Deal Interest");
			if (correctingDealInterestField == null) {
				continue;
			}
    		int correctingDealNumber = correctingDealField.getValueAsInt();
    		Transaction correctingDeal = context.getTradingFactory().retrieveTransaction(correctingDealNumber);
    		SimulationFactory sf = context.getSimulationFactory();
    		StaticDataFactory sdf = context.getStaticDataFactory();
			try (QueryResult qr = context.getIOFactory().createQueryResult(EnumQueryType.Transaction);
					ResultType rt = sf.getResultType("JM JDE Extract Data");
					Simulation sim = sf.createSimulation("temp");
					Scenario scen = sf.createScenario("temp")) {
				qr.add(correctingDeal.getTransactionId());
				scen.getResultTypes().add(rt);
				sim.addScenario(scen);
				try (SimResults simResults = sim.run(qr);) {
					RevalResults results = simResults.getScenarioResults("temp");
					ConstGeneralResult constGeneralResult = results.getGeneralResult(rt);
					ConstTable jdeExtractData = constGeneralResult.getConstTable();
					double interest = jdeExtractData.getDouble("interest", 0);
					correctingDealInterestField.setValue(interest);
				} finally {
					
				}
			} finally {
				
			}
    	}
    	return PreProcessResult.succeeded();
    }
}
