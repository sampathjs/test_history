/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_Delta.java

Date Of Last Revision:     10-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM Delta result.
                             
 */
package  com.olf.result;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.result.APMUtility.APMUtility; 
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class APM_UDSR_Delta implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
   
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
	   else if (operation == USER_RESULT_OPERATIONS.USER_RES_OP_DWEXTRACT.toInt())
		   OConsole.oprint( "\nDWEXTRACT operation is not coded");
	   
	   Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   does the calculation for the APM Delta result.
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   Table    tScenResult = null;      /* Not freeable */
	   Table    tGenResult = null;       /* Not freeable */
	   Table    tDeltaResult = null;     /* Not freeable */
	   Table    tDeltaResults = null;    /* Not freeable */
	   Table    tDFResults = null;       /* Not freeable */
	   Table    tIndexToPowerProduct = null;    
	   boolean bExpAllDeltaVal = false;
	   int  iAttributeGroup = 0;
	   
	   Table tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));
	   if (tblAttributeGroups.getNumRows() > 0)
		   iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
	   Table tblConfig = SimResult.getResultConfig(iAttributeGroup);
	   tblConfig.sortCol("res_attr_name");
	    
	   if (APMUtility.ParamHasValue(tblConfig, "Tran Gpt Delta by Leg Format Config") > 0) 
	   {
		   if (APMUtility.GetParamStrValue(tblConfig,"Tran Gpt Delta by Leg Format Config").matches("Expand all Delta Values"))
			   bExpAllDeltaVal = true;
	   }	
	    
	   /* Get Tran Gpt Delta By Leg Result */ 
	   tScenResult = argt.getTable( "sim_results", 1);
	   tGenResult = tScenResult.getTable( 1, 4);
	   tDeltaResults = SimResult.getGenResultTables(tGenResult, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_LEG_RESULT.toInt()); /*201*/
	   	 
	   if (tDeltaResults.getNumRows() == 0)
	   {
	      return;
	   }
	   else
	   {
	      tDeltaResult = tDeltaResults.getTable( 1, 1);
	   }

	   /* Only interested in PRICING_MODEL_ENUM.discounting index and DF factor; take from Tran Leg results */
	   tDFResults = tScenResult.getTable( 1, 3);

	   /* Now need to transfer over relevant fields */
	   returnt.addCols( "I(deal_num) I(deal_leg) I(deal_pdc) I(ins_type) I(index)");
	   returnt.addCols( "I(gpt_id) I(date) F(mtm_from_index_changes) I(disc_idx) I(pwr_product_id)");
	   returnt.addCols( "F(df) F(df_by_leg) F(delta) F(fv_delta) F(gamma)");
	   returnt.addCols( "F(fv_gamma) F(contract_delta) F(fv_contract_delta) F(contract_gamma) F(fv_contract_gamma)");
	   returnt.addCols( "I(event_source) I(ins_seq_num) I(ins_source_id) I(pymt_date)" );
	   
	   // Skip all else, if no delta (e.g. all deals in the past)
	   if (tDeltaResult.getNumRows() == 0)
	   {
	      return;
	   }

           returnt.setColTitle("ins_seq_num", "Instrument\nSeq\nNum");
           returnt.setColTitle("event_source", "Event\nSource");
           returnt.setColTitle("ins_source_id", "Instrument\nSource\nID");

	   /* Set the initial delta values */
	   returnt.tuneGrowth( tDeltaResult.getNumRows());
	   if (bExpAllDeltaVal)
  		   returnt.select( tDeltaResult, "deal_num, deal_leg, deal_pdc, index, gpt_id, delta, gamma, event_source, ins_seq_num, ins_source_id", "index GT 0");
  	   else
           {
		   returnt.select( tDeltaResult, "deal_num, deal_leg, deal_pdc, index, gpt_id, delta, gamma, ins_type", "index GT 0");
		   returnt.colHide("ins_seq_num");
		   returnt.colHide("event_source");
		   returnt.colHide("ins_source_id");
           }

	   /* For each index/gridpoint, identify date */
	   identify_gpt_date(returnt, tDeltaResult, bExpAllDeltaVal);
	   
	   /* For all indices, store the valuation product id (if any) */
	   
	   tIndexToPowerProduct = APM_GetPowerProductTable();
	   returnt.select( tIndexToPowerProduct, "valuation_product_id(pwr_product_id)", "index_id EQ $index");
	   
	   /* Get the PRICING_MODEL_ENUM.discounting index and DF for the deal */
	   /* Discounting index is used by FX delta calculation; it needs disc_idx to decide whether to convert between */
	   returnt.select( tDFResults, "disc_idx, " + Integer.toString(PFOLIO_RESULT_TYPE.DF_BY_LEG_RESULT.toInt())+ "(df_by_leg), ins_type", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg AND deal_pdc EQ $deal_pdc");

	   /* Convert delta/gamma to the contract versions via this function */
	   Index.tableColNotnlToNumContracts(returnt, "index", "delta", "contract_delta");
	   Index.tableColNotnlToNumContracts(returnt, "index", "gamma", "contract_gamma");

	   /* Divide by discount factor from discount index for that date to get FV deltas (standard and contract) */
	   Index.tableColIndexDateToDF(returnt, "disc_idx", "date", "df");

           if (bExpAllDeltaVal)  /*  for expanded delta, use payment date for df_by_leg.  */
               Index.tableColIndexDateToDF(returnt, "disc_idx", "pymt_date", "df_by_leg");
	   
	   /* Fix up the discount factor for toolsets which do not have DF_BY_LEG working properly   
	      This is ComFut in older Endur cuts, where it was used for both futures and forwards, but DF_BY_LEG wasn't working
	      The futures settle daily, so their PV Delta is same as FV Delta, so set DF to 1.0; the forwards are discounted
	      to present-day value, so if DF_BY_LEG does not work, need to take the value from DF and divide by it to arrive at FV
	      This is also ComFwd toolset, and possibly other toolsets for which DF_BY_LEG does not work, so needs to be used generally
       */
	   fix_discount_factor(returnt);
	}

	
	/*-------------------------------------------------------------------------------
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
	   returnt.setColFormatAsRef( "index",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsDate( "date",           DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsRef( "disc_idx",       SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsRef( "ins_type",       SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
	   returnt.setColFormatAsRef( "pwr_product_id", SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);
	   returnt.setColFormatAsRef( "event_source",   SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
       returnt.colHide("pymt_date");
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_GetBaseinsForIns()
	Description:   Gets the base instrument type for an ins type

	In V80R1C Instrument.getBaseInsType does not exist.  This fn replicates that functionality.

	Return Values: Base ins type
	-------------------------------------------------------------------------------*/
	int APM_GetBaseInsType(int ins_type) throws OException
	{
	   Table user_instruments;
	   int   retval, row;

	   if (ins_type < 1000000)
	       return ins_type;

	   user_instruments = Table.getCachedTable ("APM_BaseInsTypes");
	   if(Table.isTableValid(user_instruments) != 0)
	   {
	      if ( (row = user_instruments.findInt( 1, ins_type, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0 )
	      {
	         Table.destroyCachedTable("APM_BaseInsTypes");
	         user_instruments = null;
	      }
	      else
	         return user_instruments.getInt( 2, row);
	   }

	   user_instruments = Table.tableNew();
	   retval = APM_TABLE_LoadFromDbWithSQL(user_instruments, "id_number, base_ins_id", "instruments", "id_number >= 1000000");

	   if ( retval == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() && Table.isTableValid(user_instruments)==1 )
	   {
	      user_instruments.sortCol( "id_number");
	      Table.cacheTable("APM_BaseInsTypes", user_instruments);

	      if ( (row = user_instruments.findInt( 1, ins_type, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0 )
	         return ins_type;

	      return user_instruments.getInt( 2, row);
	   }
	   else
	      return ins_type;
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
        	// for error reporting further down
        	String message = null;
        	
            try {
                // db call
            	iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
            } catch (OException exception) {
                iRetVal = exception.getOlfReturnCode().toInt();
                
                message = exception.getMessage();
            } finally {
                if (iRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
                    numberOfRetriesThusFar++;

                    if(message == null) {
                        message = String.format("Query execution retry %1$d of %2$d. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES);
                    } else {
                        message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES, message);
                    }
                    
                    OConsole.oprint(message);
                    
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
	Name:          identify_gpt_date()
	Description:   For each index/gridpoint, identify date
    Parameters:    Table returnt, Table tDeltaResult
	Return Values: 
	-------------------------------------------------------------------------------*/
	void identify_gpt_date(Table returnt, Table tDeltaResult, boolean bExpAllDeltaVal) throws OException
	{
		int      iRow, iRow2 = 0;
	    int      iNumRows = 0;
	    int      iNumRows2 = 0;
		int      iIndex = 0;
	    int      indexCol = 0;
	    int      curveDateCol = 0;
	    int      curveDateIntCol = 0;
		Table    tIndexData = null;
		Table tIndices = Table.tableNew();
		tIndices.select( tDeltaResult, "DISTINCT, index", "index GT 0");

		iNumRows = tIndices.getNumRows();
		   
		for (iRow = 1; iRow <= iNumRows; iRow++)
		{ 
		   iIndex = tIndices.getInt( 1, iRow);
		   tIndexData = Index.loadAllGpts(iIndex); 

		   tIndexData.addCols( "I(index) I (curve_date_int)");
		      
		   curveDateIntCol = tIndexData.getColNum("curve_date_int"); 
		   curveDateCol = tIndexData.getColNum("curve_date"); 
		   indexCol = tIndexData.getColNum("index"); 
		      
		   iNumRows2 = tIndexData.getNumRows();	      
		   for (iRow2 = 1; iRow2 <= iNumRows2; iRow2++)
		   {
		  	  tIndexData.setInt( indexCol, iRow2, iIndex);
	          tIndexData.setInt( curveDateIntCol, iRow2, ODateTime.strDateTimeToDate(tIndexData.getString( curveDateCol, iRow2)));
		   }     

           /* Using curve_date_int, as curve_date is given as a String */
	       returnt.select( tIndexData, "curve_date_int(date)", "index EQ $index AND id EQ $gpt_id");

	       if (bExpAllDeltaVal)    /*  for expanded delta, use payment date for non-profile event_source. */
	       {
	    	   returnt.select( tDeltaResult, "payment_date(pymt_date)",
	    		   "deal_num EQ $deal_num AND deal_leg EQ $deal_leg AND ins_seq_num EQ $ins_seq_num AND index EQ $index AND gpt_id EQ $gpt_id AND ins_source_id EQ $ins_source_id AND event_source NE "+EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt());
	       }
	       tIndexData.destroy();
	    }
	    tIndices.destroy();
	}
	
	/*-------------------------------------------------------------------------------
	Name:          fix_discount_factor()
	Description:   Fix up the discount factor for toolsets which do not have DF_BY_LEG working properly 
	Parameters:    Table returnt
	Return Values: Table of the cached table
	-------------------------------------------------------------------------------*/
	void fix_discount_factor(Table returnt) throws OException
	{
		double   df = 0.0;
	    double   df_by_leg = 0.0;
		int iInsType = 0;
		int insTypeCol = returnt.getColNum("ins_type");
		int dfCol = returnt.getColNum("df");
		int dfByLegCol = returnt.getColNum("df_by_leg");
		int deltaCol = returnt.getColNum("delta");
		int gammaCol = returnt.getColNum("gamma");
		int fvDeltaCol = returnt.getColNum("fv_delta");
		int fvGammaCol = returnt.getColNum("fv_gamma");
		int contractDeltaCol = returnt.getColNum("contract_delta");
		int contractGammaCol = returnt.getColNum("contract_gamma");
		int fvContractDeltaCol = returnt.getColNum("fv_contract_delta");
		int fvContractGammaCol = returnt.getColNum("fv_contract_gamma");
                int iToolset = 0;
                boolean discountExchangeForwards = isDiscountExchangeForwardsEnabled();
		   
		int iNumRows  = returnt.getNumRows();
		for (int iRow = 1; iRow <= iNumRows ; iRow++)
		{
		    iInsType = APM_GetBaseInsType(returnt.getInt( insTypeCol, iRow));
                    iToolset = Ref.getToolsetFromInsType(iInsType);

                    if (iToolset == TOOLSET_ENUM.BOND_TOOLSET.toInt())
                    {
                        df = 0.0;
                        df_by_leg = 0.0;
                        returnt.setDouble(dfByLegCol, iRow, df);
                        returnt.setDouble(dfCol, iRow, df_by_leg);
                    }
		    /* This condition matches the internal Endur logic for DF_BY_LEG
		       ones in brackets are types that don;t exist in V80R1 */
		    else if ( iInsType == INS_TYPE_ENUM.energy_exch_future.toInt() || iInsType == INS_TYPE_ENUM.mtl_exch_future.toInt()        ||
		         iInsType == INS_TYPE_ENUM.soft_exch_future.toInt()   || iInsType == INS_TYPE_ENUM.energy_exch_avg_future.toInt() ||
		         iInsType == INS_TYPE_ENUM.prec_exch_future.toInt()   || iInsType == INS_TYPE_ENUM.energy_exch_spd_future.toInt() || 
		         iInsType == INS_TYPE_ENUM.power_exch_future.toInt()  || iInsType == INS_TYPE_ENUM.power_exch_bom_future.toInt()  /* INS_TYPE_ENUM.power_exch_bom_forward */ ||
		         iInsType == INS_TYPE_ENUM.metal_exch_avg_future.toInt() || iInsType == INS_TYPE_ENUM.energy_phy_inv.toInt())
		    {
		      	 df = 1.0;
		      	 df_by_leg = 1.0;
		         returnt.setDouble( dfByLegCol, iRow, df);
		         returnt.setDouble( dfCol, iRow, df_by_leg); /* Do this so that FV contract delta works too */
                    }
                    else if (iToolset != TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() && iToolset != TOOLSET_ENUM.STANDARD_PRODUCTS_TOOLSET.toInt())
                    {
                        df = returnt.getDouble(dfCol, iRow);
		        df_by_leg = returnt.getDouble(dfByLegCol, iRow);
                        if (df_by_leg == 0.0 && df != 0.0)
                        {
                           returnt.setDouble(dfByLegCol, iRow, df);
                           df_by_leg = df;
                        }
                    }
             else 
	         {
                     /* if we are in the else clause then we must be a fwd instrument */

                     if (!discountExchangeForwards)
                     {
                        df = 1.0;
                        df_by_leg = 1.0;
                        returnt.setDouble(dfByLegCol, iRow, df);
                        returnt.setDouble(dfCol, iRow, df_by_leg);
                     }

	        	 df = returnt.getDouble( dfCol, iRow);
		         df_by_leg = returnt.getDouble( dfByLegCol, iRow);
	        	 if (df_by_leg == 0.0 && df != 0.0)
	        	 {
	                returnt.setDouble( dfByLegCol, iRow, df);
	                df_by_leg = df;
	        	 }
	          }
		         
	          /* Divide contract delta by its own, generated, discount factor */
	          if (df != 0.0)
	          {
		  	     returnt.setDouble( fvContractDeltaCol, iRow, returnt.getDouble( contractDeltaCol, iRow)/df);
		  	     returnt.setDouble( fvContractGammaCol, iRow, returnt.getDouble( contractGammaCol, iRow)/df);
	          }
	   	     /* Divide delta and gamma values by the DF_BY_LEG discount factor
		        This way, FV deltas for non-derivative products will match notional value, as expected */
		      if (df_by_leg != 0.0)
		      {
		  	     returnt.setDouble( fvDeltaCol, iRow, returnt.getDouble( deltaCol, iRow)/df_by_leg);
		  	     returnt.setDouble( fvGammaCol, iRow, returnt.getDouble( gammaCol, iRow)/df_by_leg);
		      }
		 }
	}
	
	/*-------------------------------------------------------------------------------
	Name:          APM_GetPowerProductTable()
	Description:   Gets a cached table which contains the Index to Power Product Id mapping...

	Return Values: Table of the cached table
	-------------------------------------------------------------------------------*/
	Table APM_GetPowerProductTable() throws OException
	{
	   Table powerIdxToProductTable;
	   Table cachedTable = Table.getCachedTable( "indexToPowerProductTable" );
	   
	   if(Table.isTableValid( cachedTable ) == 0)
	   {
	      int i = 0;
	      int iNumRows = 0;
	      int cachedTableIndexCol = 0;
	      int powerTableIndexCol = 0;
	      int valuationProductIdCol = 0;
	      int productIdCol = 0;
	      
	      /* Load the pwr_pricing_config table. */
	      cachedTable = Table.tableNew("indexToPowerProductTable");
	      APM_TABLE_LoadFromDbWithSQL( cachedTable, "index_id, valuation_product_id", "pwr_pricing_config", "1=1" );
	      cachedTableIndexCol = cachedTable.getColNum("index_id");
	      cachedTable.sortCol( cachedTableIndexCol);

	      /* Load the USER_PowerIdxToProduct table */
	      powerIdxToProductTable = Table.tableNew("powerIdxToProductTable");
	      APM_TABLE_LoadFromDbWithSQL( powerIdxToProductTable, "*", "USER_PowerIdxToProduct", "1=1" );
	      powerTableIndexCol = powerIdxToProductTable.getColNum("index_id");
	      powerIdxToProductTable.sortCol( powerTableIndexCol );
	   
	      valuationProductIdCol = cachedTable.getColNum("valuation_product_id");
	      productIdCol = powerIdxToProductTable.getColNum( "product_id");
	      
	      iNumRows = powerIdxToProductTable.getNumRows();
	      for ( i = 1; i <= iNumRows ; i++ )
	      {
			 if ( cachedTable.unsortedFindInt( cachedTableIndexCol, powerIdxToProductTable.getInt( powerTableIndexCol, i ) ) > 0 )
			 {
			    /* Prefer the index from pwr_pricing_config if found... */
			    continue;
			 }
			 else
			 {
			    int row = cachedTable.addRow();
			    cachedTable.setInt( cachedTableIndexCol, row, powerIdxToProductTable.getInt( powerTableIndexCol, i ) );
			    cachedTable.setInt( valuationProductIdCol, row, powerIdxToProductTable.getInt( productIdCol, i ) );
			 }
	      }
	      
	      cachedTable.sortCol( cachedTableIndexCol);
	   
	      /* Cache the results and clean up. */
	      Table.cacheTable( "indexToPowerProductTable", cachedTable );
	      powerIdxToProductTable.destroy();      
	   }
	   
	   return ( cachedTable );
	}

        /*-------------------------------------------------------------------------------
        Name:          isDiscountExchangeForwardsEnabled()
        Description:   Check the environment variable AB_DISCOUNT_EXCHG_FWDS.
        
        Return Values: true or false.
        -------------------------------------------------------------------------------*/
        boolean isDiscountExchangeForwardsEnabled() throws OException
        {
           boolean discExchFwd = false;
        
           String envVar = Str.toUpper(Util.getEnv("AB_DISCOUNT_EXCHG_FWDS"));
           if ((Str.isEmpty(envVar) == 0) && (Str.equal(envVar, "TRUE") == 1))
              discExchFwd = true;
        
           return discExchFwd;
           
        }
        

	}
