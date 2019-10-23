package com.matthey.testutil.toolsetupdate;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;

/**
 * This class handles the deal attributes that are common for Unique Toolsets.
 * @author jains03
 *
 */
public abstract class UniqueToolset extends DealInjector 
{

	public UniqueToolset(Transaction argClonedTransaction, DealDelta argDealDelta) 
	{
		super(argClonedTransaction, argDealDelta);
	}

	/**
	 * Invokes updateToolset of super class
	 * Adds warningMsg for the deal attributes that are not applicable for Unique Toolsets
	 */
	public void updateToolset() throws OException 
	{
		super.updateToolset();
	}
}
