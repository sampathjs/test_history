package com.olf.jm.fo.tranfield.tozgmmetalpricecalc;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class GramPriceCalculator extends AbstractTransactionListener {

	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context 
	public static final String CONST_REPO_SUBCONTEXT = "GmPriceCalculator"; // sub context 
	public static final String FIELD_BASE_METAL_PRICE_WITH_VAT = "Base Metal Price with VAT";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olf.embedded.trading.AbstractTransactionListener#notify(com.olf.embedded .application.Context, com.olf.openrisk.trading.Transaction)
	 */
	@Override
	public void notify(Context context, Transaction tran) {

		try {
			init(context);
			CalculatorUtil util = new CalculatorUtil(context);
			util.setGmField(context, tran);


		} catch (Throwable t) {
			Logging.error(t.toString());
			throw new RuntimeException(t);
		} finally {
			Logging.info(" ... finished");
			Logging.close();
		}
	}

	

	protected boolean isSupported(Transaction tran) {
		// Supports only Loan Dep
		if (tran.getToolset() == EnumToolset.Loandep) {
			return true;
		} else {
			return false;
		}

	}
	

	/**
	 * Initialises the log by retrieving logging settings from constants repository.
	 * 
	 * @param context
	 * @throws Exception
	 */
	private void init(Session session) throws Exception {
	
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
			throw new Exception("Error initialising logging. "  + e.getLocalizedMessage());
		}
	}
}
