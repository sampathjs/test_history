/* Released with version 05-Feb-2020_V17_0_126 of APM */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// DELETS A TRAN NUM WAITING IN THE QUEUE TO BE PROCESSED
// FOR A PARTICULAR SERVICE
// CONFIGURE A WORKFLOW TO RUN THIS SCRIPT
// SET THE TRAN NUM AND THE SERVICE NAME TO RUN AGAINST 
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package standard.apm.utilities;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;

public class APM_DelTrannumFromQueue implements IScript 
{
	int TRAN_NUM_TO_DELETE = 1234;
	String SERVICE_NAME = "APM";
	
	public void execute(IContainerContext arg0) throws OException 
	{
	   OConsole.oprint("START RUNNING APM_DelTrannumFromQueue\n");
	   OConsole.oprint("Using TranNum:" + TRAN_NUM_TO_DELETE + "\n");		
	   OConsole.oprint("Using Service Name:" + SERVICE_NAME + "\n");		

	   Table paramTable = Table.tableNew();
	   paramTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
	   paramTable.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
	   paramTable.addRow();
	   paramTable.setString(1, 1, SERVICE_NAME);
	   paramTable.setInt(2, 1, TRAN_NUM_TO_DELETE);

	   int retval = DBase.runProc("USER_tfe_del_trannum_from_q", paramTable);
	   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
	      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBASE_RunSql() failed" ) );
	   else
	      OConsole.oprint("Removed Trannum: " + TRAN_NUM_TO_DELETE + " from service: " + SERVICE_NAME + "\n");		

	   paramTable.destroy();

	   OConsole.oprint("FINISHED RUNNING APM_DelTrannumFromQueue\n");		
	}	
}
