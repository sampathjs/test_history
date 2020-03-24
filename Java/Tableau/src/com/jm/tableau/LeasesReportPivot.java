package com.jm.tableau;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;

public class LeasesReportPivot implements IScript
{
	String leasesReport = "PMM Leases";

    public void execute(IContainerContext context) throws OException
    {
        // Run report builder
        ReportBuilder rb = ReportBuilder.createNew(leasesReport);
        
        try{rb.setOutputProperty("Output_01", "Deliver report via email", "0");
        }catch(OException e){}
        
        Table reportOutput = Table.tableNew();                              
        rb.setOutputTable(reportOutput);            
        rb.runReport();
        
        reportOutput.colConvertDateTimeToInt("maturity_date");
        
        // Convert each maturity date to EOM (so we get consistent dates for the pivot)
        for(int i = 1 ; i <= reportOutput.getNumRows(); i++)
        {
        	reportOutput.setInt("maturity_date", i , OCalendar.parseString("1fom", -1, reportOutput.getInt("maturity_date", i))-1);
        }
        
        // make sure all following 12 months + currencies are present in the table, there again for the pivot
        Table allMtlCcys = Table.tableNew();
        DBase.runSqlFillTable("select name from currency where LEFT(name,1)='X'", allMtlCcys);
        for(int i = 1 ; i <= 12; i++)
        {
        	reportOutput.setInt("maturity_date", reportOutput.addRow(), OCalendar.parseString(i+"fom")-1);
        	reportOutput.setString("currency", reportOutput.getNumRows(), (i<=allMtlCcys.getNumRows()?allMtlCcys.getString(1,i):"XAU"));
        }       

        reportOutput.colConvertIntToDateTime("maturity_date");
        reportOutput.setColName("maturity_date", "report_month");
        
        // Pivot  pivotTable.viewTable()
        Table pivotTable = reportOutput.pivot("currency", "report_month", "position", "");
        
        
        // Reverse pivot
        Table reversePivot = pivotTable.copyTable();
        for(int j = 2; j <= reversePivot.getNumCols(); j++)
        {
        	double runningTotal = pivotTable.sumRangeDouble(j, 1, pivotTable.getNumRows());
        	
        	reversePivot.setDouble(j, 1, runningTotal);
        	
            for(int i = 2; i <= reversePivot.getNumRows(); i++)
            {
            	runningTotal -= pivotTable.getDouble(j, i - 1);
            	reversePivot.setDouble(j, i, runningTotal);
            }
            
            reversePivot.mathMultColConst(j, -1.0, j);
        }
        reversePivot.addCol("select_all", COL_TYPE_ENUM.COL_INT);
        
        context.getReturnTable().select(reversePivot, "*", "select_all EQ 0");
        context.getReturnTable().delCol("select_all");
        
        reversePivot.destroy();
        pivotTable.destroy();
        reportOutput.destroy();   
    }
    
}
