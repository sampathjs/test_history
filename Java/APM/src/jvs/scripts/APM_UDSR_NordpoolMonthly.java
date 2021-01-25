/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_NordpoolMonthly.java

Date Of Last Revision:     30-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back period-level information, with
                           daily-settling monthly power futures' data fixed up, so that all days will 
                           be rolled into a single monthly row
                           
                            
*/
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_NordpoolMonthly implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
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
	      compute_result(argt, returnt);
	   else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
	      format_result(returnt);

	   Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
	   returnt.setColFormatAsDate( "startdate",          DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsDate( "enddate",            DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsDate( "startdate_int",      DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsDate( "enddate_int",        DATE_FORMAT.DATE_FORMAT_DEFAULT);

	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   UDSR compute function
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   Table    tScenResult;   /* Not freeable */
	   Table    tGenResult;    /* Not freeable */
	   Table    tDealInfo = null;     /* Not freeable */
	   int      i,  iRow, iNumRows, base_ins_type, iSD, iED;
    
	   Table    tRolledDates;
	   Table	tPeriodInfo;
	   Table	tPeriodInfoCopy;
	   Table	tDealInfoCopy;
	   boolean 	isQaMode = false;
	      
	   SimResultType  userResultApmPeriodInfo = SimResultType.create("USER_RESULT_APM_PERIOD_INFO");
	   SimResultType  userResultApmDealInfo = SimResultType.create("USER_RESULT_APM_DEAL_INFO");
			   
	   tScenResult = argt.getTable( "sim_results", 1);
	   tGenResult = tScenResult.getTable( 1, 4);
	   
	   i = tGenResult.unsortedFindInt( "result_type", userResultApmPeriodInfo.getId() /*USER_RESULT_APM_PERIOD_INFO */);

	   //Preserving the sorting of DealInfo Table from TABLE_Select if AB_APM_QA_MODE is set to true
	   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	   if(envVar != null)
	   {
		   envVar = envVar.toUpperCase();
		   isQaMode = envVar.equals("TRUE");
	   }
	   
	   if (i > 0)
	   {
		   if (isQaMode)
		   {
			   
			   tPeriodInfo = tGenResult.getTable( "result", i);
			   tPeriodInfoCopy = tPeriodInfo.copyTable();
			   returnt.select( tPeriodInfoCopy, "*", "dealnum GE 0");
			   if(Table.isTableValid(tPeriodInfoCopy) == 1)
				   tPeriodInfoCopy.destroy();
		   }
		   else
		   {
			   returnt.select( tGenResult.getTable( "result", i), "*", "dealnum GE 0");
		   }
	   }

	   i = tGenResult.unsortedFindInt( "result_type", userResultApmDealInfo.getId() /*USER_RESULT_APM_DEAL_INFO */);
	  
	   if (i > 0)
		   tDealInfo = tGenResult.getTable( "result", i);

	   /* Roll up monthly futures/forwards  */
	   tRolledDates = Table.tableNew("");
	   tRolledDates.addCols( "I (dealnum) I(param_startdate) I(param_enddate) I(ins_type) F(sd_d) F (ed_d)");

	   if (tDealInfo != null)
	   {
		   if (isQaMode)
		      {
		    	  tDealInfoCopy = tDealInfo.copyTable();
		    	  tRolledDates.select( tDealInfoCopy, "dealnum, param_startdate, param_enddate, ins_type", "dealnum GE 0");
		    	  if(Table.isTableValid(tDealInfoCopy) == 1)
		    		  tDealInfoCopy.destroy();
		      }
		      else
		      {
		    	  tRolledDates.select( tDealInfo, "dealnum, param_startdate, param_enddate, ins_type", "dealnum GE 0");
		      }
	   }
		   

	   iNumRows = tRolledDates.getNumRows();
       for (iRow = iNumRows; iRow >= 1; iRow--)
	   {
	      base_ins_type = APM_GetBaseInsType(tRolledDates.getInt( 4, iRow));
	      iSD = tRolledDates.getInt( 2, iRow);
	      iED = tRolledDates.getInt( 3, iRow);

	      /* SOME CUTS DO NOT HAVE THESE DEFINES */
	      if (((base_ins_type == INS_TYPE_ENUM.power_exch_future.toInt()) || (base_ins_type == INS_TYPE_ENUM.power_exch_forward.toInt())) &&
             ((iED - iSD >= 28) && (iED - iSD <= 31)))
          {
	          /* This is a monthly Power future - fix up its period info; to do so, leave it be in the table */
	    	  /* convert the int's to double's */
	          tRolledDates.setDouble( 5, iRow, (1.0 * tRolledDates.getInt(2, iRow))); /* start date conversion */
	          tRolledDates.setDouble( 6, iRow, (1.0 * tRolledDates.getInt(3, iRow))); /* end date conversion */
          }
          else
          {
              /* Not a monthly future, ergo, not interested, ergo, delete the row */
              tRolledDates.delRow( iRow);
          }
       }

       if (tRolledDates.getNumRows() > 0)
       {      
          returnt.select( tRolledDates, "sd_d (startdate), ed_d (enddate)", "dealnum EQ $dealnum");
       }
      
       tRolledDates.destroy();

       returnt.addCols( "I(startdate_int) I(enddate_int)");

	   returnt.mathAddColConst( "startdate", 0.0, "startdate_int");
	   returnt.mathAddColConst( "enddate", 0.0, "enddate_int");
	   
       if (isQaMode)
       {   
          returnt.clearGroupBy ();
          returnt.addGroupBy ("dealnum");
          returnt.addGroupBy ("leg");
          returnt.addGroupBy ("periodnum");
          returnt.groupBy ();
    	}
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
            	iRetVal = DBaseTable.execISql(table, "Select "+what+" from "+from+" where "+where);
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
        	OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.execISql failed " ) );
		
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
	            // db call
	            iQueryId = Query.tableQueryInsert( tTable, sColumn );
	        } catch (OException exception) {
	            OLF_RETURN_CODE olfReturnCode = exception.getOlfReturnCode();
	            
	            if (olfReturnCode == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR) {
	                numberOfRetriesThusFar++;
	                
	                String message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES, exception.getMessage());

	                OConsole.oprint(message);
	            } else {
	                // it's not a retryable error, so leave
	                break;
	            }
	        }
	    } while (iQueryId == 0 && numberOfRetriesThusFar < nAttempts);

	   return iQueryId;
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

 }

