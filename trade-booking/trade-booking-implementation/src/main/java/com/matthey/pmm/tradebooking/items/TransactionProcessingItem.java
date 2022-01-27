package com.matthey.pmm.tradebooking.items;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.tradebooking.TransactionProcessingTo;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionProcessingItem extends TransactionItem<TransactionProcessingTo, TransactionTo, Transaction, Transaction> {
    private static Logger logger = null;

	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(TransactionProcessingItem.class);
		}
		return logger;
	}
    
    @Builder
    public TransactionProcessingItem(int order, TransactionProcessingTo transactionProcessing, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, transactionProcessing, transaction, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
        getLogger().info("Processing command #" + order + " categorised as processing (the deal to be booked) to a new status");
        EnumTranStatus newStatusEnum;
        try {
            newStatusEnum = EnumTranStatus.valueOf(item.getStatus());
        } catch (IllegalArgumentException ex) {
        	String errorMsg = "The tran status '" + item.getStatus() + "' is not valid. Allowed values are " + EnumTranStatus.values() + ": ";
            logException(ex, getLogger(), errorMsg);
            throw ex;
        }
        try {
            input.process(newStatusEnum);
        } catch (OpenRiskException ex) {
            String errorMsg = "Error while processing transaction to status '" + item.getStatus() + "': " + ex.toString() + "\n";
            logException(ex, getLogger(), errorMsg);
            throw ex;
        }
        String msg = "Successfully processed deal to new status '" + item.getStatus() + "'";
        getLogger().info(msg);
        logTable.addLogEntry(order, true, msg);
        return input;
    }

    public String toString() {
        return "Process transaction to status '" + item.getStatus() + "'";
    }
}
