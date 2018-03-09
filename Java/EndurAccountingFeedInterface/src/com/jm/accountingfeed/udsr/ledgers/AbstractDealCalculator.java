package com.jm.accountingfeed.udsr.ledgers;

import com.olf.openjvs.Transaction;

/**
 * Abstract implementation of Deal Calculator to define underlying Transaction pointer  
 * @author jains03
 *
 */
public abstract class AbstractDealCalculator implements IDealCalculator
{
	protected Transaction tran;
	
	public AbstractDealCalculator()
	{
		
	}
	
	public AbstractDealCalculator(Transaction tran)
	{
		this.tran = tran;
	}
}
