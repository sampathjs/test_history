/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_DealInfo.java

Date Of Last Revision:     10-Mar-2014 - Converted from AVS to OpenJVS
						   20-Mar-2017 - Updated to match AVS script
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back deal tran-level information from core db tables.
                           It doesn't use any current sim results but is just used to enrich sim data for filtering and bucketing.
                             
 */
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class APM_UDSR_DealInfo implements IScript {

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

   if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
      compute_result_using_db(argt, returnt);
   else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
      format_result(returnt);

   Util.exitSucceed();
}


/*-------------------------------------------------------------------------------
Name:          compute_result_using_db()
Description:   deal info result using core db tables.
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void compute_result_using_db(Table argt, Table returnt) throws OException
{
   Table    tTranNums = null;        /* Not freeable */
   Table    tSimDef = null;          /* Not freeable */
   Table    tVersion = null;
   Table    tEnabledTranInfoFilters = null;
   Table    tEnabledDocInfoFilters = null;

   int         iQueryId = 0;
   int         iEnabledTranInfoFilterDetailsCol = 0;
   int         iEnabledDocInfoFilterDetailsCol = 0;
   int         major_version, minor_version, code_revision = 0;
   int         iSingleDealQueryCol = 0;
   int         iQueryIdProvided = 1;

      
   /* Get core version details */
   tVersion = Ref.getVersion();
   major_version = tVersion.getInt( "major_version", 1);
   minor_version = tVersion.getInt( "minor_version", 1);
   code_revision = tVersion.getInt( "code_revision", 1);
   tVersion.destroy();

   tTranNums   = argt.getTable( "transactions", 1);
   tSimDef     = argt.getTable( "sim_def", 1);

   /* If query ID is provided as a parameter, use it! */
   if (tSimDef != null && (iSingleDealQueryCol = tSimDef.getColNum( "APM Single Deal Query")) > 0)
      iQueryId = tSimDef.getInt(iSingleDealQueryCol, 1);

   /* If query ID was not set or left at zero, create a query ID from the list of transactions */
   if (iQueryId == 0)
   {  
      /* build up query result to get deals from ab.tran which match our sim result. */
      iQueryIdProvided = 0;
      iQueryId = APM_TABLE_QueryInsertN(tTranNums, "tran_num");
   }

   /* Query onto the database */
   load_from_database(returnt, iQueryId);
   
   /* Fix up notional and lots amounts */
   fix_notional(returnt);
   
   fixup_equity_ticker(returnt);
   fixup_com_fut_dates(returnt);
   fixup_opt_rate_fut_dates(returnt, major_version, minor_version, code_revision);
   fixup_com_opt_fut_dates(returnt, major_version, minor_version, code_revision);
   fixup_bond_dates(returnt);
   fixup_depfut_dates(returnt);

   /* If the UDSR was run beneath an APM Service, then the SimDef will include details of
      the enabled APM tran info filters based on cached APM filter configuration details */
   iEnabledTranInfoFilterDetailsCol = tSimDef.getColNum( "APM Enabled Tran Info Filters");
   if (iEnabledTranInfoFilterDetailsCol > 0)
      tEnabledTranInfoFilters = tSimDef.getTable( iEnabledTranInfoFilterDetailsCol, 1);
   else
      tEnabledTranInfoFilters = getTranInfofilters(tSimDef);

   gather_tran_info(returnt, iQueryId, tEnabledTranInfoFilters);
   gather_ins_info(returnt, iQueryId, tEnabledTranInfoFilters);

   /*------------------------------------------------------------------------------------------------
   //
   // TO-DO: This piece of code should be executed only for Endur cuts
   // V91R2SHELL or higher. V91R2 does not have the concept 
   // of contract.
   */
   iEnabledDocInfoFilterDetailsCol = tSimDef.getColNum( "APM Enabled Document Info Filters");
   if (iEnabledDocInfoFilterDetailsCol > 0)
      tEnabledDocInfoFilters = tSimDef.getTable( iEnabledDocInfoFilterDetailsCol, 1);
   else
      tEnabledDocInfoFilters = get_doc_info_filters(tSimDef);

   gather_doc_info_filters(returnt, iQueryId, tEnabledDocInfoFilters);
   //
   //------------------------------------------------------------------------------------------------

   String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
   if (envVar != null)
   {
		envVar = envVar.toUpperCase();
	  
		if (envVar.equals("TRUE"))
		{   
			returnt.clearGroupBy ();
			returnt.addGroupBy ("dealnum");
			returnt.addGroupBy ("tran_num");
			returnt.addGroupBy ("version");
			returnt.addGroupBy ("equity_ticker");
			returnt.groupBy ();
		}
	}
	
   /* cleanup if necessary */
   if (iEnabledTranInfoFilterDetailsCol <= 0) tEnabledTranInfoFilters.destroy();

   /* If query ID is provided as a parameter, somebody else should free it */
   if (iQueryIdProvided == 0 && iQueryId > 0)
   {
	   Query.clear(iQueryId);
   }
}


