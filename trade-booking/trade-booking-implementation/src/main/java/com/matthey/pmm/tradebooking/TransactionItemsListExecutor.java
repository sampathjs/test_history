package com.matthey.pmm.tradebooking;

import com.matthey.pmm.tradebooking.items.TransactionItem;
import com.olf.openrisk.trading.Transaction;

import lombok.val;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class TransactionItemsListExecutor implements Function<List<? extends TransactionItem<?, ?, ?, ?>>, Transaction> {

    @Override
    public Transaction apply(List<? extends TransactionItem<?, ?, ?, ?>> transactionItems) {

        val ar = new AtomicReference<Object>(null);

        transactionItems.forEach(ti -> {
            val result = ti.execute(ar.get());
            ar.set(result);
        });

        return (Transaction) ar.get();
    }
}
