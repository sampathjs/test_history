package com.olf.jm.stocktake.processor.clientData;

import  com.olf.jm.logging.Logging;
import com.olf.jm.stocktake.dataTables.EnumTransferData;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.BackOfficeFactory;
import com.olf.openrisk.backoffice.SettlementInstruction;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumReceiptFieldId;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.DeliveryType;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.Party;
import com.olf.openrisk.staticdata.Pipeline;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;


/**
 * The Class StockTakeTransferData. Represents a single adjustment made during the stocktake.
 */
public class StockTakeTransferData {

    /** The change made during the adjsutment, name be positive or negative. */
    private double delta;
    
	/** The location. */
	private String location;
	
	/** The purify. */
	private String purify;
	
	/** The product. */
	private String product;
	
	/** The brand. */
	private String brand;
	
	/** The form. */
	private String form;
	 
	/** The batch id. */
	private String batchId;

	/** The container id. */
	private String containerId;

	/** The initial volume. */
	private double initialVolume;

	/** Id for the adjustment. */
	private int adjustmentId;
	
	/** The status of the adjustment, new, in progress, done and error. */
	private String status;
	
	/** Holds the default settlement instructions, derived from User_JM_StockTake_Account_Map. **/
	private Table defaultSI;
	
	/** Holds the currency. **/
	private Currency currency;
	
	/** Holds the party. **/
	private Party bunit;
	
  	/** Holds the delivery_type. **/
  	private DeliveryType deliveryType;
    
  	/** the adjustment number for a given nomination, allows multiple adjustmemts 
  	 * on a single nomination. **/
  	
  	private int seqNumber;

    /**
     * Instantiates a new stock take transfer data.
     *
     * @param originalWeight the original weight
     * @param newWeight the new weight
     * @param deliveryTicket the delivery ticket
     * @param nomination the nomination
     * @param seqNumberToUse the seq number to use
     */
    public StockTakeTransferData(final double originalWeight, final double newWeight, 
            final DeliveryTicket deliveryTicket, final Nomination nomination, final int seqNumberToUse) {
        initialVolume = originalWeight;
        delta = newWeight - originalWeight;
        
        batchId = deliveryTicket.getValueAsString(EnumDeliveryTicketFieldId.BatchNumber);
        
        containerId = deliveryTicket.getValueAsString(EnumDeliveryTicketFieldId.Id);
        
        location = nomination.getReceipt().getField(EnumReceiptFieldId.LocationId).getValueAsString();
        
        purify = nomination.retrieveField(EnumNomfField.NomCmotionCsdMeasureGroupId, 0).getValueAsString();
        
        form = nomination.retrieveField(EnumNomfField.NominationCsdForm, 0).getValueAsString();
        
        product = nomination.retrieveField(EnumNomfField.NomCmotionCsdCategoryId, 0).getValueAsString();
        
        brand = nomination.retrieveField(EnumNomfField.NominationCsdBrand, 0).getValueAsString();
        
    	// Reset the adjustment id
    	try (Field adjustmentIdField = nomination.getField("AdjustmentId")) {
    		if (adjustmentIdField.getValueAsInt() == 0) {
    			adjustmentId = getNextAdjustmentId();
    			adjustmentIdField.setValue(adjustmentId);
    		} else {
    			adjustmentId = adjustmentIdField.getValueAsInt();
    		}
    	}
        
        
        status = "New"; // TODO create enum
        defaultSI = getDefaultSI();
        
        seqNumber = seqNumberToUse;
    }
    /**
     * Instantiates a new stock take transfer data. Default the sequence number to 0.
     *
     * @param originalWeight the original weight
     * @param newWeight the new weight
     * @param deliveryTicket the delivery ticket
     * @param nomination the nomination
     */
    public StockTakeTransferData(final double originalWeight, final double newWeight, 
            final DeliveryTicket deliveryTicket, final Nomination nomination) {
    	this(originalWeight, newWeight, deliveryTicket, nomination, 0);
    }    
    
    /**
     * Sets the seq number to use with this stock take adjustment.
     *
     * @param newSeqNumber the new seq number
     */
    public final void setSeqNumber(final int newSeqNumber) {
    	seqNumber = newSeqNumber;
    }

