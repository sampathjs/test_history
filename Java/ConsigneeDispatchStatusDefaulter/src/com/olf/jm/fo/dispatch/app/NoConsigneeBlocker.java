package com.olf.jm.fo.dispatch.app;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class NoConsigneeBlocker implements IScript {
	public static final String CREPO_CONTEXT = "FrontOffice";
	public static final String CREPO_SUBCONTEXT = "ConsigneeAndDispatch";
	private static final String CONSIGNEE_INFO_FIELD = "Consignee";

	
	@Override
	public void execute(IContainerContext context) throws OException {
		initLogging();
		
		for (int i=1; i <= OpService.retrieveNumTrans(); i++) {
			Transaction tran = OpService.retrieveTran(i);
			String consignee = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, CONSIGNEE_INFO_FIELD);
			int buySell = tran.getFieldInt(TRANF_FIELD.TRANF_BUY_SELL.toInt());
			if (buySell == BUY_SELL_ENUM.SELL.toInt()
				&& (consignee == null || consignee.isEmpty())) {
				OpService.serviceFail("Please select a Consignee", 0);
			}			
		}
		Logging.close();
	}

	
	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		// Constants Repository Statics
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", this.getClass()
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {
			
			Logging.init(this.getClass(), CREPO_CONTEXT, CREPO_SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}

