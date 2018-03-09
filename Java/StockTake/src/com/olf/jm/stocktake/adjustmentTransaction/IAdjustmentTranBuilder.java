package com.olf.jm.stocktake.adjustmentTransaction;

import com.olf.jm.stocktake.processor.clientData.StockTakeTransferData;
import com.olf.openrisk.trading.Transaction;


/**
 * The Interface IAdjustmentTranBuilder. 
 */
public interface IAdjustmentTranBuilder {

    /**
     * Gets the default transaction to which the adjustment data can be added.
     *
     * @return the default transaction
     */
    Transaction getDefaultTran();
    
    /**
     * Sets the adjustment data on the initialised transaction.
     *
     * @param tranToPopulate the transaction to applied the adjustment data to.
     * @param adjustment the adjustment date captured in the pre process.
     */
    void setAdjustmentData(Transaction tranToPopulate, StockTakeTransferData adjustment);
}
