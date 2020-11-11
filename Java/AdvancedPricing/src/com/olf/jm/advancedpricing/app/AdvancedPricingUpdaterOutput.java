package com.olf.jm.advancedpricing.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;


/*
 * History:
 * 2017-08-08 - V0.1 - sma - Initial Version
 */

/**
 * The Class AdvancedPricingUpdaterOutput.
 * 
 * Output script to display a message box to the user to show that the report task has completed
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AdvancedPricingUpdaterOutput extends AbstractGenericScript {
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
    
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, ConstTable table) {
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getLocalizedMessage());
		}
		if(context.hasDisplay()) {
			// Prompt the use  have they run the matching process
			reportCompleteMessage();
		} 
		Logging.close();
		return null;

	}  
	
	private void reportCompleteMessage() {
		try {
			Ask.ok("Matching process complete");
								
		} catch (OException e) {
			String errorMsg = "Error displaying the matching process dialog. " + e.getMessage();
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
	
	/**
	 * Initialise the logging framework and set the debug flag.
	 */
	private void init() {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getMessage());
		}
	}
}
