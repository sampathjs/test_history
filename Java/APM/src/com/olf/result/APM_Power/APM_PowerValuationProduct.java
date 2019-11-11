package com.olf.result.APM_Power;

import com.olf.openjvs.*;

public class APM_PowerValuationProduct implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
        Table tblReturnt = context.getReturnTable();
        Table tblPicklistValues = tblReturnt.getTable("table_value", 1);
        
        int row = tblPicklistValues.insertRowBefore(1);
        tblPicklistValues.setString(2, row, "None");
        
        //tblPicklistValues.viewTable();
    } 
} 
