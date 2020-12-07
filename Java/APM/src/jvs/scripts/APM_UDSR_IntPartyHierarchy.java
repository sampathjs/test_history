/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_IntPartyHierarchy.java

Date Of Last Revision:     30-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result 
Script Type:               Main
Description:               User defined Sim Result which brings back internal party hierarchy details from core db tables.
	                       It is used to enrich sim data for APM filter/treeview/splitters.

	                       How low do we go ... this needs to match APM filter/teeview/splitter metadata
	                       Unfortunately, the APM metadata cannot adjust to match the depth of any particular customer setup
	
                            
*/
package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_IntPartyHierarchy implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
   	
	int MAX_BU_TIER_LEVELS = 10;
	int MAX_LE_TIER_LEVELS = 5;

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
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
	   int iTier;

	   for (iTier = 1; iTier <= MAX_BU_TIER_LEVELS; iTier++)
	   {
	      returnt.setColFormatAsRef( "bu_tier" + iTier, SHM_USR_TABLES_ENUM.PARTY_TABLE);
	   }
	   for (iTier = 1; iTier <= MAX_LE_TIER_LEVELS; iTier++)
	   {
	      returnt.setColFormatAsRef( "le_tier" + iTier, SHM_USR_TABLES_ENUM.PARTY_TABLE);
	   }
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   Fill in details of the internal business unit tiers 
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   int iRetVal;
	   int iRow, iNumRows;
	   int iNumTiers, iTier;
	   Table tBusUnitTiers, tLegalEntityTiers, tScenResult, tGenResult, tAPMDealInfoResult=null;
	   int iIntBusUnitId, iDealNum, iIntLegalEntity;
	   int iPreviousPartyId = -1;
	   String sErrMsg= "";
	   String envVar;
	   int iScenId, iAPMDealInfoResultID;
       int result_type_col, internal_bunit_col, internal_lentity_col, dealnum_col;
       Table tAPMDealInfoResultCopy = null;
	   Boolean qaMode = false;
       
	   iRetVal = 1;

	   iScenId = argt.getInt( "scen_id", 1);
	   tScenResult = argt.getTable( "sim_results", 1);
	   tGenResult = tScenResult.getTable( 1, 4);

	   /* This result depends on the USER_RESULT_APM_DEAL_INFO result ... */
	   iAPMDealInfoResultID = SimResult.getResultIdFromEnum("USER_RESULT_APM_DEAL_INFO");
	   
	   result_type_col = tGenResult.getColNum("result_type");
	   iNumRows = tGenResult.getNumRows();
	   for(iRow=1; iRow<=iNumRows; iRow++)
	   {
	      if (tGenResult.getInt( result_type_col, iRow) == iAPMDealInfoResultID)
	      {       
	         tAPMDealInfoResult = tGenResult.getTable( "result", iRow);
	         break;
	      }
	   }   

	   if(tAPMDealInfoResult == null)
	   {
	      argt.setString( "error_msg", 1, "No APM Deal Info results for scenario " + iScenId + "...Unable to continue.");
	      throw new OException("No APM Deal Info results for scenario " + iScenId + "...Unable to continue.");
	   }

	   /* Setup the returnt columns ... */
	   returnt.addCol( "dealnum", COL_TYPE_ENUM.COL_INT);
	   for (iTier = 1; iTier <= MAX_BU_TIER_LEVELS; iTier++)
	   {
	      returnt.addCol( "bu_tier" + iTier, COL_TYPE_ENUM.COL_INT);
	   }
	   for (iTier = 1; iTier <= MAX_LE_TIER_LEVELS; iTier++)
	   {
	      returnt.addCol( "le_tier" + iTier, COL_TYPE_ENUM.COL_INT);
	   }
	   returnt.addCols( "I(internal_bunit) I(internal_lentity)");

	   returnt.tuneGrowth( tAPMDealInfoResult.getNumRows());
	   
	   //Preserving the sorting of DealInfo Table from TABLE_Select if AB_APM_QA_MODE is set to true
	   envVar = SystemUtil.getEnvVariable ("AB_APM_QA_MODE");
	   if (envVar != null)
	   {
		   envVar = envVar.toUpperCase();
		   qaMode = envVar.equals("TRUE");
	   }
	   
	   if(qaMode)
	   {
		   tAPMDealInfoResultCopy = tAPMDealInfoResult.copyTable();
		   returnt.select( tAPMDealInfoResultCopy, "dealnum, internal_bunit, internal_lentity", "dealnum GT 0");
		   tAPMDealInfoResultCopy.destroy();
	   }
	   else
	   {
		   returnt.select( tAPMDealInfoResult, "dealnum, internal_bunit, internal_lentity", "dealnum GT 0");
	   }
	   /* Sort for optimized processing later */
	   returnt.sortCol( "internal_bunit");

	   tBusUnitTiers = Table.tableNew("BusUnitTiers");
       tBusUnitTiers.addCol( "TierPartyId", COL_TYPE_ENUM.COL_INT);
       tLegalEntityTiers = Table.tableNew("LegalEntityTiers");
       tLegalEntityTiers.addCol( "TierPartyId", COL_TYPE_ENUM.COL_INT);
       
       internal_bunit_col = returnt.getColNum("internal_bunit");
       internal_lentity_col = returnt.getColNum("internal_lentity");
       dealnum_col = returnt.getColNum("dealnum");
    	   
	   /* Iterate Math.round each row ... */
	   iNumRows = returnt.getNumRows();
	   for (iRow = 1; (iRetVal ==1 && (iRow <= iNumRows )); iRow++)
	   {
	      iIntBusUnitId = returnt.getInt( internal_bunit_col, iRow);
	      /* Optimized iteration ... see sort above */
	      if (iIntBusUnitId != iPreviousPartyId)
	      {
	         iPreviousPartyId = iIntBusUnitId;
	         iIntLegalEntity = returnt.getInt( internal_lentity_col, iRow);
	         
	         if (GetHierarchyTiersForIntBusUnit (iIntBusUnitId, iIntLegalEntity, tBusUnitTiers, tLegalEntityTiers) == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	         {
	            /* Copy the business unit hierarchy across ... */
	            iNumTiers = tBusUnitTiers.getNumRows();
	            if (iNumTiers > MAX_BU_TIER_LEVELS) 
	            {
	               sErrMsg = "Business unit hierarchy for party : '" + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, iIntBusUnitId) + "' is " +
	                  iNumTiers + " deep which exceeds limit of : " + MAX_BU_TIER_LEVELS + "\n";
	               iRetVal = 0;
	            }
	            else
	            {
	               for (iTier = 1; iTier <= iNumTiers; iTier++)
	                  returnt.setInt( "bu_tier" + iTier, iRow, tBusUnitTiers.getInt( 1, iTier));

	               /* Copy the legal entity hierarchy across ... */
	               iNumTiers = tLegalEntityTiers.getNumRows();
	               if (iNumTiers > MAX_LE_TIER_LEVELS) 
	               {
	                  sErrMsg = "Legal entity hierarchy for party : '" + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, iIntBusUnitId) + "' is " +
	                     iNumTiers + " deep which exceeds limit of : " + MAX_BU_TIER_LEVELS + "\n";
	                  iRetVal = 0;
	               }
	               else
	               {
	                  for (iTier = 1; iTier <= iNumTiers; iTier++)
	                     returnt.setInt( "le_tier" + iTier, iRow, tLegalEntityTiers.getInt( 1, iTier));
	               }               
	            }
	         }
	         else
	         {
	            sErrMsg = "Failed to load party hierarchy for party : '" + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, iIntBusUnitId) + "'\n";
	            iRetVal = 0;
	         }

	         tBusUnitTiers.clearRows();
	         tLegalEntityTiers.clearRows();

	         if (iRetVal !=1)
	         {
	            argt.setString( "error_msg", 1, sErrMsg);
	            break;
	         }
	      }
	      else
	      {
	         /* Copy (all bar dealnum) from previous row if the internal business unit matched */
	         iDealNum = returnt.getInt( dealnum_col, iRow);
	         returnt.copyRow( iRow-1, returnt, iRow);  
	         returnt.setInt( dealnum_col, iRow, iDealNum);
	      }
	   }

	   tBusUnitTiers.destroy();
       tLegalEntityTiers.destroy();
       
	   /* Don't need the internal_bunit & lentity columns any more ... */
	   returnt.delCol( "internal_bunit");
	   returnt.delCol( "internal_lentity");
	   
	   // Sorting table for when AB_APM_QA_MODE is set to true
	   if (qaMode)
	   {
	      returnt.clearGroupBy();
	      returnt.addGroupBy( "dealnum");
	      returnt.groupBy();
	   }
	   
	   if (iRetVal!=1) 
		   throw new OException(sErrMsg);
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
	Name:          ForceCacheRefreshIfNecessary()
	Description:   Drops in-memory cached tables if party_structure has changed since 
	               the last run (based on max(last_update) from party_structure_history).
	               Note that CachedIntPartyHierarchyLastUpdateDateTime table is cached
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	int ForceCacheRefreshIfNecessary () throws OException
	{
	   Table tCachedIntPartyHierarchyLastUpdateDateTime=null, tPartyStructureLastUpd;
	   int iRetVal, iLastUpdDate, iLastUpdTime;

	   tCachedIntPartyHierarchyLastUpdateDateTime = Table.getCachedTable ( "CachedIntPartyHierarchyLastUpdateDateTime" );
	   if (Table.isTableValid( tCachedIntPartyHierarchyLastUpdateDateTime ) != 1)
	   {
	      tCachedIntPartyHierarchyLastUpdateDateTime = Table.tableNew("tCachedIntPartyHierarchyLastUpdateDateTime");
	      tCachedIntPartyHierarchyLastUpdateDateTime.addCols( "I(date) I(time)");      
	      tCachedIntPartyHierarchyLastUpdateDateTime.addRow();
	      Table.cacheTable ( "CachedIntPartyHierarchyLastUpdateDateTime", tCachedIntPartyHierarchyLastUpdateDateTime );
	   }

	   /* Get the last update date/time for the party_structure from the DB ... can't really avoid this DB hit */
	   tPartyStructureLastUpd = Table.tableNew("Pty Strct chg datetime");
	   tPartyStructureLastUpd.addCol( "last_update", COL_TYPE_ENUM.COL_DATE_TIME);
	   iRetVal = APM_TABLE_LoadFromDbWithSQL(tPartyStructureLastUpd, "max(last_update) last_update", "party_structure_history", "1=1");
	   if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	   {
	      if (tPartyStructureLastUpd.getNumRows() > 0)
	      {
	         iLastUpdDate = tPartyStructureLastUpd.getDate( 1, 1);
	         iLastUpdTime = tPartyStructureLastUpd.getTime( 1, 1);

	         /* If the last update date/time does not match, force a refresh */
	         if ((iLastUpdDate != tCachedIntPartyHierarchyLastUpdateDateTime.getInt( 1, 1) ||
	              (iLastUpdTime != tCachedIntPartyHierarchyLastUpdateDateTime.getInt( 2, 1))))
	         {
	            Table.destroyCachedTable("CachedIntBusUnitHierarchyInfo");
	            Table.destroyCachedTable("CachedIntPartyHierarchies");
	            tCachedIntPartyHierarchyLastUpdateDateTime.setInt( 1, 1, iLastUpdDate);
	            tCachedIntPartyHierarchyLastUpdateDateTime.setInt( 2, 1, iLastUpdTime);
	         }
	      }    
	      else
	      {
	         iRetVal = DB_RETURN_CODE.SYB_RETURN_APP_FAILURE.toInt();
	         OConsole.oprint ("No rows returned from 'select max(last_update) from party_structure_history'");
	      }
	   }
	   tPartyStructureLastUpd.destroy();
	   return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          GetHierarchyTiersForParty
	Description:   Get the party hierarchy for the specified Party.
	               Note that CachedIntPartyHierarchies table is cached
	               for performance
	Parameters:    
	Return Values: Party hierarchy, or null for failure
	-------------------------------------------------------------------------------*/
	Table GetHierarchyTiersForParty (int iPartyId) throws OException
	{
	   Table tTiers, tCachedIntPartyHierarchies;
	   int iHierarchyRow, iParentId, iParentRow;
	   int iRetVal;

	   /* Cache the DB party_structure if not already ... */
	   tCachedIntPartyHierarchies = Table.getCachedTable("CachedIntPartyHierarchies"); 
	   if(Table.isTableValid(tCachedIntPartyHierarchies) == 0)
	   {
	      OConsole.oprint ("Loading internal party hierarchy from the database\n");
	      tCachedIntPartyHierarchies = Table.tableNew("CachedIntPartyHierarchies");      
	      /* Load party structure for int_ext:Internal */
	      iRetVal = APM_TABLE_LoadFromDbWithSQL(tCachedIntPartyHierarchies, "p.party_id, ps.parent_id", "party p, party_structure ps", "p.int_ext = 0 and p.party_id = ps.child_id");
	      if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) 
	      {
	         tCachedIntPartyHierarchies.destroy();
	         return null;
	      }
	      tCachedIntPartyHierarchies.sortCol( "party_id");
	      Table.cacheTable("CachedIntPartyHierarchies", tCachedIntPartyHierarchies); 
	   }

	   tTiers = Table.tableNew("tiers_" + iPartyId);
	   tTiers.addCol( "TierPartyId", COL_TYPE_ENUM.COL_INT);
	   
	   /* Add this internal business unit as the last tier  */
	   tTiers.addRow();
	   tTiers.setInt( 1, 1, iPartyId);
	   
	   iHierarchyRow = tCachedIntPartyHierarchies.findInt( "party_id", iPartyId, SEARCH_ENUM.FIRST_IN_GROUP);
	   if (iHierarchyRow > 0)
	   {
	      iParentId = tCachedIntPartyHierarchies.getInt( "parent_id", iHierarchyRow);
	      while (iParentId > 0)
	      {
	         tTiers.insertRowBefore( 1);
	         tTiers.setInt( 1, 1, iParentId);
	         iParentRow = tCachedIntPartyHierarchies.findInt( "party_id", iParentId, SEARCH_ENUM.FIRST_IN_GROUP);
	         if (iParentRow < 1) break;
	         iParentId = tCachedIntPartyHierarchies.getInt( "parent_id", iParentRow);          
	      }
	   }
	   return tTiers;
	}

	/*-------------------------------------------------------------------------------
	Name:          GetHierarchyTiersForIntBusUnit
	Description:   Get the internal business unit & legal entity hierarchy for the specified 
	               Internal business unit.
	               Note that CachedIntBusUnitHierarchyInfo table is cached
	               for performance
	Parameters:    
	Return Values: DB_RETURN_CODE.SYB_RETURN_SUCCEED for success
	-------------------------------------------------------------------------------*/
	int GetHierarchyTiersForIntBusUnit (int iIntBusUnitId, int iIntLegalEntity, Table tBusUnitTiers, Table tLegalEntityTiers) throws OException
	{
	   Table tCachedIntBusUnitHierarchyInfo, tTiers;
	   int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
	   int iIntBusUnitIdRow;

	   iRetVal = ForceCacheRefreshIfNecessary();
	   if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) return iRetVal;
	   
	   /* Create Cache for Internal business unit Party hierarchy. This is filled 'on touch' */
	   tCachedIntBusUnitHierarchyInfo = Table.getCachedTable("CachedIntBusUnitHierarchyInfo"); 
	   if(Table.isTableValid(tCachedIntBusUnitHierarchyInfo) == 0)
	   {
	      tCachedIntBusUnitHierarchyInfo = Table.tableNew("CachedIntBusUnitHierarchyInfo");      
	      tCachedIntBusUnitHierarchyInfo.addCols( "I(int_bunit_id) A(bu_tiers) A(le_tiers)");
	      tCachedIntBusUnitHierarchyInfo.sortCol( "int_bunit_id");
	      Table.cacheTable("CachedIntBusUnitHierarchyInfo", tCachedIntBusUnitHierarchyInfo); 
	   }
	   
	   iIntBusUnitIdRow = tCachedIntBusUnitHierarchyInfo.findInt( "int_bunit_id", iIntBusUnitId, SEARCH_ENUM.FIRST_IN_GROUP);
	   if (iIntBusUnitIdRow <= 0)
	   {
	      iIntBusUnitIdRow = iIntBusUnitIdRow * -1;      
	      tCachedIntBusUnitHierarchyInfo.insertRowBefore( iIntBusUnitIdRow );
	      tCachedIntBusUnitHierarchyInfo.setInt( "int_bunit_id", iIntBusUnitIdRow, iIntBusUnitId);
	   }

	   /* Get the business unit hierarchy for this business unit */
	   tTiers = tCachedIntBusUnitHierarchyInfo.getTable( "bu_tiers", iIntBusUnitIdRow);
	   if(Table.isTableValid(tTiers) == 0)
	   {
	      tTiers = GetHierarchyTiersForParty (iIntBusUnitId);
	      if(Table.isTableValid(tTiers) == 0) return DB_RETURN_CODE.SYB_RETURN_APP_FAILURE.toInt();
	      tCachedIntBusUnitHierarchyInfo.setTable( "bu_tiers", iIntBusUnitIdRow, tTiers);
	   }
	   tTiers.copyRowAddAll( tBusUnitTiers);

	   /* Get the legal entity hierarchy for this business unit */
	   tTiers = tCachedIntBusUnitHierarchyInfo.getTable( "le_tiers", iIntBusUnitIdRow);
	   if(Table.isTableValid(tTiers) == 0)
	   {
	      tTiers = GetHierarchyTiersForParty (iIntLegalEntity);
	      if(Table.isTableValid(tTiers) == 0) return DB_RETURN_CODE.SYB_RETURN_APP_FAILURE.toInt();
	      tCachedIntBusUnitHierarchyInfo.setTable( "le_tiers", iIntBusUnitIdRow, tTiers);
	   }
	   tTiers.copyRowAddAll( tLegalEntityTiers);
	   
	   return DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
	}


	}
