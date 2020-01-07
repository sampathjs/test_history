/* Released with version 29-Aug-2019_V17_0_124 of APM */

/*
 Description : This forms part of the Active Position Manager package

 Will be run automatically as postprocess script on revalservice cluster node if revalservice configured.  
 Otherwise has to be manually called. 

 1)	Calls HandlesResults class
 2) 	Calls UpdateTables script
 3)	Updates return statuses
 4)	Makes sure the simulation result data is destroyed (otherwise results will be aggregated and sent back to caller ? not good !!)
*/

package standard.apm;

import com.olf.openjvs.*;

// This file is now just a script wrapper.
// All code that used to exist in this file has been moved to APM_RevalPost_Impl.

public class APM_RevalPost implements IScript {
	private APM_RevalPost_Impl m_revalPost = null;
	
	public APM_RevalPost(){
		m_revalPost = new APM_RevalPost_Impl();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		int iRetVal = m_revalPost.execute(argt, returnt);
				
		if ( iRetVal == 0 )
			Util.exitFail();
		else
			Util.exitSucceed();		
	}
}
