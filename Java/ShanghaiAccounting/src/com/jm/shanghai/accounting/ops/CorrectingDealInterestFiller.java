package com.jm.shanghai.accounting.ops;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.EnumQueryType;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.simulation.ConstGeneralResult;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CorrectingDealInterestFiller extends AbstractTradeProcessListener {
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "CorrectingDealInterestFiller"; // sub context of constants repository

	
    public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
            final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
    	init(context);
    	PluginLog.info("Start of plugin '" + getClass().getName() + "'");
    	for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
    		Transaction tran = ppi.getTransaction();
    		PluginLog.info("Processing transaction #" + tran.getTransactionId());
    		Field interfaceTranTypeField = tran.getField("Interface_Trade_Type");
    		if (interfaceTranTypeField == null) {
        		PluginLog.info("Skipping transaction #" + tran.getTransactionId() + " as no " 
        			+ " tran info field 'Interface_Trade_Type' is found");
    			continue;
    		}
    		if (!interfaceTranTypeField.getValueAsString().equalsIgnoreCase("Correction")) {
        		PluginLog.info("Skipping transaction #" + tran.getTransactionId() + " as " 
            			+ " tran info field 'Interface_Trade_Type' is not 'Correction'");
    			continue;
    		}
    		Field correctingDealField =  tran.getField("Correcting Deal");
    		if (correctingDealField == null) {
        		PluginLog.info("Skipping transaction #" + tran.getTransactionId() + " as no " 
            			+ " tran info field 'Correcting Deal' is found");
    			continue;
    		}
    		if (correctingDealField.getValueAsString() == null || correctingDealField.getValueAsString().isEmpty()) {
        		PluginLog.info("Skipping transaction #" + tran.getTransactionId() + " as " 
            			+ " tran info field 'Correcting Deal' is null or empty");
    			continue;
    		}
			Field correctingDealInterestField = tran.getField("Correcting Deal Interest");
			if (correctingDealInterestField == null) {
        		PluginLog.info("Skipping transaction #" + tran.getTransactionId() + " as no " 
            			+ " tran info field 'Correcting Deal Interest' is found");
				continue;
			}
    		int correctingDealNumber = correctingDealField.getValueAsInt();
    		Transaction correctingDeal = null;
    		try {
    			correctingDeal = context.getTradingFactory().retrieveTransaction(correctingDealNumber);
    		} catch (OpenRiskException ex) {
    	    	return PreProcessResult.failed("The transaction #" + tran.getTransactionId()
    	    			+ " references transaction #" + correctingDealNumber + " as correcting deal "
    	    			+ " in the tran info field 'Correcting Deal', but this deal does not exist");
    		}
    		
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
	        		PluginLog.info("Setting tran info field 'Correcting Deal Interest' of "
	        				+ " transaction #" + tran.getTransactionId() + " to " 
	            			+ interest);
				} finally {
					
				}
			} finally {
				
			}
    	}
    	return PreProcessResult.succeeded();
    }
    
	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(final Session session) {
		try {
			String abOutdir = session.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		PluginLog.info("\n\n********************* Start of new run ***************************");		
	}
}
