package com.olf.jm.advancedPricingReporting;

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
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDpDailySummaryReportOutput.
 * 
 * Output script to display a message box to the user to show that the report task has completed
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class ApDpDailySummaryReportOutput extends AbstractGenericScript {
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Daily Summary Reporting";
    
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, ConstTable table) {
		
		try {
			init();
		

		if(context.hasDisplay()) {
			// Prompt the use  have they run the matching process
			reportCompleteMessage();
		} 
		
		return null;
		} catch (Exception e) {
			Logging.error("Error running APDP Daily Summary report. " + e.getLocalizedMessage());
			throw new RuntimeException("Error running APDP Daily Summary report. " + e.getLocalizedMessage());
		}finally{
			Logging.close();
		}

	}  
	
	private void reportCompleteMessage() {
		try {
			Ask.ok("Report generation complete");
								
		} catch (OException e) {
			String errorMsg = "Error displaying the matching process dialog. " + e.getMessage();
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
	

	
	/**
	 * Initilise the logging framework and set the debug flag.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
	
}
