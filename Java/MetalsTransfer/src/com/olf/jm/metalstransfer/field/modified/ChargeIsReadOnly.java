package com.olf.jm.metalstransfer.field.modified;

import com.olf.embedded.trading.AbstractFieldEventListener;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Field even notification plugin for 'Charge (in USD)' tran info field.
 * <ol>
 * <li>Make the field read only if 'Charges' field is 'no'.</li>
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
public class ChargeIsReadOnly extends AbstractFieldEventListener {

    @Override
    public boolean isReadOnly(Session session, Field field, boolean isReadOnly) {
        Transaction tran = null;
        try {
            Logging.init(session, this.getClass(), "MetalsTransfer", "UI");
            tran = field.getTransaction();
            Logging.info("Processing transaction " + tran.getTransactionId());
            String charges = field.getTransaction().getField("Charges").getValueAsString();
            boolean returnValue = "no".equalsIgnoreCase(charges);
            Logging.info("Completed transaction " + tran.getTransactionId());
            return returnValue;
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
