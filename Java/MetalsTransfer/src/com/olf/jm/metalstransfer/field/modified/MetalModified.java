package com.olf.jm.metalstransfer.field.modified;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.field.picklist.UnitPickListFilter;
import com.olf.jm.metalstransfer.field.setter.PortfolioFieldSetter;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin for 'Metal' tran info field.
 * <ol>
 * <li>Clears the internal portfolio based on the metal.</li>
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
 * | 002 | 07-Sep-2016 |               | S. Curran       | Updated to use the portfolio setter class.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class MetalModified extends AbstractTransactionListener {

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

    	PortfolioFieldSetter.setField(context, tran);

        // Set or clear the unit field
    	String metal = tran.getField("Metal").getValueAsString();
        ReferenceChoices choices = new UnitPickListFilter().retrieveOptions(context, metal);
        for(int i=0;i<choices.size();i++){
        	System.out.println(choices.get(i).getName());
        }
        
        Field unit = tran.getField("Unit");
        String unitName = unit.getValueAsString();
        // If there is only one choice set the account BU field to that choice
        if (choices.size() == 1) {
            unit.setValue(choices.get(0).getName());
        }else if (choices.findChoice(unitName)!=null && choices.findChoice(unitName).getId() !=-1 ) {
			unit.setValue(unitName);
		}else {
            unit.setValue("");                                                                                                
        }
    }
}

