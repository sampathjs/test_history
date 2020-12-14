/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_ResetDatesInfo.java 

Date Of Last Revision:     10-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back deal leg-level information from core db tables.
                           It doesn't use any current sim results but is just used to enrich sim data for filtering and bucketing.

                            
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_ResetDatesInfo implements IScript {
  
/*-------------------------------------------------------------------------------
Name:          main()
Description:   deal info UDSR Main
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
	      compute_result_using_db(argt, returnt);
	   else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
	      format_result(returnt);

	   Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result_using_db()
	Description:   deal info result using core db tables.
	Parameters:    Table argt, Table returnt  
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result_using_db(Table argt, Table returnt) throws OException
	{
	   Table    tTranNums;        /* Not freeable */
	   Table    tSimDef;          /* Not freeable */
	   Table    tSupplementary;
	   int      iQueryId=0;
	   int      iRetVal;
	   int      iToolset;
	   int      row, myLeg, myDeal, nextDeal, pricingLevel;
	   String   sRequest;

	   tTranNums   = argt.getTable( "transactions", 1);
	   tSimDef     = argt.getTable( "sim_def", 1);
	        
	   /* If query ID is provided as a parameter, use it! */
	   if (tSimDef.getColNum( "APM Single Deal Query") > 0)
	      iQueryId = tSimDef.getInt( "APM Single Deal Query", 1);

	   /* If query ID was not set or left at zero, create a query ID from the list of transactions */
	   if (iQueryId == 0)
	   {  
	      /* build up query result to get deals from ab.tran which match our sim result. */
	      iQueryId = APM_TABLE_QueryInsertN(tTranNums, "tran_num");
	   }

	   sRequest =
		         " select " + 
	          " ab.deal_tracking_num dealnum, ab.toolset, pa.param_seq_num leg, pa.start_date leg_start, pa.mat_date leg_end, pa.start_date oil_date_start, pa.mat_date oil_date_end, 0 reset_start, 0 reset_end " +         
	          " from " + 
	          " ab_tran ab, ins_parameter pa, query_result qr " +
	          " where " + 
	          " pa.ins_num = ab.ins_num AND " + 
		         " qr.query_result = ab.tran_num AND " + 
	          " qr.unique_id = " + iQueryId;

	   iRetVal = DBase.runSql(sRequest);
	   iRetVal = DBase.createTableOfQueryResults(returnt);

	   returnt.colConvertDateTimeToInt( "leg_start");
	   returnt.colConvertDateTimeToInt( "leg_end");

	   returnt.colConvertDateTimeToInt( "oil_date_start");
	   returnt.colConvertDateTimeToInt( "oil_date_end");

	   tSupplementary = Table.tableNew("Reset Dates");

	   sRequest =
			   " select  ab_tran.deal_tracking_num dealnum, ab_tran.toolset, reset.param_seq_num leg, phys_header.pricing_level , min(reset.reset_date) reset_start, max(reset.reset_date) reset_end" +    
	          " from query_result, ab_tran  "	  + 
	          " left join reset on reset.ins_num = ab_tran.ins_num   " +
	          " left join phys_header on ab_tran.ins_num = phys_header.ins_num   " +
	          " where query_result.query_result = ab_tran.tran_num and query_result.unique_id = " + iQueryId + 
	          " group by ab_tran.deal_tracking_num, ab_tran.toolset, reset.param_seq_num, phys_header.pricing_level";
	   
	   iRetVal = DBase.runSql(sRequest);
	   iRetVal = DBase.createTableOfQueryResults(tSupplementary);
	   
	   tSupplementary.colConvertDateTimeToInt( "reset_start");
	   tSupplementary.colConvertDateTimeToInt( "reset_end");

	   returnt.select( tSupplementary, "reset_start, reset_end, pricing_level", "dealnum EQ $dealnum AND leg EQ $leg");
	   returnt.select( tSupplementary, "reset_start (oil_date_start), reset_end (oil_date_end)", "dealnum EQ $dealnum AND leg EQ $leg AND toolset EQ " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt());
	   returnt.select( tSupplementary, "reset_start (oil_date_start), reset_end (oil_date_end)", "dealnum EQ $dealnum AND leg EQ $leg AND toolset EQ " + TOOLSET_ENUM.SWAP_TOOLSET.toInt());
	   
	   returnt.setColFormatAsDate( "reset_start");
	   returnt.setColFormatAsDate( "reset_end");
	   
	   returnt.colHide("pricing_level"); 

	   returnt.setColFormatAsRef("toolset", SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);

	   
	   returnt.addGroupBy( "dealnum");   
	   returnt.addGroupBy( "leg");

	   returnt.groupBy();
	   

	   /* Now fix up the deal reset dates on Commodity toolset for fake leg zero */
	   int dealnum_col = returnt.getColNum("dealnum");
	   int leg_col = returnt.getColNum("leg");
	   int toolset_col = returnt.getColNum("toolset");
	   int reset_start_col = returnt.getColNum("reset_start");
	   int reset_end_col = returnt.getColNum("reset_end");
	   int pricing_level_col = returnt.getColNum("pricing_level");
	   
	   for (row = 1; row <= returnt.getNumRows(); row++)
	   {
	      pricingLevel = returnt.getInt( pricing_level_col, row);
	      iToolset = returnt.getInt( toolset_col, row);
	      
	      if(pricingLevel == PRICING_LEVEL_ENUM.PRICING_LEVEL_DEAL.toInt() && iToolset == TOOLSET_ENUM.COMMODITY_TOOLSET.toInt())
		  continue;

	      myDeal = returnt.getInt( dealnum_col, row);
	      myLeg = returnt.getInt( leg_col, row);     
	      
	      if (row < returnt.getNumRows())
	      {
	         nextDeal = returnt.getInt( dealnum_col, row+1);
	      }
	      else
	      {
	         nextDeal = -1;
	      }
	        

	      if ((iToolset == TOOLSET_ENUM.COMMODITY_TOOLSET.toInt()) && (myLeg == 0) && (nextDeal == myDeal))
	      {
	         /* Enrich with values from leg 1 (next row) */
	         returnt.setInt( reset_start_col, row, returnt.getInt( reset_start_col, row+1));         
	         returnt.setInt( reset_end_col, row, returnt.getInt( reset_end_col, row+1));
	      }
	   }
	   

	   /* If query ID is provided as a parameter, somebody else should free it */
	   if ((tSimDef.getColNum( "APM Single Deal Query") < 1) || (tSimDef.getInt( "APM Single Deal Query", 1) == 0))
	   {
	      Query.clear(iQueryId);
	   }
	   
	   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	   if (envVar != null)
	   {
			envVar = envVar.toUpperCase();
		  
			if (envVar.equals("TRUE"))
			{   
				returnt.clearGroupBy ();
				returnt.addGroupBy ("dealnum");
				returnt.addGroupBy ("leg");
				returnt.groupBy ();
			}
	   }  	   

	}

	/*-------------------------------------------------------------------------------
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
		
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_LoadFromDBWithSQL
	Description:   deadlock protected version of the fn
	Parameters:      As per TABLE_LoadFromDBWithSQL
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int APM_TABLE_LoadFromDbWithSQL(Table table, String what, String from, String where) throws OException
	{
        final int nAttempts = 10;

        int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

        int numberOfRetriesThusFar = 0;
        do {
            try {
                // db call
            	iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
            } catch (OException exception) {
                iRetVal = exception.getOlfReturnCode().toInt();
            } finally {
                if (iRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
                    numberOfRetriesThusFar++;

                    Debug.sleep(numberOfRetriesThusFar * 1000);
                } else {
                    // it's not a retryable error, so leave
                    break;
                }
            }
        } while (numberOfRetriesThusFar < nAttempts);

        if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
        	OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed " ) );
		
	   return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_QueryInsertN
	Description:   Insert a range of values from a table as a new query result.
	Parameters:    
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int APM_TABLE_QueryInsertN( Table tTable, String sColumn ) throws OException
	{
        final int nAttempts = 10;

        int iQueryId = 0;
        int numberOfRetriesThusFar = 0;
        do {
            try {
            	iQueryId = Query.tableQueryInsert( tTable, sColumn );
            } catch (OException exception) {
            	OLF_RETURN_CODE olfReturnCode = exception.getOlfReturnCode();
            	
            	if(olfReturnCode == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR) {
            		numberOfRetriesThusFar++;
            	} else {
            		break;
            	}
            }
        } while (iQueryId == 0 && numberOfRetriesThusFar < nAttempts);

	   return iQueryId;
	}
}
