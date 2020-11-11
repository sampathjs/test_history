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
 * The Class ApDpDailySummaryReportParam.
 * 
 * Message box confirming that the user has run the matching process
 * 
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class ApDpDailySummaryReportParam extends AbstractGenericScript {
	
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
		
		
		Table returnT = context.getTableFactory().createTable();
	
		
		if(context.hasDisplay()) {
			// Prompt the use  have they run the matching process
			confirmMatchingRun();
		} 
			
		return returnT;
		} catch (Exception e) {
			Logging.error("Error running APDP Daily Summary report. " + e.getLocalizedMessage());
			throw new RuntimeException("Error running APDP Daily Summary report. " + e.getLocalizedMessage());
		}finally{
			Logging.close();
		}

	}  
	
	private void confirmMatchingRun() {
		try {
			int response = Ask.okCancel("Did you run the matching process and AD-DP Pricing report?");
			
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
	

	/**
	 * Initialise the logging framework and set the debug flag.
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
