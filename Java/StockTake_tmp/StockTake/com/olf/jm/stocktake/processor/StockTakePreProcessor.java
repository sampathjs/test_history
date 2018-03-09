package com.olf.jm.stocktake.processor;


import java.util.List;

import com.olf.jm.stocktake.processor.clientData.StockTakeTransfer;
import com.olf.jm.stocktake.processor.clientData.StockTakeTransferData;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.Portfolio;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.DeliveryTickets;
import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/**
 * The Class StockTakePreProcessor. Implements the logic for the pre process script in the 
 * stock take process. Loop though all the nominations looking for differences in the net weight.
 * 
 * For each adjustment found enrich the data from the batch and create a row in the user table.
 */
public class StockTakePreProcessor extends StockTakeProcessor {
	
	/**
	 * The Class SeqNumber. Represents the number assigned to each adjustment made to a nomination
	 */
	public class SeqNumber {
		/** Value of the current sequence. */
		private int count;
		
		/**
		 * Instantiates a new seq number.
		 */
		public SeqNumber() {
			reset();
		}
		
		/**
		 * Gets the next value in the sequence.
		 *
		 * @return the next value
		 */
		public final int getNextValue() {
			return ++count;
		}
		
		/**
		 * Reset the sequence to 0.
		 */
		public final void reset() {
			count = 0;
		}
	}
    
    /** The stock take transfer. Contains all the adjustment details. */
    private StockTakeTransfer stockTakeTransfer;
	
    /** the sequence number used for each adjustment with in a nomination. */
	private SeqNumber seqNumber;
	
	/** Flag to indicate if this nomination containes deleted containers. */
	private boolean containsDeletedContainers;
    
    /**
     * Instantiates a new stock take pre processor.
     *
     * @param currentSession the current session
     * @param currentConstRep the current const repository
     */
    public StockTakePreProcessor(final Session currentSession, final ConstRepository currentConstRep) {
        super(currentSession);
        
        stockTakeTransfer = new StockTakeTransfer(getSession());
               
        seqNumber = new SeqNumber();
        
        containsDeletedContainers = false;
    }

    /**
     * Pre process logic.
     *
     * @param clientData the client data from the pre method.
     * @param nominations the nominations from the pre method.
     * @param originalNominations the original nominations from the pre method.
     * @throws StockTakeException the stock take exception
     */
    public final void preProcess(final Table clientData, final Nominations nominations, 
            final Nominations originalNominations) throws StockTakeException {

        for (Nomination nomination : nominations) {
        	seqNumber.reset();
        	// Reset the adjustment id
        	try (Field adjustmentId = nomination.getField("AdjustmentId")) {
        		adjustmentId.setValue(0);
        	}
        	
            if (!isValidNomination(nomination)) {
                continue;
            }
                   
            processNomination(nomination, originalNominations);
        }
        
       
        if (stockTakeTransfer.dataToProcess()) {
        try {
            int result = 0;
            String displayMessage = "Warning: The selected changes will generate a Stock Take Adjustment. Would you like to continue? ";
                       
            result = Ask.okCancel(displayMessage);
            
            if (result == 0) {
                throw new StockTakeException("User canceled the opperation.");
            } 
            
        } catch (OException e) {
            String errorMessage = "Error getting the user input. " + e.getMessage();
            PluginLog.error(errorMessage);
            throw new StockTakeException(errorMessage);
        }
     
        validateUser();
        
        stockTakeTransfer.populateTable();
        } 
    }
    
