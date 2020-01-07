/* Released with version 29-Aug-2019_V17_0_124 of APM */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// GENERATES ALL INCREMENTAL STATS FOR ALL SERVICES FROM THE START OF YESTERDAY
// USEFUL WHEN MULTIPLE APM SERVICES ARE CONFIGURED
// CONFIGURE A WORKFLOW TO RUN THIS SCRIPT OR RUN VIA A TASK
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package standard.apm.utilities;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;

public class APM_ViewAllIncrementals_Yest implements IScript 
{
	public void execute(IContainerContext arg0) throws OException 
	{
	   String sServiceName;
	
	   Table arg_table;
	   Table target_table;
	   Table tServices;
	   Table outputTable = Util.NULL_TABLE;
	   Table tSecondaryEntityNums, tSupplementary;
	   int retval, i, iSupQueryId;
	   ODateTime dStartDateTime; 
	   ODateTime dEndDateTime; 
	
	   OConsole.oprint("!!! Starting to generate Incremental history stats from start of yesterday for all APM services !!!!\n");
	
	   dStartDateTime = ODateTime.dtNew();
	   dStartDateTime.setDate(OCalendar.getServerDate() - 1); /* FROM DATE (start of yesterday) */
	
	   dEndDateTime = ODateTime.dtNew();
	   dEndDateTime.setDate(OCalendar.getServerDate()); /* TO DATE - i.e. CURRENT DATETIME */
	   dEndDateTime.setTime(Util.timeGetServerTime());
	
	   arg_table= Table.tableNew();
	   arg_table.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
	   arg_table.addCol("start_date", COL_TYPE_ENUM.COL_DATE_TIME);
	   arg_table.addCol("end_date", COL_TYPE_ENUM.COL_DATE_TIME);
	   arg_table.addRow();
	
	   // The first argument, the setting name
	   arg_table.setDateTime(2, 1, dStartDateTime); 
	   arg_table.setDateTime( 3, 1, dEndDateTime); 
	
	   tServices = Table.tableNew();
	   retval = getListOfActiveAPMServices(tServices);
	
	   if ( retval == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
	   {
	      for ( i = 1; i <= tServices.getNumRows(); i++)
	      {
	         sServiceName = tServices.getString(1, i);
	         arg_table.setString( 1, 1, sServiceName); 
	         target_table = Table.tableNew("Incremental Update History - All Since start yest");
	         retval = generateHistoryForService(arg_table, target_table);
	
	         if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	            break;
	
	         if ( i == 1 )
	            outputTable = target_table.cloneTable();
	
	         target_table.copyRowAddAll(outputTable);
	         target_table.destroy();
	      }
	   }
	
	   if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	      OConsole.oprint("!!! Failed to generate incremental history stats !!!!\n");
	   else
	      OConsole.oprint("!!! Succeeded generating incremental history stats !!!!\n");
	
	   if ( outputTable.getNumRows() > 0 )
	   {
		   // enrich with deal number
		   tSecondaryEntityNums = Table.tableNew();
		   tSecondaryEntityNums.select(outputTable, "DISTINCT, secondary_entity_num", "secondary_entity_num GT 0" );
		
		   iSupQueryId = Query.tableQueryInsert(tSecondaryEntityNums, 1);
		   tSupplementary = Table.tableNew();
		   retval = DBaseTable.loadFromDbWithSQL(tSupplementary, 
		                                       "distinct tran_num, deal_tracking_num", 
		                                       "ab_tran, query_result qr", 
		                                       "ab_tran.tran_num = qr.query_result and qr.unique_id = " + iSupQueryId);
		   Query.clear(iSupQueryId);
		   tSecondaryEntityNums.destroy();
		
		   outputTable.select(tSupplementary, "deal_tracking_num", "tran_num EQ $secondary_entity_num"); 
		   tSupplementary.destroy();
		
		   outputTable.sortCol(1);
		   outputTable.viewTable();
	   }
	   else
		   OConsole.oprint("No incremental updates since yesterday !\n");

	   outputTable.destroy();
	   arg_table.destroy();

	}
	

	int generateHistoryForService(Table arg_table, Table target_table)  throws OException
	{
	   int retval;
	
	   retval = DBase.runProc("USER_apm_get_updates_by_date", arg_table);
	   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBASE_RunSql() failed" ) );
	   else
	   {
	      retval = DBase.createTableOfQueryResults(target_table);
	      if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBASE_CreateTableOfQueryResults() failed" ) );
	
	      target_table.sortCol(1);
	
	   }
	
	   return retval;
	}
	
	int getListOfActiveAPMServices(Table tServices) throws OException
	{
	   int retval;
	   Table args;
	   Table results = Util.NULL_TABLE;
	
	   args = Table.tableNew("args");
	   args.addRow();
	
	   retval = DBase.runProc("USER_apm_get_batch_stats", args);
	   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBASE_RunSql() failed" ) );
	   else
	   {
	      results = Table.tableNew();
	      retval = DBase.createTableOfQueryResults(results);
	      if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBASE_CreateTableOfQueryResults() failed" ) );
	   }
	
	   if ( retval == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	   {
	      tServices.select(results, "DISTINCT, service_name", "msg_type EQ -1");
	   }
	   
	   args.destroy();
	   results.destroy();
	
	   return retval;
	}

}
