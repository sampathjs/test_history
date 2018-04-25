package com.olf.recon.automatch.output;

import java.util.ArrayList;

import com.olf.openjvs.AutoMatch;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.utils.Util;
import com.openlink.util.logging.PluginLog;

/**
 * Executes an AutoMatch definition automatically with a specified 
 * collection of actions.
 */
public class RunAutoMatchDefinition implements IScript 
{	
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		Util.initialiseLog();
		
		Table tblArgt = context.getArgumentsTable();
		if (tblArgt.getNumRows() == 0)
		{
			throw new ReconciliationRuntimeException("Incorrect args supplied!");
		}
		
		String autoMatchDefinitionName = getAutoMatchDefinitionName(tblArgt);
		ArrayList<String> actions = getAutoMatchActions(tblArgt);
		
		PluginLog.info("Attempting to run Auto Match definition: " + autoMatchDefinitionName);

		AutoMatch automatchDefinition = null;
		
		try
		{
			/* Create a new instance of the specified Auto Match definition name */
			automatchDefinition = AutoMatch.createNew(autoMatchDefinitionName);
	
			/* Run the definition */
			automatchDefinition.run();
		
			/* Run the action(s) */
			for (String actionName : actions)
			{
				PluginLog.info("Executing Auto Match action: " + actionName);
				automatchDefinition.runAction(actionName);	
			}
		}
		catch (Exception e)
		{
			throw new ReconciliationRuntimeException("Unable to run automatch definition: " + autoMatchDefinitionName + ", " + e.getMessage(), e);
		}
		finally
		{
			if (automatchDefinition != null)
			{
				automatchDefinition.dispose();
			}
		}
		
		PluginLog.info("Auto Match definition run complete for: " + autoMatchDefinitionName);
	}
	
	/**
	 * Return the automatch definition name from the param script
	 * 
	 * @param tblArgt
	 * @return
	 * @throws OException
	 */
	private String getAutoMatchDefinitionName(Table tblArgt) throws OException
	{
		if (tblArgt.getColNum("automatch_definition_name") <= 0)
		{
			throw new ReconciliationRuntimeException("Could not find parameter: automatch_definition_name");
		}
		
		return tblArgt.getString("automatch_definition_name", 1);	
	}
	
	/**
	 * Return a list of actions specified in the param script 
	 * 
	 * @param tblArgt
	 * @return
	 * @throws OException
	 */
	private ArrayList<String> getAutoMatchActions(Table tblArgt) throws OException
	{
		if (tblArgt.getColNum("automatch_actions") <= 0)
		{
			throw new ReconciliationRuntimeException("Could not find parameter: automatch_actions");
		}
		
		ArrayList<String> actions = new  ArrayList<String>();
		
		Table tblActions = tblArgt.getTable("automatch_actions", 1);
		int numRows = tblActions.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			String actionName = tblActions.getString("action_name", row);
			
			actions.add(actionName);
		}
		
		return actions;	
	}
}
