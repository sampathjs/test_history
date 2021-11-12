package com.olf.jm.operation;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.trading.EnumFeeFieldId;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Fee;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

/**
 * 
 * When clear Tran Num is pressed, clear Broker fee fields.
 * 
 * Revision History:
 * Version  Updated By    Date         Ticket#    Description
 * -----------------------------------------------------------------------------------
 * 	01      Prashanth     23-Jul-2021  EPI-1712   Initial version
 *  02      Gaurav        27-Sep-2021  EPI-1897   'Clear Tran Num'for Physical Dispatch deals. 
 */

@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class ClearBrokerFees extends AbstractTransactionListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Broker Fee";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "Trading";
	
	@Override
	public void notify(Context context, Transaction tran) {
		try {
			init();			
			if(isDispatchDeals(tran)){
				Logging.info("Dispatch Deal:, Clear Tran is starting");
				processClearTranForPhyDispatch(tran, context);
				Logging.info("Dispatch Deal:, Clear Tran is complating");
			 } 

			//Clear Broker
			if(tran.getField("Broker").isApplicable() && tran.getField("Broker").getValueAsInt()>0){
				tran.getField("Broker").setValue(0); 
			}
			
		} catch (Exception e) {
			Logging.error("Error clearing Broker Fee  Fields. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}
	
	private boolean isDispatchDeals(Transaction tran) {

		String  insType = tran.getField(EnumTransactionFieldId.InstrumentType).getValueAsString(); 
		String buySellFixLeg = tran.getField(EnumTransactionFieldId.BuySell).getValueAsString();    
		 
	 	if(buySellFixLeg.equalsIgnoreCase("Sell") && tran.getField("Dispatch Status").isApplicable() && insType.equalsIgnoreCase("COMM-PHYS")){
	 		return true;
		  }
		return false;
		
	}

	private void processClearTranForPhyDispatch(Transaction tran, Context context) {
	 	tran.getField("Dispatch Status").setValue("None");
	 	Date tradingDate = context.getTradingDate();  
	 	for (Leg leg : tran.getLegs()) {
			if(!( leg.getField(EnumLegFieldId.StartDate).isReadOnly())){
				 leg.getField(EnumLegFieldId.StartDate).setValue(tradingDate  );
			}
			if(!( leg.getField(EnumLegFieldId.RateSpread).isReadOnly())){
				 leg.getField(EnumLegFieldId.RateSpread).setValue("1.00");
			}
			
			if(!( leg.getResetDefinition().getField(EnumResetDefinitionFieldId.PaymentDateOffset).isReadOnly())){
					leg.getResetDefinition().getField(EnumResetDefinitionFieldId.PaymentDateOffset).setValue(new SimpleDateFormat("dd-MMM-yyyy").format(tradingDate)); 
					 
				}  
			  
		 	for (Fee fee :  leg.getFees()) {
				if(!( fee.getField(EnumFeeFieldId.OneTimePaymentDate).isReadOnly())){
					fee.getField(EnumFeeFieldId.OneTimePaymentDate).setValue(tradingDate); 
					
				}  
			}
	 	} 
	}

	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */	private void init() throws Exception {
		try {
			Logging.init(this.getClass(), constRep.getContext(), constRep.getSubcontext());
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
}