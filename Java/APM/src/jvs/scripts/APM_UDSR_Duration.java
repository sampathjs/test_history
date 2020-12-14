/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_Duration.java 

Date Of Last Revision:     10-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM Duration, Mod.Duration, Convexity result.
                            
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class APM_UDSR_Duration implements IScript {


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
Description:   does the calculation for the APM Duration result.
Parameters:    Table argt, Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result(Table argt, Table returnt) throws OException
{
   Table    tScenResult = null;      /* Not freeable */
   Table    tTranResult = null;      /* Not freeable */
   String   sWhat = null;
   int row, numRows = 0;
   int currentNotnlCol = 0;
   int baseRvCol = 0;
   int durationCol = 0;
   int modDurationCol = 0;
   int convexityCol = 0;
   int yToCallCol =0;
   int yToMatCol = 0;
   int yToWorstCol = 0;
   int yToNextCol =0;
   int durationXNotionalCol = 0;
   int modDurationXNotionalCol = 0;
   int convexityXNotionalCol = 0;
   int yToCallXNotionalCol = 0;
   int yToMatXNotionalCol = 0;
   int yToWorstXNotionalCol = 0;
   int yToNextXNotionalCol = 0;
   int durationXRvCol = 0;
   int modDurationXRvCol = 0;
   int convexityXRvCol = 0;
   int yToCallXRvCol = 0;
   int yToMatXRvCol = 0;
   int yToWorstXRvCol = 0;
   int yToNextXRvCol = 0;
   double currentNotnl = 0;
   double baseRv = 0;
   double duration = 0;
   double modDuration = 0;
   double convexity = 0;
   double yToCall =0;
   double yToMat = 0;
   double yToWorst = 0;
   double yToNext =0;
   
   tScenResult = argt.getTable( "sim_results", 1);
   tTranResult = tScenResult.getTable( 1, 1);
   
   /* Add basic row identification columns anf weighting columns */
   returnt.addCols("I(dealnum) I(projindex) I(disc_idx) I(leg) F(current_notnl) F(rv) F(base_rv)");
  
   /* Add basic data columns */
   returnt.addCols( "F(duration) F(mod_duration) F(convexity) F(y_to_call) F(y_to_mat) F(y_to_worst) F(y_to_next) F(accrued_interest)");

   /* Add data columns to be multiplied by notional for weighted averages by notional */
   returnt.addCols( "F(duration_x_notional) F(mod_duration_x_notional) F(convexity_x_notional) F(y_to_call_x_notional) F(y_to_mat_x_notional) F (y_to_worst_x_notional) F (y_to_next_x_notional)"); 

   /* Add data columns to be multiplied by Replacement Value for weighted averages by Replacement Value */
   returnt.addCols( "F(duration_x_rv) F (mod_duration_x_rv) F(convexity_x_rv) F(y_to_call_x_rv) F(y_to_mat_x_rv) F(y_to_worst_x_rv) F(y_to_next_x_rv)");

   sWhat = "deal_num (dealnum), deal_leg (leg), proj_idx (projindex), disc_idx, " +
   /*0*/   Integer.toString(PFOLIO_RESULT_TYPE.PV_RESULT.toInt()) +"(mtm), " +
   /*177*/ Integer.toString(PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT.toInt()) +"(current_notnl), " +
   /*259*/ Integer.toString(PFOLIO_RESULT_TYPE.REPLACEMENT_VALUE_RESULT.toInt()) +"(rv), " +
   /*299*/ Integer.toString(PFOLIO_RESULT_TYPE.BASE_REPLACEMENT_VALUE_RESULT.toInt()) +"(base_rv), " +
   /*91*/  Integer.toString(PFOLIO_RESULT_TYPE.DURATION_RESULT.toInt()) +"(duration), " +
   /*92*/  Integer.toString(PFOLIO_RESULT_TYPE.MOD_DURATION_RESULT.toInt()) +"(mod_duration), " +
   /*93*/  Integer.toString(PFOLIO_RESULT_TYPE.CONVEXITY_RESULT.toInt()) +"(convexity), " +
   /*307*/ Integer.toString(PFOLIO_RESULT_TYPE.YIELD_TO_CALL_RESULT.toInt()) +"(y_to_call), " +
   /*309*/ Integer.toString(PFOLIO_RESULT_TYPE.YIELD_TO_MAT_RESULT.toInt()) +"(y_to_mat), " +
   /*308*/ Integer.toString(PFOLIO_RESULT_TYPE.YIELD_TO_WORST_RESULT.toInt()) +"(y_to_worst), " +
   /*249*/ Integer.toString(PFOLIO_RESULT_TYPE.YIELD_TO_NEXT_RESULT.toInt()) +"(y_to_next), " +
   /*70*/  Integer.toString(PFOLIO_RESULT_TYPE.ACCRUED_INTEREST_RESULT.toInt()) +"(accrued_interest)";
   
   returnt.select( tTranResult, sWhat, "deal_num GT 0");

   /*get column num*/
   currentNotnlCol         = returnt.getColNum("current_notnl");
   baseRvCol               = returnt.getColNum("base_rv");
   durationCol             = returnt.getColNum("duration");
   modDurationCol          = returnt.getColNum("mod_duration");
   convexityCol            = returnt.getColNum("convexity");
   yToCallCol              = returnt.getColNum("y_to_call");
   yToMatCol               = returnt.getColNum("y_to_mat");
   yToWorstCol             = returnt.getColNum("y_to_worst");
   yToNextCol              = returnt.getColNum("y_to_next");
   durationXNotionalCol    = returnt.getColNum("duration_x_notional");
   modDurationXNotionalCol = returnt.getColNum("mod_duration_x_notional");
   convexityXNotionalCol   = returnt.getColNum("convexity_x_notional");
   yToCallXNotionalCol     = returnt.getColNum("y_to_call_x_notional");
   yToMatXNotionalCol      = returnt.getColNum("y_to_mat_x_notional");
   yToWorstXNotionalCol    = returnt.getColNum("y_to_worst_x_notional");
   yToNextXNotionalCol     = returnt.getColNum("y_to_next_x_notional");
   durationXRvCol          = returnt.getColNum("duration_x_rv");
   modDurationXRvCol       = returnt.getColNum("mod_duration_x_rv");
   convexityXRvCol         = returnt.getColNum("convexity_x_rv");
   yToCallXRvCol           = returnt.getColNum("y_to_call_x_rv");
   yToMatXRvCol            = returnt.getColNum("y_to_mat_x_rv");
   yToWorstXRvCol          = returnt.getColNum("y_to_worst_x_rv");
   yToNextXRvCol           = returnt.getColNum("y_to_next_x_rv");
   
   numRows = returnt.getNumRows();
   
   for (row=1; row<=numRows; row++)
   {
	   currentNotnl = returnt.getDouble(currentNotnlCol, row);
	   baseRv = returnt.getDouble(baseRvCol, row);
	   duration = returnt.getDouble(durationCol, row);
	   modDuration = returnt.getDouble(modDurationCol, row);
	   convexity = returnt.getDouble(convexityCol, row);
	   yToCall = returnt.getDouble(yToCallCol, row);
	   yToMat = returnt.getDouble(yToMatCol, row);
	   yToWorst = returnt.getDouble(yToWorstCol, row);
	   yToNext = returnt.getDouble(yToNextCol, row);
	   
	   /* Multiply by notional */
       returnt.setDouble( durationXNotionalCol,    row, duration * currentNotnl);
	   returnt.setDouble( modDurationXNotionalCol, row, modDuration * currentNotnl);
	   returnt.setDouble( convexityXNotionalCol,   row, convexity * currentNotnl);
	   returnt.setDouble( yToCallXNotionalCol,     row, yToCall * currentNotnl);
	   returnt.setDouble( yToMatXNotionalCol,      row, yToMat * currentNotnl);
	   returnt.setDouble( yToWorstXNotionalCol,    row, yToWorst * currentNotnl);
	   returnt.setDouble( yToNextXNotionalCol,     row, yToNext * currentNotnl);

	   /* Multiply by RV */
	   returnt.setDouble( durationXRvCol,          row, duration * baseRv);
	   returnt.setDouble( modDurationXRvCol,       row, modDuration * baseRv);
	   returnt.setDouble( convexityXRvCol,         row, convexity * baseRv);
	   returnt.setDouble( yToCallXRvCol,           row, yToCall * baseRv);
	   returnt.setDouble( yToMatXRvCol,            row, yToMat * baseRv);
	   returnt.setDouble( yToWorstXRvCol,          row, yToWorst * baseRv);
	   returnt.setDouble( yToNextXRvCol,           row, yToNext * baseRv);
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
Parameters:    Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void format_result(Table returnt) throws OException
{
   returnt.setColFormatAsRef( "projindex",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "disc_idx",           SHM_USR_TABLES_ENUM.INDEX_TABLE);
}


}
