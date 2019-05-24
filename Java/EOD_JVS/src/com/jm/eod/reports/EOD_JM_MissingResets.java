/********************************************************************************
 * Script Name: EOD_JM_MissingResets
 * Script Type: Main
 * 
 * Execute specified query and report any deals with missing resets. 
 * Exit with failure status if found, otherwise exit with success.
 *  
 * Parameters : Region Code e.g. HK
 *              Query Name
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     06-Nov-15  D.Connolly  Initial Version
 * 1.1     12-Oct-15  J.Waechter  Added retrieval of fixings for those the 
 *                                historical prices have disappeared
 * 1.2		10-May-19 Jyotsna	SR 232369 - added steps to save report output to CSV format for global missing prior reset report                                
 ********************************************************************************/

package com.jm.eod.reports;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;

import standard.include.JVS_INC_Standard;

import com.jm.eod.common.*;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class EOD_JM_MissingResets implements IScript
{	
	private JVS_INC_Standard stdLib;
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "MissingResetsReport";
	private static ConstRepository repository = null;
	
	public EOD_JM_MissingResets() {
		stdLib = new JVS_INC_Standard();
	}
	
    public void execute(IContainerContext context) throws OException
    {       
    	Table output = Util.NULL_TABLE,
    		  rptData = Util.NULL_TABLE;
    	int qid = 0;
    	
		repository = new ConstRepository(CONTEXT, SUBCONTEXT);
        Utils.initPluginLog(repository, this.getClass().getName()); 
        String symbolicDate = repository.getStringValue("earliestDate", "-30d");
        int jdEarliestDate = -1;
        if (symbolicDate != null && symbolicDate.trim().length() > 0) {
        	try {
            	jdEarliestDate = OCalendar.parseString(symbolicDate);        		
        	} catch (OException ex) {
        		String message = "Could not parse symbolic date '" + symbolicDate + "' as "
        				+ " specified in Constants Repository " + CONTEXT + "\\" + SUBCONTEXT
        				+ "\\earliestDate";
        		PluginLog.error(message);
        		throw new OException (message);
        	}
        }
    	try 
    	{
    		Table params = context.getArgumentsTable();
    		String dbDate = Utils.getParam(params, Const.QUERY_DATE); // get date from params
    		//String dbDate = OCalendar.formatJdForDbAccess(OCalendar.today()-1);
    		
    		qid = getQry(Utils.getParam(params, Const.QUERY_COL_NAME));
    		
            output = getDeals(qid, dbDate, jdEarliestDate);
            getNonCommodityCflowResets(output, qid, dbDate);
            getCommodityCflowResets(output, qid, dbDate);
            getNotionalResets(output, qid, dbDate);
            getNotionalCcyResets(output, qid, dbDate);

            rptData = createReport(output, Utils.getParam(params, Const.REGION_COL_NAME).trim(),Utils.getParam(params, Const.QUERY_REPORT).trim());
            //1.2 to save Global EOD report in CSV format 
            String RegionCode=params.getString((Const.REGION_COL_NAME).trim(), 1);
            
            if(RegionCode.equalsIgnoreCase("GLOBAL"))
            {
            	String strFilename = getFileName((Const.REGION_COL_NAME).trim());
                rptData.printTableDumpToFile(strFilename); 
            }
            
            
            if (Table.isTableValid(rptData) == 1 && rptData.getNumRows() > 0) 
            {
        		PluginLog.error("Found deals with missed resets - please check EOD report.");
        		Util.scriptPostStatus(output.getNumRows() + " missing reset(s)."); //1.2 changed table to output from rptdata to correct the num of missing resets
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
    		if (qid > 0) {
    			Query.clear(qid);
    		}
    		Utils.removeTable(output);
    		Utils.removeTable(rptData);
    	}

    	Util.scriptPostStatus("No missing resets.");
		PluginLog.exitWithStatus();
    }
    
    /*
     * Execute regional query 
     * @param qryName: name of query to execute
     * @return qid - main driving query id.
     */
    private int getQry(String qryName) throws OException
    {
		int qid = Query.run(qryName);
		if (qid < 1)    	
		{
			String msg = "Run Query failed: " + qryName;
			throw new OException(msg);
		}
		return qid;
    }
    
    /*
     * Retrieve deal reset data
     * @param qid - driving query id
     * @param dbDate - today's date formated for db access
     * @return table of selected data
     */
    private Table getDeals(int qid, String dbDate, int jdEarliestDate) throws OException
    {   
    	Table deals = Util.NULL_TABLE,
    		  dates = Util.NULL_TABLE,
    		  after;
    	try 
    	{    		
    		String sqlWhere = "WHERE  qr.unique_id = " + qid + "\n"
                            ;
			
    		String sql = "SELECT ab.internal_bunit,\n"
 				   	   + "	     ab.deal_tracking_num,\n"
 				       + "       ab.tran_num,\n"
 				       + "	     ab.tran_status,\n"
 			 	       + "       ab.ins_num,\n"
 				       + "	     ab.ins_type,\n"
    				   + "	     ab.book,\n"
    				   + "       ab.tran_type,\n"
    				   + "	     ab.toolset,\n"
    				   + "	     p.proj_index,\n"
    				   + "	     p.proj_index_tenor,\n"
    				   + "	     p.index_src,\n"
    				   + "	     h.ticker,\n"
    				   + "	     h.cusip,\n"
    				   + "	     r.reset_date,\n"
    				   + "	     r.start_date,\n"
    				   + "	     r.end_date,\n"
    				   + "       'Price/Rate Reset' reset_type,\n "
    				   + "       ISNULL(hp.index_id, 0) AS keep,\n "
    				   + "       r.value_status\n"
                       + "FROM   " + Query.getResultTableForId(qid) + " qr\n"
                       + "       INNER JOIN ab_tran ab\n"
                       + "       ON ab.tran_num = qr.query_result\n"
                       + "       INNER JOIN parameter p\n"
                       + "       ON p.ins_num = ab.ins_num"
                       + "       INNER JOIN param_reset_header prh\n"
                       + "       ON prh.ins_num = ab.ins_num\n"
                       + "		 AND prh.param_seq_num = p.param_seq_num\n"	
                       + "       INNER JOIN header h\n"
                       + "		 ON h.ins_num = ab.ins_num\n"
                       + "       INNER JOIN reset r\n"
                       + "       ON r.ins_num = ab.ins_num\n"
                       + "		 AND r.param_seq_num = p.param_seq_num\n"
                       + "		 AND r.reset_date <= '" + dbDate + "'\n"
                       + ((jdEarliestDate == -1)?"":
                    	        ("AND    r.reset_date >= '" + OCalendar.formatJdForDbAccess(jdEarliestDate) + "'"))
                       + "		 LEFT OUTER JOIN idx_historical_prices hp"
                       + "		 ON hp.reset_date = r.reset_date\n"
                       + "		 AND hp.start_date = r.ristart_date\n"
                       + "	     AND hp.index_id = p.proj_index \n"
                       + "		 AND hp.ref_source = prh.ref_source\n"
                       + sqlWhere
                       + "AND    r.calc_type > 1\n"
                       + "AND  (ISNULL(hp.index_id, 0) = 0\n "
                       + "		  OR (ISNULL(hp.index_id, 0) != 0\n"
                       + "            AND r.value_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + "))\n"
                       ;			
    			

    		deals = Utils.runSql(sql);
    		PluginLog.debug("deals: Found " + deals.getNumRows() + " rows.");
    		
    		// get dates			
    		sql = "SELECT ab.internal_bunit,\n"
			    + "       ab.deal_tracking_num,\n"
				+ "       ab.tran_num,\n"
				+ "	      ab.tran_status,\n"
			 	+ "       ab.ins_num,\n"
				+ "	      ab.ins_type,\n"
				+ "	      ab.book,\n"
				+ "       ab.tran_type,\n"
				+ "	      ab.toolset,\n"
    			+ "       CASE p.spot_index WHEN 0 then p.proj_index ELSE p.spot_index END proj_index,\n"
 				+ "	      p.proj_index_tenor,\n"
 				+ "	      p.index_src,\n"
 				+ "	      h.ticker,\n"
 				+ "	      h.cusip,\n"
 				+ "	      r.reset_date,\n"
 				+ "	      r.reset_date start_date,\n"
 				+ "	      r.reset_date end_date,\n"
 				+ "       'Price/Rate Reset' reset_type,\n "
			    + "       ISNULL(hp.index_id, 0) AS keep,\n"
 			    + "       r.reset_status AS value_status\n"
                + "FROM   " + Query.getResultTableForId(qid) + " qr\n"
                + "       INNER JOIN ab_tran ab\n"
                + "       ON ab.tran_num = qr.query_result\n"
                + "       INNER JOIN parameter p\n"
                + "       ON p.ins_num = ab.ins_num\n"
                + "       INNER JOIN param_reset_header prh\n"
                + "       ON prh.ins_num = ab.ins_num\n"
                + "		  AND prh.param_seq_num = p.param_seq_num\n"	
                + "       INNER JOIN header h\n"
                + "		  ON h.ins_num = ab.ins_num\n"
                + "       INNER JOIN profile_reset r\n" // different reset table but same alias
                + "       ON r.ins_num = ab.ins_num\n"
                + "		  AND r.param_seq_num = p.param_seq_num\n"
                + "		  AND r.reset_date <= '" + dbDate + "'\n"
                + ((jdEarliestDate == -1)?"":
                		("AND    r.reset_date >= '" + OCalendar.formatJdForDbAccess(jdEarliestDate) + "'"))
                + "		  LEFT OUTER JOIN idx_historical_prices hp"
                + "		  ON hp.reset_date = r.reset_date\n"
                + "	      AND hp.index_id = p.proj_index\n"
                + "		  AND hp.ref_source = prh.ref_source\n"
                + sqlWhere
                + "AND  (ISNULL(hp.index_id, 0) = 0\n "
                + "		  OR (ISNULL(hp.index_id, 0) != 0\n"
                + "           AND r.reset_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + "))\n"
                ;

    		dates = Utils.runSql(sql);
    		PluginLog.debug("dates: Found " + dates.getNumRows() + " rows.");
    		
    		deals.select(dates,  "*",  "tran_num GT 0");
