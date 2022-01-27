package com.matthey.pmm.tradebooking.items;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.tradebooking.DebugShowTo;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Transaction;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugItem extends TransactionItem<DebugShowTo, TransactionTo, Transaction, Transaction> {
    private static Logger logger = null;
    
	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(TransactionItem.class);
		}
		return logger;
	}

    @Builder
    public DebugItem(int order, DebugShowTo debugShowTo, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, debugShowTo, transaction, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
        getLogger().info("Processing command #" + order + " - showing transaction to user");
        boolean success = ocSession.getTradingFactory().viewTransaction(input);
        if (success) {
            String msg = "Successfully showed transaction to user";
            getLogger().info(msg);
            logTable.addLogEntry(order, true, msg);
        } else {
            String msg = "Failed to show transaction to user";
            getLogger().error(msg);
            logTable.addLogEntry(order, false, msg);
        }
        return input;
    }

    @Override
    public String toString() {
        return "showTransactionToUser";
    }
}
