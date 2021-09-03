package com.openlink.matthey.reportbuilder;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.PFOLIO_RESULT_TYPE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2021-04-09	V1.0	prashanth	- initial version - To Run Var Standard simlation in EOD
 */

public class VarStandardDS implements IScript {

	public static final String CONST_REPO_CONTEXT = "Var Standard";
	public static final String CONST_REPO_SUBCONTEXT = "Var Standard RB";
	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRepo;
	
	private static final String CONST_REPO_VAR_SIM_DEF = "SimulationDefinition";
	
	private static final String CONST_REPO_VAR_SAVED_QUERY = "SavedQueryName";
	

	public static String SIMULATION_DEFINITION = "";
	
	public static String SAVED_QUERY = "";

	/**
	 * OLF entry point to execute custom processing <br>
	 */
	@Override
	public void execute(IContainerContext context) throws OException {

		init();
		Logging.info("Starting script %s", this.getClass().getSimpleName());
		Table returnt = context.getReturnTable();
		try {
			Logging.info("Fetching sim results for simulation definition %s", SIMULATION_DEFINITION);

			if (context.getArgumentsTable().getInt("ModeFlag", 1) == 0) {
				getOutTableStructure(returnt);
			} else {
				getOutputFromSimResults(context, returnt);
			}

		} catch (Exception e) {
			throw new OException("Failed to load saved sim results: " + e.getMessage());

		} finally {
			Logging.close();
		}
	}

