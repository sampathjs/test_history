package com.matthey.testutil.toolsetupdate;

import java.util.Date;
import java.util.Map;

import com.matthey.testutil.enums.EndurTranInfoField;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

public class ComposerToolset extends PerpetualToolset 
{
	public ComposerToolset(Transaction argClonedTransaction, DealDelta argDealDelta) 
	{
		super(argClonedTransaction, argDealDelta);
	}

	public void updateToolset() throws OException 
	{
		PluginLog.info("Started updating fiels in toolset");
		
		updateTransactionField(TRANF_FIELD.TRANF_SETTLE_DATE, 0, dealDelta.getSettleDate());
		
		int timestamp = (int) new Date().getTime();
		updateTransactionField(TRANF_FIELD.TRANF_REFERENCE, 0, dealDelta.getTestReference() + "_" + timestamp);
		
		/* We set via tran.setField so instrument builder scripts get triggered (event/field notifications) */
		updateTranInfoField(EndurTranInfoField.TO_ACC_LOCO.toString(), dealDelta.getToAccLoco());
		updateTranInfoField(EndurTranInfoField.TO_ACC_FORM.toString(), dealDelta.getToAccForm());
		updateTranInfoField(EndurTranInfoField.TO_ACC.toString(), dealDelta.getToAcc());
		updateTranInfoField(EndurTranInfoField.TO_ACC_BU.toString(), dealDelta.getToAccBu());
		
		updateTranInfoField(EndurTranInfoField.SAP_METAL_TRANSFER_REQUEST_NUMBER.toString(), dealDelta.getMtrNum());
		
		PluginLog.info("Completed updating fiels in toolset");
	}
	
	@Override
	public void updateTransactionInfo(Map<String, String> infoFieldDefaultValues) throws OException 
	{
		/* Overriden to do nothing on Composer toolsets as target fields are all tran infos! */
	}
	
	@Override
	public Transaction createClone() throws OException
	{
		super.createClone();			
		return this.transaction;
	}
	
	@Override
	public void copySettlementInstructions() throws OException 
	{
		/* Overriden to intentionally do nothing */
	}
}
