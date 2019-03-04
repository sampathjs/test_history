package com.olf.jm.vatinclusivecalc;

import com.olf.openjvs.OException;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UnsupportedException;
import com.olf.openrisk.staticdata.ConstField;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Abstract root class for tran fields and events handling for VAT Inclusive deals.
 * Concrete classes will define, which fields to check and update for specific toolsets and/or instrument types
 * @author iborisov
 * 
 */
public abstract class VatInclusiveTranHandler implements AutoCloseable {
	protected Session session;
	protected Transaction tran;
	
	protected static final String MSG_GUI_WARNING_PREFIX = "VAT-Inclusive Price Handling";
	protected static final String MSG_NO_PARTY_NO_VAT = "Party is not specified, VAT rate cannot be determined";
	
	protected static final String PARTY_FIELD_VAT_RATE_REDUCED = "LBMA Member";
	protected double VAT_RATE_REDUCED = 1.06;
	protected static final String PARTY_FIELD_VAT_ZERO = "LPPM Member";
	protected  double VAT_RATE_STANDARD = 1.16;

	protected static final String FIELD_EVENT_AMOUNT = "para_position";
	
	private  double MAX_RONDING_DIFF = 0.01;
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "VAT Inclusive Field Deal Entry"; // sub context of constants repository
	
	public static final String STRATEGY_EXTERNAL_BUSINESS_UNIT = "To A/C BU";
	
	public VatInclusiveTranHandler(Session session, Transaction tran) {
		this.session = session;
		this.tran = tran;
		ConstRepository constRep;
		try {
			constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			MAX_RONDING_DIFF = constRep.getDoubleValue("RoundingTolerance");
		} catch (OException e) {
			e.printStackTrace();
		}
		initVatValues();
		PluginLog.debug("Handler " + this.getClass().getSimpleName() + " created for " + tran.toString());
	}
	
	/**
	 * Updates the applicable transaction fields according to the value entered in the fields with VAT and the VAR rate.<br>
	 * Method will first check if the transaction is applicable to ensure only supported versions 
	 * @throws UnsupportedException in case when the transaction is not supported yet
	 */
	public abstract void updateDependingFields() throws UnsupportedException;
	
	/**
	 * Reviews the generated events and updates amount on them to be consistent with initially entered figures with VAT.<br>
	 * Method will first check if the transaction is applicable to ensure only supported versions. It will ignore unsupported trans
	 * @throws Exception 
	 */
	public abstract void adjustEvents() throws Exception;
	
	/**
	 * Checks if the transaction is supported, i.e. the combination of toolset, ins-type, cashflow type etc. is supported
	 * @return true is transaction is supported, otherwise false
	 */
	protected abstract boolean isSupported();
	
	/**
	 * Returns maximum allowed tolerance for rounding differences for compating amounts with and without VAT
	 */
	protected double maxRoundingDiff() {
		return MAX_RONDING_DIFF;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		if(tran != null)
			tran.close();		
	}

	/**
	 * Method retrieves the VAT rate factor for the transaction
	 * @return
	 */
	protected double getVatRate() throws IllegalStateException {
		PluginLog.debug("Retrieving VAT rate using Party info from External LE");
		EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
		int partyId;
		if(insType == EnumInsType.Strategy)
		{
			Field extBuField = tran.getField(STRATEGY_EXTERNAL_BUSINESS_UNIT);
			String buName = extBuField.getValueAsString();
			int id = extBuField.getValueAsInt();
			String sql = "select legal_entity_id from party_relationship where business_unit_id = "+id;
			Table legalUnitTable = session.getIOFactory().runSQL(sql);
			partyId = legalUnitTable.getInt(0, 0);
			PluginLog.debug("BU for Strategy "+buName);
			legalUnitTable.dispose();
		}
		else
		{
			
			partyId = tran.getValueAsInt(EnumTransactionFieldId.ExternalLegalEntity);
		}
		if(partyId <= 0)
			throw new IllegalStateException(MSG_NO_PARTY_NO_VAT);
				
		double rate;
		
		try(LegalEntity party = (LegalEntity) session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.LegalEntity, partyId)) {
			ConstField fldStandard = party.getConstField(PARTY_FIELD_VAT_ZERO);
			if(fldStandard.isApplicable() && fldStandard.getValueAsString().compareToIgnoreCase("Yes") == 0) {
				rate = 0;
				PluginLog.debug(String.format("Field %s on %s is set to Yes, VAT rate is %.2f", PARTY_FIELD_VAT_ZERO, 
						party.getName(), rate));
			}
			else {
				rate = VAT_RATE_STANDARD;
			}
			
			PluginLog.debug(String.format("VAT rate is %.2f", rate));
		}
		
