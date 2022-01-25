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

public class ResetPropertyItem extends TransactionItem<PropertyTo, LegTo, Transaction, Transaction> {
    private static final Logger logger = LogManager.getLogger(ResetPropertyItem.class);

    @Builder
    public ResetPropertyItem(int order, PropertyTo property, LegTo leg, Session ocSession, LogTable logTable) {
        super(order, property, leg, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
        logger.info("Processing command #'" + order + "' - setting a reset definition field");
        while (input.getLegCount() < context.getLegId()) {
            try {
                Leg newLeg = input.getLegs().addItem();
                logger.info("Successfully added a new leg to the new transaction.");
            } catch (Exception ex) {
                super.logException(ex, logger, "Error while adding new leg to transaction: ");
                throw ex;
            }
        }
        Leg leg = input.getLeg(context.getLegId());
        Field field = leg.getResetDefinition().getField(item.getName());
        if (field == null) {
            String errorMsg = "The field '" + item.getName() + "' was not found on the reset definition.";
            logger.error(errorMsg);
            logTable.addLogEntry(order, false, errorMsg);
            throw new RuntimeException(errorMsg);
        }
        try {
            field.setValue(item.getValue());
        } catch (Exception ex) {
            logException(ex, logger, "Could not set reset definition field '" + item.getName() + "' to value '" + item.getValue() + "' on leg #" + context.getLegId());
            throw ex;
        }
        String msg = "Successfully set on Leg #'" + context.getLegId() + " the field '" + item.getName() + "' on the reset definition to new value '" + item.getValue() + "'";
        logger.info(msg);
        logTable.addLogEntry(order, true, msg);
        return input;
    }

    public String toString() {
        return "Set reset definition field '" + item.getName() + "' on leg #" + context.getLegId() + " to value '" + item.getValue() + "'";
    }
}
