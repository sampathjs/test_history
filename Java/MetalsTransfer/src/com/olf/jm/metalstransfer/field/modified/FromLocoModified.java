package com.olf.jm.metalstransfer.field.modified;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin for 'From A/C Loco' tran info field.
 * <ol>
 * <li>Sets the 'From A/C Form' field with the default form for the loco.</li>
 * <li>Clears the 'From A/C' field.</li>
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
 * | 002 | 21-Nov-2016 |               | S. Curran       | Force the reset of the Form by setting it to none first.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class FromLocoModified extends AbstractTransactionListener {

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

        Field fromLoco = tran.getField("From A/C Loco");
        String fromLocoValue = fromLoco.getValueAsString();
        
        // reset the form to None so that the logic below changes the field
        tran.getField("From A/C Form").setValue("None");
        
        // Get and set default form for loco 
        try (Table defaultValue = context.getIOFactory().runSQL(
                "\n SELECT default_form" +
                "\n   FROM USER_jm_loco" +
                "\n  WHERE loco_name = '" + fromLocoValue + "'")) {
            Field form = tran.getField("From A/C Form");
            if (defaultValue.getRowCount() > 0) {
                String value = defaultValue.getString(0, 0); 
                form.setValue(value.trim().isEmpty() ? "None" : value);
            }
            else {
                form.setValue("None");
            }
        }
    }
}
