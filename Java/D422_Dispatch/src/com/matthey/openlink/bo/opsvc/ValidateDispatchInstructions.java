package com.matthey.openlink.bo.opsvc;

import java.util.Arrays;
import java.util.List;

import com.matthey.openlink.LondonBullionMarketAssociation;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Cargos;
import com.olf.openrisk.scheduling.Crate;
import com.olf.openrisk.scheduling.CrateItem;
import com.olf.openrisk.scheduling.EnumDeliveryFieldId;
import com.olf.openrisk.scheduling.EnumDeliveryStatus;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.NominationActivityType;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.DeliveryTickets;
import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.olf.openrisk.trading.EnumScheduleDetailFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.ScheduleDetail;
import com.olf.openrisk.trading.ScheduleDetails;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

/**
 * D422 Dispatch warehouse validation (4.2.4)
 * <br>Upon selecting <b>Dispatch</b> or updating <i><b>Save</b></i> a dispatch deal determine 'Is LGD required'
 * In the case where it is apply the most current value to the active batch
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking})
public class ValidateDispatchInstructions extends AbstractNominationProcessListener {

	static final int ERROR_BASE = 42200;
	static final int ERR_UNEXPECTED = ERROR_BASE + 88;
	static final int DISPATCH_BOOKING_ERROR = ERROR_BASE + 98;
	static final int DISPATCH_DELIVERY_ERROR = ERROR_BASE + 41;
	static private List<EnumTranStatus> permissableStatus = Arrays
			.asList(new EnumTranStatus[] { EnumTranStatus.New });

	@Override
	public PreProcessResult preProcess(Context context, Cargos cargos,
			Cargos originalCargos, Table clientData) {
		return super.preProcess(context, cargos, originalCargos, clientData);
	}

	@Override
	public PreProcessResult preProcess(Context context,
			Nominations nominations, Nominations originalNominations,
			Transactions transactions, Table clientData) {

		try {
			Logging.init(context, this.getClass(), "", "");
			if (/*true == true */this.hasDispatch()) {
				
				for (Nomination currentNomination : nominations) {

					if (currentNomination instanceof Batch /* &&  currentNomination.getType() == EnumSchedulingObject.Dispatch */) {
						Batch batch = (Batch) currentNomination;
						if ("Warehouse Dispatch".equalsIgnoreCase(batch
								.retrieveField(
										EnumNomfField.NomCmotionCsdActivityId,
										0).getDisplayString())) {


							PreProcessResult result;
							if (LondonBullionMarketAssociation.qualifiesForLGD(context, batch)) {
							  Logging.info("Qualifies for LGD");
								 
								if (EnumDeliveryStatus.Deleted.getValue() != currentNomination.getField("Status").getValueAsInt()) { // FIX SR44-13715, don't attempt iteration of deleted delivery
									// We only generate LGD when we are a completed dispatch 
									//this.getDispatch().getField(EnumDispatchFieldId.IsDispatched).getValueAsBoolean()) 
									
									LondonBullionMarketAssociation lbmaJM = new LondonBullionMarketAssociation(context);
						
										for (DeliveryTicket batchItem : batch.getBatchContainers()) {
											com.olf.openrisk.trading.Field batchLGD = batchItem.getField(LondonBullionMarketAssociation.properties.getProperty(LondonBullionMarketAssociation.DELIVERY_INFO));
											if (!batchItem.getTransaction()
													.getField(EnumTransactionFieldId.InternalLegalEntity)
													.getDisplayString()
												.equalsIgnoreCase(
														batch.getField(EnumNominationFieldId.InternalLegalEntity)
																.getDisplayString())) {
												
											Logging.info(String.format(
													"Batch#%d Container#%d Dispatch & Receipt mismatch on Internal LE... for LGD(%s)",
																batch.getBatchId(),
																batchItem.getDeliveryTicketNumber(),
																batchLGD.getDisplayString()));
											}
											
											if (null!=batchLGD && batchLGD.getDisplayString().trim().length()<1) {
												batchLGD.setValue(LondonBullionMarketAssociation.getLGD(context, lbmaJM).toString());
											Logging.info(
														String.format("SEtTING LGD>%s<", 
																batchLGD.getDisplayString().trim()));

											}
											//Sync TicketInfo across inventory transactions
											batchSyncTicketInfo(context, batchItem, transactions, batch);
											
										Logging.info(
													String.format("Batch#%d Container#%d has LDG=%s",
															batch.getBatchId(),
															batchItem.getDeliveryTicketNumber(),
															batchLGD.getDisplayString()));

										}

								}
							} 
						}
					}
				}
			}
			return PreProcessResult.succeeded();
		} catch (Exception e) {
			String reason = String.format("PreProcess(%d)> FAILED %s CAUSE:%s",
					ERR_UNEXPECTED, this.getClass().getSimpleName(),
					e.getLocalizedMessage());
			Logging.error(reason, e);
			e.printStackTrace();
			return PreProcessResult.failed(reason);

		} finally {
			Logging.close();
		}
		
	}

	private void batchSyncTicketInfo(Context context, DeliveryTicket batchTicket, Transactions transactions, Batch batch) {

		if (null != batch) {
			// check Nominations for a match!!!
			int dispatchDealNumber = getSaleDealNumFromDispatchInventory(batch);
			if (dispatchDealNumber > 0) {
				ScheduleDetail parentTSD = (ScheduleDetail)batchTicket.getParent();
				int deliveryId = parentTSD.getField(EnumScheduleDetailFieldId.DeliveryId).getValueAsInt();
				syncDispatchDeliveryTicketInfo(batchTicket, deliveryId,
						getTransactionFromDealNumber(dispatchDealNumber, transactions));
			}

		} else {
			throw new ValidateDispatchInstructionsException("Batch not set!", DISPATCH_DELIVERY_ERROR);
		}

	}
	
	/**
	 * Finds the activity Id in the system for Dispatched Noms 
	 * This can be a constant in known configuration.
	 * @return
	 */
	private int FindDispatchActivityTypeID(Context context) {
		//This is the exact description of the Dispatch Activity type
		String dispatchActivityType = "Warehouse Dispatch";
		
		StaticDataFactory staticFactory = context.getStaticDataFactory();
		NominationActivityType nomact = staticFactory.getReferenceObject(NominationActivityType.class, dispatchActivityType );
	
		if (nomact == null)
			throw new OpenRiskException("NominationActivity Type '" + dispatchActivityType + "' is not configured.  Cannot continue.");
		
		return nomact.getId();
	}
	
	
	/**
	 * Find a Transaction from Deal Number
	 * @param dealNumber
	 * @param transactions
	 * @return
	 * @throws OpenRiskException
	 */
	Transaction getTransactionFromDealNumber(int dealNumber, Transactions transactions) throws OpenRiskException	{
		int numTrans = transactions.getCount();
		Transaction tempTran = null;
			
		for (int i = 0; i < numTrans; i++)	{
		
			tempTran = transactions.get(i);
			
			if (tempTran.getDealTrackingId() == dealNumber)
				return tempTran;
		}
		
		throw new ValidateDispatchInstructionsException("Unable to find Transaction for provided dealnumber " + dealNumber, DISPATCH_DELIVERY_ERROR);
			
	}
	
	/**
	 * There is no limitation that says we can't Dispatch 
	 * a dispatch that just has empty crates in it. so lets check and make sure we have inventory
	 * that is not being deleted  if we do, get the sale tran number so we can look it up later, 
	 * we will need it. If there is no inventory to process return 0
	 * @param nominations
	 * @return
	 */
	private int getSaleDealNumFromDispatchInventory(Nomination nomination) throws OpenRiskException {

		int saleDealNumber = 0;
		
		for(Crate crate : this.getDispatch().getCrates()) {
			for(CrateItem item : crate.getCrateItems()) {
				
				DeliveryTicket ticket = item.getSourceDeliveryTicket();
				ScheduleDetail parentTSD = (ScheduleDetail) ticket.getParent();
				
				int deliveryId = parentTSD.getField(EnumScheduleDetailFieldId.DeliveryId).getValueAsInt();
				if (nomination.getId() == deliveryId) {				
					saleDealNumber = nomination.getDelivery().getField(EnumDeliveryFieldId.PrimaryDealNumber).getValueAsInt();
					return saleDealNumber;
				}
				
			}
		}
					
		return saleDealNumber;
	}
	
		
	/**
	 * Delivery Ticket Info Fields need to be Synced
	 * so if we change one, we need to sync them on the sale tran
	 * @param sourceTicket
	 * @param deliveryID
	 * @param saleTran
	 * @throws OpenRiskException
	 */
	void syncDispatchDeliveryTicketInfo(DeliveryTicket sourceTicket, int deliveryID, Transaction saleTran) throws OpenRiskException {
		
		int containerId = sourceTicket.getField(EnumDeliveryTicketFieldId.ContainerId).getValueAsInt();
		
		DeliveryTicket destinationTicket = null;
		
		int legCount = saleTran.getLegCount();
		for(int leg=0;leg<legCount;leg++) {
			if (saleTran.getLeg(leg).getLegLabel().startsWith("Physical"))
				continue; //skip physical legs
	
			destinationTicket = getDestinationTicket(containerId, getTicketScheduleDetails(deliveryID, saleTran.getLeg(leg)));
			
			if (null == destinationTicket) {
				Logging.info(
						/*throw new OpenRiskException(*/" Unable to find Container with container id: " + containerId);
				continue;
			}
			
			syncDeliveryTicketsFields(sourceTicket, destinationTicket, new String[] { LondonBullionMarketAssociation.properties.getProperty(LondonBullionMarketAssociation.DELIVERY_INFO) } );
		}
	
	}
	
	/**
	 * obtain delivery ticket matching the current inventory so we can propergate info fields...
	 */
	private DeliveryTicket getDestinationTicket(int containerId, ScheduleDetail tsd) {
		
		DeliveryTicket destinationTicket = null;
		if (null == tsd) {
			return destinationTicket;
		}

		DeliveryTickets tickets = tsd.getDeliveryTickets();
		int ticketCount = tickets.getCount();
		
		for (int i = 0; i < ticketCount; i++) {
			
			DeliveryTicket tempTicket = tickets.get(i);
			
			if (tempTicket.getField("Container ID").getValueAsInt() == containerId) {
				destinationTicket = tempTicket;
				break;
			}
		}
		return destinationTicket;

	}

	/**
	 * obtain relevant schedule from the dispatch deals delivery schedules
	 * @param deliveryID
	 * @param dispatchLeg
	 * @return
	 */
	private ScheduleDetail getTicketScheduleDetails(int deliveryID, Leg dispatchLeg) {

		
		ScheduleDetails tsds = dispatchLeg.getScheduleDetails();
		ScheduleDetail tsd = null;
		
		int tsdCount = tsds.getCount();		
		for (int i = 0; i < tsdCount; i++) {
			
			tsd = tsds.get(i);
			if (tsd.getField(EnumScheduleDetailFieldId.DeliveryId).getValueAsInt() == deliveryID ) {
				
				if (tsd.getField(EnumScheduleDetailFieldId.BavFlag).getValueAsBoolean()) {
					break;
				}
			}
		}
		
		if (null == tsd) {
			Logging.info(
					/*throw new OpenRiskException(*/
					" Unable to find Sell Tran TSD with delivery id: " + deliveryID);
		}
		return tsd;
	}

	/**
	 * Copy a list of info fields across 2 tickets in a DeliveryTickets collection 
	 * @param tickets
	 * @param fromIndex
	 * @param toIndex
	 * @param fieldArray
	 * @throws OpenRiskException
	 */
	private void syncDeliveryTicketsFields(DeliveryTicket fromTicket, DeliveryTicket toTicket, String[] fieldArray) throws OpenRiskException {
		
		int numFields = fieldArray.length;
		
		for (int i = 0; i < numFields; i++)	{
			
			com.olf.openrisk.trading.Field fromField = fromTicket.getField(fieldArray[i]);
			Logging.info(
					String.format("DeliveryTickets#%d->%d Field:%s \tValue:%s",fromTicket.getDeliveryTicketNumber(), toTicket.getDeliveryTicketNumber(), fromField.getName(), fromField.getDisplayString()));
			toTicket.getField(fieldArray[i]).setValue(fromField.getDisplayString());
		}
		
	}

}
