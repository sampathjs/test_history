/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;

public class RTPE_APM_Generic_PreProcessor implements IScript 
{
	//
	public RTPE_APM_Generic_PreProcessor() 
	{
	}
	
	//
	public void execute(IContainerContext context) throws OException 
	{
		OConsole.oprint( "------- IGNORING PRE PROCESS SCRIPT - NOT LONGER USED --------\n");			
		Util.exitSucceed();
	}

}
