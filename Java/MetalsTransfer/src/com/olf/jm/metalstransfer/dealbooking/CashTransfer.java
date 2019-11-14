package com.olf.jm.metalstransfer.dealbooking;
 
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.Comment;
import com.olf.openrisk.trading.Comments;
import com.olf.openrisk.trading.EnumCommentFieldId;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

/**
 * Cash Transfer booking, books the deal to Validated.
 *  
 * @author gmoore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 12-Nov-2015 |               | G. Moore        | Use instrument to create cash deals instead of a single template.               |
 * | 003 | 23-Nov-2015 |               | G. Moore        | Set the 'Force VAT' tran info field on the cash deal.                           |
 * | 004 | 02-Dec-2015 |               | G. Moore        | No such thing as a 'Do Not Validate' deal anymore so code removed.              |
 * | 005 | 12-Apr-2016 |               | J. Waechter	 | Added method revalidateDeals													   |
 * | 006 | 21-Apr-2016 |               | J. Waechter     | Validation of strategy now in separate method                                   |
 * | 007 | 09-Jun-2016 |               | J. Waechter     | Excluded (child) offset transactions from processing                            |
 * | 008 | 07-Sep-2016 |               | J. Waechter     | Excluded transactions that are in a tran group and it exists a transaction      |
 * |     |             |               |                 | that has a lower tran num.                                                      |
 * | 009 | 13-Sep-2016 |               | J. Waechter     | No longer reprocessing strategies to validated if they are validated already    |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * *
 */
public class CashTransfer implements AutoCloseable {

    private int fromBusinessUnitId;
    private int fromAccountId;
    private int fromPortfolioId;
    private int toBusinessUnitId;
    private int toAccountId;
    private int toPortfolioId;
    private int passThruAccountId;
    private static final int MAX_TRIES = 3;
    
    /**
     * Set the transfer from fields.
     *  
     * @param fromAccountId
     * @param fromBusinessUnitId
     */
    public void setFromFields(int fromAccountId, int fromBusinessUnitId) {
        setFromFields(fromAccountId, fromBusinessUnitId, 0);
    }
    
    /**
     * Set the transfer from fields.
     * 
     * @param fromAccountId
     * @param fromBusinessUnitId
     * @param fromPortfolioId
     */
    public void setFromFields(int fromAccountId, int fromBusinessUnitId, int fromPortfolioId) {
        this.fromBusinessUnitId = fromBusinessUnitId;
        this.fromAccountId = fromAccountId;
        this.fromPortfolioId = fromPortfolioId;
    }

    /**
     * Set the transfer to fields.
     * 
     * @param toAccountId
     * @param toBusinessUnitId
     */
    public void setToFields(int toAccountId, int toBusinessUnitId) {
        setToFields(toAccountId, toBusinessUnitId, 0);
    }
    
    /**
     * Set the transfer to fields.
     * 
     * @param toAccountId
     * @param toBusinessUnitId
     * @param toPortfolioId
     */
    public void setToFields(int toAccountId, int toBusinessUnitId, int toPortfolioId) {
        this.toBusinessUnitId = toBusinessUnitId;
        this.toAccountId = toAccountId;
        this.toPortfolioId = toPortfolioId;
    }

    /**
     * Set the pass-thru account.
     * 
     * @param accountId
     */
    public void setPassThroughAccount(int accountId) {
        this.passThruAccountId = accountId;
    }
    
