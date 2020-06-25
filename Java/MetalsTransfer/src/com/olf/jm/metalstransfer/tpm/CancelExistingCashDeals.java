package com.olf.jm.metalstransfer.tpm;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.dealbooking.CashTransfer;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CancelExistingCashDeals extends AbstractProcessStep {
	String strategyRef = null ;
	 @Override
	    public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
		   Logging.init(context, this.getClass(), this.getClass().getSimpleName(), "UI");
		 	int tranNum = process.getVariable("TranNum").getValueAsInt();
	        TradingFactory factory = context.getTradingFactory();
	        try (Table results = context.getIOFactory().runSQL(getCashDeals(tranNum))){
	        	if(results.getRowCount()== 0  ){
	        		Logging.info("No cash deals were found for cancellation against strategy "+tranNum);
	        	}else{
	        	long wflowId = Tpm.getWorkflowId();
	        	Tpm.setVariable(wflowId,"IsRerun","Yes");
	        		  try (Transaction strategy = factory.retrieveTransactionById(tranNum)){
	        	  
		        	
		        	//int latestTranStatus = CashTransfer.
		        	strategyRef = strategy.getValueAsString(EnumTransactionFieldId.ReferenceString);	
		        	Logging.info("Cancelling existing CASH deals for Strategy "+tranNum+ " with  reference "+ strategyRef);
		        	
		            CashTransfer.cancelDeals(context, strategy);	            
		        }
	        	}
	         
	        } catch (OException e) {
	        	Logging.error("Error while Cancelling existing CASH deals for Strategy "+tranNum+ " with  reference "+ strategyRef+ " \n" +e.getMessage());
	        	Util.exitFail();
			}
	        
	        finally {
	            Logging.close();
	        }
			return null;
		
	
	 }
	protected String getCashDeals(int tranNum){
			
		return    "SELECT ab.tran_num as tran_num from ab_tran ab LEFT JOIN ab_tran_info ai \n" + 
								 " ON ab.tran_num = ai.tran_num \n" + 
								 " WHERE ai.value = " + tranNum+ " \n" +
								 " AND ai.type_id = 20044 "+ " \n" +
								 " AND tran_status in (" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + "," + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ","+ TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt() + ")";
					 
					
				
}
}
