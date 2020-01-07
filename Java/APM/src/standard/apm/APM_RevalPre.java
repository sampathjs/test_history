/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class APM_RevalPre implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
		Table argt = context.getArgumentsTable();

		OConsole.oprint("\n=============== Init Script Started ============\n");
		OConsole.oprint("\n=============== Init Script Finished ============\n");

		Util.exitSucceed();
    }
}
