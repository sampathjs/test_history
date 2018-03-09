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
 * Field notification that ensures the maturity date of deal is in sync with the settle date, sets the maturity date to be 3eom of settle
 * date.
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 17-Dec-2015 |               | G. Moore        | Decouple from trade date.                                                       |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class SettleDateModified extends AbstractTransactionListener {

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
        Date settleDate = tran.getField(EnumTransactionFieldId.SettleDate).getValueAsDate();
        Date maturityDate = context.getCalendarFactory().createSymbolicDate("3eom").evaluate(settleDate);
        for (Leg leg : tran.getLegs()) {
            Logging.info("Transaction %1$d: Setting maturity date (%2$td-%2$tb-%2$tY) on leg %3$d to 3eom of settle date (%4$td-%4$tb-%4$tY)",
                    tran.getTransactionId(), maturityDate, leg.getLegNumber(), settleDate);
            leg.setValue(EnumLegFieldId.MaturityDate, maturityDate);
        }
    }
}
