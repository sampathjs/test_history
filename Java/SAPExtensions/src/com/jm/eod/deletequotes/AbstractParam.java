package com.jm.eod.deletequotes;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/**
 * Abstract parameter script to define a query name in input args
 */
public abstract class AbstractParam implements IScript 
{
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		Table tblArgt = context.getArgumentsTable();
		
		tblArgt.addCol("query_name", COL_TYPE_ENUM.COL_STRING);
		
		int newRow = tblArgt.addRow();
		
		/* Set definition name */
		tblArgt.setString("query_name", newRow, getQueryName());
	}
	
	/* Override in sub class */
	protected abstract String getQueryName();
}
