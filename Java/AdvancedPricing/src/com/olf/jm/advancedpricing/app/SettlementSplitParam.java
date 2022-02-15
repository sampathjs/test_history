/*
 * File updated 05/02/2021, 17:53
 */

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
 * 2019-01-26	V1.0	lma 	- initial version
 */
/**
 * Parameter script to add a pop up which asks user confirm of the matching process
 * 
 * @author sma
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class SettlementSplitParam extends AbstractGenericScript {
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
	
	/**
	 * Confirm to run Settlement Split.
	 * {@inheritDoc}
	 */
	public Table execute(final Context context, final EnumScriptCategory category, final ConstTable table) {
		try {
			init();
						
			if(context.hasDisplay()) {
				// Prompt the use to start the Settlement Split
				confirmSettleSplitRun();
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
	
	private void init()  {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void confirmSettleSplitRun() {
		try {
			int response = Ask.okCancel("Would you like to run the process to Split Settlement?");
			
			if(response == 0) {
				String errorMsg = "User cancelled operation.";
				Logging.error(errorMsg);
				throw new RuntimeException(errorMsg);				
			}
		} catch (OException e) {
			String errorMsg = "Error displaying the Split Settlement process dialog. " + e.getMessage();
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
}