		return rate;
	}
	protected void initVatValues()
	{
		PluginLog.debug("Retrieving VAT rate from Tax module...");
		Table resultReducedRate= null, resultStandardRate= null;
		try
		{
			
			
			String sqlVatReduced = "SELECT charge_rate " 
					+" FROM   tax_rate  "
					+" WHERE  party_id = (select party_id from party where short_name ='CN TAX') " 
					+" AND short_name = 'CN Red Tax' " 
					+" AND effective_start_date < (SELECT trading_date " 
					+" FROM   system_dates)  "
					+" AND ( effective_end_date > (SELECT trading_date  "
					+" FROM   system_dates)  "
					+" OR effective_end_date = '' )  ";
			
			PluginLog.debug(" sqlVatReduced \n"+sqlVatReduced);
			resultReducedRate = session.getIOFactory().runSQL(sqlVatReduced);
			if(resultReducedRate.getRowCount() > 0)
			{
				this.VAT_RATE_REDUCED = 1 + resultReducedRate.getDouble(0, 0);
				PluginLog.debug("Reduced vat from DB "+this.VAT_RATE_REDUCED);
			}
			else
			{
				PluginLog.debug("The reduced vat is not set from Tax module. Value defaulted to "+this.VAT_RATE_REDUCED);
			}
			
			String sqlVatStandard = "SELECT charge_rate " 
					+" FROM   tax_rate "
					+" WHERE  party_id = (select party_id from party where short_name ='CN TAX') "  
					+" AND short_name = 'CN Std Tax' " 
					+" AND effective_start_date < (SELECT trading_date " 
					+" FROM   system_dates) "
					+" AND ( effective_end_date > (SELECT trading_date " 
					+" FROM   system_dates) "
					+" OR effective_end_date = '' )";
			PluginLog.debug("sqlVatStandard \n"+sqlVatStandard);
			
			resultStandardRate = session.getIOFactory().runSQL(sqlVatStandard);
			if(resultReducedRate.getRowCount() > 0)
			{
				this.VAT_RATE_STANDARD = 1 + resultStandardRate.getDouble(0, 0);
				PluginLog.debug("Standard vat from DB "+this.VAT_RATE_STANDARD);
			}
			else
			{
				PluginLog.debug("The standard vat is not set from Tax module. Value defaulted to "+ this.VAT_RATE_STANDARD);
			}

		}
		finally
		{
			resultReducedRate.dispose();
			resultStandardRate.dispose();
		}

	}

	/**
	 * Common code for most instruments to adjust events to the desired total amount with VAT
	 * @param amountWithVat
	 * @param termCcy
	 * @throws Exception
	 */
	protected void checkAndUpdateEventsWithJVS(double amountWithVat, Field ccy) throws Exception {
		com.olf.openjvs.Table cashEvents = com.olf.openjvs.Table.tableNew();
		com.olf.openjvs.Table taxEvents = com.olf.openjvs.Table.tableNew();
		try {
			int cashRet = com.olf.openjvs.Transaction.eventRetrieveEvents(tran.getTransactionId(), EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE, cashEvents);
			int taxRet = com.olf.openjvs.Transaction.eventRetrieveEvents(tran.getTransactionId(), EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE, taxEvents);

			// Find the events for our currency
			int ccyId = ccy.getValueAsInt();
			
			int iCashEventPos = 0;
			double cashPos = 0.0;
			int cashEventsCount = 0;
			int cashEventRowCount = cashEvents.getNumRows();
			PluginLog.debug("Event row count "+ cashEventRowCount);
			if(cashRet == 1)
				for(int i = 1; i <= cashEventRowCount; i++) {
					if(cashEvents.getInt("currency", i) == ccyId) {
						cashPos += cashEvents.getDouble(FIELD_EVENT_AMOUNT, i);
						if(iCashEventPos <= 0) // Remember our first event for later use
							iCashEventPos = i;
						cashEventsCount++;
					}
				}
			
			int iTaxEventPos = 0;
			double taxPos = 0.0;
			int taxEventsRowCounter = taxEvents.getNumRows();
			PluginLog.debug("TaxEvent row count"+ taxEventsRowCounter);
			if(taxRet == 1)
				for(int i = 1; i <=taxEventsRowCounter; i++) {
					if(taxEvents.getInt("currency", i) == ccyId) {
						taxPos += taxEvents.getDouble(FIELD_EVENT_AMOUNT, i);
						if(iTaxEventPos <= 0) // Remember our first event for later use
							iTaxEventPos = i;
					}
				}
			
			if(cashEvents.isRowNumValid(iCashEventPos) != 1 || taxEvents.isRowNumValid(iTaxEventPos) != 1) {
				String msg = String.format("Can't find Cash or Tax settlement events for %s %s, skipping the deal", 
						ccy.getName(), ccy.getValueAsString());
				
				// Just in case check if the deal is Validated (we should not ever get here if not), we can't expect events when it is still New or Pending
				if(EnumTranStatus.Validated == tran.getTransactionStatus()) {
					PluginLog.error(msg);
					if(com.olf.openjvs.Util.canAccessGui() == 1)
						com.olf.openjvs.Ask.ok(MSG_GUI_WARNING_PREFIX + ": " + msg); // It does works in In-Process Post-proc
				}
				else 
					PluginLog.warn(msg);
				
				return;
			}
			
			// Get the total position
			double grossAmt = cashPos + taxPos;
			// Round for comparison
			grossAmt = Math.round(grossAmt * 100.0) / 100d;
			// Adjust amountWithVat (always positive) to have the sign of grossAmt (negative for Buy)
			if(amountWithVat > 0 && grossAmt < 0 || amountWithVat < 0 && grossAmt > 0)
				amountWithVat *= -1;
			
			// 3. Difference = Target - Gross Amount
			double diff = amountWithVat - grossAmt;
			double tolerance = maxRoundingDiff() * cashEventsCount;
			PluginLog.info(String.format("Cash events amount = %,.4f, Tax events amount = %,.4f, Total = %,.4f, Amount with VAT = %,.4f, Diff = %,.4f, Tolerance =  %,.2f", 
					cashPos, taxPos, grossAmt, amountWithVat, diff, tolerance));
			
			// Raise a warning if difference's unexpectedly large
			double absDiffForCmp = Math.abs(Math.round(diff * 100.0) / 100d);
			if(absDiffForCmp > tolerance) {
				String msg = String.format("The difference between the desired Gross Amount %.2f and  /n the Sum of generated Cash and Tax events %.2f is greater than the defined tolerance: %.2f "+
						" Please check the deal and party data and the tax configuration! /n"+
						"If the difference is ok, then set to Tolerance higher to allow the adjustment of the event.", grossAmt,amountWithVat,maxRoundingDiff());
				
				/*
				String msg = String.format("Rounding difference on %d events is %.2f, which is higher than %.2f (%.2f x number of events)! The deal will not be adjusted.", 
						cashEventsCount, absDiffForCmp, tolerance, maxRoundingDiff());
						*/
				PluginLog.warn(msg);
				PluginLog.warn("Tolarance not within limits for deal "+tran.getDealTrackingId());
				if(com.olf.openjvs.Util.canAccessGui() == 1)
				{
					PluginLog.debug("Pop up window with error message will be shown");
					com.olf.openjvs.Ask.ok(MSG_GUI_WARNING_PREFIX + ": " + msg); // It does work in In-Process Post-proc
					
				}
				return;
			}
			
			// Update events
			if(grossAmt != amountWithVat) {
				// Tax Settle Amount = Tax position + Difference
				double taxAmountToAjust = taxEvents.getDouble(FIELD_EVENT_AMOUNT, iTaxEventPos);
				
				// Check the rare condition when applying the difference will change the sign on the tax event amount - and block it.
				if(Math.abs(diff) >= Math.abs(taxAmountToAjust) 
						&& (diff < 0 && taxAmountToAjust > 0 
							|| diff > 0 && taxAmountToAjust < 0)) {
					
					String msg = String.format("Rounding difference %.2f is higher than selected tax event amount %.2f, applying it will invert tax event! The deal will not be adjusted.", 
							cashEventsCount, diff, tolerance, maxRoundingDiff());
					PluginLog.warn(msg);
					if(com.olf.openjvs.Util.canAccessGui() == 1)
						com.olf.openjvs.Ask.ok(MSG_GUI_WARNING_PREFIX + ": " + msg); // It does work in In-Process Post-proc
					return;
				}
					
				taxAmountToAjust += diff;
				PluginLog.info(String.format("Updating %d Tax settle event amount to = %,.4f and saving events", 
						iTaxEventPos, taxAmountToAjust));
				int ret = com.olf.openjvs.Transaction.setTranEventSettleAmt(taxEvents.getInt64("event_num", iTaxEventPos), taxAmountToAjust, 
						taxEvents.getInt("currency", iTaxEventPos), taxEvents.getInt("unit", iTaxEventPos));
				if(ret != 1)
					PluginLog.error("...failed to update tran event, return code not 1");
			}				
		} 
		finally {
			if(com.olf.openjvs.Table.isTableValid(taxEvents) == 1)
				taxEvents.destroy();
			if(com.olf.openjvs.Table.isTableValid(cashEvents) == 1)
				cashEvents.destroy();
		}
	}
	
}
