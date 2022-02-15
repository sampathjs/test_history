/*
 * File updated 05/02/2021, 17:53
 */

package com.olf.jm.advancedpricing.fieldnotification;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.advancedpricing.model.TranInfoField;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

/*
 * History: 
 * 2017-07-26	V1.0 	sma	- Initial Version
 */ 

/**
 * Field notification script used to reset the tran info field Pricing Type.
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class ResetPricingType extends AbstractTransactionListener {

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";

	/* (non-Javadoc)
	 * @see com.olf.embedded.trading.AbstractTransactionListener#notify(com.olf.embedded.application.Context, com.olf.openrisk.trading.Transaction)
	 */
	@Override
	public void notify(Context context, Transaction tran) {
		try {
			init(this.getClass().getSimpleName());
			
			process(context, tran);
			
		} catch (Exception e) {
			Logging.error("Error resetting Tran Info Fields Pricing Type. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}
	
	
	 /**
 	 * Reset the tran info field Pricing Type.
 	 *
 	 * @param context the context the script is running in.
 	 * @param tran the transaction to reset the field on. 
 	 */
 	private void process(Context context, Transaction tran) {
		 AbstractFieldReset fieldResetter = new ResetPicklistToDefault(context);
		 
		 fieldResetter.resetField(tran, TranInfoField.PRICING_TYPE.getName());
	 }
 	
	private void init(String pluginName)  {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT,
						 CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(pluginName + " started");
	}

}
