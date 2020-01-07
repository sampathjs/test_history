/* Released with version 29-Oct-2015_V14_2_4 of APM */

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
