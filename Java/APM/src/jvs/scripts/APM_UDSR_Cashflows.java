/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_Cashflows.java

Date Of Last Revision:     10-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM Cashflows results.
                           Start out by getting PFOLIO_RESULT_TYPE.CFLOW_FUTURE_BY_DEAL_RESULT, 
                           PFOLIO_RESULT_TYPE.CFLOW_PROJECTED_BY_DEAL_RESULT and PFOLIO_RESULT_TYPE.MTM_DETAIL_RESULT values.
                           After that, use the logic from original tfe_cflow calculations.
                           
                                                      
*/
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class APM_UDSR_Cashflows implements IScript {
   
/*-------------------------------------------------------------------------------
Name:          main()
Description:   UDSR Main
Parameters:      
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
   else if (operation == USER_RESULT_OPERATIONS.USER_RES_OP_DWEXTRACT.toInt()) {
	   //argt.setString( "error_msg", 1, "DWEXTRACT not supported.");
	   OConsole.oprint("\nDWEXTRACT not supported.");
   }
   else {
       argt.setString( "error_msg", 1, "Incorrect operation code");
       throw new OException("Incorrect operation code");
   } 
 
   Util.exitSucceed();

}

/*-------------------------------------------------------------------------------
Name:          compute_result()
Description:   does the calculation for the result.
Parameters:    Table argt, Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result(Table argt, Table returnt) throws OException
{
   Table    tScenResult   = null;      /* Not freeable */
   Table    tTranResult   = null;      /* Not freeable */
   Table    tTransactions = null;      /* Not freeable */
   Table    tGenResult    = null;      /* Not freeable */
   Table    tResultsList  = null;     

   Table    tMTMResult=null, tCflowFutureResult=null, tCflowProjResult=null; /* Not freeable */
   Table    tBondMTMResult=null, tBondCflowFutureResult=null, tBondCflowProjResult=null, tBondCFLOW=null, tTemp=null, tCurrCFLOW=null, tToolsets=null;
     
   int         iEventSourceCol = 0;
   int         iCflowTypeCol = 0;
   int         i=0;
     
   
   tScenResult = argt.getTable( "sim_results", 1);
   tTranResult = tScenResult.getTable( 1, 1);
   tTransactions = argt.getTable( "transactions",1);
   tGenResult = tScenResult.getTable( 1, 4);

   /* Set up the table */
   returnt.addCols( "I(dealnum) I(leg) I(projindex) I(ins_seq_num) I(event_source) I(cflow_type) I(cflow_date)");
   returnt.addCols( "F(pv) F(df) F(known_cflow) F(proj_cflow) F(all_cflow) I(disc_index)");

      
   tResultsList = SimResult.getGenResultTables(tGenResult, PFOLIO_RESULT_TYPE.MTM_DETAIL_RESULT.toInt());
   if (tResultsList != null && tResultsList.getNumRows() > 0)
   {   
	  tMTMResult = tResultsList.getTable( 1, 1);
   }
         
   if (tMTMResult == null || tMTMResult.getNumRows() == 0)
	   return;
	
   /* Prepare the tran-to-toolset table */
   tToolsets = create_tran_to_toolset_table(tTransactions);
   
   tResultsList = SimResult.getGenResultTables(tGenResult, PFOLIO_RESULT_TYPE.CFLOW_FUTURE_BY_DEAL_RESULT.toInt());
   if (tResultsList != null && tResultsList.getNumRows() > 0)
   {   
   	  tCflowFutureResult = tResultsList.getTable( 1, 1);
   }
   
   tResultsList = SimResult.getGenResultTables(tGenResult, PFOLIO_RESULT_TYPE.CFLOW_PROJECTED_BY_DEAL_RESULT.toInt());
   if (tResultsList != null && tResultsList.getNumRows() > 0)
   {
   	  tCflowProjResult = tResultsList.getTable( 1, 1);
   }
      
   /* enrich with toolset */
   if (tCflowFutureResult != null && (tCflowFutureResult.getNumRows() > 0))
   {
	   tCflowFutureResult.sortCol("deal_num");
       tCflowFutureResult.select( tToolsets, "PREORDERED, toolset", "dealnum EQ $deal_num");
   }

   if (tCflowProjResult != null && tCflowProjResult.getNumRows() > 0)
   {      
	   tCflowProjResult.sortCol("deal_num");
       tCflowProjResult.select( tToolsets, "PREORDERED, toolset", "dealnum EQ $deal_num");
   }

   tMTMResult.sortCol("deal_num");
   tMTMResult.select( tToolsets, "PREORDERED, toolset", "dealnum EQ $deal_num");

   if (tToolsets != null)              
	   tToolsets.destroy();
   
   /* Copy Bond (toolset = 5) & MM (toolset = 20) to separate table  */
   tBondCFLOW = returnt.cloneTable();
   if (tCflowFutureResult != null ) 
   	  tBondCflowFutureResult = tCflowFutureResult.cloneTable();
      
   if (tCflowProjResult != null)
   	  tBondCflowProjResult = tCflowProjResult.cloneTable();
      
   tBondMTMResult = tMTMResult.cloneTable();

   
   if (tCflowFutureResult != null && tCflowFutureResult.getNumRows() > 0)
   {
      tBondCflowFutureResult.select(tCflowFutureResult, "*", "toolset EQ " + Integer.toString(TOOLSET_ENUM.BOND_TOOLSET.toInt()) + " AND event_source NE " + Integer.toString(EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt()));
      tBondCflowFutureResult.select(tCflowFutureResult, "PREORDERED, *", "toolset EQ " + Integer.toString(TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt())+ " AND event_source NE " + Integer.toString(EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt()));
   }

   if (tCflowProjResult != null && tCflowProjResult.getNumRows() > 0)
   {      
       tBondCflowProjResult.select(tCflowProjResult, "*", "toolset EQ " + Integer.toString(TOOLSET_ENUM.BOND_TOOLSET.toInt()) + " AND event_source NE " + Integer.toString(EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt()));
       tBondCflowProjResult.select(tCflowProjResult, "PREORDERED, *", "toolset EQ " + Integer.toString(TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt()) + " AND event_source NE " + Integer.toString(EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt()));
   }

   tBondMTMResult.select(tMTMResult, "*", "toolset EQ " + Integer.toString(TOOLSET_ENUM.BOND_TOOLSET.toInt()) + " AND event_source NE " + Integer.toString(EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt()));
   tBondMTMResult.select(tMTMResult, "PREORDERED, *", "toolset EQ " + Integer.toString(TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt()) + " AND event_source NE " + Integer.toString(EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt()));

     /* delete bond & MM from main tables  */
      if ( tBondMTMResult.getNumRows() > 0 )
      {
    	 delete_bond_and_mm(tMTMResult);
      }

      if ( tBondCflowFutureResult != null && tBondCflowFutureResult.getNumRows() > 0 )
      {
    	 delete_bond_and_mm(tCflowFutureResult);
      }

      if (tBondCflowProjResult != null && tBondCflowProjResult.getNumRows() > 0)
      {
    	 delete_bond_and_mm(tCflowProjResult);
      }

      /* ==== TOOLSETS OTHER THAN BONDS & MM  ======== */

      returnt.tuneGrowth( tMTMResult.getNumRows());
      returnt.select( tMTMResult,
                    " deal_num (dealnum), deal_leg (leg), ins_seq_num, event_source,"+
                    " payment_date (cflow_date), pv", "deal_num GT 0" );


      /* When cashflow number comes from the Profile it is always the interest type */
      /*   As MTM sim results does not include cashflow type information, we have to enforce the correct type here */
      
      iEventSourceCol = returnt.getColNum( "event_source" );
      iCflowTypeCol   = returnt.getColNum( "cflow_type" );

      for( i = returnt.getNumRows(); i > 0; i-- )
      {
         if( returnt.getInt( iEventSourceCol, i ) == EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt() )
         {
             returnt.setInt( iCflowTypeCol, i, CFLOW_TYPE.INTEREST_CFLOW.toInt() );            
         }
      }
      


      if (tCflowFutureResult != null && tCflowFutureResult.getNumRows() > 0)
      {
         returnt.select( tCflowFutureResult,
                    "df, discounted_cflow (known_cflow), cflow_type",
                    "deal_num EQ $dealnum AND deal_leg EQ $leg AND ins_seq_num EQ $ins_seq_num AND event_source EQ $event_source" );

      }

      if (tCflowProjResult != null && tCflowProjResult.getNumRows() > 0)
      {
         returnt.select( tCflowProjResult,
                    "df, discounted_cflow (proj_cflow), cflow_type",
                    "deal_num EQ $dealnum AND deal_leg EQ $leg AND ins_seq_num EQ $ins_seq_num AND event_source EQ $event_source" );

      }

      /* ==== END OF TOOLSETS OTHER THAN BONDS & MM  ======== */

      /* ==== BONDS & MM only ======== */

      /* make future known & proj the same table struct & copy proj into known */
      if (tBondCflowFutureResult != null)
    	  tBondCflowFutureResult.addCol( "proj_cflow", COL_TYPE_ENUM.COL_DOUBLE);
      
      if (tBondCflowProjResult != null)
    	  tBondCflowProjResult.setColName( 7, "proj_cflow");
      
      if (tBondCflowFutureResult != null && tBondCflowProjResult != null)	
      {
    	  tBondCflowFutureResult.tuneGrowth(tBondCflowProjResult.getNumRows());
    	  tBondCflowProjResult.copyRowAddAllByColName(  tBondCflowFutureResult);
      }      
      if (tBondCflowProjResult != null)   
    	  tBondCflowProjResult.destroy();
      
      /* group by deal, leg, event source & date & remove non summed */
      /* this gives us the subtotals by date */
      if (tBondCflowFutureResult != null  )
      {
    	 if (tBondCflowFutureResult.getNumRows() > 0)
    	 {
	         tBondCflowFutureResult.group( "deal_num,deal_leg,ins_seq_num,event_source,cflow_date");
	         tBondCflowFutureResult.groupSum( "cflow_date");
	         
	         for (i = tBondCflowFutureResult.getNumRows(); i > 0; i--)
	         {
	            if ( tBondCflowFutureResult.getRowType( i) == ROW_TYPE_ENUM.ROW_GROUP_SUM.toInt())
	               tBondCflowFutureResult.setRowType( i, ROW_TYPE_ENUM.ROW_DATA);
	            else
	               tBondCflowFutureResult.delRow( i);
	         }
          }

          /* now add in all the lines from MTM */
       	  tBondCflowFutureResult.tuneGrowth( tBondMTMResult.getNumRows());
    	  tBondCflowFutureResult.select( tBondMTMResult,
                    "deal_num, payment_date(cflow_date), deal_leg, ins_seq_num, event_source, currency, pv", 
                    "deal_num GT 0" );

    	  /* group again & remove non summed - MTM done separately so not too much memory used up - will take speed hit tho */
    	  if (tBondCflowFutureResult.getNumRows() > 0 )
    	  {
    		  tBondCflowFutureResult.group( "deal_num,deal_leg,ins_seq_num,event_source,cflow_date");
    		  tBondCflowFutureResult.groupSum( "cflow_date");
    		  for (i = tBondCflowFutureResult.getNumRows(); i > 0; i--)
    		  {
    			  if ( tBondCflowFutureResult.getRowType( i) == ROW_TYPE_ENUM.ROW_GROUP_SUM.toInt())
    				  tBondCflowFutureResult.setRowType( i, ROW_TYPE_ENUM.ROW_DATA);
    			  else
    				  tBondCflowFutureResult.delRow( i);
    		  }
    	  }	

    	  /* Copy into the tCFLOW table */
    	  
    	  if (tBondCflowFutureResult.getNumRows() > 0)
    	  {
    		  tBondCFLOW.tuneGrowth(tBondCflowFutureResult.getNumRows());
    		  tBondCFLOW.select( tBondCflowFutureResult,
                    "deal_num (dealnum), deal_leg (leg), ins_seq_num, event_source, cflow_date, df, discounted_cflow(known_cflow), proj_cflow, pv", 
                    "deal_num GT 0" );
    		  tBondCFLOW.mathAddCol( "known_cflow", "proj_cflow", "all_cflow");
    	  }

    	  /* work out the adjustments to pv in a separate table */
    	  tTemp = tBondCFLOW.cloneTable();
    	  tTemp.select( tBondCFLOW,
                    "dealnum, leg, ins_seq_num, event_source, cflow_date, df, all_cflow, pv", 
                    "known_cflow EQ 0.0 AND proj_cflow EQ 0.0 AND pv NE 0.0" );
    	  tTemp.select( tBondCFLOW,
                    "SUM, dealnum, leg, all_cflow", 
                    "dealnum EQ $dealnum AND leg EQ $leg" );
    	  tTemp.mathSubCol( "pv", "all_cflow", "pv");

    	  /* set all of the pvs in the tBondCflow table to known cflow - any later adjustments will overwrite */
    	  tBondCFLOW.mathAddCol( "known_cflow", "proj_cflow", "pv"); 

    	  /* copy adjustments back */
    	  tBondCFLOW.select( tTemp,
                    "cflow_date, pv", 
                    "dealnum EQ $dealnum AND leg EQ $leg AND ins_seq_num EQ $ins_seq_num AND event_source EQ $event_source" );

    	  /* delete any rows which are included because of rounding errors (sum of cflows does not always = MTM pv cos of rounding) */
    	  for ( i = 1; i < tBondCFLOW.getNumRows(); i++)
    	  {
    		  if ( java.lang.Math.abs(tBondCFLOW.getDouble( "pv", i)) < 0.01 && tBondCFLOW.getDouble( "known_cflow", i) == 0.0 && tBondCFLOW.getDouble( "proj_cflow", i) == 0.0)
    			  tBondCFLOW.delRow( i);
    	  }

    	  /* copy everything back to master table */
    	  tBondCFLOW.copyRowAddAll( returnt);
      }
      /* ==== END OF BONDS & MM only ======== */
      if (tBondMTMResult != null)         tBondMTMResult.destroy();
      if (tBondCflowFutureResult != null) tBondCflowFutureResult.destroy();
      if (tBondCFLOW != null)             tBondCFLOW.destroy();
      if (tTemp != null)                  tTemp.destroy();
      
      returnt.select( tTranResult, "disc_idx (projindex), disc_idx (disc_index)", "deal_num EQ $dealnum AND deal_leg EQ $leg" );

      /* Add the historic cash so cumulative known cash includes it */
      returnt.select( tTranResult,
                    "deal_num (dealnum), deal_leg (leg), " + Integer.toString(PFOLIO_RESULT_TYPE.CFLOW_PRIOR_RESULT.toInt()) +" (known_cflow), disc_idx (projindex), disc_idx (disc_index)",
                    Integer.toString(PFOLIO_RESULT_TYPE.CFLOW_PRIOR_RESULT.toInt()) + " NE 0" );

      /* now add in the current cash so that known cash/cum known cash/cash includes it */
      tCurrCFLOW = Table.tableNew();
      
      if (tTranResult.getColNum( Integer.toString(PFOLIO_RESULT_TYPE.CFLOW_CURRENT_RESULT.toInt())) > 0)
      {
         tCurrCFLOW.select( tTranResult,
                    "deal_num (dealnum), deal_leg (leg), " + Integer.toString(PFOLIO_RESULT_TYPE.CFLOW_CURRENT_RESULT.toInt())+"(known_cflow), disc_idx (proj_index), disc_idx (disc_index),"+
                    Integer.toString(PFOLIO_RESULT_TYPE.CFLOW_CURRENT_RESULT.toInt())+"(df), disc_idx (cflow_date)",
                    Integer.toString(PFOLIO_RESULT_TYPE.CFLOW_CURRENT_RESULT.toInt()) + " NE 0.0" );
         
         int df_col = tCurrCFLOW.getColNum("df");
         int cflow_date_col = tCurrCFLOW.getColNum("cflow_date");
         int today = OCalendar.today();
         for ( i = 1; i <=  tCurrCFLOW.getNumRows(); i++)
   	     {
            tCurrCFLOW.setDouble(df_col, i, 1.0);
            tCurrCFLOW.setInt( cflow_date_col, i, today);
   	     }
   	     
         returnt.tuneGrowth( tCurrCFLOW.getNumRows());
         returnt.select( tCurrCFLOW,
                    "dealnum, leg, cflow_date, known_cflow, proj_index (projindex), disc_index, df",
                    "dealnum GT 0" );
      }

      if (tCurrCFLOW != null )            tCurrCFLOW.destroy();
      
      
      /* Fix up discount factors of 0.0 */
      fix_discount_factors(returnt);
      
      String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	  if (envVar != null)
	  {
		  envVar = envVar.toUpperCase();
	  
		  if (envVar.equals("TRUE"))
		  {   
			 returnt.clearGroupBy ();
			 returnt.addGroupBy ("dealnum");
			 returnt.addGroupBy ("leg");
			 returnt.addGroupBy ("ins_seq_num");
			 returnt.addGroupBy ("event_source");
			 returnt.groupBy ();
		  }
	  }
    
}

