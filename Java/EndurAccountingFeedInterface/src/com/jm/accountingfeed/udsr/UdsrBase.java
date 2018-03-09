package com.jm.accountingfeed.udsr;

import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.openlink.util.logging.PluginLog;

/**
 * Abstract class for UDSR - defines execution flow.
 * @author jains03
 *
 */
public abstract class UdsrBase implements IScript 
{	
	protected Table tblArgt;
	protected Table tblReturnt; 
	protected Table tblTransactions;

	public void execute(IContainerContext context) throws OException 
	{
		Util.setupLog();
		
		tblArgt = context.getArgumentsTable();
		tblReturnt = context.getReturnTable();

		USER_RESULT_OPERATIONS op = USER_RESULT_OPERATIONS.fromInt(tblArgt.getInt("operation", 1));

		try 
		{
			switch (op) 
			{
			case USER_RES_OP_CALCULATE:
				if (preCalculateCheck(context)) 
				{
					tblTransactions = tblArgt.getTable("transactions", 1);

					calculate(context);
				}
				break;
			case USER_RES_OP_FORMAT:
				format(context);				
				break;
			case USER_RES_OP_AGGREGATE:
				aggregate(context);
				break;
			case USER_RES_OP_FINALIZE_AGGREGATE:
				finalizeAggregate(context);
				break;
			default:
				break;
			}
		} 
		catch (Exception e) 
		{
			Util.printStackTrace(e);
			tblArgt.setString("error_msg", 1, e.getMessage());

			PluginLog.info(e.getMessage());
			PluginLog.error(e.getMessage());
		} 
		finally 
		{
			PluginLog.info("*-End of the script");
		}
	}

	/**
	 * Implement in subclass.
	 * 
	 * @param context	Endur context.
	 * @throws OException
	 * @throws OException 
	 */
	protected abstract void calculate(IContainerContext context) throws OException;

	/**
	 * Implement in subclass.
	 * 
	 * @param context	Endur context.
	 * @throws OException
	 */
	protected abstract void format(IContainerContext context) throws OException;

	/**
	 * Add columns to the outData table.
	 * @param outData
	 * @return
	 * @throws OException
	 */
	protected Table defineOutputTable(Table outData) throws OException
	{
		return outData;
	}

	/**
	 * Default aggregate method when running on grid.
	 * Subclass may overwrite if needed.
	 * 
	 * @param context	Endur context.
	 * @throws OException
	 */
	protected void aggregate(IContainerContext context) throws OException 
	{
		Table argt = context.getArgumentsTable();
		argt.getTable("current_results", 1).copyRowAddAll(argt.getTable("master_results", 1));
	}

	/**
	 * Default finalizeAggregate method when running on grid.
	 * Subclass may overwrite if needed.
	 * 
	 * @param context	Endur context.
	 * @throws OException
	 */
	protected void finalizeAggregate(IContainerContext context) throws OException 
	{
		//do nothing by default
	}

	/**
	 * Default check before calling calculate.
	 * When running batch sim by portfolio, there may not be any transactions.
	 * Subclass may overwrite if needed.
	 * 
	 * @param context	Endur context.
	 * @return True to proceed, false otherwise.
	 * @throws OException
	 */
	protected boolean preCalculateCheck(IContainerContext context) throws OException 
	{
		Table transaction = context.getArgumentsTable().getTable("transactions", 1);

		return (transaction == null) ? false : transaction.getNumRows() > 0;
	}

	/**
	 * Return a nicely formatted String representing the duration between a start and end time
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	protected String getTimeTaken(long startTime, long endTime)
	{
		long duration = endTime - startTime;

		int seconds = (int)((duration / 1000) % 60); 
		int minutes = (int) ((duration / 1000) / 60);
		int hours   = (int) ((duration / 1000) / 3600); 

		String timeTaken = hours + " hour(s), " + minutes + " minute(s) and " + seconds + " second(s)!";

		return timeTaken;
	}
}