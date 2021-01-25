package com.olf.jm.stocktake.adjustmentTransaction;

import com.olf.jm.stocktake.processor.StockTakeException;
import com.olf.jm.stocktake.processor.clientData.StockTakeTransferData;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.olf.jm.logging.Logging;


/**
 * The Class AdjustmentTranBuilder. Builder class to convert a stock take adjustment data into a
 * transaction that can be booked in the database.
 */
public class AdjustmentTranBuilder implements IAdjustmentTranBuilder {

    /** The default template name used to create the transaction. */
    private String defaultTemplate;
    
    /** The current session. */
    private Session session;
    
    /** StockTake Post process ConstRepository object **/
    private ConstRepository constRepo;
    
    /**
     * Instantiates a new adjustment tran builder.
     *
     * @param currentSession the current session
     * @param constRep the initialised const repository
     * @throws StockTakeException the stock take exception on init errors.
     */
    public AdjustmentTranBuilder(final Session currentSession, final ConstRepository constRep) throws StockTakeException {
        defaultTemplate = "Stock_Take"; 
        
        try {
        	this.constRepo = constRep;
            defaultTemplate = constRep.getStringValue("templateName", defaultTemplate);
        } catch (ConstantTypeException | ConstantNameException | OException e) {
            String errorMessage = "Error looking up the stock take adjustment template. " + e.getMessage();
            Logging.error(errorMessage);
            throw new StockTakeException(errorMessage);
        }
        
        session = currentSession;
    }
    
    /* (non-Javadoc)
     * @see com.olf.jm.stocktake.adjustmentTransaction.IAdjustmentTranBuilder#getDefaultTran()
     */
    @Override
    public final Transaction getDefaultTran() {

        Transaction adjustmentDeal = session.getTradingFactory().createTransactionFromTemplate(getTemplateTransactionId());
        
        return adjustmentDeal;
    }

    /* (non-Javadoc)
     * @see com.olf.jm.stocktake.adjustmentTransaction.IAdjustmentTranBuilder#
     * setAdjustmentData(com.olf.openrisk.trading.Transaction, 
     *                   com.olf.jm.stocktake.processor.clientData.StockTakeTransferData)
     */
    @Override
    public final void setAdjustmentData(final Transaction tranToPopulate,
            final StockTakeTransferData adjustment) {
        adjustment.addToTrade(tranToPopulate, this.constRepo);
    }
    
    /**
     * Gets the template transaction id.
     *
     * @return the template transaction id
     */
    private int getTemplateTransactionId() {
        String sql = "SELECT tran_num FROM ab_tran \n" 
                + "WHERE reference = '" + defaultTemplate + "' \n"
                + "AND tran_status = '15' \n"
                + "AND current_flag = 1 ";
        
        IOFactory iof = session.getIOFactory();
        
        Logging.debug("About to run SQL. \n" + sql);
        
        
        Table templateData = null;
        try {
            templateData = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        if (templateData.getRowCount() != 1) {
            String errorMessage = "Error loading template: " + defaultTemplate + ". expected 1 row but found " + templateData.getRowCount();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);           
        }
        
        return templateData.getInt(0, 0);
    }

}
