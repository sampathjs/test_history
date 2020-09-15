package com.olf.jm.fo.tranfield.tozgmmetalpricecalc;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

public class CalculatorUtil {
	
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context 
	public static final String CONST_REPO_SUBCONTEXT = "GmTozPriceCalculatorUtil"; // sub context 
	public static final String FIELD_BASE_METAL_PRICE_WITH_VAT = "Base Metal Price with VAT";
	
	public CalculatorUtil(Session session) throws Exception {
		this.init(session);
		Logging.debug("Init of CalculatorUtil");
	}
	
	public double getConversionRate( Session session) 	{
		String sql = "SELECT conv.factor \n"  + 
					 " FROM   unit_conversion conv  \n" +
					 " WHERE  conv.src_unit_id = (\n" + 
					 "   SELECT unit_id FROM idx_unit WHERE unit_label = 'TOz'\n) " + 
					 " AND conv.dest_unit_id = (\n" + 
					 "   SELECT unit_id FROM idx_unit WHERE unit_label = 'gms') ";
		
		Logging.debug("SQL \n"+sql);
		double conversionRate = 1;
		try(Table result = session.getIOFactory().runSQL(sql);) {
			conversionRate = result.getDouble(0, 0);
		}

		Logging.debug("Conversion rate from the data base "+conversionRate);
		return conversionRate;
	}
	
	public void updateField(Session session, Transaction tran, boolean isGmField) {
		
		Logging.debug("start processing " + tran.toString());
		try {
			long startTime = System.currentTimeMillis();
			try (Field gramPriceField = tran.getField(FIELD_BASE_METAL_PRICE_WITH_VAT);
				Field tozPriceField = tran.getLeg(0).getField(EnumLegFieldId.CurrencyConversionRate)) {
				
				if(gramPriceField == null || (!gramPriceField.isApplicable())) {
					Logging.warn("Gram Field is either null or not applicable , possibly something wrong with Tran Field notification setup");
					return;
				}
				
				if(tozPriceField == null ||(!tozPriceField.isApplicable())) {
					Logging.warn("Toz Field is  either null or not applicable, possibly something wrong with Tran Field notification setup");
					return;
				}
				
				double tozPrice = tozPriceField.getValueAsDouble();
				double converisonRate = this.getConversionRate( session);
				double gmPrice = gramPriceField.getValueAsDouble();
				
				double price = 0;
				
				if(isGmField) {
					Logging.debug("Setting the Gram field");
					price = tozPrice / converisonRate;
					gramPriceField.setValue(price);
				} else {
					Logging.debug("Setting the toz field");
					price = gmPrice * converisonRate;
					tozPriceField.setValue(price);
				}
				
				Logging.debug("The toz price is "+tozPrice);
				Logging.debug("The conversion rate is "+converisonRate);
				Logging.debug("The gm price is "+gmPrice);
				

			}

			long elapsedTotal = System.currentTimeMillis() - startTime;

			Logging.debug(String.format( "Processing times:\t elapsedTotal = %d ms", elapsedTotal));
		} catch (Exception e) {
			throw new RuntimeException("Failed to update dependant fields: " + e.getLocalizedMessage(), e);
		} finally {
			Logging.debug(" ... finished");
		}
	}
	
	/**
	 * Initialises the log by retrieving logging settings from constants repository.
	 * 
	 * @param context
	 * @throws Exception
	 */
	private void init(Session session) throws Exception {
	
		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;
	
		try {
			ConstRepository constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);
	
			Logging.init(this.getClass(),CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
	
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getLocalizedMessage());
		}
	}
	
	public void setGmField(Session session, Transaction tran) {
		this.updateField(session, tran, true);
	}
	
	public void setTozField(Session session, Transaction tran) {
		this.updateField(session, tran, false);
	}

}
