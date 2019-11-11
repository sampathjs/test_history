/* Released with version 29-Aug-2019_V17_0_124 of APM */

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// RUNS A SAVED REVAL AGAINST A SPECIFIED TRAN NUM
// USEFUL WHEN COMPARING RESULTS WHEN RUN ON DIFFERENT APPSERVERS
// CONFIGURE A WORKFLOW TO RUN THIS SCRIPT
// AND SET SIM_NAME_TO_LOAD TO A SAVED SIM DEF (PREFERABLY ONE SAVED USING THE SAVE SIM DEF OPTION IN APM)
// AND THE TRAN NUM TO RUN AGAINST
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package standard.apm.utilities;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;

public class APM_RevalTest implements IScript 
{
	String SIM_NAME_TO_LOAD = "APM";
	int TRAN_NUM_TO_LOAD = 1234;
	
	public void execute(IContainerContext arg0) throws OException 
	{
	   Table simDef, tranNumTable, tResults;
	   Table tRevalParam = Table.tableNew("Reval Param");
	   int queryId;

	   OConsole.oprint("START RUNNING APM RevalTest\n");

	   OConsole.oprint("Loading simulation definition: " + SIM_NAME_TO_LOAD + "\n");		
	   OConsole.oprint("Using TranNum:" + TRAN_NUM_TO_LOAD + "\n");		

	   simDef = Sim.loadSimulation(SIM_NAME_TO_LOAD);

	   tranNumTable = Table.tableNew();
	   tranNumTable.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
	   tranNumTable.addRow();
	   tranNumTable.setInt(1, 1, TRAN_NUM_TO_LOAD);
	   queryId = Query.tableQueryInsert(tranNumTable, 1);

	   Sim.createRevalTable (tRevalParam);
	   tRevalParam.setInt("ClearCache", 1, 0);
	   tRevalParam.setInt("RefreshMktd", 1, 0);
	   tRevalParam.setInt("SimRunId", 1, -1);
	   tRevalParam.setInt("SimDefId", 1, -1);
	   tRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.GEN_SIM_TYPE.toInt());
	   tRevalParam.setTable("SimulationDef", 1, simDef);
	   tRevalParam.setInt("QueryId", 1, queryId);

	   tResults = Sim.runRevalByParamFixed (tRevalParam);

	   OConsole.oprint("Saving sim output to AFS: APMRevalTest_TranNum_" + TRAN_NUM_TO_LOAD + " (Use AFS Viewer to view)\n");		
	       
	   Afs.saveTable(tResults, "APMRevalTest_TranNum_" + TRAN_NUM_TO_LOAD);

	   OConsole.oprint("FINISHED RUNNING APM RevalTest\n");		
	}
	
}
