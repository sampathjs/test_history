package com.olf.jm.blockbackdateddealentry.model;

import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

public class SettleDateValidator extends ValidatorBase implements ISettleDateValidator{

	@Override
	public void validateSettleDate(Transaction transaction)
			throws ValidationException {
		validateSettleDateField( transaction, EnumTransactionFieldId.SettleDate );
	}

}
