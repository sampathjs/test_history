package com.olf.jm.metalstransfer.tpm;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.dealbooking.CashTransfer;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.TpmStep })
public class ProcessStrategy extends AbstractProcessStep {

	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		int strategyDealNum = process.getVariable("DealNum").getValueAsInt();
		try {
			int iTargetStatus = process.getVariable("TargetStatus").getValueAsInt();
			EnumTranStatus targetStatus = EnumTranStatus.retrieve(iTargetStatus);
			
			Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
			Logging.info(String.format("Input strategy deal#%d, Target Status-%s", strategyDealNum, targetStatus.getName()));
			
			process(context, process, strategyDealNum, targetStatus);
			return null;

		} catch (RuntimeException e) {
            Logging.error("Process failed for strategy deal#" + strategyDealNum + ": ", e);
            throw e;
            
        } finally {
			Logging.close();
		}
	}
	
	private void process(Context context, Process process, int dealNum, EnumTranStatus targetStatus) {
		TradingFactory tf = context.getTradingFactory();
		try (Transaction strategy = tf.retrieveTransactionByDeal(dealNum)) {
			EnumTranStatus currStatus = strategy.getTransactionStatus();
			
			switch(targetStatus.getValue()) {
				case 14:
					if (currStatus == EnumTranStatus.Validated || currStatus == EnumTranStatus.Cancelled
							|| currStatus == EnumTranStatus.Matured || currStatus == EnumTranStatus.Deleted) {
						Logging.info(String.format("Current status is %s for strategy deal#%d", currStatus.getName(), dealNum));
						Logging.info(String.format("Not processing strategy deal#%d to %s status", dealNum, targetStatus.getName()));
						//do Nothing
					} else {
						Logging.info(String.format("Trying to find & cancel CASH Transfer deals for strategy#%d, if any", dealNum));
						CashTransfer.cancelDeals(context, strategy);
						
						Logging.info(String.format("Processing strategy deal#%d to %s status", dealNum, targetStatus.getName()));
						strategy.process(targetStatus);
						Logging.info(String.format("Successfully processed strategy deal#%d to %s status", dealNum, targetStatus.getName()));
					}
					break;
				default:
					break;
			}
			
		} catch(Exception e) {
			throw e;
		}
		
		//To process it to Cancelled -
			//If status = Validated, then cancel the CASH Transfer deals & process the strategy to Cancelled
			//If status != Validated, do nothing
	}

}
