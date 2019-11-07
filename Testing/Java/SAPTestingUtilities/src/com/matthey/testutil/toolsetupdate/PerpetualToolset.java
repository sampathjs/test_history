package com.matthey.testutil.toolsetupdate;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;

/**
 * This class handles the deal attributes that are common for Perpetual Toolsets.
 * @author SharmV04
 *
 */
public abstract class PerpetualToolset extends DealInjector 
{
	/**
	 * @param argClonedTransaction
	 * @param argDealDelta
	 */
	public PerpetualToolset(Transaction argClonedTransaction, DealDelta argDealDelta) 
	{
		super(argClonedTransaction, argDealDelta);
	}
	
	public void updateToolset() throws OException 
	{
		super.updateToolset();
	}
}
