package com.matthey.pmm.transaction;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.items.*;
import com.olf.openrisk.application.Session;
import lombok.val;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TransactionConverter implements BiFunction<Session, TransactionTo, List<? extends TransactionItem<?, ?, ?, ?>>> {	
    private static final Function<Object, Integer> toIntegerGlobalOrder = o -> {
        if (o instanceof String) {
            val s = (String) o;
            if ("MIN".equals(s))
                return -1;
            else if ("MAX".equals(s))
                return Integer.MAX_VALUE;
            else throw new IllegalArgumentException("unknown order token " + s);
        }
        if (o instanceof Integer)
            return (Integer) o;
        throw new IllegalArgumentException("unknown order token " + o);
    };

    private static final Comparator<TransactionItem<?, ?, ?, ?>> TRANSACTION_ITEM_COMPARATOR = (ti1, ti2) -> {
        if (Integer.MIN_VALUE == ti1.order()) return -1;
        if (Integer.MIN_VALUE == ti2.order()) return 1;
        if (Integer.MAX_VALUE == ti1.order()) return 1;
        if (Integer.MAX_VALUE == ti2.order()) return -1;
        return ti1.order() - ti2.order();
    };
    
    private final LogTable logTable;
    
    public TransactionConverter (final LogTable logTable) {
    	this.logTable = logTable;
    }

    @Override
    public List<? extends TransactionItem<?, ?, ?, ?>> apply(Session Session, TransactionTo transaction) {

        val result = new LinkedList<TransactionItem<?, ?, ?, ?>>();

        result.addAll(buildTransactionPropertyTransactionItems(Session, transaction));

        result.addAll(buildLegPropertyTransactionItems(Session, transaction));

        result.addAll(buildResetPropertyTransactionItems(Session, transaction));

        result.addAll(buildInitializationProcessingInstructions(Session, transaction));

        result.addAll(buildDebugProcessingInstructions(Session, transaction));

        result.addAll(buildTransactionProcessingInstructions(Session, transaction));

        result.sort(TRANSACTION_ITEM_COMPARATOR);
        
        logTable.init(result);
        return result;
    }

    private List<TransactionItem<?, ?, ?, ?>> buildResetPropertyTransactionItems(Session session, TransactionTo transaction) {
        val propertyItems = new LinkedList<TransactionItem<?, ?, ?, ?>>();

        return propertyItems;
    }

    private List<TransactionItem<?, ?, ?, ?>> buildLegPropertyTransactionItems(Session session, TransactionTo transaction) {
        val propertyItems = new LinkedList<TransactionItem<?, ?, ?, ?>>();
        transaction.getLegs().forEach(leg -> {
            leg.getLegProperties().forEach(p -> 
                    propertyItems.add(LegPropertyItem.builder().property(p).leg(leg).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder.apply(p.getGlobalOrderId())).build()));
            leg.getResetProperties().forEach(reset -> {
                    propertyItems.add(ResetPropertyItem.builder().property(reset).leg(leg).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder.apply(reset.getGlobalOrderId())).build());
            });
        });
        return propertyItems;
    }

    private List<TransactionItem<?, ?, ?, ?>> buildTransactionPropertyTransactionItems(Session session, TransactionTo transaction) {
        val propertyItems = new LinkedList<TransactionItem<?, ?, ?, ?>>();
        transaction.getTransactionProperties().forEach(p ->
                propertyItems.add(TransactionPropertyItem.builder().property(p).transaction(transaction).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder.apply(p.getGlobalOrderId())).build())
        );
        return propertyItems;
    }

    private List<TransactionItem<?, ?, ?, ?>> buildTransactionProcessingInstructions(Session session, TransactionTo transaction) {
        val processingTransactionItems = new LinkedList<TransactionItem<?, ?, ?, ?>>();
        val transactionProcessing = transaction.getProcessingInstruction().getTransactionProcessing();
        if (transactionProcessing == null)
            throw new IllegalStateException("no transaction processing instruction defined");
        transactionProcessing.forEach(tp ->
                processingTransactionItems.add(TransactionProcessingItem.builder().transactionProcessing(tp).transaction(transaction).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder.apply(tp.getGlobalOrderId())).build())
        );
        return processingTransactionItems;
    }

    private List<TransactionItem<?, ?, ?, ?>> buildDebugProcessingInstructions(Session session, TransactionTo transaction) {
        val debugTransactionItems = new LinkedList<TransactionItem<?, ?, ?, ?>>();
        val debugProcessingInstructions = transaction.getProcessingInstruction().getDebugDefinition();
        if (debugProcessingInstructions != null)
            debugProcessingInstructions.forEach(dd ->
                    debugTransactionItems.add(DebugItem.builder().debugDefinition(dd).transaction(transaction).ocSession(session).logTable(logTable).order(toIntegerGlobalOrder.apply(dd.getGlobalOrderId())).build())
            );
        return debugTransactionItems;
    }

    private List<TransactionItem<?, ?, ?, ?>> buildInitializationProcessingInstructions(Session session, TransactionTo transaction) {
        val initializationTransactionItems = new LinkedList<TransactionItem<?, ?, ?, ?>>();
        val initialization = transaction.getProcessingInstruction().getInitialization();
        if (initialization != null) {
            val byTemplate = initialization.getByTemplate();
            val byClone = initialization.getByClone();
            if (byTemplate != null && byClone != null)
                throw new IllegalStateException("only by_template or by_clone must be configured");
            if (byTemplate != null)
                initializationTransactionItems.add(
                        InitializationByTemplateItem.builder().initializationByTemplate(byTemplate).transaction(transaction).logTable(logTable).ocSession(session).order(Integer.MIN_VALUE).build()
                );
            if (byClone != null)
                initializationTransactionItems.add(
                        InitializationByCloneItem.builder().initializationByClone(byClone).transaction(transaction).logTable(logTable).ocSession(session).order(Integer.MIN_VALUE).build()
                );
        }
        return initializationTransactionItems;
    }
}
