package com.jm.util.adhocsim;

import org.apache.commons.lang3.StringUtils;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.control.EnumLaunchableType;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.market.ClosingDatasetType;
import com.olf.openrisk.simulation.RevalType;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

@ScriptCategory({ EnumScriptCategory.Generic })
public class JMAdhocSimulationMain extends AbstractGenericScript {

	public Table execute(Context context, EnumScriptCategory category, ConstTable table) {

		String simDefName = table.getString("bsim_def", 0);
		String[] savedQueriesArray = table.getTable("queries", 0).getColumnValuesAsString("query_name");

		Table batchSim = retieveAdhocSimPortfolioList(context, simDefName, savedQueriesArray);

		Simulation sim = null;
		SimResults simResult = null;

		try {
			
			Logging.init(context, this.getClass(), "", "");
			
			for (TableRow row : batchSim.getRows()) {

				try {
					
					Logging.info("Running simulation for :: " + row.getString("query_name"));
					
					RevalType revalType = context.getSimulationFactory().getRevalType(row.getString("run_type_name"));
					Query query = context.getTradingFactory().getQuery(row.getString("query_name"));
					ClosingDatasetType closingDataType = context.getMarketFactory().getClosingDatasetType("Closing");

					sim = context.getSimulationFactory().retrieveSimulation(row.getString("sim_name"));
					sim.setPriorRevalType(revalType);
					sim.setRevalType(revalType);
					sim.setClosingDatasetType(closingDataType);
					simResult = sim.run(query);
					
					Logging.info("Saving simulation for :: " + row.getString("query_name"));
					simResult.save();

				} catch (Exception e) {
					Logging.error("Exception in running the simulation for saved query :: " + row.getString("query_name") + e.getMessage());
				}
			}
		} catch (Exception e) {
			Logging.error("Exception :: " + e.getMessage());
		}

		context.getControlFactory().retrieveLaunchable(EnumLaunchableType.RawSimResultsViewer).launch();

		return null;
	}

	private Table retieveAdhocSimPortfolioList(Context context, String simDefName, String[] savedQueriesArray) {

		String savedQueries = "'" + StringUtils.join(savedQueriesArray, "','") + "'";

		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT  bsd.bsim_id, ");
		sql.append(" 		 bs.title, ");
		sql.append(" 		 bsd.query_db_id, ");
		sql.append("         qv.query_name, ");
		sql.append("         bsd.sim_id, ");
		sql.append(" 		 sd.name sim_name, ");
		sql.append(" 		 bsd.run_type, ");
		sql.append("         rt.name run_type_name ");
		sql.append(" FROM batch_sim_defn bsd ");
		sql.append(" JOIN batch_sim bs ON bsd.bsim_id = bs.bsim_id ");
		sql.append(" JOIN query_view qv ON bsd.query_db_id = qv.query_id ");
		sql.append(" JOIN sim_def sd ON sd.sim_def_id = bsd.sim_id ");
		sql.append(" JOIN reval_type rt ON rt.id_number = bsd.run_type ");
		sql.append(" WHERE bs.title LIKE '").append(simDefName).append("'");
		sql.append(" AND qv.query_name IN (").append(savedQueries).append(")");

		Table batchSimDef = context.getIOFactory().runSQL(sql.toString());

		return batchSimDef;
	}
}
