package com.matthey.testutil;

import com.matthey.testutil.exception.SapTestUtilException;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

/**
 * Performs a bulk operation which has been split into a number of simple steps:
 * 1. Gather input data
 * 2. Perform some ops on the input data
 * 3. Extract output results to todays reporting directory, subfolder'd by the name of the bulk operation
 */
public abstract class BulkOperationScript extends BaseScript 
{
	/* A copy of argt to expose out */
	private Table tblArgt = null;
	
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		tblArgt = context.getArgumentsTable().copyTable();
		
		Table tblInputData = null;
		Table tblOutputData = null;
		
		try 
		{
			setupLog();
			
			tblInputData = generateInputData();
			
			tblOutputData = performBulkOperation(tblInputData);
			
			com.matthey.testutil.common.Util.generateCSVFile(tblOutputData, getOperationName(), null);
		} 
		catch (Exception e) 
		{
			PluginLog.error("Exception occured during Bulk Deal Operation: " + e.getMessage());
			throw new RuntimeException(e);
		}
		finally
		{
			if (tblInputData != null)
			{
				tblInputData.destroy();
			}
			
			if (tblOutputData != null)
			{
				tblOutputData.destroy();
			}
			
			/* tblArgt = copy, destroy it */
			if (tblArgt != null)
			{
				tblArgt.destroy();
			}
		}
	}
	
	/**
	 * @return Argument table
	 */
	public Table getArgt()
	{
		return this.tblArgt;
	}
	
	/**
	 * Generate a list of deals given the input CSV file. The CSV can be structured in any custom way
	 * and this functions provides a placeholder for any custom input rules from client to client
	 * 
	 * @return
	 * @throws OException 
	 */
	public abstract Table generateInputData() throws OException;
	
	/**
	 * Perform the needful operation
	 * 
	 * @param tbDeals
	 * @throws OException
	 */
	public abstract Table performBulkOperation(Table tblInput) throws OException, SapTestUtilException;
	
	/**
	 * Return the name of the operation
	 * @return
	 */
	public abstract String getOperationName();
}