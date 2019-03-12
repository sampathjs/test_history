package com.olf.jm.vatinclusivecalc;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class VatInclusiveAdjustEventsPost extends AbstractTradeProcessListener {
	
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "VAT Inclusive Field Deal Entry"; // sub context of constants repository

	/* (non-Javadoc)
	 * @see com.olf.embedded.trading.AbstractTradeProcessListener#postProcess(com.olf.openrisk.application.Session, com.olf.embedded.trading.TradeProcessListener.DealInfo, boolean, com.olf.openrisk.table.Table)
	 */
	@Override
	public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) {
		try {
			init();
			
			for(PostProcessingInfo<EnumTranStatus> post : deals.getPostProcessingInfo()) {
				int tranNum = post.getTransactionId();
				try(Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum);
						VatInclusiveTranHandler tranHandler = VatInclusiveCalcFactory.createHandler(session, tran)) {
					if(tranHandler == null) // Tran not applicable to our case
					{
						PluginLog.error("Handler not defined for this deal, please check configurations. Deal no. -> "+tran.getDealTrackingId());
						continue;
					}
					
					tranHandler.adjustEvents();
				}
			}
			
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			throw new RuntimeException(t);
		} finally {
			PluginLog.info("Finished");
		}
	}

	/**
	 * Initializes the log by retrieving logging settings from constants repository.
	 * @param context
	 * @throws Exception 
	 */
	private void init() throws Exception {

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			ConstRepository constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			PluginLog.init(logLevel, logDir, logFile);
			
		} catch (Exception e) {
			
			throw new Exception("Error initialising logging. " + e.getLocalizedMessage());
		}
	}
	
}
