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
 * 2017-08-08	V1.0	lma 	- initial version
 */
/**
 * Parameter script to add a pop up which asks user confirm of the matching process
 * 
 * @author sma
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AdvancedPricingUpdaterParam extends AbstractGenericScript {
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
	
	/**
	 * For same customer Id and same metal type, loop through buy validated deals oldest first, 
	 * matching them to sell deals where sell deals are validated and Match status N/P, oldest first. 
	 * Update the user-tables for buy deals, sell deals and their relationship.
	 * {@inheritDoc}
	 */
	public Table execute(final Context context, final EnumScriptCategory category, final ConstTable table) {
		try {
			init(this.getClass().getSimpleName());
						
			if(context.hasDisplay()) {
				// Prompt the use to start the matching process
				confirmMatchingRun();
			}
			

			return null;
			
		} catch (Throwable t)  {
			Logging.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		}finally{
			Logging.close();		
		}

	}
	
	private void init(String pluginName)  {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(pluginName + " started.");
	}
	
	private void confirmMatchingRun() {
		try {
			int response = Ask.okCancel("Would you like to run the matching process?");
			
			if(response == 0) {
				String errorMsg = "User cancelled operation.";
				Logging.error(errorMsg);
				throw new RuntimeException(errorMsg);				
			}
		} catch (OException e) {
			String errorMsg = "Error displaying the matching process dialog. " + e.getMessage();
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}


}
