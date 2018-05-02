package com.olf.recon.automatch.output;

import java.util.ArrayList;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/**
 * Abstract parameter class to set argt AutoMatch variables
 */
public abstract class AbstractParam implements IScript 
{
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		Table tblArgt = context.getArgumentsTable();
		
		tblArgt.addCol("automatch_definition_name", COL_TYPE_ENUM.COL_STRING);
		tblArgt.addCol("automatch_actions", COL_TYPE_ENUM.COL_TABLE);
		
		int newRow = tblArgt.addRow();
		
		/* Set definition name */
		tblArgt.setString("automatch_definition_name", newRow, getAutoMatchDefinitionName());
		
		/* Set actions */
		Table tblActions = Table.tableNew("Automatch actions");
		tblActions.addCol("action_name", COL_TYPE_ENUM.COL_STRING);
		ArrayList<String> actions = getAutoMatchActions();
		for (String action : actions)
		{
			int row = tblActions.addRow();
			tblActions.setString("action_name", row, action);
		}
		
		tblArgt.setTable("automatch_actions", newRow, tblActions);
	}
	
	/* Override in sub class */
	protected abstract String getAutoMatchDefinitionName();
	
	/* Override in sub class */
	protected abstract ArrayList<String> getAutoMatchActions();
}