    /**
     * Instantiates a new stock take transfer data.
     *
     * @param transferData the transfer data
     */
    public StockTakeTransferData(final TableRow transferData) {
    	
        batchId = transferData.getString(EnumTransferData.BATCH_ID.getColumnName());
        
        containerId = transferData.getString(EnumTransferData.CONTAINER_ID.getColumnName());
        location = transferData.getString(EnumTransferData.LOCATION.getColumnName());
        purify = transferData.getString(EnumTransferData.PURITY.getColumnName());
        product = transferData.getString(EnumTransferData.PRODUCT.getColumnName());
        form = transferData.getString(EnumTransferData.FORM.getColumnName());
        brand = transferData.getString(EnumTransferData.BRAND.getColumnName());
        initialVolume = transferData.getDouble(EnumTransferData.INITIAL_VOLUME.getColumnName());
        delta = transferData.getDouble(EnumTransferData.DELTA.getColumnName());   
        
        adjustmentId = transferData.getInt(EnumTransferData.ADJUSTMENT_ID.getColumnName());
        
        status = transferData.getString(EnumTransferData.STATUS.getColumnName());
        
        seqNumber = transferData.getInt(EnumTransferData.SEQ_NUMBER.getColumnName());
        
        defaultSI = getDefaultSI();
    }
    
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		StringBuffer output = new StringBuffer();
		
		output.append("Adjustment Id [").append(adjustmentId).append("] ");
		output.append("Batch Id [").append(batchId).append("] ");
		output.append("Container Id [").append(containerId).append("] ");
		output.append("Location [").append(location).append("] ");
		output.append("Purify [").append(purify).append("] ");
		output.append("Product [").append(product).append("] ");
		output.append("Form [").append(form).append("] ");
		output.append("Brand [").append(brand).append("] ");
		output.append("Initial Value [").append(initialVolume).append("] ");
		output.append("Delta [").append(delta).append("] ");
		output.append("Status [").append(status).append("] ");
		output.append("Sequence Num [").append(seqNumber).append("] ");
		
