package com.olf.jm.storageDealManagement.model;

public class Inventory {

	private int deliveryId;	
	private int locationId;
	private int batchId;
	private String batchNum;
	
	public Inventory(int currentDeliveryId, int currentLocationId, int currentBatchId, String currentBatchNum) {
		deliveryId = currentDeliveryId;
		locationId = currentLocationId;
		batchId = currentBatchId;
		batchNum = currentBatchNum;
	}
	public int getDeliveryId() {
		return deliveryId;
	}
	
	public int getLocationId() {
		return locationId;
	}
	
	public int getBatchId() {
		return batchId;
	}	
	
	public String getBatchNum() {
		return batchNum;
	}

	@Override
	public String toString() {
		return "Delivery Id [" + deliveryId +"] location [" + locationId + "] batch ["  + batchId + "] batchNum ["  + batchNum + "]";
	}
	
	   @Override
	    public boolean equals(Object object)
	    {
	        boolean sameSame = false;

	        if (object != null && object instanceof Inventory)
	        {
	            sameSame = (this.batchId == ((Inventory) object).batchId) 
	            		&& (this.locationId == ((Inventory) object).locationId);
	        }

	        return sameSame;
	    }
}
