package com.matthey.pmm.transaction.items;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.olf.openrisk.application.Session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public abstract class TransactionItem<T, C, I, O> implements Function<I, O> {		
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
    
    protected void logException (Throwable ex, Logger logger, String headerMessage) {
		logger.error(headerMessage + ex.toString());
		StringWriter sw = new StringWriter(4000);
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		logger.error(sw.toString());
		logTable.addLogEntry(order, false, headerMessage + ex.toString());
    }
}
