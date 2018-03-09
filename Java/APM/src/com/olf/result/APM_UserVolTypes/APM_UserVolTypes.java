package com.olf.result.APM_UserVolTypes;

import com.olf.openjvs.*;

public class APM_UserVolTypes implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
    	int row;
    	String str = "";
        Table tblReturnt = context.getReturnTable();
        Table tblPicklistValues = tblReturnt.getTable("table_value", 1);
        int numRows = tblPicklistValues.getNumRows();
        for(row = numRows; row >= 1 ; row--)
        {
        	str = tblPicklistValues.getString("label", row);
        	if(Str.isEmpty(Str.stripBlanks(str)) == 1)
        	{
        		tblPicklistValues.delRow(row);
        	}
        }
        //tblPicklistValues.viewTable();
    } 
} 
