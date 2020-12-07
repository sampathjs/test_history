/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_LegInfo.java

Date Of Last Revision:     31-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back deal leg-level information from core db tables.
                           It doesn't use any current sim results but is just used to enrich sim data for filtering and bucketing.

                             
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_LegInfo implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
   
/* Message levels (only used in calls to APM_Print */
int      cMsgLevelDebug       = 1;
int      cMsgLevelInfo        = 2;
int      cMsgLevelError       = 3;

/* Enum ids for Apm.performOperation */
int APM_CACHE_TABLE_GET = 22;
int APM_CACHE_TABLE_ADD = 23;
int APM_CACHE_TABLE_DROP = 24;
int APM_CACHE_TABLE_INFO = 25;


/*-------------------------------------------------------------------------------
Name:           
Description:    
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
      compute_result_using_db(argt, returnt);
   else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
      format_result(returnt);

   Util.exitSucceed();
}



/*-------------------------------------------------------------------------------
Name:           
Description: Returns all enabled INS_TYPE_ENUM.strategy info filters.   
Parameters:      
Return Values:    
-------------------------------------------------------------------------------*/
Table  getStrategyInfoFilters() throws OException
{
   int iRetVal;
   Table tEnabledStrategyInfoFilters;
   String sQuery;

   OConsole.oprint ("Loading APM strategy_info filter/splitter configuration from the database \n");

   /* No details of enable APM INS_TYPE_ENUM.strategy info filters provides, so load them up based on APM configuration */
   tEnabledStrategyInfoFilters = Table.tableNew("Enabled Strategy Info Filters" );     

   sQuery         = "select distinct " +
                      "tfd.filter_id, " +
                      "tfd.filter_name, " + 
                      "tfd.ref_list_id, " +
                      "aesr.result_column_name, " +
                      "tfd.filter_type " + 
                    "from " +
                      "tfe_filter_defs tfd, " +
                      "apm_pkg_enrichment_config apec, " +
                      "apm_enrichment_source_results aesr " +
                    "where " +
                       "tfd.filter_type in ( 10, 11 ) and " +
                       "tfd.filter_name = apec.enrichment_name and " +
                       "apec.on_off_flag = 1 and " +
                       "aesr.enrichment_name = apec.enrichment_name ";

   iRetVal = DBase.runSql( sQuery  );

   if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runSql() failed to load APM strategy_info filter/splitter configuration" ) );
   else
   {      
      iRetVal = DBase.createTableOfQueryResults( tEnabledStrategyInfoFilters );
      if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.createTableOfQueryResults() failed to load APM strategy_info filter/splitter configuration" ) );      
   }

   return tEnabledStrategyInfoFilters;

} /* end: Table  getStrategyInfoFilters() throws OException */



/*-------------------------------------------------------------------------------
Name:           
Description:    
Parameters:      
Return Values:    
-------------------------------------------------------------------------------*/
void gatherStrategyInfo(int iQueryId, Table tEnabledStrategyInfoFilters, Table returnt) throws OException
{
   Table tStrategyInfo;
   String   sFrom, sWhat, sWhere, sColumnName;
   int      iRetVal, iRow, iFilterId, iTranInfoId, iFilterType;
   int      numFilters = 0;
   int      iNumRows = 0;

   iNumRows = tEnabledStrategyInfoFilters.getNumRows();
   if (iNumRows > 0)
   {

	  int filter_type_col = tEnabledStrategyInfoFilters.getColNum("filter_type");
	  int filter_id_col = tEnabledStrategyInfoFilters.getColNum("filter_id");
	  int ref_list_id_col = tEnabledStrategyInfoFilters.getColNum("ref_list_id");
	  int result_column_name_col = tEnabledStrategyInfoFilters.getColNum("result_column_name");
	  	  
      sWhat = "qr.query_result tran_num";
      sFrom = "query_result qr, ab_tran";

      for (iRow = 1; iRow <= iNumRows; iRow++)
      {
         /* only INS_TYPE_ENUM.strategy info here */
         if ( tEnabledStrategyInfoFilters.getInt( filter_type_col, iRow) != 10 && 
              tEnabledStrategyInfoFilters.getInt( filter_type_col, iRow) != 11 )
            continue;

         numFilters = numFilters + 1;
         iFilterId = tEnabledStrategyInfoFilters.getInt( filter_id_col, iRow);
         iTranInfoId = tEnabledStrategyInfoFilters.getInt( ref_list_id_col, iRow);
         sColumnName = tEnabledStrategyInfoFilters.getString( result_column_name_col, iRow);
         iFilterType = tEnabledStrategyInfoFilters.getInt( filter_type_col, iRow );

         sWhat = sWhat + ", abt_" + iFilterId + ".value " + "\"" + sColumnName + "\"";
         sFrom = sFrom + " left outer join ab_tran_info abt_" + iFilterId;

         if ( iFilterType == 10 )
         {
            sFrom = sFrom +
               " on (ab_tran.int_trading_strategy = abt_" + iFilterId + ".tran_num AND abt_" + iFilterId + ".type_id = " + iTranInfoId + ")";
         }
         else if ( iFilterType == 11 )
         {
            sFrom = sFrom +
               " on (ab_tran.ext_trading_strategy = abt_" + iFilterId + ".tran_num AND abt_" + iFilterId + ".type_id = " + iTranInfoId + ")";
         }

      } /* next: iRow */

      sWhere = "qr.unique_id = " + iQueryId + " and qr.query_result = ab_tran.tran_num";

      /* if none switched on then skip it */
      if ( numFilters > 0 )
      {
         tStrategyInfo = Table.tableNew("Strategy Info");

         iRetVal = APM_TABLE_LoadFromDbWithSQL( tStrategyInfo , sWhat, sFrom, sWhere);

         if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
         {   
            returnt.select( tStrategyInfo , "*", "tran_num EQ $tran_num");
         }

         tStrategyInfo.destroy();
      }


   } /* end: if (tEnabledTranInfoFilters.getNumRows() > 0) */


} /* end: void gatherStrategyInfo(int iQueryId, Table tEnabledStrategyInfoFilters, int workOnInternalStrategy) throws OException */



