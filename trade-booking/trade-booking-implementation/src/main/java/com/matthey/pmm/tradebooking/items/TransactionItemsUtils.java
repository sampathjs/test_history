package com.matthey.pmm.tradebooking.items;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthey.pmm.tradebooking.TransactionTo;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@UtilityClass
public class TransactionItemsUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Builder
    @Data
    @Accessors(fluent = true)
    public static class OrderingState {
        Integer currentMin;
        Integer totalMin;
        Integer currentMax;
        Integer total;
    }

    public OrderingState initializeOrderingState(TransactionTo transactionTo) {

        try {
            val transactionJson = MAPPER.writeValueAsString(transactionTo);
            int countMin = StringUtils.countMatches(transactionJson, "\"global_order_id\":\"MIN\"");
            int countTotal = StringUtils.countMatches(transactionJson, "\"global_order_id\":");
            return OrderingState.builder()
                    // 1 because initialization item should always be there and should always be 0
                    .currentMin(1)
                    .totalMin(countMin)
                    .currentMax(1)
                    .total(countTotal).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not convert to a json string to initialize counters", e);
        }
    }

    public int toGlobalOrder(Object o, OrderingState orderingState) {

        if (o instanceof String) {
            val s = (String) o;
            if ("MIN".equals(s)) {
                val ret = orderingState.currentMin;
                orderingState.currentMin(ret + 1);
                return ret;
            } else if ("MAX".equals(s)) {
                val currentMax = orderingState.currentMax;
                int ret = orderingState.total - currentMax;
                orderingState.currentMax(currentMax + 1);
                return ret;
            } else throw new IllegalArgumentException("Unknown order token " + s + " in '" + o.toString() + "'");
        } else if (o instanceof Integer) {
            val orderFromJson = (Integer) o;
            // 0 is always initialization. we start at 1 in Json. all MINs will come afterwards
            return orderFromJson == 0 ? 0 : orderingState.totalMin + orderFromJson;
        } else throw new IllegalArgumentException("Unknown order token type " + o.getClass().getName());
    }

    public <T extends GloballyOrdered<?>> List<T> ensureMonotonicallyIncreasingOrder(List<T> transactionItems) {

        transactionItems.sort((ti1, ti2) -> ti1.order() - ti2.order());
        var order = 0;
        val iterator = transactionItems.iterator();
        while (iterator.hasNext()) {
            val transactionItem = iterator.next();
            transactionItem.order(order);
            order++;
        }
        return transactionItems;
    }
}
