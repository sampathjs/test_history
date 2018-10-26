package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.enums.PartyRegion;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.udsr.TranData;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * UDSR for Metal Ledger
 * This class extends 'TranData' and adds ML specific fields.
 * Supported toolsets -
 * 1. Commodity
 * 2. Cash
 * @author jains03
 *
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class MetalsLedgerData extends TranData 
{
    /**
     * This method adds ML specific columns to the Output table of UDSR
     */
    @Override
    protected Table defineOutputTable(Table data) throws OException
    {
        data = super.defineOutputTable(data);
        
        data.addCol("site", COL_TYPE_ENUM.COL_INT, "Site"); 
        
        return data;
    }

    /**
     * For all transactions in the UDSR input -
     * 1. Populate the delivery site
     */
    @Override
    protected void calculate(IContainerContext context) throws OException 
    {
        PluginLog.info("Calculating Metals Ledger data");
        super.calculate(context);
        
        Table deliverySite = getSite(tblReturnt);
        PluginLog.debug("Deal parameters populated ");
        int numOutputRow = tblReturnt.getNumRows();

        //Populate Site 
        tblReturnt.select(deliverySite, "delivery_facility(site)", "deal_num EQ $deal_num AND leg EQ $deal_leg_phy");
        OConsole.print("Location populated for HK metal extract only and for Cash trades of ins sub type of Cash Transfer" );
        deliverySite.destroy();
        PluginLog.debug("Site populated ");
        if(numOutputRow != tblReturnt.getNumRows()) 
        {
            PluginLog.warn("Number of rows in output changed during delivery_facility(site) enrichment. Expected=" +numOutputRow + ",Actual="+ tblReturnt.getNumRows());
        }
        Table Location = getlocationInfoExtAccount(tblReturnt);
        tblReturnt.select(Location, "location", "tran_num EQ $tran_num ");
        Location.destroy();
        
        PluginLog.debug("Location populated for HK metal extract only and for Cash trades of ins sub type of Cash Transfer ");
        if(numOutputRow != tblReturnt.getNumRows()) 
        {
            PluginLog.warn("Number of rows in output changed during Location enrichment. Expected=" +numOutputRow + ",Actual="+ tblReturnt.getNumRows());
        }
        
        
        /* Adjust signage for position and cash */
        Util.adjustSignageForJDE(tblReturnt, "tran_status");  
        PluginLog.info("Finished calculating Metals Ledger data");
    }
    
    @Override
    protected void format(IContainerContext context) throws OException 
    {
    	
        super.format(context);
        
        Table returnt = context.getReturnTable();

        //Show ML specific columns
        returnt.colShow("form");
        returnt.colShow("purity");
        
        returnt.setColFormatAsRef("form", SHM_USR_TABLES_ENUM.COMMODITY_FORM_TABLE); 
        returnt.setColFormatAsRef("purity", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE); 
        returnt.setColFormatAsRef("site", SHM_USR_TABLES_ENUM.FACILITY_TABLE); 
        returnt.setColFormatAsDate("returnDate");
        
    }
   	
    /**
     * This method queries the DB to fetch delivery site of all deal legs of all deal nums in the @param dealTable
     * It is applicable for Comm-Phys (Commodity) deals where different legs of a deal may have different delivery facility (site)
     * @param dealTable
     * @return
     * @throws OException
     */
    private Table getSite(Table dealTable) throws OException 
    {
        Table deliverySite = null;
        int dealTableQueryId = 0;

        try 
        {
            dealTableQueryId = Query.tableQueryInsert(dealTable, 2);
            PluginLog.debug(" queryId " + dealTableQueryId);
            
            if (dealTableQueryId <= 0) 
            {
                String error = "Unable to insert deal numbers in DB query_result table";
                
                throw new AccountingFeedRuntimeException(error);
            }

            String sQueryTable = Query.getResultTableForId(dealTableQueryId);
            deliverySite = Table.tableNew();

            String deliverySiteQuery = 
                    "SELECT \n" +                    
                            "deal_delivery.deal_num, deal_delivery.param_seq_num as leg, comm_sched_delivery_cmotion.delivery_facility \n" +
                    "FROM \n" +
                        "( \n" +
                        "SELECT \n" + // Sub-query to select Max delivery_id per deal leg
                            "MAX(deliv_deal.delivery_id) as delivery_id, deliv_deal.deal_num, deliv_deal.param_seq_num \n" +
                            "FROM \n" +
                            "comm_sched_deliv_deal deliv_deal, \n" +
                            "( select query_result from " + sQueryTable + " where unique_id=" + dealTableQueryId + " ) as q \n" +
                        "WHERE q.query_result=deliv_deal.deal_num \n" +
                        "GROUP BY deliv_deal.deal_num, deliv_deal.param_seq_num \n" +
                        ") deal_delivery \n" +
                  "JOIN \n" +
                      "comm_sched_delivery_cmotion  \n" +
                  "ON deal_delivery.delivery_id = comm_sched_delivery_cmotion.delivery_id";
            
            int iRetVal = DBaseTable.execISql(deliverySite, deliverySiteQuery);
            if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) 
            {
                throw new AccountingFeedRuntimeException(" Unable to execute query SQL. Return code= " + iRetVal + "." + deliverySiteQuery);
            }

        }
        catch (OException oe) 
        {
            PluginLog.error("Exception for queryId=" + dealTableQueryId + "." + oe.getMessage());
            throw oe;
        }
        finally 
        {
            Query.clear(dealTableQueryId);
        }
        return deliverySite;
    }
  //Populate Location for Cash deals in ML for HK region only

  	private   Table getlocationInfoExtAccount(Table dealTable) throws OException
  	{   //tblgetlocationInfoExtAccount
  		Table Location = null;
  		int dealTableQueryId = 0;

  		try 
  		{
  			dealTableQueryId = Query.tableQueryInsert(dealTable, 1);
  			PluginLog.debug(" queryId " + dealTableQueryId);
          
  			if (dealTableQueryId <= 0) 
  			{
  				String error = "Unable to insert deal numbers in DB query_result table";
              
  				throw new AccountingFeedRuntimeException(error);
          }

          
          Location = Table.tableNew();
  		
  	   // tblgetlocationInfoExtAccount = Table.tableNew();

  	        String LocationQuery = 
  	                "SELECT \n" +
  	                	" ab.tran_num, \n" +
  	                	" ai.info_value as location \n" +
  	                "FROM ab_tran_settle_account_view abv \n" +
      	            "JOIN account_info ai  on abv.account_id = ai.account_id  \n" +
      	            "JOIN ab_tran ab on ab.tran_num = abv.tran_num \n"  +
      	            "JOIN account_info_type at on at.type_id = ai.info_type_id \n" +
      	            "JOIN  query_result q on  q.query_result= ab.tran_num  \n" +
      	            "WHERE q.unique_id= " + dealTableQueryId + 
  	                "\n and at.type_name = 'Loco' \n" +
  	                "and abv.int_ext =1 \n" +
  	              	"and ab.toolset =" + TOOLSET_ENUM.CASH_TOOLSET.jvsValue()  ; 
  	              
      	            
  	        int iRetVal = DBaseTable.execISql(Location, LocationQuery);
              if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) 
              {
                  throw new AccountingFeedRuntimeException(" Unable to execute query SQL. Return code= " + iRetVal + "." + LocationQuery);
              }

          }
          catch (OException oe) 
          {
              PluginLog.error("Exception for queryId=" + dealTableQueryId + "." + oe.getMessage());
              throw oe;
          }
          finally 
          {
              Query.clear(dealTableQueryId);
          }
          return Location;
      }
        
  	
  	//end
}