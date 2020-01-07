
/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_IndexInfo.java

Date Of Last Revision:     30-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result 
Script Type:               Main
Description:               User defined Sim Result which returns back index data by calling
                           core function Index.listAllIndexes

                            
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_IndexInfo implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
   
/* the number of retries to attempt before giving up when querying the database
   we wrap database calls when they hit vulnerable tables (those subject to rapid change), e.g. ab_tran */
int nAttempts = 10;

/* Message levels (only used in calls to APM_Print */
int      cMsgLevelDebug       = 1;
int      cMsgLevelInfo        = 2;
int      cMsgLevelError       = 3;


/* Function prototype definitions */
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
Parameters:    Table argt, Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result(Table argt, Table returnt) throws OException
{
   Table tArgumentTable;
   Table tValidatedIndexes;
   Table tIndexModifiedTable;
   Table tCachedTime;
   Table tEnabledIndexInfoFilters;
   Table tSimDef;          /* Not freeable */

   ODateTime  currentTime;
   ODateTime  cachedTime;
   int      iRetVal;
   int      iDoIndexUpdate = 1;
   int      iQueryId=0;
   int      iEnabledIndexInfoFilterDetailsCol;

   int      currentDay;
   int      currentSeconds;
   int      cachedDay;
   int      cachedSeconds;

   /* Create a temporary argument table. */
   tArgumentTable = Table.tableNew("APM Argument Table");
   APM_CreateArgumentTable(tArgumentTable);

   /* Create a table to hold the last update date */
   tIndexModifiedTable = Table.tableNew("Modified Time");
   tIndexModifiedTable.addCol( "last_update", COL_TYPE_ENUM.COL_DATE_TIME );

   tSimDef     = argt.getTable( "sim_def", 1);

   /* Get the last time the indices were updated and a value to icons_enum.check against. */     
   iRetVal = APM_TABLE_LoadFromDbWithSQL(tArgumentTable, tIndexModifiedTable, "max(last_update)", "idx_def", "1 = 1" );

   if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() && Table.isTableValid(tIndexModifiedTable) != 0)
   {      
      currentTime = tIndexModifiedTable.getDateTime( 1, 1); 

      tCachedTime = Table.getCachedTable("validated_indexes_cached_time"); 
   
      if(tCachedTime == null)
      {
         /* Create a new cached table to contain the cached time. */
         tCachedTime = Table.tableNew( "validated_indexes_cached_time" );      
         tCachedTime.addCol( "last_update", COL_TYPE_ENUM.COL_DATE_TIME );
         tCachedTime.addRow();
         tCachedTime.setDateTime( 1, 1, currentTime );
         Table.cacheTable( "validated_indexes_cached_time", tCachedTime.copyTable()); 
      }
      else
      {
         /* Get the last time, and icons_enum.check against the current time. */
         cachedTime = tCachedTime.getDateTime( 1, 1 );
         
         currentDay = currentTime.getDate();
         currentSeconds = currentTime.getTime();
         cachedDay = cachedTime.getDate();
         cachedSeconds = cachedTime.getTime();               

         if ((currentDay > cachedDay) || ((currentDay == cachedDay) && (currentSeconds > cachedSeconds)))
         {
            /* There is a newer time than cached, save it and we'll do an update. */
            tCachedTime.setDateTime( 1, 1, currentTime );
         }     
         else
         {
            /* No need for an update. */
            iDoIndexUpdate = 0;
         }    
      }  
   }
   else
   {
      /* If we can't icons_enum.check the date we're going to have to fall back on the cached values, or create the cache if we don't already have one... */
      APM_PrintErrorMessage(tArgumentTable, "APM_UDSR_IndexInfo: Cannot get last update time for idx_def, will not be able to refreshed cached indices, please retry." );
   }

   /* Get the validated indices */

   tValidatedIndexes = Table.getCachedTable("validated_indexes"); 
   if (tValidatedIndexes == null || Table.isTableValid(tValidatedIndexes)==0 || iDoIndexUpdate==1 )
   {
      if(Table.isTableValid(tValidatedIndexes) != 0)
      {
         Table.destroyCachedTable( "validated_indexes" );
      }

      tValidatedIndexes = Index.listAllIndexes(); /* assumed does not return a new'd table */
      if(Table.isTableValid(tValidatedIndexes) != 0)
      {
         Table.cacheTable("validated_indexes", tValidatedIndexes.copyTable()); 
      }
      
      /* Cannot do a TABLE_CopyTable as returnt needs to be original table created. */
      returnt.select( tValidatedIndexes, "DISTINCT, index_id, market, delivery_type, idx_group, idx_subgroup, format, component, unit, density_adjustment, contract_size", "index_id GE 0");
   
      tValidatedIndexes.destroy();
   }
   else
   {
      /* Cannot do a TABLE_CopyTable as returnt needs to be original table created. */
      returnt.select( tValidatedIndexes, "DISTINCT, index_id, market, delivery_type, idx_group, idx_subgroup, format, component, unit, density_adjustment, contract_size", "index_id GE 0");
   }  

   /* If the UDSR was run beneath an APM Service, then the SimDef will include details of
      the enabled APM tran info filters based on cached APM filter configuration details */
   iEnabledIndexInfoFilterDetailsCol = tSimDef.getColNum( "APM Enabled Tran Info Filters");
   if (iEnabledIndexInfoFilterDetailsCol > 0)
      tEnabledIndexInfoFilters = tSimDef.getTable( iEnabledIndexInfoFilterDetailsCol, 1);
   else
      tEnabledIndexInfoFilters = getIndexInfofilters();

   GatherIndexInfo(iQueryId, tEnabledIndexInfoFilters, returnt);

   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
   if (envVar != null)
   {
		envVar = envVar.toUpperCase();
	  
		if (envVar.equals("TRUE"))
		{   
			returnt.clearGroupBy ();
			returnt.addGroupBy ("index_id");
			returnt.groupBy ();
		}
   }   
   
   /* cleanup if necessary */
   if (iEnabledIndexInfoFilterDetailsCol <= 0) tEnabledIndexInfoFilters.destroy();

   /* Destroy the last modified table */
   tIndexModifiedTable.destroy();

   /* Destroy the temporary argument table */
   tArgumentTable.destroy();
}

