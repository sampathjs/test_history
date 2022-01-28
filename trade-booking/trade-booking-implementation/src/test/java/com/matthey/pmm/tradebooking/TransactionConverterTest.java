package com.matthey.pmm.tradebooking;

import com.matthey.pmm.tradebooking.items.GloballyOrdered;
import com.matthey.pmm.tradebooking.items.TransactionItemsUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.val;
import org.junit.Test;

import java.util.ArrayList;

public class TransactionConverterTest {

    @AllArgsConstructor
    @Data
    @Accessors(fluent = true)
    static class TestOrderd implements GloballyOrdered<TestOrderd> {

        int order;
        String reference;
    }

    @Test
    public void test() {

        TransactionTo tx = TestUtils.buildTransaction();

        val orderingState = TransactionItemsUtils.initializeOrderingState(tx);

        val orders = new Object[]{3, 3, 1, "MIN", 2, 4, "MIN", 6, 5, "MAX"};

        val testOrderedItems = new ArrayList<TestOrderd>();

        for (Object o : orders) {
            testOrderedItems.add(new TestOrderd(TransactionItemsUtils.toGlobalOrder(o, orderingState), "" + o));
        }

        val finalResult = TransactionItemsUtils.ensureMonotonicallyIncreasingOrder(testOrderedItems);

        finalResult.forEach(to -> System.out.println(to.toString()));

        System.out.println("---------");

//        LogTable mockLogTable = Mockito.mock(LogTable.class);       
//        Session mockSession = Mockito.mock(Session.class);
//        Mockito.when(mockSession.getIOFactory()).thenReturn(Mockito.mock(IOFactory.class));
//        
//        val result = new TransactionConverter(mockLogTable).apply(mockSession, tx);
//
//        result.forEach(ti -> System.out.println("" + ti + " " + ti.getClass().getName()));
//
//        new TransactionItemsListExecutor().apply(result);

    }

}