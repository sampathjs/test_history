package com.olf.jm.vatinclusivecalc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.olf.openjvs.OException;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.staticdata.ConstField;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

public class VatInclusiveCashTranHandler extends VatInclusiveTranHandler {
	
	private static final String FIELD_SRC_AMOUNT_WITH_VAT = "Amount with VAT";
	private static final EnumTransactionFieldId FIELD_ENUM_DEST_AMOUNT = EnumTransactionFieldId.Position;
	private static final Integer METAL_RENTALS_GOLD = 2023;
	private static final Integer METAL_RENTALS_SILVER = 2024;
	private static final Integer METAL_RENTALS_PLATINUM = 2025;
	private static final Integer METAL_RENTALS_PALLADIUM = 2026;
	private static final Integer METAL_RENTALS_RHODIUM = 2027;
	private static final Integer METAL_RENTALS_IRIDIUM = 2028;	
	private static final Integer METAL_RENTALS_OSMIUM = 2029;
	private static final Integer METAL_RENTALS_RUTHENIUM = 2030;

	public VatInclusiveCashTranHandler(Session session, Transaction tran) {
		super(session, tran);
	}

	@Override
	public void updateDependingFields() throws UnsupportedException {
		if(!isSupported()) 
			throw new UnsupportedException(tran.toString() + " - type is not supported by VAT-inclusive price calculator");
		
		PluginLog.debug("start processing " + tran.toString());
		try {
				long startTime = System.currentTimeMillis();
				try(Field fldSrc = tran.getField(FIELD_SRC_AMOUNT_WITH_VAT);
						Field fldDest1 = tran.getField(FIELD_ENUM_DEST_AMOUNT)) {

			
				if(!this.validateFields(fldSrc, fldDest1))
				{
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
					PluginLog.error(msg);
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
					PluginLog.info("No VAT rate applicable, skipping the deal");
					return;
				}
				double amountExclVat = amountWithVat / vatRate;

				PluginLog.info(String.format("Field %s value = %,.4f, VAT rate = %,.2f, Result = %,.9f into field %s", 
						fldSrc.getName(), amountWithVat, vatRate, amountExclVat, fldDest1.getName()));
				fldDest1.setValue(amountExclVat);

				long elapsedTotal = System.currentTimeMillis() - startTime;
				
				PluginLog.debug(String.format("Processing times:\t elapsedToFields = %d ms\t elapsedToVatRate = %d ms\t elapsedTotal = %d ms", 
						elapsedToFields, elapsedToVatRate, elapsedTotal));
			}
		} 
		catch(OException e) {
			throw new RuntimeException("Failed to update dependant fields: " + e.getLocalizedMessage(), e);
		}
		finally {
			PluginLog.debug(" ... finished");
		}
	}

