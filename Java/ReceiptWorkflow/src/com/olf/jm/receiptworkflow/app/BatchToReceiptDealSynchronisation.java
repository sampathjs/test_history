package com.olf.jm.receiptworkflow.app;

import java.util.HashSet;
import java.util.Set;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.jm.receiptworkflow.model.RelNomField;
import com.olf.jm.receiptworkflow.persistence.BatchUtil;
import com.olf.jm.receiptworkflow.persistence.DBHelper;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNominationActivityTypeFieldId;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2015-10-21	V1.0	jwaechter	-	Initial version
 */

/**
 * This plugin updates the receipt deal's loco according to the logic
 * defined in the Receipt Workflow definition and updates the nominations
 * counterparty with the counterparty of the COMM-PHYS deal.
 * 
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class BatchToReceiptDealSynchronisation extends
		AbstractNominationProcessListener {
	
    @Override
    public PreProcessResult preProcess(final Context context, final Nominations nominations,
                                       final Nominations originalNominations, final Transactions transactions,
                                       final Table clientData) {
    	try {
			if (BatchUtil.isSafeUser(context.getUser()) ) {
				return PreProcessResult.succeeded(false);
			}
    		init(context);
        	process (context, nominations, transactions);
			PluginLog.info ("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) has ended successfully ******************");
        	return PreProcessResult.succeeded();
    	} catch (Throwable t) {
    		PluginLog.error (t.toString());
			PluginLog.info ("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) has ended with error ******************");
    		throw t;
    	}
    }

	private void process(Session session, Nominations nominations, Transactions transactions) {
		for (Nomination nom : nominations) {
			String activityType = RelNomField.ACTIVITY_ID.guardedGetString(nom);
			if (activityType.equalsIgnoreCase("Warehouse Receipt")) {
				int receiptDealNum = RelNomField.RECEIPT_DEAL.guardedGetInt(nom);
				if (receiptDealNum == 6) {
					PluginLog.info ("Skipped nomination #" + nom.getId() + " as it does not have "
							+ " a receipt deal assigned");
					continue;
				}
				if (!(nom instanceof Batch)) {
					PluginLog.info ("Skipped nomination #" + nom.getId() + " as it is not a batch");
					continue;
				}
				Transaction receiptDeal = findTransaction (receiptDealNum, transactions);
				updateNomCounterpartyIfNec(receiptDeal, nom);
				
				int warehouseDealNum =  RelNomField.WAREHOUSE_DEAL.guardedGetInt(nom); 
				if (warehouseDealNum == 6) {
					PluginLog.info ("Skipped nomination #" + nom.getId() + " for warehouse -> receipt "
							+ " synchronization as it does not have a receipt deal associated");
					continue;
				}
				Transaction warehouseDeal = findTransaction(warehouseDealNum, transactions);
				updateReceiptDealLocoIfNec (session, receiptDeal, nom, warehouseDeal);
			} else {
				PluginLog.info ("Skipped nomination #" + nom.getId() + " as it's" 
						+ RelNomField.ACTIVITY_ID.getName(nom) + " is not 'Warehouse Receipt'");
			}
		}
	}
	
	private void updateReceiptDealLocoIfNec(Session session, Transaction receiptDeal,
			Nomination nom, Transaction warehouseDeal) {
		String locationWarehouse = getLocationFromWarehouseDeal (warehouseDeal);
		String locoWarehouse = DBHelper.mapCommStorLocoToCommPhysLoco(session, locationWarehouse);
		Field receiptLocoField = receiptDeal.getField("Loco"); 
		String locoReceipt = receiptLocoField.getDisplayString();
		
		if (!locoReceipt.equals(locoWarehouse)) {
			receiptLocoField.setValue(locoWarehouse);
			PluginLog.info ("Updated receipt deal #" + receiptDeal.getDealTrackingId() + "." + receiptLocoField.getName()
					+ " from " + locoReceipt + " to " +  locoWarehouse);
		} else {
			PluginLog.info ("Receipt deal #" + receiptDeal.getDealTrackingId() + "." + receiptLocoField.getName()
					+ " is already synchronized with warehouse deal #" + warehouseDeal.getDealTrackingId() 
					+ " location field (" + locationWarehouse + " ->  " + locoWarehouse + ")");			
		}		
	}
	
	private String getLocationFromWarehouseDeal(Transaction warehouseDeal) {
		Leg leg = warehouseDeal.getLeg(1);		
		return leg.getValueAsString(EnumLegFieldId.Location);
	}


	private void updateNomCounterpartyIfNec (Transaction receiptDeal, Nomination nom) {
		
		String counterpartyDeal = receiptDeal.getDisplayString(EnumTransactionFieldId.ExternalBusinessUnit);
		String counterpartyNom = RelNomField.COUNTERPARTY.guardedGetString(nom);
		if (!counterpartyDeal.equals(counterpartyNom)) {
			RelNomField.COUNTERPARTY.guardedSet(nom, counterpartyDeal);
			PluginLog.info ("Updated nomination #" + nom.getId() + "." + RelNomField.COUNTERPARTY.getName(nom)
					+	" from " + counterpartyNom + " to " + counterpartyDeal);
			try {
				Ask.ok("The counterparty cannot be changed once a receipt is linked to a batch");
			} catch (OException ex) {
				PluginLog.info ("Can't notify user about counterparty synchronisation" + ex);
			}
		} else {
			PluginLog.info ("Nomination #" + nom.getId() + "." + RelNomField.COUNTERPARTY.getName(nom)
					+	" is already synchronized with receipt deal's external business unit " + counterpartyDeal);
			
		}
	}
	
	
	private Transaction findTransaction(int receiptDealNum, Transactions transactions) {
		for (Transaction trans : transactions) {
			if (trans.getDealTrackingId() == receiptDealNum) {
				return trans;
			}
		}
		throw new RuntimeException ("Could not find receipt deal #" + receiptDealNum + " among processed transactions");
	}

	public void init (Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {
			logLevel = ConfigurationItem.LOG_LEVEL.getValue();
			String logFile = ConfigurationItem.LOG_FILE.getValue();
			//String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
			String logDir = abOutdir + "\\error_logs";
			
			PluginLog.init(logLevel, logDir, logFile);
			PluginLog.info ("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) started ******************");
		}  catch (OException e) {
			throw new RuntimeException(e);
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}