package com.olf.jm.storageDealManagement.model;

import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.ContainerTransfers;
import com.olf.openrisk.scheduling.SchedulingFactory;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Transaction;
import  com.olf.jm.logging.Logging;

public class InventoryTransfer {
	
	enum TransferType {LINKED, UNLINKED};

	private Context context;
	
	private SchedulingFactory schedulingFactory;
	
	public InventoryTransfer( Context currentContext) {
		context = currentContext;
		
		schedulingFactory = context.getSchedulingFactory();
	}
	
	public void transfer(StorageDeal source, Transaction destination, String excludeDeliveryID, Table logTable) {
		
		// Move linked inventory onto new comm-stor
		transferLinkedInventory(source, destination, excludeDeliveryID, logTable);
		
		// Move unlinked inventory onto new comm-stor
		transferUnlinkedInventory(source, destination, excludeDeliveryID, logTable);			
	}
	
	public void logInventoryEntry(Table logTable, StorageDeal storageDeal, Inventory inventory, String status, String message) {
		logTable.addRow();
		logTable.setString("Location", logTable.getRowCount()-1, storageDeal.getLocation());
		logTable.setString("Metal", logTable.getRowCount()-1, storageDeal.getMetal());
		logTable.setInt("Delivery ID", logTable.getRowCount()-1, inventory.getDeliveryId());
		logTable.setString("Batch Num", logTable.getRowCount()-1, inventory.getBatchNum());
		logTable.setString("Status", logTable.getRowCount()-1, status);
		logTable.setString("Message", logTable.getRowCount()-1, message);
	}
	
	private void transferLinkedInventory(StorageDeal source, Transaction destination, String excludeDeliveryID, Table logTable) {
	
		List<Inventory> inventoryToMove = source.getLinkedReceiptBatches();
		
		if(inventoryToMove.size() == 0) {
			
			Logging.info("No linked inventory to move.");
			return;
		}
		Logging.info("Transfering linked inventory. Moving " + inventoryToMove.size() +  " batches.");
		
		final int masterLocation = inventoryToMove.get(0).getLocationId();
		
		int loopCOunt = 0;
		for (Inventory inventory :inventoryToMove ) {
			loopCOunt ++;
			int deliveryId = inventory.getDeliveryId();
			int locationId = inventory.getLocationId();
			
			boolean foundExcludedDeliveryID = false;
			foundExcludedDeliveryID = isDeliveryIDtobeExlucded(excludeDeliveryID, deliveryId);
			
			if (foundExcludedDeliveryID ){  
				logInventoryEntry (logTable, source, inventory, "Skipped", "Batch skipped as part of the exclusion list (" +excludeDeliveryID + ")" );
				Logging.info("Found Corrupt Linked Inventory DeliverID. " + deliveryId);
			} else {
				Logging.info("Transfering linked inventory. Moving delivery id " + deliveryId + " Count: " + loopCOunt + " of " + inventoryToMove.size());
				
				if(masterLocation != locationId) {
					String errorMessage = "Expecting to move batches for a single location. Expected location " + masterLocation + " but found location " + locationId;
					Logging.error(errorMessage);
					logInventoryEntry (logTable, source, inventory, "Error", "Batch skipped as the batch location ID does match to the master location ID (" +  masterLocation + " != " + locationId + ")");
				}
				
				try(Leg leg = getLocationLeg( destination,  locationId);
					Batch inv = schedulingFactory.retrieveBatchByDeliveryId(deliveryId);
					ContainerTransfers transfer = schedulingFactory.createContainerTransfers(leg)	) {
					
					transfer.addBatch(inv);
					transfer.save();
							
					logInventoryEntry (logTable, source, inventory, "Success", "Batch processed successfully");
					Logging.debug("Transfering inventory batch " + deliveryId + " from deal " + source.getDealTrackingNumber() + " to " + destination.getDealTrackingId());				
				} catch (Exception e) {
					String errorMessage = "Error during transfer of linked inventor. " +  e.getMessage();
					Logging.error(errorMessage);
					logInventoryEntry (logTable, source, inventory, "Error", errorMessage);
				}
			}
		}
		
		validateTransfer(source, destination, inventoryToMove, TransferType.LINKED);
		Logging.info("Transfering linked inventory complete.");
		
	}
	