/*-------------------------------------------------------------------------------
Name:          compute_result_using_db()
Description:   deal info result using core db tables.
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result_using_db(Table argt, Table returnt) throws OException
{
   Table    tTranNums;        /* Not freeable */
   Table    tSimDef;          /* Not freeable */
   Table    tStrategies;
   int      iQueryId = 0;
   int      iRetVal;
   int      iDatabaseType;
   int      iEnabledStrategyInfoFilterDetailsCol;
   String   sPutCall, sFrom, sWhat, sWhere, sVal = null;

   Table    tSourceOfStrategy, tVersion;
   int         major_version, minor_version, code_revision;

   Table    tEnabledStrategyInfoFilters;

   tTranNums   = argt.getTable( "transactions", 1);
   tSimDef     = argt.getTable( "sim_def", 1);

   /* If query ID is provided as a parameter, use it! */
   if (tSimDef.getColNum( "APM Single Deal Query") > 0)
      iQueryId = tSimDef.getInt( "APM Single Deal Query", 1);

   /* If query ID was not set or left at zero, create a query ID from the list of transactions */
   if (iQueryId == 0)
   {  
      /* build up query result to get deals from ab.tran which match our sim result. */
      iQueryId = APM_TABLE_QueryInsertN(tTranNums, "tran_num");
   }

   iDatabaseType = DBase.getDbType();

   /* Create put_call filter to SELECT clause. */
   if (iDatabaseType == DBTYPE_ENUM.DBTYPE_ORACLE.toInt())
   {
      sPutCall = "NVL(io.put_call,2) put_call";
   }
   else
   {
      sPutCall = "isnull(io.put_call,2) put_call";   
   }

   /* Retrieve the Endur code version */
   tVersion = Ref.getVersion();
   major_version = tVersion.getInt( "major_version", 1);
   minor_version = tVersion.getInt( "minor_version", 1);
   code_revision = tVersion.getInt( "code_revision", 1);
   tVersion.destroy();
 
   /* Figure out whether the strategies are in play - this is Endur version-dependent */
   tSourceOfStrategy = Table.getCachedTable ("APM_Source_Of_Strategy");
   if(tSourceOfStrategy == null || Table.isTableValid(tSourceOfStrategy) == 0)
   {
     tSourceOfStrategy = Table.tableNew("APM_Source_Of_Strategy");
     tSourceOfStrategy.addCols( "S(what) S(from) S(where)");
     tSourceOfStrategy.addNumRows(2);
     
     if ((major_version > 9) || (major_version == 9 && minor_version >= 2) ||
         (major_version == 9 && minor_version == 1 && code_revision >= 20000))
     {
         tSourceOfStrategy.setString( 1, 1, ", ab.int_trading_strategy int_str, ab.ext_trading_strategy ext_str");
         tSourceOfStrategy.setString( 2, 1, "");
         tSourceOfStrategy.setString( 3, 1, "");

         tSourceOfStrategy.setString( 1, 2, "ab_tran.deal_tracking_num dealnum, ab_tran.int_trading_strategy int_str, ab_tran.ext_trading_strategy ext_str");
         tSourceOfStrategy.setString( 2, 2, "ab_tran, query_result");
         tSourceOfStrategy.setString( 3, 2, "ab_tran.ins_num = query_result.query_result AND query_result.unique_id = " + iQueryId);
     }
     else
     {
         tSourceOfStrategy.setString( 1, 1, ", 0 as int_str, 0 as ext_str");
         tSourceOfStrategy.setString( 2, 1, "");
         tSourceOfStrategy.setString( 3, 1, "");
     }

     Table.cacheTable("APM_Source_Of_Strategy", tSourceOfStrategy);
   }    
   
   returnt.addCols("I(dealnum) I(tran_num) I(ins_num) I(toolset) I(version) I(ins_type) I(toolset_leg)");
   returnt.addCols("I(ins_type_leg) I(ins_sub_type_leg) I(projindex) I(projection_method_id) I(ref_source)");
   returnt.addCols("I(disc_index) I(leg_start) I(leg_end) F(notional) I(leg) I(delivery_type) I(settlement_type)");
   returnt.addCols("I(pay_rec) I(fx_flt) I(currency) F(strike) F(rate)");
   
   if(USER_APM_IsContractCodeFilterSwitchedOn() != 0)
        returnt.addCol("contract_code", COL_TYPE_ENUM.COL_INT);

   returnt.addCols("I(market_px_index_id) I(put_call) I(int_str) I(ext_str) S(int_str_s)  S(ext_str_s)");
  
   /* Query onto the database */
   sWhat = "ab.deal_tracking_num dealnum, ab.tran_num tran_num, ab.ins_num ins_num, ab.toolset toolset, ab.version_number version, ab.ins_type ins_type, " +
       "ab.toolset toolset_leg, ab.ins_type ins_type_leg, ab.ins_sub_type ins_sub_type_leg, " +
       "prh.proj_index projindex, prh.projection_method_id, prh.ref_source, pa.disc_index disc_index, pa.start_date leg_start, pa.mat_date leg_end, " +
       "pa.notnl notional, pa.param_seq_num leg, pa.delivery_type, pa.settlement_type, pa.pay_rec, pa.fx_flt, pa.currency, " +
       "io.strike strike, pa.rate rate, ";
   
   if(USER_APM_IsContractCodeFilterSwitchedOn() != 0)
      sWhat = sWhat + "mi.contract_code, ";

   sWhat = sWhat + "mi.market_px_index_id, " + sPutCall;
   sWhat = sWhat + tSourceOfStrategy.getString( 1, 1);

   sFrom =  "ab_tran ab, " + 
            "ins_parameter pa " +
            "left outer join misc_ins mi on (mi.ins_num = pa.ins_num and mi.param_seq_num = pa.param_seq_num and mi.misc_ins_seq_num = 0) " +
            "left outer join ins_option io on (io.ins_num = pa.ins_num and io.param_seq_num = pa.param_seq_num and io.option_seq_num = 0), " +
            "param_reset_header prh, " + 
            "query_result qr";
   
   sWhere = "ab.ins_num = pa.ins_num AND qr.query_result = ab.tran_num AND prh.ins_num = pa.ins_num AND " + 
            "prh.param_seq_num = pa.param_seq_num AND prh.param_reset_header_seq_num = 0 AND " +
            "qr.unique_id = " + iQueryId;

   iRetVal = APM_TABLE_LoadFromDbWithSQL(returnt, sWhat, sFrom, sWhere);

   /* now add the INS_TYPE_ENUM.strategy names so we can pivot by name */
   tStrategies = Table.tableNew("");

   if (tSourceOfStrategy.getNumRows() > 1)
       sVal = tSourceOfStrategy.getString( 1, 2);
   
   if (sVal != null && sVal.length() > 0)
   {
      iRetVal = APM_TABLE_LoadFromDbWithSQL(tStrategies, "strategy_id, strategy_name", "strategy_listing", "strategy_id > 0");
      // add a default row for None
      int row = tStrategies.addRow();
      tStrategies.setInt( 1, row, 0);
      tStrategies.setString( 2, row, "None");
      
      returnt.select( tStrategies, "strategy_name (int_str_s)", "strategy_id EQ $int_str");
      returnt.select( tStrategies, "strategy_name (ext_str_s)", "strategy_id EQ $ext_str");

      /* If the UDSR was run beneath an APM Service, then the SimDef will include details of 
         the enabled APM tran info filters based on cached APM filter configuration details */
      iEnabledStrategyInfoFilterDetailsCol = tSimDef.getColNum( "APM Enabled Tran Info Filters");

      if (iEnabledStrategyInfoFilterDetailsCol > 0)
         tEnabledStrategyInfoFilters = tSimDef.getTable( iEnabledStrategyInfoFilterDetailsCol, 1);
      else
         tEnabledStrategyInfoFilters = getStrategyInfoFilters( );

      gatherStrategyInfo( iQueryId, tEnabledStrategyInfoFilters, returnt );

      /* cleanup if necessary */
      if (iEnabledStrategyInfoFilterDetailsCol <= 0) tEnabledStrategyInfoFilters.destroy();
   }


   /* Some fix-ups take information from one leg of the deal, and apply to another
   For ease of use, grouping by deal and leg here */
   returnt.group( "dealnum, leg");
   
   fixup_option_strike_and_type(returnt);
   fixup_equity_ticker(returnt);
   
   if ( tSourceOfStrategy.getNumRows() > 1 && tSourceOfStrategy.getString( 1, 2).length() > 0 && (returnt.getNumRows() > 0))
   {
      fixup_gas_deals_strategies(tSourceOfStrategy, tStrategies, returnt);
   }

   tStrategies.destroy();

   if ((major_version > 8) || 
       (major_version == 8 && minor_version >= 1)) 
   {
      fixup_leg_toolset_for_hybrid_deals(iQueryId, returnt);
   }

   gather_param_info(iQueryId, tSimDef, returnt);
   
  
  
   /* If query ID is provided as a parameter, somebody else should free it */
   if ((tSimDef.getColNum( "APM Single Deal Query") < 1) || (tSimDef.getInt( "APM Single Deal Query", 1) == 0))
   { 
	   if (iQueryId != 0)
		   Query.clear(iQueryId);
   }
   
   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
   if (envVar != null)
   {
		envVar = envVar.toUpperCase();
	  
		if (envVar.equals("TRUE"))
		{   
			returnt.clearGroupBy ();
			returnt.addGroupBy ("dealnum");
			returnt.addGroupBy ("tran_num");
			returnt.addGroupBy ("leg");
			returnt.groupBy ();
		}
   }      
}