    /**
     * Book the cash transfer deal to New status taking additional fields data from the strategy deal.
     * 
     * @param session
     * @param strategy
     * @return transaction id
     */
    public int bookDeal(Session session, Transaction strategy) {

        TradingFactory factory = session.getTradingFactory();
		
        // Create a cash transfer deal based on the holding instrument for the unit
        try (Transaction template = factory.retrieveTransactionByReference("Metal Transfer", EnumTranStatus.Template);
                Transaction cash = factory.createTransactionFromTemplate(template)) {
            
            // Set fields from the strategy deal
            cash.setValue(EnumTransactionFieldId.ReferenceString, strategy.getField(EnumTransactionFieldId.ReferenceString).getValueAsString());
            cash.setValue(EnumTransactionFieldId.Position, strategy.getField("Qty").getValueAsDouble());
            cash.getLeg(0).setValue(EnumLegFieldId.Currency, strategy.getField("Metal").getValueAsString());

            // Set the transfer from fields
            cash.setValue(EnumTransactionFieldId.FromAccount, fromAccountId);
            cash.setValue(EnumTransactionFieldId.FromBusinessUnit, fromBusinessUnitId);
        	cash.setValue(EnumTransactionFieldId.FromUnit, strategy.getField("Unit").getValueAsString());

            // Set the transfer to fields, To Unit will default to the same as From Unit
            cash.setValue(EnumTransactionFieldId.ToAccount, toAccountId);
            cash.setValue(EnumTransactionFieldId.ToBusinessUnit, toBusinessUnitId);

            // Set the pass-thru account, must be set before portfolios are set
            if (passThruAccountId > 0)
                cash.setValue(EnumTransactionFieldId.PassThruAccount, passThruAccountId);
            
            if (fromPortfolioId > 0)
                cash.setValue(EnumTransactionFieldId.FromPortfolio, fromPortfolioId);
            if (toPortfolioId > 0)
                cash.setValue(EnumTransactionFieldId.ToPortfolio, toPortfolioId);

            // Record the strategy deal id onto the cash transfer
            setStrategyNum(cash, strategy.getDealTrackingId());
            // Set the Force VAT field
            setForceVat(cash, strategy);
            
            // Set the SAP transfer number
            setSAPMTRNo(cash, strategy);

            // Copy comments from the strategy deal to the cash transfer
            copyDealComments(strategy, cash);

            // Set the dates
            Field tradeDateField = cash.getField(EnumTransactionFieldId.TradeDate);
			if (!tradeDateField.isReadOnly())
				cash.setValue(EnumTransactionFieldId.TradeDate, strategy.getField(EnumTransactionFieldId.TradeDate).getValueAsDate());
            
			Field settleDateField = cash.getField(EnumTransactionFieldId.SettleDate);
			if (!settleDateField.isReadOnly())
				cash.setValue(EnumTransactionFieldId.SettleDate, strategy.getField(EnumTransactionFieldId.SettleDate).getValueAsDate());
            
            // Process to New
			Logging.info ("Processing to New");
			processDealWithDelay (cash, EnumTranStatus.New);
			
            Logging.info ("Processed to New");
            
            return cash.getTransactionId();            
            
        }
    }

    
    
    /**
     * Set the SAP metal transfer number tran info field on the cash deal with the strategy SAP metal transfer number.
     * 
     * @param cash
     * @param strategyNum
     */
    private void setSAPMTRNo(Transaction cash, Transaction strategy) {
        Field field = cash.getField("SAP-MTRNo");
        field.setValue(strategy.getField("SAP-MTRNo").getValueAsString());
    }
    
    /**
     * Set the strategy number tran info field on the cash deal with the strategy deal number.
     * 
     * @param cash
     * @param strategyNum
     */
    private void setStrategyNum(Transaction cash, int strategyNum) {
        Field field = cash.getField("Strategy Num");
        field.setValue(strategyNum);
    }

    /**
     * Set the force VAT tran info field on the cash deal from the strategy deal.
     * 
     * @param cash
     * @param strategy
     */
    private void setForceVat(Transaction cash, Transaction strategy) {
        Field field = cash.getField("Force VAT");
        field.setValue(strategy.getField("Force VAT").getValueAsString());
    }

    /**
     * Copy comments from the strategy deal to the cash transfer.
     * 
     * @param strategy
     * @param cash
     */
    private void copyDealComments(Transaction strategy, Transaction cash) {
        Comments strategyComments = strategy.getComments();
        Comments cashComments = cash.getComments();
        for (Comment comment : strategyComments) {
            Comment newComment = cashComments.addItem();
            for (EnumCommentFieldId field : EnumCommentFieldId.values()) {
                if (field != EnumCommentFieldId.TimeStamp && field != EnumCommentFieldId.User)
                    newComment.setValue(field, comment.getField(field).getValueAsString());
            }
        }
    }

    /**
     * Validate all cash deals associated with the strategy
     * 
     * @param session
     * @param strategy
     */
    public static void validateDeals(Session session, Transaction strategy) {
    	Logging.info("Processing to validated status");
        processCashDeals(session, strategy, EnumTranStatus.New, EnumTranStatus.Validated);
    }

    /**
     * Validate all cash deals associated with the strategy
     * 
     * @param session
     * @param strategy
     */
    public static void validateStrategy(Session session, Transaction strategy) {
    	if (strategy.getTransactionStatus() != EnumTranStatus.Validated) {
            strategy.process(EnumTranStatus.Validated); 
    	}
    }

    

