package com.matthey.pmm.transaction.items;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.TransactionProcessingTo;
import com.matthey.pmm.transaction.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;

import lombok.Builder;

public class TransactionProcessingItem extends TransactionItem<TransactionProcessingTo, TransactionTo, Transaction, Transaction> {
    private static final Logger logger = LogManager.getLogger(TransactionProcessingItem.class);

    @Builder
    public TransactionProcessingItem(int order, TransactionProcessingTo transactionProcessing, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, transactionProcessing, transaction, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
		logger.info("Processing command '" + order + "' categorised as processing (the deal to be booked) to a new status");
		EnumTranStatus newStatusEnum;
		try {
			newStatusEnum = EnumTranStatus.valueOf(item.getStatus());
		} catch (IllegalArgumentException ex) {
			logger.error("The tran status '" + item.getStatus() + "' is not valid. Allowed values are " + EnumTranStatus.values());
			throw ex;
		}
		try {
			input.process(newStatusEnum);			
		} catch (OpenRiskException ex) {
			String errorMsg = "Error while processing transaction to status '" + item.getStatus() + "': " + ex.toString() + "\n";
			logException(ex, logger, errorMsg);
			throw ex;
		}
		String msg = "Successfully processed deal to new status '" + item.getStatus() + "'";
		logger.info(msg);
		return input;
    }
}
