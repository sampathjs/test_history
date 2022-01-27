package com.matthey.pmm.tradebooking;

import com.matthey.pmm.tradebooking.items.*;
import com.matthey.pmm.tradebooking.items.TransactionItemsUtils.OrderingState;
import com.matthey.pmm.tradebooking.processors.LogTable;
import com.olf.openrisk.application.Session;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class TransactionConverter implements BiFunction<Session, TransactionTo, List<? extends TransactionItem<?, ?, ?, ?>>> {
    private static Logger logger = null;

    private final LogTable logTable;

    private static Logger getLogger() {
        if (logger == null) {
            logger = LogManager.getLogger(TransactionConverter.class);
        }
        return logger;
    }

    public TransactionConverter(final LogTable logTable) {
        this.logTable = logTable;
    }

    @Override
    public List<? extends TransactionItem<?, ?, ?, ?>> apply(Session session, TransactionTo transaction) {

        val orderingState = TransactionItemsUtils.initializeOrderingState(transaction);

        List<TransactionItem<?, ?, ?, ?>> result = new ArrayList<>(orderingState.total());

        buildInitializationProcessingInstructions(session, transaction, orderingState, result);

        buildTransactionPropertyTransactionItems(session, transaction, orderingState, result);

        buildLegPropertyTransactionItems(session, transaction, orderingState, result);

        buildDebugProcessingInstructions(session, transaction, orderingState, result);

        buildTransactionProcessingInstructions(session, transaction, orderingState, result);

        val monotonicallyIncreasingOrderingResult =
                TransactionItemsUtils.ensureMonotonicallyIncreasingOrder(result);

        logTable.init(monotonicallyIncreasingOrderingResult);
        return monotonicallyIncreasingOrderingResult;
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


    private void buildLegPropertyTransactionItems(Session session, TransactionTo transaction, OrderingState orderingState,
                                                  List<TransactionItem<?, ?, ?, ?>> result) {
        transaction.getLegs().forEach(leg -> {
            leg.getLegProperties().forEach(p ->
                    result.add(LegPropertyItem.builder().property(p).leg(leg).ocSession(session).logTable(logTable)
                            .order(TransactionItemsUtils.toGlobalOrder(p.getGlobalOrderId(), orderingState)).build()));
            leg.getResetProperties().forEach(reset -> {
                result.add(ResetPropertyItem.builder().property(reset).leg(leg).ocSession(session).logTable(logTable)
                        .order(TransactionItemsUtils.toGlobalOrder(reset.getGlobalOrderId(), orderingState)).build());
            });
        });
    }

    private void buildTransactionPropertyTransactionItems(Session session, TransactionTo transaction, OrderingState orderingState,
                                                          List<TransactionItem<?, ?, ?, ?>> result) {
        transaction.getTransactionProperties().forEach(p ->
                result.add(TransactionPropertyItem.builder().property(p).transaction(transaction).ocSession(session).logTable(logTable)
                        .order(TransactionItemsUtils.toGlobalOrder(p.getGlobalOrderId(), orderingState)).build())
        );
    }

    private void buildTransactionProcessingInstructions(Session session, TransactionTo transaction, OrderingState orderingState,
                                                        List<TransactionItem<?, ?, ?, ?>> result) {
        val transactionProcessing = transaction.getProcessingInstruction().getTransactionProcessing();
        if (transactionProcessing == null)
            throw new IllegalStateException("No transaction processing instruction defined. Exactly one transaction processing instruction is required.");
        transactionProcessing.forEach(tp ->
                result.add(
                        TransactionProcessingItem.builder().transactionProcessing(tp).transaction(transaction).ocSession(session)
                                .logTable(logTable).order(TransactionItemsUtils.toGlobalOrder(tp.getGlobalOrderId(), orderingState)).build())
        );
    }

    private void buildDebugProcessingInstructions(Session session, TransactionTo transaction, OrderingState orderingState,
                                                  List<TransactionItem<?, ?, ?, ?>> result) {
        val debugProcessingInstructions = transaction.getProcessingInstruction().getDebugShow();
        if (debugProcessingInstructions != null)
            debugProcessingInstructions.forEach(ds ->
                    result.add(DebugItem.builder().debugShowTo(ds).transaction(transaction).ocSession(session)
                            .logTable(logTable).order(TransactionItemsUtils.toGlobalOrder(ds.getGlobalOrderId(), orderingState)).build())
            );
    }

    private void buildInitializationProcessingInstructions(Session session, TransactionTo transaction, OrderingState orderingState, List<TransactionItem<?, ?, ?, ?>> result) {
        val initialization = transaction.getProcessingInstruction().getInitialization();
        if (initialization != null) {
            val byTemplate = initialization.getByTemplate();
            val byClone = initialization.getByClone();
            if (byTemplate != null && byClone != null)
                throw new IllegalStateException("One of by_template or by_clone but not both must be configured");
            if (byTemplate != null)
                result.add(
                        InitializationByTemplateItem.builder().initializationByTemplate(byTemplate).transaction(transaction).logTable(logTable)
                                .ocSession(session).order(TransactionItemsUtils.toGlobalOrder(0, orderingState)).build()
                );
            if (byClone != null)
                result.add(
                        InitializationByCloneItem.builder().initializationByClone(byClone).transaction(transaction).logTable(logTable)
                                .ocSession(session).order(TransactionItemsUtils.toGlobalOrder(0, orderingState)).build()
                );
        }
    }
}