/*-------------------------------------------------------------------------------
Name:          fix_notional
Description:   Fix up notional and lots amounts
Parameters:    Table returnt  
Return Values:   
-------------------------------------------------------------------------------*/
void fix_notional(Table returnt)throws OException
{
	int insClass = 0;
	int insClassCol = returnt.getColNum("ins_class");
	int positionCol = returnt.getColNum("position");
	int mvalueCol = returnt.getColNum("mvalue");
	int notionalCol = returnt.getColNum("notional");
	int tickerCol = returnt.getColNum("ticker");
	int tickerReferenceCol = returnt.getColNum("ticker_reference");
	int tpriceCol = returnt.getColNum("tprice");
	    
	/* Fix up notional and lots amounts */
	int iNumRows = returnt.getNumRows();
	for (int i = 1; i <= iNumRows; i++) 
	{
	   insClass = returnt.getInt( insClassCol, i);

	   if (insClass == 1) /* If this is a share-able */
	   {
	      /* Replace the position with mvalue for shareables */
	      returnt.setDouble( positionCol, i, returnt.getDouble( mvalueCol, i));

	      /* Multiply the notional by position for shareables */
	      returnt.setDouble( notionalCol, i, returnt.getDouble( notionalCol, i) * returnt.getDouble( positionCol, i));
	         
	       /* For Ticker\Reference splitter, on share-ables, set the value to be that of the ticker */
	       returnt.setString( tickerReferenceCol, i, returnt.getString( tickerCol, i));
	    }
	      
	    /* Transform price into cost via multiplying by position */
	    returnt.setDouble(tpriceCol, i, returnt.getDouble(tpriceCol, i) * returnt.getDouble(positionCol, i));
	}	
}


/*-------------------------------------------------------------------------------
Name:          load_from_database
Description:   Query onto the database
Parameters:    Table returnt, int iQueryId   
Return Values:   
-------------------------------------------------------------------------------*/
void load_from_database(Table returnt, int iQueryId)throws OException
{
	String sWhat = "ab.tran_num, ab.deal_tracking_num dealnum, ab.version_number version, ab.ins_num insnum, ab.ins_class, " +
	    "ab.asset_type, ab.book, ab.buy_sell, ab.ins_type, ab.ins_sub_type, ab.internal_bunit, ab.internal_lentity, " +
	    "ab.external_bunit, ab.external_lentity, ab.unit, ab.last_update, ab.maturity_date, ab.internal_portfolio, ab.external_portfolio, " +
	    "ab.template_tran_num, ab.toolset, ab.trade_date, ab.trade_date trade_date2, ab.input_date, ab.internal_contact, ab.tran_status, ab.tran_type, " +
	    "ab.settle_date, ab.settle_date settle_date2, ab.price tprice, ab.position, ab.mvalue, pa.notnl notional, ab.start_date startdate, ab.maturity_date enddate, " + 
	    "prh.proj_index projindex, pa.disc_index disc_index, header.ticker ticker, header.ticker equity_ticker, " + 
	    "ab.reference ticker_reference, ab.reference reference, ab.personnel_id, pa.start_date param_startdate, pa.mat_date param_enddate, ab.broker_id broker_id, " +
	    "ab2.external_bunit exchange_pit, pa.param_seq_num param_seq_num_0, ata.party_agreement_id";

    String sFrom = "ab_tran ab left outer join ab_tran ab2 on (ab.ins_class = 1 and ab.ins_num = ab2.ins_num and ab2.tran_type = 2) left outer join ab_tran_agreement ata on (ab.tran_num = ata.tran_num), ins_parameter pa, param_reset_header prh, header, query_result qr";

    String sWhere = "ab.ins_num = pa.ins_num AND pa.param_seq_num = 0 AND " + 
         "prh.param_seq_num = 0 AND prh.param_reset_header_seq_num = 0 AND prh.ins_num = pa.ins_num AND " + 
         "header.ins_num = ab.ins_num AND " + 
         "qr.query_result = ab.tran_num AND qr.unique_id = " + iQueryId;

    int iRetVal = APM_TABLE_LoadFromDbWithSQL(returnt, sWhat, sFrom, sWhere);	
	
	if (returnt.getColType("last_update") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "last_update");
		
	if (returnt.getColType("maturity_date") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "maturity_date");
		
	if (returnt.getColType("trade_date") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "trade_date");
		
	if (returnt.getColType("trade_date2") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "trade_date2");
		
	if (returnt.getColType("input_date") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "input_date");
		
	if (returnt.getColType("settle_date") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "settle_date");
		
	if (returnt.getColType("settle_date2") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "settle_date2");
    
    if (returnt.getColType("startdate") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "startdate");
    
    if (returnt.getColType("enddate") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "enddate");  
    
    if (returnt.getColType("param_startdate") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "param_startdate");
    
    if (returnt.getColType("param_enddate") != COL_TYPE_ENUM.COL_INT.toInt())
    	returnt.colConvertDateTimeToInt( "param_enddate");  
}
/*-------------------------------------------------------------------------------
Name:          fixup_equity_ticker()
Description:   For deals from EquityOtcOpt toolset, replace ticker with underlying's ticker
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_equity_ticker(Table returnt) throws OException
{
   Table    tSupplementary = null, tSupplementary2 = null;
   String      sFrom, sWhat, sWhere;
   int         iSupQueryId = 0, iRetVal = 0;

   {
      tSupplementary = Table.tableNew("");
      tSupplementary.select( returnt, "insnum", "toolset EQ " + TOOLSET_ENUM.EQUITY_OTC_OPTION_TOOLSET.toInt()); /* 45 = TOOLSET_ENUM.EQUITY_OTC_OPTION_TOOLSET */

      if (tSupplementary.getNumRows() > 0)
      {
         iSupQueryId = APM_TABLE_QueryInsertN(tSupplementary, "insnum");
         tSupplementary.destroy();
         tSupplementary = Table.tableNew("");

         sWhat = "distinct ins_num, underlying_tran";
         sFrom = "tran_underlying_link, query_result qr";
         sWhere = "qr.query_result = tran_underlying_link.ins_num AND qr.unique_id = " + iSupQueryId;

         iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary, sWhat, sFrom, sWhere);
         Query.clear(iSupQueryId);

         if (tSupplementary.getNumRows() == 1) //exclude basket options
         {
            iSupQueryId = APM_TABLE_QueryInsertN(tSupplementary, "underlying_tran");
            tSupplementary2 = Table.tableNew("");

            sWhat = "ab.tran_num, header.ins_num, header.ticker";
            sFrom = "ab_tran ab, header, query_result qr";
            sWhere = "qr.query_result = ab.tran_num AND ab.ins_num = header.ins_num AND qr.unique_id = " + iSupQueryId;

            iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary2, sWhat, sFrom, sWhere);
            Query.clear(iSupQueryId);

            tSupplementary.select( tSupplementary2, "ticker", "tran_num EQ $underlying_tran");
            returnt.select( tSupplementary, "ticker (equity_ticker)", "ins_num EQ $insnum");

            tSupplementary2.destroy();            
         }
      }
      tSupplementary.destroy();
   }
}