/*-------------------------------------------------------------------------------
Name:          format_result()
Description:   UDSR format function. (Default Formatting used)
Parameters:    Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void format_result(Table returnt) throws OException
{
   returnt.setColFormatAsRef( "projindex",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "event_source",       SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
   returnt.setColFormatAsDate("cflow_date",         DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "cflow_type",         SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
   returnt.setColFormatAsRef( "disc_index",         SHM_USR_TABLES_ENUM.INDEX_TABLE);
}

/*-------------------------------------------------------------------------------
Name:          create_tran_to_toolset_table
Description:   Prepare the tran-to-toolset table
Parameters:    Table tTransactions  
Return Values: Table   
-------------------------------------------------------------------------------*/
Table create_tran_to_toolset_table(Table tTransactions) throws OException
{
   Transaction TranPointer = null;
   int         iDealNum = 0;
   int         iToolset = 0;
   Table tToolsets = Table.tableNew("Tran To Toolset");
   
   tToolsets.addCols( "I(dealnum) I(toolset)");
	   
   int idealNumCol = tTransactions.getColNum( "deal_num");
   int itranPtrCol = tTransactions.getColNum( "tran_ptr");
	   
   int iNumRows = tTransactions.getNumRows();
   tToolsets.tuneGrowth(iNumRows); 
   tToolsets.addNumRows(iNumRows);

   for (int i = 1; i <= iNumRows; i++)
   {
      iDealNum    = tTransactions.getInt( idealNumCol, i);
      TranPointer = tTransactions.getTran( itranPtrCol, i);
      iToolset    = TranPointer.getFieldInt( TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "", -1);   

      tToolsets.setInt( 1, i, iDealNum);
      tToolsets.setInt( 2, i, iToolset);
   } 
   
   tToolsets.sortCol(1);
   
   return tToolsets;	
}
/*-------------------------------------------------------------------------------
Name:          delete_bond_and_mm
Description:    
Parameters:    Table tResult  
Return Values:   
-------------------------------------------------------------------------------*/
void delete_bond_and_mm(Table tResult) throws OException
{
	int i=0;
	int iToolsetCol = tResult.getColNum("toolset");
	int iEventSourceCol = tResult.getColNum("event_source");
	tResult.sortCol(iToolsetCol);
    int first_row = tResult.findInt( iToolsetCol, TOOLSET_ENUM.BOND_TOOLSET.toInt(), SEARCH_ENUM.FIRST_IN_GROUP);
    int last_row = tResult.findInt( iToolsetCol, TOOLSET_ENUM.BOND_TOOLSET.toInt(), SEARCH_ENUM.LAST_IN_GROUP);
    if ( first_row > 0 )
    {
       for (i=last_row; i >= first_row; i--)
       {
          if (tResult.getInt( iEventSourceCol, i) != EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt())
             tResult.delRow( i);
       }
    }
    first_row = tResult.findInt( iToolsetCol, TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt(), SEARCH_ENUM.FIRST_IN_GROUP);
    last_row = tResult.findInt( iToolsetCol, TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt(), SEARCH_ENUM.LAST_IN_GROUP);
    if ( first_row > 0 )
    {
       for (i=last_row; i >= first_row; i--)
       {
          if (tResult.getInt( iEventSourceCol, i) != EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt())
             tResult.delRow( i);
       }
    }
}
/*-------------------------------------------------------------------------------
Name:          fix_discount_factors
Description:   Fix up discount factors of 0.0  
Parameters:    Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void fix_discount_factors(Table returnt) throws OException
{
	double df, knownCflow, projCflow = 0.0;
	int iDFColID =       returnt.getColNum( "df");
    int iKnownCflowCol = returnt.getColNum( "known_cflow");
    int iProjCflowCol =  returnt.getColNum( "proj_cflow");
    int iAllCflowCol =   returnt.getColNum( "all_cflow");
    
    int iNumRows = returnt.getNumRows();
    
    for (int i = 1; i <= iNumRows; i++)
    {
       df = returnt.getDouble( iDFColID, i);
       if (df == 0.0)
       {
      	  df = 1.0;
          returnt.setDouble( iDFColID, i, df);
       }    
       
       knownCflow = returnt.getDouble(iKnownCflowCol, i);
       projCflow  = returnt.getDouble(iProjCflowCol, i);
       /* Divide by discount factor */
       returnt.setDouble(iKnownCflowCol, i, knownCflow/df);
       returnt.setDouble(iProjCflowCol, i, projCflow/df);
       /* now make sure that the known & cum are added together for the cash result */      
       returnt.setDouble(iAllCflowCol, i, (knownCflow/df) + (projCflow/df));
    }
}

}
