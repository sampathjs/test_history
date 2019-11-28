package com.olf.jm.cancellationvalidator;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.DealEvents;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumDealEventGroup;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/**
 * Concrete class specific to Loan Dep Toolset
 * 
 * @author
 * 
 */
public class LoanDepCancelValidator extends AbstractValidator {

	public LoanDepCancelValidator(Transaction tran, Context context) {
		super(tran, "Cancellation is blocked for this deal due to business rules, \n "
				+ "cancellation needs to be approved by Finance,\nplease contact Support team for help", context);

	}

	@Override
	public boolean isCancellationAllowed() throws OException {
		boolean cancellationAllowed = false;
		try {
			int dealSettleDate = getSettleDate();
			int currentTradingDate = getCurrentTradingDate();
			int dealTradeDate = getDealTradeDate();
			boolean isFutureSettleDate = (dealSettleDate >= currentTradingDate);
	

			// If trade month is past and settle date has not arrived show
			// warning message and allow over ride.
			// Else allow cancellation only if settle date has not arrived yet.
			if (isFutureMonth(currentTradingDate, dealTradeDate) && isFutureSettleDate) {
				errorMessage = "To confirm cancellation click commit else click cancel.";
				allowOverride = true;
				// cancellationAllowed is set to false because we want to show
				// warning message
				cancellationAllowed = false;
				PluginLog.info("Trade Date on this deal is in Past and Settle date has not arrived Yet. It can be cancelled. ");
			} else if (isFutureSettleDate) {
				cancellationAllowed = true;
				PluginLog.info("Settle date has not arrived Yet. It can be cancelled. ");
			}
			
			if (!cancellationAllowed && !allowOverride) {
				PluginLog.info("Cancellation criteria for this deal is not satisfied. It can't be cancelled. ");
			}
		} catch (OException exp) {
			PluginLog.error("There was an error checking cacnellation criteria for this deal \n" + exp.getMessage());
			throw new OException(exp.getMessage());
		}

		return cancellationAllowed;

	}

	private int getSettleDate() throws OException {

		int minSettleDate = 0;
		DealEvents settleEvent = tran.getDealEvents(EnumDealEventGroup.Settlement);
		for (DealEvent event : settleEvent) {
			if (event.getField("pymt_type").getValueAsInt() == EnumCashflowType.Interest.getValue()) {
				int eventDate = event.getField("event_date").getValueAsInt();
				if (minSettleDate == 0 || minSettleDate > eventDate) {
					minSettleDate = eventDate;
				}

			}

		}
		PluginLog.info("Minimum Cash Settlement Date on deal " + tran.getDealTrackingId() + " Is " + OCalendar.formatJd(minSettleDate));
		return minSettleDate;
	}

}
