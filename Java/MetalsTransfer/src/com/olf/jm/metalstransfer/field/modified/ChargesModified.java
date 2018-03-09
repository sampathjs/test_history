package com.olf.jm.metalstransfer.field.modified;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldEventListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Field even notification plugin for 'Charges' tran info field.
 * <ol>
 * <li>Set the 'Charge (in USD)' field to zero if 'Charges' field is 'no'.</li>
 * </ol>
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 23-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.EventNotification })
public class ChargesModified extends AbstractFieldEventListener {

    @Override
    public String getValue(Session session, Field field, String oldValue, String newValue) {
        Transaction tran = null;
        try {
            Logging.init(session, this.getClass(), "MetalsTransfer", "UI");
            tran = field.getTransaction();
            Logging.info("Processing transaction " + tran.getTransactionId());
            if ("no".equalsIgnoreCase(newValue)) {
                Field charge = tran.getField("Charge (in USD)");
                charge.setValue("0");
            }
            Logging.info("Completed transaction " + tran.getTransactionId());
            return newValue;
        }
        catch (RuntimeException e) {
            Logging.error("Process failed for transaction " + tran.getTransactionId() + ": ", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

}
