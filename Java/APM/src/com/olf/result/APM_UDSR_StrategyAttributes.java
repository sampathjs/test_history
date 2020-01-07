/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_StrategyAttributes.java

Date Of Last Revision:     24-Mar-2015 - New UDSR
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back Strategy Attributes from core db tables.
                            
*/
package com.olf.result;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_StrategyAttributes implements IScript {
	    

/*-------------------------------------------------------------------------------
  Description:   Strategy Attribute UDSR Main
  Return Values: returnt is a global table  
  -------------------------------------------------------------------------------*/
    public void execute(IContainerContext context) throws OException
    {
       Table argt = context.getArgumentsTable();
       Table returnt = context.getReturnTable();
	
       int operation;
       
       operation = argt.getInt( "operation", 1);
	
       if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt() )
           compute_result(argt, returnt);
       else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
           format_result(returnt);
       
       Util.exitSucceed();
    }
	

    public void compute_result(Table argt, Table returnt)
          throws OException 
    {
    	Table dataTable = preparePreParcelReturnTable();
    	try
    	{
    		retrieveStrategyAttribute(dataTable);	  
    		returnt.select(dataTable, "*", "strategy_id GE 0");
    	}
    	finally
    	{
    		dataTable.destroy();
    	}
    }     
    
    public void format_result(Table returnt) throws OException 
    {
       returnt.setColTitle("strategy_id", "Strategy ID");
       returnt.setColFormatAsRef("strategy_id",	SHM_USR_TABLES_ENUM.STRATEGY_LISTING_TABLE);
       
       returnt.setColTitle("internal_bunit", "Internal\nBunit");
       returnt.setColFormatAsRef("internal_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
      
       returnt.setColTitle("internal_lentity", "Internal\nLentity");
       returnt.setColFormatAsRef("internal_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
     
       returnt.setColTitle("internal_portfolio", "Internal\nPfolio");
       returnt.setColFormatAsRef("internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
       
       returnt.setColTitle("tran_status", "Status");
       returnt.setColFormatAsRef("tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);

       returnt.setColTitle("start_date", "Start Date");
       returnt.setColTitle("end_date", "End Date");
       
    }
    
    public Table preparePreParcelReturnTable() throws OException
    {
 	   Table dataTable = Table.tableNew();
 	   
 	   dataTable.addCol("strategy_id", COL_TYPE_ENUM.COL_INT);
 	   dataTable.addCol("internal_bunit", COL_TYPE_ENUM.COL_INT);
 	   dataTable.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT);
 	   dataTable.addCol("internal_portfolio", COL_TYPE_ENUM.COL_INT);
 	   dataTable.addCol("tran_status", COL_TYPE_ENUM.COL_INT);
 	   dataTable.addCol("start_date", COL_TYPE_ENUM.COL_DATE_TIME);
 	   dataTable.addCol("end_date", COL_TYPE_ENUM.COL_DATE_TIME);	
 	   
 	   return dataTable;
    }
    
    public void retrieveStrategyAttribute(Table dataTable) throws OException 
    {
 	   
 	   String sQuery = "select deal_tracking_num strategy_id, internal_bunit, internal_lentity, internal_portfolio, tran_status, start_date, maturity_date end_date from ab_tran where tran_type = 39"; 
 	    	   
 	   com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);
    }
 }
