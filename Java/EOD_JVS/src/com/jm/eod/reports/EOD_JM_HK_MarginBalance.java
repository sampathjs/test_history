/********************************************************************************
 * Script Name: EOD_JM_HK_MarginBalance
 * Script Type: Main
 * 
 * Load the user table USER_jm_ap_dp_balance and retrieve total balance where open date is current business date
 *  
 ********************************************************************************/

/*
 *  
 * History:
 * Version 1.0     10-Aug-2017  sma  Initial Version
 * 
 */
package com.jm.eod.reports;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;
import com.jm.eod.common.*;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_HK_MarginBalance implements IScript
{	
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "MarginBalance";
	private static final String USER_TABLE_JM_AP_DP_BALANCE = "USER_jm_ap_dp_balance";

	private static ConstRepository repository = null;
	
    public void execute(IContainerContext context) throws OException
    {
    	Table outputTbl = Util.NULL_TABLE,
      		  rptData = Util.NULL_TABLE;    	
		 
    	try 
    	{
    		repository = new ConstRepository(CONTEXT, SUBCONTEXT);
            Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
           
    		outputTbl = getOutputData(USER_TABLE_JM_AP_DP_BALANCE);
    		rptData = createReport(outputTbl);
            if (Table.isTableValid(rptData) == 1 && rptData.getNumRows() > 0) 
            {
        		Logging.info("EOD HK Margin Balance - please check EOD report.");

            }
        }
        catch(Exception e)
        {
			Logging.error(e.getLocalizedMessage());
			throw new OException(e);
        }
    	finally
    	{
    		Logging.close();
    		Utils.removeTable(outputTbl);
    		Utils.removeTable(rptData);
    	}

		Util.scriptPostStatus("EOD HK Margin Balance - please check EOD report.");
		
    }
    
    private Table getOutputData(String userTableName) throws OException {
    	Table outputData = Util.NULL_TABLE;    
    	
    	String sql = "SELECT u.open_date, p.short_name customer, u.todaysdollar_balance\n"
                    + "FROM   " + userTableName + " u,\n"
                    + "       party p, system_dates sd \n"
                    + "WHERE  u.customer_id = p.party_id\n"
                    + "AND    u.open_date = sd.business_date";
        
    	outputData = Utils.runSql(sql);

    	return outputData;
	}
    
    private Table createReport(Table output) throws OException
    {        
    	output.setColTitle("open_date", "Open Date");
    	output.setColTitle("customer", "Customer Name");
    	output.setColTitle("todaysdollar_balance", "Total Balance");

    	output.setRowHeaderWidth(1);
    	output.formatSetWidth("open_date", 22);
    	output.formatSetWidth("customer", 30);
    	output.setColFormatAsDate("open_date"); 
        output.setColFormatAsNotnl("todaysdollar_balance", Util.NOTNL_WIDTH, Util.NOTNL_PREC,
                COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
    	
    	output.setTitleBreakPosition(ROW_POSITION_ENUM.ROW_BOTH);
    	output.setTitleAboveChar("=");
    	output.setTitleBelowChar("=");
    	output.showTitleBreaks();

        String filename = "HK_Margin.eod";
        String title = "HK Margin Balance"; 
       
        
        if(output.getNumRows() > 0)
        {
            Report.reportStart(filename, title);
            output.setTableTitle(title);
            Report.printTableToReport(output,REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
        }
        else
        {
        	output.hideTitleBreaks();
        	output.hideTitles();
            Report.reportStart (filename, title);
            output.setTableTitle("No data in the report.");
            Report.printTableToReport(output, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
            output.showTitles();
        }
        
        return output;
    }   
}