/*-------------------------------------------------------------------------------
Name:          fixup_equity_ticker()
Description:   For deals from EquityOtcOpt toolset, replace ticker with underlying's ticker
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_equity_ticker(Table returnt) throws OException
{
   Table       tSupplementary, tSupplementary2;
   String      sFrom, sWhat, sWhere;
   Integer     iSupQueryId, iRetVal;

   {
      tSupplementary = Table.tableNew("");
      tSupplementary.select(returnt, "ins_num", "toolset EQ 45"); // EQUITY_OTC_OPTION_TOOLSET

      if (tSupplementary.getNumRows() > 0)
      {
         iSupQueryId = APM_TABLE_QueryInsertN(tSupplementary, "ins_num");
         tSupplementary.destroy();
         tSupplementary = Table.tableNew("");

         sWhat = "distinct ins_num, underlying_tran, param_seq_num";
         sFrom = "tran_underlying_link, query_result qr";
         sWhere = "qr.query_result = tran_underlying_link.ins_num AND qr.unique_id = " + iSupQueryId;

         iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary, sWhat, sFrom, sWhere);
         Query.clear(iSupQueryId);

         if (tSupplementary.getNumRows() > 0) // include basket options 
         {
            iSupQueryId = APM_TABLE_QueryInsertN(tSupplementary, "underlying_tran");
            tSupplementary2 = Table.tableNew("");

            sWhat = "ab.tran_num, header.ins_num, header.ticker";
            sFrom = "ab_tran ab, header, query_result qr";
            sWhere = "qr.query_result = ab.tran_num AND ab.ins_num = header.ins_num AND qr.unique_id = " + iSupQueryId;

            iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary2, sWhat, sFrom, sWhere);
            Query.clear(iSupQueryId);

            tSupplementary.select(tSupplementary2, "ticker", "tran_num EQ $underlying_tran");
            returnt.select(tSupplementary, "ticker (leg_underlying_equity_ticker)", "ins_num EQ $ins_num AND param_seq_num EQ $leg");

            tSupplementary2.destroy();            
         }
      }
	  
      if (returnt.getColNum("leg_underlying_equity_ticker") < 0)
      {
         returnt.addCol("leg_underlying_equity_ticker", COL_TYPE_ENUM.COL_STRING);
      }
	  
      tSupplementary.destroy();
   }
}

/* We want to have INS_TYPE_ENUM.hybrid constituents' toolset, ins type and subtype exposed as APM filters
   This retrieves appropriate values and updates as necessary */
