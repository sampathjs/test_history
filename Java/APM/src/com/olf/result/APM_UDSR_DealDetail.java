/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_DealDetail.java

Date Of Last Revision:     18-May-2015 - New UDSR
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back detail level information from deal.
                            
*/

package com.olf.result;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.olf.result.APMUtility.APMUtility;

public class APM_UDSR_DealDetail implements IScript {
	
/*-------------------------------------------------------------------------------
  Description:   DealDetail UDSR Main
  Return Values: returnt is a global table  
  -------------------------------------------------------------------------------*/
    public void execute(IContainerContext context) throws OException
    {
       Table argt = context.getArgumentsTable();
       Table returnt = context.getReturnTable();
	
       int operation = 0;
       
       if (argt.getNumRows() < 1)
       {
    	   APMUtility.APM_Print("APM_UDSR_DealDetail:  no argument passed in. ");
    	   Util.exitFail();
       }
       
       operation = argt.getInt( "operation", 1);
       if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt() )
           compute_result(argt, returnt);
       else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
           format_result(returnt);
       
       Util.exitSucceed();
    }
	
    /*-------------------------------------------------------------------------------
    Description:   UDSR format function. (Default Formatting used)
    -------------------------------------------------------------------------------*/
    void format_result(Table returnt) throws OException
    {	
        returnt.setColTitle("deal_num", "Deal Number");
        returnt.setColTitle("tran_num", "Transaction\nNumber");
        returnt.setColTitle("deal_leg", "Deal Side");
        returnt.setColTitle("deal_pdc", "Deal PDC");
        returnt.setColTitle("tran_status", "Tran\nStatus");
        returnt.setColFormatAsRef("tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
        returnt.setColTitle("event_source", "Event\nSource");
        returnt.setColFormatAsRef("event_source", SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
        returnt.setColTitle("ins_seq_num", "Ins Seq Num");
        returnt.setColTitle("ins_source_id", "Ins Source\nID");
        returnt.setColTitle("payment_date", "Payment\nDate");
        returnt.setColFormatAsDate("payment_date");
        returnt.setColTitle("strategy_id", "Strategy\nID");
        returnt.setColTitle("parcel_id", "Parcel\nID");

    	return;
    }
  
    /*-------------------------------------------------------------------------------
    Description:   build deal info detail result using getStrategy interface.
    -------------------------------------------------------------------------------*/
    void compute_result(Table argt, Table returnt) throws OException
    {
    	Table tTrans = argt.getTable("transactions", 1);
    	Table tranStrategy, tempTable = Util.NULL_TABLE;
    	int tranNum=0;
    	
    	if (Table.isTableValid(tTrans) != 1)
    	{
    		APMUtility.APM_Print("APM_UDSR_DealDetail: Invalid transaction table.");
    		throw new OException("Invalid transaction table."); 
    	}
    	
        APMUtility.APM_Print("APM_UDSR_DealDetail: preparing result table for " + tTrans.getNumRows() + " deals.");
    	for (int i = 1; i <= tTrans.getNumRows(); i++)
    	{
    		Transaction tran = tTrans.getTran("tran_ptr", i);
    		try
    		{
    			tranNum = tran.getTranNum();
    			tranStrategy = tran.getStrategies();
    			tranStrategy.addCol("parcel_id", COL_TYPE_ENUM.COL_INT, "parcel_id");

    			for (int iRow = 1; iRow <= tranStrategy.getNumRows(); iRow++)
    			{
    				if (tranStrategy.getInt("event_source", iRow) == 33 || tranStrategy.getInt("event_source", iRow) == 34  // parcel(33) or parcel commodity(34)
    						|| tranStrategy.getInt("event_source", iRow) == 39 )  					// forward balance(39)
    					tranStrategy.setInt("parcel_id", iRow, tranStrategy.getInt("ins_seq_num", iRow));
    			}   
    			
    			if (1 == i)
    				tempTable = tranStrategy.copyTable();
    			else
    				tranStrategy.copyRowAddAll(tempTable);

    			tranStrategy.destroy();
    		}
    		catch (Exception e)
    		{
    			APMUtility.APM_Print("APM_UDSR_DealDetail: tran num " +tranNum + " failed to get strategy. " + e.getMessage());
    		}
    	}

    	returnt.select(tempTable, "*", "deal_num GT 0");
    	
    	tempTable.destroy();
    }
}
