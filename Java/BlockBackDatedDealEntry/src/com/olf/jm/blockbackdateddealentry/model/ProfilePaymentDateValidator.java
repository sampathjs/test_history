package com.olf.jm.blockbackdateddealentry.model;

import java.util.Date;

import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

public class ProfilePaymentDateValidator extends ValidatorBase implements ISettleDateValidator {

	@Override
	public void validateSettleDate(Transaction transaction)
			throws ValidationException {

		Date tradeDate = getTradeDate(transaction);
		
		for(Leg leg : transaction.getLegs()) {
		
			for(Profile profile : leg.getProfiles()) {

				Date profilePaymentDate = profile.getValueAsDate(EnumProfileFieldId.PaymentDate);
				
				if(profilePaymentDate.before(tradeDate)) {
					String errorMessage = "Leg " + leg.getLegLabel() + " period " + (profile.getProfileNumber() + 1) + " has a "
							+ "payment date of " + profilePaymentDate + " which is before the trade date "
							+ tradeDate;
					Logging.error(errorMessage);
					throw new ValidationException(errorMessage);
				}
			}
		}
		
	}

}
