package com.jm.tableau;

import com.olf.openjvs.*;

public class TradeCheckPopUp implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
    	  ReportBuilder rb = ReportBuilder.createNew("Management Trade Check");
          
    	  Table reportOutput = Table.tableNew();                              
          rb.setOutputTable(reportOutput);            
          rb.runReport();
          
          reportOutput.setTableName("Management Trade Check");
          reportOutput.viewTable();
          
          reportOutput.destroy();
    }
}
