/* Released with version 09-Jan-2014_V14_0_6 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;

// Script which will purge the apm_deal_locking table of all deals.
public class APM_Purge_Deal_Locking_Table implements IScript 
{	
	/// Utility functions.
	private APM_Utils m_utils;
	
	/// Minimal argt which can be used for 
	private Table m_fakeArgt;
	
	/// Constructor.
	public APM_Purge_Deal_Locking_Table() throws OException
	{
		m_utils = new APM_Utils();		
		
		m_fakeArgt = Table.tableNew("argt");
		m_fakeArgt.addCol("Message Context", COL_TYPE_ENUM.COL_TABLE);
		m_fakeArgt.addCol("Log File", COL_TYPE_ENUM.COL_STRING);
		m_fakeArgt.addRow();
		
		m_fakeArgt.setTable("Message Context", 1, Table.tableNew("context"));
		m_fakeArgt.setString("Log File", 1, "test.txt");			
	}
	
	/// Execute the script.  
	public void execute(IContainerContext context) throws OException 
	{
		try 
		{
			int iRetVal = 1;
			int iAttempt;
			
			OConsole.oprint("Running APM_PurgeDealLockingTable\n");
			
			for (iAttempt = 0; (iAttempt == 0) || ((iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt()) && (iAttempt < 10)); ++iAttempt) {
				if (iAttempt > 0)
					Debug.sleep(iAttempt * 1000);

				iRetVal = DBase.runProc("USER_purge_apm_deal_locking", Table.tableNew());
			}
			
			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "SQL statement 'delete from apm_deal_locking where 1=1' failed \n"));

			OConsole.oprint("Completed APM_PurgeDealLockingTable\n");						
		} 
		catch (OException e) 
		{
			e.printStackTrace();
		}		
	}		
}
