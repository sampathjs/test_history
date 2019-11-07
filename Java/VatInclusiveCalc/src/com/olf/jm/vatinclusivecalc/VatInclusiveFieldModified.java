package com.olf.jm.vatinclusivecalc;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2018-10-30 - V0.1 - iborisov - Initial Version
 */

/**
 * VatInclusiveFieldModified is Tran Field Notifiction listener that will be configured to trigger 
 * when the user enters or updates one of the VAT Inclusive tran info fields such as Price with VAR or Amount with VAT
 * 
 * @author iborisov
 * @version 0.1
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class VatInclusiveFieldModified extends AbstractTransactionListener {

	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "VAT Inclusive Field Deal Entry"; // sub context of constants repository
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.trading.AbstractTransactionListener#notify(com.olf.embedded.application.Context, com.olf.openrisk.trading.Transaction)
	 */
	@Override
	public void notify(Context context, Transaction tran) {

		try {
			init();
			
			updateTran(context, tran);
			
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			throw new RuntimeException(t);
		} finally {
			PluginLog.info(" ... finished");
		}
	}

	/**
	 * Performs the main operation: <br>
	 * - Identifies whether the transaction is applicable<br>
	 * - Passes the control to the appropriate class for handling
	 * @param tran
	 */
	private void updateTran(Context context, Transaction tran) throws Exception {
		try(VatInclusiveTranHandler tranHandler = VatInclusiveCalcFactory.createHandler(context, tran)) {
			if(tranHandler == null)
			{
				PluginLog.error("The handler is NULL, please check if the handler is defined for this deal type, deal no.->  "+tran.getDealTrackingId());
				throw new UnsupportedException("Handler not available for deal type!! Deal no."+tran.getDealTrackingId());
			}
			
			tranHandler.updateDependingFields();
		}
	}

	/**
	 * Initializes the log by retrieving logging settings from constants repository.
	 * @throws Exception 
	 */
	private void init() throws Exception {

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			ConstRepository constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			PluginLog.init(logLevel, logDir, logFile);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getLocalizedMessage());
		}
	}
	
	
}