/*-------------------------------------------------------------------------------
Name:          fixup_com_fut_dates()
Description:   Need to replace startdate and enddate for COM_FUTs with startdate and enddate from misc_ins
Parameters:    Table returnt   
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_com_fut_dates(Table returnt) throws OException
{
   Table tDistinctTran, tMiscIns;
   int iSubQueryId, iRetVal;
   String sWhat, sFrom, sWhere;

   /* Need to replace startdate and enddate for COM_FUTs with startdate and enddate from misc_ins */
   tDistinctTran = Table.tableNew();
   tDistinctTran.select( returnt, "DISTINCT, insnum, projindex", "toolset EQ " + Integer.toString(TOOLSET_ENUM.COM_FUT_TOOLSET.toInt()));
   if (tDistinctTran.getNumRows() > 0)
   {
      iSubQueryId = APM_TABLE_QueryInsertN(tDistinctTran, "insnum");

      tMiscIns = Table.tableNew();
      tMiscIns.addCols( "I(insnum) I(startdate) I(enddate)");
      sWhat = "mi.ins_num insnum, mi.first_delivery_date startdate, mi.last_delivery_date enddate, mi.last_trade_date, pa.settlement_type";
      sFrom = "misc_ins mi, query_result q, ins_parameter pa";
      sWhere = "mi.ins_num = q.query_result and pa.ins_num = q.query_result and q.unique_id = " + iSubQueryId;
      sWhere = sWhere + " and mi.param_seq_num = 0 and mi.misc_ins_seq_num = 0 and pa.param_seq_num = 0";

      iRetVal = APM_TABLE_LoadFromDbWithSQL(tMiscIns, sWhat, sFrom, sWhere);
      
      if(iRetVal != 0)
      {
         /* Overwrite original start and end dates by the delivery dates set on underlying ComFut instrument */
         use_com_fut_ins_dates(returnt, tDistinctTran, tMiscIns );
      }

      tMiscIns.destroy();
      Query.clear(iSubQueryId);
   }
   tDistinctTran.destroy();
}


