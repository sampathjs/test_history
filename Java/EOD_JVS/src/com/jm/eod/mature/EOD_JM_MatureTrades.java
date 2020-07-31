/********************************************************************************
 * Script Name: EOD_JM_MatureTrades
 * Script Type: Main
 * 
 * This script will change the tran_status to TRAN_STATUS_MATURE for master
 * instruments/trades that have a close event_date <= today - "Days Before Today" 
 * re: const repository.
 * 
 * Exit with failure status if found, otherwise exit with success.
 *  
 * Parameters : Business Units
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     12-Nov-15  D.Connolly  Initial Version
 ********************************************************************************/

package com.jm.eod.mature;

import com.olf.openjvs.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.enums.*;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;
import com.jm.eod.common.*;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_MatureTrades implements IScript
{	
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "MatureTrades";
	private static final String CR_DAYS_BEFORE_TODAY = "DaysBeforeToday";
	private static ConstRepository repository = null;
	
    public void execute(IContainerContext context) throws OException
    {       
    	Table intBUnits = Util.NULL_TABLE,
    	      matDeals = Util.NULL_TABLE;
    	
    	try{
    		Logging.init(this.getClass(),CONTEXT, SUBCONTEXT);
    	}catch(Error ex){
    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
    	}
    	
		try 
    	{
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);	        
	        
    		Table params = context.getArgumentsTable();
    		
    		// get internal business units
    		if(params.getNumRows() <= 0 || params.getColNum("bunit") < 0){
    			throw new OException( "Missing parameter: Internal Business Unit (column name = bunit)");
    		}
    		intBUnits = Table.tableNew();
    		intBUnits.select(params, "bunit", "bunit GT 0");
    		
    		// calculate maturity date
    		int matDate = 0;
    		try {
    			int daysBefore = Math.abs(repository.getIntValue(CR_DAYS_BEFORE_TODAY, 0));
    			matDate = OCalendar.today() - daysBefore;
    		}
    		catch (Exception e){
    			throw new OException("Missing or invalid value: constants repository variable = " + CR_DAYS_BEFORE_TODAY);
    		}
    		
    		// mature master instruments / BU trades
    		matDeals = matureTrades(intBUnits, matDate);
    		
    		// report on deals validated -> matured
    	    createReport(matDeals);
        }
        catch(Exception e)
        {
			Logging.error(e.getLocalizedMessage());
			throw new OException(e);
        }
    	finally
    	{
    		Utils.removeTable(intBUnits);
    		Utils.removeTable(matDeals);
    		Logging.close();
    	}

		
    }
    
    /**
     * Mature master instruments & trades for specified internal business units
     * @param intBUnits: list of internal business units
     * @param matDate: mature deals up to and including this date
     */	
    private Table matureTrades(Table intBUnits, int matDate) throws OException
    {
    	Table valDeals = Util.NULL_TABLE,
    		  matDeals = Util.NULL_TABLE;
    	int retStatus = 0,
    		qid = 0;
    	
    	Logging.info("Maturing master instruments/trades with closing event date <= " + OCalendar.formatJd(matDate, DATE_FORMAT.DATE_FORMAT_DLMLY_DASH));
    	
    	try
    	{
	    	// get validated deals
	    	String sql = "Select distinct ab.deal_tracking_num deal_num\n"
	 			       + "FROM   ab_tran ab\n"
	 			       + "WHERE  tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	 	    valDeals = Utils.runSql(sql);
	 	    qid = Query.tableQueryInsert(valDeals, 1);
	    	
	    	// mature master instruments
			retStatus = EndOfDay.matureMasterIns(matDate);
			if (retStatus != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
			{
				String msg = DBUserTable.dbRetrieveErrorInfo(retStatus, "Failed to mature master instruments");
				throw new OException(msg);
			}
	    	Logging.info("Matured master instruments");
	
			// actual trades for each specified internal BU
	    	int numRows = intBUnits.getNumRows();
	    	for (int i = 1; i <= numRows; i++)
	    	{
	    		int intBU = intBUnits.getInt("bunit", i);
	    		String intBUName = Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, intBU);
	    		retStatus = EndOfDay.matureTrades(intBU, matDate);
	    		if (retStatus != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
	    		{
	    			String msg = DBUserTable.dbRetrieveErrorInfo(retStatus, "Failed to mature trades for business unit " + intBUName);
	    			throw new OException(msg);
	    		}
	    		Logging.info("Matured trades for business unit " + intBUName);
	    	}
	    	
	    	// get matured deals;
	    	sql = "Select ab.internal_bunit bunit,\n"
	    		+ "       ab.deal_tracking_num deal_num,\n"
	    		+ "       ab.tran_num\n"
	    	    + "FROM   query_result qr,\n"
	     	    + "       ab_tran ab\n"
	    	    + "WHERE  qr.unique_id = " + qid + "\n"
	    	    + "AND    ab.deal_tracking_num = qr.query_result\n"
	    	    + "AND    ab.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	        matDeals = Utils.runSql(sql);
    	}
    	finally
    	{
    		if (qid > 0)
    		{
    			Query.clear(qid);
    		}
    		Utils.removeTable(valDeals);
    	}
    	
    	return matDeals;
    }
    
    /**
     * Prints status report
     * @param data: list of matured deals
     */
    private void createReport(Table data) throws OException
    {  
    	Table output = Table.tableNew();
        output.select(data, "bunit, deal_num, tran_num", "deal_num GT 0");
		
		output.setColTitle( "bunit", "Business\nUnit");
		output.setColTitle( "deal_num", "Deal\nNum");
		output.setColTitle( "tran_num", "Tran\nNum");
		output.formatSetWidth( "bunit", 30);
		output.formatSetWidth( "deal_num", 8);
		output.formatSetWidth( "tran_num", 8);
		output.setColFormatAsRef( "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		output.setRowHeaderWidth(1);
		
		output.groupFormatted( "bunit, deal_num");
		output.groupTitleAbove("bunit");
		output.setTitleBreakPosition( ROW_POSITION_ENUM.ROW_BOTH);
		output.setTitleAboveChar("=");
		output.setTitleBelowChar("=");
		output.showTitleBreaks();
		output.colHide("bunit");
		
        String filename = "Mature_Trades.eod";
        String title = "Mature Trades Report for " 
                     + OCalendar.formatJd(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
        
        if(output.getNumRows() > 0)
        {
            Report.reportStart(filename, title);
            output.setTableTitle(title);
            Report.printTableToReport(output, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
    		output.colShow("bunit");
    		
    		Logging.info(output.getNumRows() + " master instruments/trades have been matured");
        }
        else
        {
        	output.hideTitleBreaks();
        	output.hideTitles();
            Report.reportStart (filename, title);
            output.setTableTitle("No master instruments/trades matured");
            Report.printTableToReport(output, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd();
            output.showTitles();
        }
		
        Utils.removeTable(output);
    }
}
