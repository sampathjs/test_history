package com.olf.jm.blockbackdateddealentry.model;

import java.util.Date;

import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

public class CommPhysStartDateValidator extends ValidatorBase implements
		ISettleDateValidator {

	@Override
	public void validateSettleDate(Transaction transaction)
			throws ValidationException {
		Date tradeDate = getTradeDate(transaction);

		for (Leg leg : transaction.getLegs()) {

			Date startDate = leg.getValueAsDate(EnumLegFieldId.StartDate);

			if (startDate.before(tradeDate)) {
				String errorMessage = "Leg " + leg.getLegLabel() + "  has a "
						+ "start date of " + startDate
						+ " which is before the trade date " + tradeDate;
				PluginLog.error(errorMessage);
				throw new ValidationException(errorMessage);
			}
		}
	}

}
