package com.matthey.pmm.tradebooking.items;

import com.matthey.pmm.tradebooking.InitializationByCloneTo;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.matthey.pmm.tradebooking.processors.LogTable;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InitializationByCloneItem extends TransactionItem<InitializationByCloneTo, TransactionTo, Void, Transaction> {
    private static Logger logger = null;

    private static Logger getLogger() {
        if (logger == null) {
            logger = LogManager.getLogger(InitializationByCloneItem.class);
        }
        return logger;
    }

    @Builder
    public InitializationByCloneItem(int order, InitializationByCloneTo initializationByClone, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, initializationByClone, transaction, ocSession, logTable, Void.class);
    }

    @Override
    public Transaction apply(Void input) {
        getLogger().info("Processing command #" + order + " - init by cloning existing transaction");
        int tranNum = Integer.parseInt(item.getDealId());
        String sql = "SELECT tran_num FROM ab_tran WHERE deal_tracking_num = " + item.getDealId() + " AND current_flag = 1";
        try (Table sqlResult = ocSession.getIOFactory().runSQL(sql)) {
            if (sqlResult.getRowCount() == 0) {
                String errorMessage = "Could not find the source transaction having deal tracking #" + item.getDealId();
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception ex) {
            logException(ex, getLogger(), "Error while executing SQL '" + sql + "': ");
            throw ex;
        }
        try {
            Transaction cloned = ocSession.getTradingFactory().cloneTransaction(tranNum);
            String message = "New transaction cloned from existing transaction #" + tranNum + " successfully";
            logTable.addLogEntry(order, true, message);
            getLogger().info(message);
            return cloned;
        } catch (Exception ex) {
            logException(ex, getLogger(), "Error while cloning from transaction #" + tranNum + ": ");
            throw ex;
        }
    }

    @Override
    public String toString() {
        return "Init Transaction By Clone From Tran #" + item.getDealId();
    }
}