/* Released with version 29-Oct-2015_V14_2_4 of APM */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// GENERATES LATEST BATCH STATS FOR ALL APM SERVICES
// USEFUL WHEN MULTIPLE APM SERVICES ARE CONFIGURED
// CONFIGURE A WORKFLOW TO RUN THIS SCRIPT OR RUN VIA A TASK
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package standard.apm.utilities;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.DB_RETURN_CODE;

public class APM_View_Batch_Stats implements IScript 
{	
	public void execute(IContainerContext arg0) throws OException 
	{
	   int ret;
	   int iRetVal;
	   Table args;
	   Table results;

	   OConsole.oprint("!!! Starting to generate Latest Batch stats for all APM services !!!!\n");

	   args = Table.tableNew("args");
	   args.addRow();

	   ret = DBase.runProc("USER_apm_get_batch_stats",args);
	   if (ret != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) 
	      OConsole.oprint("Get Batch Stats proc failed\n");
	   else
	      OConsole.oprint("Get Batch Stats proc ran ok\n");

	   results = Table.tableNew("USER_apm_get_batch_stats results"); 
	   ret = DBase.createTableOfQueryResults(results);

	   if( ret != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	      OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(ret, "DBASE_CreateTableOfQueryResults() failed" ) );

	   results.viewTable();

	   results.destroy();
	   args.destroy();

	   OConsole.oprint("FINISHED generating latest batch stats for all APM services\n");		
	}
	
}