/*-------------------------------------------------------------------------------
Name:          fixup_opt_rate_fut_dates()
Description:   
Parameters:    Table returnt   
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_opt_rate_fut_dates(Table returnt, int major_version, int minor_version, int code_revision) throws OException
{
   Table tDistinctTran, tUnderlyingTrans;
   int iSubQueryId, iRetVal;
   String sWhat, sFrom, sWhere;

   tDistinctTran = Table.tableNew();
   tDistinctTran.select( returnt, "DISTINCT,insnum", "toolset EQ " + Integer.toString(TOOLSET_ENUM.OPT_RATE_FUT_TOOLSET.toInt()));
   if (tDistinctTran.getNumRows() > 0)
   {
      iSubQueryId = APM_TABLE_QueryInsertN(tDistinctTran, "insnum");
      tUnderlyingTrans = Table.tableNew();
      tUnderlyingTrans.addCols( "I(insnum) I(startdate) I(enddate)");
      sWhat = "tul.ins_num insnum, pa.start_date startdate, pa.mat_date enddate";
      sFrom = "query_result q, parameter pa, tran_underlying_link tul, ab_tran ab";
      sWhere = "q.unique_id = " + iSubQueryId;
      sWhere = sWhere + " and tul.ins_num = q.query_result";
      sWhere = sWhere + " and tul.underlying_tran = ab.tran_num";
      sWhere = sWhere + " and ab.ins_num = pa.ins_num";
      sWhere = sWhere + " and pa.param_seq_num = 0";

      if( major_version >= 8 || (major_version == 7 && code_revision >= 2000) )
      {
         sWhere = sWhere + " and tul.param_seq_num = 0";
      }

      iRetVal = APM_TABLE_LoadFromDbWithSQL(tUnderlyingTrans, sWhat, sFrom, sWhere);


      if(iRetVal != 0)
      {
         returnt.select(tUnderlyingTrans,"startdate, enddate", "insnum EQ $insnum AND startdate GT 0" );
      }

      tUnderlyingTrans.destroy();
      Query.clear(iSubQueryId);
   }
   tDistinctTran.destroy();
}


/*-------------------------------------------------------------------------------
Name:          fixup_com_opt_fut_dates()
Description:   
Parameters:    Table returnt   
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_com_opt_fut_dates(Table returnt, int major_version, int minor_version, int code_revision) throws OException
{
   Table tDistinctTran, tUnderlyingTrans;
   int iSubQueryId, iRetVal;
   String sWhat, sFrom, sWhere;

   tDistinctTran = Table.tableNew();
   tDistinctTran.select( returnt, "DISTINCT, insnum, projindex", "toolset EQ " + Integer.toString(TOOLSET_ENUM.COM_OPT_FUT_TOOLSET.toInt()));
   if (tDistinctTran.getNumRows() > 0)
   {
      iSubQueryId = APM_TABLE_QueryInsertN(tDistinctTran, "insnum");
      tUnderlyingTrans = Table.tableNew();
      tUnderlyingTrans.addCols( "I(insnum) I(startdate) I(enddate)");

      sWhat = "tul.ins_num insnum, mi.first_delivery_date startdate, mi.last_delivery_date enddate, mi.last_trade_date, pa.settlement_type";
      sFrom = "query_result q, misc_ins mi, tran_underlying_link tul, ab_tran ab, ins_parameter pa";
      sWhere = "q.unique_id = " + iSubQueryId;
      sWhere = sWhere + " and tul.ins_num = q.query_result";
      sWhere = sWhere + " and tul.underlying_tran = ab.tran_num";
      sWhere = sWhere + " and pa.ins_num = q.query_result and pa.param_seq_num = 0";
      sWhere = sWhere + " and ab.ins_num = mi.ins_num";
      sWhere = sWhere + " and mi.param_seq_num = 0 and mi.misc_ins_seq_num = 0";

      if( major_version >= 8 || (major_version == 7 && code_revision >= 2000) )
      {
         sWhere = sWhere + " and tul.param_seq_num = 0";
      }

      iRetVal = APM_TABLE_LoadFromDbWithSQL(tUnderlyingTrans, sWhat, sFrom, sWhere);
      
      if(iRetVal != 0)
      {
         /* Overwrite original start and end dates by the delivery dates set on underlying ComFut instrument */
         use_com_fut_ins_dates( returnt, tDistinctTran, tUnderlyingTrans );
      }


      tUnderlyingTrans.destroy();
      Query.clear(iSubQueryId);
   }
   tDistinctTran.destroy();
}


/*-------------------------------------------------------------------------------
Name:          fixup_bond_dates()
Description:   
Parameters:    Table returnt   
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_bond_dates(Table returnt) throws OException
{
   int iNumRows, iLoop, iInsClass, iToolset, iStartDate, iSettleDate;
   int iInsClassCol, iToolsetCol, iStartDateCol, iSettleDateCol;

   /* Take start_date as the greater of start_date & settle_date for bonds, MM and Equities */
   iNumRows = returnt.getNumRows();
   iInsClassCol = returnt.getColNum( "ins_class");
   iToolsetCol = returnt.getColNum( "toolset");
   iStartDateCol = returnt.getColNum( "startdate");
   iSettleDateCol = returnt.getColNum( "settle_date");

   for (iLoop = 1; iLoop <= iNumRows; iLoop++)
   {
      iInsClass = returnt.getInt( iInsClassCol, iLoop);
      if (iInsClass == 1)
      {
         iToolset = returnt.getInt( iToolsetCol, iLoop);
         if (iToolset == TOOLSET_ENUM.BOND_TOOLSET.toInt() || iToolset == TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt() || iToolset == TOOLSET_ENUM.EQUITY_TOOLSET.toInt())
         {
            iStartDate = returnt.getInt( iStartDateCol, iLoop);
            iSettleDate = returnt.getInt( iSettleDateCol, iLoop);
            if (iSettleDate > iStartDate) 
               returnt.setInt( iStartDateCol, iLoop, iSettleDate);
         }
      }
   }
}

