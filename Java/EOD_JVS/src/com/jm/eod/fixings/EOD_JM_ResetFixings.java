/********************************************************************************
 * Script Name: EOD_JM_ResetFixings
 * Script Type: Main
 * 
 * Execute specified query and fix resets for those deals. 
 * Exit with failure status if found, otherwise exit with success.
 *  
 * Parameters : Region Code e.g. HK
 *              Query Name
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     11-Nov-15  D.Connolly  Initial Version
 ********************************************************************************/

package com.jm.eod.fixings;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;


import com.jm.eod.common.*;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_ResetFixings implements IScript
{	
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "ResetFixings";
	private static ConstRepository repository = null;
	
    public void execute(IContainerContext context) throws OException
    {       
    	Table resetInfo = Util.NULL_TABLE,
    	      rptData = Util.NULL_TABLE;
    	
    	try{
    		Logging.init(this.getClass(),CONTEXT, SUBCONTEXT);
    	}catch(Error ex){
    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
    	}
    	
		try 
    	{
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);
			Table params = context.getArgumentsTable();
    		int today = OCalendar.today();
    		
    		resetInfo = applyResets(Utils.getParam(params, Const.QUERY_COL_NAME), today);
    	    rptData = createReport(resetInfo, Utils.getParam(params, Const.REGION_COL_NAME).trim());
            if (Table.isTableValid(rptData) > 0 && rptData.getNumRows() > 0) 
            {
        		Logging.error("Reset fixing process encountered errors - please check EOD report.");
        		Util.scriptPostStatus(rptData.getNumRows() + " resest fixing error(s).");
            	Util.exitFail(rptData.copyTable());
            }
        }
        catch(Exception e)
        {
			Logging.error(e.getLocalizedMessage());
			throw new OException(e);
        }
    	finally
    	{
    		Utils.removeTable(resetInfo);
    		Utils.removeTable(rptData);
    		Logging.close();
    	}

		Util.scriptPostStatus("No fixing errors.");
		
    }
    
    /**
     * Fixed resets for deals identified by query
     * @param qryName: regional query name
     * @param resetDt: reset date
     */	
    private Table applyResets(String qryName, int resetDt) throws OException
    {
    	Table resetInfo = Util.NULL_TABLE;
    	int qid = 0;
    	
    	try 
    	{
    		Logging.debug("Retrieve deals for fixing query " + qryName);
    		qid = Query.run(qryName);
    		if (qid < 1)    	
    		{
    			String msg = "Run Query failed: " + qryName;
    			throw new OException(msg);
    		}
    		
    		String sql = "SELECT ab.tran_num\n"
                   + "FROM   " + Query.getResultTableForId(qid) + " qr,\n"
                   + "       ab_tran ab\n"
                   + "WHERE  qr.unique_id = " + qid + "\n"
                   + "AND    ab.tran_num = qr.query_result\n";

    		Table dealsToFix = Utils.runSql(sql);
    		
			if (Table.isTableValid(dealsToFix) < 1)
			{
				throw new OException("Error loading trades to fix.");
    		}   		
    		
    		Logging.debug("Run ResetDealsByQid for reset date " + OCalendar.formatJd(resetDt, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH));
    		resetInfo = EndOfDay.resetDealsByTranList(dealsToFix, resetDt);
			if (Table.isTableValid(resetInfo) < 1)
			{
				throw new OException("EndOfDay.resetDealsByTranList returned invalid table.");
    		}
    	}
    	finally
    	{
    		if (qid > 0) {
    			Query.clear(qid);
    		}
    	}
    	
		return resetInfo;
    }
    
    /**
     * Prints status report
     * @param resetInfo table: reset info from fixing process
     * @param region code: regional identifier
     */
    private Table createReport(Table resetInfo, String regionCode) throws OException
    {  
		Table errors = Table.tableNew();
		String cols = "tran_num, deal_num, param_seq_num, profile_seq_num, reset_seq_num"
				    + ", spot_value, value, message";
		errors.select(resetInfo, cols, "success EQ 0");

		formatOutput(errors);
		errors.group("tran_num, param_seq_num, profile_seq_num, reset_seq_num");

		errors.setTitleBreakPosition(ROW_POSITION_ENUM.ROW_BOTH);
		errors.setTitleAboveChar("=");
		errors.setTitleBelowChar("=");
		errors.showTitleBreaks();
		
        String filename = "Reset_Fixings.eod";
        String title = "Reset Fixings Report for " 
                     + OCalendar.formatJd(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
        if (regionCode.length() > 0) 
        {
        	filename = regionCode + "_" + filename;
        	title = regionCode + " " + title;
        }
        
        if(errors.getNumRows() > 0)
        {
            Report.reportStart(filename, title);
            errors.setTableTitle(title);
            Report.printTableToReport(errors, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
            
    		Logging.debug("Reset fixing errors found: "  + errors.getNumRows());
        }
        else
        {
        	errors.hideTitleBreaks();
        	errors.hideTitles();
            Report.reportStart (filename, title);
            errors.setTableTitle("No reset fixing errors.");
            Report.printTableToReport(errors, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
            errors.showTitles();
        }
		
        return errors;
    }
    
	private void formatOutput(Table report) throws OException
	{	
		report.setColTitle( "tran_num", "Tran\nNum");
		report.setColTitle( "deal_num", "Deal\nNum");
		report.setColTitle( "param_seq_num", "Param\nSeq Num");
		report.setColTitle( "profile_seq_num", "Profile\nSeq Num");
		report.setColTitle( "reset_seq_num", "Reset\nSeq Num");
		report.setColTitle( "spot_value", "Spot\nValue");
		report.setColTitle( "value", "\nValue");
		report.setColTitle( "message", "\nError Message");
		
		report.formatSetJustifyRight( "tran_num");
		report.formatSetJustifyRight( "deal_num");
		report.formatSetJustifyCenter( "param_seq_num");
		report.formatSetJustifyCenter( "profile_seq_num");
		report.formatSetJustifyCenter( "reset_seq_num");
		report.formatSetJustifyRight( "spot_value");
		report.formatSetJustifyRight( "value");
		report.formatSetJustifyLeft( "message");
		
		report.formatSetWidth( "tran_num", 9);
		report.formatSetWidth( "deal_num", 9);
		report.formatSetWidth( "param_seq_num", 9);
		report.formatSetWidth( "profile_seq_num", 9);
		report.formatSetWidth( "reset_seq_num", 9);
		report.formatSetWidth( "spot_value", 15); 
		report.formatSetWidth( "value", 15);
		report.formatSetWidth( "message", 80);
		
		report.setRowHeaderWidth(1);
	}
}
