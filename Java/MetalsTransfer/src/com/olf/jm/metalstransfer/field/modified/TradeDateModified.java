package com.olf.jm.metalstransfer.field.modified;

import java.util.Date;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification that ensures the start date of deal is in sync with the trade date.
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 17-Dec-2015 |               | G. Moore        | Decouple from settle date.                                                      |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class TradeDateModified extends AbstractTransactionListener {

    @Override
    public void notify(Context context, Transaction tran) {
        try {
            Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
            Logging.info("Processing transaction " + tran.getTransactionId());
            process(context, tran);
            Logging.info("Completed transaction " + tran.getTransactionId());
        }
        catch (RuntimeException e) {
            Logging.error("Process failed for transaction " + tran.getTransactionId() + ": ", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }
    
    /**
     * Main processing method.
     * 
     * @param session
     * @param tran
     */
    private void process(Context context, Transaction tran) {
        Date tradeDate = tran.getField(EnumTransactionFieldId.TradeDate).getValueAsDate();
        for (Leg leg : tran.getLegs()) {
            Logging.info("Transaction %1$d: Setting start date on leg %2$d to match trade date of %3$td-%3$tb-%3$tY",
                    tran.getTransactionId(), leg.getLegNumber(), tradeDate);
            leg.setValue(EnumLegFieldId.StartDate, tradeDate);
        }
    }
}
