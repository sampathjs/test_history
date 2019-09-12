package com.olf.jm.bankholidaychecks;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.Afs;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
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
		Transaction tran = null;
		try {

			PluginLog.info("Starting " + getClass().getSimpleName());
			init();
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				Transaction newTran = ppi.getTransaction();

				int dealNumber = newTran.getDealTrackingId();
				PluginLog.info("Started processing deal number " + dealNumber);

				String errorMessage = checkResetAgainst(newTran);
				long startTime = System.currentTimeMillis();
				errorMessage = errorMessage + checkPaymentFormula(newTran, context);
				PluginLog.info(String.format("Time taken by checkPaymentFormula - %s", getTimeTaken(startTime, System.currentTimeMillis())));
				
				if (errorMessage != null && !errorMessage.isEmpty()) {
					preProcessResult = PreProcessResult.failed(errorMessage, false);
				}

			}
		} catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			PluginLog.error(message);
			preProcessResult = PreProcessResult.failed(message, false);
		} finally {
			if (tran != null) {
				tran.dispose();
			}
		}

		PluginLog.info("End " + getClass().getSimpleName());
		return preProcessResult;
	}

	private String getTimeTaken(long startTime, long endTime) {
		long duration = endTime - startTime;
		
		int seconds = (int)((duration / 1000) % 60); 
		int minutes = (int) ((duration / 1000) / 60);
		int hours   = (int) ((duration / 1000) / 3600); 

		//String timeTaken = hours + " hour(s), " + minutes + " minute(s) and " + seconds + " second(s)!";
		return String.valueOf(duration);
	}
	
	private String checkPaymentFormula(Transaction newTran, Context context) throws OException {
		com.olf.openjvs.Table fileTable = Util.NULL_TABLE;
		ConstTable PaymentFormulaTable = null;
		String errorMessage = "";
		try {

			if (skipFormulaCheck(newTran, context)) {
				return errorMessage;
			}
			for (Leg leg : newTran.getLegs()) {
				// reset holiday schedule needs to be set for floating leg
				// only.

				PaymentFormulaTable = leg.getField(EnumLegFieldId.PaymentFormula).getValueAsTable();
				String dealFormula = PaymentFormulaTable.getString("TRANF_VALUE_TYPE_STRING", 0);
				
				if(!isNullOrEmpty(dealFormula) && !(dealFormula.equalsIgnoreCase("DEFAULT"))){
				dealFormula = dealFormula.replaceAll("[\\n\\t ]", "");
				PluginLog.info("Deal Formula After trimming spaces, Tabs and new lines " + dealFormula);
				
					int fixFloat = leg.getValueAsInt(EnumLegFieldId.FixFloat);
					if (fixFloat == (com.olf.openrisk.trading.EnumFixedFloat.FixedRate.getValue())) {
						errorMessage = "\u2022 Payment Formula can't be applied to fixed leg. \n"
								+ "Follow below steps on the Deal Input Screen to remove payment formula from Fixed leg : \n"
								+ "- View\n"
								+ "- Payment Formula\n"
								+ "- Pay Fixed Tab\n"
								+ "- Clear Tab\n"
								+ "- Exit\n"
								+ "- Click Yes If prompted to save\n\n";
						PluginLog.info(errorMessage);
						continue;
					}

					fileTable = com.olf.openjvs.Table.tableNew();
					Afs.retrieveTable(fileTable, this.paymentFormula, 1);
					int numOfRows = fileTable.getNumRows();
					if (numOfRows <= 0) {
						String message = String.format("-No Payment formual by the name %s defined in the system. \n", this.paymentFormula);
						PluginLog.error(message);
						throw new OException(message);
					}
					String savedFormula = fileTable.getString(1, 1);
					savedFormula = savedFormula.replaceAll("[\\n\\t ]", "");
					PluginLog.info("\nSaved Formula After trimming spaces, Tabs and new lines " + savedFormula);

					int returnStatus = dealFormula.compareTo(savedFormula);

					if (returnStatus != 0) {
						errorMessage = errorMessage + String.format("\u2022 Deal Payment Formula doesn't match the latest payment formula \n"
								+ "Please follow below steps on Deal Input Screen to Select Correct Payment Formula\n"
								+ "- View\n"
								+ "- Payment Formula\n"
								+ "- Recieve Float Tab\n"
								+ "- Clear Tab\n"
								+ "- File\n"
								+ "- Load Tab Template\n"
								+ "- Select the formula %s\n"
								+ "- Repeat above steps for all the Recieve Float legs\n"
								+ "- Click Yes If prompted to save\n"
								+ "- Exit\n",  this.paymentFormula);
						PluginLog.info(errorMessage);
					}
				}

			}
		} catch (OException exp) {
			errorMessage = "Error while comparing deal payment formula witht the saved payment formula";
			PluginLog.error(errorMessage);
			return errorMessage;
		} finally {
			if (PaymentFormulaTable != null) {
				PaymentFormulaTable.dispose();
			}
			if (com.olf.openjvs.Table.isTableValid(fileTable) == 1) {

			}
			fileTable.destroy();
		}
		return errorMessage;

	}

	private boolean skipFormulaCheck(Transaction newTran, Context context) throws OException {
		boolean skipDeal = false;
		Transactions transactions = null;
		PluginLog.info("Saved Query Name " + savedQueryForSkipDeals);
		try {
			if (savedQueryForSkipDeals.trim().isEmpty()) {
				return skipDeal;
			}
			transactions = getTransactions(savedQueryForSkipDeals, context);
			if (transactions == null) {
				PluginLog.info(String.format("No deals returned as a result of executing saved query - %s", savedQueryForSkipDeals));
				return skipDeal;
			}
			int currentDealTrackingId = newTran.getDealTrackingId();
			PluginLog.info(String.format("Number of deals returned by saved query is %d", transactions.getCount()));
			for (Transaction tran : transactions) {
				if (tran != null) {
					int savedDealTrackingId = tran.getDealTrackingId();
					PluginLog.info(String.format("Deal returned from saved query ", savedDealTrackingId));
					if (currentDealTrackingId == savedDealTrackingId) {
						PluginLog.info(String.format("Deal# %s is saved in the saved query so it will skip the payment formula check", currentDealTrackingId));
						return skipDeal = true;
					}

					PluginLog.info(String.format("Deal# %s is not saved in the saved query. Payment Formula check will be applied to it."));
				}
			}
		} finally {
			if (transactions != null) {
				transactions.dispose();
			}
		}
		return skipDeal;

	}

	private String checkResetAgainst(Transaction newTran) throws OException {

		String errorMessage = "";
		for (Leg leg : newTran.getLegs()) {
			// reset holiday schedule needs to be set for floating leg
			// only.

			if (leg.getValueAsInt(EnumLegFieldId.FixFloat) == (com.olf.openrisk.trading.EnumFixedFloat.FloatRate.getValue())) {
				String dealResetAgainst = leg.getResetDefinition().getField(EnumResetDefinitionFieldId.Against).getValueAsString();
				PluginLog.info(String.format("Deal# %s has reset against set to %s", newTran.getDealTrackingId(), dealResetAgainst));
				if (dealResetAgainst != null && !dealResetAgainst.isEmpty() && !resetAgainst.contains(dealResetAgainst)) {
					errorMessage = String.format("-Reset Against Value has been set to %s \nPlease make it %s in order to proceed.\n\n", dealResetAgainst,
							resetAgainst);
				}
			}
		}
		PluginLog.info(errorMessage);
		return errorMessage;

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

	/**
	 * Gets a collection of transactions by executing a saved query by name.
	 * 
	 * @param queryName
	 *            the query name
	 * @return the transactions
	 * @throws OException
	 */
	private Transactions getTransactions(String queryName, Context context) throws OException {
		Transactions transactions = null;
		TradingFactory tradingFactory = context.getTradingFactory();
		Query query = tradingFactory.getQueries().getQuery(queryName);
		if (null != query) {
			query.execute();
			QueryResult qryResult = query.getResults();
			transactions = tradingFactory.retrieveTransactions(qryResult);
		} else {
			throw new OException(String.format("Could not find saved query with the name: %s", queryName));
		}
		return transactions;
	}
	private boolean isNullOrEmpty(String value) {
		return (value == null || "".equals(value)) ? true : false; 
	}
}