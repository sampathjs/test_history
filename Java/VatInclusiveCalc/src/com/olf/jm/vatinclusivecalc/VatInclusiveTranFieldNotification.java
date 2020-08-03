package com.olf.jm.vatinclusivecalc;


import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

@ScriptCategory(EnumScriptCategory.OpsSvcTranfield)
public class VatInclusiveTranFieldNotification extends AbstractFieldListener{
	
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "VAT Inclusive Field Deal Entry"; // sub context of constants repository
	

	@Override	
	public void postProcess(Session session, Field field, String oldValue,  String newValue,
             Table clientData) {

    Transaction tran = field.getTransaction();
	try {
		init();
		
		updateTran(session, tran);
		
	} catch (Throwable t) {
		Logging.error(t.toString());
		throw new RuntimeException(t);
	} finally {
		Logging.info(" ... finished");
		Logging.close();
	}
	
	}
	
	/**
	 * Performs the main operation: <br>
	 * - Identifies whether the transaction is applicable<br>
	 * - Passes the control to the appropriate class for handling
	 * @param tran
	 */
	private void updateTran(Session session, Transaction tran) throws Exception {
		try(VatInclusiveTranHandler tranHandler = VatInclusiveCalcFactory.createHandler(session, tran)) {
			if(tranHandler == null)
			{
				Logging.error("The handler is NULL, please check if the handler is defined for this deal type, deal no.->  "+tran.getDealTrackingId());
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

			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getLocalizedMessage());
		}
	}
	
	
	
}
