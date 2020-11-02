package com.olf.jm.bankholidaychecks;

import com.olf.embedded.application.Context;
import com.olf.openjvs.Afs;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;


public class PaymentFormulaCheck {

	private final Context context;
	private final String savedQueryForSkipDeals;
	private final String  paymentFormula;
	
	public PaymentFormulaCheck(Context context, String savedQueryForSkipDeals,  String paymentFormula){
		this.context = context;
		this.savedQueryForSkipDeals = savedQueryForSkipDeals;
		this.paymentFormula =  paymentFormula;
	}
	
	protected String checkPaymentFormula(Transaction newTran) throws OException {
		com.olf.openjvs.Table fileTable = Util.NULL_TABLE;
		ConstTable PaymentFormulaTable = null;
		String errorMessage = "";
		try {

			if (skipFormulaCheck(newTran)) {
				return errorMessage;
			}
			for (Leg leg : newTran.getLegs()) {

				String legLabel = leg.getLegLabel();
				PaymentFormulaTable = leg.getField(EnumLegFieldId.PaymentFormula).getValueAsTable();
				String dealFormula = PaymentFormulaTable.getString("TRANF_VALUE_TYPE_STRING", 0);
				
				if( !isNullOrEmpty(dealFormula)){
				dealFormula = dealFormula.replaceAll("[\\n\\t ]", "");
				Logging.info("Deal Formula After trimming spaces, Tabs and new lines " + dealFormula);
				
				int fixFloat = leg.getValueAsInt(EnumLegFieldId.FixFloat);
				if (fixFloat == (com.olf.openrisk.trading.EnumFixedFloat.FixedRate.getValue()) && !(dealFormula.equalsIgnoreCase("DEFAULT"))) {
					errorMessage = errorMessage + String.format("\u2022 Payment Formula can't be applied to leg %s. \n"
							+ "Follow below steps on the Deal Input Screen to remove payment formula: \n"
							+ "- View\n"
							+ "- Payment Formula\n"
							+ "- %s Tab\n"
							+ "- Clear Tab\n"
							+ "- Exit\n"
							+ "- Click Yes If prompted to save\n\n",legLabel, legLabel );
					Logging.info(errorMessage);
					continue;
				}else if(fixFloat == (com.olf.openrisk.trading.EnumFixedFloat.FloatRate.getValue()) && !(dealFormula.equalsIgnoreCase("DEFAULT"))){
					/*if((dealFormula.equalsIgnoreCase("DEFAULT"))){
						errorMessage = errorMessage + String.format("\u2022 Deal Payment Formula hasn't been applied on leg %s \n"
								+ "Please follow below steps on Deal Input Screen to Select Correct Payment Formula\n"
								+ "- View\n"
								+ "- Payment Formula\n"
								+ "- %s Tab\n"
								+ "- Clear Tab\n"
								+ "- File\n"
								+ "- Load Tab Template\n"
								+ "- Select the formula %s\n"
								+ "- Click Yes If prompted to save\n"
								+ "- Exit\n", legLabel,legLabel, this.paymentFormula);
						Logging.info(errorMessage);
						continue;
					}
					*/
					fileTable = com.olf.openjvs.Table.tableNew();
					Afs.retrieveTable(fileTable, this.paymentFormula, 1);
					int numOfRows = fileTable.getNumRows();
					if (numOfRows <= 0) {
						String message = String.format("-No Payment formual by the name %s defined in the system. \n", this.paymentFormula);
						Logging.error(message);
						throw new OException(message);
					}
					String savedFormula = fileTable.getString(1, 1);
					savedFormula = savedFormula.replaceAll("[\\n\\t ]", "");
					Logging.info("\nSaved Formula After trimming spaces, Tabs and new lines " + savedFormula);

					int returnStatus = dealFormula.compareTo(savedFormula);

					if (returnStatus != 0) {
						errorMessage = errorMessage + String.format("\u2022 Deal Payment Formula doesn't match the latest payment formula on %s \n"
								+ "Please follow below steps on Deal Input Screen to Select Correct Payment Formula\n"
								+ "- View\n"
								+ "- Payment Formula\n"
								+ "- %s Tab\n"
								+ "- Clear Tab\n"
								+ "- File\n"
								+ "- Load Tab Template\n"
								+ "- Select the formula %s\n"
								+ "- Repeat above steps for all the Float Tabs\n"
								+ "- Click Yes If prompted to save\n"
								+ "- Exit\n", legLabel,legLabel, this.paymentFormula);
						Logging.info(errorMessage);
						}
						
				}
				}

			}
		} catch (OException exp) {
			errorMessage = "Error while comparing deal payment formula witht the saved payment formula";
			Logging.error(errorMessage);
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

	private boolean skipFormulaCheck(Transaction newTran) throws OException {
		boolean skipDeal = false;
		int tranNumArray[] = null;
		Logging.info("Saved Query Name " + savedQueryForSkipDeals);
		
			if (savedQueryForSkipDeals.trim().isEmpty()) {
				return skipDeal;
			}
			tranNumArray = getTransactions(savedQueryForSkipDeals, context);
			if (tranNumArray == null) {
				Logging.info(String.format("No deals returned as a result of executing saved query - %s", savedQueryForSkipDeals));
				return skipDeal;
			}
			int currentTranNum = newTran.getTransactionId();
			Logging.info(String.format("Number of deals returned by saved query is %d deals are %s", tranNumArray.length, tranNumArray.toString()));
			for (int tran : tranNumArray) {

					Logging.info(String.format("Deal returned from saved query %s ", tran));
					if (currentTranNum == tran) {
						Logging.info(String.format("Deal# %s is saved in the saved query so it will skip the payment formula check", currentTranNum));
						return skipDeal = true;
					}

					Logging.info(String.format("Deal# %s is not saved in the saved query. Payment Formula check will be applied to it.",currentTranNum) );
				}
			
			
		
		return skipDeal;

	}

	

	/**
	 * Gets a collection of transactions by executing a saved query by name.
	 * 
	 * @param queryName
	 *            the query name
	 * @return the transactions
	 * @throws OException
	 */
	private int[] getTransactions(String queryName, Context context) throws OException {

		int tranNumArray [] = null;
		TradingFactory tradingFactory = context.getTradingFactory();
		Query query = tradingFactory.getQueries().getQuery(queryName);
		if (null != query) {
			query.execute();
			QueryResult qryResult = query.getResults();
			 tranNumArray = qryResult.getObjectIdsAsInt();

		} else {
			throw new OException(String.format("Could not find saved query with the name: %s", queryName));
		}
		return tranNumArray;
	}
	private boolean isNullOrEmpty(String value) {
		return (value == null || "".equals(value)) ? true : false; 
	}
}