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
// All code that used to exist in this file has been moved to ADS_CommitBatchDataSet.

public class APM_ADSCommitBatchDataSet implements IScript {
	private ADS_CommitBatchDataSet m_commitBatchDataSet = null;

	public APM_ADSCommitBatchDataSet() {
		m_commitBatchDataSet = new ADS_CommitBatchDataSet();
	}

	public void execute(IContainerContext context) throws OException {

		Table argt = context.getArgumentsTable();
		
		int retVal = m_commitBatchDataSet.execute(argt);

		if (retVal != 1)
			Util.exitFail();
		else
			Util.exitSucceed();
	}
}