/*-------------------------------------------------------------------------------
Name:          fixup_depfut_dates()
Description:   
Parameters:    Table returnt   
Return Values:   
-------------------------------------------------------------------------------*/
void fixup_depfut_dates(Table returnt) throws OException
{
   int iNumRows, iLoop, iToolset, iParamStartDate, iParamEndDate;
   int iToolsetCol, iStartDateCol, iEndDateCol, iParamStartDateCol, iParamEndDateCol;

   /* Take start_date as the start date from param 0 for DEPFUTs */
   iNumRows = returnt.getNumRows();

   iToolsetCol = returnt.getColNum( "toolset");

   iStartDateCol = returnt.getColNum( "startdate");
   iParamStartDateCol = returnt.getColNum( "param_startdate");
   iEndDateCol = returnt.getColNum( "enddate");
   iParamEndDateCol = returnt.getColNum( "param_enddate");

   for (iLoop = 1; iLoop <= iNumRows; iLoop++)
   {
      iToolset = returnt.getInt( iToolsetCol, iLoop);
      if (iToolset == TOOLSET_ENUM.DEPFUT_TOOLSET.toInt())
      {
         iParamStartDate = returnt.getInt( iParamStartDateCol, iLoop);
         if (iParamStartDate > 0)
         {
            iParamEndDate = returnt.getInt( iParamEndDateCol, iLoop);
            returnt.setInt( iStartDateCol, iLoop, iParamStartDate);
            returnt.setInt( iEndDateCol, iLoop, iParamEndDate);
         }
      }
   }
}


/*-------------------------------------------------------------------------------
Name:          get_doc_info_filters()
Description:   
Parameters:    Table tSimDef   
Return Values: Returns a table of document info fields related to contracts (but not agreements)
-------------------------------------------------------------------------------*/
Table get_doc_info_filters(Table tSimDef) throws OException
{
   int iRetVal;
   Table tEnabledDocInfoFilters;

   OConsole.oprint ("Loading APM document info filter/splitter configuration from the database \n");
   tEnabledDocInfoFilters = Table.tableNew("Enabled Document Info Filters" );     

   /* Only contracts are connected to deal, agreement documents will have to be
      handled in different UDSR(s) */

   iRetVal = DBase.runSql(
      "select distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, aesr.column_name_append, tfd.filter_type " + 
      "from tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr " +
      "where tfd.filter_type in (12, 13) and tfd.filter_name = apec.enrichment_name " +
      "and apec.on_off_flag = 1 and aesr.enrichment_name = apec.enrichment_name ORDER by tfd.filter_type");

   if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runSql() failed to load APM Document Info filter/splitter configuration" ) );
   else
   {      
      iRetVal = DBase.createTableOfQueryResults(tEnabledDocInfoFilters);
      if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.createTableOfQueryResults() failed to load APM Document Info filter/splitter configuration" ) );
   }

   return tEnabledDocInfoFilters;

}

