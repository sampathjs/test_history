package com.olf.jm.vatinclusivecalc;

import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import  com.olf.jm.logging.Logging;

public class VatInclusiveStrategyTranHandler extends VatInclusiveTranHandler {
	
	private static final String FIELD_SRC_AMOUNT_WITH_VAT = "Amount with VAT";
	private static final String FIELD_ENUM_DEST_AMOUNT = "Charge (in USD)";

	public VatInclusiveStrategyTranHandler(Session session, Transaction tran) {
		super(session, tran);
	}

	@Override
	public void updateDependingFields() throws UnsupportedException {
		if(!isSupported()) 
			throw new UnsupportedException(tran.toString() + " - type is not supported by VAT-inclusive price calculator");
		
		Logging.debug("start processing " + tran.toString());
		try {
			long startTime = System.currentTimeMillis();
			
			try(Field fldSrc = tran.getField(FIELD_SRC_AMOUNT_WITH_VAT);
					Field fldDest1 = tran.getField(FIELD_ENUM_DEST_AMOUNT)) {

				if(! this.validateFields(fldSrc, fldDest1))
				{
					Logging.error("There are validation errors for the fields.");
					return;
				}

				double amountWithVat = fldSrc.getValueAsDouble();
				long elapsedToFields = System.currentTimeMillis() - startTime;
				
				// Get VAT rate
				double vatRate = 0;
				try {
					vatRate = getVatRate(); // Comes in a form like 1.16
				} 
				catch (IllegalStateException e) {
					String msg = e.getLocalizedMessage();
					Logging.error(msg);
					if(com.olf.openjvs.Util.canAccessGui() == 1 && !fldSrc.getValueAsString().trim().equals("0")){
						com.olf.openjvs.Ask.ok(MSG_GUI_WARNING_PREFIX + ": " + msg);
					
						// Reset the source value to make sure trader enters it again
						fldSrc.setValue("");
						fldDest1.setValue("");		
					}
					return;
				}
				long elapsedToVatRate = System.currentTimeMillis() - startTime;
				
				// Set Net Amount
				if(vatRate == 0) { // We are not expecting any calculations that produce this 0, so should be safe to compare directly
					Logging.info("No VAT rate applicable, skipping the deal");
					return;
				}
				double amountExclVat = amountWithVat / vatRate;

				Logging.info(String.format("Field %s value = %,.4f, VAT rate = %,.2f, Result = %,.9f into field %s", 
						fldSrc.getName(), amountWithVat, vatRate, amountExclVat, fldDest1.getName()));
				fldDest1.setValue(amountExclVat);

				long elapsedTotal = System.currentTimeMillis() - startTime;
				
				Logging.debug(String.format("Processing times:\t elapsedToFields = %d ms\t elapsedToVatRate = %d ms\t elapsedTotal = %d ms", 
						elapsedToFields, elapsedToVatRate, elapsedTotal));
			}
		} 
		catch(OException e) {
			throw new RuntimeException("Failed to update dependant fields: " + e.getLocalizedMessage(), e);
		}
		finally {
			Logging.debug(" ... finished");
		}
	}

	@Override
	public void adjustEvents() throws Exception {
		if(!isSupported()) {
			Logging.info(tran.toString() + "\n - type is not supported by VAT-inclusive price calculator, transaction ignored");
			return;
		}
		
		Logging.debug("start processing " + tran.toString());
		try {
			// First check if the deal is Validated, we can't expect events when it is still New or Pending
			if(EnumTranStatus.Validated != tran.getTransactionStatus()) {
				Logging.debug("Tran status is not Validated, skipping the deal");
				return;
			}

			// Get our Payment currency
			Field fldPymtCcy = tran.getLeg(0).getField(EnumLegFieldId.Currency);
			Logging.debug("currency: " + fldPymtCcy);

			
			/*
			 * Algorithm:
			 * 1. Target = Amount with VAT (tran info field)
			 * 2. Gross Amount = Cash position + Tax position
			 * 3. Difference = Target - Gross Amount
			 * RAISE WARNING if abs(Difference) > 0.01, DO NOT ADJUST events in this case
			 * 4. Tax Settle Amount = Tax position + Difference
			 */
			
			Field fldSrc = tran.getField(FIELD_SRC_AMOUNT_WITH_VAT);

			if(fldSrc == null || !fldSrc.isApplicable()) {
				Logging.warn("Source Field " + fldSrc.getName() + " is not applicable, possibly something wrong with Tran Info field setup");
				return;
			}

			boolean isAmountWithVatEmpty = fldSrc.getValueAsString().trim().isEmpty();
			
			if(isAmountWithVatEmpty) {
				Logging.info(String.format("Field %s is not used, no adjustment is needed", 
						fldSrc.getName()));
				return;
			}
			
			double amountWithVat = fldSrc.getValueAsDouble();
			Logging.info(String.format("Field %s value = %,.2f", 
					FIELD_SRC_AMOUNT_WITH_VAT, amountWithVat));

			long elapsed = System.currentTimeMillis();
			checkAndUpdateEventsWithJVS(amountWithVat, fldPymtCcy);
		
			elapsed = System.currentTimeMillis() - elapsed;
			Logging.debug(String.format("processing events took %d ms", elapsed));
		
		} 
		finally {
			Logging.debug(" ... finished");
		}
	}
	
	@Override
	protected boolean isSupported() {
		// This class is for Cash toolset/instruments
		if(tran.getToolset() != EnumToolset.Composer)
			return false;
		
		EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
		return (insType == EnumInsType.Strategy);
	}
	
	private boolean validateFields(Field fldSrc, Field fldDest1)
	{
		if(fldSrc == null)
		{
			Logging.warn("Source Field is NULL. Possibly something wrong with Tran Field notification setup");
			return false;
		}
		if(!fldSrc.isApplicable()) {
			Logging.warn("Source Field " + fldSrc.getName() + " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		}
		else {
			if(fldSrc.getValueAsString().trim().isEmpty())
				return false; // This is a case where we would reset the value due to an error and be called recursively
		}
		if(fldDest1 == null)
		{
			Logging.warn("Destination field is NULL, possibly something wrong with Tran Field notification setup");
			return false;
		}
		if(!fldDest1.isApplicable()) {
			Logging.warn("Destination field " + fldDest1.getName() + " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		}
		return true;
	}

}
