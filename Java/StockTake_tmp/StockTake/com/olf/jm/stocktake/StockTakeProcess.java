package com.olf.jm.stocktake;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.stocktake.processor.StockTakePostProcess;
import com.olf.jm.stocktake.processor.StockTakePreProcessor;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/**
 * The Class StockTakeProcess. Ops service entry point into the stock take workflow. 
 * 
 * Used a pre  ops service to detect the change and get the user input and a post process to 
 * book the required deals. 
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class StockTakeProcess extends AbstractNominationProcessListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "StockTake";
	
	/** The Constant pre process SUBCONTEXT used to identify entries in the const repository.. */
	public static final String PRE_SUBCONTEXT = "PreProcess";

	/** The Constant post process SUBCONTEXT used to identify entries in the const repository.. */
	public static final String POST_SUBCONTEXT = "PostProcess";
	
	/**
	 * Initialise the class loggers.
	 *
	 * @param subContext the sub context
	 * @throws Exception the exception
	 */
	private void init(final String subContext) throws Exception {
		constRep = new ConstRepository(CONTEXT, subContext);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.scheduling.AbstractNominationProcessListener#
	 * preProcess(com.olf.embedded.application.Context, 
	 *            com.olf.openrisk.scheduling.Nominations, 
	 *            com.olf.openrisk.scheduling.Nominations, 
	 *            com.olf.openrisk.trading.Transactions, 
	 *            com.olf.openrisk.table.Table)
	 */
	@Override
	public final PreProcessResult preProcess(final Context context,
			final Nominations nominations, final Nominations originalNominations,
			final Transactions transactions, final Table clientData) {
		
		try {
			init(PRE_SUBCONTEXT);
			
			if (this.hasDispatch()) {
			    // Dispatch present so skip.
			    PluginLog.info("Skipping ops service dispatch present.");
			    return PreProcessResult.succeeded();
			}
			
			if (originalNominations == null || originalNominations.size() == 0) {
			    PluginLog.info("Skipping ops service no original nominations so new booking.");
			    return PreProcessResult.succeeded();				
			}
			
			StockTakePreProcessor processor = new StockTakePreProcessor(context, constRep);
			
			processor.preProcess(clientData, nominations, originalNominations);
			
			return PreProcessResult.succeeded();
			
		} catch (Exception e) {
			PluginLog.error("Error processing pre process stock take adjustments. " + e.getMessage());
			
			return PreProcessResult.failed(e.getMessage());
			
		} 
	}

	/* (non-Javadoc)
	 * @see com.olf.embedded.scheduling.AbstractNominationProcessListener#
	 *     postProcess(com.olf.openrisk.application.Session, 
	 *                 com.olf.openrisk.scheduling.Nominations, 
	 *                 com.olf.openrisk.table.Table)
	 */
	@Override
	public final void postProcess(final Session session, final Nominations nominations,
			final Table clientData) {
		
		try {
			init(POST_SUBCONTEXT);
			
	         if (this.hasDispatch()) {
	                // Dispatch present so skip.
	                PluginLog.info("Skipping ops service dispatch present.");
            }
			
			StockTakePostProcess processor = new StockTakePostProcess(session, constRep);
			
			processor.postProcess(nominations);
			
		} catch (Exception e) {
		    String errorMessage = "Error generating stock take adjustment transaction in the post process. " 
		            + e.getMessage();
		    
			PluginLog.error(errorMessage);
			
			throw new RuntimeException(errorMessage);
		}
	}



}
