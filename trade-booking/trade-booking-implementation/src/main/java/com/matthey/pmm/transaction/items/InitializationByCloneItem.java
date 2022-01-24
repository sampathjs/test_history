package com.matthey.pmm.transaction.items;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.InitializationByCloneTo;
import com.matthey.pmm.transaction.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;

import lombok.Builder;

public class InitializationByCloneItem extends TransactionItem<InitializationByCloneTo, TransactionTo, Void, Transaction> {
    private static final Logger logger = LogManager.getLogger(InitializationByCloneItem.class);
	
    @Builder
    public InitializationByCloneItem(int order, InitializationByCloneTo initializationByClone, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, initializationByClone, transaction, ocSession, logTable, Void.class);
    }

    @Override
    public Transaction apply(Void input) {
        int tranNum = Integer.parseInt(item.getDealId());
        String sql = "SELECT tran_num FROM ab_tran WHERE deal_tracking_num = " + item.getDealId() + " AND current_flag = 1";
        try (Table sqlResult = ocSession.getIOFactory().runSQL(sql)) {
        	if (sqlResult.getRowCount() == 0) {
        		String errorMessage = "Could not find the source transaction having deal tracking #" + item.getDealId();
        		throw new RuntimeException (errorMessage);
        	}
        } catch (Exception ex) {
    		logException(ex, logger, "Error while executing SQL '" + sql + "': ");
    		throw ex;
        }
		try {
			Transaction cloned = ocSession.getTradingFactory().cloneTransaction(tranNum);
			logTable.addLogEntry(order, true, "");
			logger.info("New transaction cloned from existing transaction #" + tranNum + " successfully");
	        return cloned;
		} catch (Exception ex) {
			logException (ex, logger, "Error while cloning from transaction #" + tranNum + ": ");
			throw ex;
		}
    }
}