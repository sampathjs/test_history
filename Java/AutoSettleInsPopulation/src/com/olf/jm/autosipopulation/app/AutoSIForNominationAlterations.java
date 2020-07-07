package com.olf.jm.autosipopulation.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.autosipopulation.app.SharedControlLogic.ReceiptLinkedStatus;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.scheduling.Field;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Deal;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2020-07-07	V1.0	jwaechter		- Initial Version
 */

/**
 * This class is applying Auto SI if a deal receipt deal is linked to a batch.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class AutoSIForNominationAlterations extends AbstractNominationProcessListener {
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "Auto SI Population On Nom Change"; // sub context of constants repository
	
    /**
     * {@inheritDoc}
     */
    public void postProcess(final Session session, final Nominations nominations, final Table clientData) {
    	init (session);
    	try {
        	for (Nomination nom : nominations) {
        		Field activityIdField = nom.retrieveField(EnumNomfField.NomCmotionCsdActivityId, 0);
        		String activity = null;
        		if (activityIdField != null && activityIdField.isApplicable() && activityIdField.isReadable()) {
        			activity =  activityIdField.getValueAsString();
        		}
    			if (!activity.equals("Warehouse Receipt")) {
    				Logging.info ("Skipping nomination " + nom.getId() + " as " + 
    						EnumNomfField.NomCmotionCsdActivityId + " is not 'Warehouse Receipt'");
    				continue;
    			}
        		
        		for (Deal deal : nom.getReceipt().getDeals()) {
        			Transaction tran = deal.getTransaction();
        			Logging.info("Current transaction #" + tran.getTransactionId() );
    				SharedControlLogic.ReceiptLinkedStatus receiptLinkedStatus = SharedControlLogic.retrieveReceiptStatus(session, tran);
        			Logging.info("Transaction #" + tran.getTransactionId() + " is in status " + receiptLinkedStatus);
        			if (receiptLinkedStatus == ReceiptLinkedStatus.RECEIPT_AND_LINKED) {
            			Logging.info("Processing transaction #" + tran.getTransactionId() + " from / to " + tran.getTransactionStatus());    				
        				tran.process(tran.getTransactionStatus());
            			Logging.info("Transaction #" + tran.getTransactionId() + " processed successfully");        				
        			}
        		}
        	}	
    	} finally {
    		Logging.close();
    	}
    }

    
	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			
			try {
				Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		Logging.info("\n\n********************* Start of new run ***************************");
	}

}
