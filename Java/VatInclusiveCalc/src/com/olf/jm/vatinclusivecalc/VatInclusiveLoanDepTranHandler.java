package com.olf.jm.vatinclusivecalc;

import com.olf.openjvs.OException;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.staticdata.ConstField;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

public class VatInclusiveLoanDepTranHandler extends VatInclusiveTranHandler {
	
	private static final String FIELD_SRC_PRICE_WITH_VAT = "Price with VAT";
	private static final String FIELD_DEST_AMOUNT_WITH_VAT = "Amount with VAT";
	private static final EnumLegFieldId FIELD_DEST_TRADE_LEG_RATE = EnumLegFieldId.Rate;

	private static final double MAX_RONDING_DIFF = 0.03;
	
	public VatInclusiveLoanDepTranHandler(Session session, Transaction tran) {
		super(session, tran);
	}

	@Override
	public void updateDependingFields() throws UnsupportedException {
		if(!isSupported()) 
			throw new UnsupportedException(tran.toString() + " - type is not supported by VAT-inclusive price calculator");
		
		PluginLog.debug("start processing " + tran.toString());
		try {
			long startTime = System.currentTimeMillis();
			
			try(Field fldSrc = tran.getField(FIELD_SRC_PRICE_WITH_VAT);
					Field fldDestRate = tran.getLeg(0).getField(FIELD_DEST_TRADE_LEG_RATE);
					Field fldDestTotal = tran.getField(FIELD_DEST_AMOUNT_WITH_VAT)) {

				if(! this.validateFields(fldSrc, fldDestRate, fldDestTotal))
				{
					PluginLog.error("There are errors in field validation");
					return;
				}

				boolean isRateWithVatEmpty = fldSrc.getValueAsString().trim().isEmpty();
				double rateWithVat = fldSrc.getValueAsDouble();
				long elapsedToFields = System.currentTimeMillis() - startTime;
				
				if(isRateWithVatEmpty) {
			//		PluginLog.info(String.format("Field %s is empty, resetting fields %s and %s and exiting", 
			//				fldSrc.getName(), fldDestRate.getName(), fldDestRate.getName()));
			//		fldDestRate.setValue(0.0); // Trade price
					fldDestTotal.setValue(""); // Amount with VAT
					return;
				}
				
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
						fldSrc.setValue(0.0);
						fldDestRate.setValue(0.0); // Trade price
						fldDestTotal.setValue(0.0); // Amount with VAT
					}
					return;
				}
				long elapsedToVatRate = System.currentTimeMillis() - startTime;
				
				// Set Net price
				if(vatRate == 0) { // We are not expecting any calculations that produce this 0, so should be safe to compare directly
					PluginLog.info("No VAT rate applicable, skipping the deal");
					return;
				}
				double rateExclVat = rateWithVat / vatRate;

				/*
				 * Algorithm:
				 * 1. In order to get the Total Amount with VAT we want to make Endur calculate it for us:
				 * 	We shall set the Rate with VAT into Rate native field and then take the sum of all payments in our currency 
				 * 	- this will go into Amount with VAT field 
				 * 2. After that we calculate Net rate put it into field Rate.
				 */
				
				// 1. Put Rate with VAT into Rate
				PluginLog.debug(String.format("To calculate Total Amount with VAT put field %s value = %,.4f into field %s", 
						fldSrc.getName(), rateWithVat, fldDestRate.getTranfName()));
				fldDestRate.setValue(rateWithVat);
				
				// Get the tran leg profiles
				double amountWithVat = 0;
				for(Profile prof : tran.getLeg(0).getProfiles()) {
					amountWithVat += prof.getValueAsDouble(EnumProfileFieldId.Payment);
				}
				
				long elapsedToCalcTotal = System.currentTimeMillis() - startTime;				
				
				// Set Amount with VAT
				double roundedAmountWithVat = Math.round(amountWithVat * 100.0) / 100d;
				PluginLog.info(String.format("Calculated Total Amount with VAT = %,.6f, rounded = %,.2f, setting into field %s", 
						amountWithVat, roundedAmountWithVat, fldDestTotal.getName()));
				fldDestTotal.setValue(String.format("%.2f", roundedAmountWithVat));				
				
				// 2. Calculate and set the Net rate
				PluginLog.info(String.format("Field %s value = %,.4f, VAT rate = %,.2f, Result = %,.9f into field %s", 
						fldSrc.getName(), rateWithVat, vatRate, rateExclVat, fldDestRate.getTranfName()));
				fldDestRate.setValue(rateExclVat);

				long elapsedTotal = System.currentTimeMillis() - startTime;
				
				PluginLog.debug(String.format("Processing times:\t elapsedToFields = %d ms\t elapsedToVatRate = %d ms\t elapsedToCalcTotal = %d ms\t elapsedTotal = %d ms", 
						elapsedToFields, elapsedToVatRate, elapsedToCalcTotal, elapsedTotal));
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

			// Get our Term currency
			// for PM there're no cases at JM where the base/term currencies are swapped in the ticker, so we can safely use single one always
			Field fldPymtCcy = tran.getLeg(0).getField(EnumLegFieldId.Currency);
			PluginLog.debug("currency: " + fldPymtCcy);

			
			/*
			 *  TODO Algorithm:
			 *  1. Target = Amount with VAT (tran info field)
			 *  2. Gross Amount = sum of (Cash position + Tax position) on all profiles
			 *  3. Difference = Target - Gross Amount
			 *  RAISE WARNING if abs(Difference) > 0.01 * number of profiles, DO NOT ADJUST events in this case
			 *  4. Tax Settle Amount on First profile = Tax position + Difference
			 */
			
			Field fldSrc = tran.getField(FIELD_DEST_AMOUNT_WITH_VAT);

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
					FIELD_DEST_AMOUNT_WITH_VAT, amountWithVat));

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
	protected double maxRoundingDiff() {
		return MAX_RONDING_DIFF;
	}

	@Override
	protected boolean isSupported() {
		// This class is for LoanDep toolset/DEPO-ML and LOAN-ML instruments
		if(tran.getToolset() != EnumToolset.Loandep)
			return false;
		
		EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
		return (insType == EnumInsType.MultilegDeposit 
				|| insType == EnumInsType.MultilegLoan);
	}

	@Override
	protected double getVatRate() throws IllegalStateException {
		/*
		 * - ‘Reduced Tax’: it depends on the counterparty on the Lease if a reduced tax is applicable. Currently it’s only CPC. 
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
		
		try(LegalEntity party = (LegalEntity) session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.LegalEntity, partyId)) {
			String partyName = party.getName();
			EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
			ConstField fldReduced = party.getConstField(PARTY_FIELD_VAT_RATE_REDUCED);
			if(fldReduced.isApplicable() && fldReduced.getValueAsString().compareToIgnoreCase("Yes") == 0 && insType == EnumInsType.MultilegLoan) {
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
	
	private boolean validateFields(Field fldSrc,Field fldDestRate,Field fldDestTotal)
	{
		if(fldSrc == null)
		{
			PluginLog.warn("Source field is NULL, possibly something wrong with Tran Field notification setup");
			return false;
		}
		if(!fldSrc.isApplicable()) {
			PluginLog.warn("Source Field " + fldSrc.getName() + " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		}
		if(fldDestRate == null)
		{
			PluginLog.warn("Destination field is NULL, possibly something wrong with Tran Info field setup");
			return false;
		}
		if( !fldDestRate.isApplicable()) {
			PluginLog.warn("Destination field " + fldDestRate.getName() + " is not applicable, possibly something wrong with Tran Info field setup");
			return false;
		}
		if(fldDestTotal == null)
		{
			PluginLog.warn("Destination field NULL, possibly something wrong with Tran Field notification setup");
			return false;
			
		}
		if(!fldDestTotal.isApplicable()) {
			PluginLog.warn("Destination field " + fldDestTotal.getName() + " is not applicable, possibly something wrong with Tran Field notification setup");
			return false;
		}
		return true;
	}

}
