package com.matthey.pmm.transaction.items;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.DebugDefinitionTo;
import com.matthey.pmm.transaction.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Transaction;

import lombok.Builder;

public class DebugItem extends TransactionItem<DebugDefinitionTo, TransactionTo, Transaction, Transaction> {
    private static final Logger logger = LogManager.getLogger(TransactionItem.class);

    @Builder
    public DebugItem(int order, DebugDefinitionTo debugDefinition, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, debugDefinition, transaction, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
		logger.info("Showing new transaction to user");
		boolean success = ocSession.getTradingFactory().viewTransaction(input);
		if (success) {
			String msg = "Successfully showed transaction to user";
			logger.info(msg);
			logTable.addLogEntry(order, true, "");
		} else {
			String msg = "Failed to show transaction to user";
			logger.info(msg);
			logTable.addLogEntry(order, false, msg);
		}
		return input;
    }

	@Override
	public String toString() {
		return "showTransactionToUser";
	}
}
