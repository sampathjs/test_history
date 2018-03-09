package com.olf.jm.stocktake.processor;


/**
 * The Class StockTakeException. Errors detected during the processing of 
 * stocktake changes.
 */
public class StockTakeException extends Exception {

    /**
     * Auto generated serial ID.
     */
    private static final long serialVersionUID = -837821616487434047L;

    /**
     * Instantiates a new stock take exception.
     *
     * @param errorMessage the error message
     */
    public StockTakeException(final String errorMessage) {
        super(errorMessage);
    }
}
