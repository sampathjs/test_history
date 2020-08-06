package com.olf.jm.autosipopulation.app;

import java.util.List;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.autosipopulation.persistence.DBHelper;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.DealEvents;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-05-05	V1.0	jwaechter	- Initial Version
 * 2016-05-09	V1.1	jwaechter	- Now also processing internal target
 * 2016-05-10	V1.2	jwaechter	- refactored logic to be less dependent on infos provided by
 *                                    framework
 *                                  - moved several methods to DBHelper class.
 * 2016-06-07	V1.3	jwaechter	-   Added processing of external si / bu
 */

/**
 * Plugin to set the settlement instructions on relevant events for the VAT amounts. <br/>
 * Summary of the plugin logic: <br/>
 * Retrieve all transactions of the latest tran group of the deal being processed. <br/>
 * For each transaction loop through all deals <br/>
 * If it's possible to find a SI matching the criteria below, overwrite the existing internal SI of the event with the new SI:
 * <ul> 
 *   <li>    
 *   	The SI is associated with an account such that the currency of the account matches the "Settle CCY" of the event
 *   <li>
 *   <li>    
 *   	The SI is assigned to the same party as the internal business unit of the deal being processed
 *   <li>
 *   <li>    
 *   	The account associated with the SI has the account info field "VAT and Cash" being set to "Yes"
 *   <li>
 *   <li>    
 *   	In the new user table "USER_jm_unhedged_account_si" there is a row such that 
 *      i. the ins type of the deal matches the ins_type column 
 *      ii. the event type of the deal matches the event_type column and 
 *      iii. the hedged_in_endur column is set to 'No'
 *   <li>
 * </ul>
 * @author jwaechter 
 * @version 1.3
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class SettleInsForVatAndTaxEvents extends AbstractTradeProcessListener {
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "Auto SI Population VAT"; // sub context of constants repository
	
    /**
     * {@inheritDoc}
     */
	@Override
    public void postProcess(final Session session, final DealInfo<EnumTranStatus> deals,
                    final boolean succeeded, final Table clientData) {
		init (session);
		List<Transaction> transOfTranGroup = null;
		try {
			Logging.info(this.getClass().getName() + " started in postProcess\n"); 
			for (PostProcessingInfo<EnumTranStatus> ppi : deals.getPostProcessingInfo()) {
				int dealTrackingId = ppi.getDealTrackingId();
				try {
					transOfTranGroup = DBHelper.retrieveTransOfTranGroup(session, dealTrackingId);
					for (Transaction tran : transOfTranGroup) {
						process (session, tran);
					}
				} finally {
					if (transOfTranGroup != null) {
						for (Transaction tran : transOfTranGroup) {
							tran.dispose();
						}
						transOfTranGroup.clear();
					}					
				}
			}
			Logging.info(this.getClass().getName() + " ended\n");
			session.logStatus("Succeeded");
		} catch (Throwable ex) {
			Logging.error(ex.toString());
			Logging.error(this.getClass().getName() + " ended with status failed\n");
			session.logStatus("Failed");
			throw ex;
		}finally{
			Logging.close();
		}
    }
	
	@Override
    public void postProcessInternalTarget(final Session session,
            final DealInfo<EnumTranStatusInternalProcessing> deals, final boolean succeeded,
            final Table clientData) {
		init (session);
		List<Transaction> transOfTranGroup = null;
		try {
			Logging.info(this.getClass().getName() + " started in postProcess\n"); 
			for (PostProcessingInfo<EnumTranStatusInternalProcessing> ppi : deals.getPostProcessingInfo()) {
				int dealTrackingId = ppi.getDealTrackingId();
				try {
					transOfTranGroup = DBHelper.retrieveTransOfTranGroup(session, dealTrackingId);
					for (Transaction tran : transOfTranGroup) {
						process (session, tran);
					}
				} finally {
					if (transOfTranGroup != null) {
						for (Transaction tran : transOfTranGroup) {
							tran.dispose();
						}
						transOfTranGroup.clear();
					}					
				}
			}
			Logging.info(this.getClass().getName() + " ended\n");
			session.logStatus("Succeeded");
		} catch (Throwable ex) {
			Logging.error(ex.toString());
			Logging.error(this.getClass().getName() + " ended with status failed\n");
			session.logStatus("Failed");
			throw ex;
		}finally{
			Logging.close();
		}
    }

    
	private void process(Session session, Transaction tran) {
		DealEvents des = tran.getDealEvents();
		if (des == null) {
			return;
		}
		for (DealEvent de : des) {
			String settleCcy = de.getField("Settle CCY").getValueAsString();

			int siIdInt = DBHelper.getSiId (session, settleCcy, de.getId(), tran.getTransactionId(), true);
			if (siIdInt != -1) {
				int settleFieldIdInt = de.getFieldId("Int Settle Id");
				int oldSettleIdInt = de.getValueAsInt(settleFieldIdInt);
				if (oldSettleIdInt != siIdInt) {
					de.setValue(settleFieldIdInt, siIdInt);
					session.getBackOfficeFactory().saveSettlementInstructions(de);				
				}
			}

			int siIdExt = DBHelper.getSiId (session, settleCcy, de.getId(), tran.getTransactionId(), false);
			if (siIdInt != -1) {
				int settleFieldIdExt = de.getFieldId("Ext Settle Id");
				int oldSettleIdExt = de.getValueAsInt(settleFieldIdExt);
				if (oldSettleIdExt != siIdExt) {
					de.setValue(settleFieldIdExt, siIdExt);
					session.getBackOfficeFactory().saveSettlementInstructions(de);				
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
