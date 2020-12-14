/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_PowerInfo.java

Date Of Last Revision:     30-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back Power filter information from core db tables.

                            
*/
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_PowerInfo implements IScript {
	
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
	      format_result(argt);

	   Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   Gas filter info result using core db tables.
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   Table    tScenResult;      /* Not freeable */
	   Table    tTranResult;      /* Not freeable */
	   Table    tTranNums;        /* Not freeable */
	   Table    tSupplementary;
	   Table    tVersion;
	   int         iQueryId;
	   int         iRetVal;
	   int         row, myLeg, myDeal, prevDeal, myVal;
	   String      sFrom, sWhat, sWhere;
	   int         major_version, minor_version;
       int         iNumRows, dealnum_col, leg_col, power_location_col;
       int         power_ctl_area_col, power_region_col, power_product_col;
       
	   tTranNums   = argt.getTable( "transactions", 1);
	   tScenResult = argt.getTable( "sim_results", 1);
	   tTranResult = tScenResult.getTable( 1, 1);

	   returnt.addCols( "I(dealnum) I(trannum) I(ins_num) I(leg) I(power_location) S(power_location_str)"); /* Hidden column for financial deals */
	   returnt.addCols( "I(power_ctl_area) I(power_region) I(power_product) I(power_choice) I(power_initial_term)");
	   returnt.addCols( "I(power_service_type) I(power_timezone) I(power_loc_rec) I(power_loc_del)");

	   returnt.tuneGrowth( tTranResult.getNumRows());

	   /* Set up return table with necessary fields for keying TABLE_Select's into */
	   returnt.select( tTranResult, "deal_num (dealnum), ins_num, deal_leg (leg)", "deal_num GE 0");
	   returnt.select( tTranNums, "tran_num (trannum)", "deal_num EQ $dealnum");

	   /* get major & minor version */
	   tVersion = Ref.getVersion();
	   major_version = tVersion.getInt( "major_version", 1);
	   minor_version = tVersion.getInt( "minor_version", 1);
	   tVersion.destroy();

	   /* Retrieve the data from pwr_tran_aux_data, keyed by transaction number */
	   iQueryId = APM_TABLE_QueryInsertN(tTranNums, "tran_num");   

	   tSupplementary = Table.tableNew("Supplementary Power Info");

	   sWhat =  "pwr_tran_aux_data.tran_num, pwr_tran_aux_data.choice power_choice, " + 
	            "pwr_tran_aux_data.phys_tran_type power_initial_term, pwr_tran_aux_data.service_type power_service_type, " + 
	            "pwr_tran_aux_data.time_zone power_timezone, pwr_tran_aux_data.pt_of_receipt_loc power_loc_rec, " + 
	            "pwr_tran_aux_data.pt_of_delivery_loc power_loc_del, pwr_tran_aux_data.product power_product";

	   sFrom =  "pwr_tran_aux_data, query_result";

	   sWhere = "pwr_tran_aux_data.tran_num = query_result.query_result AND " + 
	            "query_result.unique_id =" + iQueryId;

	   iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary, sWhat, sFrom, sWhere);

	   /* In V80 versions product does not exist on pwr_phys_param - you have to go to pwr_tran_aux_data */
	   sWhat = "power_choice, power_initial_term, power_service_type, power_timezone, power_loc_rec, power_loc_del, power_product";

	   returnt.select( tSupplementary, sWhat, "tran_num EQ $trannum");
	   tSupplementary.destroy();

	   /* For ComFuts and ComOptFuts, can bring the Power Product from the Reset Pricing Product */
	   tSupplementary = Table.tableNew("Supplementary Power Info");

	   sWhat =  "ab.tran_num, ppr.pricing_product as power_product, ppr.param_seq_num as leg";
	   sFrom =  "pwr_phys_reset ppr, query_result qr, ab_tran ab";
	   sWhere = "ab.ins_num = ppr.ins_num AND ab.tran_num = qr.query_result AND qr.unique_id = " + iQueryId + 
	            " AND ab.toolset in (" + TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() + "," + TOOLSET_ENUM.COM_OPT_FUT_TOOLSET.toInt() + ")";

	   iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary, sWhat, sFrom, sWhere);

	   sWhat = "power_product, leg";

	   returnt.select( tSupplementary, sWhat, "tran_num EQ $trannum AND leg EQ $leg");
	   tSupplementary.destroy();

	   Query.clear(iQueryId);   

	   /* build up query result to get instrument numbers which match our sim result. */
	   iQueryId = APM_TABLE_QueryInsertN(tTranResult, "ins_num");

	   /* Query onto the database */
	   tSupplementary = Table.tableNew("Supplementary Power Info");

	   /* In V80 versions product does not exist on pwr_phys_param - you can only go to pwr_tran_aux_data
	      However, pwr_phys_param does not exist for cash-settling physical swaps, so for these, the power 
	      product should be populated from pwr_tran_aux_data (already done above) */

	   /* Anything you add to sWhat here, icons_enum.check that it gets populated on the corresponding financial leg (logic is below) */
	   sWhat = "pwr_phys_param.ins_num ins_num, pwr_phys_param.param_seq_num leg, pwr_phys_param.location_id power_location, pwr_locations.location_ctl_area power_ctl_area, pwr_control_area.ctl_area_region power_region";
	   if ( (major_version == 8 && minor_version >= 1) || major_version > 8 )
	      sWhat = sWhat + ", pwr_phys_param.product power_product";

	   sFrom = "pwr_phys_param, pwr_locations, pwr_control_area, query_result";
	   
	   sWhere = "pwr_phys_param.ins_num = query_result.query_result AND " + 
	            "pwr_phys_param.location_id = pwr_locations.location_id AND " + 
	            "pwr_control_area.ctl_area_id = pwr_locations.location_ctl_area AND " + 
	            "query_result.unique_id =" + iQueryId;

	   iRetVal = APM_TABLE_LoadFromDbWithSQL(tSupplementary, sWhat, sFrom, sWhere);

	   /* In V80 versions product does not exist on pwr_phys_param - you can only go to pwr_tran_aux_data */
	   sWhat = "power_location, power_ctl_area, power_region";
	   if ( (major_version == 8 && minor_version >= 1) || major_version > 8 )
	      sWhat = sWhat + ", power_product";

	   returnt.select( tSupplementary, sWhat, "ins_num EQ $ins_num AND leg EQ $leg");
	   tSupplementary.destroy();
	   
	   returnt.addGroupBy( "dealnum");   
	   returnt.addGroupBy( "leg");

	   dealnum_col = returnt.getColNum("dealnum");
	   leg_col = returnt.getColNum("leg");
	   power_location_col = returnt.getColNum("power_location");
	   power_ctl_area_col = returnt.getColNum("power_ctl_area");
	   power_region_col = returnt.getColNum("power_region");
	   power_product_col = returnt.getColNum("power_product");
			   
	   /* Now fill out the financial legs (odd, 1+) */
	   iNumRows = returnt.getNumRows();
	   for (row = 1; row <= iNumRows; row++)
	   {
	      myDeal = returnt.getInt( dealnum_col, row);
	      myLeg = returnt.getInt( leg_col, row);
	      myVal = returnt.getInt( power_location_col, row);

	      if (row > 1)
	      {
	         prevDeal = returnt.getInt( dealnum_col, row-1);
	      }
	      else
	      {
	         prevDeal = -1;
	      }

	      if (((myLeg % 2) == 1) && (myVal == 0) && (prevDeal == myDeal))
	      {
	         /* Enrich with the corresponding physical leg's values (previous row) */
	         returnt.setInt( power_location_col, row, returnt.getInt( power_location_col, row-1));         
	         returnt.setInt( power_ctl_area_col, row, returnt.getInt( power_ctl_area_col, row-1));
	         returnt.setInt( power_region_col, row, returnt.getInt( power_region_col, row-1));
	         returnt.setInt( power_product_col, row, returnt.getInt( power_product_col, row-1));
	      }
	   }

	   AddFinancialDealsLocations(iQueryId, returnt);

	   Query.clear(iQueryId);   

	   returnt.groupBy();  
	}

	/*-------------------------------------------------------------------------------
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
	   returnt.setColFormatAsRef( "power_location",         SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
	   returnt.setColFormatAsRef( "power_ctl_area",         SHM_USR_TABLES_ENUM.PWR_CTL_AREA_TABLE);
	   returnt.setColFormatAsRef( "power_region",           SHM_USR_TABLES_ENUM.PWR_REGION_TABLE);
	   returnt.setColFormatAsRef( "power_product",          SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);
	   returnt.setColFormatAsRef( "power_choice",           SHM_USR_TABLES_ENUM.PWR_CHOICE_TABLE);
	   returnt.setColFormatAsRef( "power_initial_term",     SHM_USR_TABLES_ENUM.INITIAL_TERM_TABLE);
	   returnt.setColFormatAsRef( "power_service_type",     SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);
	   returnt.setColFormatAsRef( "power_timezone",         SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
	   returnt.setColFormatAsRef( "power_loc_rec",          SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
	   returnt.setColFormatAsRef( "power_loc_del",          SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);

	   returnt.colHide( "power_location_str");
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

	void AddFinancialDealsLocations(int iQueryId, Table tData) throws OException
	{
	   int i, locID, infoTypeID=0, numIndexInfoRows, iNumRows;
	   int power_location_str_col, power_location_col, power_loc_rec_col, power_loc_del_col;
	   Table tFinDeals, tBlankLocDeals, tIndexInfoPresent;
	   String   sFrom, sWhat, sWhere, sIndexInfoName;

	   sIndexInfoName = "'Default Power Location ID'";

	   sWhat = "type_id";
	   sFrom = "idx_info_types";
	   sWhere = "type_name = " + sIndexInfoName;
	   tIndexInfoPresent = Table.tableNew();
	   APM_TABLE_LoadFromDbWithSQL( tIndexInfoPresent, sWhat, sFrom, sWhere);
	   
	   if(tIndexInfoPresent != null)
	   {
		  numIndexInfoRows = tIndexInfoPresent.getNumRows();
	      if (numIndexInfoRows == 0)
	      {
	         tIndexInfoPresent.destroy();
	         return;
	      }
	      else if (numIndexInfoRows == 1)
	         infoTypeID = tIndexInfoPresent.getInt( 1, 1);
	      else if (numIndexInfoRows > 1)
	      {
	         tIndexInfoPresent.destroy();
	         APM_Print("Found more than one Index Info entry of name " + sIndexInfoName + ".  If locations for financial deals are required please correct.");
	         return;
	      }
	      tIndexInfoPresent.destroy();
	   }

	   tFinDeals = Table.tableNew("Fin Deals");
	   tBlankLocDeals = Table.tableNew("Blank Loc Deals");

	   /* The order of columns matches the final returnt format */
	   sWhat =    "DISTINCT ab_tran.deal_tracking_num dealnum, ab_tran.tran_num trannum, ab_tran.ins_num, pa.param_seq_num leg, pa.param_seq_num power_location, ii.info_value power_location_str," +
	              "pwr_locations.location_ctl_area power_ctl_area, pwr_control_area.ctl_area_region power_region, ppr.pricing_product power_product, " +
	              "0 power_choice, 0 power_initial_term, 0 power_service_type, 0 power_timezone, 0 power_loc_rec, 0 power_loc_del";

	   sFrom = "ab_tran, ins_parameter pa, param_reset_header prh, query_result, idx_info ii, idx_def id, pwr_locations, pwr_control_area, pwr_phys_reset ppr";
	   
	   sWhere =   "ab_tran.ins_num = query_result.query_result AND " + 
	              "pa.ins_num = ab_tran.ins_num AND " +
	              "pa.ins_num = prh.ins_num AND " +
	              "prh.param_seq_num = pa.param_seq_num AND prh.param_reset_header_seq_num = 0 AND " +
	              "prh.proj_index = id.index_id AND id.db_status = 1 AND id.index_version_id = ii.index_id AND " +
	              "ii.type_id = " + infoTypeID + " AND " +
	              "pwr_locations.location_id = ii.info_value AND pwr_locations.location_ctl_area = pwr_control_area.ctl_area_id AND " + 
	              "ppr.ins_num = ab_tran.ins_num AND " +
	              "query_result.unique_id =" + iQueryId;

	   APM_TABLE_LoadFromDbWithSQL( tFinDeals, sWhat, sFrom, sWhere);

	   if (tFinDeals != null && (tFinDeals.getNumRows() > 0))
	   {
	      /* We retrieved location as an Index Info field value, which is a String - convert to int */
		  power_location_str_col = tFinDeals.getColNum("power_location_str");
		  power_location_col = tFinDeals.getColNum("power_location");
		  power_loc_rec_col = tFinDeals.getColNum("power_loc_rec"); 
		  power_loc_del_col = tFinDeals.getColNum("power_loc_del");
		  iNumRows = tFinDeals.getNumRows();
	      for (i = 1; i <= iNumRows; i++)
	      {
	         locID = Integer.parseInt(tFinDeals.getString( power_location_str_col, i));
	         tFinDeals.setInt( power_location_col, i, locID);
	         tFinDeals.setInt( power_loc_rec_col, i, locID);
	         tFinDeals.setInt( power_loc_del_col, i, locID);
	      }

	      tBlankLocDeals.select( tData, "dealnum, trannum, ins_num, leg", "power_location EQ 0");

	      tBlankLocDeals.select( tFinDeals, "*", "dealnum EQ $dealnum AND trannum EQ $trannum AND leg EQ $leg");
	      tData.select( tBlankLocDeals, "*", "dealnum EQ $dealnum AND trannum EQ $trannum AND leg EQ $leg");

	      tFinDeals.destroy();
	   }
	   tBlankLocDeals.destroy();
	}

	void APM_Print(String sProcessingMessage) throws OException
	{
	   String sMsg;

	   sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + sProcessingMessage + "\n";
	   OConsole.oprint(sMsg);
	}


	}
