/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_PeriodInfo.java

Date Of Last Revision:     30-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back period-level information
                           
                            
*/
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_PeriodInfo implements IScript {
	
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
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   UDSR compute function
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   Table    tScenResult;      /* Not freeable */
	   Table    tTranLegResult;   /* Not freeable */
	   Table    tGenResult;       /* Not freeable */
	   Table    tDealInfo = null; /* Not freeable */
	   Table    tTrans;           /* Not freeable */
	   Table    tDeliveryDates;     
	   Table    tDeleteTable; 
	   Table    tDealInfoCopy;

	   int         iRow, iNumRows, iFoundRow, iDealInfoResultID, base_ins_type, iToolset;
	   int         settleDate, result_type_col, tran_ptr_col;
       int         deal_num_col, dealnum_col, startdate_col, enddate_col;
       
	   Transaction TranPointer;
	   boolean isQaMode = false;
	   tScenResult = argt.getTable( "sim_results", 1);
	   tTranLegResult = tScenResult.getTable( 1, 3);
	   tTrans = argt.getTable( "transactions", 1);

	   returnt.addCols( "I(dealnum) I(leg) I(periodnum) F(startdate) F(enddate)");

	   if ( tTranLegResult.getNumRows() > 0 )
	      returnt.tuneGrowth( tTranLegResult.getNumRows());

	   returnt.select( tTranLegResult, "deal_num (dealnum), deal_leg (leg), deal_pdc (periodnum), 195 (startdate), 196 (enddate)", "deal_num GT 0");

	   if ( returnt.getNumRows() < 1 )
	      returnt.addRow();

	   /* for non power COMFUT's take the start date/end date from the DEALINFO result (we'll always want to see by delivery) */
	   tGenResult = tScenResult.getTable( 1, 4);
	   iDealInfoResultID = SimResult.getResultIdFromEnum("USER_RESULT_APM_DEAL_INFO");
	   
	   result_type_col = tGenResult.getColNum("result_type");
	   iNumRows = tGenResult.getNumRows();
	   for(iRow=1; iRow <= iNumRows; iRow++)
	   {
	      if (tGenResult.getInt( result_type_col, iRow) == iDealInfoResultID)
	      {       
	         tDealInfo = tGenResult.getTable( "result", iRow);
	         break;
	      }
	   }   

	   if(tDealInfo == null)
	      return;

	   /* select across
	      don't do this for shareable power types...
	      PWR-EXCH-BOM-FUT, PWR-EXCH-BOM-FWD,PWR-EXCH-FUT,PWR-EXCH-FWD,PWR-EXCH-OPT,PWR-SHARED-PHYS,PWR-SHARED-FTP_INDEX_PURPOSE.SWAP
	      this table should be inserted in order so FindInt works */
	   tDeleteTable = Table.tableNew();
	   tDeleteTable.addCol( "base_ins_type", COL_TYPE_ENUM.COL_INT);
	   tDeleteTable.addNumRows( 7);
	   tDeleteTable.setInt( 1, 1, INS_TYPE_ENUM.power_exch_future.toInt());      /* 32009 */
	   tDeleteTable.setInt( 1, 2, INS_TYPE_ENUM.power_exch_forward.toInt());     /* 32010 */
	   tDeleteTable.setInt( 1, 3, INS_TYPE_ENUM.power_exch_bom_future.toInt());  /* 32011 */
	   tDeleteTable.setInt( 1, 4, INS_TYPE_ENUM.power_exch_bom_forward.toInt()); /* 32012 */
	   tDeleteTable.setInt( 1, 5, INS_TYPE_ENUM.power_shared_phys.toInt());      /* 32013 */
	   tDeleteTable.setInt( 1, 6, INS_TYPE_ENUM.power_shared_swap.toInt());      /* 32014 */
	   tDeleteTable.setInt( 1, 7, INS_TYPE_ENUM.power_exch_option.toInt());      /* 36007 */
	   tDeleteTable.sortCol( 1);

	   /* need to convert the dates to doubles because of period info returning doubles rather than int's */
	   tDeliveryDates = Table.tableNew();
	   tDeliveryDates.addCols( "I(dealnum) I(ins_type) I(startdate) I(enddate) F(startdate_d) F(enddate_d)");
	   
	   //Preserving the sorting of DealInfo Table from TABLE_Select if AB_APM_QA_MODE is set to true
	   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
	   if(envVar != null)
	   {
		   envVar = envVar.toUpperCase();
		   isQaMode = envVar.equals("TRUE");
	   }

	   if (isQaMode)
	   {
		   tDealInfoCopy = tDealInfo.copyTable();
		   tDeliveryDates.select( tDealInfoCopy, "dealnum,ins_type,startdate,enddate", "ins_class EQ 1" );
		   if(Table.isTableValid(tDealInfoCopy) == 1)
			   tDealInfoCopy.destroy();
	   }
	   else
	   {
		   tDeliveryDates.select( tDealInfo, "dealnum,ins_type,startdate,enddate", "ins_class EQ 1" );
	   }

	   /* change the ins type to base ins type to cope with user defined instruments
	      delete where a match */
	   iNumRows = tDeliveryDates.getNumRows();
	   for (iRow = iNumRows; iRow >= 1; iRow--)
	   {
	     base_ins_type = APM_GetBaseInsType(tDeliveryDates.getInt( 2, iRow));
	     if ( tDeleteTable.findInt( 1, base_ins_type, SEARCH_ENUM.FIRST_IN_GROUP) > 0 )
	         tDeliveryDates.delRow( iRow);
	     else
	     {
	    	 /* convert the int's to double's */
	  	     tDeliveryDates.setDouble(5, iRow, (1.0 * tDeliveryDates.getInt(3, iRow))); /* start date conversion */
	  	     tDeliveryDates.setDouble(6, iRow, (1.0 * tDeliveryDates.getInt(4, iRow))); /* end date conversion */
	     }
	   }
	   tDeleteTable.destroy();
	   
	   returnt.select( tDeliveryDates, "startdate_d (startdate), enddate_d (enddate)", "dealnum EQ $dealnum" );
	   tDeliveryDates.destroy();

	   /* Fix up the ComFwd start and end date from settle date */
	   tran_ptr_col = tTrans.getColNum("tran_ptr");
	   deal_num_col = tTrans.getColNum("deal_num");
	   dealnum_col = returnt.getColNum("dealnum");
	   startdate_col = returnt.getColNum("startdate");
	   enddate_col = returnt.getColNum("enddate");
	   
	   iNumRows = tTrans.getNumRows();
	   for  (iRow = 1; iRow <= iNumRows; iRow++)
	   {
	      TranPointer = tTrans.getTran( tran_ptr_col, iRow);
	      iToolset = TranPointer.getFieldInt( TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "", -1);

	      if (iToolset == TOOLSET_ENUM.COM_FWD_TOOLSET.toInt())
	      {
	         settleDate = TranPointer.getFieldInt( TRANF_FIELD.TRANF_SETTLE_DATE.toInt(), 0, "", -1);
	         iFoundRow = returnt.unsortedFindInt( dealnum_col, tTrans.getInt( deal_num_col, iRow));
	         returnt.setDouble( startdate_col, iFoundRow, 0.0 + settleDate);
	         returnt.setDouble( enddate_col, iFoundRow, 0.0 + settleDate);
	      }
	   }

	   //Sorting if AB_APM_QA_MODE is set to true
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
	    	// for error reporting further down
	    	String message = null;
	    	
	        try {
	            // db call
	            iQueryId = Query.tableQueryInsert( tTable, sColumn );
	        } catch (OException exception) {
	            OLF_RETURN_CODE olfReturnCode = exception.getOlfReturnCode();
	            
	            if (olfReturnCode == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR) {
	                numberOfRetriesThusFar++;
	                
                    if(message == null) {
                        message = String.format("Query execution retry %1$d of %2$d. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES);
                    } else {
                        message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES, message);
                    }
	                
	                Debug.sleep(numberOfRetriesThusFar * 1000);
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
