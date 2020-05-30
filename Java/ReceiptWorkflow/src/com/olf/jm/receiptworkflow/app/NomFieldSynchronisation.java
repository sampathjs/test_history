package com.olf.jm.receiptworkflow.app;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.jm.receiptworkflow.model.Pair;
import com.olf.jm.receiptworkflow.persistence.BatchUtil;
import com.olf.jm.receiptworkflow.persistence.DBHelper;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Debug;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Delivery;
import com.olf.openrisk.scheduling.EnumDeliveryFieldId;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.scheduling.Receipt;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.scheduling.Field;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-09-23	V1.0	jwaechter	-	initial version
 */

/**
 * Class containing logic according to section 4.4 of implementation item D3520 Receipt Workflow.
 * This plugins synchronizes contents between the the batches being created on Dispatch and 
 * Safe desktop.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class NomFieldSynchronisation extends AbstractNominationProcessListener {
	
//	private Debug dbg;
	
	 /**
     * {@inheritDoc}
     */
	@Override
    public PreProcessResult preProcess(final Context context, final Nominations nominations,
                                       final Nominations originalNominations, final Transactions transactions,
                                       final Table clientData) {
		try {
			init (context);
			if (BatchUtil.amIDoingBatchInjection(nominations)) {
				propagateNomInfoFieldsOnReciept(context, nominations);				
			}
			Logging.info ("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) has ended successfully ******************");
		} catch (Throwable t) {
			Logging.error (t.toString());
			Logging.error (Arrays.deepToString(t.getStackTrace()));
			Logging.info ("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) has ended with error ******************");
			throw t;
		}finally{
			Logging.close();
		}
		return PreProcessResult.succeeded();
    }

	/**
	 * Propagate Nom Info Fields during Batch Receipt will only propagate if you edit the 
	 * Origin (Warehouse Receipt) Nom.  Core fields propagate forward when you change them on any
	 * nom in the chain, but core doesn't sync info fields. The user should only change info field 
	 * on the Origin Nom if they want it to propagate forward.  
	 * On Receipt, only receipt noms propagate info fields, on transfer, changes to any nom 
	 * propagate forward.  this is an implementation choice, but care should be taken with this.
	 * If the code is changed to operate on every nom, then order of processing will matter and
	 * you might get unintended side effects. More advanced things could be done, like comparing 
	 * values to DB noms, or going to the database to determine what changed, and then finding the oldest
	 * nom in each chain to get the desired results, but the only intention
	 * here was to show how to propagate info fields down the chain.  Transfer is done a bit differently
	 * just to show an example
	 * 
	 * @param nominations
	 * @throws OpenRiskException
	 */
	private void propagateNomInfoFieldsOnReciept(Session session, Nominations nominations) throws OpenRiskException 	{
		int numNoms = nominations.getCount();
		Batch nomInject = null;
		int storageDealNum = 0;
		int originDeliveryID;
		
		Set<String> relevantNomInfoFieldNames = DBHelper.loadRelevantNomInfoFieldNames (session);
		
		// Go through all the noms - 
		for (Nomination nom : nominations) {
			if (!(nom instanceof Batch)) {
				continue;
			}				
			// All Nomination chains start with an origin nom, otherwise we are not propagating
			nomInject = (Batch) nom;
			if (!nomInject.isOriginBatch())
				continue;
			
			// if Origin Nomination has no storage deal, there is nothing to propagate
			storageDealNum = nomInject.getField(EnumNominationFieldId.ServiceProviderDealNumber).getValueAsInt();
			
			if (storageDealNum != 6)	{
					
				originDeliveryID = nomInject.getId();
				
				Logging.info("Syncing Info Fields For Nom chain with Origin (Warehouse Receipt) Nom Delivy ID:" + originDeliveryID);
			
				forwardPropagateFieldsThroughNominationChain(nominations, nomInject, relevantNomInfoFieldNames);
			
			}
		}
	}	
	
	



	/**
	 * Copy the field value down the nomination chain
	 * @param nominations
	 * @param nominationn
	 * @param fieldArray
	 */
	private void forwardPropagateFieldsThroughNominationChain(Nominations nominations, Batch sourceNomination, Set<String> relevantInfoFields) {
		
		int currentDeliveryId = sourceNomination.getId();
		
		for (Nomination nomDownStream : nominations) {
			
			int downStreamNomDeliveryId = nomDownStream.getId();
			int downStreamNomSourceDeliveryId = nomDownStream.getField(EnumNominationFieldId.SourceDeliveryId).getValueAsInt();
			
			if (downStreamNomDeliveryId != currentDeliveryId) {
				if (downStreamNomSourceDeliveryId == currentDeliveryId) {
					
					syncFieldsFromNomtoNom(sourceNomination, nomDownStream, relevantInfoFields);
					Logging.info("     - Synced Info Fields from Nom ID:" + currentDeliveryId + 
							" to Nom delivey ID:" + downStreamNomDeliveryId);
					
					forwardPropagateFieldsThroughNominationChain(nominations, (Batch)nomDownStream, relevantInfoFields );
			
				}
			}
		}
	}
	
	/**
	 * Copy a list of info fields across 2 nominations in a Nominations collection
	 * @param noms
	 * @param fromIndex
	 * @param toIndex
	 * @param fieldArray
	 * @throws OpenRiskException
	 */
	private void syncFieldsFromNomtoNom(Nominations noms, int fromIndex, int toIndex, Set<String> relevantInfoFields) throws OpenRiskException {
		
		Nomination fromNom = noms.get(fromIndex);
		Nomination toNom = noms.get(toIndex);
	
		syncFieldsFromNomtoNom(fromNom, toNom, relevantInfoFields);
	}
	
	private void syncFieldsFromNomtoNom(Nomination fromNom, Nomination toNom, Set<String> relevantInfoFields) throws OpenRiskException {
		
		String value = "";
		
		for (String fieldName : relevantInfoFields)	{
			Field fromField = fromNom.getField(fieldName);
			Field toField = toNom.getField(fieldName);
			if (fromField != null && fromField.isApplicable() && fromField.isReadable()
			 && toField != null && toField.isApplicable() && toField.isWritable()) {
				value = fromField.getValueAsString();
				toField.setValue(value);				
			}
		}
	}

	public void init (Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {

			logLevel = ConfigurationItem.LOG_LEVEL.getValue();
			String logFile = ConfigurationItem.LOG_FILE.getValue();
			//String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
			String logDir = abOutdir + "\\error_logs";
			
			Logging.init(this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
			Logging.info("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) started ******************");
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
	