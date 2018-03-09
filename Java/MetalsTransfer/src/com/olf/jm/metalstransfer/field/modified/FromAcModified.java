package com.olf.jm.metalstransfer.field.modified;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.field.picklist.BusinessUnitPickListFilter;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin for 'From A/C' tran info field.
 * <ol>
 * <li>Clears the 'From A/C BU' field.</li>
 * <li>Clears the 'Metal' field.</li>
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
public class FromAcModified extends AbstractTransactionListener {

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
        // Set or clear the account business unit field
        String accountName = tran.getField("From A/C").getValueAsString();
        ReferenceChoices choices = new BusinessUnitPickListFilter().retrieveOptions(context, accountName);
        Field bu = tran.getField("From A/C BU");
        // If there is only one choice set the account BU field to that choice
        if (choices.size() == 1) {
            bu.setValue(choices.get(0).getName());
        }
        else {
            bu.setValue("");
        }
    }
}
