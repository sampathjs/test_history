/* Released with version 05-Feb-2020_V17_0_126 of APM */

/*
 Description : This forms part of the Trader Front End, Active Position Manager
 package

 -------------------------------------------------------------------------------
 Revision No.  Date        Who  Description
 -------------------------------------------------------------------------------
 1.0.0         
 */

package standard.apm;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

// This file is now just a script wrapper.
// All code that used to exist in this file has been moved to ADS_LogStatusMessage.

public class APM_ADSLogStatusMessage implements IScript {
	private ADS_LogStatusMessage m_logStatusMessage = null;

	public APM_ADSLogStatusMessage() {
		m_logStatusMessage = new ADS_LogStatusMessage();
	}

	public void execute(IContainerContext context) throws OException {

		Table argt = context.getArgumentsTable();

		int retVal = m_logStatusMessage.execute( argt );
		
		if (retVal != 1)
			Util.exitFail();
		else
			Util.exitSucceed();
	}
}
