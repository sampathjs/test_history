/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_MTM_Notional.java

Date Of Last Revision:     30-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM MTM & Current Notional results.
 
                           Start out by getting PV_TOTAL_BY_LEG, PRICE_BY_LEG and SIZE_BY_LEG values
 
                           For FX options, PV_TOTAL_BY_LEG is not available, so get MTM 
                           from "MTM" result
                           Note that this will give us double or triple MTM for Bermudan
                           options where we get multiple entries per leg
  
                           Note for FX, Bonds etc. the PV_TOTAL_BY_LEG & SIZE_BY_LEG results
                           don't exist either. As such, we'll use the same approach as for 
                           FX Options (i.e. we'll take it from the MTM & Current Notional results)

                            
 */
package jvs.scripts;
import java.lang.Math;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_MTM_Notional implements IScript {
 

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
	   
	   Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   does the calculation for the APM Theta result.
	Parameters:    Table argt, Table returnt  
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   Table    tScenResult;      /* Not freeable */
	   Table    tTranResult;      /* Not freeable */
	   Table    tTranLegResult;   /* Not freeable */
	   Table    tGenResult;       /* Not freeable */
	   Table    tIndexEnrichment = null; /* Not freeable */
	   Table    tTransactions;    /* Not freeable */
	   Table    tDealsWithBlankLegResults, tNtnlStatus, tUnitStatus, tToRemove;
	   int         iRow, iNewRow, iLegNum;
	   int         iDealNum, iNumRows, iNumParams;
	   int         iToolset, iSettlementType, iUnit, iPricingLevel;
	   int         iIndexEnrichmentResultID;
	   int         iPVTotalByLegResultID;
	   int         iResultTypeCol;
	   boolean     useMTM;
	   Transaction TranPointer;

	   tScenResult = argt.getTable( "sim_results", 1);
	   tTranResult = tScenResult.getTable( 1, 1);
	   tTranLegResult = tScenResult.getTable( 1, 3);
	   tGenResult = tScenResult.getTable( 1, 4);

	   iIndexEnrichmentResultID = SimResult.getResultIdFromEnum("USER_RESULT_APM_INDEX_INFO");
	   iPVTotalByLegResultID = SimResult.getResultIdFromEnum("PV_TOTAL_BY_LEG_RESULT");

	   iResultTypeCol = tGenResult.getColNum("result_type");
	   iNumRows = tGenResult.getNumRows();
	   for(iRow=1; iRow<=iNumRows; iRow++)
	   {
	      if (tGenResult.getInt( iResultTypeCol, iRow) == iIndexEnrichmentResultID)
	      {       
	         tIndexEnrichment = tGenResult.getTable( "result", iRow);
	         break;
	      }
	   }   

	   if(Table.isTableValid(tIndexEnrichment) == 0)
	      return;


	   tTransactions = argt.getTable( "transactions",1);

	   returnt.addCols( "I(dealnum) I(projindex) I(disc_idx) I(leg) I(periodnum) I(is_official) I(unit)");
	   returnt.addCols( "F(mtm) F(notnl_raw) F(notnl) F(price) F(price_x_notnl) F(notnl_contract) I(index_unit)");
	   returnt.addCols( "F(index_density) F(index_contract_size) I(to_delete)");

	   /* Note that our MTM comes from PV_TOTAL_BY_LEG result, not the MTM/PV result.
	      Our notional comes from SIZE_BY_LEG, not CURRENT_NOTIONAL */
	   returnt.select( tTranLegResult, "deal_num (dealnum), deal_leg (leg), deal_pdc (periodnum), proj_idx (projindex), disc_idx, " + 
	                Str.intToStr(PFOLIO_RESULT_TYPE.PV_TOTAL_BY_LEG_RESULT.toInt()) + " (mtm), " + 
	                Str.intToStr(PFOLIO_RESULT_TYPE.PV_TOTAL_BY_LEG_RESULT.toInt()) + " (fv_mtm), " + 
	                Str.intToStr(PFOLIO_RESULT_TYPE.SIZE_BY_LEG_RESULT.toInt()) + " (notnl_raw), " + 
	                Str.intToStr(PFOLIO_RESULT_TYPE.PRICE_BY_LEG_RESULT.toInt()) + " (price), " + 
	                Str.intToStr(PFOLIO_RESULT_TYPE.DF_BY_LEG_RESULT.toInt()) + " (df) ", "deal_num GT 0");

	   returnt.sortCol( "dealnum");
	   
	   /* This table holds the details of any FX options, FX, Bonds etc. as these require special handling */
	   tDealsWithBlankLegResults = Table.tableNew("DealsWithBlankLegResults");
	   tDealsWithBlankLegResults.addCol( "dealnum", COL_TYPE_ENUM.COL_INT);

	   /* Holds the notional status of valid legs */
	   tNtnlStatus = Table.tableNew("tNtnlStatus");
	   tNtnlStatus.addCols( "I(dealnum) I(leg) I(is_official)");
       int dealnum_c = 1;
       int leg_c = 2;
       int is_official_c = 3;
       
	   /* Holds the unit of valid legs */
	   tUnitStatus = Table.tableNew("tUnitStatus");
	   tUnitStatus.addCols( "I(dealnum) I(leg) I(unit)");
       int unit_c = 3;
       
	   tUnitStatus.tuneGrowth( tTranResult.getNumRows());

	   /* Holds the details of rows that need to be removed from final result */
	   tToRemove = Table.tableNew("tToRemove");
	   tToRemove.addCols( "I(dealnum) I(leg) I(to_delete)");
	   int to_delete_c = 3;
	   
	   int deal_num_col = tTransactions.getColNum("deal_num");
	   int tran_ptr_col = tTransactions.getColNum("tran_ptr");
	   int dealnum_col = returnt.getColNum("dealnum");
	   // For each row...
	   iNumRows = tTransactions.getNumRows();
	   for (iRow = 1; iRow <= iNumRows; iRow++)
	   {
	      iDealNum    = tTransactions.getInt( deal_num_col, iRow);
	      TranPointer = tTransactions.getTran( tran_ptr_col, iRow);
	      iToolset = TranPointer.getFieldInt( TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "", -1);
	      iPricingLevel = TranPointer.getFieldInt( TRANF_FIELD.TRANF_PRICING_LEVEL.toInt(), 0, "", -1);

		  if (SimResult.isResultAllowedForTran(iPVTotalByLegResultID,TranPointer) == 0)
	      {
			 //Only add result if not valid so that it takes it from PV Result below
	         iNewRow = tDealsWithBlankLegResults.addRow();         
	         tDealsWithBlankLegResults.setInt( 1, iNewRow, iDealNum); /* it has one col "dealnum"*/          
	      }
		  
		  if (iToolset == TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() /* 6 */)
		  {
			// if there are no profiles then we should use the MTM result instead of PV Total by Leg
			useMTM = true;
			
			iNumParams = TranPointer.getNumParams();
			for (iLegNum = 0; iLegNum < iNumParams; iLegNum++)
			{
				if(TranPointer.getFieldInt( TRANF_FIELD.TRANF_PROFILE_NUMREC.toInt(), 0, "", -1) > 0)
				{
					useMTM = false;
					break;
				}
			}
			
			if(useMTM)
			{
				iNewRow = tDealsWithBlankLegResults.addRow(); 
				tDealsWithBlankLegResults.setInt( 1, iNewRow, iDealNum);     
			}		  		  
		  }

	      /* Now, need to figure out which sides of the deal we are interested in for "Notional" result
	         Any legs with physical settlement for commodity toolset
	         Only leg zero for all other toolsets */
	      if (iToolset == TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() /*36 */)
	      {
	    	 iNumParams = TranPointer.getNumParams();
	         for (iLegNum = 0; iLegNum < iNumParams; iLegNum++)
	         {
	            iSettlementType = TranPointer.getFieldInt( TRANF_FIELD.TRANF_SETTLEMENT_TYPE.toInt(), iLegNum, "", -1);
	            
	            if (iSettlementType == OPTION_SETTLEMENT_TYPE.SETTLEMENT_TYPE_PHYSICAL.toInt() /*2 */) /* If this leg is physically-settling */
	            {
	               iNewRow = tNtnlStatus.addRow();
	               
	               tNtnlStatus.setInt( dealnum_c, iNewRow, iDealNum);
	               tNtnlStatus.setInt( leg_c, iNewRow, iLegNum);                 
	            }
	         }

	         /* We want to remove leg zero for pricing level of zero (Location) for commodity deals, as it should have no MTM or notional value */
	         if (iPricingLevel == 0)
	         {
	            iNewRow = tToRemove.addRow();
	               
	            tToRemove.setInt( dealnum_c, iNewRow, iDealNum);
	            tToRemove.setInt( leg_c, iNewRow, 0);             
	         }
	      }
	      else
	      {
	          iNewRow = tNtnlStatus.addRow();          
	          tNtnlStatus.setInt( dealnum_c, iNewRow, iDealNum);
	          tNtnlStatus.setInt( leg_c, iNewRow, 0);
	      }

	      iNumParams = TranPointer.getNumParams();
	      for (iLegNum = 0; iLegNum < iNumParams; iLegNum++)
	      {
	         iUnit = TranPointer.getFieldInt( TRANF_FIELD.TRANF_UNIT.toInt(), iLegNum, "", -1);

	         iNewRow = tUnitStatus.addRow();
	               
	         tUnitStatus.setInt( dealnum_c, iNewRow, iDealNum);
	         tUnitStatus.setInt( leg_c, iNewRow, iLegNum);
	         tUnitStatus.setInt( unit_c, iNewRow, iUnit);
	      }
	   }

	   /* For FX options, FX, Bonds etc. take the MTM from PV result, since PV_TOTAL_BY_LEG is blank
	      Also take the notional from the PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT */
	   if (tDealsWithBlankLegResults.getNumRows() > 0)
	   {      
		  tDealsWithBlankLegResults.deleteWhere("dealnum", returnt, "dealnum");

		  tDealsWithBlankLegResults.select( tTranResult, "deal_leg (leg), " + Str.intToStr(PFOLIO_RESULT_TYPE.PV_RESULT.toInt()) + " (mtm), " +
	                   Str.intToStr(PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT.toInt()) + " (notnl_raw), proj_idx (projindex), disc_idx", "deal_num EQ $dealnum");
	      returnt.select( tDealsWithBlankLegResults, "dealnum, leg, mtm, notnl_raw, projindex, disc_idx", "dealnum GT 0");
	   }

	   if (tNtnlStatus.getNumRows() > 0)
	   {
	      tNtnlStatus.setColValInt( is_official_c, 1);
	      returnt.select( tNtnlStatus, "is_official", "dealnum EQ $dealnum AND leg EQ $leg");
	   }

	   if (tUnitStatus.getNumRows() > 0)
	   {
	      returnt.select( tUnitStatus, "unit", "dealnum EQ $dealnum AND leg EQ $leg");
	   }

	   /* Now remove any rows that should be removed - fake leg zero on COMMODITY toolset, for example */
	   if (tToRemove.getNumRows() > 0)
	   {
		  tToRemove.setColValInt( "to_delete", 1);
	      returnt.select( tToRemove, "to_delete", "dealnum EQ $dealnum AND leg EQ $leg");
	      returnt.deleteWhereValue( "to_delete", 1);
	   }

	   tDealsWithBlankLegResults.destroy();
	   tNtnlStatus.destroy();
	   tUnitStatus.destroy();
	   tToRemove.destroy();

	   /* Now, convert the notional to contract equivalents */
	   returnt.select( tIndexEnrichment, "unit (index_unit), density_adjustment (index_density), contract_size (index_contract_size)", "index_id EQ $projindex");
	   
       fnc_convert_notional_to_contracts(returnt);
	   
	   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	   if (envVar != null)
	   {
			envVar = envVar.toUpperCase();
		  
			if (envVar.equals("TRUE"))
			{   
			  returnt.clearGroupBy ();
			  returnt.addGroupBy ("dealnum");
			  returnt.addGroupBy ("leg");
			  returnt.addGroupBy ("periodnum");
			  returnt.groupBy ();
			}
	   }         
    
	}
	/*-------------------------------------------------------------------------------
	Name:          fnc_convert_notional_to_contracts()
	Description:   does the conversion to notional contracts.
	Parameters:    Table returnt  
	Return Values:   
	-------------------------------------------------------------------------------*/
	void fnc_convert_notional_to_contracts(Table returnt) throws OException
	{
		double val, density, contractSize, df;
		int fromUnit, toUnit;
		
		int notnl_raw_col = returnt.getColNum("notnl_raw");
		int is_official_col = returnt.getColNum("is_official");
		int notnl_col = returnt.getColNum("notnl");
		int price_col = returnt.getColNum("price");
		int price_x_notnl_col = returnt.getColNum("price_x_notnl");
		int fv_mtm_col = returnt.getColNum("fv_mtm");
		int df_col = returnt.getColNum("df");
		int unit_col = returnt.getColNum("unit");
		int index_unit_col = returnt.getColNum("index_unit");
		int index_density_col = returnt.getColNum("index_density");
		int index_contract_size_col = returnt.getColNum("index_contract_size");
		int notnl_contract_col = returnt.getColNum("notnl_contract");
		int iNumRows = returnt.getNumRows();
		for (int iRow = 1; iRow <= iNumRows; iRow++)  
		{
		   val = returnt.getDouble(notnl_raw_col, iRow) * returnt.getInt(is_official_col, iRow);
		   returnt.setDouble( notnl_col, iRow, val);
		   returnt.setDouble( price_x_notnl_col, iRow, returnt.getDouble(notnl_raw_col, iRow) * returnt.getDouble(price_col, iRow));
		   df = returnt.getDouble(df_col, iRow);
		   if (df == 0.0)
			  returnt.setDouble( fv_mtm_col, iRow, 0.0);
		   else 
			   returnt.setDouble( fv_mtm_col, iRow, returnt.getDouble(fv_mtm_col, iRow) / df); 
		   
		   fromUnit = returnt.getInt(unit_col, iRow);
		   toUnit = returnt.getInt(index_unit_col, iRow);
		   density = returnt.getDouble(index_density_col, iRow);
		   contractSize = returnt.getDouble(index_contract_size_col, iRow);

		   val = val * Transaction.getUnitConversionFactor(fromUnit, toUnit);

		      /* Check in case an un-configured index has a contract size of zero */
		      if (contractSize > 0.00001)
		      {
		         val = val / contractSize;
		      }

		      /* Skip boring density values such as 0.0 and 1.0 */
		      if ((Math.abs(density) > 0.00001) && (Math.abs(density-1.0) > 0.00001))
		      {
		         if ((Util.utilGetUnitTypeFromUnit(fromUnit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_VOLUME.toInt()) && 
		             (Util.utilGetUnitTypeFromUnit(toUnit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_MASS.toInt()))
		         {
		            val = val * density;
		         }
		         else if ((Util.utilGetUnitTypeFromUnit(fromUnit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_MASS.toInt()) && 
		                  (Util.utilGetUnitTypeFromUnit(toUnit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_VOLUME.toInt()))
		         {
		            val = val / density;
		         }         
		      }

		      returnt.setDouble( notnl_contract_col, iRow, val);
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
	   returnt.setColFormatAsRef( "projindex",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsRef( "disc_idx",           SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsRef( "is_official",        SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE);

	   returnt.setColFormatAsRef( "unit",               SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
	   returnt.setColFormatAsRef( "index_unit",         SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);


	   returnt.setColHideStatus( returnt.getColNum( "to_delete"), 0);
	}


	}
