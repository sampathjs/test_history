package com.matthey.pmm.transaction.items;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.transaction.LegTo;
import com.matthey.pmm.transaction.PropertyTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LegPropertyItem extends TransactionItem<PropertyTo, LegTo, Transaction, Transaction> {
    private static final Logger logger = LogManager.getLogger(LegPropertyItem.class);

    @Builder
    public LegPropertyItem(int order, PropertyTo property, LegTo leg, Session ocSession, LogTable logTable) {
        super(order, property, leg, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
        logger.info("Processing line '" + order + "' - setting a leg value");
        logger.info("On Leg #'" + context.getLegId() + " setting the field '" + item.getName() + "' to new value '" + item.getValue() + "'");
        while (input.getLegCount() < context.getLegId()) {
            try {
                Leg newLeg = input.getLegs().addItem();
                logger.info("Successfully added a new leg to the new transaction.");
            } catch (Exception ex) {
                String errorMsg = "Error while adding new leg to transaction: ";
                logException(ex, logger, errorMsg);
                throw ex;
            }
        }
        Leg leg = input.getLeg(context.getLegId());
        Field field = leg.getField(item.getName());
        if (field == null) {
            String errorMsg = "The leg field '" + item.getName() + "' was not found.";
            logger.error(errorMsg);
            logTable.addLogEntry(order, false, errorMsg);
            throw new RuntimeException(errorMsg);
        }
        try {
            field.setValue(item.getValue());
        } catch (Exception ex) {
            logException(ex, logger, "Could not set leg field '" + item.getName() + "' to value '" + item.getValue() + "' on leg #" + context.getLegId());
            throw ex;
        }
        String msg = "Successfully set On Leg #'" + context.getLegId() + " the field '" + item.getName() + "' to new value '" + item.getValue() + "'";
        logger.info(msg);
        logTable.addLogEntry(order, true, msg);
        return input;
    }

    @Override
    public String toString() {
        return "Set field '" + item.getName() + "' on leg #" + context.getLegId() + " to value '" + item.getValue() + "'";
    }
}