		return output.toString();
	}
	
	/**
	 * Adds the adjustment data to a table adding a new row.
	 *
	 * @param tableToAddTo the table to add data to
	 */
	public final void addToTable(final Table tableToAddTo) {
        int newRow = tableToAddTo.addRows(1);
        
        tableToAddTo.setInt(EnumTransferData.ADJUSTMENT_ID.getColumnName(), newRow, adjustmentId);
        tableToAddTo.setString(EnumTransferData.BATCH_ID.getColumnName(), newRow, batchId);
        tableToAddTo.setString(EnumTransferData.CONTAINER_ID.getColumnName(), newRow, containerId);
        tableToAddTo.setString(EnumTransferData.LOCATION.getColumnName(), newRow, location);
        tableToAddTo.setString(EnumTransferData.PURITY.getColumnName(), newRow, purify);
        tableToAddTo.setString(EnumTransferData.PRODUCT.getColumnName(), newRow, product);
        tableToAddTo.setString(EnumTransferData.FORM.getColumnName(), newRow, form);
        tableToAddTo.setString(EnumTransferData.BRAND.getColumnName(), newRow, brand);
        tableToAddTo.setDouble(EnumTransferData.INITIAL_VOLUME.getColumnName(), newRow, initialVolume);
        tableToAddTo.setDouble(EnumTransferData.DELTA.getColumnName(), newRow, delta);	  
        tableToAddTo.setString(EnumTransferData.STATUS.getColumnName(), newRow, status);
        tableToAddTo.setInt(EnumTransferData.SEQ_NUMBER.getColumnName(), newRow, seqNumber);
    }
    
    /**
     * Update a transaction applying the adjustment data.
     *
     * @param transaction the transaction to set data on.
     */
    public final void addToTrade(final Transaction transaction, final ConstRepository constRepo) {
        
        // Set Info Fields
        transaction.getField("ST-Batch ID").setValue(batchId);
        transaction.getField("ST-Container ID").setValue(containerId);
        transaction.getField("ST-Initial Volume").setValue(initialVolume);
        transaction.getField("ST-ID").setValue(adjustmentId);
         
        Leg physicalLeg = transaction.getLeg(1);        
        if (physicalLeg == null) {
            throw new RuntimeException("Error getting the physical leg for the adjustment deal.");
        }

        physicalLeg.setValue(EnumLegFieldId.CommoditySubGroup, product);
        
        // Check the region on the deal matches the region off the template, better know as pipeline_id
        int regionId = checkLocationIsValidForRegion(physicalLeg.getField(EnumLegFieldId.Pipeline).getId());
        if (regionId > 0) {
        	physicalLeg.setValue(EnumLegFieldId.Pipeline, regionId);
        }
        physicalLeg.setValue(EnumLegFieldId.Location, location);        
        physicalLeg.setValue(EnumLegFieldId.CommodityForm, form);
        physicalLeg.setValue(EnumLegFieldId.CommodityBrand, brand);        
        physicalLeg.setValue(EnumLegFieldId.MeasureGroup, purify);
        
        Session session = Application.getInstance().getCurrentSession();
        physicalLeg.setValue(EnumLegFieldId.StartDate, session.getBusinessDate());
        physicalLeg.setValue(EnumLegFieldId.MaturityDate, session.getBusinessDate());
        
        // If the delta is negative the trade is buy, positive is a sell.
        if (delta < 0) {
            transaction.setValue(EnumTransactionFieldId.BuySell, EnumBuySell.Buy.getName());
            physicalLeg.setValue(EnumLegFieldId.DailyVolume, Math.abs(delta));
        } else {
            transaction.setValue(EnumTransactionFieldId.BuySell, EnumBuySell.Sell.getName());
            physicalLeg.setValue(EnumLegFieldId.DailyVolume, delta);
        }
        
        // Set settlement instructions, deal level
        assignDefaultSettlementaInstructions(transaction);
        
        //Additional logic for JM PMM CN stock take trades
        addLogicForShanghai(transaction, constRepo);
    }
    
    /**
     * This method is used to made specific updates to StockTake trade for JM PMM CN 
     * like updates Unit field to gms instead of TOz.
     * 
     * @param transaction
     */
	private void addLogicForShanghai(final Transaction transaction, final ConstRepository constRepo) {
		try {
			String bUnitCR = constRepo.getStringValue("CN_BusUnit", "JM PMM CN");
			String unitCR = constRepo.getStringValue("CN_TranUnit", "gms");
			String intBU = transaction.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit);
	        if (intBU != null && bUnitCR.equals(intBU)) {
	        	if(transaction.getLegCount() >0){
	        		Leg leg = transaction.getLeg(1);
	        	if (leg.getField(EnumLegFieldId.Unit).isApplicable() && !leg.getField(EnumLegFieldId.Unit).isReadOnly()) {
	        		leg.setValue(EnumLegFieldId.Unit, unitCR);
	        		Logging.info("Unit field updated for CN StockTake trade to gms");
	        	} else {	        		
	        		Logging.info("Can't update Unit field for CN StockTake trade as it's not applicable or read only");
	        		
	        	}
	        	}
	        }
		} catch(OException oe) {
			Logging.error("Error in updating Unit field for CN, Message - " + oe.getMessage(), oe);
		}
	}
    
    /**
     * @description We need to check that the metal 'pipeline' is applicable for the location of the adjustment
     * @param 		templatePipelineId 
     * @return		pipelineId
     */
    private int checkLocationIsValidForRegion(final int templatePipelineId) {
    	
    	// Check the region on this deal matches the region of the location on the required adjustment
    	int pipeLineId = 0;
    	StringBuilder sql =	new StringBuilder();
        Table metalPipeline = Application.getInstance().getCurrentSession().getTableFactory().createTable();
        IOFactory iof = Application.getInstance().getCurrentSession().getIOFactory();
        
        try {
        	// Check the location is applicable
    		sql.append("SELECT pipeline_id as region \n");
    		sql.append("FROM gas_phys_location g, idx_subgroup idx \n");
    		sql.append("WHERE g.location_name='").append(location).append("' \n");
    		sql.append("AND g.idx_subgroup=idx.id_number \n");
    		sql.append("AND idx.name = '").append(product).append("'");
    		Logging.debug("About to run SQL. \n" + sql);
    		
    		try {
    			metalPipeline = iof.runSQL(sql.toString());
    		} catch (Exception e) {
    			throw new RuntimeException("ERROR executing SQL : " + sql.toString());
    		}		
    		if (metalPipeline.getRowCount() != 1) {
    			throw new RuntimeException("Failed to locate a distinct geographical location, aborting process.");
    		}

    		// Extract pipeline
			Pipeline localePipelineId = (Pipeline) Application
					.getInstance()
					.getCurrentSession()
					.getStaticDataFactory()
					.getReferenceObject(EnumReferenceObject.Pipeline,
							metalPipeline.getInt(0, 0));
    		if (templatePipelineId != localePipelineId.getId())  {
    			pipeLineId = localePipelineId.getId();
    		}
        } finally {
        	metalPipeline.dispose();
        }
    	return pipeLineId;	
	}

	/**
     * Gets the adjustment id.
     *
     * @return the adjustment id
     */
    public final int getAdjustmentId() {
        return adjustmentId;
    }
    
    /**
     * Gets the internal portfolio based on the default SI defined in the user table
     * User_JM_StockTake_Account_Map.
     *
     * @return the internal portfolio
     */
    public final String getInternalPortfolio() {
        // Unpack default SI results and apply to trade
        if (defaultSI == null || defaultSI.getRowCount() != 2) {
        	throw new RuntimeException("An incomplete set of default SI's was detected, please check User_JM_StockTake_Account_Map");
        }
    	// Unpack results
        for (TableRow row : defaultSI.getRows()) {
        	
        	Logging.debug(row.toString());
        	
        	boolean isInternalBunit = (row.getInt("int_ext") == 0 ? true : false); 
        	if (isInternalBunit) {

        		return row.getString("portfolio");
        	}
        }

        throw new RuntimeException("Unable to locate internal portfolio value, please check User_JM_StockTake_Account_Map");
        
        
    }

    
    /**
     * Gets the next adjustment id from the database sequence.
     *
     * @return the next adjustment id
     */
    private int getNextAdjustmentId() {
        
        String sql = "SELECT NEXT VALUE FOR JM_stocktake AS adjustment_id";
       
        Session session = Application.getInstance().getCurrentSession();
        
        IOFactory iof = session.getIOFactory();
        
        Logging.debug("About to run SQL. \n" + sql);
        
        
        Table sequenceNumber = null;
        try {
            sequenceNumber = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        if (sequenceNumber.getRowCount() != 1) {
            String errorMessage = "Error loading sequence number:  expected 1 row but found " + sequenceNumber.getRowCount();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);           
        }
        
        return sequenceNumber.getInt(0, 0);
    }
    
    /**
     * @description Attempts to retrieve the default SI from the user table user_jm_stocktake_account_map
     * @return the default SI information
     */
    private Table getDefaultSI() {
        
        StringBuilder sql = new StringBuilder();
        Table settlementMap = Application.getInstance().getCurrentSession().getTableFactory().createTable();
        IOFactory iof = Application.getInstance().getCurrentSession().getIOFactory();

        try {
            // Get default internal and external SI's
            sql.append("SELECT ").append(adjustmentId).append(" as adjustment_id, m.our_unit as bunit, '").append(product).append("' as product, \n");
            sql.append("m.int_portfolio as portfolio, si1.settle_id as settle_id, 0 as int_ext \n");
            sql.append("FROM user_jm_form_map f, user_jm_stocktake_account_map m, settle_instructions si1 \n");
            sql.append("WHERE f.src_batch_form='").append(form).append("' \n");
            sql.append("AND m.location='").append(location).append("' \n");
            sql.append("AND f.dst_receipt_form=m.form_phys \n");
            sql.append("AND m.int_settle_name=si1.settle_name \n");
            sql.append("UNION ALL \n");
            sql.append("SELECT ").append(adjustmentId).append(" as adjustment_id, m.ctp_unit as bunit, '").append(product).append("' as product, \n");
            sql.append("m.ext_portfolio as portfolio, si1.settle_id as settle_id, 1 as int_ext \n");
            sql.append("FROM user_jm_form_map f, user_jm_stocktake_account_map m, settle_instructions si1 \n");
            sql.append("WHERE f.src_batch_form='").append(form).append("' \n");
            sql.append("AND m.location='").append(location).append("' \n");
            sql.append("AND f.dst_receipt_form=m.form_phys \n");
            sql.append("AND m.ext_settle_name=si1.settle_name \n"); 
        	Logging.debug("About to run SQL. \n" + sql);
            settlementMap = iof.runSQL(sql.toString());
            
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            settlementMap.dispose();
            throw new RuntimeException(errorMessage);
        }
        return settlementMap;
    }
    
    

    /**
     * Assign default settlementa instructions. Load the SI rules from user table User_JM_StockTake_Account_Map
     *              If this method fails to find a row it will block the deal booking
     *
     * @param transaction the transaction to apply the SI to.
     */
    private void assignDefaultSettlementaInstructions(final Transaction transaction) {
        
    	final String DELIVERY_TYPE = "Cash";
        Session session = Application.getInstance().getCurrentSession();
        BackOfficeFactory bof = session.getBackOfficeFactory();
        String currencyName = transaction.getLeg(2).getValueAsString(EnumLegFieldId.Currency);
        StringBuilder message = new StringBuilder();

        try {
            
            // Unpack default SI results and apply to trade
            if (defaultSI == null || defaultSI.getRowCount() != 2) {
            	throw new RuntimeException("An incomplete set of default SI's was detected, please check User_JM_StockTake_Account_Map");
            }
        	// Unpack results
            for (TableRow row : defaultSI.getRows()) {
            	
            	Logging.debug(row.toString());
                String businessUnit = row.getString("bunit");
                String portfolio = row.getString("portfolio");
                int settleId = row.getInt("settle_id");
                boolean isInternalBunit = (row.getInt("int_ext") == 0 ? true : false); 
                
                // Set the bunit and portfolio on the deal.
                if (isInternalBunit) {
                	transaction.setValue(EnumTransactionFieldId.InternalBusinessUnit, businessUnit);
                	transaction.setValue(EnumTransactionFieldId.InternalPortfolio, portfolio);
                } else { 
                	transaction.setValue(EnumTransactionFieldId.ExternalBusinessUnit, businessUnit);
                	
                	if (portfolio != null && portfolio.length() > 0) {
                		transaction.setValue(EnumTransactionFieldId.ExternalPortfolio, portfolio);
                	}
                }
                
                // Retrieve SI arguments
				bunit = (Party) Application
						.getInstance()
						.getCurrentSession()
						.getStaticDataFactory()
						.getReferenceObject(EnumReferenceObject.BusinessUnit,
								businessUnit);
				currency = (Currency) Application
						.getInstance()
						.getCurrentSession()
						.getStaticDataFactory()
						.getReferenceObject(EnumReferenceObject.Currency,
								currencyName);
				deliveryType = (DeliveryType) Application
						.getInstance()
						.getCurrentSession()
						.getStaticDataFactory()
						.getReferenceObject(EnumReferenceObject.DeliveryType,
								DELIVERY_TYPE);
               
                // Get possible settlement instructions from the trade based from the default rule information
                if (settleId < 1) {
                	message.append("Failed to locate settlement instructions for Party: '").append(bunit.getName()).append(" \n'");
                	throw new RuntimeException(message.toString());
                }
                
                message.append("Retrieving SI's for: '").append(currency.getName()).append("' \n");
                message.append("Bunit: '").append(bunit.getName()).append("' \n");
                message.append("DeliveryType: '").append(deliveryType.getName()).append("' \n");
                Logging.debug(message.toString());
                
                // try and locate the instructions
                SettlementInstruction siInternal = bof.retrieveSettlementInstruction(settleId);
                if (siInternal != null) {
                	try {
                		bof.setSettlementInstruction(transaction, currency, bunit, deliveryType, isInternalBunit, siInternal);
                	} catch (Exception e) {
                		message = new StringBuilder();
                		message.append("Failed to save Settlement Instruction: '").append(siInternal.getName()).append("' \n");
                		message.append("ID: '").append(settleId).append(" \n");
                		message.append("Party = '").append(bunit.getName()).append("' \n");
                		message.append("Currency = '").append(currency).append("' \n");
                		message.append("DeliveryType = '").append(deliveryType.getName()).append("' \n");
                		Logging.error(message.toString());
                		throw new RuntimeException(message.toString());
                	}
                }
            }
            // Persist the change to the database
            bof.saveSettlementInstructions(transaction);
            
        } catch (Exception e) {
            String errorMessage = "An error was detected when attempting to apply the default SI to the trade:" +  e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return;           
    }
    
    /**
     * @description Checks whether a default rule exists in table USER_JM_Stocktake_Account_Map
     * @return         ruleExists
     */
    public final boolean getDefaultSIRulesExist() {
        boolean ruleExists = true;
        if (defaultSI.getRowCount() == 0) {
        	throw new RuntimeException("No default settlement instruction exists, please check USER_JM_Stocktake_Account_Map");
        } else if (defaultSI.getRowCount() != 2) {
        	throw new RuntimeException("Unable to detect full settlement instructions, please check USER_JM_Stocktake_Account_Map");
        }
        return ruleExists;
    }
    
    /**
     * Map the batch location to the deal loco.
     *
     */
//    private String mapLocationtoLoco(String location) {
//        String sql = "select dst_loco_info from USER_jm_loco_map where src_comm_stor_location = '" + location + "'";
//        
//        IOFactory iof = Application.getInstance().getCurrentSession().getIOFactory();
//        
//        Logging.debug("About to run SQL. \n" + sql);
//        
//        
//        Table locoMapData = null;
//        try {
//            locoMapData = iof.runSQL(sql);
//        } catch (Exception e) {
//            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
//            Logging.error(errorMessage);
//            throw new RuntimeException(errorMessage);
//        }
//        
//        if (locoMapData.getRowCount() != 1) {
//            String errorMessage = "Error loading location mapping data for : " 
//   			 + location + ". expected 1 row but found " + locoMapData.getRowCount();
//            Logging.error(errorMessage);
//            throw new RuntimeException(errorMessage);           
//        }
//        
//        return locoMapData.getString(0, 0);
//    }
}