	@Override
	public void adjustEvents() throws Exception {
		if(!isSupported()) {
			PluginLog.info(tran.toString() + "\n - type is not supported by VAT-inclusive price calculator, transaction ignored");
			return;
		}
		
		PluginLog.debug("start processing " + tran.toString());
		try {
			// First check if the deal is Validated, we can't expect events when it is still New or Pending
			if(EnumTranStatus.Validated != tran.getTransactionStatus()) {
				PluginLog.debug("Tran status is not Validated, skipping the deal");
				return;
			}

			// Get our Payment currency
			Field fldPymtCcy = tran.getLeg(0).getField(EnumLegFieldId.Currency);
			PluginLog.debug("currency: " + fldPymtCcy);

			
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
				PluginLog.warn("Source Field " + fldSrc.getName() + " is not applicable, possibly something wrong with Tran Info field setup");
				return;
			}

			boolean isAmountWithVatEmpty = fldSrc.getValueAsString().trim().isEmpty();
			
			if(isAmountWithVatEmpty) {
				PluginLog.info(String.format("Field %s is not used, no adjustment is needed", 
						fldSrc.getName()));
				return;
			}
			
			double amountWithVat = fldSrc.getValueAsDouble();
			PluginLog.info(String.format("Field %s value = %,.2f", 
					FIELD_SRC_AMOUNT_WITH_VAT, amountWithVat));

			long elapsed = System.currentTimeMillis();
			checkAndUpdateEventsWithJVS(amountWithVat, fldPymtCcy);
		
			elapsed = System.currentTimeMillis() - elapsed;
			PluginLog.debug(String.format("processing events took %d ms", elapsed));
		
		} 
		finally {
			PluginLog.debug(" ... finished");
		}
	}
	
	@Override
	protected boolean isSupported() {
		// This class is for Cash toolset/instruments
		if(tran.getToolset() != EnumToolset.Cash)
			return false;
		
		EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
		return (insType == EnumInsType.CashInstrument);
	}
	@Override
	protected double getVatRate() throws IllegalStateException {
		/*
		 * - ‘Reduced Tax’: it depends on the counterparty on the CASH if a reduced tax is applicable. Currently it’s only CPC and cash flow type as Metal Rentals_*. 
		 * 		We will use an existing Party Info field on the Legal Entity to save if the reduced VAT rate is applicable: 
		 * 			‘LBMA Member = Yes’. So the script needs to get this value from the counterparty on the deal.
		 *  - The VAT rate factor (1.16) and reduced VAT rate factor (1.06) should be set based on the defined Tax Rates in the Tax Manager, 
		 *  		considering the effective Start/End Date:
		 *  		o	1.16 = 1 + (Rate of ‘CN Std Tax’)
		 *  		o	1.06 = 1 + (Rate of ‘CN Red Tax’)
		 */
		PluginLog.debug("Retrieving VAT rate using Party info from External LE");
		int partyId = tran.getValueAsInt(EnumTransactionFieldId.ExternalLegalEntity);
		if(partyId <= 0)
			throw new IllegalStateException(MSG_NO_PARTY_NO_VAT);
		double rate;
		
			
		List<Integer> cFlowID = Arrays.asList(METAL_RENTALS_GOLD,METAL_RENTALS_SILVER,METAL_RENTALS_PLATINUM,METAL_RENTALS_PALLADIUM,METAL_RENTALS_RHODIUM,METAL_RENTALS_IRIDIUM,METAL_RENTALS_OSMIUM,METAL_RENTALS_RUTHENIUM);
		
		try(LegalEntity party = (LegalEntity) session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.LegalEntity, partyId)) {
			String partyName = party.getName();
			EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
			ConstField fldReduced = party.getConstField(PARTY_FIELD_VAT_RATE_REDUCED);
			Field cflowTypeField = tran.getField(EnumTransactionFieldId.CashflowType);
			int cflowType = cflowTypeField.getValueAsInt();
			
			if(fldReduced.isApplicable() && fldReduced.getValueAsString().compareToIgnoreCase("Yes") == 0 && insType == EnumInsType.CashInstrument 
					&& cFlowID.contains(cflowType)) {	
			rate = VAT_RATE_REDUCED;
				PluginLog.debug(String.format("Field %s on %s is set to Yes, VAT rate is %.2f", PARTY_FIELD_VAT_RATE_REDUCED, partyName, rate));
			}
			
			else {
				ConstField fldStandard = party.getConstField(PARTY_FIELD_VAT_ZERO);
				if(fldStandard.isApplicable() && fldStandard.getValueAsString().compareToIgnoreCase("Yes") == 0) {
					rate = 0;
					PluginLog.debug(String.format("Field %s on %s is set to Yes, VAT rate is %.2f", PARTY_FIELD_VAT_ZERO, partyName, rate));
				}
				else {
					rate = VAT_RATE_STANDARD;
				}
			}			
			
			PluginLog.debug(String.format("VAT rate is %.2f", rate));
		}
		
		return rate;
	}
	
	private boolean validateFields(Field fldSrc,Field fldDest1 )
	{
		

		if (fldSrc == null) {
			PluginLog
					.warn("Source Field is NULL. Possibly something wrong with Tran Field notification setup");
			return false;
		}
		if (!fldSrc.isApplicable()) {
			PluginLog
					.warn("Source Field "
							+ fldSrc.getName()
							+ " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		} else {
			if (fldSrc.getValueAsString().trim().isEmpty())
				return false; // This is a case where we would reset the value
								// due to an error and be called recursively
		}
		if (fldDest1 == null) {
			PluginLog
					.warn("Destination field is NULL, possibly something wrong with Tran Field notification setup");
			return false;
		}
		if (!fldDest1.isApplicable()) {
			PluginLog
					.warn("Destination field "
							+ fldDest1.getName()
							+ " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		}
		return true;
	}
}
