package com.olf.jm.cancellationvalidator;

import com.olf.embedded.application.Context;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openjvs.OException;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

/**
 * Concrete class specific to FX Toolset
 * 
 * @author
 * 
 */
public class FXCancelValidator extends AbstractValidator {
	/**
	 * @param tradeDate
	 * @param currentTradingDate
	 */

	private static final String LINKED_DEAL = "Linked Deal";

	public FXCancelValidator(Transaction tran, Context context) {
		super(tran, "Cancellation is blocked for this deal due to business rules, \n"
				+ "cancellation needs to be approved by Finance,\nplease contact Support team for help", context);
	}

	@Override
	public boolean isCancellationAllowed() throws OException {

		boolean cancellationAllowed = false;
		try {
			int dealTradeDate = getDealTradeDate();
			int currentTradingDate = getCurrentTradingDate();
			
//			if (tran.getToolset() == EnumToolset.Fx ){
//				String jdeStatus = tran.getField("General Ledger").getDisplayString();
//				int baseCurrencyId = tran.getValueAsInt(EnumTransactionFieldId.FxBaseCurrency);
//				int termCurrencyId = tran.getValueAsInt(EnumTransactionFieldId.FxTermCurrency);
//				Currency baseCur =  (Currency) context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, baseCurrencyId);
//				Currency termCur =  (Currency) context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, termCurrencyId);	
//				if ( !baseCur.isPreciousMetal() && !termCur.isPreciousMetal()) {
//					if (jdeStatus.equalsIgnoreCase("Sent")) {
//						cancellationAllowed = false;
//					} else {
//						cancellationAllowed = true;
//					}
//					return cancellationAllowed;
//				}
//			}
	        
			cancellationAllowed = monthDiff(dealTradeDate, currentTradingDate) <= 0;

			if (tran.getToolset() == EnumToolset.Fx && !cancellationAllowed) {
				cancellationAllowed = isLinkedFutCancelled();
			}
			if (!cancellationAllowed) {
				Logging.info("Trade Month on the deal is in past. Deal can't be cancelled");
			} else {
				Logging.info("Trade Month on the deal is same as current Month. Deal can be cancelled");
			}           		
		} catch (OException exp) {
			Logging.error("There was an error comparing the Trade date of deal and the current Trading date");
			throw new OException(exp.getMessage());
		}

		return cancellationAllowed;

	}

	private boolean isLinkedFutCancelled() throws OException {
		Transaction linkedFutTranPtr = null;
		boolean returnFlag = false;
		try {
			int linkedDeal = tran.getField(LINKED_DEAL).getValueAsInt();
			if (linkedDeal > 0) {
				linkedFutTranPtr = context.getTradingFactory().retrieveTransactionByDeal(linkedDeal);
				if ((linkedFutTranPtr.getTransactionStatus() == EnumTranStatus.Cancelled) || (linkedFutTranPtr.getTransactionStatus() == EnumTranStatus.CancelledNew) ) {
					returnFlag = true;
				}
			}
		} finally {
			if (linkedFutTranPtr != null) {
				linkedFutTranPtr.dispose();
			}
		}

		return returnFlag;
	}

}
