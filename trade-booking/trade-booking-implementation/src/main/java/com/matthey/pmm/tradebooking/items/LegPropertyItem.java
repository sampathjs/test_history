package com.matthey.pmm.tradebooking.items;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.matthey.pmm.tradebooking.LegTo;
import com.matthey.pmm.tradebooking.PropertyTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LegPropertyItem extends TransactionItem<PropertyTo, LegTo, Transaction, Transaction> {
    private static Logger logger = null;

	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(LegPropertyItem.class);
		}
		return logger;
	}
    
    @Builder
    public LegPropertyItem(int order, PropertyTo property, LegTo leg, Session ocSession, LogTable logTable) {
        super(order, property, leg, ocSession, logTable, Transaction.class);
    }

    @Override
    public Transaction apply(Transaction input) {
        getLogger().info("Processing command #" + order + " - setting a leg value");
        getLogger().info("On Leg #'" + context.getLegId() + " setting the field '" + item.getName() + "' to new value '" + item.getValue() + "'");
        while (input.getLegCount() < context.getLegId()) {
            try {
                Leg newLeg = input.getLegs().addItem();
                getLogger().info("Successfully added a new leg to the new transaction.");
            } catch (Exception ex) {
                String errorMsg = "Error while adding new leg to transaction: ";
                logException(ex, getLogger(), errorMsg);
                throw ex;
            }
        }
        Leg leg = input.getLeg(context.getLegId());
        Field field = leg.getField(item.getName());
        if (field == null) {
            String errorMsg = "The leg field '" + item.getName() + "' was not found.";
            getLogger().error(errorMsg);
            logTable.addLogEntry(order, false, errorMsg);
            throw new RuntimeException(errorMsg);
        }
        try {
            field.setValue(item.getValue());
        } catch (Exception ex) {
            logException(ex, getLogger(), "Could not set leg field '" + item.getName() + "' to value '" + item.getValue() + "' on leg #" + context.getLegId());
            throw ex;
        }
        String msg = "Successfully set On Leg #'" + context.getLegId() + " the field '" + item.getName() + "' to new value '" + item.getValue() + "'";
        getLogger().info(msg);
        logTable.addLogEntry(order, true, msg);
        return input;
    }

    @Override
    public String toString() {
        return "Set field '" + item.getName() + "' on leg #" + context.getLegId() + " to value '" + item.getValue() + "'";
    }
}
