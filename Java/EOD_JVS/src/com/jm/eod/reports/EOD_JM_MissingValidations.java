/********************************************************************************
 * Script Name: EOD_JM_MissingValidations
 * Script Type: Main
 * Parameter:   Query Name
 * 
 * Execute specified query and report any trades with NEW transaction status's
 * i.e. Cancelled New, Buyout New ...
 * Exit with failure status if any found, otherwise exit with success.
 * 
 * Parameters : Region Code e.g. HK
 *              Query Name
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     04-Nov-15  D.Connolly  Initial Version
 ********************************************************************************/

package com.jm.eod.reports;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;

import standard.include.JVS_INC_Standard;

import com.jm.eod.common.*;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_MissingValidations implements IScript
{	
	private JVS_INC_Standard stdLib;
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "MissingValidationsReport";
	private static ConstRepository repository = null;
	
	public EOD_JM_MissingValidations() {
		stdLib = new JVS_INC_Standard();
	}
	
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
            deals  = getDeals(qryName);
            rptData = createReport(deals, Utils.getParam(params, Const.REGION_COL_NAME).trim());
            if (Table.isTableValid(rptData) == 1 && rptData.getNumRows() > 0) 
            {
        		Logging.error("Some deals have not been validated - please check EOD report.");
        		Util.scriptPostStatus(rptData.getNumRows() + " missing validation(s).");
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
    		Logging.close();
    		Utils.removeTable(deals);
    		Utils.removeTable(rptData);
    	}

    	Util.scriptPostStatus("No missing validations.");
		
    }
    
    /**
     * Report new/amended deals that have not been validated
     * @param qryName: name of query to execute
     */
    private Table getDeals(String qryName) throws OException
    {   
    	Table data = Util.NULL_TABLE;
    	int qid = 0;

    	try 
    	{
    		qid = Query.run(qryName);
    		if (qid < 1)    	
    		{
    			String msg = "Run Query failed: " + qryName;
    			throw new OException(msg);
    		}
	
    		String sql = "SELECT ab.deal_tracking_num deal_num, ab.tran_num, ab.ins_num, ab.tran_status, ab.ins_type,\n"
        	           + "       ab.trade_date, ab.external_lentity, ab.internal_portfolio, ab.reference, ip.unit, h.ticker, h.cusip,\n"
        	           + "       ab.currency, ab.position, ab.internal_contact, p.party_id internal_bunit, ip.start_date, ip.mat_date\n"
                       + "FROM   " + Query.getResultTableForId(qid) + " qr,\n"
                       + "       ab_tran ab,\n"
                       + "       party p,\n"
                       + "       ins_parameter ip,\n"
                       + "       header h\n"
                       + "WHERE  qr.unique_id = " + qid + "\n"
                       + "AND    ab.tran_num = qr.query_result\n"
   					   + "AND    p.party_id = ab.internal_bunit\n"	// internal BU
                       + "AND    p.party_class = 1\n"               //
                       + "AND    p.int_ext = 0\n"                   //
                       + "AND    h.ins_num = ab.ins_num\n"
                       + "AND    ip.ins_num = h.ins_num\n"
                       + "AND    ip.param_seq_num = 0";
        
    		data = Utils.runSql(sql);
    		Logging.debug("Found " + data.getNumRows() + " deal(s) that are not validated.");
    	}
    	finally
    	{
    		if (qid > 0) {
    			Query.clear(qid);
    		}
    	}
    	
    	return data;
    }
    
    /**
     * Generate report showing missing validations
     * @param 
     */
    private Table createReport(Table deals, String regionCode) throws OException
    {
		Table output = createContainer();
		output.select(deals, "*", "tran_num GT 0");

		formatOutput(output, stdLib.STD_GetReportType());
		groupOutput(output);
		
		output.groupTitleAbove( "internal_bunit");
		output.setTitleBreakPosition( ROW_POSITION_ENUM.ROW_BOTH);
		output.setTitleAboveChar( "=");
		output.setTitleBelowChar( "=");
		output.showTitleBreaks();
		output.colHide( "internal_bunit");
		
		String filename = "Missed_Validations.eod"; 
		String title = "Missed Validations Report for "
				     + OCalendar.formatJd(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
        if (regionCode.length() > 0) 
        {
        	filename = regionCode + "_" + filename;
        	title = regionCode + " " + title;
        }
        
        if(output.getNumRows() > 0)
        {
            Report.reportStart(filename, title);
        	output.setTableTitle(title);
            Report.printTableToReport(output, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd ();
    		output.colShow("internal_bunit");
    		
    		Logging.info("Missed validation errors found: "  + output.getNumRows());
        }
        else
        {
        	output.hideTitleBreaks ();
            output.hideTitles ();
            Report.reportStart (filename, title);
            output.setTableTitle("No Missing Validations");
            Report.printTableToReport (output, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd ();
        }	
		
		return output;
    }
    
    /**
     * Create report container
     */
    private Table createContainer() throws OException
    {
		Table container = Table.tableNew();
		container.addCol( "internal_bunit", COL_TYPE_ENUM.COL_INT );
		container.addCol( "deal_num", COL_TYPE_ENUM.COL_INT );
		container.addCol( "tran_num", COL_TYPE_ENUM.COL_INT );
		container.addCol( "ins_num", COL_TYPE_ENUM.COL_INT );
		container.addCol( "tran_status", COL_TYPE_ENUM.COL_INT );
		container.addCol( "ins_type", COL_TYPE_ENUM.COL_INT );
		container.addCol( "trade_date", COL_TYPE_ENUM.COL_DATE_TIME );
		container.addCol( "external_lentity", COL_TYPE_ENUM.COL_INT );
		container.addCol( "internal_portfolio", COL_TYPE_ENUM.COL_INT );
		container.addCol( "reference", COL_TYPE_ENUM.COL_STRING );
		container.addCol( "unit", COL_TYPE_ENUM.COL_INT );
		container.addCol( "ticker", COL_TYPE_ENUM.COL_STRING );
		container.addCol( "cusip",  COL_TYPE_ENUM.COL_STRING );
		container.addCol( "currency", COL_TYPE_ENUM.COL_INT );
		container.addCol( "position", COL_TYPE_ENUM.COL_DOUBLE );
		container.addCol( "internal_contact", COL_TYPE_ENUM.COL_INT );
		container.addCol( "start_date", COL_TYPE_ENUM.COL_DATE_TIME );
		container.addCol( "mat_date", COL_TYPE_ENUM.COL_DATE_TIME );
		return container;
    }
    
    /**
     * Format report data
     * @param report table
     * @param report type (which columns to show)
     */
	private void formatOutput(Table report, int report_type) throws OException
	{
		/* Load Party and trader Names */
		report.setColFormatAsRef( "external_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		report.setColFormatAsRef( "internal_bunit",   SHM_USR_TABLES_ENUM.PARTY_TABLE);
		report.setColFormatAsRef( "internal_contact", SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);

		report.colHide( "ins_num");

		/* Column Formatting */
		report.setColFormatAsRef( "ins_type",    		SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		report.setColFormatAsRef( "tran_status", 		SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		report.setColFormatAsRef( "currency",    		SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		report.setColFormatAsRef( "unit",    			SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		report.setColFormatAsRef( "internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		report.setColFormatAsNotnlAcct( "position", Util.NOTNL_WIDTH, Util.NOTNL_PREC,COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		report.setColFormatAsDate( "trade_date");  
		report.setColFormatAsDate( "start_date");  
		report.setColFormatAsDate( "mat_date");  

		report.setRowHeaderWidth( 25); //squeeze left
		
		report.formatSetWidth( "deal_num",     			9);
		report.formatSetWidth( "tran_num",              9); 
		report.formatSetWidth( "tran_status",          15);
		report.formatSetWidth( "ins_type",             20);
		report.formatSetWidth( "ticker",               20);
		report.formatSetWidth( "cusip",                20);
		report.formatSetWidth( "isin",                 20);
		report.formatSetWidth( "position",             14);
		report.formatSetWidth( "currency",              5);
		report.formatSetWidth( "unit",                 12);
		report.formatSetWidth( "external_lentity",     30);
		report.formatSetWidth( "internal_bunit",       27);
		report.formatSetWidth( "internal_portfolio",   25);
		report.formatSetWidth( "reference",            20);
		report.formatSetWidth( "internal_contact",     13);
		report.formatSetWidth( "tran_status",          20);
		report.formatSetWidth( "trade_date",           12);
		report.formatSetWidth( "start_date",           12);
		report.formatSetWidth( "mat_date",             12);

		/* Format the table and set column titles */
		report.setColTitle( "deal_num",    			"Deal\nNum");
		report.setColTitle( "tran_num",    			"Tran\nNum");
		report.setColTitle( "tran_status", 			"Tran\nStatus");
		report.setColTitle( "ins_type",    			"Instrument\nType");
		report.setColTitle( "ticker",      			" \nTicker");
		report.setColTitle( "cusip",       			" \nCusip");
		report.setColTitle( "unit",        			" \nUnit");
		report.setColTitle( "position",    			" \nPosition");
		report.setColTitle( "currency",    			" \nCcy");
		report.setColTitle( "external_lentity",   	" \nCounterparty");
		report.setColTitle( "internal_portfolio", 	" \nPortfolio");
		report.setColTitle( "reference",          	" \nReference");
		report.setColTitle( "internal_contact",   	" \nTrader");
		report.setColTitle( "trade_date",         	"Trade\nDate");
		report.setColTitle( "start_date",         	"Start\nDate");
		report.setColTitle( "mat_date",           	"End\nDate");
		report.setColTitle( "internal_bunit", 		"Business\nUnit");

		if(report_type == 1) {
			report.colHide( "ticker");
			report.colHide( "cusip");
		}
		else {
			report.colHide( "unit");
			report.colHide( "position");
		}
	}

    /**
     * Sort data by Internal BU, deal no, tran no
     * @param report table
     */
	private void groupOutput(Table report) throws OException
	{
		report.clearGroupBy();
		report.addGroupBy( "internal_bunit");
		report.addGroupBy( "deal_num");
		report.addGroupBy( "tran_num");
		report.groupByFormatted();
	}
}