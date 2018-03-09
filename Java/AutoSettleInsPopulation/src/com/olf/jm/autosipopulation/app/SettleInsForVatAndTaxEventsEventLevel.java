package com.olf.jm.autosipopulation.app;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.jm.autosipopulation.persistence.DBHelper;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.DealEvents;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-05-10	V1.0	jwaechter	- 	Initial Version
 * 2016-06-07	V1.1	jwaechter	-   Added processing of external si / bu
 */

/**
 * Plugin ensures the settlement instructions on relevant events for the VAT amounts are set. <br/>
 * See class {@link SettleInsForVatAndTaxEvents} for details about the logic as 
 * SettleInsForVatAndTaxEventsEventLevel is just utilizing a different trigger.
 * @author jwaechter
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcSaveSettleInstr })
public class SettleInsForVatAndTaxEventsEventLevel extends
		AbstractGenericOpsServiceListener {
    private static final String CONST_REPO_SUBCONTEXT = "FrontOffice";
	private static final String CONST_REPO_CONTEXT = "Auto SI Population VAT Event";



	/**
     * {@inheritDoc}
     */
    public void postProcess(final Session session, final EnumOpsServiceType type,
                            final ConstTable table, final Table clientData) {
    	try {
    		init (session);
    		process (session, table);
    		PluginLog.info(this.getClass().getName() + " end successfully");
			session.logStatus("Succeeded");
    	} catch (Throwable t) {
    		String errorMessage = t.getMessage();
    		for (StackTraceElement ste : t.getStackTrace()) {
        		errorMessage += "\n" + ste.toString();
    		}
    		PluginLog.error(this.getClass().getName() + " fails:");
    		PluginLog.error(errorMessage);
    		throw t;
    	}
    }
    
    private void process (Session session, final ConstTable t) {
    	for (int row = t.getRowCount()-1; row >= 0; row--) {
    		int tranNum = t.getInt("tran_num", row);
    		long eventId = t.getLong("event_num", row);
    		int intSettleId = t.getInt("int_settle_id", row);
    		int extSettleId = t.getInt("ext_settle_id", row);
    		int settleCcyId = t.getInt ("settle_currency_id", row);
    		String settleCcy = session.getStaticDataFactory().getName(EnumReferenceTable.Currency, settleCcyId);

    		int supposedIdInt = DBHelper.getSiId(session, settleCcy, eventId, tranNum, true);
			if (supposedIdInt != -1) { // -1 = not relevant for processing
	    		if (intSettleId != supposedIdInt) {
	    			try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
	        			DealEvents events = tran.getDealEvents();
	        			for (DealEvent event : events) {
	        				long eventIdDealEvent = event.getId();
	        				if (eventIdDealEvent == eventId) {
	            				int settleFieldId = event.getFieldId("Int Settle Id");
	        					event.setValue(settleFieldId, supposedIdInt);
	        					session.getBackOfficeFactory().saveSettlementInstructions(event);				
	        				}
	        			}    				
	    			}
	    		}
			}
    		int supposedIdExt = DBHelper.getSiId(session, settleCcy, eventId, tranNum, false);
			if (supposedIdExt != -1) { // -1 = not relevant for processing
	    		if (extSettleId != supposedIdExt) {
	    			try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
	        			DealEvents events = tran.getDealEvents();
	        			for (DealEvent event : events) {
	        				long eventIdDealEvent = event.getId();
	        				if (eventIdDealEvent == eventId) {
	            				int settleFieldId = event.getFieldId("Ext Settle Id");
	        					event.setValue(settleFieldId, supposedIdExt);
	        					session.getBackOfficeFactory().saveSettlementInstructions(event);
	        				}
	        			}			
	    			}
	    		}
			}
    	}
    }
    
	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param session
	 */
	private void init(final Session session) {
		try {
			String abOutdir = session.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		PluginLog.info("\n\n********************* Start of new run ***************************");
	}
}
