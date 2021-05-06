package com.openlink.matthey.simresults;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2021-04-09	V1.0	prashanth	- EPI-1705 initial version - To Run Var Standard simlation in EOD
 */

public class VarStandardBatchSimulation implements IScript {

	public static final String CONST_REPO_CONTEXT = "";
	public static final String CONST_REPO_SUBCONTEXT = "";

	public static final String SIMULATION_DEFINITION = "VaR Standard";
	public static final String SAVED_QUERY = "VaR Trades";

	/**
	 * OLF entry point to execute custom processing <br>
	 */
	@Override
	public void execute(IContainerContext context) throws OException {

		init();
		Table simDef = Util.NULL_TABLE;
		Table simArgt = Util.NULL_TABLE;
		Table reval = Util.NULL_TABLE;
		Table simRes = Util.NULL_TABLE;
		try {

			Logging.info("Preparing to run simulation... Simulation Definition: %s, deal set from saved query : %s",
					SIMULATION_DEFINITION, SAVED_QUERY);

			int queryId = Query.run(SAVED_QUERY);
			int runDate = OCalendar.getLgbd(OCalendar.today());

			if (queryId > 0) {
				// Create simulation definition
				simDef = Sim.loadSimulation(SIMULATION_DEFINITION);

				if (Table.isTableValid(simDef) == 0) {
					Logging.error("Failed to load simulation definition %s", SIMULATION_DEFINITION);
				}

				// Create revaluation definition
				simArgt = Table.tableNew("SimArgumentTable");
				Sim.createRevalTable(simArgt);
				simArgt.setInt("QueryId", 1, queryId);
				simArgt.setTable("SimulationDef", 1, simDef);
				simArgt.setInt("UseMarketPrices", 1, 1);

				// Package into a table as expected by Sim.runRevalByParamFixed
				reval = Table.tableNew("RevalTable");
				reval.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
				reval.addRow();
				reval.setTable("RevalParam", 1, simArgt);

				// Load closing proces
				Sim.loadAllCloseMktd(runDate);
				// Run the simulation
				Logging.info("Running simulation for current date.");
				simRes = Sim.runRevalByParamFixed(reval);
				if (Table.isTableValid(simRes) == 0) {
					throw new Exception("Failed to run reval");
				}
				
				Logging.info("Saving Sim Results .");
				int retVal = Sim.insertSimResults(simRes, 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt(),
						runDate, SIMULATION_DEFINITION);
				if (retVal <= 0) {
					throw new Exception("Failed to save simulation restults");
				}
				
			}
			Logging.info("Simulation saved succesfully.");
		} catch (Exception e) {
			throw new OException("Failed to run and save sim results: " + e.getMessage());

		} finally {
			destroyTable(simDef);
			destroyTable(simArgt);
			destroyTable(reval);
			destroyTable(simRes);
			
			Logging.close();
		}
	}

	private void destroyTable(Table table) throws OException {
		if (table != null)
			table.destroy();
	}

	private void init() {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info("********************* Start of new run ***************************");
	}

}
