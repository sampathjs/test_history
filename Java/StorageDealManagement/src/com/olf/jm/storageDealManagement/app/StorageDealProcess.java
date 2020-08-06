package com.olf.jm.storageDealManagement.app;

import java.util.Date;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.storageDealManagement.model.ActivityReport;
import com.olf.jm.storageDealManagement.model.InventoryTransfer;
import com.olf.jm.storageDealManagement.model.StorageDeal;
import com.olf.jm.storageDealManagement.model.StorageDeals;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import  com.olf.jm.logging.Logging;

public class StorageDealProcess {
	
	private Context context;
	
	private Table dealDurations;
	
	public StorageDealProcess(Context currentContext) {
		context = currentContext;
		
		loadDealDurations();
	}

	public void processStorageDeals(Date currentDate) {
		
		// Load the storage deal to process
		StorageDeals storageDeals = new StorageDeals(context);
		
		List<StorageDeal> dealsToProcess = storageDeals.getStorageDeal(currentDate);
		
		Logging.info("About to process " + dealsToProcess.size() + " storage deals.");
		ActivityReport.storageDealToProcess(dealsToProcess.size());
		InventoryTransfer transfer = new InventoryTransfer(context);
		
		for(StorageDeal storageDeal : dealsToProcess) {
			Logging.info("About to process storage deal " + storageDeal);
			
			// Roll the storage deal to the next month
			String dealDuration = getDealDuration(storageDeal);
			
			// Create thew new storage deal, we need to check the unlinked inventory before setting the start date
			Transaction commmStor = storageDeal.generateNextStoreDeal(dealDuration);
			String storageLocation = storageDeal.getLocation();
			String storageMetal = storageDeal.getMetal();
			String excludedDeliveryID = getExcludedDeliveryID(storageLocation, storageMetal); 

			// Move the inventory onto the deal
			transfer.transfer(storageDeal, commmStor,excludedDeliveryID);
			
			Logging.info("Finished process storage deal " + storageDeal);
		}
		
		Logging.info("Finished processing storage deals.");
		
	}
	public void processStorageDeals(Date currentDate,Date targetMatDate, Date localDate, String location, String metal) {
		
		// Load the storage deal to process
		StorageDeals storageDeals = new StorageDeals(context);
		
		List<StorageDeal> dealsToProcess = storageDeals.getStorageDeal(currentDate, location, metal);
		
		Logging.info("About to process " + dealsToProcess.size() + " storage deals.");
		ActivityReport.storageDealToProcess(dealsToProcess.size());
		 
		InventoryTransfer transfer = new InventoryTransfer(context);
		
		int loopCOunt = 0;
		for(StorageDeal storageDeal : dealsToProcess) {
			loopCOunt ++ ;
			Logging.info("About to process storage deal " + storageDeal + " Deal: " + loopCOunt + " of " + dealsToProcess.size());
			
			// Roll the storage deal to the next month
			//String dealDuration = getDealDuration(storageDeal);
			
			// Create thew new storage deal, we need to check the unlinked inventory before setting the start date
			
			Transaction commmStor = storageDeal.generateNextStoreDeal(localDate, targetMatDate); 
			
			// Move the inventory onto the deal
			String storageLocation = storageDeal.getLocation();
			String storageMetal = storageDeal.getMetal();
			String excludedDeliveryID = getExcludedDeliveryID(storageMetal,storageLocation); 
			 
			transfer.transfer(storageDeal, commmStor, excludedDeliveryID);
			
			Logging.info("Finished process storage deal " + storageDeal);
		}
		
		Logging.info("Finished processing storage deals.");
		
	}
	
	private String getDealDuration(StorageDeal storageDeal) {
		
		String metal = storageDeal.getMetal();
		
		String location = storageDeal.getLocation();
		
		String dealDuration = getDealDuration(metal, location);
		
		if(dealDuration != null && dealDuration.length() > 0) {
			return dealDuration;
		}
		
		dealDuration = getDealDuration("*", location);		
		if(dealDuration != null && dealDuration.length() > 0) {
			return dealDuration;
		}
		
		dealDuration = getDealDuration(metal, "*");		
		if(dealDuration != null && dealDuration.length() > 0) {
			return dealDuration;
		}	
		
		dealDuration = getDealDuration("*", "*");		
		if(dealDuration != null && dealDuration.length() > 0) {
			return dealDuration;
		}
		
		String errorMessage = "Invalid entry for metal [" + metal + "] location [" + location  + "]. No match found.";
		Logging.error(errorMessage);
		throw new RuntimeException(errorMessage);
		
	}
	
	private String getDealDuration(String metal, String location ) {
		
		ConstTable result = dealDurations.createConstView("*", "metal == '" + metal + "' and location == '"  + location + "'" + " AND system_default == 0" );
		
		if(result.getRowCount() == 0) {
			return null;
		} else if(result.getRowCount() > 1){
			String errorMessage = "Invalid entry for metal [" + metal + "] location [" + location + "]." + 
									" Expecting 0 or 1 rows but found  " + result.getRowCount() + " rows.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}		
		return result.getString("deal_duration", 0);
	}
	
	private String getExcludedDeliveryID(String metal, String location ) {
		
		ConstTable result = dealDurations.createConstView("*", "metal == '" + metal + "' and location == '"  + location + "'");
		
		if(result.getRowCount() == 0) {
			return "0";
		} else if(result.getRowCount() > 1){
			String errorMessage = "Invalid entry for metal [" + metal + "] location [" + location + "]." + 
									" Expecting 0 or 1 rows but found  " + result.getRowCount() + " rows.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}		
		return result.getString("exclude_delivery_id", 0);
	}
	
	private void loadDealDurations() {
		String sql = "SELECT * FROM USER_jm_comm_stor_mgmt";
		
        IOFactory iof = context.getIOFactory();
        
        Logging.debug("About to run SQL. \n" + sql);
        
        try {
        	dealDurations = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }		
	}	
}
