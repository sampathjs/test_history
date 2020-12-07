/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                      APM_UDSR_IndexGptInfo.java
 
Report Name:                    NONE 
 
Output File Name:               NONE
 
Author:                         
Creation Date:                  
 
Revision History:               10-Mar-2014 - Converted from AVS to OpenJVS           
                                                
Script Type:                    User-defined simulation result
 
Description:                    User defined Sim Result which uses the USER_RESULT_APM_INDEX_INFO and PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT 
                                to work out the start date based on the end date given for each.
                                In addition, also provides index rate values for "Batch Index Rates" package.
                                

*/ 
package jvs.scripts;

import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_IndexGptInfo implements IScript {
   
   
   
   private static final Map<Integer, IndexDetails> CACHED_INDEX_CLASSES = new HashMap<Integer, IndexDetails>();

   private static final int SECONDS_IN_DAY = 60 * 60 * 24;
   
   static class IndexDetails {
	   int index_id;
	   int class_id;
	   int inheritance;
	   int construction_method;
	   
	   public IndexDetails(int indexId, int classId, int inheritance, int construction_method) {
		   this.index_id = indexId;
		   this.class_id = classId;
		   this.inheritance = inheritance;
		   this.construction_method = construction_method;
	   }
	   
	   public int getIndexId() {
		   return this.index_id;
	   }
	   public int getClassId() {
		   return this.class_id;
	   }
	   public int getInheritance() {
		   return this.inheritance;
	   }
	   public int getConstructionMethod() {
		   return this.construction_method;
	   }
   }

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
Description:   works out start date based on previous end date for Index gpts.
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result(Table argt, Table returnt) throws OException
{
   Table    tScenResult;      /* Not freeable */
   Table    tGenResult;       /* Not freeable */
   Table    tIndexRates;
   Table    tIndexEnrichment = null;
   int         iRow;
   int         iNumRows;
   int         iRatesRow;
   int         iOldEndDate;
   int         iEndTime;
   int         iStartDate;
   int         iEndDate;
   int         iTimeColNum;
   int         iCurrentIndex;
   int         iIndexEnrichmentResultID;
   int         iIndexEnrichmentRow;
   
      
   /* Get Index Rates */
   tScenResult = argt.getTable( "sim_results", 1);
   tGenResult = tScenResult.getTable( 1, 4);

   /* Find us the APM Index Enrichment table */
   int  result_type_col = tGenResult.getColNum("result_type");
   int  result_col = tGenResult.getColNum("result");
   
   iIndexEnrichmentResultID = SimResult.getResultIdFromEnum("USER_RESULT_APM_INDEX_INFO");
   iNumRows = tGenResult.getNumRows();
   for(iRow=1; iRow<=iNumRows; iRow++)
   {
      if (tGenResult.getInt( result_type_col, iRow) == iIndexEnrichmentResultID)
      {       
         tIndexEnrichment = tGenResult.getTable(result_col, iRow);
         break;
      }
   }   

   if(tIndexEnrichment == null || Table.isTableValid(tIndexEnrichment) == 0)
      return;

   /* Set up the columns in the return table */
   SetupTable(returnt);

   /* Iterate over general results, looking for "Index Rates" */
   for(iRow=1; iRow<=iNumRows; iRow++)
   {
      if (tGenResult.getInt( result_type_col, iRow) == PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT.toInt())
      {       
         tIndexRates = Table.tableNew("Index Rates");
         iCurrentIndex = tGenResult.getInt( "disc_idx", iRow);
         SetupTable(tIndexRates);

         /* This variable will keep the previous row's end date; each start date will be "this + 1" */
         iOldEndDate = 0;

         /* Retrieve data from results, dependant on whether time column was present there */
         iTimeColNum = tGenResult.getTable( result_col, iRow).getColNum("time");
         if (iTimeColNum == Util.NOT_FOUND)
         {
            tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, id (index_gpt_id), end_date, coverage_start_date, coverage_end_date, label (gpt_label)", "id GE 0");
            tIndexRates.group( "end_date");
         }
         else
         {
            tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, id (index_gpt_id), end_date, coverage_start_date, coverage_end_date, time(end_time), label (gpt_label)", "id GE 0");  
            tIndexRates.group( "end_date, end_time");
         }

         int index_id_col = tIndexRates.getColNum("index_id");
         int start_date_col = tIndexRates.getColNum("start_date");
         int end_date_col = tIndexRates.getColNum("end_date");
         int start_time_col = tIndexRates.getColNum("start_time");
         int end_time_col = tIndexRates.getColNum("end_time");
         int coverage_start_date_col = tIndexRates.getColNum("coverage_start_date");
         int coverage_end_date_col = tIndexRates.getColNum("coverage_end_date");
         
         int iIdxNumRows = tIndexRates.getNumRows();    
       
         for (iRatesRow = 1; iRatesRow <=iIdxNumRows ; iRatesRow++)
         {
        	/* The projection index is known from the main table */
            tIndexRates.setInt( index_id_col, iRatesRow, iCurrentIndex);  
            
            /* Retrieve time - this identifies the index as date- or time- based */
            if (iTimeColNum == Util.NOT_FOUND)
            {
               iEndTime = -1;
            }
            else
            {
               iEndTime = tIndexRates.getInt( end_time_col, iRatesRow);
            }

            if (iEndTime == -1)
            {
               /* Pure date-based indices */
               if (iOldEndDate > 0)
               {
                  /* For most gridpoints, the start date of one is the preceding's end date + 1 */
                  tIndexRates.setInt( start_date_col, iRatesRow, iOldEndDate + 1);               
               }
               else
               {
                  /* Normally, we use the coverage start date for 1st gpt but sometimes it is greater than its end date! */
                  iStartDate = tIndexRates.getInt( coverage_start_date_col, iRatesRow);
                  iEndDate = tIndexRates.getInt( end_date_col, iRatesRow);

                  if (iStartDate > iEndDate)
                  {
                     /* Heuristic 1 - set start date to be today */
                     iStartDate = OCalendar.today();
                  }
                  if (iStartDate > iEndDate)
                  {
                     /* Heuristic 2 - set start date to be one day earlier than end date */
                     iStartDate = iEndDate - 1;
                  }

                  tIndexRates.setInt( start_date_col, iRatesRow, iStartDate);
               }

               iOldEndDate = tIndexRates.getInt( end_date_col, iRatesRow);
                  
               tIndexRates.setInt( start_time_col, iRatesRow, 0);
               tIndexRates.setInt( end_time_col, iRatesRow, SECONDS_IN_DAY);
            }
            else
            {
               /* Time-based indices are in play */

               /* All gridpoints will have matching start and end dates */
               tIndexRates.setInt( start_date_col, iRatesRow, tIndexRates.getInt( end_date_col, iRatesRow));
               tIndexRates.setInt( coverage_start_date_col, iRatesRow, tIndexRates.getInt( coverage_end_date_col, iRatesRow));

               /* Where last gridpoint was yesterday or this is the first gridpoint, assume start time of zero */
               if ((iRatesRow == 1) || 
                   (tIndexRates.getInt( end_date_col, iRatesRow) > tIndexRates.getInt( end_date_col, iRatesRow - 1))
                  )
               {
                  tIndexRates.setInt( start_time_col, iRatesRow, 0);
               }
               else
               {
                  /* Set start time of this gridpoint to match end time of last gridpoint */
                  tIndexRates.setInt( start_time_col, iRatesRow, tIndexRates.getInt( end_time_col, iRatesRow - 1));
               }
            }            
         }

         /* Now identify whether this is a commodities curve */

        iIndexEnrichmentRow = tIndexEnrichment.unsortedFindInt( "index_id", iCurrentIndex);
		IndexDetails details = GetIndexDetails(iCurrentIndex);
		
		  /* If we use curve ctor, lookup output */
		if (details.getConstructionMethod() > IDX_CONSTRUCTION_METHOD_ENUM.NUM_IDX_CON.toInt())
		{
			tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, output_value (indexratevalue), id (index_gpt_id)", "id EQ $index_gpt_id");
		}
		else if ((iIndexEnrichmentRow > 0) && 
             (tIndexEnrichment.getInt( "market", iIndexEnrichmentRow) == IDX_MARKET_ENUM.IDX_MARKET_COMMODITIES.toInt()))
		{
			/* if we are a commodities, time-based forward curve, lookup output*/
			if (details.getClassId() == IDX_CLASS_ENUM.IDX_CLASS_TIMEBASE_FWD.toInt())
			{
				tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, output_value (indexratevalue), id (index_gpt_id)", "id EQ $index_gpt_id");
			}
			/* if we are a commodities, datebased forward curve and index inheritance is on (index inheritance is ON by default for parent indices, optional for child indexes) - get effective value*/
			else if (details.getClassId() == IDX_CLASS_ENUM.IDX_CLASS_DATEBASE_FWD.toInt() && details.getInheritance() == 1)
			{
				tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, effective_value (indexratevalue), id (index_gpt_id)", "id EQ $index_gpt_id");
			}
			else
			{
				/* lookup output value */
				tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, output_value (indexratevalue), id (index_gpt_id)", "id EQ $index_gpt_id");
			}
		}
		else
		{
				/* lookup effective value */
			tIndexRates.select( tGenResult.getTable( result_col, iRow), "DISTINCT, effective_value (indexratevalue), id (index_gpt_id)", "id EQ $index_gpt_id");
		}
		
         returnt.select( tIndexRates, "*", "index_gpt_id GE 0");

         tIndexRates.destroy();
      }
   }

   /* Finally, set the weights to 1.0 */
   returnt.setColValDouble( "weight", 1.0);

   /* get the contract name from index. */
   returnt.addCol( "jd_col", COL_TYPE_ENUM.COL_INT);
   int contractCol = returnt.getColNum("contract");
   int dateCol = returnt.getColNum("coverage_start_date");
   int idxCol = returnt.getColNum("index_id");
   int jdCol = returnt.getColNum("jd_col");

   Index.colConvertDateToContract(returnt, idxCol, dateCol, contractCol, jdCol);
   returnt.delCol(jdCol);
   
	String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	if (envVar != null)
	{
		envVar = envVar.toUpperCase();
	  
		if (envVar.equals("TRUE"))
		{   
		  returnt.clearGroupBy ();
		  returnt.addGroupBy ("index_id");
		  returnt.addGroupBy ("index_gpt_id");
		  returnt.groupBy ();
		}
	}

}

/*-------------------------------------------------------------------------------
Name:          format_result()
Description:   UDSR format function.
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void format_result(Table returnt) throws OException
{
   returnt.setColTitle( "index_id", "Index");
   returnt.setColTitle( "index_gpt_id", "Gridpoint ID");

   returnt.clearGroupBy();
   returnt.addGroupBy( "index_id");
   returnt.addGroupBy( "start_date");
   returnt.addGroupBy( "start_time");

   returnt.setColFormatAsRef( "index_id",        SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsDate( "start_date",     DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "end_date",       DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsTime( "start_time");
   returnt.setColFormatAsTime( "end_time");
   returnt.setColFormatAsDate( "coverage_start_date", DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "coverage_end_date",   DATE_FORMAT.DATE_FORMAT_DEFAULT);

   returnt.setColHideStatus( returnt.getColNum( "weight"), 1);
}

/*-------------------------------------------------------------------------------
Name:          SetupTable(Table table)
Description:   Table Schema for returned result.
Parameters:    new table.  
Return Values:   
-------------------------------------------------------------------------------*/
void SetupTable(Table table) throws OException
{
   table.addCols( "I(index_id) I(index_gpt_id) I(start_date) I(end_date) I(start_time) I(end_time)");
   table.addCols( "I(coverage_start_date) I(coverage_end_date) F(indexratevalue) S(gpt_label) F(weight) S(contract)");
}

/*-------------------------------------------------------------------------------
Name:          APM_TABLE_TuneGrowth
Description:   Only tune if no rows > 0 !!
Parameters:
Return Values:   retval (success or failure)
Effects:   <any *>
-------------------------------------------------------------------------------*/
void APM_TABLE_TuneGrowth( Table tDestTable, int iNumRows ) throws OException
{
   if ( iNumRows > 0 )
      tDestTable.tuneGrowth( iNumRows );
}

/**
* Returns the index class ID for the index with the provided ID.  The IDs encountered are cached.
*  
* @param indexId Id of the index to get the class.
* 
* @return Id of the class of the index with the specified id; or -1 if the class id could not be retrieved.
*/
static IndexDetails GetIndexDetails(int indexId) {
  // number of retries for the db call
  final int maxNumberOfRetries = 5;

  int classId = Integer.MIN_VALUE;
  int inheritance = Integer.MIN_VALUE;
  int construction_method = Integer.MIN_VALUE;

  if (CACHED_INDEX_CLASSES.containsKey(indexId)) {
    return CACHED_INDEX_CLASSES.get(indexId);
  } else {
    try {
      Table classTable = Table.tableNew();

      int numberOfRetriesThusFar = 0;
      int dbCallRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

      do {
        try {
          final String what = "class, inheritance, construction_method";
          final String from = "idx_def";
          final String where = "db_status = 1 and index_id = " + indexId;

          dbCallRetVal = DBaseTable.loadFromDbWithSQL(classTable, what, from, where);
        } catch (OException exception) {
          dbCallRetVal = exception.getOlfReturnCode().toInt();
        } finally {
          if (dbCallRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
          numberOfRetriesThusFar += 1;
          // have a wait before trying again
          Debug.sleep(numberOfRetriesThusFar * 1000);
        } else {
          // exit the loop
          break;
        }
        }
      } while (numberOfRetriesThusFar < maxNumberOfRetries);

      // now that the db call succeeded...
      classId = classTable.getInt("class", 1);
      inheritance = classTable.getInt("inheritance", 1);
      construction_method = classTable.getInt("construction_method", 1);
      
      CACHED_INDEX_CLASSES.put(indexId, new IndexDetails(indexId, classId, inheritance, construction_method));
	  
      classTable.destroy();
	  return CACHED_INDEX_CLASSES.get(indexId);
    } catch (OException exception) {
      if(classId == Integer.MIN_VALUE) {
        classId = -1;
      }
	  return null;
    }
  }

  
}

}
