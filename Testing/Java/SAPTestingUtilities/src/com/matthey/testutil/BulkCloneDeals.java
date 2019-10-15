package com.matthey.testutil;

/**
 * Base class for performing bulk cloning of deals. 
 *
 * Can extend/further abstract as necessary
 */
public abstract class BulkCloneDeals extends BulkOperationScript
{
	@Override
	public String getOperationName() 
	{
		return "Bulk Clone Deals";
	}
}
