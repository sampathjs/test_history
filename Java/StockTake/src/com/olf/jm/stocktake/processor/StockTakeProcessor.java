package com.olf.jm.stocktake.processor;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/**
 * The Class StockTakeProcessor. Base class for stocktake pre and post process classes.
 */
public class StockTakeProcessor {

    /** The current session the script is running in. */
    private Session session;
    


    /**
     * Instantiates a new stock take processor.
     *
     * @param currentSession the current session
     */
    protected StockTakeProcessor(final Session currentSession) {
        session = currentSession;
    }

    /**
     * Checks if the nomination is valid to be processed as a stocktake adjustment. 
     * Only process "Warehouse Inventory" nominations.
     *
     * @param nomination the nomination to validate
     * @return true, if is valid to stocktake adjustments.
     */
    protected final boolean isValidNomination(final Nomination nomination) {
        
        // Check if there is a change in net weight
    	double netQtyChange = nomination
				.getValueAsDouble(EnumNominationFieldId.NetQuantityChange);
    	if (netQtyChange == 0) {
    		return false;
    	}
        
        com.olf.openrisk.scheduling.Field activityId = nomination
                .retrieveField(EnumNomfField.NomCmotionCsdActivityId, 0);
                
        com.olf.openrisk.scheduling.Field status = nomination.getField(EnumNominationFieldId.CMotionDeliveryStatus);
        
        Logging.debug("isValidNomination: [activityId] " + activityId.getValueAsString() 
        		+ "[status] " + status.getValueAsString());
        		//+ "[receiptDealNum] " + receiptDealNum);
        
    	if (activityId.getValueAsString().equals("Warehouse Inventory") 
    			&& !status.getValueAsString().equals("Cancelled")) {
    		
    		
            
            int deliveryId = nomination.getValueAsInt(EnumNominationFieldId.SourceDeliveryId);
            
            Logging.debug("isValidNomination: [deliveryId] " + deliveryId);
            
    		if (isBatchLinked(deliveryId)) {
    			return true;
    		}
    	}
    	
    	return false;
        
    }
    
    /**
     * Gets the session.
     *
     * @return the session
     */
    protected final Session getSession() {
        return session;
    }
    
    /**
     * Checks if the deliver id is part of a linked batch. 
     *
     * @param deliveryId the delivery id to check
     * @return true, if linked batch.
     */
    protected final boolean isBatchLinked(final int deliveryId) {
    	
    	String sql = "select * from comm_sched_deliv_deal where delivery_id  = " + deliveryId 
    			+ " and receipt_delivery = 0";
    	
    	IOFactory ioFactory = session.getIOFactory();
    	
        Logging.debug("About to run SQL. \n" + sql);
        
        
        Table results = null;
        try {
        	results = ioFactory.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        if (results == null || results.getRowCount() != 1) {
            String errorMessage = "Error checking is the recepit is linked.";
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    	
        if (results.getInt("deal_num", 0) == 6) {
        	return false; // value of 6 indicates a unlinked nom, got to love core code.
        }
        
        return true;
    }
}