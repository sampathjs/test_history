/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_ParcelInfo.java

Date Of Last Revision:     3-Mar-2015 - New UDSR
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back Parcel info filter information from core db tables.

                            
*/
package com.olf.result;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_ParcelInfo implements IScript {
	
    private static final int MAX_NUMBER_OF_DB_RETRIES = 10;

    int SERVER_MODE_MANUAL = 0;
    int SERVER_MODE_BATCH = 1;
    
    Table scriptDetails;

/*-------------------------------------------------------------------------------
  Description:   Parcel info UDSR Main
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
      Description:   Parcel info result using core db tables.
      -------------------------------------------------------------------------------*/
    void compute_result(Table argt, Table returnt) throws OException
    {
       Table   tSimDef, tEnableParcelInfoFilters, tPassedInFilters;
       int     iEnabledParcelInfoFilterDetailsCol;
       int     apmServerProcessModeColNum, apmServerProcessMode;
       int     retVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
	
       APM_CreateScriptDetails();
       APM_AddScriptDetail("SCRIPT", "APM_UDSR_ParcelInfo");
	
       tSimDef     = argt.getTable( "sim_def", 1);
       
       apmServerProcessMode = SERVER_MODE_MANUAL;
       apmServerProcessModeColNum = tSimDef.getColNum( "APM Run Mode");
       if (apmServerProcessModeColNum  > 0)
           apmServerProcessMode = tSimDef.getInt( apmServerProcessModeColNum, 1);
	
       APM_AddScriptDetail("MODE", Integer.toString(apmServerProcessMode));
	
       tEnableParcelInfoFilters = Table.tableNew("Enabled Parcel Info Filters" );     
	
       iEnabledParcelInfoFilterDetailsCol = tSimDef.getColNum( "APM Enabled Parcel Info Filters");
       if (iEnabledParcelInfoFilterDetailsCol  > 0)
       {
          tPassedInFilters = tSimDef.getTable( iEnabledParcelInfoFilterDetailsCol , 1);
          tEnableParcelInfoFilters.select( tPassedInFilters, "*", "filter_type EQ 19");
       }
       else
       {
          retVal = getParcelInfoFilters(tEnableParcelInfoFilters);
       }
	
       Table tTrans = argt.getTable("transactions", 1);
       int iQid = getQueryID(tTrans, "tran_num");

       if (iQid > 0)
       {
          try
          {
             retVal = gatherParcelInfo(tEnableParcelInfoFilters, returnt, iQid);
          }
          catch (OException exception) {
             retVal = exception.getOlfReturnCode().toInt();
          }          
          finally
          {
             Query.clear(iQid);             
          }
       }
       
       tEnableParcelInfoFilters.destroy();
	
       if ( retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
           throw new OException("Failed to load APM parcel_info.");
    }
	
    /*-------------------------------------------------------------------------------
      Description: Returns all enabled parcel info filters.   
      -------------------------------------------------------------------------------*/
    int getParcelInfoFilters(Table tEnableParcelInfoFilters) throws OException
    {
       String sQuery;
       int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
       
       APM_Print ("Loading APM parcel_info filter/splitter configuration from the database");
	
       sQuery = "select distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, tfd.filter_type " + 
                "from  tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr " +
                "where tfd.filter_type = 19 and " +
                "tfd.filter_name = apec.enrichment_name and " +
                "apec.on_off_flag = 1 and " +
                "aesr.enrichment_name = apec.enrichment_name ";
       
       iRetVal = DBase.runSql( sQuery  ); /* we don't care about deadlocks here as this will only get called if run outside of APM */
	
       if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
           APM_Print (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runSql() failed to load APM parcel_info filter/splitter configuration" ) );
       else
       {      
          iRetVal = DBase.createTableOfQueryResults( tEnableParcelInfoFilters );
          if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
              APM_Print (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.createTableOfQueryResults() failed to load APM parcel_info filter/splitter configuration" ) );      
       }
       
       return iRetVal;	
    } 
	
	
    /*-------------------------------------------------------------------------------
      Description: Gathers values for all enabled parcel info filters.  
      -------------------------------------------------------------------------------*/
    int gatherParcelInfo(Table tEnableParcelInfoFilters, Table tData, int iQueryID) throws OException
    {
       String   sFrom, sWhat, sWhere, sColumnName;
       int      iRow, iFilterId, iTranInfoId, iRetVal;
       int      numFilters = 0;
       Table    tParcelInfo;
	
       iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
       if (tEnableParcelInfoFilters.getNumRows() > 0)
       {
           tParcelInfo = Table.tableNew("parcel_info");
           sWhat = "pd.parcel_id ";
           sFrom = "parcel_data pd join ab_tran ab on ab.ins_num = pd.ins_num join query_result qr on ab.tran_num = qr.query_result ";
           sWhere = "qr.unique_id = " + iQueryID;
	
           for (iRow = 1; iRow <= tEnableParcelInfoFilters.getNumRows(); iRow++)
           {
                numFilters = numFilters + 1;
                iFilterId = tEnableParcelInfoFilters.getInt( "filter_id", iRow);
                iTranInfoId = tEnableParcelInfoFilters.getInt( "ref_list_id", iRow);
                sColumnName = tEnableParcelInfoFilters.getString( "result_column_name", iRow);
	
                sWhat = sWhat + ", pi_" + iFilterId + ".info_value " + "\"" + sColumnName + "\"";
                sFrom = sFrom + " left outer join parcel_info pi_" + iFilterId;
                sFrom = sFrom + " on (pd.parcel_id = pi_" + iFilterId + ".parcel_id AND pi_" + iFilterId + ".type_id = " + iTranInfoId + ") ";
           } /* next iRow */
	
           iRetVal = APM_TABLE_LoadFromDbWithSQL( tParcelInfo, sWhat, sFrom, sWhere);
           if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
        	   APM_Print (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL() failed to load APM parcel_data" ) );
	
           tData.select( tParcelInfo, "*", "parcel_id GT 0");
           tParcelInfo.destroy();
       }
	
       return iRetVal;
    }
	
    /*-------------------------------------------------------------------------------
      Description:   UDSR format function. (Default Formatting used)
      -------------------------------------------------------------------------------*/
    void format_result(Table returnt) throws OException
    {
       return;
    }
	
    /*-------------------------------------------------------------------------------
      Description:   deadlock protected version of the fn
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
      Description:   Insert a range of values from a table as a new query result.
      -------------------------------------------------------------------------------*/
    int getQueryID( Table tTable, String sColumn ) throws OException
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
      Description:   makes sure date time added into msg
      -------------------------------------------------------------------------------*/
    void APM_Print(String sProcessingMessage) throws OException
    {
       String sMsg;
       int row, iNumRows;
       sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ": ";
	
       iNumRows = scriptDetails.getNumRows();
       for (row = 1; row <= iNumRows; row++)
       {
          sMsg = sMsg + "[" + scriptDetails.getString( 1, row) + ":" + scriptDetails.getString( 2, row) + "]";
       }
       sMsg = sMsg + " => " + sProcessingMessage + "\n";
       OConsole.oprint(sMsg);
    }
	
    /*-------------------------------------------------------------------------------
      Description:   creates a table containing context
      -------------------------------------------------------------------------------*/
    void APM_CreateScriptDetails() throws OException
    {
       scriptDetails = Table.tableNew();
       scriptDetails.addCols( "S(context)  S(value)");
    }
	
    /*-------------------------------------------------------------------------------
      Description:   sets some context info
      -------------------------------------------------------------------------------*/
    void APM_AddScriptDetail(String context, String value) throws OException
    {
       int row;
       
       row = scriptDetails.addRow();
       scriptDetails.setString( 1, row, context);
       scriptDetails.setString( 2, row, value);
    }
    
}
