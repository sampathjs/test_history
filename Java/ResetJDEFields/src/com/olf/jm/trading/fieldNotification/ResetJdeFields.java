package com.olf.jm.trading.fieldNotification;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2016-11-07	V1.0 	scurran	- Initial Version
 */ 

/**
 * Field notification script used to reset the tran info field 
 * associated with the JDE interface.
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class ResetJdeFields extends AbstractTransactionListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "JDE";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "Trading";
	
	
	
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.trading.AbstractTransactionListener#notify(com.olf.embedded.application.Context, com.olf.openrisk.trading.Transaction)
	 */
	@Override
	public void notify(Context context, Transaction tran) {
		try {
			init();
			
			process(context, tran);
			
		} catch (Exception e) {
			PluginLog.error("Error resetting JDE Info Fields. " + e.getMessage());
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
	 
	 /**
 	 * Reset the JDE info fields.
 	 *
 	 * @param context the context the script is running in.
 	 * @param tran the transaction to reset the field on. 
 	 */
 	private void process(Context context, Transaction tran) {
		 AbstractFieldReset fieldResetter = new ResetPicklistToDefault(context);
		 
		 fieldResetter.resetField(tran, "General Ledger");
		 fieldResetter.resetField(tran, "Metal Ledger");
	 }

}
