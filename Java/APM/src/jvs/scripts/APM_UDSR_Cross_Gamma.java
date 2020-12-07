/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_Cross_Gamma.java

Date Of Last Revision:     10-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM Cross Gamma result.
                           Relies on TRAN_GPT_DELTA_BY_LEG and PFOLIO_RESULT_TYPE.TRAN_GPT_CROSS_GAMMA_RESULT.
                           The former provides the diagonal for the cross-gamma matrix, the latter gives
                           the upper half of the matrix. This result combines the two, flips the cross gamma
                           to get the lower part of the matrix, and adds it on. For every non-diagonal entry
                           the delta result is set to zero, but is sensitive to the appropriate cross-gamma.
  
                           
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class APM_UDSR_Cross_Gamma implements IScript {

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

   int operation = 0;

   operation = argt.getInt( "operation", 1);

   if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt() )
      compute_result(argt, returnt);
   else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
      format_result(returnt);

   Util.exitSucceed();
}

/*-------------------------------------------------------------------------------
Name:          compute_result()
Description:   does the calculation for the APM Cross Gamma result.
Parameters:    Table argt, Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result(Table argt, Table returnt) throws OException
{
   Table    tScenResult        = null;      /* Not freeable */
   Table    tGenResult         = null;      /* Not freeable */
   Table    tDeltaResult       = null;      /* Not freeable */
   Table    tDeltaResults      = null;      /* Not freeable */
   Table    tCrossGammaResult  = null;      /* Not freeable */
   Table    tCrossGammaResults = null;      /* Not freeable */
   Table    tDFResults         = null;      /* Not freeable */
   
   
   /* Get Tran Gpt Delta By Leg Result */ 
   tScenResult = argt.getTable( "sim_results", 1);
   tGenResult = tScenResult.getTable( 1, 4);
   tDeltaResults = SimResult.getGenResultTables(tGenResult, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_LEG_RESULT.toInt()); /*201*/
   tCrossGammaResults = SimResult.getGenResultTables(tGenResult, PFOLIO_RESULT_TYPE.TRAN_GPT_CROSS_GAMMA_RESULT.toInt()); /*218*/
   
   if (tDeltaResults == null || tCrossGammaResults == null || tDeltaResults.getNumRows() == 0)
   {
      return;
   }
   else
   {
      tDeltaResult = tDeltaResults.getTable( 1, 1);
   }

   /* We could have a valid situation where no cross gamma results are generated */
   if (tCrossGammaResults.getNumRows() > 0)
   {
      tCrossGammaResult = tCrossGammaResults.getTable( 1, 1);
   }

   /* Interested in PRICING_MODEL_ENUM.discounting index and DF factor; take them from Tran Leg results */
   tDFResults = tScenResult.getTable( 1, 3);

   /* Now need to transfer over relevant fields */
   returnt.addCols( "I(deal_num) I(deal_leg) I(deal_pdc) I(index1) I(gpt_id1)I(index2) I(gpt_id2) I(date) I(disc_idx)");
   returnt.addCols( "F(df) F(df_by_leg) F(delta) F(fv_delta) F(gamma) F(fv_gamma)");

   /* Size of return table is size of delta (diagonal) + twice the cross gamma table (as it is only one half of the matrix) */
   returnt.tuneGrowth( tDeltaResult.getNumRows() + 2 * (tCrossGammaResult.getNumRows()));
   
   /* Populate the short diagonal */
   returnt.select( tDeltaResult, "deal_num, deal_leg, deal_pdc, index (index1), gpt_id (gpt_id1), index (index2), gpt_id (gpt_id2), delta, gamma", "index GT 0");

   /* Populate the upper half of the matrix - note that delta is not populated and will remain zero */
   returnt.select( tCrossGammaResult, "deal_num, deal_leg, deal_pdc, index1, gpt_id1, index2, gpt_id2, cross_gamma(gamma)", "deal_num GT 0" );
   
   /* Populate the lower half of the matrix - note that delta is not populated and will remain zero */
   returnt.select( tCrossGammaResult, "deal_num, deal_leg, deal_pdc, index1 (index2), gpt_id1 (gpt_id2), index2 (index1), gpt_id2 (gpt_id1), cross_gamma(gamma)", "deal_num GT 0" );
   
   /* For each index/gridpoint, identify date and delta shift */
   identify_gpt_date(returnt, tDeltaResult);
   
   /* Get the PRICING_MODEL_ENUM.discounting index and DF for the deal */
   /* Discounting index is used by FX delta calculation; it needs disc_idx to decide whether to convert between */
   returnt.select( tDFResults, "disc_idx, " + Integer.toString(PFOLIO_RESULT_TYPE.DF_BY_LEG_RESULT.toInt())+ "(df_by_leg)", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg AND deal_pdc EQ $deal_pdc");

   /* Divide by discount factor from discount index for that date to get FV deltas (standard and contract) */
   Index.tableColIndexDateToDF(returnt, "disc_idx", "date", "df");

   /* Fix up the discount factor for toolsets which do not have DF_BY_LEG working properly */  
   fix_discount_factor(returnt);
   
   
   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
   if (envVar != null)
   {
		envVar = envVar.toUpperCase();
	  
		if (envVar.equals("TRUE"))
		{   
		  returnt.clearGroupBy ();
		  returnt.addGroupBy ("deal_num");
		  returnt.addGroupBy ("deal_leg");
		  returnt.addGroupBy ("deal_pdc");
		  returnt.addGroupBy ("index1");
		  returnt.addGroupBy ("gpt_id1");
		  returnt.addGroupBy ("index2");
		  returnt.addGroupBy ("gpt_id2");
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
   returnt.setColFormatAsRef( "index1",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "index2",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsDate( "date",           DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "disc_idx",        SHM_USR_TABLES_ENUM.INDEX_TABLE);

}


/*-------------------------------------------------------------------------------
Name:          fix_discount_factor
Description:   Fix up the discount factor for toolsets which do not have DF_BY_LEG working properly
Parameters:    Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void fix_discount_factor(Table returnt)throws OException
{
   double   dfByLeg    = 0.0;
   double   delta      = 0.0;
   double   gamma      = 0.0;
   int dfByLegCol = returnt.getColNum("df_by_leg");
   int dfCol      = returnt.getColNum("df");
   int deltaCol   = returnt.getColNum("delta");
   int gammaCol   = returnt.getColNum("gamma");
   int fvDeltaCol = returnt.getColNum("fv_delta");
   int fvGammaCol = returnt.getColNum("fv_gamma");
	   
   int iNumRows = returnt.getNumRows();
   for (int iRow = 1; iRow <= iNumRows; iRow++)
   {
	  dfByLeg = returnt.getDouble( dfByLegCol, iRow);
	  if ( dfByLeg == 0.0)
	  {
	     dfByLeg = returnt.getDouble( dfCol, iRow);
	     returnt.setDouble( dfByLegCol, iRow, dfByLeg);
	  }

	  if (dfByLeg == 0.0)
	     continue;
	       
	  /* Divide delta and gamma values by the DF_BY_LEG discount factor */
	  /* This way, FV deltas for non-derivative products will match notional value, as expected */
	  delta = returnt.getDouble(deltaCol, iRow);
	  if (delta != 0.0)
		 returnt.setDouble( fvDeltaCol, iRow, delta / dfByLeg);
	       
      gamma = returnt.getDouble(gammaCol, iRow);
	  if (gamma != 0.0)
		 returnt.setDouble( fvGammaCol, iRow, gamma / dfByLeg);
   }
}

/*-------------------------------------------------------------------------------
Name:          identify_gpt_date
Description:   For each index/gridpoint, identify date and delta shift
Parameters:    Table returnt, Table tDeltaResult  
Return Values:   
-------------------------------------------------------------------------------*/
void identify_gpt_date(Table returnt, Table tDeltaResult) throws OException
{
   Table    tIndexData = null;
   Table    tIndices   = null;       
   int      iRow       = 0;
   int      iRow2      = 0;
   int      iNumRows   = 0;
   int      iNumRows2  = 0;
   int      indexCol   = 0;
   int      index      = 0;
   int      curveDateCol = 0;
   int      curveDateIntCol = 0;
	
   tIndices = Table.tableNew();
   tIndices.select( tDeltaResult, "DISTINCT, index", "index GT 0");

   iNumRows = tIndices.getNumRows();
   for (iRow = 1; iRow <= iNumRows; iRow++)
   { 
	  index = tIndices.getInt( 1, iRow);
      tIndexData = Index.loadAllGpts(index); 

      tIndexData.addCols( "I(index) I(curve_date_int)");
	      
      curveDateIntCol = tIndexData.getColNum("curve_date_int"); 
      curveDateCol = tIndexData.getColNum("curve_date"); 
      indexCol = tIndexData.getColNum("index"); 
	      
      iNumRows2 = tIndexData.getNumRows();
      for (iRow2 = 1; iRow2 <= iNumRows2; iRow2++)
      {
    	  tIndexData.setInt( indexCol, iRow2, index);
          tIndexData.setInt( curveDateIntCol, iRow2, ODateTime.strDateTimeToDate(tIndexData.getString( curveDateCol, iRow2)));
      }     

      /* Using curve_date_int, as curve_date is given as a String */
      returnt.select( tIndexData, "curve_date_int(date)", "index EQ $index1 AND id EQ $gpt_id1");

      tIndexData.destroy();
   }
   
   tIndices.destroy();
}
}
