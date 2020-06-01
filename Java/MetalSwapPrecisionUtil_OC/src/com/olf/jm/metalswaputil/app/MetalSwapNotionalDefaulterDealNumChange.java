package com.olf.jm.metalswaputil.app;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-10-18	V1.0	jwaechter	- Initial Version
 * 2016-10-19	V1.1	jwaechter	- Now executing only if deal tracking num is 
 *                                    not GT 0
 */

/**
 * This plugin clears all values from the tran info fields 
 * "{@link #PRECISE_NOTIONAL_INFO_NAME}" (labeled: Precise Notional) in case the
 * deal tracking num changes ("Clear Tran Num").
 * @author jwaechter
 * @version 1.1
 */

@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class MetalSwapNotionalDefaulterDealNumChange extends
		AbstractTransactionListener {
	public static final String PRECISE_NOTIONAL_INFO_NAME = "NotnldpSwap";
	
	private static final String CREPO_CONTEXT = "FrontOffice";
	private static final String CREPO_SUBCONTEXT = "MetalSwap";

	
    /**
     * {@inheritDoc}
     */
	@Override
    public void notify(final Context context, final Transaction tran) {
		try {
			initLogging();
			if (tran.getDealTrackingId() > 0) {
				Logging.info("Deal num is greater than 0, skipping");
				return;
			}
			for (Leg leg : tran.getLegs()) {
				Field field = leg.getField(PRECISE_NOTIONAL_INFO_NAME);
				if (field == null || !field.isApplicable() || !field.isWritable()) {
					continue;
				}
				field.setValue("");
				Logging.info("Param info field '" + PRECISE_NOTIONAL_INFO_NAME + "' " + 
						"' is cleared for leg " + leg.getLegNumber());
			}
			Logging.info("Finishes processing transaction");			
		} catch (Throwable t) {
			Logging.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
		}finally{
			Logging.close();
		}
    }
	
	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() {
		// Constants Repository Statics
		try {
			ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
					CREPO_SUBCONTEXT);
			String logLevel = constRep.getStringValue("logLevel", "info");
			String logFile = constRep.getStringValue("logFile", this.getClass()
					.getSimpleName()
					+ ".log");
			String logDir = constRep.getStringValue("logDir", Util.getEnv("AB_OUTDIR") + "\\error_logs");

			Logging.init(this.getClass(),CREPO_CONTEXT,	CREPO_SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			throw new RuntimeException(errMsg, e);
		}
	}
}
