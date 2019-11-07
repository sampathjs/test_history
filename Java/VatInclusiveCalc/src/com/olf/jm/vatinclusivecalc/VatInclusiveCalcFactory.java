package com.olf.jm.vatinclusivecalc;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.Transaction;

/**
 * Factory class to build VAT-inclusive transaction handlers
 * @author iborisov
 *
 */
public class VatInclusiveCalcFactory {

	public static VatInclusiveTranHandler createHandler(Session session, Transaction tran) {
		EnumToolset toolset = tran.getToolset();
		
		// If derived ins types are introduced, the below may start failing and needs to be changed to check the names/ids
		EnumInsType insType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
				
		switch(toolset) {
		case Fx:
			return new VatInclusiveFxTranHandler(session, tran);
			
		case Cash:
			switch(insType) {
			case CashInstrument:
				return new VatInclusiveCashTranHandler(session, tran);
			default:
				return null;
			}
			
		case Loandep:
			switch(insType) {
			case MultilegDeposit:
			case MultilegLoan:
				return new VatInclusiveLoanDepTranHandler(session, tran);
			default:
				return null;
			}
		case Composer:
			switch(insType) {
			case Strategy:
				return new VatInclusiveStrategyTranHandler(session, tran);
			default:
				return null;
			}
			
		default:
			return null;
		}
	}

}
