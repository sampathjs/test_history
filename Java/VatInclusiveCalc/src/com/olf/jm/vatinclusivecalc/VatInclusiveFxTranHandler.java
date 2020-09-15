package com.olf.jm.vatinclusivecalc;

import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import  com.olf.jm.logging.Logging;

public class VatInclusiveFxTranHandler extends VatInclusiveTranHandler {

	private static final String FIELD_SRC_PRICE_WITH_VAT = "Price with VAT";
	private static final String FIELD_DEST_AMOUNT_WITH_VAT = "Amount with VAT";
	private static final String FIELD_DEST_TRADE_PRICE = "Trade Price";
	private static final EnumTransactionFieldId FIELD_ENUM_SRC_POSITION = EnumTransactionFieldId.FxDealtAmount;
	
	public VatInclusiveFxTranHandler(Session session, Transaction tran) {
		super(session, tran);
	}
	
	@Override
	public void updateDependingFields() throws UnsupportedException {
		if(!isSupported()) {
			//JW: removed exception and replaced with return statement to avoid blocking 
			// of processing of unsupported FX deals.
			Logging.info(tran.toString() + "\n - type is not supported by VAT-inclusive price calculator, transaction ignored");
			return;
		}
		Logging.debug("start processing " + tran.toString());
		try {
			long startTime = System.currentTimeMillis();
			
			try(Field fldSrc = tran.getField(FIELD_SRC_PRICE_WITH_VAT);
					Field fldDest1 = tran.getField(FIELD_DEST_TRADE_PRICE);
					Field fldDest2 = tran.getField(FIELD_DEST_AMOUNT_WITH_VAT)) {

				if(! validateFields(fldSrc, fldDest1, fldDest2))
				{
					Logging.error("Field validations failed !!");
				}

				boolean isPriceWithVatEmpty = fldSrc.getValueAsString().trim().isEmpty();
				double priceWithVat = fldSrc.getValueAsDouble();
				long elapsedToFields = System.currentTimeMillis() - startTime;
				
				if(isPriceWithVatEmpty) {
					Logging.info(String.format("Field %s is empty, resetting fields %s and %s and exiting", 
							fldSrc.getName(), fldDest1.getName(), fldDest2.getName()));
					//fldDest1.setValue(0.0); // Trade price
					fldDest2.setValue(""); // Amount with VAT
					return;
				}
				
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
						fldSrc.setValue(0.0);
						fldDest1.setValue(0.0);
						fldDest2.setValue(0.0);
					}
					return;
				}
				long elapsedToVatRate = System.currentTimeMillis() - startTime;
				
				// Set Net price
				if(vatRate == 0) { // We are not expecting any calculations that produce this 0, so should be safe to compare directly
					Logging.info("No VAT rate applicable, skipping the deal");
					return;
				}
				double priceExclVat = priceWithVat / vatRate;

				Logging.info(String.format("Field %s value = %,.4f, VAT rate = %,.2f, Result = %,.9f into field %s", 
						fldSrc.getName(), priceWithVat, vatRate, priceExclVat, fldDest1.getName()));
				fldDest1.setValue(priceExclVat);//------------------------------

				// Set Amount with VAT
				double position = getPosition();
				
				if(com.olf.openjvs.Util.canAccessGui() == 1 && position == 0){
					com.olf.openjvs.Ask.ok(MSG_GUI_WARNING_PREFIX + ": Please enter a quantity.");

				}
				double amountWithVat = Math.round(position * priceWithVat * 100.0) / 100d;
				String strAmountWithVat = String.format("%.2f", amountWithVat);
				Logging.info(String.format("Field %s value = %,.4f, %s = %,.4f, Result = %s into field %s", 
						FIELD_ENUM_SRC_POSITION, position, fldSrc.getName(), priceWithVat, strAmountWithVat, fldDest2.getName()));
				
				fldDest2.setValue(strAmountWithVat);
				
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

	/**
	 * Retrieves tran position as entered by the trader
	 * The method is created to handle the situations where FX amount is saved in TOz in the database (and returned by APIs that way)
	 *   but the amount on the deal is in different UOM used by trader - and this is the one we target for
	 * @return
	 * @throws OException 
	 */
	private double getPosition() {
		String strPosition = tran.getValueAsString(FIELD_ENUM_SRC_POSITION);
		try {
			double position = com.olf.openjvs.Str.inputAsNotnl(strPosition.trim());
			return position;
		} catch (OException e) {
			throw new RuntimeException(String.format("Failed to convert %s value of %s to double", FIELD_ENUM_SRC_POSITION, strPosition), e); 
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

			// Get our Term currency
			// for PM there're no cases at JM where the base/term currencies are swapped in the ticker, so we can safely use single one always
			Field fldTermCcy = tran.getLeg(0).getField(EnumLegFieldId.BoughtCurrency);
			Logging.debug("currency: " + fldTermCcy);

			/*
			 * Algorithm:
			 * 1. Target = Amount with VAT (tran info field)
			 * 2. Gross Amount = Cash position + Tax position
			 * 3. Difference = Target - Gross Amount
			 * RAISE WARNING if abs(Difference) > 0.01, DO NOT ADJUST events in this case
			 * 4. Tax Settle Amount = Tax position + Difference
			 */
			
			Field fldSrc = tran.getField(FIELD_DEST_AMOUNT_WITH_VAT);

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
					FIELD_DEST_AMOUNT_WITH_VAT, amountWithVat));

			long elapsed = System.currentTimeMillis();
			checkAndUpdateEventsWithJVS(amountWithVat, fldTermCcy);
			
			elapsed = System.currentTimeMillis() - elapsed;
			Logging.debug(String.format("processing events took %d ms", elapsed));
		
		} 
		finally {
			Logging.debug(" ... finished");
		}
	}

	@Override
	protected boolean isSupported() {
		// For FX we support Spot and Forward
		if(tran.getToolset() != EnumToolset.Fx)
			return false;
		
		int cflowType = tran.getValueAsInt(EnumTransactionFieldId.CashflowType);
		
		return (cflowType == EnumCashflowType.FxSpot.getValue()
				|| cflowType == EnumCashflowType.FxFwdFwd.getValue());
	}
	
	private boolean validateFields(Field fldSrc,Field fldDest1,Field fldDest2 )
	{
		if (fldSrc == null)
		{
			Logging.warn("Source Field is NULL, possibly something wrong with Tran Field notification setup");
			return false;
		}
		if( !fldSrc.isApplicable()) {
			Logging.warn("Source Field " + fldSrc.getName() + " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		}
		if(fldDest1 == null)
		{
			Logging.warn("Destination field is NULL, possibly something wrong with Tran Info field setup");
			return false;
		}
		if(!fldDest1.isApplicable()) {
			Logging.warn("Destination field " + fldDest1.getName() + " is not applicable, possibly something wrong with Tran Info field setup");
			return false;
		}
		if(fldDest2 == null)
		{
			Logging.warn("Destination field is NULL, possibly something wrong with Tran Info field setup");
			return false;
		}
		if(!fldDest2.isApplicable()) {
			Logging.warn("Destination field " + fldDest2.getName() + " is not applicable, possibly something wrong with Tran Info field setup");
			return false;
		}
		return true;
	}


}
