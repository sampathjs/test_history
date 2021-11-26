package com.olf.jm.operation;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * 
 * When clear Tran Num is pressed, clear Broker fee fields.
 * 
 * Revision History:
 * Version  Updated By    Date         Ticket#    Description
 * -----------------------------------------------------------------------------------
 * 	01      Prashanth     23-Jul-2021  EPI-1712   Initial version
 * 	02      Gaurav        11-Nov-2021  EPI-1532   Passthrough Changes
 */

@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class ClearBrokerFees extends AbstractTransactionListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Broker Fee";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "Trading";
	
	@Override
	public void notify(Context context, Transaction tran) {
		try {
			init();
			
			//Clear Broker
			if(tran.getField("Broker").isApplicable() && tran.getField("Broker").getValueAsInt()>0){
				tran.getField("Broker").setValue(0); 
			}
			
		} catch (Exception e) {
			Logging.error("Error clearing Broker Fee  Fields. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), constRep.getContext(), constRep.getSubcontext());
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
}