package com.olf.jm.metalstransfer.field.modified;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.field.picklist.AccountPickListFilter;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin for 'To A/C Form' tran info field.
 * <ol>
 * <li>Clears the 'To A/C' field.</li>
 * </ol>
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class ToFormModified extends AbstractTransactionListener {

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
        // Set or clear the account field
        String loco = tran.getField("To A/C Loco").getValueAsString();
        String form = tran.getField("To A/C Form").getValueAsString();
        ReferenceChoices choices = new AccountPickListFilter().retrieveOptions(context, form, loco);
        Field account = tran.getField("To A/C");
        // If there is only one choice set the account field to that choice
        if (choices.size() == 1) {
            account.setValue(choices.get(0).getName());
        }
        else {
            account.setValue("");
        }
    }
}
