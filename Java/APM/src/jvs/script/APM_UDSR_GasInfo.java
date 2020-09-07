/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
 * File Name:           APM_UDSR_GasInfo.java
 * Last Update:         24-April-2017 - Converted from AVS to OpenJVS
 * Script Category:     Simulation Result
 * Script Type:         User defined simulation result
 * Description:         User defined simulation result which brings back Gas filter information from core db tables.
 */

package jvs.script;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class APM_UDSR_GasInfo implements IScript
{
   @Override
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
         formatResult(returnt);
      }
   }

   /*
    * Name:             computeResult()
    * Description:      Gas filter info result using core db tables.
    * Parameters:      
    * Return Values:   
    */
   private void computeResult(Table argt, Table returnt) throws OException
   {
      Table tScenResult; // Not freeable
      Table tTranResult; // Not freeable
      Table tSupplementary;
      int iQueryId;
      int row, myLeg, myDeal, nextDeal, prevDeal, myVal;
      int iInsType, iBuySell, iPayRec, iBalance;
      String sFrom, sWhat, sWhere;

      Table tSourceOfBalance;
      String sSourceOfBalance;
      int major_version, minor_version;

      Table tDealTranMap, tSimDef, tEnabledLocationInfoFilters;
      int iEnabledLocationInfoFilterDetailsCol;

      tScenResult = argt.getTable("sim_results", 1);
      tTranResult = tScenResult.getTable(1, 1);

      /* We will need tran_num to get the location info. */
      tDealTranMap = argt.getTable("transactions", 1);
      tSimDef = argt.getTable("sim_def", 1);

      returnt.addCol("dealnum", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("ins_num", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("leg", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("source_leg", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_location", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_location_str", COL_TYPE_ENUM.COL_STRING); /* Hidden column to support Index Info-based location values. */
      returnt.addCol("gas_zone", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_pipeline", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_vpool", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_service_type", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_loc_long_name", COL_TYPE_ENUM.COL_STRING);
      returnt.addCol("gas_meter_id", COL_TYPE_ENUM.COL_STRING);
      returnt.addCol("gas_index_subgroup", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("gas_loc_type", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("measure_group_id", COL_TYPE_ENUM.COL_INT);
      returnt.addCol("inj_wth", COL_TYPE_ENUM.COL_INT);

      returnt.tuneGrowth(tTranResult.getNumRows());

      /* Figure out whether the Endur is_balance field is available - this is Endur version-dependent. */
      tSourceOfBalance = Table.getCachedTable("APM_Source_Of_Balance");
      if (Table.isTableValid(tSourceOfBalance) == 0)
      {
         Table tVersion = Ref.getVersion();
         major_version = tVersion.getInt("major_version", 1);
         minor_version = tVersion.getInt("minor_version", 1);
         tVersion.destroy();

         tSourceOfBalance = Table.tableNew("APM_Source_Of_Balance");
         tSourceOfBalance.addCol("source", COL_TYPE_ENUM.COL_STRING);
         tSourceOfBalance.addRow();

         if ((major_version > 8) || (major_version == 8 && minor_version >= 2))
         {
            tSourceOfBalance.setString(1, 1, ", gas_phys_param.is_balance is_balance");
         } else
         {
            tSourceOfBalance.setString(1, 1, ", 0 is_balance");
         }

         Table.cacheTable("APM_Source_Of_Balance", tSourceOfBalance);
      }

      sSourceOfBalance = tSourceOfBalance.getString(1, 1);

      returnt.select(tTranResult, "deal_num (dealnum), ins_num, deal_leg (leg)", "deal_num GE 0");

      /* build up query result to get deals from ab.tran which match our sim result. */
      iQueryId = APM_TABLE_QueryInsertN(tTranResult, "ins_num");

      /* Query onto the database */
      tSupplementary = Table.tableNew("Supplementary Gas Info");

      sWhat = "ab_tran.tran_num, ab_tran.buy_sell, ab_tran.ins_type, ins_parameter.pay_rec, 0 inj_wth, "
            + "gas_phys_param.ins_num ins_num, gas_phys_param.param_seq_num leg, gas_phys_param.param_seq_num source_leg, gas_phys_param.location_id gas_location, "
            + "gas_phys_location.zone_id gas_zone, gas_phys_location.pipeline_id gas_pipeline, gas_phys_location.vpool_id gas_vpool, "
            + "phys_header.service_type gas_service_type, gas_phys_location.loc_long_name gas_loc_long_name, gas_phys_location.meter_id gas_meter_id, "
            + "gas_phys_location.idx_subgroup gas_index_subgroup, gas_phys_location.location_type gas_loc_type, "
            + "gas_phys_param.measure_group_id measure_group_id" + sSourceOfBalance;

      sFrom = "ab_tran, ins_parameter, gas_phys_location, gas_phys_param, phys_header, query_result";

      sWhere = "ab_tran.ins_num = gas_phys_param.ins_num AND ab_tran.ins_num = ins_parameter.ins_num AND ins_parameter.param_seq_num = gas_phys_param.param_seq_num AND "
            + "gas_phys_param.ins_num = phys_header.ins_num AND gas_phys_param.ins_num = query_result.query_result AND "
            + "gas_phys_param.location_id = gas_phys_location.location_id AND query_result.unique_id =" + iQueryId;

      APM_TABLE_LoadFromDbWithSQL(tSupplementary, sWhat, sFrom, sWhere);

      // Now handle the injection-withdrawal values as follows:
      // Leave at 0 to mean "Neither" for everything
      // Then, for COMM-STORAGE (ins_type 48030) deals only (and custom ins types based on it), set these values:
      // One is "Withdrawal", which means that either we buying the deal where physical is received; or selling the deal where physical is paid
      // Two is "Injection", which has inverse logic
      for (row = 1; row <= tSupplementary.getNumRows(); row++)
      {
         iInsType = tSupplementary.getInt("ins_type", row);
         iBuySell = tSupplementary.getInt("buy_sell", row);
         iPayRec = tSupplementary.getInt("pay_rec", row);
         iBalance = tSupplementary.getInt("is_balance", row);

         if (iBalance == 1)
         {
            tSupplementary.setInt("inj_wth", row, 3);
         } else if ((iInsType == INS_TYPE_ENUM.comm_storage.toInt()) && (iBuySell != iPayRec))
         {
            tSupplementary.setInt("inj_wth", row, 2);
         } else if ((iInsType == INS_TYPE_ENUM.comm_storage.toInt()) && (iBuySell == iPayRec))
         {
            tSupplementary.setInt("inj_wth", row, 1);
         }

         /* This sets incorrect values on financial legs, but we fix these later alongside all the gas-related filters. */
      }

      returnt.select(tSupplementary,
            "gas_location, gas_zone, gas_pipeline, gas_vpool, gas_service_type, gas_loc_long_name, gas_meter_id, gas_index_subgroup, gas_loc_type, measure_group_id, inj_wth, source_leg",
            "ins_num EQ $ins_num AND leg EQ $leg");

      returnt.addGroupBy("dealnum");
      returnt.addGroupBy("leg");

      returnt.groupBy();

      /* Now fill out the financial legs (even, 2+), and the fake leg zero. */
      for (row = 1; row <= returnt.getNumRows(); row++)
      {
         myDeal = returnt.getInt("dealnum", row);
         myLeg = returnt.getInt("leg", row);
         myVal = returnt.getInt("gas_location", row);

         if (row > 1)
         {
            prevDeal = returnt.getInt("dealnum", row - 1);
         } else
         {
            prevDeal = -1;
         }

         if (row < returnt.getNumRows())
         {
            nextDeal = returnt.getInt("dealnum", row + 1);
         } else
         {
            nextDeal = -1;
         }

         // Here the leg column stores the correct leg. the source_leg is identical on physical legs of deal
         // For financial legs, source_leg stores the corresponding physical leg from which Gas information will be taken.

         if ((myLeg == 0) && (myVal == 0) && (nextDeal == myDeal))
         {
            /* Enrich with values from leg 1 (next row). */
            returnt.setInt("source_leg", row, returnt.getInt("leg", row + 1));
            returnt.setInt("gas_location", row, returnt.getInt("gas_location", row + 1));
            returnt.setInt("gas_zone", row, returnt.getInt("gas_zone", row + 1));
            returnt.setInt("gas_pipeline", row, returnt.getInt("gas_pipeline", row + 1));
            returnt.setInt("gas_vpool", row, returnt.getInt("gas_vpool", row + 1));
            returnt.setInt("gas_service_type", row, returnt.getInt("gas_service_type", row + 1));
            returnt.setString("gas_loc_long_name", row, returnt.getString("gas_loc_long_name", row + 1));
            returnt.setString("gas_meter_id", row, returnt.getString("gas_meter_id", row + 1));
            returnt.setInt("gas_index_subgroup", row, returnt.getInt("gas_index_subgroup", row + 1));
            returnt.setInt("gas_loc_type", row, returnt.getInt("gas_loc_type", row + 1));
            returnt.setInt("measure_group_id", row, returnt.getInt("measure_group_id", row + 1));
            returnt.setInt("inj_wth", row, returnt.getInt("inj_wth", row + 1));
         } else if (((myLeg % 2) == 0) && (myVal == 0) && (prevDeal == myDeal))
         {
            /* Enrich with the corresponding physical leg's values (previous row). */
            returnt.setInt("source_leg", row, returnt.getInt("leg", row - 1));
            returnt.setInt("gas_location", row, returnt.getInt("gas_location", row - 1));
            returnt.setInt("gas_zone", row, returnt.getInt("gas_zone", row - 1));
            returnt.setInt("gas_pipeline", row, returnt.getInt("gas_pipeline", row - 1));
            returnt.setInt("gas_vpool", row, returnt.getInt("gas_vpool", row - 1));
            returnt.setInt("gas_service_type", row, returnt.getInt("gas_service_type", row - 1));
            returnt.setString("gas_loc_long_name", row, returnt.getString("gas_loc_long_name", row - 1));
            returnt.setString("gas_meter_id", row, returnt.getString("gas_meter_id", row - 1));
            returnt.setInt("gas_index_subgroup", row, returnt.getInt("gas_index_subgroup", row - 1));
            returnt.setInt("gas_loc_type", row, returnt.getInt("gas_loc_type", row - 1));
            returnt.setInt("measure_group_id", row, returnt.getInt("measure_group_id", row - 1));
            returnt.setInt("inj_wth", row, returnt.getInt("inj_wth", row - 1));
         }
      }

      /* Populate tran_num. */
      returnt.select(tDealTranMap, "tran_num", "deal_num EQ $dealnum");

      /* We also add the financial deals' default locations based on their projection index. */
      AddFinancialDealsLocations(iQueryId, returnt);

      iEnabledLocationInfoFilterDetailsCol = tSimDef.getColNum("APM Enabled Location Info Filters");

      if (iEnabledLocationInfoFilterDetailsCol > 0)
         tEnabledLocationInfoFilters = tSimDef.getTable(iEnabledLocationInfoFilterDetailsCol, 1);
      else
         tEnabledLocationInfoFilters = getLocationInfoFilters();

      gatherLocationInfo(tEnabledLocationInfoFilters, returnt);

      Query.clear(iQueryId);
   }

   /*
    * Name:             formatResult()
    * Description:      UDSR format function. (Default Formatting used)
    * Parameters:      
    * Return Values:   
    */
   private void formatResult(Table returnt) throws OException
   {
      returnt.setColFormatAsRef("gas_location", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
      returnt.setColFormatAsRef("gas_zone", SHM_USR_TABLES_ENUM.GAS_PHYS_ZONE_TABLE);
      returnt.setColFormatAsRef("gas_pipeline", SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
      returnt.setColFormatAsRef("gas_vpool", SHM_USR_TABLES_ENUM.GAS_PHYS_VALUE_POOL_TABLE);
      returnt.setColFormatAsRef("gas_service_type", SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);
      returnt.setColFormatAsRef("gas_index_subgroup", SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);
      returnt.setColFormatAsRef("gas_loc_type", SHM_USR_TABLES_ENUM.GAS_PHYS_LOC_TYPE);
      returnt.setColFormatAsRef("measure_group_id", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);
      returnt.setColFormatAsRef("inj_wth", SHM_USR_TABLES_ENUM.INJECTION_WITHDRAWAL_TABLE);

      returnt.colHide("gas_location_str");
      
      returnt.setColTitle("dealnum", "Deal Num");
      returnt.setColTitle("tran_num", "Transaction Num");
      returnt.setColTitle("ins_num", "Instrument Num");
      returnt.setColTitle("leg", "Side");
      returnt.setColTitle("source_leg", "Source Side");
      returnt.setColTitle("gas_location", "Location");
      returnt.setColTitle("gas_zone", "Zone");
      returnt.setColTitle("gas_pipeline", "Pipeline");
      returnt.setColTitle("gas_vpool", "Valuation Pool");
      returnt.setColTitle("gas_service_type", "Service Type");
      returnt.setColTitle("gas_loc_long_name", "Location Name");
      returnt.setColTitle("gas_meter_id", "Meter ID");
      returnt.setColTitle("gas_index_subgroup", "Subgroup");
      returnt.setColTitle("gas_loc_type", "Location Type");
      returnt.setColTitle("measure_group_id", "Measure Group");
      returnt.setColTitle("inj_wth", "Inject Withdraw");
   }

   /*
    * Name:             APM_TABLE_LoadFromDBWithSQL
    * Description:      deadlock protected version of the fn
    * Parameters:       As per TABLE_LoadFromDBWithSQL
    * Return Values:    iRetVal (success or failure)
    * Effects:          <any *>
    */
   private int APM_TABLE_LoadFromDbWithSQL(Table table, String what, String from, String where) throws OException
   {
      int iRetVal;
      int iAttempt;
      int nAttempts = 10;

      iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
      for (iAttempt = 0; (iAttempt == 0) || ((iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt())
            && (iAttempt < nAttempts)); ++iAttempt)
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

   /*
    * Name:             APM_TABLE_QueryInsertN
    * Description:      Insert a range of values from a table as a new query result.
    * Parameters:    
    * Return Values:    iQueryId
    * Effects:          <any *>
    */
   private int APM_TABLE_QueryInsertN(Table tTable, String sColumn) throws OException
   {
      int iQueryId = 0;
      int iAttempt;

      for (iAttempt = 0; (iQueryId <= 0) && (iAttempt < 10); ++iAttempt)
         iQueryId = Query.tableQueryInsert(tTable, sColumn);

      if (iQueryId <= 0)
         OConsole.oprint("Failed to create query ID in APM_TABLE_QueryInsert.  TableName = " + tTable.getTableName()
               + ", Colnum = " + sColumn);

      return iQueryId;
   }

   /*
    * Name:           
    * Description: Returns all enabled location info filters.   
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

   private void gatherLocationInfo(Table tEnabledLocationInfoFilters, Table tData) throws OException
   {
      Table tInfoFilters;
      String sFrom, sWhat, sWhere, sColumnName, sColumnNameAppend, defaultGroup;
      int iRetVal, iRow, iFilterId, iTranInfoId;
      int numFilters = 0;

      if (tEnabledLocationInfoFilters.getNumRows() > 0)
      {
         /* Retrieve the default values for the location type names configured in 
          * the DB. Blank values will be set to these defaults */
         Table tLocInfoTypes = Table.tableNew();

         APM_TABLE_LoadFromDbWithSQL( tLocInfoTypes, "type_id, default_value", "loc_info_types", "default_value  != ' '");
         tLocInfoTypes.addGroupBy(1);
         tLocInfoTypes.groupBy(); 
	  
         sWhat = "loc.location_id gas_location ";
         sFrom = "gas_phys_location loc ";

         for (iRow = 1; iRow <= tEnabledLocationInfoFilters.getNumRows(); iRow++)
         {
            numFilters = numFilters + 1;
            iFilterId = tEnabledLocationInfoFilters.getInt("filter_id", iRow);
            iTranInfoId = tEnabledLocationInfoFilters.getInt("ref_list_id", iRow);
            sColumnName = tEnabledLocationInfoFilters.getString("result_column_name", iRow);
            sColumnNameAppend = tEnabledLocationInfoFilters.getString("column_name_append", iRow);

            int xrow = tLocInfoTypes.findInt(1, iTranInfoId, SEARCH_ENUM.FIRST_IN_GROUP);
            if ( xrow > 0)
            {
				defaultGroup = tLocInfoTypes.getString(2, xrow);
            }
            else
            {
				defaultGroup = "None";
            } 

            sWhat = sWhat +
            ", case when " + "loi_" + iFilterId + ".info_value " + "is null then " + "'" + defaultGroup + "' " +
            "else loi_" + iFilterId + ".info_value " +
            "end as " + sColumnName;
			
            sFrom = sFrom + " left outer join loc_info loi_" + iFilterId + sColumnNameAppend;
            sFrom = sFrom + " on (loc.location_id = loi_" + iFilterId + sColumnNameAppend + ".location_id AND loi_"
                  + iFilterId + sColumnNameAppend + ".type_id = " + iTranInfoId + ") ";
         }

         sWhere = "1 = 1";
         tLocInfoTypes.destroy();
         if (numFilters > 0)
         {
            tInfoFilters = Table.tableNew("Location Info");
            iRetVal = APM_TABLE_LoadFromDbWithSQL(tInfoFilters, sWhat, sFrom, sWhere);

            if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            {
               tData.select(tInfoFilters, "*", "gas_location EQ $gas_location");
            }

            tInfoFilters.destroy();
         }
      }
   }

   private void AddFinancialDealsLocations(int iQueryId, Table tData) throws OException
   {
      int i, infoTypeID = 0;
      Table tFinDeals, tBlankLocDeals, tIndexInfoPresent;
      String sFrom, sWhat, sWhere, sIndexInfoName;

      sIndexInfoName = "'Default Physical Location ID'";
      sWhat = "type_id";
      sFrom = "idx_info_types";
      sWhere = "type_name = " + sIndexInfoName;
      tIndexInfoPresent = Table.tableNew();
      APM_TABLE_LoadFromDbWithSQL(tIndexInfoPresent, sWhat, sFrom, sWhere);
      
      if (Table.isTableValid(tIndexInfoPresent) != 0)
      {
         if (tIndexInfoPresent.getNumRows() == 0)
         {
            tIndexInfoPresent.destroy();
            return;
         } else if (tIndexInfoPresent.getNumRows() == 1)
            infoTypeID = tIndexInfoPresent.getInt(1, 1);
         else if (tIndexInfoPresent.getNumRows() > 1)
         {
            tIndexInfoPresent.destroy();
            APM_Print("Found more than one Index Info entry of name " + sIndexInfoName
                  + ".  If locations for financial deals are required please correct.");
            return;
         }
      }
      tIndexInfoPresent.destroy();

      tFinDeals = Table.tableNew("Fin Deals");
      tBlankLocDeals = Table.tableNew("Blank Loc Deals");

      /* The order of columns matches the final returnt format. */
      sWhat = "DISTINCT ab_tran.deal_tracking_num dealnum, ab_tran.tran_num, ab_tran.ins_num, pa.param_seq_num leg, pa.param_seq_num gas_location, pa.param_seq_num source_leg,ii.info_value gas_location_str,"
            + "gpl.zone_id gas_zone, gpl.pipeline_id gas_pipeline, gpl.vpool_id gas_vpool, 0 gas_service_type, "
            + "gpl.loc_long_name gas_loc_long_name, gpl.meter_id gas_meter_id, gpl.idx_subgroup gas_index_subgroup, "
            + "gpl.location_type gas_loc_type, 0 measure_group_id, 0 inj_wth";

      sFrom = "ab_tran, ins_parameter pa, param_reset_header prh, query_result, idx_info ii, idx_def id, gas_phys_location gpl";

      sWhere = "ab_tran.ins_num = query_result.query_result AND " + "pa.ins_num = ab_tran.ins_num AND "
            + "pa.ins_num = prh.ins_num AND "
            + "prh.param_seq_num = pa.param_seq_num AND prh.param_reset_header_seq_num = 0 AND "
            + "prh.proj_index = id.index_id AND id.db_status = 1 AND id.index_version_id = ii.index_id AND "
            + "ii.type_id = " + infoTypeID + " AND " + "gpl.location_id = ii.info_value AND "
            + "query_result.unique_id =" + iQueryId;

      APM_TABLE_LoadFromDbWithSQL(tFinDeals, sWhat, sFrom, sWhere);

      if (Table.isTableValid(tFinDeals) == 1 && (tFinDeals.getNumRows() > 0))
      {
         /* We retrieved gas location as an Index Info field value, which is a String - convert to int. */
         for (i = 1; i <= tFinDeals.getNumRows(); i++)
         {
            tFinDeals.setInt("gas_location", i, Str.strToInt(tFinDeals.getString("gas_location_str", i)));
         }

         tBlankLocDeals.select(tData, "dealnum, tran_num, ins_num, leg", "gas_location EQ 0");
         tBlankLocDeals.select(tFinDeals, "*", "dealnum EQ $dealnum AND tran_num EQ $tran_num AND leg EQ $leg");
         tData.select(tBlankLocDeals, "*", "dealnum EQ $dealnum AND tran_num EQ $tran_num AND leg EQ $leg");

         tFinDeals.destroy();
      }

      tBlankLocDeals.destroy();
   }

   private void APM_Print(String sProcessingMessage) throws OException
   {
      String sMsg;

      sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " "
            + Util.timeGetServerTimeHMS() + ":" + sProcessingMessage + "\n";
      OConsole.oprint(sMsg);
   }
}