	private void getOutputFromSimResults(IContainerContext context, Table returnt) throws OException {

		Table simResults = Util.NULL_TABLE;
		Table genResults = Util.NULL_TABLE;
		Table genResultsSceMC = Util.NULL_TABLE;
		Table parametricVaR = Util.NULL_TABLE;
		Table historicMCVaR = Util.NULL_TABLE;
		Table monteCarloVaR = Util.NULL_TABLE;

		try {
			Table pluginParam = context.getArgumentsTable().getTable("PluginParameters", 1);
			int row = pluginParam.findString("parameter_name", "PARAM_DATE", SEARCH_ENUM.FIRST_IN_GROUP);
			int date = OCalendar.getNgbd(OCalendar.parseString(pluginParam.getString("parameter_value", row)));
			date = date == -1 ? OCalendar.today() : date;
			simResults = loadSimResults(date);

			genResults = SimResult.getGenResults(simResults, 1);
			genResultsSceMC = SimResult.getGenResults(simResults, 2);

			row = genResults.unsortedFindInt("result_type", PFOLIO_RESULT_TYPE.VALUE_AT_RISK_RESULT.toInt());
			parametricVaR = genResults.getTable("result", row).copyTable();
			row = genResults.unsortedFindInt("result_type", PFOLIO_RESULT_TYPE.MCARLO_VAR_STATISTICS_RESULT.toInt());
			historicMCVaR = genResults.getTable("result", row).copyTable();
			row = genResultsSceMC.unsortedFindInt("result_type",
					PFOLIO_RESULT_TYPE.MCARLO_VAR_STATISTICS_RESULT.toInt());
			monteCarloVaR = genResultsSceMC.getTable("result", row).copyTable();

			returnt.select(parametricVaR, "id, label", "id GT -1");
			reorderid(context, returnt);
			parametricVaR.select(returnt, "id", "label EQ $label");
			historicMCVaR.setColName("Summary Item", "summary_item");
			historicMCVaR.select(returnt, "id", "label EQ $summary_item");
			monteCarloVaR.setColName("Summary Item", "summary_item");
			monteCarloVaR.select(returnt, "id", "label EQ $summary_item");

			returnt.sortCol("id");
			parametricVaR.sortCol("id");
			historicMCVaR.sortCol("id");
			monteCarloVaR.sortCol("id");

			returnt.addCol("result", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("summary_group", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("component_var", COL_TYPE_ENUM.COL_DOUBLE);
			parametricVaR.copyCol("result", returnt, "result");
			parametricVaR.copyCol("Summary Group", returnt, "summary_group");
			parametricVaR.copyCol("Component VaR", returnt, "component_var");

			returnt.addCol("historic_min_mtm_var_loss", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_max_mtm_var_profit", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_std_dev_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_mean_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_mode_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_median_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_var_kurtosis", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_var_skew", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_var_at_90_conf", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_var_at_95_conf", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_var_at_98_conf", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("historic_var_at_99_conf", COL_TYPE_ENUM.COL_DOUBLE);

			historicMCVaR.copyCol("Min MTM VaR (Loss)", returnt, "historic_min_mtm_var_loss");
			historicMCVaR.copyCol("Max MTM VaR (Profit)", returnt, "historic_max_mtm_var_profit");
			historicMCVaR.copyCol("Std Dev VaR", returnt, "historic_std_dev_var");
			historicMCVaR.copyCol("Mean VaR", returnt, "historic_mean_var");
			historicMCVaR.copyCol("Mode VaR", returnt, "historic_mode_var");
			historicMCVaR.copyCol("Median VaR", returnt, "historic_median_var");
			historicMCVaR.copyCol("VaR Kurtosis", returnt, "historic_var_kurtosis");
			historicMCVaR.copyCol("VaR Skew", returnt, "historic_var_skew");
			historicMCVaR.copyCol("VaR at 90% conf", returnt, "historic_var_at_90_conf");
			historicMCVaR.copyCol("VaR at 95% conf", returnt, "historic_var_at_95_conf");
			historicMCVaR.copyCol("VaR at 98% conf", returnt, "historic_var_at_98_conf");
			historicMCVaR.copyCol("VaR at 99% conf", returnt, "historic_var_at_99_conf");

			// returnt.setRowType(row_num, ROW_TYPE_ENUM.ROW_DATA)
			returnt.addCol("monte_carlo_min_mtm_var_loss", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_max_mtm_var_profit", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_std_dev_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_mean_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_mode_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_median_var", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_var_kurtosis", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_var_skew", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_var_at_90_conf", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_var_at_95_conf", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_var_at_98_conf", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("monte_carlo_var_at_99_conf", COL_TYPE_ENUM.COL_DOUBLE);

			monteCarloVaR.copyCol("Min MTM VaR (Loss)", returnt, "monte_carlo_min_mtm_var_loss");
			monteCarloVaR.copyCol("Max MTM VaR (Profit)", returnt, "monte_carlo_max_mtm_var_profit");
			monteCarloVaR.copyCol("Std Dev VaR", returnt, "monte_carlo_std_dev_var");
			monteCarloVaR.copyCol("Mean VaR", returnt, "monte_carlo_mean_var");
			monteCarloVaR.copyCol("Mode VaR", returnt, "monte_carlo_mode_var");
			monteCarloVaR.copyCol("Median VaR", returnt, "monte_carlo_median_var");
			monteCarloVaR.copyCol("VaR Kurtosis", returnt, "monte_carlo_var_kurtosis");
			monteCarloVaR.copyCol("VaR Skew", returnt, "monte_carlo_var_skew");
			monteCarloVaR.copyCol("VaR at 90% conf", returnt, "monte_carlo_var_at_90_conf");
			monteCarloVaR.copyCol("VaR at 95% conf", returnt, "monte_carlo_var_at_95_conf");
			monteCarloVaR.copyCol("VaR at 98% conf", returnt, "monte_carlo_var_at_98_conf");
			monteCarloVaR.copyCol("VaR at 99% conf", returnt, "monte_carlo_var_at_99_conf");

			returnt.addCol("run_date", COL_TYPE_ENUM.COL_STRING);
			returnt.setColValString(returnt.getColNum("run_date"), OCalendar.formatDateInt(date));

			Logging.info("Sim results fetched successfully");
		} catch (Exception e) {
			throw new OException("Failed to fectch VaR data from sim results: " + e.getMessage());

		} finally {
			destroyTable(parametricVaR);
			destroyTable(historicMCVaR);
			destroyTable(monteCarloVaR);
		}
	}

	private Table loadSimResults(int date) throws OException {

		Table simResults = null;
		try {
			// Portfolio id for sim results fo run type intraday are not consistent across the environments 
			// hence trying different options  
			simResults = SimResult.tableLoadSrun(-1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE, date);
			if(simResults == null ) {
				simResults = SimResult.tableLoadSrun(0, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE, date);
			}
			if(simResults == null ) {
				simResults = SimResult.tableLoadSrun(1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE, date);
			}
			if(simResults == null ) {
				throw new OException("");	
			}
			
		} catch (OException e) {
			throw new OException("Falied to Load saved sim results");	
		}
		
		return simResults;
	}

	private void reorderid(IContainerContext context, Table returnt) throws OException {

		Table partyPortfolio = Util.NULL_TABLE;
		Table distinctParty = Util.NULL_TABLE;
		Table returntCopy = Util.NULL_TABLE;

		try {
			String sql = "SELECT p.short_name party_id,  po.name portfolio_id" + "\nFROM party_portfolio pp"
					+ "\nJOIN party p ON p.party_id = pp.party_id"
					+ "\nJOIN portfolio po ON po.id_number = pp.portfolio_id ";
			partyPortfolio = Table.tableNew();
			int ret = DBaseTable.execISql(partyPortfolio, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed to get party portfolio"));
				throw new OException("Failed to get party portfolio");
			}
			returntCopy = returnt.copyTable();
			returnt.clearRows();
			returntCopy.select(partyPortfolio, "party_id", "portfolio_id EQ $label");

			distinctParty = Table.tableNew();
			distinctParty.addCol("party_id", COL_TYPE_ENUM.COL_STRING);
			returntCopy.copyColDistinct("party_id", distinctParty, "party_id");

			// Copy Global Overall row as id = 0
			returnt.select(returntCopy, "id, label", "id EQ 0");
			int id = 1;
			for (int partyRow = 1; partyRow <= distinctParty.getNumRows(); partyRow++) {

				String partyId = distinctParty.getString("party_id", partyRow);
				if (partyId == null) {
					continue;
				}
				int row = returnt.addRow();
				returnt.setInt("id", row, id);
				returnt.setString("label", row, partyId);
				id++;
				for (int dataRow = 1; dataRow <= returntCopy.getNumRows(); dataRow++) {
					String dataRowPartyId = returntCopy.getString("party_id", dataRow);
					if (dataRowPartyId != null && dataRowPartyId.equalsIgnoreCase(partyId)) {
						row = returnt.addRow();
						returnt.setInt("id", row, id);
						returnt.setString("label", row, returntCopy.getString("label", dataRow));
						id++;
					}
				}
			}

		} catch (Exception e) {
			throw new OException(e.getMessage());
		} finally {
			destroyTable(partyPortfolio);
			destroyTable(distinctParty);
			destroyTable(returntCopy);
		}

	}

	private void getOutTableStructure(Table returnt) throws OException {
		returnt.addCol("id", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("label", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("result", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("summary_group", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("component_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_min_mtm_var_loss", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_max_mtm_var_profit", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_std_dev_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_mean_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_mode_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_median_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_var_kurtosis", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_var_skew", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_var_at_90_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_var_at_95_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_var_at_98_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("historic_var_at_99_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_min_mtm_var_loss", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_max_mtm_var_profit", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_std_dev_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_mean_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_mode_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_median_var", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_var_kurtosis", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_var_skew", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_var_at_90_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_var_at_95_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_var_at_98_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("monte_carlo_var_at_99_conf", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("run_date", COL_TYPE_ENUM.COL_STRING);

	}

	private void destroyTable(Table table) throws OException {
		if (table != null)
			table.destroy();
	}

	private void init() throws OException {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info("********************* Start of new run ***************************");
		
		constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		SIMULATION_DEFINITION = constRepo.getStringValue(CONST_REPO_VAR_SIM_DEF);
		SAVED_QUERY = constRepo.getStringValue(CONST_REPO_VAR_SAVED_QUERY);
	}

}