    /**
     * Validate all cash deals associated with the strategy and the strategy deal itself.
     * 
     * @param session
     * @param strategy
     */
    public static void revalidateDeals(Session session, Transaction strategy) {
        processCashDeals(session, strategy, EnumTranStatus.Validated, EnumTranStatus.Validated); 
    }

    
    /**
     * Cancel/delete all cash deals associated with the strategy.
     * 
     * @param session
     * @param strategy
     */
    public static void cancelDeals(Session session, Transaction strategy) {
        processCashDeals(session, strategy, EnumTranStatus.New, EnumTranStatus.Deleted);
        processCashDeals(session, strategy, EnumTranStatus.Validated, EnumTranStatus.Cancelled);
    }

    /**
     * Process cash deals associated with the strategy from one status to another.
     *  
     * @param session
     * @param strategy
     * @param fromStatus
     * @param toStatus
     * @param doNotProcessTranNum Transaction number of deal not to process to validated
     */
    private static void processCashDeals(Session session, Transaction strategy, EnumTranStatus fromStatus, EnumTranStatus toStatus) {
        
        int strategyNum = strategy.getDealTrackingId();
        
        try (Table results = session.getIOFactory().runSQL(
                "\n SELECT ab.tran_num, ab.tran_group, ISNULL(abg.tran_num, -1) AS lower_tran_num" +
                "\n   FROM ab_tran ab" +
                "\n   JOIN ab_tran_info_view ativ ON (ativ.tran_num = ab.tran_num)" +
                "\n   LEFT OUTER JOIN ab_tran abg ON (abg.tran_group = ab.tran_group" +
                "\n     AND abg.tran_num < ab.tran_num)" +
                "\n  WHERE ativ.type_name = 'Strategy Num'" +
                "\n    AND value = '" + strategyNum + "'" +
                "\n    AND ab.offset_tran_type IN (0, 1)" + // 0 = "No Offset", 1 = Original Offset
                "\n    AND ab.tran_status = " + fromStatus.getValue())) {
        	
        	int deals = results.getRowCount();
        	
        	Logging.info ("Deals to process: " + deals);
        	
            for (TableRow row : results.getRows()) {
                int tranNum = row.getInt(0);
                int lowerTranNum = row.getInt(2);
                
                try (Transaction cash = session.getTradingFactory().retrieveTransactionById(tranNum)) {
                    // Check status of deal hasn't changed since sql run as it may be part of an offset deal
//    				Field offsetTranTypeField = cash.getField(EnumTransactionFieldId.OffsetTransactionType);
//    				String offsetTranType = (offsetTranTypeField != null && offsetTranTypeField.isApplicable())?
//    						offsetTranTypeField.getValueAsString():"";
    				Field transferTypeField = cash.getField(EnumTransactionFieldId.AccountTransferType);
    				int transferType = (transferTypeField != null && transferTypeField.isApplicable())?
    						transferTypeField.getValueAsInt():-1;

                    if (cash.getTransactionStatus() == fromStatus
//                    	&& (offsetTranType.equals("No Offset") || offsetTranType.equals("Original Offset")
//                    	    || offsetTranType.equals(""))
                    	&& ((transferType == 0 || transferType == 3 || transferType == -1) 
                    	&& lowerTranNum == -1)
                    		) {
                        Logging.info("Processing #" + row.getNumber() + " - cash deal transaction " + tranNum + " from: " + fromStatus + " to " + toStatus);
                        processDealWithDelay (cash, toStatus);
                        }                    
                } 
            } 
        } 
    } 
    
    /***
     * 
     * Sleep for stipulated seconds
     * 
     * @param seconds Seconds to sleep
     */
    private static void sleepFor (int seconds) {
        try {
        	Thread.sleep(seconds * 1000);
        } catch ( InterruptedException ie) {
            Logging.error("Could not sleep for " + seconds + " seconds", ie);                	
        }
    }
    
    /**** Process deal
     * 
     * @param cash Transaction to process
     * @param toStatus Status to process
     */
    private static void processDealWithDelay (Transaction cash, EnumTranStatus toStatus) {
    	/*** Process the deal. Try it for configured number of times. Any exception, log it.  
			 This is temporary solution only to fix the issue of exception access violation ***/
        for (int i=0; i < MAX_TRIES;i++) {
        	Logging.info("Trying to  process. Try: " + i);
            try {  
            	sleepFor (15);
                cash.process(toStatus);
                sleepFor (15);
                break;
            }
            catch (Exception e) {
            	Logging.error("Unable to process transaction", e);
            	if (i == MAX_TRIES)
            		throw new RuntimeException (e);
            }
        }
    }
    
    public void close() { 
    	;
    }
} 