Table getIndexInfofilters() throws OException
{
   int iRetVal;
   Table tEnabledIndexInfoFilters;

   OConsole.oprint ("Loading APM index_info filter/splitter configuration from the database \n");
   /* No details of enable APM index info filters provides, so load them up based on APM configuration */
   tEnabledIndexInfoFilters = Table.tableNew("Enabled Index Info Filters" );     
   iRetVal = DBase.runSql("select distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, tfd.filter_type " + 
                          "from tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr " +
                          "where tfd.filter_type in (9) and tfd.filter_name = apec.enrichment_name " +
                          "and apec.on_off_flag = 1 and aesr.enrichment_name = apec.enrichment_name");
   if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runSql() failed to load APM index_info filter/splitter configuration" ) );
   else
   {      
      iRetVal = DBase.createTableOfQueryResults(tEnabledIndexInfoFilters);
      if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.createTableOfQueryResults() failed to load APM index_info filter/splitter configuration" ) );      
   }

   return tEnabledIndexInfoFilters;
}   


void GatherIndexInfo(int iQueryId, Table tEnabledIndexInfoFilters, Table returnt) throws OException
{
   Table tIndexInfo;
   String sFrom, sWhat, sWhere, sColumnName;
   int iFilterColId, iRefListId, iResultColumnName;
   int iRetVal, iRow, iFilterId, iIndexInfoId, iFilterTypeCol;
   int iNumRows, numFilters = 0;

   if (tEnabledIndexInfoFilters.getNumRows() > 0)
   {
      /*
        For :
         select id.index_id, idxi_20001.info_value fltr_50000, idxi_20002.info_value fltr_50001
         from idx_def id
         left outer join idx_info idxi_20001 on (id.index_version_id = idxi_20001.index_id and idxi_20001.type_id = 20001)
         left outer join idx_info idxi_20002 on (id.index_version_id = idxi_20002.index_id and idxi_20002.type_id = 20002)
         where id.db_status = 1

      */
      sWhat = "id.index_id";
      sFrom = "idx_def id";
      
      iFilterColId = tEnabledIndexInfoFilters.getColNum( "filter_id");
      iRefListId = tEnabledIndexInfoFilters.getColNum( "ref_list_id");
      iResultColumnName = tEnabledIndexInfoFilters.getColNum( "result_column_name");
      iFilterTypeCol = tEnabledIndexInfoFilters.getColNum( "filter_type");
      iNumRows = tEnabledIndexInfoFilters.getNumRows();
      for (iRow = 1; iRow <= iNumRows; iRow++)
      {
         /* only tran info here (not ins info or param info) */
         if ( tEnabledIndexInfoFilters.getInt( iFilterTypeCol, iRow) != 9 )
            continue;

         numFilters = numFilters + 1;
         iFilterId = tEnabledIndexInfoFilters.getInt( iFilterColId, iRow);
         iIndexInfoId = tEnabledIndexInfoFilters.getInt( iRefListId, iRow);
         sColumnName = tEnabledIndexInfoFilters.getString( iResultColumnName, iRow);
         
         sWhat = sWhat + ", idxi_" + iFilterId + ".info_value " + "\"" + sColumnName + "\"";
         sFrom = sFrom + " left outer join idx_info idxi_" + iFilterId + " on (id.index_version_id = idxi_" + iFilterId + ".index_id AND idxi_" + iFilterId + ".type_id = " + iIndexInfoId + ")";
      }
      sWhere = "id.db_status = 1"; /* Verified */

      /* if none switched on then skip it */
      if ( numFilters == 0 )
         return;
         
      tIndexInfo = Table.tableNew("Index Info");
      iRetVal = APM_TABLE_LoadFromDbWithSQL(tIndexInfo, sWhat, sFrom, sWhere);
      if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      {   
         returnt.select( tIndexInfo, "*", "index_id EQ $index_id");
      }
      tIndexInfo.destroy();
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
   int iRetVal;
   int iAttempt;

   iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
   for( iAttempt = 0; (iAttempt == 0 ) || ((iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt()) && (iAttempt < nAttempts)); ++iAttempt )
   {
      if( iAttempt > 0 )
         Debug.sleep( iAttempt * 1000 );

  	  iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
   }

   if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
   {
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed " ) );
   }

   return iRetVal;
}



/*-------------------------------------------------------------------------------
Name:          format_result()
Description:   UDSR format function.
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void APM_CreateArgumentTable(Table tArgumentTable) throws OException
{
   String sLogFilePath;
   String sLogFilename;

   tArgumentTable.addCol( "Log File", COL_TYPE_ENUM.COL_STRING );
   tArgumentTable.addRow();

   String errLogPath = Util.getEnv("AB_ERROR_LOGS_PATH");
   if ( errLogPath == null || errLogPath.isEmpty())
      sLogFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
   else
      sLogFilePath = errLogPath + "/";

   /* default name for the log - proper name found after next call */
   sLogFilename = sLogFilePath + "APM_UDSR_IndexInfo.log";
   tArgumentTable.setString( "Log File", 1, sLogFilename);
}

