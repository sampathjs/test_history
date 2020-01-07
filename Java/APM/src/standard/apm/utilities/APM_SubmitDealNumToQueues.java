/* Released with version 29-Aug-2019_V17_0_124 of APM */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// FORCES A DEAL THROUGH THE OPS SERVICE DEFINITIONS AGAIN VIA THE REEVALUATE API
// USEFUL WHEN WANTING TO MANUALLY FORCE A DEAL INTO APM
// NOTE THE APM SERVICES HAVE TO BE CONFIGURED TO HAVE
// "ReEvaluate Trades" set in the "Tran Status Internal" criteria
// SET THE DEAL NUM TO RUN AGAINST
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package standard.apm.utilities;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class APM_SubmitDealNumToQueues implements IScript 
{
	int DEAL_NUM_TO_LOAD = 1234;
	
	public void execute(IContainerContext arg0) throws OException 
	{
	   OConsole.oprint("START RUNNING APM_SubmitDealNumToQueues\n");
	   OConsole.oprint("About to touch trade using ReEvaluate API\n");
	   OConsole.oprint("NOTE - make sure ReEvaluate Trades is a Tran Status Internal selection on the APM services for this to be detected\n");
	   OConsole.oprint("Using DealNum:" + DEAL_NUM_TO_LOAD + "\n");		

	   Table dealNumTable = Table.tableNew();
	   dealNumTable.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
	   dealNumTable.addRow();
	   dealNumTable.setInt(1, 1, DEAL_NUM_TO_LOAD);
	   int queryId = Query.tableQueryInsert(dealNumTable, 1);
	   dealNumTable.destroy();

	   TranOpService.reEvaluateTrades(queryId);

	   Query.clear(queryId);

	   OConsole.oprint("Touched Deal: " + DEAL_NUM_TO_LOAD + " using ReEvaluate API to reprocess it.\n");		
	       
	   OConsole.oprint("FINISHED RUNNING APM_SubmitDealNumToQueues\n");		
	}
	
}
