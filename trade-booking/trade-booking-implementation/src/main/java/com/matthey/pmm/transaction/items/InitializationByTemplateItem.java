package com.matthey.pmm.transaction.items;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.InitializationByTemplateTo;
import com.matthey.pmm.transaction.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;

import lombok.Builder;

public class InitializationByTemplateItem extends TransactionItem<InitializationByTemplateTo, TransactionTo, Void, Transaction> {
    private static final Logger logger = LogManager.getLogger(InitializationByCloneItem.class);

    @Builder
    public InitializationByTemplateItem(int order, InitializationByTemplateTo initializationByTemplate, TransactionTo transaction, Session ocSession, LogTable logTable) {
        super(order, initializationByTemplate, transaction, ocSession, logTable, Void.class);
    }

    @Override
    public Transaction apply(Void input) {
        String sql = "SELECT tran_num FROM ab_tran WHERE reference = '" + item.getTemplateReference() + "' AND current_flag = 1";
        int tranNum; 
        try (Table sqlResult = ocSession.getIOFactory().runSQL(sql)) {
        	if (sqlResult.getRowCount() == 0) {
        		String errorMessage = "Could not find the template transaction having reference '" + item.getTemplateReference() + "'"; 
        		throw new RuntimeException (errorMessage);
        	}
        	tranNum = sqlResult.getInt(0, 0);
        } catch (Exception ex) {
    		logException(ex, logger, "Error while executing SQL '" + sql + "': ");
    		throw ex;
        }
		try {
			Transaction cloned = ocSession.getTradingFactory().createTransactionFromTemplate(tranNum);
			logTable.addLogEntry(order, true, "");
			logger.info("New transaction created from templat #" + tranNum + " having reference '" + item.getTemplateReference() + "'successfully");
	        return cloned;
		} catch (Exception ex) {
			logException (ex, logger, "Error while creating transaction from template #" + tranNum + " having reference '" + item.getTemplateReference() + "' : ");
			throw ex;
		}
    }
}