    /**
     * Validate user. Check that the user has the correct permissions. 
     *
     * @throws StockTakeException is the user does not have the correct permissions.
     */
    private void validateUser() throws StockTakeException {
    	Portfolio[] porfolios = this.getSession().getUser().getPortfolios();
    	
    	List<String> stockTakePorfolios = stockTakeTransfer.getInternalPortfolios();
    	
    	// Check that the user belongs to one of the stocktake portfolios
    	for (Portfolio porfolio : porfolios) {
    		if (stockTakePorfolios.contains(porfolio.getName())) {
    			return;
    		}
    	}
        StringBuilder sb = new StringBuilder();
        for (String n : stockTakePorfolios) { 
            sb.append("'").append(n).append("',");
        }
        
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
               
    	// User does not belong to the correct portfolios
        String errorMessage = "Error validating user. User does not belong to one of the following portfolios " + sb.toString();
     
        PluginLog.error(errorMessage);
        throw new StockTakeException(errorMessage);   	
    	
    }
       
    /**
     * Process nomination. Loop over all the nominations passed to the ops service looking 
     * for volume adjustments.
     *
     * @param nomination the nomination
     * @param originalNominations the original nominations
     * @throws StockTakeException the stock take exception
     */
    private void processNomination(final Nomination nomination, final Nominations originalNominations) throws StockTakeException {
        Batch batch = (Batch) nomination;
        DeliveryTickets deliveryTickets = batch.getBatchContainers();
        
        validateContainer(nomination.getId(), deliveryTickets, originalNominations);
        
        for (DeliveryTicket deliveryTicket : deliveryTickets) {      
            processContainer(nomination, deliveryTicket, originalNominations);
        }  
        
        if (containsDeletedContainers) {
        	processDeletedContainer(nomination, originalNominations);
        }
    }
    
    /**
     * Process container. Loop over all the containers attached to a nomination looking
     * for volume adjustments. 
     *
     * @param nomination the nomination after adjustment
     * @param deliveryTicket the delivery ticket
     * @param originalNominations the original nominations before adjustment
     * @throws StockTakeException the stock take exception
     */
    private void processContainer(final Nomination nomination, final DeliveryTicket deliveryTicket, 
            final Nominations originalNominations) throws StockTakeException {
        Nomination originalNomination = getNominationById(nomination.getId(), originalNominations);
        
        if (originalNomination == null) {
            String errorMessage = "Error loading original nomination for id " + nomination.getId();
            PluginLog.info(errorMessage);
           
            return;
        }  
        
        
        int ticketId = deliveryTicket.getValueAsInt(EnumDeliveryTicketFieldId.SystemTicketId);
        
        DeliveryTicket originalTicket = getDeliveryTicketById(ticketId, originalNomination);
        
        double originalValue;
        if (originalTicket == null) {
            String errorMessage = "Error loading original container for id " + nomination.getId() + " container " + ticketId;
            PluginLog.info(errorMessage);
            //deliveryTicket.getField("AdjustmentId").setValue(0);
            //return;
        	originalValue = 0;
        } else {
        	originalValue = originalTicket.getValueAsDouble(EnumDeliveryTicketFieldId.Volume);
        }
        
        double newValue = deliveryTicket.getValueAsDouble(EnumDeliveryTicketFieldId.Volume);
        
        if (originalValue != newValue) {
            processWeightChange(originalValue, newValue, deliveryTicket, originalNomination, nomination);
        } 
    }
    
    /**
     * Process weight change. If an adjustment in the volume is detected add it to the transfer list.
     *
     * @param originalValue the original value
     * @param newValue the new value
     * @param deliveryTicket the delivery ticket
     * @param originalNomination the original nomination before adjustment
     * @param newNomination the new nomination after adjustment
     */
    private void processWeightChange(final double originalValue, final double newValue, 
            final DeliveryTicket deliveryTicket, final Nomination originalNomination, final Nomination newNomination) {
        StockTakeTransferData adjustment = new StockTakeTransferData(originalValue,  newValue,  deliveryTicket, newNomination);
        
        // Only proceed with alteration of existing storage deal, if a default SI rule exists for the new COMM-PHYS deal.
        if (adjustment.getDefaultSIRulesExist()) {
        	
        	int nextSeqNumber = seqNumber.getNextValue();
        	adjustment.setSeqNumber(nextSeqNumber);
        	stockTakeTransfer.addTransferData(adjustment);        
                       
            PluginLog.debug("Transfer data " + adjustment.toString());
        }
    }
    