/*-------------------------------------------------------------------------------
Name:          format_result()
Description:   UDSR format function.
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void format_result(Table returnt) throws OException
{
   returnt.setColFormatAsRef( "index_id",       SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "market",         SHM_USR_TABLES_ENUM.IDX_MARKET_TABLE);
   returnt.setColFormatAsRef( "delivery_type",  SHM_USR_TABLES_ENUM.DELIVERY_TYPE_TABLE);
   returnt.setColFormatAsRef( "idx_group",      SHM_USR_TABLES_ENUM.IDX_GROUP_TABLE);
   returnt.setColFormatAsRef( "idx_subgroup",   SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);
   returnt.setColFormatAsRef( "format",         SHM_USR_TABLES_ENUM.IDX_FORMAT_TABLE);
   returnt.setColFormatAsRef( "component",      SHM_USR_TABLES_ENUM.IDX_COMPONENT_TABLE);
}

/*-------------------------------------------------------------------------------
Name:          APM_TABLE_LoadFromDBWithSQL
Description:   deadlock protected version of the fn
Parameters:      As per TABLE_LoadFromDBWithSQL
Return Values:   retval (success or failure)
Effects:   <any *>
-------------------------------------------------------------------------------*/
int APM_TABLE_LoadFromDbWithSQL(Table tAPMArgumentTable, Table table, String what, String from, String where) throws OException
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
    	APM_PrintErrorMessage (tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed " ));
	
   return iRetVal;
}

