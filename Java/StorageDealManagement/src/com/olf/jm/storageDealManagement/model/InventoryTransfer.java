package com.olf.jm.storageDealManagement.model;

import java.text.SimpleDateFormat;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.ContainerTransfers;
import com.olf.openrisk.scheduling.SchedulingFactory;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

public class InventoryTransfer {
	
	enum TransferType {LINKED, UNLINKED};

	private Context context;
	
	private SchedulingFactory schedulingFactory;
	
	public InventoryTransfer( Context currentContext) {
		context = currentContext;
		
		schedulingFactory = context.getSchedulingFactory();
	}
	
	public void transfer(StorageDeal source, Transaction destination) {
		
		// Move linked inventory onto new comm-stor
		transferLinkedInventory(source, destination);
		
		// Move unlinked inventory onto new comm-stor
		transferUnlinkedInventory(source, destination);		
	}
	
	private void transferLinkedInventory(StorageDeal source, Transaction destination) {
	
		List<Inventory> inventoryToMove = source.getLinkedReceiptBatches();
		
		if(inventoryToMove.size() == 0) {
			PluginLog.info("No linked inventory to move.");
			return;
		}
		PluginLog.info("Transfering linked inventory. Moving " + inventoryToMove.size() +  " batches.");
		
		final int masterLocation = inventoryToMove.get(0).getLocationId();
		
		for (Inventory inventory :inventoryToMove ) {
			int deliveryId = inventory.getDeliveryId();
			int locationId = inventory.getLocationId();
			
			PluginLog.info("Transfering linked inventory. Moving delivery id " + deliveryId);
			
			if(masterLocation != locationId) {
				String errorMessage = "Expecting to move batches for a single location. Expected location "
						+ masterLocation + " but found location " + locationId;
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
			
			try(Leg leg = getLocationLeg( destination,  locationId);
				Batch inv = schedulingFactory.retrieveBatchByDeliveryId(deliveryId);
				ContainerTransfers transfer = schedulingFactory.createContainerTransfers(leg)	) {
				
				transfer.addBatch(inv);
				transfer.save();
						
				PluginLog.debug("Transfering inventory batch " + deliveryId + " from deal " + source.getDealTrackingNumber() + " to " + destination.getDealTrackingId());				
			} catch (Exception e) {
				String errorMessage = "Error during transfer of linked inventor. " +  e.getMessage();
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);				
			}
		}
		
		validateTransfer(source, destination, inventoryToMove, TransferType.LINKED);
		
		PluginLog.info("Transfering linked inventory complete.");
		
	}
	
	private void transferUnlinkedInventory(StorageDeal source, Transaction destination) {

		List<Inventory> inventoryToMove = source.getUnLinkedReceiptBatches();
		if(inventoryToMove.size() == 0) {
			PluginLog.info("No unlinked inventory to move.");
			return;
		}
		PluginLog.info("Transfering unlinked inventory. Moving " + inventoryToMove.size() +  " batches.");
		
		final int masterLocation = inventoryToMove.get(0).getLocationId();
		
		PluginLog.info("About to move " + inventoryToMove.size() + " unlinked batches.");
		
		for (Inventory inventory : inventoryToMove) {
			int locationId = inventory.getLocationId();
			int batchDeliveryId = inventory.getDeliveryId();
			
			PluginLog.info("Transfering unlinked inventory. Moving delivery id " + batchDeliveryId);
			
			if(masterLocation != locationId) {
				String errorMessage = "Expecting to move batches for a single location. Expected location "
						+ masterLocation + " but found location " + locationId;
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
			
			try(Batch batch = schedulingFactory.retrieveBatchByDeliveryId(batchDeliveryId)) {
				PluginLog.debug("Moving receipt batch " + batchDeliveryId + " from deal " + source.getDealTrackingNumber() + " to " + destination.getDealTrackingId());			
				batch.assignBatchToStorage(destination);
				batch.save();				
			} catch (Exception e) {
				String errorMessage = "Error during transfer of unlinked inventor. " +  e.getMessage();
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);				
			}
		}
		
		validateTransfer(source, destination, inventoryToMove, TransferType.UNLINKED);
		
		PluginLog.info("Transfering unlinked inventory complete.");
	}
	
	private Leg getLocationLeg(Transaction commStore, int locationId) {
		
		Legs legs = commStore.getLegs();
		
		for(Leg leg : legs) {
			
			if(leg.isApplicable(EnumLegFieldId.Location) && leg.getValueAsInt(EnumLegFieldId.Location) == locationId) {
				return leg;
			}
		}
		
		throw new RuntimeException("Unable to fine a leg for location id " + locationId + " on COMM-STOR deal.");
	}
	
	private void validateTransfer(StorageDeal source, Transaction destination, List<Inventory> originalInventory, TransferType transferType) {
		StorageDeal newStorageDeal = new StorageDeal(destination.getDealTrackingId());
		
		List<Inventory> newInventory = null; 
		
		if(transferType == TransferType.LINKED) {
			newInventory = newStorageDeal.getLinkedReceiptBatches();
		} else {
			newInventory = newStorageDeal.getUnLinkedReceiptBatches();
		}
		
		for(Inventory inventory : originalInventory) {
			int batchId = inventory.getBatchId();			
			
			if(!newInventory.contains(inventory)) {
				String errorMessage = "Error validating moved invemtory. Batch Id " + batchId + " is missing from the new storage deal";
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);					
			}
		}
		
		ActivityReport.transfer(source.getDealTrackingNumber(), 
				destination.getDealTrackingId(), originalInventory.size(), 
				transferType ==  TransferType.LINKED ? "Linked": "Unlinked",
				source.getMetal(), source.getLocation()	);
		

	}
	

}