	private boolean isDeliveryIDtobeExlucded(String excludeDeliveryID, int deliveryId) {
		if (excludeDeliveryID==null || excludeDeliveryID.trim().length()==0 ){
			
		} else {
			excludeDeliveryID = excludeDeliveryID.replaceAll(",", ";");
			String deliveryIDsSplit [] = excludeDeliveryID.split(";");
			int deliveryIDsSplitCount = deliveryIDsSplit.length;
			
			for (int iLoop = 0; iLoop<deliveryIDsSplitCount;iLoop++){
				String thisDeliveryID = deliveryIDsSplit[iLoop].trim();
				if (thisDeliveryID.length()>0){
					int tempDeliveryID = 0;
					try {
						tempDeliveryID = Integer.parseInt(thisDeliveryID);
					} catch (NumberFormatException e) {
					}
					if (tempDeliveryID==deliveryId){
						return true;
					}
				}
			}
		}
		return false;
	}

	 

	private void transferUnlinkedInventory(StorageDeal source, Transaction destination, String excludeDeliveryID, Table logTable) {
		
		List<Inventory> inventoryToMove = source.getUnLinkedReceiptBatches();
		if(inventoryToMove.size() == 0) {
			Logging.info("No unlinked inventory to move.");
			return;
		}
		Logging.info("Transfering unlinked inventory. Moving " + inventoryToMove.size() +  " batches.");
		
		final int masterLocation = inventoryToMove.get(0).getLocationId();
		
		Logging.info("About to move " + inventoryToMove.size() + " unlinked batches.");
		
		int loopCOunt = 0;
		for (Inventory inventory : inventoryToMove) {
			loopCOunt ++; 
			int locationId = inventory.getLocationId();
			int batchDeliveryId = inventory.getDeliveryId();
			boolean foundExcludedDeliveryID = false;
			foundExcludedDeliveryID = isDeliveryIDtobeExlucded(excludeDeliveryID, batchDeliveryId);
			if (foundExcludedDeliveryID){
				logInventoryEntry (logTable, source, inventory, "Skipped", "Batch skipped as part of the exclusion list (" +excludeDeliveryID + ")" );
				Logging.info("Found Corrupt UnLinked Inventory DeliverID. " + batchDeliveryId);				
			} else { 
				Logging.info("Transfering unlinked inventory. Moving delivery id " + batchDeliveryId + " Count: " + loopCOunt + " of " + inventoryToMove.size());
				
				if(masterLocation != locationId) {
					String errorMessage = "Expecting to move batches for a single location. Expected location " + masterLocation + " but found location " + locationId;
					Logging.error(errorMessage);
					logInventoryEntry (logTable, source, inventory, "Error", "Batch skipped as the batch location ID does match to the master location ID (" +  masterLocation + " != " + locationId + ")");
				}
				
				try(Batch batch = schedulingFactory.retrieveBatchByDeliveryId(batchDeliveryId)) {
					Logging.debug("Moving receipt batch " + batchDeliveryId + " from deal " + source.getDealTrackingNumber() + " to " + destination.getDealTrackingId());			
					batch.assignBatchToStorage(destination);
					batch.save();		
					logInventoryEntry (logTable, source, inventory, "Success", "Batch processed successfully");
				} catch (Exception e) {
					String errorMessage = "Error during transfer of unlinked inventor. " +  e.getMessage();
					Logging.error(errorMessage);
					logInventoryEntry (logTable, source, inventory, "Error", errorMessage);
				}
			}
		}
		
		validateTransfer(source, destination, inventoryToMove, TransferType.UNLINKED);
		
		Logging.info("Transfering unlinked inventory complete.");
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
		
		boolean continueON = true;
		for(Inventory inventory : originalInventory) {
			int batchId = inventory.getBatchId();			
			
			if(!newInventory.contains(inventory)) {
				String errorMessage = "Error validating moved inventory. Batch Id " + batchId + " Delivery ID: " + inventory.getDeliveryId() + " is missing from the new storage deal";
				Logging.error(errorMessage);
				continueON = false;
				//throw new RuntimeException(errorMessage);					
			}
		}
		
		if (continueON){
			ActivityReport.transfer(source.getDealTrackingNumber(), 
				destination.getDealTrackingId(), originalInventory.size(), 
				transferType ==  TransferType.LINKED ? "Linked": "Unlinked",
				source.getMetal(), source.getLocation()	);
		}

	}
	

}