/*-------------------------------------------------------------------------------
Name:          getTranInfofilters()
Description:   
Parameters:    Table tSimDef   
Return Values: Table  
-------------------------------------------------------------------------------*/
Table getTranInfofilters(Table tSimDef) throws OException
{
   int iRetVal;
   Table tEnabledTranInfoFilters;

   OConsole.oprint ("Loading APM tran_info filter/splitter configuration from the database \n");
   /* No details of enable APM tran info filters provides, so load them up based on APM configuration */
   tEnabledTranInfoFilters = Table.tableNew("Enabled Tran Info Filters" );     
   iRetVal = DBase.runSql("select distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, aesr.column_name_append, tfd.filter_type " + 
                          "from tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr " +
                          "where tfd.filter_type in (5, 7, 8) and tfd.filter_name = apec.enrichment_name " +
                          "and apec.on_off_flag = 1 and aesr.enrichment_name = apec.enrichment_name");
   if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runSql() failed to load APM tran_info filter/splitter configuration" ) );
   else
   {      
      iRetVal = DBase.createTableOfQueryResults(tEnabledTranInfoFilters);
      if( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.createTableOfQueryResults() failed to load APM tran_info filter/splitter configuration" ) );      
   }

   return tEnabledTranInfoFilters;
}   

/*-------------------------------------------------------------------------------
Name:          gather_doc_info_filters()
Description:   
Parameters:    Table returnt, int iQueryId, Table tEnabledDocInfoFilters   
Return Values:   
-------------------------------------------------------------------------------*/
void gather_doc_info_filters(Table returnt, int iQueryId, Table tEnabledDocInfoFilters) throws OException
{
   Table tDocInfo;
   String sFrom, sWhat, sWhere, sColumnName;
   int iRetVal, iRow, iFilterId, iTranInfoId;
   int iNumRows = 0;
   int filterIdCol = 0 ;
   int refListIdCol = 0;
   int resultNameCol = 0;
   int numFilters = 0;

   iNumRows = tEnabledDocInfoFilters.getNumRows();
   if (iNumRows > 0)
   {
	  filterIdCol = tEnabledDocInfoFilters.getColNum("filter_id");
	  refListIdCol = tEnabledDocInfoFilters.getColNum("ref_list_id");
	  resultNameCol = tEnabledDocInfoFilters.getColNum("result_column_name");
	  
      sWhat = "qr.query_result tran_num ";
      sFrom = "query_result qr, ab_tran_agreement ata, party_agreement agr ";
      
      for (iRow = 1; iRow <= iNumRows; iRow++)
      {
         numFilters = numFilters + 1;
         iFilterId = tEnabledDocInfoFilters.getInt( filterIdCol, iRow);
         iTranInfoId = tEnabledDocInfoFilters.getInt( refListIdCol, iRow);
         sColumnName = tEnabledDocInfoFilters.getString( resultNameCol, iRow);

         sWhat = sWhat + ", doi_" + iFilterId + ".value " + "\"" + sColumnName + "\"";
         sFrom = sFrom + " left outer join doc_info doi_" + iFilterId + " on (agr.party_agreement_id = doi_" + iFilterId + ".doc_id AND doi_" + iFilterId + ".type_id = " + iTranInfoId + ")";
      }
      //sFrom = sFrom + " ON (ata.tran_num = qr.query_result) " ;
      sWhere = "qr.unique_id = " + iQueryId + " AND ata.tran_num = qr.query_result AND ata.party_agreement_id = agr.party_agreement_id ";

      /* if none switched on then skip it */
      if ( numFilters == 0 )
         return;
         
      tDocInfo = Table.tableNew("Document Info");
      iRetVal = APM_TABLE_LoadFromDbWithSQL(tDocInfo , sWhat, sFrom, sWhere);

      if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {   
         returnt.select( tDocInfo , "*", "tran_num EQ $tran_num");
      }
      tDocInfo.destroy();
   }

}

/*-------------------------------------------------------------------------------
Name:          gather_tran_info()
Description:   
Parameters:    Table returnt, int iQueryId, Table tEnabledTranInfoFilters   
Return Values:   
-------------------------------------------------------------------------------*/
void gather_tran_info(Table returnt, int iQueryId, Table tEnabledTranInfoFilters) throws OException
{
   Table tTranInfo;
   String sFrom, sWhat, sWhere, sColumnName, sColumnNameAppend;
   int iRetVal, iRow, iFilterId, iTranInfoId;
   int iNumRows = 0;
   int filterTypeCol = 0;
   int filterIdCol = 0 ;
   int refListIdCol = 0;
   int resultNameCol = 0;
   int numFilters = 0;
   int nameAppendCol = 0;

   iNumRows = tEnabledTranInfoFilters.getNumRows();
   if (iNumRows > 0)
   {
      /*
        For :
        filter_id 5007, tran_info_id 20057
        filter_id 5018, tran_info_id 20060

        select qr.query_result trannum, abt_20057.value fltr_5007, abt_20060.value fltr_5018
        from query_result qr
        left outer join ab_tran_info abt_20057 on (qr.query_result = abt_20057.tran_num AND abt_20057.type_id = 20057)
        left outer join ab_tran_info abt_20060 on (qr.query_result = abt_20060.tran_num AND abt_20060.type_id = 20060)
        where qr.unique_id = 4755574
      */
	  filterTypeCol = tEnabledTranInfoFilters.getColNum("filter_type");
	  filterIdCol = tEnabledTranInfoFilters.getColNum("filter_id");
	  refListIdCol = tEnabledTranInfoFilters.getColNum("ref_list_id");
	  resultNameCol = tEnabledTranInfoFilters.getColNum("result_column_name");
	  nameAppendCol = tEnabledTranInfoFilters.getColNum("column_name_append");
		
      sWhat = "qr.query_result tran_num";
      sFrom = "query_result qr";
      
      for (iRow = 1; iRow <= iNumRows; iRow++)
      {
         /* only tran info here (not ins info or param info) */
         if ( tEnabledTranInfoFilters.getInt( filterTypeCol, iRow) != 5 )
            continue;

         numFilters = numFilters + 1;
         iFilterId = tEnabledTranInfoFilters.getInt( filterIdCol, iRow);
         iTranInfoId = tEnabledTranInfoFilters.getInt( refListIdCol, iRow);
         sColumnName = tEnabledTranInfoFilters.getString( resultNameCol, iRow);
		 sColumnNameAppend = tEnabledTranInfoFilters.getString( nameAppendCol, iRow);
         sWhat = sWhat + ", abt_" + iFilterId + sColumnNameAppend + ".value " + "\"" + sColumnName + "\"";
         sFrom = sFrom + " left outer join ab_tran_info abt_" + iFilterId + sColumnNameAppend + " on (qr.query_result = abt_" + iFilterId + sColumnNameAppend + ".tran_num AND abt_" + iFilterId + sColumnNameAppend + ".type_id = " + iTranInfoId + ")";
      }
      sWhere = "qr.unique_id = " + iQueryId;

      // if none switched on then skip it
      if ( numFilters == 0 )
         return;
         
      tTranInfo = Table.tableNew("Tran Info");
      iRetVal = APM_TABLE_LoadFromDbWithSQL(tTranInfo, sWhat, sFrom, sWhere);
      if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {   
         returnt.select( tTranInfo, "*", "tran_num EQ $tran_num");
      }
      tTranInfo.destroy();
   }
}


/*-------------------------------------------------------------------------------
Name:          gather_ins_info()
Description:   
Parameters:    Table returnt, int iQueryId, Table tEnabledTranInfoFilters   
Return Values:   
-------------------------------------------------------------------------------*/
void gather_ins_info(Table returnt, int iQueryId, Table tEnabledTranInfoFilters) throws OException
{
   Table tTranInfo;
   String sFrom, sWhat, sWhere,  sColumnName, sColumnNameAppend;
   int iRetVal, iRow, iFilterId, iTranInfoId;
   int iNumRows = 0;
   int filterTypeCol = 0;
   int filterIdCol = 0 ;
   int refListIdCol = 0;
   int resultNameCol = 0;
   int numFilters = 0;
   int nameAppendCol = 0;

   iNumRows = tEnabledTranInfoFilters.getNumRows();
   if (iNumRows > 0)
   {
     /* 
        select ab.ins_num insnum, abt_5043.value "ins_info_string", abt_5060.value "ins_info_picklist"
        from query_result qr, ab_tran ab, ab_tran ab2 
        left outer join ab_tran_info abt_5043 on ( ab2.tran_num = abt_5043.tran_num AND abt_5043.type_id = 20028) 
        left outer join ab_tran_info abt_5060 on ( ab2.tran_num = abt_5060.tran_num AND abt_5060.type_id = 20029)
        where qr.unique_id = 473098
        and qr.query_result = ab.tran_num
        and ab2.ins_num = ab.ins_num
        and ab2.tran_type = 2 
      */

	  filterTypeCol = tEnabledTranInfoFilters.getColNum("filter_type");
	  filterIdCol = tEnabledTranInfoFilters.getColNum("filter_id");
	  refListIdCol = tEnabledTranInfoFilters.getColNum("ref_list_id");
	  resultNameCol = tEnabledTranInfoFilters.getColNum("result_column_name");
	  nameAppendCol = tEnabledTranInfoFilters.getColNum("column_name_append");
			
      sWhat = "distinct ab.ins_num insnum";
      sFrom = "query_result qr, ab_tran ab, ab_tran ab2";
      
      for (iRow = 1; iRow <= iNumRows; iRow++)
      {
         /* only ins info here (not ins info or param info) */
         if ( tEnabledTranInfoFilters.getInt( filterTypeCol, iRow) != 7 )
            continue;

         numFilters = numFilters + 1;
         iFilterId = tEnabledTranInfoFilters.getInt( filterIdCol, iRow);
         iTranInfoId = tEnabledTranInfoFilters.getInt( refListIdCol, iRow);
         sColumnName = tEnabledTranInfoFilters.getString( resultNameCol, iRow);
         sColumnNameAppend = tEnabledTranInfoFilters.getString( nameAppendCol, iRow);
         sWhat = sWhat + ", abt_" + iFilterId + sColumnNameAppend + ".value " + "\"" + sColumnName + "\"";
         sFrom = sFrom + " left outer join ab_tran_info abt_" + iFilterId + sColumnNameAppend + " on (ab2.tran_num = abt_" + iFilterId + sColumnNameAppend + ".tran_num AND abt_" + iFilterId + sColumnNameAppend + ".type_id = " + iTranInfoId + ")";
      }
      sWhere = "qr.unique_id = " + iQueryId + " and qr.query_result = ab.tran_num and ab2.ins_num = ab.ins_num and ab2.tran_type = 2";
         
      /* if none switched on then skip it */
      if ( numFilters == 0 )
         return;

      tTranInfo = Table.tableNew("Ins Info");
      iRetVal = APM_TABLE_LoadFromDbWithSQL(tTranInfo, sWhat, sFrom, sWhere);
      if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
      {   
         returnt.select( tTranInfo, "*", "insnum EQ $insnum");
      }
      tTranInfo.destroy();
   }
}


/*-------------------------------------------------------------------------------
Name:          use_com_fut_ins_dates()
Description:   Overwrites dates in the output table by the delivery dates of the 
               underlying ComFut instrument
Parameters:      
Return Values:   
-------------------------------------------------------------------------------*/
void use_com_fut_ins_dates(Table returnt, Table tDistinctTran, Table tUnderlyingComFutTrans ) throws OException
{
   int iRow = 0;
   int iNumRows = 0;
   int settlementTypeCol = 0;
   int startdateCol = 0;
   int enddateCol = 0;
   
   /* Overwrite original start and end dates */
   returnt.select( tUnderlyingComFutTrans ,"startdate, enddate", "insnum EQ $insnum AND startdate GT 0" );
   
   /* We need to deal with the case when delivery dates set on ComFut instrument are invalid. These are:
   // -instruments with the physical settlement and blank delivery dates ( normally not, but we are protecting ourselvers )
   // -cash settling instruments. Normally these one have the delivery dates left blank but if they are not it 
   //  means someone managed to corrupted Endur defaults. In any case we can't take these dates so cash settling ComFut 
   //  instruments always need a special treatment 
   */
         
   /* First leave only deals that needs the dates to be fixed. */
   settlementTypeCol = tUnderlyingComFutTrans.getColNum("settlement_type");
   startdateCol = tUnderlyingComFutTrans.getColNum("startdate");
      
   iNumRows = tUnderlyingComFutTrans.getNumRows();
   for (iRow = iNumRows; iRow > 0; iRow--)
   {
      if ( tUnderlyingComFutTrans.getInt( settlementTypeCol , iRow) != OPTION_SETTLEMENT_TYPE.SETTLEMENT_TYPE_CASH.toInt() && 
           tUnderlyingComFutTrans.getInt( startdateCol , iRow) > 0 )  
      {
           tUnderlyingComFutTrans.delRow( iRow);
      }
   }

   /* Now ready to deal with invalid dates - we'll work out the contract month based on the last trade date
   // NOTE : This assumes that they're all monthly !
   */
   if (tUnderlyingComFutTrans.getNumRows() > 0)
   {
      tUnderlyingComFutTrans.addCol( "contract", COL_TYPE_ENUM.COL_STRING);
      tUnderlyingComFutTrans.select( tDistinctTran, "projindex", "insnum EQ $insnum");
      Index.colConvertDateToContract(tUnderlyingComFutTrans, "projindex", "last_trade_date", "contract", "startdate");
      enddateCol = tUnderlyingComFutTrans.getColNum("enddate");
      
      iNumRows = tUnderlyingComFutTrans.getNumRows();
      for (iRow = iNumRows; iRow > 0; iRow--)
            tUnderlyingComFutTrans.setInt( enddateCol, iRow, OCalendar.getEOM(tUnderlyingComFutTrans.getInt( startdateCol, iRow)));
      /* Overwrite original start and end dates */
      returnt.select( tUnderlyingComFutTrans,"startdate, enddate", "insnum EQ $insnum AND startdate GT 0" );
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
   returnt.setColFormatAsRef( "ins_class",          SHM_USR_TABLES_ENUM.INS_CLASS_TABLE);
   returnt.setColFormatAsRef( "asset_type",         SHM_USR_TABLES_ENUM.ASSET_TYPE_TABLE );
   returnt.setColFormatAsRef( "buy_sell",           SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
   returnt.setColFormatAsRef( "ins_type",           SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
   returnt.setColFormatAsRef( "ins_sub_type",       SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);
   returnt.setColFormatAsRef( "internal_bunit",     SHM_USR_TABLES_ENUM.BUNIT_TABLE);
   returnt.setColFormatAsRef( "internal_lentity",   SHM_USR_TABLES_ENUM.LENTITY_TABLE);
   returnt.setColFormatAsRef( "external_bunit",     SHM_USR_TABLES_ENUM.BUNIT_TABLE);
   returnt.setColFormatAsRef( "external_lentity",   SHM_USR_TABLES_ENUM.LENTITY_TABLE);
   returnt.setColFormatAsRef( "unit",               SHM_USR_TABLES_ENUM.UNIT_TYPE_TABLE);
   returnt.setColFormatAsDate( "last_update",        DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "maturity_date",      DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
   returnt.setColFormatAsRef( "toolset",            SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
   returnt.setColFormatAsDate( "trade_date",         DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "input_date",         DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "internal_contact",   SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
   returnt.setColFormatAsRef( "tran_status",        SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
   returnt.setColFormatAsRef( "tran_type",          SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE);
   returnt.setColFormatAsDate( "settle_date",        DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "startdate",          DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "enddate",            DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "projindex",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "disc_index",         SHM_USR_TABLES_ENUM.INDEX_TABLE);
   returnt.setColFormatAsRef( "personnel_id",       SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
   returnt.setColFormatAsDate( "param_startdate",    DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsDate( "param_enddate",      DATE_FORMAT.DATE_FORMAT_DEFAULT);
   returnt.setColFormatAsRef( "broker_id",          SHM_USR_TABLES_ENUM.BUNIT_TABLE);
   returnt.setColFormatAsRef( "exchange_pit",       SHM_USR_TABLES_ENUM.BUNIT_TABLE);
   returnt.setColFormatAsRef( "external_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);


   returnt.colHide( "trade_date2");
   returnt.colHide( "settle_date2");
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

}
