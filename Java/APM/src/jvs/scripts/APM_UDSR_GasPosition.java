/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
 * File Name:           APM_UDSR_GasPosition.java
 * Author:              Maksim Stseglov
 * Creation Date:       Aug 13, 2008
 * Revision History:    14 July 2011 - Amended so that result takes parameter of Volume types     
 * Last Update:         24-April-2017 - Converted from AVS to OpenJVS
 * Script Category:     Simulation Result                                           
 * Script Type:         User defined simulation result 
 * Description:         APM Gas Position result
 */

package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.Transaction;

public class APM_UDSR_GasPosition implements IScript
{

   public void execute(IContainerContext arg0) throws OException
   {
      int intOperation;
      Table argt = Util.NULL_TABLE, returnt = Util.NULL_TABLE;

      argt = arg0.getArgumentsTable();
      returnt = arg0.getReturnTable();
      intOperation = argt.getInt("operation", 1);
      /* Call the virtual functions according to action type. */
      if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
      {
         computeResult(argt, returnt);
      } else if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
      {
         formatResult();
      }
   }

   /*
    * Name:             getStrategyInfoFilters           
    * Description:      Returns all enabled INS_TYPE_ENUM.strategy info filters.   
    * Parameters:
    * Return Values:    
    */
   private Table getStrategyInfoFilters() throws OException
   {
      int iRetVal;
      Table tEnabledStrategyInfoFilters;
      String sQuery;

      OConsole.oprint("Loading APM strategy_info filter/splitter configuration from the database \n");

      /* No details of enable APM INS_TYPE_ENUM.strategy info filters provides, so load them up based on APM Configuration.class */
      tEnabledStrategyInfoFilters = Table.tableNew("Enabled Strategy Info Filters");

      sQuery = "select distinct " + "tfd.filter_id, " + "tfd.filter_name, " + "tfd.ref_list_id, "
            + "aesr.result_column_name, " + "aesr.column_name_append, " + "tfd.filter_type " + "from "
            + "tfe_filter_defs tfd, " + "apm_pkg_enrichment_config apec, " + "apm_enrichment_source_results aesr "
            + "where " + "tfd.filter_type in ( 10, 11 ) and " + "tfd.filter_name = apec.enrichment_name and "
            + "apec.on_off_flag = 1 and " + "aesr.enrichment_name = apec.enrichment_name ";

      iRetVal = DBase.runSql(sQuery);

      if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal,
               "DBase.runSql() failed to load APM strategy_info filter/splitter configuration"));
      else
      {
         iRetVal = DBase.createTableOfQueryResults(tEnabledStrategyInfoFilters);
         if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal,
                  "DBase.createTableOfQueryResults() failed to load APM strategy_info filter/splitter configuration"));
      }

      return tEnabledStrategyInfoFilters;
   }

   /*
    * Name:             getLocationInfoFilters           
    * Description:      Returns all enabled location info filters.   
    * Parameters:      
    * Return Values:    
    */
   private Table getLocationInfoFilters() throws OException
   {
      int iRetVal;
      Table tEnabledInfoFilters;
      String sQuery;

      OConsole.oprint("Loading APM location_info filter/splitter configuration from the database \n");

      /* No details of enable APM location info filters provides, so load them up based on APM configuration. */
      tEnabledInfoFilters = Table.tableNew("Enabled Location Info Filters");

      sQuery = "select distinct " + "tfd.filter_id, " + "tfd.filter_name, " + "tfd.ref_list_id, "
            + "aesr.result_column_name, " + "aesr.column_name_append, " + "tfd.filter_type " + "from "
            + "tfe_filter_defs tfd, " + "apm_pkg_enrichment_config apec, " + "apm_enrichment_source_results aesr "
            + "where " + "tfd.filter_type = 16 and " + "tfd.filter_name = apec.enrichment_name and "
            + "apec.on_off_flag = 1 and " + "aesr.enrichment_name = apec.enrichment_name ";

      iRetVal = DBase.runSql(sQuery);

      if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal,
               "DBase.runSql() failed to load APM strategy_info filter/splitter configuration"));
      else
      {
         iRetVal = DBase.createTableOfQueryResults(tEnabledInfoFilters);
         if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal,
                  "DBase.createTableOfQueryResults() failed to load APM strategy_info filter/splitter configuration"));
      }

      return tEnabledInfoFilters;
   }

   private void gatherLocationInfo(int iQueryId, Table tEnabledLocationInfoFilters, Table returnt) throws OException
   {
      Table tInfoFilters;
      String sFrom, sWhat, sWhere, sColumnName, sColumnNameAppend;
      int iRetVal, iRow, iFilterId, iTranInfoId;
      int numFilters = 0;

      if (tEnabledLocationInfoFilters.getNumRows() > 0)
      {
         sWhat = "DISTINCT qr.query_result tran_num, gp.param_seq_num";
         sFrom = "query_result qr, ab_tran ab, header h, gas_phys_param gp, phys_header ph, gas_phys_location loc ";

         for (iRow = 1; iRow <= tEnabledLocationInfoFilters.getNumRows(); iRow++)
         {

            numFilters = numFilters + 1;
            iFilterId = tEnabledLocationInfoFilters.getInt("filter_id", iRow);
            iTranInfoId = tEnabledLocationInfoFilters.getInt("ref_list_id", iRow);
            sColumnName = tEnabledLocationInfoFilters.getString("result_column_name", iRow);
            sColumnNameAppend = tEnabledLocationInfoFilters.getString("column_name_append", iRow);

            sWhat = sWhat + ", loi_" + iFilterId + sColumnNameAppend + ".info_value " + "\"" + sColumnName + "\"";
            sFrom = sFrom + " left outer join loc_info loi_" + iFilterId + sColumnNameAppend;
            sFrom = sFrom + " on (loc.location_id = loi_" + iFilterId + sColumnNameAppend + ".location_id AND loi_"
                  + iFilterId + sColumnNameAppend + ".type_id = " + iTranInfoId + ") ";

         }

         sWhere = "qr.unique_id = " + iQueryId + " and qr.query_result = ab.tran_num ";
         sWhere = sWhere
               + " and ab.ins_num = gp.ins_num and  ab.ins_num = ph.ins_num  and  ab.ins_num =  h.ins_num  and  gp.ins_num = ph.ins_num  and  gp.ins_num =  h.ins_num  and  ph.ins_num =  h.ins_num  and loc.location_id = gp.location_id ";

         if (numFilters > 0)
         {

            tInfoFilters = Table.tableNew("Location Info");

            iRetVal = APM_TABLE_LoadFromDbWithSQL(tInfoFilters, sWhat, sFrom, sWhere);

            if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            {
               returnt.select(tInfoFilters, "*", "tran_num EQ $tran_num AND param_seq_num EQ $param_seq_num");
            }

            tInfoFilters.destroy();
         }
      }
   }

   private void gatherStrategyInfo(int iQueryId, Table tEnabledStrategyInfoFilters, Table returnt) throws OException
   {
      Table tStrategyInfo;
      String sFrom, sWhat, sWhere, sColumnName, sColumnNameAppend;
      int iRetVal, iRow, iFilterId, iTranInfoId, iFilterType;
      int numFilters = 0;

      if (tEnabledStrategyInfoFilters.getNumRows() > 0)
      {
         sWhat = "qr.query_result tran_num";
         sFrom = "query_result qr, ab_tran";

         for (iRow = 1; iRow <= tEnabledStrategyInfoFilters.getNumRows(); iRow++)
         {
            /* only INS_TYPE_ENUM.strategy info here. */
            if (tEnabledStrategyInfoFilters.getInt("filter_type", iRow) != 10
                  && tEnabledStrategyInfoFilters.getInt("filter_type", iRow) != 11)
               continue;

            numFilters = numFilters + 1;
            iFilterId = tEnabledStrategyInfoFilters.getInt("filter_id", iRow);
            iTranInfoId = tEnabledStrategyInfoFilters.getInt("ref_list_id", iRow);
            sColumnName = tEnabledStrategyInfoFilters.getString("result_column_name", iRow);
            sColumnNameAppend = tEnabledStrategyInfoFilters.getString("column_name_append", iRow);
            iFilterType = tEnabledStrategyInfoFilters.getInt("filter_type", iRow);

            sWhat = sWhat + ", abt_" + iFilterId + sColumnNameAppend + ".value " + "\"" + sColumnName + "\"";
            sFrom = sFrom + " left outer join ab_tran_info abt_" + iFilterId + sColumnNameAppend;

            if (iFilterType == 10)
            {
               sFrom = sFrom + " on (ab_tran.int_trading_strategy = abt_" + iFilterId + sColumnNameAppend
                     + ".tran_num AND abt_" + iFilterId + sColumnNameAppend + ".type_id = " + iTranInfoId + ")";
            } else if (iFilterType == 11)
            {
               sFrom = sFrom + " on (ab_tran.ext_trading_strategy = abt_" + iFilterId + sColumnNameAppend
                     + ".tran_num AND abt_" + iFilterId + sColumnNameAppend + ".type_id = " + iTranInfoId + ")";
            }
         }

         sWhere = "qr.unique_id = " + iQueryId + " and qr.query_result = ab_tran.tran_num";

         // if none switched on then skip it
         if (numFilters > 0)
         {

            tStrategyInfo = Table.tableNew("Strategy Info");

            iRetVal = APM_TABLE_LoadFromDbWithSQL(tStrategyInfo, sWhat, sFrom, sWhere);

            if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            {
               returnt.select(tStrategyInfo, "*", "tran_num EQ $tran_num");
            }

            tStrategyInfo.destroy();
         }
      }
   }

   private void LogDebugMessage(String sProcessingMessage) throws OException
   {
      String msg;
      msg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " "
            + Util.timeGetServerTimeHMS() + " : " + sProcessingMessage + "\n";
      OConsole.oprint(msg);
   }

   private int GetParamDateValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow, iDate;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);
      iDate = OCalendar.parseString(tResConfig.getString("value", iRow));

      return iDate;
   }

   private int GetParamIntValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow, iVal;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);
      iVal = Str.strToInt(tResConfig.getString("value", iRow));

      return iVal;
   }

   private String GetParamStrValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow;
      String sVal;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);
      sVal = tResConfig.getString("value", iRow);

      return sVal;
   }

   private int ParamHasValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow;
      String sValue;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);
      sValue = tResConfig.getString("value", iRow);

      if ((sValue.length() > 0) && !sValue.isEmpty())
      {
         return 1;
      }

      return 0;
   }

   /*
    * Name:             APM_TABLE_ExecISql
    * Description:      deadlock protected version of the fn
    * Parameters:       As per DBaseTable.execISql
    * Return Values:    iRetVal (success or failure)
    * Effects:   <any *>
    */
   private int APM_TABLE_ExecISql(Table table, String sql_query, int max_rows) throws OException
   {
      int iRetVal;
      int iAttempt;
      int nAttempts = 10;

      iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
      for (iAttempt = 0; (iAttempt == 0)
            || ((iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt()) && (iAttempt < nAttempts)); ++iAttempt)
      {
         if (iAttempt > 0)
            Debug.sleep(iAttempt * 1000);

         iRetVal = DBaseTable.execISql(table, sql_query, max_rows);
      }

      if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.execISql failed "));
      }

      return iRetVal;
   }

   /*
    * Name:          addSelectedVolumeTypesIfShowAll
    * Description:   Changes SQL to add only selected volume statuses if show all is selected
    * Return Values:   retval (success or failure)
    */
   private String addSelectedVolumeTypesIfShowAll(int bShowAllVolumeStatuses, Table tResConfig, String sql_query,
         Table tMasterListVolumeTypes) throws OException
   {
      Table volumeTypeList;
      int startStringPosition, endStringPosition, numRowsAdded = 0, iFoundRow, iRow;
      String volumeTypes, temporaryVolumeTypes, volumeTypeStr, volTypesSQL = "", volumeTypesStripped;

      startStringPosition = 1;
      endStringPosition = 1;

      if (bShowAllVolumeStatuses == 0)
         return sql_query;

      volumeTypeList = Util.NULL_TABLE;

      /* work out whether the result has only a subset of the volume types selected. */
      if (ParamHasValue(tResConfig, "APM Gas Position Volume Types") > 0)
      {
         volumeTypes = GetParamStrValue(tResConfig, "APM Gas Position Volume Types");

         /* now split the volume types apart so that we can create an ID comma separated String. */
         volumeTypesStripped = Str.stripBlanks(volumeTypes);
         if (Str.isNull(volumeTypesStripped) == 0 && Str.isEmpty(volumeTypesStripped) == 0)
         {
            volumeTypeList = Table.tableNew("volume_types");
            volumeTypeList.addCol("volume_type", COL_TYPE_ENUM.COL_STRING);
            volumeTypeList.addCol("volume_type_ID", COL_TYPE_ENUM.COL_INT);

            while (endStringPosition > 0)
            {
               startStringPosition = 0;
               endStringPosition = Str.findSubString(volumeTypes, ";");

               numRowsAdded += 1;
               volumeTypeList.addRow();

               if (endStringPosition > 0)
               {
                  volumeTypeList.setString(1, numRowsAdded,
                        Str.substr(volumeTypes, startStringPosition, endStringPosition - startStringPosition));

                  temporaryVolumeTypes = Str.substr(volumeTypes, endStringPosition + 1,
                        Str.len(volumeTypes) - endStringPosition - 1);
                  volumeTypes = temporaryVolumeTypes;
               } else
               {
                  volumeTypeList.setString(1, numRowsAdded,
                        Str.substr(volumeTypes, startStringPosition, Str.len(volumeTypes) - startStringPosition));
               }
            }

            /* if no rows then exit as we should have something. */
            if (volumeTypeList.getNumRows() < 1)
            {
               volumeTypeList.destroy();
               LogDebugMessage(
                     "APM Gas Position Volume Types field populated but no valid values !!! Please correct the simulation result mod\n");
               Util.exitFail();
            }

            /* now we have a table of statuses - next job is to convert them into ID's. */
            for (iRow = 1; iRow <= volumeTypeList.getNumRows(); iRow++)
            {
               volumeTypeStr = Str.toUpper(volumeTypeList.getString(1, iRow));

               // BAV handled separately
               if (Str.equal(volumeTypeStr, "BAV") != 0)
                  continue;

               iFoundRow = tMasterListVolumeTypes.findString(1, volumeTypeStr, SEARCH_ENUM.FIRST_IN_GROUP);
               if (iFoundRow > 0)
               {
                  if (volTypesSQL.length() > 0)
                     volTypesSQL = volTypesSQL + ",";

                  volTypesSQL = volTypesSQL + Str.intToStr(tMasterListVolumeTypes.getInt(2, iFoundRow));
               } else
               {
                  volumeTypeList.destroy();
                  LogDebugMessage("Cannot find volume type from APM Gas Position Volume Types field: '" + volumeTypeStr
                        + "'. Please correct the simulation result mod\n");
                  Util.exitFail();
               }

            }

            // filter on volume type in comm_schedule_header - note this does not include the BAV type
            if (Str.len(volTypesSQL) > 0)
               sql_query += " and (comm_schedule_header.volume_type in (" + volTypesSQL + ")";

            // check for BAV
            volumeTypeList.sortCol(1);
            iFoundRow = volumeTypeList.findString(1, "BAV", SEARCH_ENUM.FIRST_IN_GROUP);
            if (Str.len(volTypesSQL) > 0) // if BAV required then we need an OR on the bav_flag
            {
               if (iFoundRow > 0)
               {
                  sql_query += " or comm_schedule_detail.bav_flag = 1)";
               } else
                  sql_query += ")";
            } else // only value is BAV - same effect as setting show all statuses = 0
               sql_query += " and comm_schedule_detail.bav_flag = 1";
         }
      }

      if (Table.isTableValid(volumeTypeList) == 1)
         volumeTypeList.destroy();

      return sql_query;
   }

   private void computeResult(Table argt, Table returnt) throws OException
   {
      int max_rows = 999999999;

      String sql_query, strStartingPeriod, strStoppingPeriod, sCachedTableName, sSourceOfQuantity, sWhatStrategy;
      int iQueryId = 0, iRow, row, retval, iResultRow, iResultID, iStartDate, iEndDate, iStartTime, iEndTime;
      int iInsType, iBuySell, iPayRec;
      int iResult, iLoop;
      int iEnabledStrategyInfoFilterDetailsCol, iEnabledLocationInfoFilterDetailsCol;
      int strategyListingPresent = 0;

      Table tTrans, tListResultTypes, tResConfig, tAllTrans, tEnabledStrategyInfoFilters, tEnabledLocationInfoFilters,
            tSimDef, tStrategies;
      Transaction tTransaction;
      double quantity, dHours;
      ODateTime dtStartingPeriod, dtStoppingPeriod;

      Table tSourceOfQuantity, tSourceOfStrategy, tVersion;
      int major_version, minor_version, code_revision;

      Table tMasterListVolumeTypes;
      int bShowAllVolumeStatuses;
      String volumeTypeStr;

      bShowAllVolumeStatuses = 1;

      retval = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();

      tSimDef = argt.getTable("sim_def", 1);

      // output table format
      returnt.addCol("deal_tracking_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("ins_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("ins_type", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("delivery_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("location_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gmt_start_date_time", COL_TYPE_ENUM.COL_DATE_TIME);
      returnt.addCol("gmt_end_date_time", COL_TYPE_ENUM.COL_DATE_TIME);
      returnt.addCol("volume_type", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("bav_flag", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("quantity", COL_TYPE_ENUM.COL_DOUBLE);
      returnt.addCol("total_quantity", COL_TYPE_ENUM.COL_DOUBLE);
      returnt.addCol("bav_quantity", COL_TYPE_ENUM.COL_DOUBLE);
      returnt.addCol("total_hours", COL_TYPE_ENUM.COL_DOUBLE);
      returnt.addCol("unit", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("param_seq_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("profile_seq_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("proj_index", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("pay_rec", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("startdate", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("enddate", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("start_time", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("end_time", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("service_type", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("pipeline_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("zone_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("meter_id", COL_TYPE_ENUM.COL_STRING);
      returnt.addCol("vpool_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("loc_long_name", COL_TYPE_ENUM.COL_STRING);
      returnt.addCol("idx_subgroup", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("location_type", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("measure_group_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("inj_wth", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("int_str", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("ext_str", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("int_str_s", COL_TYPE_ENUM.COL_STRING);
      returnt.addCol("ext_str_s", COL_TYPE_ENUM.COL_STRING);

      returnt.setColFormatAsRef("location_id", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
      returnt.setColFormatAsRef("pipeline_id", SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
      returnt.setColFormatAsRef("service_type", SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);
      returnt.setColFormatAsRef("zone_id", SHM_USR_TABLES_ENUM.GAS_PHYS_ZONE_TABLE);
      returnt.setColFormatAsRef("vpool_id", SHM_USR_TABLES_ENUM.GAS_PHYS_VALUE_POOL_TABLE);
      returnt.setColFormatAsRef("volume_type", SHM_USR_TABLES_ENUM.GAS_TSD_VOLUME_TYPES_TABLE);
      returnt.setColFormatAsRef("idx_subgroup", SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);
      returnt.setColFormatAsRef("location_type", SHM_USR_TABLES_ENUM.GAS_PHYS_LOC_TYPE);
      returnt.setColFormatAsRef("measure_group_id", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);
      returnt.setColFormatAsRef("proj_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);

      // Prepare the result parameters table - this is our data source for populating the return table 
      sCachedTableName = "Pfolio Result Attrs";
      tListResultTypes = Table.getCachedTable(sCachedTableName);
      if (Table.isTableValid(tListResultTypes) == 0)
      {
         tListResultTypes = Table.tableNew();
         retval = DBase.runSql("select * from pfolio_result_attr_groups");
         if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed"));
         else
         {
            retval = DBase.createTableOfQueryResults(tListResultTypes);
            if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
               OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "DBase.createTableOfQueryResults() failed"));
            else
               Table.cacheTable(sCachedTableName, tListResultTypes);
         }
      }

      if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         argt.setString("error_msg", 1, "Problem when retrieving result attribute groups");
         Util.exitFail();
      }

      // Prepare the master gas volume types table from db - this is used to get ID's from specified volume statuses 
      sCachedTableName = "APM Volume Types";
      tMasterListVolumeTypes = Table.getCachedTable(sCachedTableName);
      if (Table.isTableValid(tMasterListVolumeTypes) == 0)
      {
         tMasterListVolumeTypes = Table.tableNew();
         retval = DBase.runSql("select gas_name, id_number from volume_type where gas_active_flag = 1");
         if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed"));
         else
         {
            retval = DBase.createTableOfQueryResults(tMasterListVolumeTypes);
            // uppercase them so that any case sensitive stuff does not rear its ugly head
            for (iRow = 1; iRow <= tMasterListVolumeTypes.getNumRows(); iRow++)
            {
               volumeTypeStr = Str.toUpper(tMasterListVolumeTypes.getString(1, iRow));
               tMasterListVolumeTypes.setString(1, iRow, volumeTypeStr);
            }

            // sort so that the types can be easily found
            tMasterListVolumeTypes.sortCol(1);

            if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
               OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "DBase.createTableOfQueryResults() failed"));
            else
               Table.cacheTable(sCachedTableName, tMasterListVolumeTypes);
         }
      }

      if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         argt.setString("error_msg", 1, "Problem when retrieving master volume types");
         Util.exitFail();
      }

      iResultRow = tListResultTypes.unsortedFindString("res_attr_grp_name", "APM Gas Position",
            SEARCH_CASE_ENUM.CASE_INSENSITIVE);
      iResultID = tListResultTypes.getInt("res_attr_grp_id", iResultRow);

      tResConfig = SimResult.getResultConfig(iResultID);
      tResConfig.sortCol("res_attr_name");

      // If query ID is provided as a parameter, use it!
      if (tSimDef.getColNum("APM Single Deal Query") > 0)
         iQueryId = tSimDef.getInt("APM Single Deal Query", 1);

      // If query ID was not set or left at zero, create a query ID from the list of transactions
      if (iQueryId == 0)
      {
         // Get input information
         iResult = argt.getInt("result_type", 1);
         tAllTrans = argt.getTable("transactions", 1);
         tTrans = tAllTrans.cloneTable();

         // Loop on all transactions
         for (iLoop = tAllTrans.getNumRows(); iLoop > 0; iLoop--)
         {
            tTransaction = tAllTrans.getTran("tran_ptr", iLoop);

            // Keep only valid ones
            if (SimResult.isResultAllowedForTran(iResult, tTransaction) != 0)
               tAllTrans.copyRowAdd(iLoop, tTrans);
         } // for iLoop.

         if (Table.isTableValid(tTrans) == 0 || tTrans.getNumRows() == 0)
         {
            OConsole.oprint("No eligible transactions passed to APM Gas Position sim result\n");
            return;
         }
         iQueryId = Query.tableQueryInsert(tTrans, tTrans.getColNum("tran_num"));
         if (iQueryId <= 0)
         {
            OConsole.oprint("Failed to create query Id in APM Gas Position sim result\n");
            return;
         }
      }

      // Figure out the table in which 'quantity' column is stored - this is Endur version-dependent
      tSourceOfQuantity = Table.getCachedTable("APM_Source_Of_Quantity");
      if (Table.isTableValid(tSourceOfQuantity) == 0)
      {
         tVersion = Ref.getVersion();
         major_version = tVersion.getInt("major_version", 1);
         minor_version = tVersion.getInt("minor_version", 1);
         code_revision = tVersion.getInt("code_revision", 1);
         tVersion.destroy();

         tSourceOfQuantity = Table.tableNew("APM_Source_Of_Quantity");
         tSourceOfQuantity.addCol("source_of_quantity", COL_TYPE_ENUM.COL_STRING);
         tSourceOfQuantity.addRow();

         if ((major_version > 8) || (major_version == 8 && minor_version >= 1)
               || (major_version == 8 && minor_version == 0 && code_revision >= 2000))
         {
            tSourceOfQuantity.setString(1, 1, "comm_schedule_detail");
         } else
         {
            tSourceOfQuantity.setString(1, 1, "comm_schedule_header");
         }

         Table.cacheTable("APM_Source_Of_Quantity", tSourceOfQuantity);
      }

      sSourceOfQuantity = tSourceOfQuantity.getString(1, 1);

      // Figure out whether the strategies are in play - this is Endur version-dependent
      tSourceOfStrategy = Table.getCachedTable("APM_Source_Of_Strategy_GasPos");
      if (Table.isTableValid(tSourceOfStrategy) == 0)
      {
         tVersion = Ref.getVersion();
         major_version = tVersion.getInt("major_version", 1);
         minor_version = tVersion.getInt("minor_version", 1);
         code_revision = tVersion.getInt("code_revision", 1);
         tVersion.destroy();

         tSourceOfStrategy = Table.tableNew("APM_Source_Of_Strategy_GasPos");
         tSourceOfStrategy.addCol("what", COL_TYPE_ENUM.COL_STRING);
         tSourceOfStrategy.addCol("from", COL_TYPE_ENUM.COL_STRING);
         tSourceOfStrategy.addCol("where", COL_TYPE_ENUM.COL_STRING);
         tSourceOfStrategy.addRow();
         tSourceOfStrategy.addRow();

         if ((major_version > 9) || (major_version == 9 && minor_version >= 2)
               || (major_version == 9 && minor_version == 1 && code_revision >= 2000))
         {
            tSourceOfStrategy.setString(1, 1,
                  ", comm_schedule_header.int_strategy_id int_str, comm_schedule_header.ext_strategy_id ext_str");
            tSourceOfStrategy.setString(2, 1, "");
            tSourceOfStrategy.setString(3, 1, "");
            strategyListingPresent = 1;
         } else
         {
            tSourceOfStrategy.setString(1, 1, ", 0 int_str, 0 ext_str");
            tSourceOfStrategy.setString(2, 1, "");
            tSourceOfStrategy.setString(3, 1, "");
         }

         Table.cacheTable("APM_Source_Of_Strategy_GasPos", tSourceOfStrategy);
      }

      sWhatStrategy = tSourceOfStrategy.getString(1, 1);

      // Handle the start and end times
      dtStartingPeriod = ODateTime.dtNew();
      dtStoppingPeriod = ODateTime.dtNew();

      // Set start date to be today, and stop date to be 30 days from now as a default (that should always be overwritten)
      iStartDate = OCalendar.today();
      iEndDate = OCalendar.today() + 30;

      if (ParamHasValue(tResConfig, "APM Gas Position Natural Startpoint") > 0)
      {
         iStartDate = GetParamDateValue(tResConfig, "APM Gas Position Natural Startpoint");
      }

      if (ParamHasValue(tResConfig, "APM Gas Position Natural Endpoint") > 0)
      {
         iEndDate = GetParamDateValue(tResConfig, "APM Gas Position Natural Endpoint");
      }

      dtStartingPeriod.setDateTime(iStartDate, 0);
      dtStoppingPeriod.setDateTime(iEndDate, 0);

      strStartingPeriod = dtStartingPeriod.formatForDbAccess();
      strStoppingPeriod = dtStoppingPeriod.formatForDbAccess();

      sql_query = " select " + "    ab_tran.deal_tracking_num, " + "    ab_tran.tran_num, " + "    ab_tran.ins_num, "
            + "    ab_tran.ins_type, " + "    ab_tran.buy_sell, " + "    comm_schedule_header.delivery_id, "
            + "    comm_schedule_header.location_id, " + "    comm_schedule_detail.gmt_start_date_time, "
            + "    comm_schedule_detail.gmt_end_date_time, " + "    comm_schedule_header.volume_type, "
            + "    comm_schedule_detail.bav_flag, " + "    " + sSourceOfQuantity + ".quantity, " + "    "
            + sSourceOfQuantity + ".quantity total_quantity, " + "    " + sSourceOfQuantity + ".quantity bav_quantity, "
            + "    " + sSourceOfQuantity + ".quantity total_hours, " + "    comm_schedule_header.unit, "
            + "    comm_schedule_header.param_seq_num, " + "    comm_schedule_header.profile_seq_num, "
            + "    parameter.proj_index, " + "    parameter.pay_rec, " + "    parameter.pay_rec startdate, "
            + "    parameter.pay_rec enddate, " + "    parameter.pay_rec start_time, "
            + "    parameter.pay_rec end_time, " + "    phys_header.service_type, "
            + "    gas_phys_location.pipeline_id, " + "    gas_phys_location.zone_id, "
            + "    gas_phys_location.meter_id, " + "    gas_phys_location.vpool_id, "
            + "    gas_phys_location.loc_long_name, " 
            + "    gas_phys_location.idx_subgroup, " + "    gas_phys_location.location_type, "
            + "    gas_phys_param.measure_group_id, " + "    0 inj_wth " + sWhatStrategy + " from "
            + "    comm_schedule_header, " + "    comm_schedule_detail, " + "    gas_phys_location, "
            + "    gas_phys_param, " + "    phys_header, " + "    ab_tran, " + "    parameter, " + "    query_result "
            + "where " + "    comm_schedule_header.schedule_id = comm_schedule_detail.schedule_id and "
            + "    gas_phys_location.location_id = comm_schedule_header.location_id and "
            + "    comm_schedule_header.ins_num = phys_header.ins_num and "
            + "    ab_tran.ins_num = comm_schedule_header.ins_num and "
            + "    parameter.ins_num = comm_schedule_header.ins_num and "
            + "    parameter.param_seq_num = comm_schedule_header.param_seq_num and "
            + "    parameter.ins_num = gas_phys_param.ins_num and parameter.param_seq_num = gas_phys_param.param_seq_num and "
            + "    comm_schedule_header.total_quantity != 0.0 and " // Don't want zero-valued entries cluttering the result
            + "    query_result.query_result = ab_tran.tran_num and " + "    '" + strStartingPeriod
            + "' < comm_schedule_detail.gmt_end_date_time and " + "    '" + strStoppingPeriod
            + "' > comm_schedule_detail.gmt_start_date_time and " + "    query_result.unique_id = " + iQueryId;

      // If we don't have "show all volume statuses" set, only show BAV
      if ((ParamHasValue(tResConfig, "APM Gas Position Show All Volume Statuses") == 0)
            || (GetParamIntValue(tResConfig, "APM Gas Position Show All Volume Statuses") == 0))
      {
         sql_query += " and comm_schedule_detail.bav_flag = 1";
         bShowAllVolumeStatuses = 0;
      }

      sql_query = addSelectedVolumeTypesIfShowAll(bShowAllVolumeStatuses, tResConfig, sql_query,
            tMasterListVolumeTypes);

      retval = APM_TABLE_ExecISql(returnt, sql_query, max_rows);

      for (iRow = 1; iRow <= returnt.getNumRows(); iRow++)
      {
         iInsType = APM_GetBaseInsType(returnt.getInt("ins_type", iRow));
         iBuySell = returnt.getInt("buy_sell", iRow);
         iPayRec = returnt.getInt("pay_rec", iRow);

         // Fix up the signage by pay/receive flag
         if (iPayRec == 1)
         {
            quantity = returnt.getDouble("quantity", iRow);
            quantity = -(quantity);
            returnt.setDouble("quantity", iRow, quantity);
         }

         // Fix up Injection/Withdrawal same way as "Gas Info" script
         // This would set incorrect values on financial legs, but this UDSR only gives info on physical amounts
         if ((iInsType == 48030) && (iBuySell != iPayRec))
         {
            returnt.setInt("inj_wth", iRow, 2);
         } else if ((iInsType == 48030) && (iBuySell == iPayRec))
         {
            returnt.setInt("inj_wth", iRow, 1);
         }

         // Fix up the dates by converting to a date+time combo
         iStartDate = returnt.getDate("gmt_start_date_time", iRow);
         iStartTime = returnt.getTime("gmt_start_date_time", iRow);

         returnt.setInt("startdate", iRow, iStartDate);
         returnt.setInt("start_time", iRow, iStartTime);

         // For end time, we want to change 32767:0 to 32766:86400 for better bucketing on client
         iEndDate = returnt.getDate("gmt_end_date_time", iRow);
         iEndTime = returnt.getTime("gmt_end_date_time", iRow);

         if (iEndTime == 0)
         {
            iEndDate--;
            iEndTime = 86400;
         }

         returnt.setInt("enddate", iRow, iEndDate);
         returnt.setInt("end_time", iRow, iEndTime);

         // Now, calculate the difference between start and end times in hours
         dHours = (iEndDate - iStartDate) * 24 + (iEndTime - iStartTime) / 3600;

         returnt.setDouble("total_hours", iRow, dHours);
      }

      returnt.mathMultCol("quantity", "total_hours", "total_quantity");
      returnt.mathMultCol("total_quantity", "bav_flag", "bav_quantity");

      iEnabledLocationInfoFilterDetailsCol = tSimDef.getColNum("APM Enabled Location Info Filters");

      if (iEnabledLocationInfoFilterDetailsCol > 0)
         tEnabledLocationInfoFilters = tSimDef.getTable(iEnabledLocationInfoFilterDetailsCol, 1);
      else
         tEnabledLocationInfoFilters = getLocationInfoFilters();

      gatherLocationInfo(iQueryId, tEnabledLocationInfoFilters, returnt);

      if (strategyListingPresent == 1)
      {
         tStrategies = Table.tableNew();
         APM_TABLE_LoadFromDbWithSQL(tStrategies, "strategy_id, strategy_name", "strategy_listing", "strategy_id > 0");
         // add a default row for None
         row = tStrategies.addRow();
         tStrategies.setInt(1, row, 0);
         tStrategies.setString(2, row, "None");

         returnt.select(tStrategies, "strategy_name (int_str_s)", "strategy_id EQ $int_str");
         returnt.select(tStrategies, "strategy_name (ext_str_s)", "strategy_id EQ $ext_str");
         tStrategies.destroy();

         // If the UDSR was run beneath an APM Service, then the SimDef will include details of
         // the enabled APM tran info filters based on cached APM filter configuration details
         iEnabledStrategyInfoFilterDetailsCol = tSimDef.getColNum("APM Enabled Tran Info Filters");

         if (iEnabledStrategyInfoFilterDetailsCol > 0)
            tEnabledStrategyInfoFilters = tSimDef.getTable(iEnabledStrategyInfoFilterDetailsCol, 1);
         else
            tEnabledStrategyInfoFilters = getStrategyInfoFilters();

         gatherStrategyInfo(iQueryId, tEnabledStrategyInfoFilters, returnt);

         // cleanup if necessary
         if (iEnabledStrategyInfoFilterDetailsCol <= 0)
            tEnabledStrategyInfoFilters.destroy();
      }

      // If query ID is provided as a parameter, somebody else should free it
      if ((tSimDef.getColNum("APM Single Deal Query") <= 0) || (tSimDef.getInt("APM Single Deal Query", 1) == 0))
      {
         Query.clear(iQueryId);
      }

   }

   private void formatResult() throws OException
   {
      // Nothing for now
   }

   /*
    * Name:          APM_GetBaseinsForIns()
    * Description:   Gets the base instrument type for an ins type   
    * In V80R1C Instrument.getBaseInsType does not exist.  This fn replicates that functionality.   
    * Return Values: Base ins type
    */
   private int APM_GetBaseInsType(int ins_type) throws OException
   {
      Table user_instruments;
      int retval, row;

      if (ins_type < 1000000)
         return ins_type;

      user_instruments = Table.getCachedTable("APM_BaseInsTypes");
      if (Table.isTableValid(user_instruments) != 0)
      {
         if ((row = user_instruments.findInt(1, ins_type, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
         {
            Table.destroyCachedTable("APM_BaseInsTypes");
            user_instruments = Util.NULL_TABLE;
         } else
            return user_instruments.getInt(2, row);
      }

      user_instruments = Table.tableNew();
      retval = APM_TABLE_LoadFromDbWithSQL(user_instruments, "id_number, base_ins_id", "instruments",
            "id_number >= 1000000");

      if (retval == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() && Table.isTableValid(user_instruments) == 1)
      {
         user_instruments.sortCol("id_number");
         Table.cacheTable("APM_BaseInsTypes", user_instruments);

         if ((row = user_instruments.findInt(1, ins_type, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
            return ins_type;

         return user_instruments.getInt(2, row);
      } else
         return ins_type;
   }

   /*
    * Name:             APM_TABLE_LoadFromDBWithSQL
    * Description:      deadlock protected version of the fn
    * Parameters:       As per TABLE_LoadFromDBWithSQL
    * Return Values:    iRetVal (success or failure)
    */
   private int APM_TABLE_LoadFromDbWithSQL(Table table, String what, String from, String where) throws OException
   {
      int iRetVal;
      int iAttempt;
      int nAttempts = 10;

      iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
      for (iAttempt = 0; (iAttempt == 0)
            || ((iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt()) && (iAttempt < nAttempts)); ++iAttempt)
      {
         if (iAttempt > 0)
            Debug.sleep(iAttempt * 1000);

         iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
      }

      if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed "));
      }

      return iRetVal;
   }
}
