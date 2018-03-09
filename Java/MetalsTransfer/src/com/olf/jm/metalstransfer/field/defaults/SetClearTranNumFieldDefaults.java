package com.olf.jm.metalstransfer.field.defaults;

import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.field.setter.DealRefFieldSetter;
import com.olf.jm.metalstransfer.field.setter.PortfolioFieldSetter;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin that sets default field values on the metals transfer strategy input when 
 * the deal number of reset.
 * <ol>
 * <li>Set deal reference to [UserId]_DDMMYYYY_HHMMSS.</li>
 * <li>Set the portfolio based on the metal selected.</li>
 * <li>Clear the balance check field.</li>
 * </ol>
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 07-Sep-2016 |               | S. Curran       | Initial version                                                                 |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class SetClearTranNumFieldDefaults extends SetInitialFieldDefaults{

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
    
    private void process(Context context, Transaction tran) {
    	if (tran.getTransactionId() == 0) {
    		DealRefFieldSetter.setField(context, tran);
    	
    		PortfolioFieldSetter.setField(context, tran);
        
    		// Reset the check balance fields
        	clearField(tran, "NegThreshold");
        	clearField(tran, "FromACBalBefore");
        	clearField(tran, "FromACBalAfter");
    	}
        
    }

    private void clearField(Transaction tran, String fieldName) {
    	try(Field field = tran.getField(fieldName)) {
    		field.setValue("");
    	}
    }
}
