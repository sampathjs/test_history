/* Released with version 05-Feb-2020_V17_0_126 of APM */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// PURGES THE INCREMENTAL QUEUE FOR AN APM SERVICE
// USEFUL WHEN LOTS OF STALE ITEMS IN THE QUEUE
// CONFIGURE A WORKFLOW TO RUN THIS SCRIPT OR RUN VIA A TASK
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package standard.apm.utilities;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.Util;

public class APM_PurgeIncrementalQueue implements IScript 
{	
	String SERVICE_NAME = "APM";

	public void execute(IContainerContext arg0) throws OException 
	{
	   int ret;
	   Table args;

	   OConsole.oprint("!!! Starting to purge incremental queue for APM service: " + SERVICE_NAME + " !!!!\n");

	   args = Table.tableNew("args");
	   args.addCol("defn_name", COL_TYPE_ENUM.COL_STRING);
	   args.addCol("start_date", COL_TYPE_ENUM.COL_DATE_TIME);
	   args.addCol("end_date", COL_TYPE_ENUM.COL_DATE_TIME);
	   args.addRow();

	   args.setString(1, 1, SERVICE_NAME);
	   // set to start of time
	   args.setDateTime(2, 1, ODateTime.strToDateTime("01/01/01 1:01:01pm"));
	   // take off 30 secs - don't blow away stuff you might not want to lose
	   args.setDateTimeByParts(3, 1, OCalendar.getServerDate(), Util.timeGetServerTime() - 30);

	   ret = DBase.runProc("USER_apm_prune_entity_queue",args);
	   if (ret != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) 
	      OConsole.oprint("Purge Incremental proc failed\n");
	   else
	      OConsole.oprint("Purge Incremental proc ran ok\n");

	   args.destroy();

	   OConsole.oprint("FINISHED purging queue for APM service: " + SERVICE_NAME + "\n");		
	}
	
}
