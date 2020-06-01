package com.olf.jm.blockbackdateddealentry.model;

import java.util.Date;

import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

public class ValidatorBase {

	public ValidatorBase() {
		super();
	}

	protected Date getTradeDate(Transaction transaction) {
		return transaction.getValueAsDateTime(EnumTransactionFieldId.TradeDate);
	}
	
	protected void validateSettleDateField(Transaction transaction, EnumTransactionFieldId fieldId) throws ValidationException { 
		try(Field settleDateField = transaction.getField(fieldId)) {
			if(settleDateField.isApplicable()) {
				Date tradeDate = getTradeDate(transaction);
				
				Date settleDate = settleDateField.getValueAsDate();
				
				if(settleDate.before(tradeDate)) {
					String errorMessage = "Field " + settleDateField.getName() + " has a "
							+ "date of " + settleDate + " which is before the trade date "
							+ tradeDate;
					Logging.error(errorMessage);
					throw new ValidationException(errorMessage);
				}				
			}
		} 
	}

}