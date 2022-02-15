package com.olf.jm.advancedpricing.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-08-08 - V0.1 - sma - Initial Version
 */

/**
 * The Class SettlementSplitOutput.
 * 
 * Output script to display a message box to the user to show that the report task has completed
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class SettlementSplitOutput extends AbstractGenericScript {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
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
			throw new RuntimeException("Error initilising logging. " + e.getLocalizedMessage());
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
			Ask.ok("Settlement Split complete");
								
		} catch (OException e) {
			String errorMsg = "Error displaying the matching process dialog. " + e.getMessage();
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
	

	
	/**
	 * Initilise the logging framwork and set the debug flag.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, 
					CONST_REPOSITORY_SUBCONTEXT);

		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	

}