/*-------------------------------------------------------------------------------
Name:          APM_Print
Description:   This function should only be called by APM_PrintMessage, APM_PrintDebugMessage or APM_PrintErrorMessage
Parameters:    
Effects:   <any *>
-------------------------------------------------------------------------------*/
void APM_Print(int iMsgLevel, Table tAPMArgumentTable, String sProcessingMessage) throws OException
{
   String sMsg;
   String sLogFilename;
   sMsg = APM_GetFullMsgContext (tAPMArgumentTable);

   sMsg = sMsg + " => " + sProcessingMessage;

   /* Write errors & debug messages to the error log */
   if (iMsgLevel == cMsgLevelDebug || iMsgLevel == cMsgLevelError) 
   {
      sLogFilename = tAPMArgumentTable.getString( "Log File", 1);
      Util.errorWriteString (sLogFilename, sMsg);
   }

   sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + sMsg + "\n";
   OConsole.oprint(sMsg);
}

/*-------------------------------------------------------------------------------
Name:          APM_PrintErrorMessage
Description:   Prints out error type messages
Parameters:    
Return Values:   retval (success or failure)
Effects:   <any *>
-------------------------------------------------------------------------------*/
void APM_PrintErrorMessage(Table tAPMArgumentTable, String sProcessingMessage) throws OException
{
   APM_Print (cMsgLevelError, tAPMArgumentTable, "================================================="); 
   APM_Print (cMsgLevelError, tAPMArgumentTable, sProcessingMessage);
   APM_Print (cMsgLevelError, tAPMArgumentTable, "================================================="); 
}

/*-------------------------------------------------------------------------------
Name:          APM_GetFullMsgContext
Description:   
Parameters:    
Return Values:   retval (success or failure)
Effects:   <any *>
-------------------------------------------------------------------------------*/
String APM_GetFullMsgContext(Table tAPMArgumentTable) throws OException
{
   Table tMsgContext = null;
   int iContextRow;
   String sMsgContext = "";
   String sContextValue;
   
   tMsgContext = tAPMArgumentTable.getTable( "Message Context", 1);
   for (iContextRow = 1; iContextRow <= tMsgContext.getNumRows(); iContextRow++)
   {
      sContextValue = tMsgContext.getString( "ContextValue", iContextRow);
      if (sContextValue.length() > 0)
      {
         sMsgContext = sMsgContext + "[" + tMsgContext.getString( "ContextName", iContextRow) + ": " + sContextValue + "]";
      }
   }
   return sMsgContext;
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

                Debug.sleep(numberOfRetriesThusFar * 1000);
            } else {
                // it's not a retryable error, so leave
                break;
            }
        }
    } while (iQueryId == 0 && numberOfRetriesThusFar < nAttempts);

   return iQueryId;
}




}
