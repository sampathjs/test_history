package com.olf.jm.swaps.app;

import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.Field;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-07-27     V1.0       scurran            - initial version 
 */ 
@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class PriceVolumeUnitSynchroniser extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "FrontOffice";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "PriceVolumeUnitSynchroniser";
	
	
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.trading.AbstractFieldListener#postProcess(com.olf.openrisk.application.Session, com.olf.openrisk.trading.Field, java.lang.String, java.lang.String, com.olf.openrisk.table.Table)
	 */
	@Override
	public void postProcess(Session session, Field field, String oldValue,
			String newValue, Table clientData) {
		
		try {
			init();
			
			Logging.debug("About to try synchronising Volumne Unit.");
			Synchroniser volumeUnitSynchroniser = new Synchroniser(EnumTranfField.Unit);
			volumeUnitSynchroniser.synchronise(field);
			
			Logging.debug("About to try synchronising Price Unit.");
			Synchroniser priceUnitSynchroniser = new Synchroniser(EnumTranfField.PriceUnit);
			priceUnitSynchroniser.synchronise(field);			
		} catch (Exception e) {
			Logging.error("Error processing Event. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}

}
