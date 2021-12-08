package com.olf.jm.metalswaputil.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
/*
 * History:
 * 2021-11-10	V1.0	RodriR02	- Initial version - Setting the Payment Date Offset to 7d for Tanaka deals                                   
 */

/**
 * This plugin sets the Payment Date Offset to 7d for Tanaka deals on the float side 
 * (labeled: Payment Date Offset) in case the
 * Payment Date Offset changes on the fixed side of the deal.
 * @author RodriR02
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class MetalSwapPaymentDateOffset extends AbstractTransactionListener {

	private static final String CONTEXT = "FrontOffice";
	private static final String SUBCONTEXT = "MetalSwap";
    /**
     * {@inheritDoc}
     */
	@Override
    public void notify(final Context context, final Transaction tran) {
		initLogging();
		try {
			if (!isTanakaDeal(tran)) {
				Logging.info("Not Tanakka Deal, skipping");
				return;
			}
			
			for (Leg leg : tran.getLegs()) {
				Field field = leg.getResetDefinition().getField(EnumResetDefinitionFieldId.PaymentDateOffset);
				if (field == null || !field.isApplicable() || !field.isWritable()) {
					continue;
				}
				if(leg.getField(EnumLegFieldId.FixFloat).getValueAsString().equalsIgnoreCase("Float")){
					field.setValue("7d");
					Logging.info("PaymentDateOffset field is set for leg " + leg.getLegNumber());
				}
			}
			
			Logging.info("Finishes processing transaction");			
		} catch (Throwable t) {
			Logging.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
		}finally{
			Logging.close();
		}
    }
	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() {
		// Constants Repository Statics
		try {
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			throw new RuntimeException(errMsg, e);
		}
	}
	
	private boolean isTanakaDeal(Transaction tranPtr) {
		boolean blnReturn = false;
		try {
			int cptLE = tranPtr.getField(EnumTransactionFieldId.ExternalLegalEntity).getValueAsInt();
			int tanakaLE = com.olf.openjvs.Ref.getValue(com.olf.openjvs.enums.SHM_USR_TABLES_ENUM.PARTY_TABLE, "TANAKA KIKINZOKU KOGYO KK - LE");

			if (cptLE == tanakaLE ) {
				blnReturn = true;
			}
		} catch (OException e) {
			Logging.error(e.getMessage(), e);
		}
		return blnReturn;
	}
}
