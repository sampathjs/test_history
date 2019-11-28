package com.olf.jm.bankholidaychecks;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class BankHolidayFactorsCheck extends AbstractTradeProcessListener {

	private ConstRepository constRep;

	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "OpsService";

	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "MetalSwapValidations";
	private String resetAgainst = null;

	private String paymentFormula;

	private String savedQueryForSkipDeals = "";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus, final PreProcessingInfo<EnumTranStatus>[] infoArray,
			final Table clientData) {
		PreProcessResult preProcessResult = PreProcessResult.succeeded();

		try {

			PluginLog.info("Starting " + getClass().getSimpleName());
			init();
			ResetAgainstCheck resetAgainstCheck = new ResetAgainstCheck(resetAgainst);
			PaymentFormulaCheck paymentFormulaCheck = new PaymentFormulaCheck(context, savedQueryForSkipDeals, paymentFormula);
			
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				Transaction newTran = ppi.getTransaction();

				int dealNumber = newTran.getDealTrackingId();
				PluginLog.info("Started processing deal number " + dealNumber);

				String errorMessage = resetAgainstCheck.checkResetAgainst(newTran);
				
				errorMessage = errorMessage + paymentFormulaCheck.checkPaymentFormula(newTran);
				
				
				if (errorMessage != null && !errorMessage.isEmpty()) {
					preProcessResult = PreProcessResult.failed(errorMessage, false);
				}
				

			}
		} catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			PluginLog.error(message);
			preProcessResult = PreProcessResult.failed(message, false);
		} 
		

		PluginLog.info("End " + getClass().getSimpleName());
		return preProcessResult;
	}

	
	

	/**
	 * Initialise logging
	 * 
	 * @throws Exception
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);
			this.resetAgainst = constRep.getStringValue("resetAgainst", "0d");
			this.paymentFormula = constRep.getStringValue("paymentFormula", "PUBLIC/StructPaymentFormula/MetalSwap_PaymentFormula");
			this.savedQueryForSkipDeals = constRep.getStringValue("savedQueryForSkipDeals", "");

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
}