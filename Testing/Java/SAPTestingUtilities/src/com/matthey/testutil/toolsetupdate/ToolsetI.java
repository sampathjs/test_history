package com.matthey.testutil.toolsetupdate;

import java.util.Map;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;

/**
 * Toolset Interface declares methods which facilitate Cloning of deals. 
 * @author SharmV04
 *
 */
public interface ToolsetI 
{
	/**
	 * Update the value of certain fields of the underlying Transaction.
	 * The fields to be updated can vary for different Toolset and Instruments. 
	 * @throws OException
	 */
	void updateToolset() throws OException;
	
	/**
	 * Set default value to all the tran info fields
	 * @param argInfoFieldDefaultValues
	 * @throws OException
	 */
	void updateTransactionInfo( Map<String, String> argInfoFieldDefaultValues) throws OException;
	
	/**
	 * This method creates a clone of the deal.
	 * @return tran_num of the created trade
	 * @throws OException
	 */
	Transaction createClone() throws OException;
	
	/**
	 * This method gets the SSI from the original tran.
	 * Copies all SSI to the cloned transaction.
	 * @throws OException
	 */
	void copySettlementInstructions() throws OException;
}
