package com.matthey.pmm.tradebooking.items;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public abstract class TransactionItem<T, C, I, O> implements Function<I, O>, GloballyOrdered<TransactionItem<T, C, I, O>> {

    @Setter
    int order;
    protected T item;
    protected C context;
    protected Session ocSession;
    protected LogTable logTable;

    private Class<I> inputClass;

    public abstract O apply(I input);

    public O execute(Object input) {
        if (Void.class.equals(inputClass) || (input != null && inputClass.isInstance(input))) {
            return apply((I) input);
        }
        throw new IllegalArgumentException("incompatible argument. Expected " + inputClass.getName() + ", got " + input.getClass().getName());
    }

    protected void logException(Throwable ex, Logger logger, String headerMessage) {
        logger.error(headerMessage + ex.toString());
        StringWriter sw = new StringWriter(4000);
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        logger.error(sw.toString());
        logTable.addLogEntry(order, false, headerMessage + ex.toString());
    }
    
    protected void ensureLegCount (Transaction tran, int maxLegId, Logger logger) {
        while (tran.getLegCount() < maxLegId + 1) { // + 1 because the first leg has ID #0
        	logger.info("Leg count of #" + tran.getLegCount() + " is insufficient - adding one more leg");
            try {
                Leg newLeg = tran.getLegs().addItem();
                logger.info("Successfully added a new leg to the new transaction.");
            } catch (Exception ex) {
                logException(ex, logger, "Error while adding new leg to transaction: ");
                throw ex;
            }
        }
    }
}
