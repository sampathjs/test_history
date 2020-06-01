package com.olf.jm.stocktake.processor.clientData;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.stocktake.adjustmentTransaction.IAdjustmentTranBuilder;
import com.olf.jm.stocktake.dataTables.EnumClientData;
import com.olf.jm.stocktake.dataTables.EnumTransferData;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import  com.olf.jm.logging.Logging;


/**
 * The Class StockTakeTransfer. Class containing all the adjustments made during a stocktake. 
 */
public class StockTakeTransfer {

    /** The stock take adjustments. */
    private List<StockTakeTransferData> stockTakeAdjustments;
    
    /** The session the script is running in. */
    private Session session;
    
    /**
     * Instantiates a new stock take transfer.
     *
     * @param currentSession the current session
     */
    public StockTakeTransfer(final Session currentSession) {
        stockTakeAdjustments = new ArrayList<StockTakeTransferData>();
        
        session = currentSession;
    }
    
    
    /**
     * Adds a new adjustment to the list.
     *
     * @param adjustment the adjustment to add.
     */
    public final void addTransferData(final StockTakeTransferData adjustment) {
        stockTakeAdjustments.add(adjustment);
    }
    
    /**
     * Instantiates a new stock take transfer. Initialise the list based on the
     * data in the clientData table
     *
     * @param currentSession the current session the script is running in.
     * @param adjustmentId the adjustment id identifing the data to load from the user table.
     */
    public StockTakeTransfer(final Session currentSession, final int adjustmentId) {
        this(currentSession);
        
        Table adjustmentData = loadAdjustmentData(adjustmentId);
        
        for (TableRow row : adjustmentData.getRows()) {
            StockTakeTransferData adjustment = new StockTakeTransferData(row);
            
            addTransferData(adjustment);
        }
    }      
    
    /**
     * Populate the table with all the adjustments found.  
     * 
     * The resulting table is saved to the session client data.
     *
     */
    public final void populateTable() {

        Table transferData = session.getTableFactory().createTable(EnumClientData.STOCKTAKE_TRANSFER_DATA.getColumnName());
        
        for (EnumTransferData column : EnumTransferData.values()) {
        	if (column.getColumnType() !=  EnumColType.Table) {
				transferData.addColumn(column.getColumnName(), column.getColumnType());
			}
        }
        
        for (StockTakeTransferData adjustment : stockTakeAdjustments) {
            adjustment.addToTable(transferData);
        }
        

        UserTable stocktakeAdjustments = session.getIOFactory().getUserTable("USER_jm_stocktake_adjustments"); // TODO remove string name
        
        stocktakeAdjustments.insertRows(transferData);
        
       
        Logging.debug("Adjustment transfer data: " + transferData.asXmlString());
        
    }
    
    /**
     * Construct a collection of transaction representing the adjustments made during the stocktake.
     * Each adjustment is represented by a single transaction. 
     *
     * @param tranBuilder the tran builder used to construct the transactions.
     * @return the transactions
     */
    public final Transactions populateTransactions(final IAdjustmentTranBuilder tranBuilder) {
        Transactions tranCollection = buildCollection();
        
        for (StockTakeTransferData adjustment : stockTakeAdjustments) {
            Transaction adjustmentTran = tranBuilder.getDefaultTran();
            
            tranBuilder.setAdjustmentData(adjustmentTran, adjustment);
            
            tranCollection.add(adjustmentTran);
        }
        
        return tranCollection;
    }
    
    /**
     * Builds an empty transaction collection.
     *
     * @return the transactions
     */
    private Transactions buildCollection() {
        TradingFactory tradingFactory = session.getTradingFactory();
        
        Transactions tranCollection = tradingFactory.createTransactions();
        
        return tranCollection;
    }
    
    /**
     * Load adjustment data from the user table.
     *
     * @param adjustmentId the adjustment id
     * @return the table
     */
    private Table loadAdjustmentData(final int adjustmentId) {
        String sql = "SELECT * FROM USER_jm_stocktake_adjustments WHERE adjustment_id = " + adjustmentId;
        
        
        IOFactory iof = session.getIOFactory();
        
        Logging.debug("About to run SQL. \n" + sql);
        
        
        Table adjustmentData = null;
        try {
            adjustmentData = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        
        return adjustmentData;       
    }
    
    /** Is there adjustment data to process.
     *
     * @return true, if there is data to process
     */
    public final boolean dataToProcess() {
    	return stockTakeAdjustments.size() > 0;
    }
    
    /**
     * Gets the a list of all the internal portfolios used in the adjustments.
     *
     * @return the internal portfolios
     */
    public final List<String> getInternalPortfolios() {
    	ArrayList<String> portfolios = new ArrayList<String>();
    	for (StockTakeTransferData adjustment : stockTakeAdjustments) {
    		String portfolio = adjustment.getInternalPortfolio();
    		
    		if (!portfolios.contains(portfolio)) {
    			portfolios.add(portfolio);
    		}
    	}
    	
    	return portfolios;
    }
    
}
