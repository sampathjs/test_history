package com.olf.jm.metalstransfer.dealbooking;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

public  class CashTrasferChargesBookingProcessor {

	public CashTrasferChargesBookingProcessor() {
		super();
	}

	/**
	 * Main processing method.
	 * 
	 * @param session
	 * @param variables 
	 */
	public void process(Session session, Variables variables) {
	
	    boolean success = true;
	    
	    IOFactory ioFactory = session.getIOFactory();
	    String submitter = (variables.getVariable("Submitter")).getValueAsString();
	    String sql = 
	            "\n SELECT ab.tran_num" +
	            "\n   FROM ab_tran ab" +
	            "\n   JOIN ab_tran_info_view ativ1 ON (ativ1.tran_num = ab.tran_num)" +
	            "\n   JOIN ab_tran_info_view ativ2 ON (ativ2.tran_num = ab.tran_num)" +
	            "\n  WHERE ativ1.type_name = 'Charges'" +
	            "\n    AND ativ1.value = 'Yes'" +
	            "\n    AND ativ2.type_name = 'Charge Generated'" +
	            "\n    AND ativ2.value = 'No'" +
	            "\n    AND ab.tran_status = " + EnumTranStatus.Validated.getValue() +
	            "\n    AND ab.internal_portfolio IN " + 
	            "\n       (SELECT pp.portfolio_id FROM portfolio_personnel pp " + 
	            "\n         INNER JOIN personnel p ON p.id_number = pp.personnel_id " + 
	            "\n         WHERE p.id_number = " +
	            "\n        " + submitter + " AND pp.access_type = 0)";  // access_type 0 = read and write
	    
	    // Find all strategies that have a charge and the charge has not been generated
	    try (Market market = session.getMarket();
	         Table chargeables = ioFactory.runSQL(sql)) {
	        Logging.info("SQL received " + chargeables.getRowCount() + " rows for submitter " + submitter);
	        Logging.info(sql);
	        TradingFactory tradeFactory = session.getTradingFactory();
	        StaticDataFactory staticFactory = session.getStaticDataFactory();
	
	        // Force refresh of universal prices to ensure latest data
	        market.refresh(true, true);
	        market.loadUniversal();
	
	        for (TableRow row : chargeables.getRows()) {
	            try (Transaction strategy = tradeFactory.retrieveTransactionById(row.getInt("tran_num"))) {
	
	                String strategyRef = strategy.getValueAsString(EnumTransactionFieldId.ReferenceString);
	                Logging.info("Working with strategy deal " + strategy.getTransactionId() + ", reference " + strategyRef);
	
	                // Preferred currency of the From A/C BU
	                String fromBunit = strategy.getField("From A/C BU").getValueAsString();
	                String preferredCcy = "USD";// getBusinessUnitPreferredCcy(ioFactory, fromBunit);
	                
	                // Get the cash instrument for the preferred currency and create cash transaction for the charges
	                try (Instrument ins = tradeFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, preferredCcy);
	                     Transaction cash = tradeFactory.createTransaction(ins)) {
	
	                    // Set cash flow type
	                    cash.setValue(EnumTransactionFieldId.CashflowType, "Transfer Charge");
	                    
	                    // Cash deal gets same reference as strategy deal
	                    String reference = strategy.getField(EnumTransactionFieldId.ReferenceString).getValueAsString();
	                    cash.setValue(EnumTransactionFieldId.ReferenceString, reference);
	                    
	                    // Get details from strategy
	                    Field bunit = strategy.getField(EnumTransactionFieldId.InternalBusinessUnit);
	                    int lentityId = strategy.getField(EnumTransactionFieldId.InternalLegalEntity).getValueAsInt();
	                    double charge = strategy.getField("Charge (in USD)").getValueAsDouble();
	
	                    // Get spot rate for currency conversion
	                    String cashCcy = cash.getField(EnumTransactionFieldId.ConversionCurrency).getValueAsString();
	                    double spotRate = market.getFXSpotRate(
	                        staticFactory.getReferenceObject(Currency.class, cashCcy),
	                        staticFactory.getReferenceObject(Currency.class, preferredCcy));
	
	                    // Set the cash deal fields
	                    cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, bunit.getValueAsInt());
	                    cash.setValue(EnumTransactionFieldId.InternalLegalEntity, lentityId);
	                    cash.setValue(EnumTransactionFieldId.InternalPortfolio, retrieveFeePortfolioId(ioFactory, bunit));
	                    cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, fromBunit);
	                    cash.setValue(EnumTransactionFieldId.Position, charge * spotRate);
	
	                    // Settle date
	                    cash.setValue(EnumTransactionFieldId.SettleDate, getSettleDate(market.getCurrentDate()));
	                    
	                    // Set conversion factor used 
	                    Field ccyConvFxRate = cash.getField("Ccy Conv FX Rate");
	                    ccyConvFxRate.setValue(new DecimalFormat("#,##0.000000").format(spotRate));
	
	                    // Set the strategy number
	                    Field strategyNum = cash.getField("Strategy Num");
	                    strategyNum.setValue(strategy.getDealTrackingId());
	                    
	                    setSAPMTRNo(cash, strategy);
	
	                    Logging.info("Booking cash deal for charges on strategy " + strategyRef);
	                    cash.process(EnumTranStatus.Validated);
	                    Logging.info("Booked cash deal " + cash.getTransactionId() + " for charges on strategy " + strategyRef);
	
	                    try {
	                        // Mark strategy as charges generated
	                        Field chargeGenerated = strategy.getField("Charge Generated");
	                        chargeGenerated.setValue("Yes");
	                        strategy.saveInfoFields();
	                    
	                    }
	                    catch (RuntimeException e) {
	                        success = false;
	                        Logging.error("Failed while setting 'Charge Generated' tran info field to 'Yes' on strategy " + strategyRef, e);
	                        try {
	                            // Update of strategy deal charges generated flag failed so cancel charges cash deal
	                            // If this is not done then a second charge could potentially be created
	                        	// JW: Note that the transaction might have been
	                        	// updated by post process services and needs
	                        	// refresh:
	                        	try {
									Thread.sleep(10000);
								} catch (InterruptedException e1) {
									Logging.info("The thread could not wait for 10 seconds to finish post process services");
								}
	                        	
	                        	int dealTrackingId = cash.getDealTrackingId();
	                        	try (Transaction cashUpdated = session.getTradingFactory().retrieveTransaction(dealTrackingId)) {
	                        		cashUpdated.process(EnumTranStatus.Cancelled);
	                        	}                            	
	                        }
	                        catch (RuntimeException e1) {
	                            Logging.error("Failed cancelling cash charge deal" + cash.getDealTrackingId() + " for strategy " + strategyRef, e1);
	                        }
	                    }
	                }
	                catch (RuntimeException e) {
	                    success = false;
	                    Logging.error("Failed while creating cash charge deal for strategy " + strategyRef, e);
	                }
	            }
	        }
	    }
	    
	    if (!success) {
	        throw new RuntimeException("Transfer charges booking failed. See log file for further details.");
	    }
	}

	/**
	 * Get the preferred currency for the business unit.
	 * 
	 * @param factory
	 * @param bunit
	 * @return currency
	 */
	private String getBusinessUnitPreferredCcy(IOFactory factory, String bunit) {
	    try (Table ccy = factory.runSQL(
	            "\n SELECT IsNull(piv.value, pit.default_value) AS ccy" +
	            "\n   FROM party pty" +
	            "\n   JOIN party_info_types pit ON (pit.type_name = 'Preferred Currency')" +
	            "\n   LEFT JOIN party_info_view piv ON (piv.party_id = pty.party_id AND piv.type_name = 'Preferred Currency')" +
	            "\n  WHERE pty.short_name = '" + bunit + "'")) {
	        
	        if (ccy.getRowCount() > 0) {
	            return ccy.getString(0, 0);
	        }
	        throw new RuntimeException("No preferred currency set for business unit " + bunit);
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
	 * Retrieve the portfolio id of the fee portfolio for the business unit. The portfolio name is expected to end with 'Fees'.
	 * 
	 * @param factory
	 * @param bunit
	 * @return
	 */
	private int retrieveFeePortfolioId(IOFactory factory, Field bunit) {
	
	    try (Table portfolio = factory.runSQL(
	            "\n SELECT id_number" +
	            "\n   FROM portfolio p" +
	            "\n   JOIN party_portfolio pp ON (pp.portfolio_id = p.id_number)" +
	            "\n  WHERE p.name LIKE '% Fees'" +
	            "\n    AND pp.party_id = " + bunit.getValueAsInt())) {
	        if (portfolio.getRowCount() > 0) {
	                return portfolio.getInt(0, 0);
	        }
	    }
	    throw new RuntimeException("No fees portfolio found for business unit " + bunit.getValueAsString());
	}

	/**
	 * Settlement date will be the 15th of the next month.
	 * 
	 * @param date Starting date
	 * @return settle date
	 */
	private Date getSettleDate(Date date) {
	    Calendar today = Calendar.getInstance();
	    today.setTime(date);
	    today.add(Calendar.MONTH, 1);
	    today.set(Calendar.DAY_OF_MONTH, 15);
	    return today.getTime();
	}

}