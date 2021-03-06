package com.matthey.openlink.pnl;

/**
 * 
 * Description:
 * This script is used to run simulation with input as Query in string format and result to be run
 * Revision History:
 * 28.05.20  GuptaN02  initial version
 *  
 */

import com.matthey.utilities.ExceptionUtil;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class RunSimulation {
	
	
	/**
	 * @param finalSqlQuery
	 * @param simName
	 * @return
	 * @throws OException
	 * Return Gen Result
	 */
	protected static Table runSimulation(int queryId, String simName) throws OException
	{
		Table tblSim=Util.NULL_TABLE;
		Table tblResultList=Util.NULL_TABLE;
		Table simResults= Util.NULL_TABLE;
		Table genResults=Util.NULL_TABLE;
		try{
			Logging.info("Preparing for simulation..Deal Set is taken from queryId: "+queryId+" , simulation to be run is: "+simName);
			tblSim = Sim.createSimDefTable();
			Sim.addSimulation(tblSim, "Sim");
			Sim.addScenario(tblSim, "Sim", "Base", Ref.getLocalCurrency());

			/* Build the result list */
			tblResultList = Sim.createResultListForSim();
			SimResult.addResultForSim(tblResultList, SimResultType.create(simName));
			Sim.addResultListToScenario(tblSim, "Sim", "Base", tblResultList);

			// Create reval table
			Table revalParam = Table.tableNew("SimArgumentTable");
			Sim.createRevalTable(revalParam);
			revalParam.setInt("QueryId", 1, queryId);
			revalParam.setTable("SimulationDef", 1, tblSim);		

			// Package into a table as expected by Sim.runRevalByParamFixed
			Table revalTable = Table.tableNew("RevalTable");
			revalTable.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
			revalTable.addRow();
			revalTable.setTable("RevalParam", 1, revalParam);

			Logging.info("Running simulation..");
			// Run the simulation
			simResults = Sim.runRevalByParamFixed(revalTable);    
			Logging.info("Simulation completed");


			if (Table.isTableValid(simResults) != 1)
			{   
				Logging.error("Could not find sim result.");
				throw new OException("Could not find sim results");
			}
			Logging.info("Processing ad-hoc simulation results...\n");

			genResults = SimResult.getGenResults(simResults, 1).copyTable();

			if (Table.isTableValid(genResults) != 1)
			{
				Logging.error("Could not find gen result.");
				throw new OException("Could not find gen results");
			}
			return genResults;
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while processin simulation for deal set");
			throw new OException("Error took place while processin simulation for deal set "+e.getMessage());
			
		}
		finally{
			if(tblSim!=null)
				tblSim.destroy();
			if (tblResultList!=null)
				tblResultList.destroy();
			if (simResults!=null)
				simResults.destroy();
		}

	}

}
