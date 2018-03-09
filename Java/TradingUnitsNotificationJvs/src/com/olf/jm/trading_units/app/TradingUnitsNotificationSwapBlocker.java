package com.olf.jm.trading_units.app;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRANF_FIELD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-01-21	V1.0	jwaechter	- Initial Version
 * 2016-01-25	V1.1	jwaechter	- Added processing of Non Pass through deals
 * 2016-10-17	V1.2	jwaechter	- "Generated Offset" is now considered as a
 *                                    no pass thru indicator.
 */

/**
 * This plugin contains a pre process trading OPS blocking the processing of FX swap deals in case the 
 * trading price on the far leg is the same like on the near leg.
 * @author jwaechter
 * @version 1.2
 */
public class TradingUnitsNotificationSwapBlocker implements IScript {
	public static final double EPSILON = 0.00000001d;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		OConsole.oprint ("\n\n\n\n\n\n\n\n " + this.getClass().getSimpleName() + " STARTED.");
		try {
			initLogging();
			process();
			PluginLog.info(this.getClass().getName() + " finished successfully");
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			throw t;
		}
	}
	
	private void process() throws OException {
		//if (Util.canAccessGui() == 0) {
		//	PluginLog.info("Can't access GUI. Skipping processing");
		//	return;
		//}
		Map<Integer, List<Double>> tradePrices = new HashMap<>();

		//PluginLog.info("Can access GUI");
		for (int i = OpService.retrieveNumTrans(); i >= 1;i--) {
			Transaction origTran = OpService.retrieveTran(i);
			int dealTrackingNum = origTran.getFieldInt(TRANF_FIELD.TRANF_TRAN_GROUP.jvsValue(), 0);
			PluginLog.info("Processing transaction #" + origTran.getTranNum());
			String cflowType = origTran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.jvsValue());
			if (!(cflowType.equals("Swap") || cflowType.equals("Location Swap") || cflowType.equals("Quality Swap"))) {
				PluginLog.info("Skipping transaction as cash flow type is " + cflowType);
				continue;
			}
			String offsetTranType = origTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.jvsValue());
			if (offsetTranType == null || offsetTranType.equals("") || isPTE(offsetTranType) || isNoPassThrough(offsetTranType)) {
				PluginLog.info("Processing transaction having offset tran type " + offsetTranType);				
				double tradePrice = origTran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.jvsValue(), 0, 
						TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME);
				addToPrices (tradePrices, dealTrackingNum, tradePrice);
			} else if (isPTI(offsetTranType) || isPTO (offsetTranType)) {			
				PluginLog.info("Skipping transaction as transactio is either Pass Thru Internal or Pass Thru Offset");
			}
			PluginLog.info("Finished Processing transaction #" + origTran.getTranNum());
		}
		for (int dealTrackingNum : tradePrices.keySet()) {
			List<Double> prices = tradePrices.get(dealTrackingNum);
			if (prices.size() != 2) {
				continue;
			}
			double tradePriceFar = prices.get(0);
			double tradePrice = prices.get(1);
			if (Math.abs(tradePriceFar-tradePrice) < EPSILON ) {
				String message = "Note that Trade Price on Far and Near leg are identical. Please Commit to proceed or Cancel to enter different Trade price";
				PluginLog.info(message);
				OpService.serviceFail(message, 1);
			}
		}
		
	}

	private void addToPrices(Map<Integer, List<Double>> tradePrices, int dealTrackingNum, double tradePrice) {
		if (!tradePrices.containsKey(dealTrackingNum)) {
			tradePrices.put(dealTrackingNum, new ArrayList<Double>(2));
		}
		List<Double> prices = tradePrices.get(dealTrackingNum);
		prices.add(tradePrice);
	}

	private boolean isNoPassThrough(String offsetTranType) {
		return offsetTranType.equals("No Offset") || offsetTranType.equals("Generated Offset");
	}
	
	private boolean isPTE(String offsetTranType) {
		return "Pass Thru External".equals(offsetTranType);
	}

	private boolean isPTI(String offsetTranType) {
		return "Pass Thru Internal".equals(offsetTranType);
	}

	private boolean isPTO(String offsetTranType) {
		return "Pass Thru Offset".equals(offsetTranType) || "Pass Through Party".equals(offsetTranType);
	}
	
	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		// Constants Repository Statics
		ConstRepository constRep = new ConstRepository(TradingUnitsNotificationJVS.CREPO_CONTEXT,
				TradingUnitsNotificationJVS.CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", this.getClass()
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {		
			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
			PluginLog.info("*****************" + this.getClass().getCanonicalName() + " started ********************");
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
