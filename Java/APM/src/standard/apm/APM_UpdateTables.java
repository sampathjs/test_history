/* Released with version 29-Aug-2019_V17_0_124 of APM */

/*
 Description : This forms part of the Trader Front End, Active Position Manager
 package

 This script perfoms the following :
 - Checks the data that is about to be BCPed into the database to
 make sure the underlying database table exists and that it matches
 the schema
 - The APM Reporting tables are not populated for backouts (for speed)
 This script adds the secondary entity nums to the first table for backouts
 - Calls APM functions to BCP the data in
 - Clears the APM reporting data in preparation for the next entity group

 - Block updates now cope with empty or partially empty data tables

 -------------------------------------------------------------------------------
 Revision No.  Date        Who  Description
 -------------------------------------------------------------------------------
 */

package standard.apm;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

// This file is now just a script wrapper.
// All code that used to exist in this file has been moved to APM_UpdateTables_Impl.

public class APM_UpdateTables implements IScript {
	private APM_UpdateTables_Impl m_updateTables = null;

	public APM_UpdateTables() {
		m_updateTables = new APM_UpdateTables_Impl();
	}

	public void execute(IContainerContext context) throws OException {

		Table argt = context.getArgumentsTable();
		
		int iRetVal = m_updateTables.execute(argt);

		if (iRetVal == 0)
			Util.exitFail();
		else
			Util.exitSucceed();
	}	
}
