package com.matthey.pmm.transaction.items;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.PropertyTo;
import com.matthey.pmm.transaction.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

import lombok.Builder;

public class TransactionPropertyItem extends TransactionItem<PropertyTo, TransactionTo, Transaction, Transaction> {
    private static final Logger logger = LogManager.getLogger(TransactionProcessingItem.class);

    @Builder
    public TransactionPropertyItem(int order, PropertyTo property, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, property, transaction, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
		logger.info("Processing command '" + order + "' - Setting a transaction level field");
		Field field = input.getField(item.getName());
		if (field == null) {
			String errorMsg ="The field '"  + item.getName() + "' was not found on the transaction";
			logger.error(errorMsg);	
			logTable.addLogEntry(order, false, errorMsg);
			throw new RuntimeException(errorMsg);
		}
		try {
			field.setValue(item.getValue());			
		} catch (Exception ex) {
			logException(ex, logger, "Could not set tran field '" + item.getName() + "' to value '" + item.getValue() + "'");
			throw ex;
		}
		String msg = "Successfully set tranField '" + item.getName() + "' to new value '" + item.getValue() + "'";
		logger.info(msg);
		return input;
    }

	public String toString() {
		return "Set transaction field '" + item.getName() + "' to value '" +  item.getValue() + "'";
	}

}