void fixup_leg_toolset_for_hybrid_deals(int iQueryId, Table returnt) throws OException
{
   int         iRetVal, iNumRows, i, j, iFirstLeg, iNumLegs;
   String      sWhat, sFrom, sWhere;
   Table    tHybridData, tFinalHybridData;

   sWhat =  "ab.deal_tracking_num dealnum, icm.starting_param param_seq_num, icm.ins_type ins_type_leg, " +
            "icm.ins_sub_type ins_sub_type_leg, i.toolset toolset_leg, icm.num_param";

   sFrom = "ab_tran ab, ins_component_map icm, instruments i, query_result qr";

   /* 44 and 46 refer to TOOLSET_ENUM.HYBRID_TOOLSET and TOOLSET_ENUM.HYBRID_NOTE_TOOLSET, respectively,
      but these are undefined in earlier cuts, so using literals here */
   sWhere = " qr.unique_id = " + iQueryId + 
            " AND qr.query_result = ab.tran_num" +
            " AND ab.toolset IN (" + TOOLSET_ENUM.HYBRID_TOOLSET.toInt() + ", " + TOOLSET_ENUM.HYBRID_NOTE_TOOLSET.toInt() + ")" +
                                        " AND icm.ins_num = ab.ins_num" + 
                                        " AND i.id_number = icm.ins_type";

   tHybridData = Table.tableNew("Hybrid Data");
   
   iRetVal = APM_TABLE_LoadFromDbWithSQL(tHybridData, sWhat, sFrom, sWhere);

   if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
   {
      OConsole.oprint ("Could not retrieve Hybrid Data to support leg-level toolset filter.\n");
      return;
   }
   
   tFinalHybridData = tHybridData.cloneTable();
   int param_seq_num_col = tHybridData.getColNum("param_seq_num"); /*this will be the same # for cloned table */
   int num_param_col = tHybridData.getColNum("num_param");
   
   iNumRows = tHybridData.getNumRows();

   for (i = 1; i <= iNumRows; i++)
   {
      iFirstLeg = tHybridData.getInt( param_seq_num_col, i);
      iNumLegs = tHybridData.getInt( num_param_col, i);

      for (j = 0; j < iNumLegs; j++)
      {
         tHybridData.copyRowAdd( i, tFinalHybridData);
         tFinalHybridData.setInt( param_seq_num_col, tFinalHybridData.getNumRows(), iFirstLeg + j);
      }
   }

   /* Now copy over the correct leg toolset and other info */
   returnt.select( tFinalHybridData, "ins_type_leg, ins_sub_type_leg, toolset_leg", "dealnum EQ $dealnum AND param_seq_num EQ $leg");

   tHybridData.destroy();
   tFinalHybridData.destroy();
}

