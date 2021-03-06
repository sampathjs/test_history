package com.olf.jm.trading_units.app;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-02-10	V1.0	jwaechter	- Initial Version
 */

/**
 * This plugin contains a pre process trading OPS blocking the processing of FX deals having an empty trade price
 * @author jwaechter
 * @version 1.1
 */
public class TradingUnitsNotificationEmptyTradePriceBlocker implements IScript {
	private static final String SOMETHING = "something";
	public static final double EPSILON = 0.00000001d;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		OConsole.oprint ("\n\n\n\n\n\n\n\n " + this.getClass().getSimpleName() + " STARTED.");
		try {
			initLogging();
			process();
			Logging.info(this.getClass().getName() + " finished successfully");
		} catch (Throwable t) {
			Logging.error(t.toString());
			throw t;
		}finally {
			Logging.close();
		}
	}
	
	private void process() throws OException {
		//if (Util.canAccessGui() == 0) {
		//	Logging.info("Can't access GUI. Skipping processing");
		//	return;
		//}
		//Logging.info("Can access GUI");
		for (int i = OpService.retrieveNumTrans(); i >= 1;i--) {
			Transaction origTran = OpService.retrieveTran(i);
			Logging.info("Processing transaction #" + origTran.getTranNum());
//			Transaction tran = OpService.retrieveTran(i);
			String cflowType = origTran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
			String offsetTranType = origTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.toInt());

			if (offsetTranType == null || offsetTranType.equals("") || isPTE(offsetTranType) || isNoPassThrough(offsetTranType)) {
				Logging.info("Processing transaction having offset tran type " + offsetTranType);
				String tradePrice = origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, 
						TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME);
				
				if (tradePrice == null || tradePrice.equals("")) {
					String message = "Note that Trade Price is not entered. Please Validate trade after entering Trade Price";
					Logging.info(message);
					OpService.serviceFail(message, 0);
				} 
			} else if (isPTI(offsetTranType) || isPTO (offsetTranType)) {			
				Logging.info("Skipping transaction as transactio is either Pass Thru Internal or Pass Thru Offset");
			}				
			Logging.info("Finished Processing transaction #" + origTran.getTranNum());
		}
	}

	private boolean isNoPassThrough(String offsetTranType) {
		return offsetTranType.equals("No Offset");
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
			Logging.init(this.getClass(), TradingUnitsNotificationJVS.CREPO_CONTEXT, TradingUnitsNotificationJVS.CREPO_SUBCONTEXT);
			Logging.info("*****************" + this.getClass().getCanonicalName() + " started ********************");
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
