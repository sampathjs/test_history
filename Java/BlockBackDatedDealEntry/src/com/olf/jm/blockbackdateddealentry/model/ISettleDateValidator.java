package com.olf.jm.blockbackdateddealentry.model;

import com.olf.openrisk.trading.Transaction;

public interface ISettleDateValidator {

	void validateSettleDate(Transaction transaction) throws ValidationException;
}
