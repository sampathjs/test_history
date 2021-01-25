/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_Theta.java 

Date Of Last Revision:     31-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM Theta result.
                           The PFOLIO_RESULT_TYPE.THETA_BY_LEG_RESULT is taken and multiplied by the PFOLIO_RESULT_TYPE.SIZE_BY_LEG_RESULT.
                           For deals like FX, Bonds etc. where the by leg results are blank, 
                           theta is taken from the PFOLIO_RESULT_TYPE.THETA_RESULT.
                            
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_Theta implements IScript {
   
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
	   Table    tGenResult;       /* Not freeable */
	   Table    tLegResult;       /* Not freeable */
	   Table    tAPMDealInfoResult=null; /* Not freeable */
	   Table    tTransactions;      /* Not freeable */
	   Table    tDealsWithBlankLegResults;
	   int         iScenId;
	   int         iThetaCol;
	   int         iSizeCol;
	   int         iPymntCol;
	   int         iSortCol;
	   int         iDealNumCol;
	   int         iTranDealNumCol;
	   int         iRow;
	   int         iAPMDealInfoResultID;
	   int         iDealNum;
	   int         iNewRow;
	   int         iNumRows;
	   int 		   iToolset;
	   double      dTheta;
	   Table    tSortedLegResult;	/* Freeable */
	   Table 	tLegResultCopy = null;
	   Table 	tApmDealInfoResultCopy = null;
	   Boolean 	   qaMode = false;

	   /* Get Theta Result */
	   iScenId = argt.getInt( "scen_id", 1);

	   tScenResult = argt.getTable( "sim_results", 1);
	   tTranResult = tScenResult.getTable( 1, 1);
	   tLegResult = tScenResult.getTable( 1, 3);
	   tGenResult = tScenResult.getTable( 1, 4);
	   tTransactions = argt.getTable( "transactions",1);

	   iThetaCol = tLegResult.getColNum(Integer.toString(PFOLIO_RESULT_TYPE.THETA_BY_LEG_RESULT.toInt()));
	   if (iThetaCol < 1 )
	   {
	      argt.setString( "error_msg", 1, "No Theta by leg results for scenario " + iScenId + "...Unable to continue.");
	      throw new OException("No Theta by leg results for scenario " + iScenId + "...Unable to continue.");
	   }

	   iSizeCol = tLegResult.getColNum(Integer.toString(PFOLIO_RESULT_TYPE.SIZE_BY_LEG_RESULT.toInt()));
	   if (iSizeCol < 1 )
	   {
	      argt.setString( "error_msg", 1, "No Size by leg results for scenario " + iScenId + "...Unable to continue.");
	      throw new OException("No Size by leg results for scenario " + iScenId + "...Unable to continue.");
	   }

	   iPymntCol = tLegResult.getColNum(Integer.toString(PFOLIO_RESULT_TYPE.PAYMENT_DATE_BY_LEG_RESULT.toInt()));
	   if (iPymntCol < 1 )
	   {
	      argt.setString( "error_msg", 1, "No Payment date by leg results for scenario " + iScenId + "...Unable to continue.");
	      throw new OException("No Payment date by leg results for scenario " + iScenId + "...Unable to continue.");
	   }

	   iAPMDealInfoResultID = SimResult.getResultIdFromEnum("USER_RESULT_APM_LEG_INFO");
	   
	   int result_type_col = tGenResult.getColNum("result_type");
	   iNumRows = tGenResult.getNumRows();
	   for(iRow=1; iRow<=iNumRows; iRow++)
	   {
	      if (tGenResult.getInt( result_type_col, iRow) == iAPMDealInfoResultID)
	      {       
	         tAPMDealInfoResult = tGenResult.getTable( "result", iRow);
	         break;
	      }
	   }   

	   if(Table.isTableValid(tAPMDealInfoResult) == 0)
	   {
	      argt.setString( "error_msg", 1, "No APM Leg Info results for scenario " + iScenId + "...Unable to continue.");
	      throw new OException("No APM Leg Info results for scenario " + iScenId + "...Unable to continue.");

	   }

	   /* Now need to transfer over relevent fields */
	   returnt.addCols( "I(deal_num) I(deal_leg) I(deal_pdc) I(proj_idx) F(theta)"); 
	   returnt.addCols( "I(pymtdate) I(disc_idx) I(toolset) I(sort)");

	   returnt.tuneGrowth( tLegResult.getNumRows());
	   
	   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	   if(envVar != null)
	   {
		   envVar = envVar.toUpperCase();
		   qaMode = envVar.equals("TRUE");
	   }
	   
	   
	   // If AB_APM_QA_MODE is true, preserve the sorting of the DealInfo Table columns by using a copy
       if (qaMode)
       {
    	   tLegResultCopy = tLegResult.copyTable();
       	   tApmDealInfoResultCopy = tAPMDealInfoResult.copyTable();
       	   returnt.select( tLegResultCopy, "deal_num, deal_leg, deal_pdc, proj_idx, disc_idx, sort", "deal_num GT 0");
       	   returnt.select( tApmDealInfoResultCopy, "toolset", "dealnum EQ $deal_num AND leg EQ $deal_leg"); 
       }
       else
       {
       	   returnt.select( tLegResult, "deal_num, deal_leg, deal_pdc, proj_idx, disc_idx, sort", "deal_num GT 0");
       	   returnt.select( tAPMDealInfoResult, "toolset", "dealnum EQ $deal_num AND leg EQ $deal_leg"); 
       }
       
	   tSortedLegResult = Table.tableNew("sorted Leg Result");
	   tSortedLegResult.tuneGrowth( tLegResult.getNumRows());

	   /* Copy only payment, size, theta and sort columns */
	   if (qaMode)
	   {
		   tSortedLegResult.select( tLegResultCopy, tLegResultCopy.getColName( iPymntCol) + ", " + 
				   tLegResultCopy.getColName( iSizeCol) + ", " + 
				   tLegResultCopy.getColName( iThetaCol)  + ", sort", "deal_num GT 0" );
				   tLegResultCopy.destroy();
	   }
	   else
	   {
		   tSortedLegResult.select( tLegResult, tLegResult.getColName( iPymntCol) + ", " + 
				   tLegResult.getColName( iSizeCol) + ", " + 
				   tLegResult.getColName( iThetaCol)  + ", sort", "deal_num GT 0" );   
	   }
	   
	   /* Column position will differ in a new table! */
	   iPymntCol = 1;
	   iSizeCol  = 2;
	   iThetaCol = 3;

	   /* Copy all Theta values from destination (tTranResult) to the return table */
	   returnt.select( tTranResult, Integer.toString(PFOLIO_RESULT_TYPE.THETA_RESULT.toInt()) + "(theta)", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg");

	   tSortedLegResult.sortCol( "sort" );
	   iSortCol = returnt.getColNum("sort");
	   returnt.sortCol(iSortCol);

	   int pymtdate_col = returnt.getColNum("pymtdate");
	   int toolset_col = returnt.getColNum("toolset");
	   int theta_col = returnt.getColNum("theta");
	   iNumRows = tSortedLegResult.getNumRows();
	   for( iRow=1; iRow <= iNumRows; iRow++)
	   {
	      returnt.setInt( pymtdate_col, iRow, com.olf.openjvs.Math.doubleToInt(tSortedLegResult.getDouble( iPymntCol, iRow)));

	      /* Now do the calc for theta
	         here we are having to loop rather than call MathAbs on the col because we want to retain the sign on the notnl
	         and we don't want to addcol & delcol cos of memory fragmentation

	         Replace Theta values for toolsets other than FX Options
	         For FX options in some cuts there are NO theta by leg values (V80)
	         For FX options in other cuts theta by leg is always zero (V81R2) 
	         Therefore For FX options if theta by leg is non zero then use theta by leg - as its the best we have 
	      */
	      iToolset = returnt.getInt( toolset_col, iRow );
	      if (iToolset != TOOLSET_ENUM.FX_OPTION_TOOLSET.toInt() )
	      {
	          // Dependent on toolset, we need to multiply by either the original volume or absolute volume
	          // For shared toolset, use original volume, as underlying Theta By Leg is per holding instrument, so need to distinguish buy \ sell
	          // For unique toolset, use absolute volume, as underlying Theta By Leg is per unique instrument, and is affected already by buy \ sell
	          if (( iToolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() ) || ( iToolset == TOOLSET_ENUM.COM_OPT_FUT_TOOLSET.toInt() ))
	             dTheta = tSortedLegResult.getDouble( iSizeCol, iRow) * tSortedLegResult.getDouble( iThetaCol, iRow);
	          else 
	          	 dTheta = java.lang.Math.abs(tSortedLegResult.getDouble( iSizeCol, iRow)) * tSortedLegResult.getDouble( iThetaCol, iRow);
	          
	          returnt.setDouble( theta_col, iRow, dTheta);
	      }
	      else
	      {
	         /* FX options: note that this fix to use theta by leg rather tha theta is ONLY valid
	            if we never get the situation where theta by leg is zero and theta is non zero
	            otherwise we'll end up with a mix of theta and theta by leg */
	         if ( tSortedLegResult.getDouble( iThetaCol, iRow) != 0.0 )
	         {
	            dTheta = java.lang.Math.abs(tSortedLegResult.getDouble( iSizeCol, iRow)) * tSortedLegResult.getDouble( iThetaCol, iRow);
	            returnt.setDouble( theta_col, iRow, dTheta);
	         }
	      }


	      /* DEBUG SANITY CHECK */
	      if ( returnt.getInt(iSortCol, iRow ) != tSortedLegResult.getInt( "sort", iRow ))
	      {
	    	 argt.setString( "error_msg", 1,"APM_UDS_Theta error: sorteg leg result and return table missmatch \n");
	    	 throw new OException("APM_UDS_Theta error: sorteg leg result and return table missmatch.");
	      }
	   }

	   iDealNumCol = returnt.getColNum("deal_num");
	   iTranDealNumCol = tTransactions.getColNum("deal_num");
	   returnt.sortCol( iDealNumCol);

	   /* This table holds the details of any FX options, FX, Bonds etc. as these require special handling */
	   tDealsWithBlankLegResults = Table.tableNew("DealsWithBlankLegResults");
	   tDealsWithBlankLegResults.addCol( "deal_num", COL_TYPE_ENUM.COL_INT);

	   iNumRows = tTransactions.getNumRows();
	   for (iRow = 1; iRow <= iNumRows; iRow++)
	   {
	      iDealNum = tTransactions.getInt( iTranDealNumCol, iRow);
	      if (returnt.findInt( iDealNumCol, iDealNum, SEARCH_ENUM.FIRST_IN_GROUP) <=0)
	      {
	         iNewRow = tDealsWithBlankLegResults.addRow();         
	         tDealsWithBlankLegResults.setInt( 1, iNewRow, iDealNum);           
	      }
	   }

	   /* For FX options, FX, Bonds etc. take the Theta from Theta result, since THETA_BY_LEG is blank */
	   if (tDealsWithBlankLegResults.getNumRows() > 0)
	   {      
	      tDealsWithBlankLegResults.select( tTranResult, "deal_leg, " + Integer.toString(PFOLIO_RESULT_TYPE.THETA_RESULT.toInt()) + " (theta), " +
	                    "proj_idx, disc_idx", "deal_num EQ $deal_num");
	
		   if (qaMode)
		   {
			  tDealsWithBlankLegResults.select( tApmDealInfoResultCopy, "leg_end(pymtdate)", "dealnum EQ $deal_num AND leg EQ $deal_leg" );
		   }
		   else
		   {
			  tDealsWithBlankLegResults.select( tAPMDealInfoResult, "leg_end(pymtdate)", "dealnum EQ $deal_num AND leg EQ $deal_leg" );
		   }

			  returnt.select( tDealsWithBlankLegResults, "deal_num, deal_leg, theta, proj_idx, disc_idx, pymtdate", "deal_num GT 0");
		}

       if (qaMode)
       {   
          returnt.clearGroupBy ();
          returnt.addGroupBy ("deal_num");
          returnt.addGroupBy ("deal_leg");
          returnt.addGroupBy ("deal_pdc");
          returnt.groupBy ();
    	}
       
	   tDealsWithBlankLegResults.destroy();
	   tSortedLegResult.destroy();
	   if(tApmDealInfoResultCopy != null)
	   {
		   tApmDealInfoResultCopy.destroy();
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
	   returnt.setColFormatAsRef( "proj_idx",        SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsDate( "pymtdate",        DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsRef( "disc_idx",        SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsRef( "toolset",         SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
	   returnt.colHide( "sort" );
	}


	}
