package com.matthey.pmm.tradebooking;

import com.matthey.pmm.tradebooking.items.*;
import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.tradebooking.LegTo;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.olf.openrisk.application.Session;
import lombok.val;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionConverter implements BiFunction<Session, TransactionTo, List<? extends TransactionItem<?, ?, ?, ?>>> {
	private static Logger logger = null;
	
    private static final Comparator<TransactionItem<?, ?, ?, ?>> TRANSACTION_ITEM_COMPARATOR = (ti1, ti2) -> {
        if (Integer.MIN_VALUE == ti1.order()) return -1;
        if (Integer.MIN_VALUE == ti2.order()) return 1;
        if (Integer.MAX_VALUE == ti1.order()) return 1;
        if (Integer.MAX_VALUE == ti2.order()) return -1;
        return ti1.order() - ti2.order();
    };

    private final LogTable logTable;

	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(TransactionConverter.class);
		}
		return logger;
	}
    
    public TransactionConverter(final LogTable logTable) {
        this.logTable = logTable;
    }

    @Override
    public List<? extends TransactionItem<?, ?, ?, ?>> apply(Session Session, TransactionTo transaction) {
        int countOfItems = calculateCountOfItems(transaction);

        val result = new ArrayList<TransactionItem<?, ?, ?, ?>>(countOfItems);

        buildInitializationProcessingInstructions(Session, transaction, countOfItems, result);

        buildTransactionPropertyTransactionItems(Session, transaction, countOfItems, result);

        buildLegPropertyTransactionItems(Session, transaction, countOfItems, result);

        buildDebugProcessingInstructions(Session, transaction, countOfItems, result);

        buildTransactionProcessingInstructions(Session, transaction, countOfItems, result);
        result.sort(TRANSACTION_ITEM_COMPARATOR);
        
        logTable.init(result);
        return result;
    }

    private int calculateCountOfItems(TransactionTo transaction) {
        int itemCount = 0;
        for (LegTo leg : transaction.getLegs()) {
            itemCount += leg.getLegProperties().size();
            itemCount += leg.getResetProperties().size();
        }
        itemCount += transaction.getProcessingInstruction().getDebugShow().size();
        itemCount += transaction.getProcessingInstruction().getTransactionProcessing().size();
        itemCount += 1; // initialisation
        itemCount += transaction.getTransactionProperties().size();
        return itemCount;
    }


    private void buildLegPropertyTransactionItems(Session session, TransactionTo transaction, int countOfItems, ArrayList<TransactionItem<?, ?, ?, ?>> result) {
        transaction.getLegs().forEach(leg -> {
            leg.getLegProperties().forEach(p ->
                    result.add(LegPropertyItem.builder().property(p).leg(leg).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder(p.getGlobalOrderId(), countOfItems, result)).build()));
            leg.getResetProperties().forEach(reset -> {
                result.add(ResetPropertyItem.builder().property(reset).leg(leg).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder(reset.getGlobalOrderId(), countOfItems, result)).build());
            });
        });
    }

    private void buildTransactionPropertyTransactionItems(Session session, TransactionTo transaction, int countOfItems, ArrayList<TransactionItem<?, ?, ?, ?>> result) {
        transaction.getTransactionProperties().forEach(p ->
                result.add(TransactionPropertyItem.builder().property(p).transaction(transaction).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder(p.getGlobalOrderId(), countOfItems, result)).build())
        );
    }

    private void buildTransactionProcessingInstructions(Session session, TransactionTo transaction, int countOfItems, ArrayList<TransactionItem<?, ?, ?, ?>> result) {
        val transactionProcessing = transaction.getProcessingInstruction().getTransactionProcessing();
        if (transactionProcessing == null)
            throw new IllegalStateException("No transaction processing instruction defined. Exactly one transaction processing instruction is required.");
        transactionProcessing.forEach(tp ->
                result.add(TransactionProcessingItem.builder().transactionProcessing(tp).transaction(transaction).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder(countOfItems-1, countOfItems, result)).build())
        );
    }

    private void buildDebugProcessingInstructions(Session session, TransactionTo transaction, int countOfItems, ArrayList<TransactionItem<?, ?, ?, ?>> result) {
        val debugProcessingInstructions = transaction.getProcessingInstruction().getDebugShow();
        if (debugProcessingInstructions != null)
            debugProcessingInstructions.forEach(ds ->
                    result.add(DebugItem.builder().debugShowTo(ds).transaction(transaction).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder(ds.getGlobalOrderId(), countOfItems, result)).build())
            );
    }

    private void buildInitializationProcessingInstructions(Session session, TransactionTo transaction, int countOfItems, ArrayList<TransactionItem<?, ?, ?, ?>> result) {
        val initialization = transaction.getProcessingInstruction().getInitialization();
        if (initialization != null) {
            val byTemplate = initialization.getByTemplate();
            val byClone = initialization.getByClone();
            if (byTemplate != null && byClone != null)
                throw new IllegalStateException("One of by_template or by_clone but not both must be configured");
            if (byTemplate != null)
                result.add(
                        InitializationByTemplateItem.builder().initializationByTemplate(byTemplate).transaction(transaction).logTable(logTable)
                                .ocSession(session).order(toIntegerGlobalOrder(0, countOfItems, result)).build()
                );
            if (byClone != null)
                result.add(
                        InitializationByCloneItem.builder().initializationByClone(byClone).transaction(transaction).logTable(logTable)
                                .ocSession(session).order(toIntegerGlobalOrder(0, countOfItems, result)).build()
                );
        }
    }

    int toIntegerGlobalOrder(Object o, int countOfItems, ArrayList<TransactionItem<?, ?, ?, ?>> result) {
        result.sort(TRANSACTION_ITEM_COMPARATOR);
        int ret = 0;
        if (o instanceof String) {
            val s = (String) o;
            if ("MIN".equals(s))
                ret = 0;
            else if ("MAX".equals(s))
                ret = countOfItems;
            else throw new IllegalArgumentException("Unknown order token " + s + " in '" + o.toString() + "'");
        } else if (o instanceof Integer)
            ret = (Integer) o;
        if (ret >= countOfItems) {
        	throw new IllegalArgumentException ("Global Order ID " + ret + " in '" + o.toString() + "' is greater than count of items (" + countOfItems + ").");
        }
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).order() <= ret) {
                continue;
            }
            if (result.get(i).order() == ret + 1) {
                ret++;
                break;
            }
        }
        getLogger().info("Action Item '" + o.toString() + "' is going to have order id #" + ret);
        return ret;
    }
}