    /**
     * Gets the original nomination from a collection based on the nomination id.
     *
     * @param nominationId the nomination id to look up
     * @param nominations the original nominations
     * @return the original nom matching the nom id.
     */
    private Nomination getNominationById(final int nominationId, final Nominations nominations) {
        for (Nomination nomination : nominations) {
            if (nomination.getId() == nominationId) {
                return nomination;
            }
        }
        
        return null;
    }
    
    /**
     * Gets the original delivery ticket from a collection based on the id.
     *
     * @param ticketId the ticket id to look up
     * @param originalNomination the original nomination to select ticket from
     * @return the delivery ticket by id
     */
    private DeliveryTicket getDeliveryTicketById(final int ticketId, final Nomination originalNomination) {
        
        Batch batch = (Batch) originalNomination;
        DeliveryTickets deliveryTickets = batch.getBatchContainers();
        
        for (DeliveryTicket deliveryTicket : deliveryTickets) {
            if (deliveryTicket.getValueAsInt(EnumDeliveryTicketFieldId.SystemTicketId) == ticketId) {
                return deliveryTicket;
            }
        }
		return null;
        
    }
    
    /**
     * Validate container.
     *
     * @param nominationId the nomination id
     * @param deliveryTickets the delivery tickets
     * @param originalNominations the original nominations
     * @throws StockTakeException if the container fails validation
     */
    private void validateContainer(final int nominationId, final DeliveryTickets deliveryTickets, 
            final Nominations originalNominations) throws StockTakeException {
    	
        Nomination originalNomination = getNominationById(nominationId, originalNominations);
        
        if (originalNomination == null) {
            String errorMessage = "Error loading original nomination for id " + nominationId;
            PluginLog.error(errorMessage);
            throw new StockTakeException(errorMessage);
        } 
        
        Batch batch = (Batch) originalNomination;
        DeliveryTickets originalDeliveryTickets = batch.getBatchContainers();      
        
        // As part of EPMM-2041 allow deleted containers to generate a stock take adjustment
        if (deliveryTickets.getCount() < originalDeliveryTickets.getCount()) {
        	containsDeletedContainers = true;
        	PluginLog.info("Processing nomination that contains deleted containers.");
        //    String errorMessage = "Deleting of containers is not allowed.";
        //    PluginLog.error(errorMessage);
        //    throw new StockTakeException(errorMessage);        	
        } else {
        	containsDeletedContainers = false;
        }
        
    }
    
    /**
     * Process deleted container and generate a stock take adjustment.
     *
     * @param nomination the nomination
     * @param originalNominations the original nominations
     * @throws StockTakeException the stock take exception
     */
    private void processDeletedContainer(final Nomination nomination, final Nominations originalNominations) throws StockTakeException {
        Nomination originalNomination = getNominationById(nomination.getId(), originalNominations);
        
        if (originalNomination == null) {
            String errorMessage = "Error loading original nomination for id " + nomination.getId();
            PluginLog.info(errorMessage);
           
            return;
        }  
        
        Batch batch = (Batch) originalNomination;
        DeliveryTickets originalDeliveryTickets = batch.getBatchContainers();   
        
        for (DeliveryTicket originalTicket : originalDeliveryTickets) {  
        	int ticketId = originalTicket.getValueAsInt(EnumDeliveryTicketFieldId.SystemTicketId);
        
        	DeliveryTicket newTicket = getDeliveryTicketById(ticketId, nomination);
        
        
        	if (newTicket == null) {
        		// Deleted Container
        		Double originalValue = originalTicket.getValueAsDouble(EnumDeliveryTicketFieldId.Volume);
        		processWeightChange(originalValue, 0, originalTicket, originalNomination, nomination);
        	} 
        }    
    }
}
