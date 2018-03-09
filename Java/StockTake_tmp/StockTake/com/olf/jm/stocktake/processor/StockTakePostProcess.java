package com.olf.jm.stocktake.processor;

import com.olf.jm.stocktake.adjustmentTransaction.AdjustmentTranBuilder;
import com.olf.jm.stocktake.dataTables.EnumTransferData;
import com.olf.jm.stocktake.processor.clientData.StockTakeTransfer;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;



/**
 * The Class StockTakePostProcess. Implements the post process logic of the stock take adjustments.
 * 
 * Using the data captured in the pre process book a COMM-PHYS deal for each adjustment made. 
 */ 
public class StockTakePostProcess extends StockTakeProcessor {
    
    /** The const rep initialised with the correct context / sub context. */
    private ConstRepository constRep;
    
    /**
     * Instantiates a new stock take post process.
     *
     * @param currentSession the current session the script is running in.
     * @param initialisedConstRep the initialised const repository
     */
    public StockTakePostProcess(final Session currentSession,  final ConstRepository initialisedConstRep) {
        super(currentSession);  

        constRep = initialisedConstRep;
    }
    
    /**
     * Main processing method. get the adjustment data and create the trades based off that data and book
     * the trades to a validated status.
     *
     * @param nominations the nominations to process
     * @throws StockTakeException the stock take exception
     */
    public final void postProcess(final Nominations nominations) throws StockTakeException {
        
        for (Nomination nomination : nominations) {
            if (!isValidNomination(nomination)) {
                continue;
            }
                   
            processNomination(nomination);
            
            ((Batch) nomination).save();
        }     
    }
    
    /**
     * Process nomination. Loop over all the entries in the table  USER_jm_stocktake_adjustments 
     * for the current nominations adjustment id.
     *
     * @param nomination the nomination to process
     * @throws StockTakeException the stock take exception
     */
    private void processNomination(final Nomination nomination) throws StockTakeException {
    	try (Field adjustmentIdField = nomination.getField("AdjustmentId")) {
    		if (adjustmentIdField.getValueAsInt() != 0) {
    			processAdjustment(adjustmentIdField.getValueAsInt());
    			
    			// Clear down the adjustment field after processing the changes.
    			adjustmentIdField.setValue(0);
    		} 
    	} catch (Exception e) {
    		throw new RuntimeException("Error processing Nomination. " + e.getMessage());
    	}
    } 
    
    /**
     * Process container. Check if the container has an adjustment id associated with it. If so book 
     * the adjustment transaction and update the status in the user table and info field.
     *
     * @param adjustmentId the id of the adjustment to process
     * @throws StockTakeException the stock take exception
     */
    private void processAdjustment(int adjustmentId) throws StockTakeException {
                
        if (adjustmentId > 0) { 
            updateAdjustmentStatus(adjustmentId, "In Progress");
           // Update user table to in process
           StockTakeTransfer stockTakeTransfer = new StockTakeTransfer(getSession(), adjustmentId);
            
           // Book the transaction
           AdjustmentTranBuilder tranBuilder = new AdjustmentTranBuilder(getSession(), constRep);     
           try {

        	   Transactions adjustments = stockTakeTransfer.populateTransactions(tranBuilder);      
               adjustments.process(EnumTranStatus.Validated);
           } catch (Exception e) {
               updateAdjustmentStatus(adjustmentId, "Error");
               throw new RuntimeException(e);
           }           
            
           // Update the user table to show trade booking status
           updateAdjustmentStatus(adjustmentId, "Done"); 
       }
    }
    
    /**
     * Update adjustment status. Update the status column in the stocktake user table. 
     *
     * @param adjustmentId the adjustment id
     * @param status the status
     */
    private void updateAdjustmentStatus(final int adjustmentId, final String status) {
        IOFactory ioFactory = getSession().getIOFactory();
        
        UserTable stocktakeAdjustment = ioFactory.getUserTable("USER_jm_stocktake_adjustments"); // TODO remove string name
        
        Table update = getSession().getTableFactory().createTable();
        
        update.addColumn(EnumTransferData.ADJUSTMENT_ID.getColumnName(), EnumTransferData.ADJUSTMENT_ID.getColumnType());
        update.addColumn(EnumTransferData.STATUS.getColumnName(), EnumTransferData.STATUS.getColumnType());
        
        update.addRows(1);
        
        update.setInt(EnumTransferData.ADJUSTMENT_ID.getColumnName(), 0, adjustmentId);
        update.setString(EnumTransferData.STATUS.getColumnName(), 0, status);
        
        stocktakeAdjustment.updateRows(update, EnumTransferData.ADJUSTMENT_ID.getColumnName());
    }
    

    
    

}
