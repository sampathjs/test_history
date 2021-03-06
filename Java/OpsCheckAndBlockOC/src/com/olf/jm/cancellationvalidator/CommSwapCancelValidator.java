package com.olf.jm.cancellationvalidator;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OException;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

/**
 * Concrete class specific to CommSwap Toolset
 * 
 * @author
 * 
 */
public class CommSwapCancelValidator extends AbstractValidator {
	/**
	 * @param tradeDate
	 * @param currentTradingDate
	 */

	

	public CommSwapCancelValidator(Transaction tran, Context context) {
		super(tran, "Cancellation is blocked for this deal due to business rules, \n"
				+ "cancellation needs to be approved by Finance,\nplease contact Support team for help", context);

	}

	@Override
	public boolean isCancellationAllowed() throws OException {

		boolean cancellationAllowed = false;
		int maxResetDate = 0;
		try {
			int dealTradeDate = getDealTradeDate();
			int currentTradingDate = getCurrentTradingDate();
			
				// If trade has maturity date in same month then Allow
				// cancellation
				// till month end
				// else allow cancellation till last reset date.
				if (isSameMonth(currentTradingDate, dealTradeDate)) {
					cancellationAllowed = true;
					Logging.info("Trade Month on the deal is same as current Month. Deal can be cancelled");

				} else {
					// Allow cancellation till last reset date
					Logging.info("Trade Month on the deal is in past, check if the last reset date has passed");
					maxResetDate = getMaxResetDate();
					if (maxResetDate >= currentTradingDate) {
						cancellationAllowed = true;
						Logging.info("Last Reset Date for this deal is in future. This can be cancelled");
					}

				}

		
			if (!cancellationAllowed) {

				Logging.info("Cancellation criteria is not satisfied. This deal can't be cancelled ");
			}
		} catch (OException exp) {
			Logging.error("There was an error checking cancellation criteria for this deal" + exp.getMessage());
			throw new OException(exp.getMessage());
		}

		return cancellationAllowed;

	}

	private int getMaxResetDate() {
		int maxResetDate = 0;
		Legs legs = tran.getLegs();
		for (Leg leg : legs) {
			if (leg.getValueAsInt(EnumLegFieldId.FixFloat) == (com.olf.openrisk.trading.EnumFixedFloat.FloatRate.getValue())) {
				int resetDate = leg.getReset(leg.getResets().size() - 1).getValueAsInt(EnumResetFieldId.Date);
				if (resetDate > maxResetDate) {
					maxResetDate = resetDate;
				}
			}
		}
		return maxResetDate;
	}



}
