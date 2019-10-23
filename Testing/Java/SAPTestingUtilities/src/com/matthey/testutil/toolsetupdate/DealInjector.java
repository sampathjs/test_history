package com.matthey.testutil.toolsetupdate;

import java.util.Map;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Abstract & default implementation of ToolsetI
 * This class handles the deal attributes that are common for all Toolsets.
 * This class must be extended by Toolset specific classes to provide handling of toolset specific deal attributes.
 * @author jains03
 *
 */
public abstract class DealInjector implements ToolsetI 
{
	protected Transaction transaction;
	protected DealDelta dealDelta;
	
	/**
	 * @param argClonedTransaction
	 * @param argDealDelta
	 */
	public DealInjector(Transaction argClonedTransaction, DealDelta argDealDelta) 
	{
		this.transaction = argClonedTransaction;
		this.dealDelta =argDealDelta;
	}

	/**
	 * Sets the value of following transaction fields -
	 * (i) Trade date
	 * (ii) Reference : Clone_<user input>_olddeal_<original deal number>
	 * @return Returns empty comments
	 */
	@Override
	public void updateToolset() throws OException
	{
		String referenceText = dealDelta.getTestReference();
		StringBuilder ref = new StringBuilder("Clone_");
		if(referenceText != null)
		{
			ref.append(referenceText);
		}
		ref.append("_olddeal_").append(dealDelta.getDealNum());
		// Reference text is Clone_<user input>_olddeal_<original deal number>
		updateTransactionField( TRANF_FIELD.TRANF_TRADE_DATE, 0, dealDelta.getTradeDate());
		updateTransactionField( TRANF_FIELD.TRANF_REFERENCE, 0, ref.toString());
		addDealComment(dealDelta.getDealComment());
	}

	/**
	 * @param fieldToUpdateEnum
	 * @param argLeg
	 * @param newValue
	 * @throws OException
	 */
	protected void updateTransactionField( TRANF_FIELD fieldToUpdateEnum,int argLeg, String newValue) throws OException 
	{
		int returnedValue = 1;
				
		PluginLog.debug("Updating transaction field " + fieldToUpdateEnum + " in leg "+ argLeg + " with value " + newValue);
		if (null != newValue && !newValue.isEmpty()) {
			returnedValue = transaction.setField(fieldToUpdateEnum.toInt(), argLeg, "", newValue);
			com.matthey.testutil.common.Util.setupLog();
		}
		if (returnedValue == 0) {
			com.matthey.testutil.common.Util.setupLog();
			final String ERROR_MESSAGE = "Value for field " + fieldToUpdateEnum + " not set";
			PluginLog.error(ERROR_MESSAGE );
			throw new OException(ERROR_MESSAGE);
		}
		PluginLog.debug("Value of metal weight in transaction: " + transaction.getField(TRANF_FIELD.TRANF_FX_D_AMT.jvsValue()));
	}
	
	/**
	 * @param tranInfoName
	 * @param newValue
	 * @throws OException
	 */
	protected void updateTranInfoField(String tranInfoName, String newValue) throws OException 
	{
		int returnedValue = 1;
				
		PluginLog.debug("Updating transaction field " + tranInfoName + " with value " + newValue);
		if (null != newValue && !newValue.isEmpty()) {
			returnedValue = transaction.setField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, tranInfoName, newValue);
			
			com.matthey.testutil.common.Util.setupLog();
		}
		if (returnedValue == 0) {
			com.matthey.testutil.common.Util.setupLog();
			PluginLog.error("Value for field " + tranInfoName + " not set");
			throw new OException("Value for field " + tranInfoName + " not set");
		}
	}
	
	@Override
	public void updateTransactionInfo( Map<String, String> infoFieldDefaultValues) throws OException
	{
		Table tranInfoValuesInTransactionTable = this.transaction.getTranInfo();
		int numberOfRows = tranInfoValuesInTransactionTable.getNumRows();
		/*
		 * Iterate all the tran info fields of the Transaction.
		 * Set the value to default (if default value is available in the @param infoFieldDefaultValues)
		 */
		for (int rowNumber = 1; rowNumber <= numberOfRows; rowNumber++)		
		{
			String infoFieldName = tranInfoValuesInTransactionTable.getString("Type", rowNumber);
			String defaultInfoFieldValue = infoFieldDefaultValues.get(infoFieldName);
			if(null != defaultInfoFieldValue)
			{
				tranInfoValuesInTransactionTable.setString("Value", rowNumber, defaultInfoFieldValue);
			}
		}
	}
	
	/**
	 * This method creates a clone of the deal in Validated state.
	 * @return tran_num of the created trade
	 * @throws OException
	 */
	@Override
	public Transaction createClone() throws OException
	{
		if(dealDelta.getStatusInEndur().equals("Quote"))
		{
			this.transaction.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_PENDING);
		}
		else if (dealDelta.getStatusInEndur().equals("Validated"))
		{
			this.transaction.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);
		}
		else if (dealDelta.getStatusInEndur().equals("New"))
		{
			this.transaction.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_NEW);
		}
		
		return this.transaction;
	}
	
	/**
	 * @param dealComment
	 * @throws OException
	 */
	private void addDealComment(String dealComment) throws OException
	{
		if(dealComment != null && !dealComment.isEmpty())
		{
			this.transaction.loadDealComments();
			this.transaction.addDealComment();
			int numDealComments = this.transaction.getNumRows(0, TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt());

			this.transaction.insertDealComment(numDealComments);
			this.transaction.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_COMMENT.toInt(), 0, "", dealComment, numDealComments);
		}
	}

	@Override
	public void copySettlementInstructions() throws OException 
	{
		int originalTranNum = this.dealDelta.getTranNum();
		Transaction originalTran = Util.NULL_TRAN;
		Table originalSSI = Util.NULL_TABLE;
		Table clonedSSI = Util.NULL_TABLE;
		try
		{
			PluginLog.debug("Copying settlement instruction from originalTranNum=" + originalTranNum);
			originalTran = Transaction.retrieve(originalTranNum);
			originalSSI = originalTran.getSettlementInstructionsTable();
			clonedSSI = this.transaction.getSettlementInstructionsTable();

			clonedSSI.clearRows();
			originalSSI.copyRowAddAll(clonedSSI);
			this.transaction.verifySettlementInstructions();
			
		}
		finally
		{
			if(Transaction.isNull(originalTran) != 1)
			{
				originalTran.destroy();	
			}
		}		
	}
	
}