//    		for (int row = deals.getNumRows(); row >= 1;row--) {
//    			int status = deals.getInt("value_status", row);
//    			int keep = deals.getInt("keep", row);
//    			if (!(     (  keep == 0) 
//    				    || (  keep != 0 && status == VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt())) ) {
//    				deals.delRow(row);
//    			}
//    		}    		
    	}
    	finally
    	{
    		Utils.removeTable(dates);
    	}
    	
    	return deals;
    }
    
    /*
     * Retrieve non commodity cashflow resets
     * @param output  - table of selected data
     * @param qid - driving query id
     * @param dbDate - today's date formated for db access
     */
    private void getNonCommodityCflowResets(Table output, int qid, String dbDate) throws OException
    {   
    	Table cflows = Util.NULL_TABLE;
    	try 
    	{
    		String sql = "SELECT ab.internal_bunit,\n"
    			       + "       ab.deal_tracking_num,\n"
    				   + "       ab.tran_num,\n"
    				   + "	     ab.tran_status,\n"
    			 	   + "       ab.ins_num,\n"
    				   + "	     ab.ins_type,\n"
    				   + "	     ab.book,\n"
    				   + "       ab.tran_type,\n"
    				   + "	     ab.toolset,\n"
        			   + "       p.spot_index proj_index,\n"
     				   + "	     p.proj_index_tenor,\n"
     				   + "	     h.ticker,\n"
     				   + "	     h.cusip,\n"
     				   + "	     pc.cflow_type,\n"
     				   + "	     pc.cflow_date reset_date,\n"
     				   + "	     pc.cflow_date start_date,\n"
     				   + "	     pc.cflow_date end_date,\n"
     				   + "       prh.ref_source index_src,\n"
     				   + "       'Cash Flow - ' + ct.name reset_type\n"
                       + "FROM   query_result qr,\n"
                       + "       ab_tran ab,\n"
                       + "       parameter p,\n"
                       + "       header h,\n"
                       + "       physcash pc,\n"
                       + "       cflow_type ct,\n"
                       + "       param_reset_header prh\n"					// different resets
                       + "WHERE  qr.unique_id = " + qid + "\n"
                       + "AND    ab.tran_num = qr.query_result\n"
                       + "AND    ab.toolset != " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "\n"
                       + "AND    h.ins_num = ab.ins_num\n"
                       + "AND    p.ins_num = h.ins_num\n"
                       + "AND    pc.ins_num = p.ins_num\n"
                       + "AND    pc.param_seq_num = p.param_seq_num\n"
                       + "AND    pc.cflow_date <= '" + dbDate + "'\n"
                       + "AND    pc.cflow_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + "\n"
    				   + "AND    ct.id_number = pc.cflow_type\n"
                       + "AND    prh.ins_num = p.ins_num\n"
                       + "AND    prh.param_seq_num = p.param_seq_num";
    		
    		cflows = Utils.runSql(sql);
    		PluginLog.debug("Non Commodity Cashflow Resets: Found " + cflows.getNumRows() + " rows.");
    		getUnderlyingIdx(cflows);
    		
			cflows.delCol("cflow_type");
			output.select(cflows, "*", "tran_num GT 0");
    	}
        finally
       	{
        	Utils.removeTable(cflows);
    	}
    }
    		
    /*
     * If deal projection_index is not defined, retrieve index from underlying instrument...
     *   CFLOW_TYPE.PREPAYMENT_PRINCIPAL_CFLOW - spot_index of the underlying instrument
     *	 CFLOW_TYPE.COUPON_PAYMENT_CFLOW - proj_index of the underlying instrument
     * @param cflows: cash flow data
     */
    private void getUnderlyingIdx(Table cflows) throws OException
    {   
    	Table insNums = Util.NULL_TABLE,
    		  underlyingIdx = Util.NULL_TABLE,
    		  tmp = Util.NULL_TABLE;
    	int insNumQid = 0;
    	
    	try 
    	{
    		insNums = Table.tableNew();
    		insNums.select(cflows, "DISTINCT, ins_num", "ins_num GT 0");
    		insNumQid = Query.tableQueryInsert(insNums, 1);
 			
			String sqlWhere = "WHERE  qr.unique_id = " + insNumQid + "\n"
	                        + "AND    h.ins_num = qr.query_result\n"
					        + "AND    p.ins_num = h.ins_num\n"
					        + "AND    lnk.param_seq_num = p.param_seq_num\n";
			
			String sql = //"WITH underlying as (\n" +
					     "SELECT lnk.ins_num deriv_ins_num, lnk.underlying_tran, h.ins_num, p.proj_index, p.spot_index\n"
					   + "FROM   query_result qr, tran_underlying_link lnk, header h, parameter p\n" 
				       + sqlWhere 
				       + "AND    lnk.underlying_tran = h.tran_num\n"
				       + "UNION\n"
				       + "SELECT lnk.ins_num deriv_ins_num, lnk.underlying_tran_num, h.ins_num, p.proj_index, p.spot_index\n"
				       + "FROM   query_result qr, constituent_underlying_data lnk, header h, parameter p\n" 
				       + sqlWhere
				       + "AND    lnk.underlying_tran_num = h.tran_num\n"
				       + "UNION\n"
			           + "SELECT lnk.ins_num deriv_ins_num, lnk.underlying_tran_num, h.ins_num, p.proj_index, p.spot_index\n"
				       + "FROM   query_result qr, ins_component_map lnk, header h, parameter p\n" 
			           + sqlWhere
				       + "AND    lnk.underlying_tran_num = h.tran_num";
				       //+ "\n)\n"
				       //+ "SELECT DISTINCT * FROM underlying";
			
			underlyingIdx = Utils.runSql(sql);
				 
			tmp = Table.tableNew();
			tmp.select(cflows, "*", "proj_index EQ 0 and cflow_type EQ " + CFLOW_TYPE.PREPAYMENT_PRINCIPAL_CFLOW.toInt());
			if(tmp.getNumRows() > 0){
				tmp.select(underlyingIdx, "spot_index(proj_index)", "deriv_ins_num EQ $ins_num");
				cflows.select(tmp, "*", "tran_num EQ $tran_num and reset_date EQ $reset_date" +
						                " and cflow_type EQ " + CFLOW_TYPE.PREPAYMENT_PRINCIPAL_CFLOW.toInt());
			}
			Utils.removeTable(tmp);
	
			tmp = Table.tableNew();
			tmp.select(cflows, "*", "proj_index EQ 0 and cflow_type EQ "+ CFLOW_TYPE.COUPON_PAYMENT_CFLOW.toInt());
			if(tmp.getNumRows() > 0){
				tmp.select(underlyingIdx, "proj_index", "deriv_ins_num EQ $ins_num");
				cflows.select(tmp, "*", "tran_num EQ $tran_num and reset_date EQ $reset_date" +
						                " and cflow_type EQ " + CFLOW_TYPE.COUPON_PAYMENT_CFLOW.toInt());
			}
    	}
        finally
       	{
       		if (insNumQid > 0) {
       			Query.clear(insNumQid);
       		}
       		Utils.removeTable(tmp);    
       		Utils.removeTable(underlyingIdx); 
       		Utils.removeTable(insNums); 
    	}
    }
    
    /*
     * Retrieve commodity cashflow resets
     * @param output  - table of selected data
     * @param qid - driving query id
     * @param dbDate - today's date formated for db access
     */
    private void getCommodityCflowResets(Table output, int qid, String dbDate) throws OException
    {   
    	Table cflows = Util.NULL_TABLE;
    	try 
    	{		
    		String sql = "SELECT ab.internal_bunit,\n"
    			       + "       ab.deal_tracking_num,\n"
    				   + "       ab.tran_num,\n"
    				   + "	     ab.tran_status,\n"
    			 	   + "       ab.ins_num,\n"
    				   + "	     ab.ins_type,\n"
    				   + "	     ab.book,\n"
    				   + "       ab.tran_type,\n"
    				   + "	     ab.toolset,\n"
        			   + "       p.proj_index,\n"
     				   + "	     p.proj_index_tenor,\n"
        			   + "       p.index_src,\n"
     				   + "	     h.ticker,\n"
     				   + "	     h.cusip,\n"
     				   + "	     pc.cflow_date reset_date,\n"
     				   + "	     pc.cflow_date start_date,\n"
     				   + "	     pc.cflow_date end_date,\n"
     				   + "       'Cash Flow - ' + ct.name reset_type\n"
                       + "FROM   query_result qr,\n"
                       + "       ab_tran ab,\n"
                       + "       parameter p,\n"
                       + "       header h,\n"
                       + "       physcash pc,\n"
                       + "       cflow_type ct\n"
                       + "WHERE  qr.unique_id = " + qid + "\n"
                       + "AND    ab.tran_num = qr.query_result\n"
                       + "AND    ab.toolset = " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "\n"
                       + "AND    h.ins_num = ab.ins_num\n"
                       + "AND    p.ins_num = h.ins_num\n"
                       + "AND    pc.ins_num = p.ins_num\n"
                       + "AND    pc.param_seq_num = p.param_seq_num\n"
                       + "AND    pc.cflow_date <= '" + dbDate + "'\n"
                       + "AND    pc.cflow_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + "\n"
    				   + "AND    ct.id_number = pc.cflow_type";
    		
    		cflows = Utils.runSql(sql);
    		PluginLog.debug("Commodity Cashflow Resets: Found " + cflows.getNumRows() + " rows.");
    	}
        finally
       	{
        	Utils.removeTable(cflows);
    	}
    }
 
    /*
     * Retrieve notional resets
     * @param output  - table of selected data
     * @param qid - driving query id
     * @param dbDate - today's date formated for db access
     */
    private void getNotionalResets(Table output, int qid, String dbDate) throws OException
    {
    	Table notnl = Util.NULL_TABLE;
    	try 
    	{	
    		// proj_index, index_src are not selected by design
    		//
    		String sql = "SELECT ab.internal_bunit,\n"
    			       + "       ab.deal_tracking_num,\n"
    				   + "       ab.tran_num,\n"
    				   + "	     ab.tran_status,\n"
    			 	   + "       ab.ins_num,\n"
    				   + "	     ab.ins_type,\n"
    				   + "	     ab.book,\n"
    				   + "       ab.tran_type,\n"
    				   + "	     ab.toolset,\n"
     				   + "	     p.proj_index_tenor,\n"
     				   + "	     h.ticker,\n"
     				   + "	     h.cusip,\n"
     				   + "	     rst.reset_date,\n"
     				   + "	     rst.reset_start_date start_date,\n"
     				   + "	     rst.reset_end_date end_date,\n"
     				   + "       'Profile Reset - ' + rpct.rst_param_calc_type_name reset_type\n"
                       + "FROM   query_result qr,\n"
                       + "       ab_tran ab,\n"
                       + "       parameter p,\n"
                       + "       header h,\n"
                       + "       rst_param_reset_tree rst,\n"
                       + "       reset_param_calc_type rpct\n"
                       + "WHERE  qr.unique_id = " + qid + "\n"
                       + "AND    ab.tran_num = qr.query_result\n"
                       + "AND    h.ins_num = ab.ins_num\n"
                       + "AND    p.ins_num = h.ins_num\n"
                       + "AND    rst.ins_num = p.ins_num\n"
                       + "AND    rst.param_seq_num = p.param_seq_num\n"
                       + "AND    rst.reset_date <= '" + dbDate + "'\n"
                       + "AND    rst.reset_value_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + "\n"
    				   + "AND    rpct.rst_param_calc_type_id = rst.reset_calc_type";
    		
    		notnl = Utils.runSql(sql);
    		PluginLog.debug("Notional Resets: Found " + notnl.getNumRows() + " rows.");
    		
    		output.select(notnl, "*", "tran_num GT 0");
    	}
        finally
       	{
       		Utils.removeTable(notnl);
    	}
    }
    
    /*
     * Report new/amended deals that have not been validated
     * @param qryName: name of query to execute
     */
    private void getNotionalCcyResets(Table output, int qid, String dbDate)  throws OException
    {
    	Table notnl = Util.NULL_TABLE;
    	try 
    	{		
    		String sql = "SELECT ab.internal_bunit,\n"
    			       + "       ab.deal_tracking_num,\n"
    				   + "       ab.tran_num,\n"
    				   + "	     ab.tran_status,\n"
    			 	   + "       ab.ins_num,\n"
    				   + "	     ab.ins_type,\n"
    				   + "	     ab.book,\n"
    				   + "       ab.tran_type,\n"
    				   + "	     ab.toolset,\n"
        			   + "       pcp.fx_index proj_index,\n"
     				   + "	     p.proj_index_tenor,\n"
        			   + "       p.index_src,\n"
     				   + "	     h.ticker,\n"
     				   + "	     h.cusip,\n"
     				   + "	     pnr.reset_date,\n"
     				   + "       pnr.rfis_date start_date,\n"
     				   + "       'Profile Notional Reset' reset_type\n"
                       + "FROM   query_result qr,\n"
                       + "       ab_tran ab,\n"
                       + "       parameter p,\n"
                       + "       header h,\n"
                       + "       profile_notnl_reset pnr,\n"
                       + "       profile_currency_param pcp\n"
                       + "WHERE  qr.unique_id = " + qid + "\n"
                       + "AND    ab.tran_num = qr.query_result\n"
                       + "AND    h.ins_num = ab.ins_num\n"
                       + "AND    p.ins_num = h.ins_num\n"
                       + "AND    pnr.ins_num = p.ins_num\n"
                       + "AND    pnr.param_seq_num = p.param_seq_num\n"
                       + "AND    pnr.reset_date <= '" + dbDate + "'\n"
                       + "AND    pnr.reset_status = " + VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt() + "\n"
                       + "AND    pcp.ins_num = p.ins_num\n"
                       + "AND    pcp.param_seq_num = p.param_seq_num";
    			
    		notnl = Utils.runSql(sql);
    		PluginLog.debug("Notional Currency Resets: Found " + notnl.getNumRows() + " rows.");
    		
    		output.select(notnl, "*", "tran_num GT 0");
    	}
        finally
       	{
       		Utils.removeTable(notnl);
    	}
    }
    
    /**
     * Generate report showing missing validations
     * @param 
     */
    private Table createReport(Table data, String regionCode, String filename) throws OException
    {
		Table report = createContainer();
		report.select(data, "*", "tran_num GT 0");

		formatOutput(report, stdLib.STD_GetReportType());
		groupOutput(report);

		report.groupTitleAbove("internal_bunit");
		report.setTitleBreakPosition( ROW_POSITION_ENUM.ROW_BOTH);
		report.setTitleAboveChar("=");
		report.setTitleBelowChar("=");
		report.showTitleBreaks();
		report.colHide("internal_bunit");
		
		//String filename = "Missed_Resets.eod"; 
		String title = "Missed Resets Report for "
				     + OCalendar.formatJd(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
        if (regionCode.length() > 0) 
        {
        	filename = regionCode + "_" + filename;
        	title = regionCode + " " + title;
        }
        
        if(report.getNumRows() > 0)
        {
            Report.reportStart(filename, title);
            report.setTableTitle(title);
            Report.printTableToReport(report, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd ();
            report.colShow("internal_bunit");
        }
        else
        {
        	report.hideTitleBreaks ();
        	report.hideTitles ();
            Report.reportStart (filename, title);
            report.setTableTitle("No Missing Resets");
            Report.printTableToReport (report, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd ();
        }	

		return report;
    }
    
    /**
     * Create report container
     */
    private Table createContainer() throws OException
    {
		Table container = Table.tableNew();		
		container.addCol("internal_bunit", 		COL_TYPE_ENUM.COL_INT);
		container.addCol("reset_date", 			COL_TYPE_ENUM.COL_DATE_TIME);
		container.addCol("deal_tracking_num",	COL_TYPE_ENUM.COL_INT);
		container.addCol("tran_num", 			COL_TYPE_ENUM.COL_INT);
		container.addCol("tran_status", 		COL_TYPE_ENUM.COL_INT);
		container.addCol("ins_num", 			COL_TYPE_ENUM.COL_INT);
		container.addCol("ins_type", 			COL_TYPE_ENUM.COL_INT); 
		container.addCol("start_date", 			COL_TYPE_ENUM.COL_DATE_TIME);
		container.addCol("end_date", 			COL_TYPE_ENUM.COL_DATE_TIME);
		container.addCol("book", 				COL_TYPE_ENUM.COL_STRING);
		container.addCol("ticker", 				COL_TYPE_ENUM.COL_STRING);
		container.addCol("cusip", 				COL_TYPE_ENUM.COL_STRING);
		container.addCol("tran_type", 			COL_TYPE_ENUM.COL_INT);
		container.addCol("toolset", 			COL_TYPE_ENUM.COL_INT); 
		container.addCol("reset_type",			COL_TYPE_ENUM.COL_STRING);
		container.addCol("proj_index", 			COL_TYPE_ENUM.COL_INT);
		container.addCol("index_src", 			COL_TYPE_ENUM.COL_INT);
		container.addCol("proj_index_tenor", 	COL_TYPE_ENUM.COL_INT);
		return container;
    }
    
    /**
     * Format report data
     * @param report table
     * @param report type (which columns to show)
     */
	private void formatOutput(Table report, int report_type) throws OException
	{		
		report.setColTitle( "tran_num",          "Tran\nNum");
		report.setColTitle( "tran_status",       "Tran\nStatus");
		report.setColTitle( "deal_tracking_num", "Deal\nNum");
		report.setColTitle( "ins_type",          "Ins\nType");      
		report.setColTitle( "proj_index",        "Projection\nIndex");
		report.setColTitle( "index_src",         "Index\nSource");
		report.setColTitle( "proj_index_tenor",  "Projection\nIndex Tenor"); 
		report.setColTitle( "book",              "\nBook");
		report.setColTitle( "ins_num",           "Ins\nNum");
		report.setColTitle( "reset_date",        "Reset\nDate"); 
		report.setColTitle( "start_date",        "Start\nDate");
		report.setColTitle( "end_date",          "End\nDate");
		report.setColTitle( "ticker",            "\nTicker");
		report.setColTitle( "cusip",             "\nCusip");
		report.setColTitle( "reset_type",        "Reset\nType");

		report.setColFormatAsRef( "internal_bunit", 	 SHM_USR_TABLES_ENUM.PARTY_TABLE);
		report.setColFormatAsRef( "tran_status", 		 SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE); 
		report.setColFormatAsRef( "ins_type", 			 SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		report.setColFormatAsRef( "proj_index", 		 SHM_USR_TABLES_ENUM.INDEX_TABLE);
		report.setColFormatAsRef(  "index_src", 		 SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
		report.setColFormatAsRef(  "internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		report.setColFormatAsPymtPeriod( "proj_index_tenor");
		report.setColFormatAsDate( "reset_date");
		report.setColFormatAsDate( "start_date");
		report.setColFormatAsDate( "end_date");

		report.formatSetJustifyLeft( "internal_bunit");
		report.formatSetJustifyLeft( "deal_tracking_num");
		report.formatSetJustifyLeft( "tran_status");
		report.formatSetJustifyLeft( "tran_num");
		report.formatSetJustifyCenter( "ins_type");
		report.formatSetJustifyLeft( "ins_num");
		report.formatSetJustifyLeft( "proj_index");
		report.formatSetJustifyLeft( "proj_index_tenor");
		report.formatSetJustifyLeft( "index_src");
		report.formatSetJustifyLeft( "reset_type");
		report.formatSetJustifyCenter( "reset_date");
		report.formatSetJustifyCenter( "start_date");
		report.formatSetJustifyCenter( "end_date");

		report.formatSetWidth( "reset_date",         12);
		report.formatSetWidth( "start_date",         12);
		report.formatSetWidth( "end_date",           12);
		report.formatSetWidth( "internal_bunit",     45);
		report.formatSetWidth( "proj_index",         20);
		report.formatSetWidth( "proj_index_tenor",   11);
		report.formatSetWidth( "index_src",          15);
		report.formatSetWidth( "deal_tracking_num",   9);
		report.formatSetWidth( "tran_num",            9);
		report.formatSetWidth( "tran_status",        15);
		report.formatSetWidth( "ins_num",             9);
		report.formatSetWidth( "ins_type",           20);
		report.formatSetWidth( "book",               20);
		report.formatSetWidth( "cusip",              20);
		report.formatSetWidth( "ticker",             20);
		report.formatSetWidth( "reset_type",	     35);
		
		if(report_type == 2)
		{
			report.colHide( "proj_index_tenor");
			report.colHide( "start_date");
			report.colHide( "end_date");			
		}
		else
		{
			report.colHide( "ticker");
			report.colHide( "cusip");

		}
		
		report.delCol("toolset");
		report.delCol("tran_type");
	}

    /**
     * Sort data by Internal BU, deal no, tran no
     * @param report table
     */
	private void groupOutput(Table report) throws OException
	{
		report.clearGroupBy();
		report.groupFormatted( "internal_bunit, reset_date,  proj_index, index_src, deal_tracking_num, tran_num, start_date");
	}
	
	//1.2 SR 232369 - Added function to get filename
	private String getFileName(String region) {
		// TODO Auto-generated method stub
		String strFilename;
		Table envInfo = Util.NULL_TABLE;
		StringBuilder fileName = new StringBuilder();

		try 
		{
		envInfo = com.olf.openjvs.Ref.getInfo();
		fileName.append(Util.reportGetDirForToday()).append("\\");
		fileName.append(envInfo.getString("task_name", 1));
		fileName.append(".csv");
		}
		catch (OException e) {
			e.printStackTrace();
		}
		strFilename = fileName.toString();
		
		return strFilename;
	}
}