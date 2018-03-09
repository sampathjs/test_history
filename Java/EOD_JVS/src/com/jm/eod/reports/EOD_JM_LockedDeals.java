/********************************************************************************
 * Script Name: EOD_JM_LockedDeals
 * Script Type: Main
 * Parameter:   Query Name
 * 
 * Execute specified query and report any trades which are locked
 * Exit with failure status if found, otherwise exit with success.
 *  
 * Parameters : Region Code e.g. HK
 *              Query Name
 *
 * Revision History:
 * Version Date       Author      Description
 * 1.0     03-Nov-15  D.Connolly  Initial Version
 ********************************************************************************/

package com.jm.eod.reports;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;
import com.jm.eod.common.*;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class EOD_JM_LockedDeals implements IScript
{	
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "LockedDealsReport";
	private static ConstRepository repository = null;
	
    public void execute(IContainerContext context) throws OException
    {
    	Table deals = Util.NULL_TABLE,
      		  rptData = Util.NULL_TABLE;
    	
		repository = new ConstRepository(CONTEXT, SUBCONTEXT);
        Utils.initPluginLog(repository, this.getClass().getName()); 
        
    	try 
    	{
    		Table params = context.getArgumentsTable();
    		String qryName = Utils.getParam(params, Const.QUERY_COL_NAME);
            deals = getLockedDeals(qryName);
    		rptData = createReport(deals, Utils.getParam(params, Const.REGION_COL_NAME).trim());
            if (Table.isTableValid(rptData) == 1 && rptData.getNumRows() > 0) 
            {
        		PluginLog.error("Found locked deals - please check EOD report.");
        		Util.scriptPostStatus(rptData.getNumRows() + " deal(s) locked.");
            	Util.exitFail(rptData.copyTable());
            }
        }
        catch(Exception e)
        {
			PluginLog.fatal(e.getLocalizedMessage());
			throw new OException(e);
        }
    	finally
    	{
    		Utils.removeTable(deals);
    		Utils.removeTable(rptData);
    	}

		Util.scriptPostStatus("No locked deals.");
		PluginLog.exitWithStatus();
    }
    
    /**
     * Report any locked deals.
     * @param qryName: name of query to execute
     */
    private Table getLockedDeals(String qryName) throws OException
    {   
    	Table lockedDeals = Util.NULL_TABLE;
    	int qid = 0;
    	
    	try 
    	{
    		qid = Query.run(qryName);
    		if (qid < 1)    	
    		{
    			String msg = "Run Query failed: " + qryName;
    			throw new OException(msg);
    		}
        
    		String sql = "SELECT ab.deal_tracking_num,\n"
     	           + "       ab.trade_date\n"
                    + "FROM   " + Query.getResultTableForId(qid) + " qr,\n"
                    + "       ab_tran ab,\n"
                    + "       deal_lock_table dl\n"
                    + "WHERE  qr.unique_id = " + qid + "\n"
                    + "AND    ab.tran_num = qr.query_result\n"
                    + "AND    dl.tran_num = ab.tran_num";
        
    		lockedDeals = Utils.runSql(sql);
    	}
    	finally
    	{
    		if (qid > 0) {
    			Query.clear(qid);
    		}
    	}
    	
    	return lockedDeals;
    }
    
    /**
     * Prints a report of locked deals.
     * @param lockedDeals table: deal number, trade date
     */
    private Table createReport(Table lockedDeals, String regionCode) throws OException
    {        
        lockedDeals.setColTitle("deal_tracking_num", "Deal\nNum");
        lockedDeals.setColTitle("trade_date", "Trade\nDate");
        lockedDeals.setRowHeaderWidth(1);
        lockedDeals.formatSetWidth("deal_num", 9);
        lockedDeals.formatSetWidth("trade_date", 22);
        lockedDeals.setColFormatAsDate("trade_date"); 
        
        lockedDeals.setTitleBreakPosition(ROW_POSITION_ENUM.ROW_BOTH);
        lockedDeals.setTitleAboveChar("=");
        lockedDeals.setTitleBelowChar("=");
        lockedDeals.showTitleBreaks();

        String filename = "Locked_Deals.eod";
        String title = "Locked Deals Report for " 
                     + OCalendar.formatJd(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
        if (regionCode.length() > 0) 
        {
        	filename = regionCode + "_" + filename;
        	title = regionCode + " " + title;
        }
        
        if(lockedDeals.getNumRows() > 0)
        {
            Report.reportStart(filename, title);
            lockedDeals.setTableTitle(title);
            Report.printTableToReport(lockedDeals,REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
        }
        else
        {
            lockedDeals.hideTitleBreaks();
            lockedDeals.hideTitles();
            Report.reportStart (filename, title);
            lockedDeals.setTableTitle("No Locked Deals");
            Report.printTableToReport(lockedDeals, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
            lockedDeals.showTitles();
        }
        
        return lockedDeals;
    }   
}