/* This function expects to see data grouped by deal number and leg */
void fixup_option_strike_and_type(Table returnt) throws OException
{
   int row, numRows, curLeg;
   int legCol, insTypeCol, strikeCol, rateCol, putCallCol;
   String insName;

   numRows = returnt.getNumRows();
   legCol = returnt.getColNum( "leg");
   insTypeCol = returnt.getColNum( "ins_type");
   strikeCol = returnt.getColNum( "strike");
   rateCol = returnt.getColNum( "rate");
   putCallCol = returnt.getColNum( "put_call");

   for (row = 1; row <= numRows; row++)
   {
      curLeg = returnt.getInt( legCol, row);
      //DTS134602 insName = Table.formatRefInt(returnt.getInt( insTypeCol, row), SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
      insName = Table.formatRefInt(Instrument.getBaseInsType(returnt.getInt(insTypeCol, row)), SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
	  
      /* Commodity toolset's options (puts\calls, daily, monthly etc.) require special handling */
      if (insName.contains("CO-CALL-") || insName.contains("CO-PUT-"))
      {
         /* Set the strike from ins_parameter.rate -> that's what STRIKE_BY_LEG does
            We do one better, and set this strike on all legs from leg 2 (financial leg that keeps it) */
         returnt.setDouble( strikeCol, row, returnt.getDouble( rateCol, row + 2 - curLeg));

         /* Let's also set the put\call flag correctly */
         if(insName.contains("CO-CALL-"))
         {
            returnt.setInt( putCallCol, row, 1);
         }
         else
         {
            returnt.setInt( putCallCol, row, 0);
         }
      }

      /* Commodity toolset's options (puts\calls, daily, monthly etc.) require special handling */
      if (insName.contains("PO-SPD-") && curLeg == 0)
      {
         /* Set the same strike on leg one of the power spread option */
         returnt.setDouble( strikeCol, row + 1, returnt.getDouble( rateCol, row));

         /* Set the same put\call value on leg one of the power spread option */
         returnt.setInt( putCallCol, row + 1, returnt.getInt( putCallCol, row));
      }     

      /* For FXO-PE and FXO-PA deals, need to fill out leg 3 and 4 (premium legs) */
      if (insName.contains("FXO-PE") || insName.contains("FXO-PA"))
      {
          if (curLeg < 2)
          {
             // Set the same strike on leg 3, 4 of the FXO-PE/FXO-PA option
        	  returnt.setDouble(strikeCol, row + 2, returnt.getDouble(rateCol, row));

            // Set the same put\call value on leg 3, 4 of the FXO-PE/FXO-PA option
        	  returnt.setInt(putCallCol, row + 2, returnt.getInt( putCallCol, row));
         }

      }
      /* For FX-Option toolset, need to fill out leg one (represents the premium leg) */
      else if (insName.contains("FXO-") && curLeg == 0) 
      {
         /* Set the same strike on leg one of the FX option */
         returnt.setDouble( strikeCol, row + 1, returnt.getDouble( rateCol, row));

         /* Set the same put\call value on leg one of the FX option */
         returnt.setInt( putCallCol, row + 1, returnt.getInt( putCallCol, row));
      }     

   }
}

 
void fixup_gas_deals_strategies(Table tGasInfo, Table tStrategies, Table returnt) throws OException
{
   int      iGasQueryId, iRetVal, iNumRows, iIntStratCol, iExtStratCol;
   int      row;
   Table tGasDeals, tGasStrategies;
   String   sFrom, sWhat, sWhere;

   /* First, identify all deals within Commodity toolset */

   tGasDeals = Table.tableNew("Gas Deals");   

   tGasDeals.select( returnt, "DISTINCT, ins_num", "toolset EQ " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt());
   
   if (tGasDeals.getNumRows() == 0)
   {
      /* No gas deals - skip the rest of the function */
      tGasDeals.destroy();
      return;
   }

   iGasQueryId = APM_TABLE_QueryInsertN(tGasDeals, "ins_num");

   /* Now we wish to enrich the gas deals with all legs available */
   tGasDeals.select( returnt, "dealnum, leg", "ins_num EQ $ins_num");

   tGasStrategies = Table.tableNew("");

   tGasStrategies.addCols("I(ins_num) I(leg) I(int_str) I(ext_str) S(int_str_s) S(ext_str_s)");

   sWhat = "gas_phys_param.ins_num, gas_phys_param.param_seq_num leg, gas_phys_param.int_trading_strategy int_str, gas_phys_param.ext_trading_strategy ext_str";
   sFrom = "gas_phys_param, query_result";
   sWhere = "gas_phys_param.ins_num = query_result.query_result AND query_result.unique_id =" + iGasQueryId;

   iRetVal = APM_TABLE_LoadFromDbWithSQL(tGasStrategies, sWhat, sFrom, sWhere);

   /* now add the INS_TYPE_ENUM.strategy names so we can pivot by name */
   tGasStrategies.select( tStrategies, "strategy_name (int_str_s)", "strategy_id EQ $int_str");
   tGasStrategies.select( tStrategies, "strategy_name (ext_str_s)", "strategy_id EQ $ext_str");

   tGasDeals.select( tGasStrategies, "int_str, ext_str, int_str_s, ext_str_s", "ins_num EQ $ins_num AND leg EQ $leg");
   tGasDeals.group( "dealnum, leg");

   /* Now fill out the financial legs (even, 2+), and the fake leg zero */
   fillOutFinancialLegs(tGasDeals);
   
   
   /* remove any rows where there is no int or ext INS_TYPE_ENUM.strategy - avoids overwriting the deal level INS_TYPE_ENUM.strategy 
      if there is physical leg for an instrument e.g. comm-fee or user def instr of base type comm-fee */
   iNumRows = tGasDeals.getNumRows();
   iIntStratCol = tGasDeals.getColNum( "int_str");
   iExtStratCol = tGasDeals.getColNum( "ext_str");
   for (row =iNumRows; row > 0; row--)
   {
      if ( (tGasDeals.getInt(iIntStratCol,row) == 0) &&
           (tGasDeals.getInt(iExtStratCol,row) == 0) )
      {
         tGasDeals.delRow(row);
      }     
   }


   /* Now enrich onto final result */
   returnt.select( tGasDeals, "int_str, ext_str, int_str_s, ext_str_s", "dealnum EQ $dealnum AND leg EQ $leg");

   tGasStrategies.destroy();
   tGasDeals.destroy();
   Query.clear(iGasQueryId);
}

/*-------------------------------------------------------------------------------
Name:          fillOutFinancialLegs()
Description:   Fills out the financial legs (even, 2+), and the fake leg zero
Parameters:    Table tGasDeals 
Return Values:   
-------------------------------------------------------------------------------*/
void fillOutFinancialLegs(Table tGasDeals)throws OException
{
	int  row, iNumRows;
    int myLeg, myDeal, nextDeal, prevDeal, myVal;
    int dealnum_col = tGasDeals.getColNum("dealnum");
    int leg_col = tGasDeals.getColNum("leg");
    int int_str_col = tGasDeals.getColNum("int_str");
    int ext_str_col = tGasDeals.getColNum("ext_str");
    int int_str_s_col = tGasDeals.getColNum("int_str_s");
    int ext_str_s_col = tGasDeals.getColNum("ext_str_s");
    
    iNumRows = tGasDeals.getNumRows();
	for (row = 1; row <= iNumRows; row++)
	{
	      myDeal = tGasDeals.getInt( dealnum_col, row);
	      myLeg = tGasDeals.getInt( leg_col, row);
	      myVal = tGasDeals.getInt( int_str_col, row);


	      if (row > 1)
	      {
	         prevDeal = tGasDeals.getInt( dealnum_col, row-1);
	      }
	      else
	      {
	         prevDeal = -1;
	      }

	      if (row < iNumRows)
	      {
	         nextDeal = tGasDeals.getInt( dealnum_col, row+1);
	      }
	      else
	      {
	         nextDeal = -1;
	      }
	        

	      if ((myLeg == 0) && (myVal == 0) && (nextDeal == myDeal))
	      {
	         /* Enrich with values from leg 1 (next row) */
	         tGasDeals.setInt( int_str_col, row, tGasDeals.getInt( int_str_col, row+1));
	         tGasDeals.setInt( ext_str_col, row, tGasDeals.getInt( ext_str_col, row+1));
	         tGasDeals.setString( int_str_s_col, row, tGasDeals.getString( int_str_s_col, row+1));
	         tGasDeals.setString( ext_str_s_col, row, tGasDeals.getString( ext_str_s_col, row+1));
	      }
	      else if (((myLeg % 2) == 0) && (myVal == 0) && (prevDeal == myDeal))
	      {
	         /* Enrich with the corresponding physical leg's values (previous row) */
	         tGasDeals.setInt( int_str_col, row, tGasDeals.getInt( int_str_col, row-1));
	         tGasDeals.setInt( ext_str_col, row, tGasDeals.getInt( ext_str_col, row-1));
	         tGasDeals.setString( int_str_s_col, row, tGasDeals.getString( int_str_s_col, row-1));
	         tGasDeals.setString( ext_str_s_col, row, tGasDeals.getString( ext_str_s_col, row-1));
	      }
	   }
}

/*-------------------------------------------------------------------------------
Name:          format_result()
Description:   UDSR format function. (Default Formatting used)
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void format_result(Table returnt) throws OException
{
   returnt.setColFormatAsRef( "ins_type",            SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
   returnt.setColFormatAsRef( "projindex",           SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "projection_method_id",SHM_USR_TABLES_ENUM.PROJECTION_METHOD_TABLE);
   returnt.setColFormatAsRef( "ref_source",          SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
   returnt.setColFormatAsRef( "disc_index",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsDate( "leg_start",           DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "leg_end",             DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "delivery_type",       SHM_USR_TABLES_ENUM.DELIVERY_TYPE_TABLE);
   returnt.setColFormatAsRef( "settlement_type",     SHM_USR_TABLES_ENUM.SETTLEMENT_TYPE_TABLE);
   returnt.setColFormatAsRef( "pay_rec",             SHM_USR_TABLES_ENUM.REC_PAY_TABLE);
   returnt.setColFormatAsRef( "fx_flt",              SHM_USR_TABLES_ENUM.FX_FLT_TABLE);
   returnt.setColFormatAsRef( "currency",            SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
   returnt.setColFormatAsRef( "contract_code",       SHM_USR_TABLES_ENUM.CONTRACT_CODES_TABLE);
   returnt.setColFormatAsRef( "market_px_index_id",  SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "put_call",            SHM_USR_TABLES_ENUM.PUT_CALL_TABLE);

   returnt.setColFormatAsRef( "ins_type_leg",        SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
   returnt.setColFormatAsRef( "ins_sub_type_leg",    SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);
   returnt.setColFormatAsRef( "toolset_leg",         SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
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
Name:          APM_DBASE_RunProc
Description:   deadlock protected version of the fn
Parameters:      As per DBase.runProc
Return Values:   retval (success or failure)
Effects:   <any *>
-------------------------------------------------------------------------------*/
int APM_DBASE_RunProc(String sp_name, Table arg_table) throws OException
{
    final int nAttempts = 10;

    int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

    int numberOfRetriesThusFar = 0;
    do {
    	// for error reporting further down
    	String message = null;
    	
        try {
            // db call
        	iRetVal = DBase.runProc(sp_name, arg_table);
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
    	OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of " + sp_name + " failed" ) );
	
   return iRetVal;
}

/*-------------------------------------------------------------------------------
Name:          USER_APM_IsFilterSwitchedOn
Description:   returns whether a particular filter is switched on 
Parameters:
Return Values:   int
Effects:   <any *>
-------------------------------------------------------------------------------*/
Table USER_APM_IsFilterSwitchedOn(String strFilterName) throws OException
{
   Table   tArgs;
   Table       tFilterOnOff;
   String   strPackageName;
   int    iRetVal = 1;

   strPackageName = "";            
         
   /* Create the function parameters and run the the stored proc */
   tArgs = Table.tableNew( "params" );
   tArgs.addCols( "S(strPackageName) S(strFilterName)");
   tArgs.addRow();
   tArgs.setString( "strPackageName", 1, strPackageName );
   tArgs.setString( "strFilterName", 1, strFilterName );
  
   iRetVal = APM_DBASE_RunProc("USER_apm_is_filter_on", tArgs );
   tArgs.destroy();
   if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      return ( null );

   tFilterOnOff = Table.tableNew();
   try {
	   iRetVal = DBase.createTableOfQueryResults( tFilterOnOff );
   }
   catch (OException exception) {
       iRetVal = exception.getOlfReturnCode().toInt();
       
       OConsole.oprint ("\nERROR: " +exception.getMessage());
   }
   
   if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
   {
      OConsole.oprint ("USER_APM_IsFilterSwitchedOn unable to retrieve results of RunProc call\n" );
      return ( null );
   }

  
   return ( tFilterOnOff );
}

/*-------------------------------------------------------------------------------
Name:          USER_APM_IsContractCodeFilterSwitchedOn
Description:   returns whether the contract code filter is switched on 
Parameters:
Return Values:   int
Effects:   <any *>
-------------------------------------------------------------------------------*/
int USER_APM_IsContractCodeFilterSwitchedOn() throws OException
{
   Table       tCachedContractCodeFilterOnOff;
   Table       tArgumentTable;
   String         strCachedTableName;
   int            iContractCodeFilterOnOff = 0;
   int            iRetVal;

   /* Create a dummy argument table */
   tArgumentTable = Table.tableNew("APM Argument Table");


   /* The cache name */
   strCachedTableName = "Contract Code Cached Table";
   
   /* Check to see whether we can get the cache table */   
   tCachedContractCodeFilterOnOff = APM_CacheTableGet( strCachedTableName, tArgumentTable );
  
   if(tCachedContractCodeFilterOnOff == null || Table.isTableValid( tCachedContractCodeFilterOnOff ) == 0)
   {
      /* Get the contract code filter setting and attempt to cache */
	   
      tCachedContractCodeFilterOnOff = USER_APM_IsFilterSwitchedOn("Contract Code");  
      if(tCachedContractCodeFilterOnOff == null || Table.isTableValid( tCachedContractCodeFilterOnOff ) == 0)
         return 0; /* If proc failed then strike must be off */
      
      iRetVal = APM_CacheTableAdd( strCachedTableName, "TFE.METADATA.CHANGED", tCachedContractCodeFilterOnOff.copyTable(), tArgumentTable);

      if(iRetVal == 0)
      {
         /* Do nothing at the moment, caching has failed, but we're still going to have the correct data. */  
      }
      
      if (tCachedContractCodeFilterOnOff.getNumRows() > 0)
    	  iContractCodeFilterOnOff = tCachedContractCodeFilterOnOff.getInt( 1, 1);

      tCachedContractCodeFilterOnOff.destroy();   
   }
   else
   {
      iContractCodeFilterOnOff = tCachedContractCodeFilterOnOff.getInt( 1, 1);
   }

   /* Destroy the dummy argument table. */
   tArgumentTable.destroy();

   /* Returns whether the contract code filter has been set. */
   return ( iContractCodeFilterOnOff );
}

void gather_param_info(int iQueryId, Table tSimDef, Table returnt) throws OException
{
   Table tEnabledTranInfoFilters, tTranInfo;
   String sFrom, sWhat, sWhere,  sColumnName;
   int iRetVal, iRow, iFilterId, iTranInfoId;
   int iEnabledTranInfoFilterDetailsCol;
   int numFilters = 0;
   int iNumRows = 0;
   
   /* If the UDSR was run beneath an APM Service, then the SimDef will include details of
      the enabled APM tran info filters based on cached APM filter configuration details */
   iEnabledTranInfoFilterDetailsCol = tSimDef.getColNum( "APM Enabled Tran Info Filters");
   if (iEnabledTranInfoFilterDetailsCol > 0)
      tEnabledTranInfoFilters = tSimDef.getTable( iEnabledTranInfoFilterDetailsCol, 1);
   else
   {
      OConsole.oprint ("Loading APM tran_info filter/splitter configuration from the database \n");
      /* No details of enable APM tran info filters provides, so load them up based on APM configuration */
      tEnabledTranInfoFilters = Table.tableNew("Enabled Tran Info Filters" );     
      iRetVal = DBase.runSql("select distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, tfd.filter_type " + 
                             "from tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr " +
                             "where tfd.filter_type in (5, 7, 8) and tfd.filter_name = apec.enrichment_name " +
                             "and apec.on_off_flag = 1 and aesr.enrichment_name = apec.enrichment_name");
      if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runSql() failed to load APM tran_info filter/splitter configuration" ) );
      else
      {      
         iRetVal = DBase.createTableOfQueryResults(tEnabledTranInfoFilters);
         if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.createTableOfQueryResults() failed to load APM tran_info filter/splitter configuration" ) );      
      }
   }   

   if (tEnabledTranInfoFilters.getNumRows() > 0)
   {
      /*
         select insp.ins_num, insp.param_seq_num leg, abt_5025.value "param_info_picklist", abt_5026.value "param_info_string"
         from query_result qr, ab_tran ab, ins_parameter insp 
         left outer join ins_parameter_info abt_5025 on (insp.ins_num = abt_5025.ins_num AND insp.param_seq_num = abt_5025.param_seq_num AND abt_5025.type_id = 20026) 
         left outer join ins_parameter_info abt_5026 on (insp.ins_num = abt_5026.ins_num AND insp.param_seq_num = abt_5026.param_seq_num AND abt_5026.type_id = 20027)
         where qr.unique_id = 473102
         and insp.ins_num = ab.ins_num
         and ab.tran_num = qr.query_result
      */
      sWhat = "distinct insp.ins_num, insp.param_seq_num leg";
      sFrom = "query_result qr, ab_tran ab, ins_parameter insp";
      iNumRows = tEnabledTranInfoFilters.getNumRows();
      for (iRow = 1; iRow <= iNumRows; iRow++)
      {
         /* only param info here (not ins info or tran info) */
         if ( tEnabledTranInfoFilters.getInt( "filter_type", iRow) != 8 )
            continue;

         numFilters = numFilters + 1;
         iFilterId = tEnabledTranInfoFilters.getInt( "filter_id", iRow);
         iTranInfoId = tEnabledTranInfoFilters.getInt( "ref_list_id", iRow);
         sColumnName = tEnabledTranInfoFilters.getString( "result_column_name", iRow);
         sWhat = sWhat + ", abt_" + iFilterId + ".value " + "\"" + sColumnName + "\"";
         sFrom = sFrom + " left outer join ins_parameter_info abt_" + iFilterId + " on (insp.ins_num = abt_" + iFilterId + ".ins_num AND insp.param_seq_num = abt_" + iFilterId + ".param_seq_num AND abt_" + iFilterId + ".type_id = " + iTranInfoId + ")";
      }
      sWhere = "qr.unique_id = " + iQueryId + " and insp.ins_num = ab.ins_num and ab.tran_num = qr.query_result";

      /* if none switched on then skip it */
      if ( numFilters == 0 )
         return;
         
      tTranInfo = Table.tableNew("Tran Info");
      iRetVal = APM_TABLE_LoadFromDbWithSQL(tTranInfo, sWhat, sFrom, sWhere);
      if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      {   
         returnt.select( tTranInfo, "*", "ins_num EQ $ins_num AND leg EQ $leg");
      }
      tTranInfo.destroy();
   }

   if (iEnabledTranInfoFilterDetailsCol <= 0) tEnabledTranInfoFilters.destroy();
}

/*-------------------------------------------------------------------------------
Name:          APM_CacheTableGet

Description:   Used to get table from Cache

Parameters:    sCachedName         -  Cached Name to find table
               tAPMArgumentTable   -  Used for error reporting.

Returns:       returns cached table or null if not found.
-------------------------------------------------------------------------------*/
Table APM_CacheTableGet(String sCachedName, Table tAPMArgumentTable) throws OException
{
   XString err_xstring = Str.xstringNew();
   Table tCacheParams;   
   Table tCachedTable = null;
   Table tSafeTable = null;
   int iRetVal = 0;
   int iCopyRow = 0;
   
   tCacheParams = Table.tableNew("CacheTableParams");
   try {
	   iRetVal = Apm.performOperation(APM_CACHE_TABLE_GET, 1, tCacheParams, err_xstring );
   }
   catch (OException exception) {
       OConsole.oprint ("\nERROR: " +exception.getMessage());
   }
  
   if(iRetVal == 0)
   {
      Str.xstringDestroy (err_xstring);
      APM_PrintErrorMessage (tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_GET: " + Str.xstringGetString(err_xstring) + "\n");
      return null;
   }

   /* Set to return ptr to cached table. */
   tCacheParams.getTable( 2, 2).setInt(1, 1, 0);
   
   /* Set the Cached Name */
   tCacheParams.getTable( 2, 1).setString(1, 1, sCachedName);

   /* Try and Retrieve Cached Table */
   try {
	   iRetVal = Apm.performOperation(APM_CACHE_TABLE_GET, 0, tCacheParams, err_xstring );  
   }
   catch (OException exception) {
       OConsole.oprint ("\nERROR: " +exception.getMessage());
   }
   
   if(iRetVal != 0)
   {
      iCopyRow = tCacheParams.unsortedFindString( "parameter_name", "copy of table", SEARCH_CASE_ENUM.CASE_SENSITIVE );

      if ( iCopyRow <= 0 )
      {
         APM_PrintErrorMessage(tAPMArgumentTable, "Error in APM_CacheTableGet, cannot find 'copy of table' table in returned parameters\n" );
         Str.xstringDestroy (err_xstring);
         return null;
      }
      else
      {
         /* If the table is a copy, we can pass this on safely, otherwise we need to pass a copy on so that
            the script can delete it... */
      
         int iTableIsCopy = tCacheParams.getTable( "parameter_value", iCopyRow ).getInt(1, 1 );
      
         if ( iTableIsCopy == 1 )
         {
            tSafeTable = tCacheParams.getTable( 2, 3);
            tCacheParams.setTable( 2, 3, null);         
         }
         else
         {          
            tCachedTable = tCacheParams.getTable( 2, 3);
            tSafeTable = tCachedTable.copyTable();
            tCacheParams.setTable( 2, 3, null);
         }
      }
   }  
   /* Don't raise error since it probably simply means that the table requested is currently not cached ... this is not an error ... 
      else
         APM_PrintErrorMessage (tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_GET: " + Str.xstringGetString(err_xstring) + "\n"); */

   Str.xstringDestroy (err_xstring);
   tCacheParams.destroy();

   return ( tSafeTable );
}

/*-------------------------------------------------------------------------------
Name:          APM_CacheTableAdd

Description:   Used to add table to cache.

Parameters:    sCachedName             -  Name to store table under.
               sRemoveTableMsgSubject  -  Subject to remove table from cache.
               tToCacheTable           -  Table to cache.
               tAPMArgumentTable       -  Used for error reporting.

Returns:       result 1 if all is well.
-------------------------------------------------------------------------------*/
int APM_CacheTableAdd(String sCachedName, String sRemoveTableMsgSubject, Table tToCacheTable, Table tAPMArgumentTable) throws OException
{
   XString err_xstring = Str.xstringNew();
   Table tCacheParams;
   int iRetVal = 1;

   tCacheParams = Table.tableNew("CacheTableParams");
   iRetVal = Apm.performOperation(APM_CACHE_TABLE_ADD, 1, tCacheParams, err_xstring );      
   if(iRetVal == 0)
   {
      APM_PrintErrorMessage (tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_ADD: " + Str.xstringGetString(err_xstring) + "\n");
   }
   else
   {
      tCacheParams.getTable( 2, 1).setString( 1, 1, sCachedName);
      tCacheParams.setTable( 2, 2, tToCacheTable);
      tCacheParams.getTable( 2, 3).setString( 1, 1, sRemoveTableMsgSubject);
      iRetVal = Apm.performOperation(APM_CACHE_TABLE_ADD, 0, tCacheParams, err_xstring );
      if(iRetVal == 0)
      {
         APM_PrintErrorMessage (tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_ADD: " + Str.xstringGetString(err_xstring) + "\n");
      }
   }
   
   Str.xstringDestroy (err_xstring);
   tCacheParams.destroy();

   return iRetVal;
}

/*-------------------------------------------------------------------------------
Name:          APM_CacheTableInfo

Description:   Used to get cache

Parameters:    

Returns:       result 1 if all is well.
-------------------------------------------------------------------------------*/
int APM_CacheTableInfo(Table tAPMArgumentTable) throws OException
{
   XString err_xstring = Str.xstringNew();
   Table tCacheParams;
   int iRetVal = 1;

   tCacheParams = Table.tableNew("CacheTableParams");
   iRetVal = Apm.performOperation(APM_CACHE_TABLE_INFO, 1, tCacheParams, err_xstring );      
   if(iRetVal != 0)
   {
      iRetVal = Apm.performOperation(APM_CACHE_TABLE_INFO, 0, tCacheParams, err_xstring );
      tCacheParams.viewTableForDebugging();
   }
   else
      APM_PrintErrorMessage (tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_INFO: " + Str.xstringGetString(err_xstring) + "\n");

   tCacheParams.destroy();
   Str.xstringDestroy (err_xstring);

   return iRetVal;
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
Name:          APM_GetFullMsgContext 
Description:   
Parameters:    
Effects:   <any *>
-------------------------------------------------------------------------------*/
String APM_GetFullMsgContext (Table tAPMArgumentTable) throws OException
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



}
