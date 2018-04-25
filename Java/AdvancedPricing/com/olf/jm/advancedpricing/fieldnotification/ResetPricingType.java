package com.olf.jm.advancedpricing.fieldnotification;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.advancedpricing.model.TranInfoField;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2017-07-26	V1.0 	sma	- Initial Version
 */ 

/**
 * Field notification script used to reset the tran info field Pricing Type.
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class ResetPricingType extends AbstractTransactionListener {

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";

	/* (non-Javadoc)
	 * @see com.olf.embedded.trading.AbstractTransactionListener#notify(com.olf.embedded.application.Context, com.olf.openrisk.trading.Transaction)
	 */
	@Override
	public void notify(Context context, Transaction tran) {
		try {
			init(context, this.getClass().getSimpleName());
			
			process(context, tran);
			
		} catch (Exception e) {
			PluginLog.error("Error resetting Tran Info Fields Pricing Type. " + e.getMessage());
		}
	}
	
	
	 /**
 	 * Reset the tran info field Pricing Type.
 	 *
 	 * @param context the context the script is running in.
 	 * @param tran the transaction to reset the field on. 
 	 */
 	private void process(Context context, Transaction tran) {
		 AbstractFieldReset fieldResetter = new ResetPicklistToDefault(context);
		 
		 fieldResetter.resetField(tran, TranInfoField.PRICING_TYPE.getName());
	 }
 	
 	/**
	 * Initial plug-in log by retrieving logging settings from constants repository.
	 * @param class1 
	 * @param context
	 */
	private void init(Session session, String pluginName)  {	
		try {
			String abOutdir = Util.getEnv("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPOSITORY_CONTEXT, 
					CONST_REPOSITORY_SUBCONTEXT);
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", pluginName + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			PluginLog.info(pluginName + " started");
		} catch (OException e) {
			PluginLog.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		}
	}

}
