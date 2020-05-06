/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

// Script which will purge the apm_entity_locking table of all entities.
public class APM_Purge_Entity_Locking_Table implements IScript 
{	
	/// Utility functions.
	private APM_Utils m_utils;
	
	/// Minimal argt which can be used for 
	private Table m_fakeArgt;
	
	/// Constructor.
	public APM_Purge_Entity_Locking_Table() throws OException
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
		try {
			OConsole.oprint("Running APM_PurgeEntityLockingTable\n");

			int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

			int numberOfRetriesThusFar = 0;
			do {
				try {
					iRetVal = DBase.runProc("USER_purge_apm_entity_locking", Table.tableNew());
				} catch (OException exception) {
					iRetVal = exception.getOlfReturnCode().toInt();

					if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
						// to be caught by the outer try-catch, which prints out stack trace
						throw exception;
					}
				} finally {
					if (iRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
						numberOfRetriesThusFar++;

						Debug.sleep(numberOfRetriesThusFar * 1000);
					} else {
						// not a retryable error, leave
						break;
					}
				}
			} while (numberOfRetriesThusFar < APM_Utils.MAX_NUMBER_OF_DB_RETRIES);

			if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "SQL statement 'delete from apm_entity_locking where 1=1' failed \n"));

			OConsole.oprint("Completed APM_PurgeEntityLockingTable\n");
		} catch (OException e) {
			e.printStackTrace();
		}
	}	
}
