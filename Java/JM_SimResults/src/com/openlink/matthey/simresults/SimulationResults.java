package com.openlink.matthey.simresults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.RESULT_CLASS;
import com.olf.openjvs.enums.SCENARIOS_COL_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;
import com.openlink.matthey.reportbuilder.DataSource;

public abstract class SimulationResults extends DataSource {

	private static final int SIMRESULTS_RETREIVAL_FAILURE = 2242;
	private static final int SIMRESULTS_HEADER_INVALID = 2232;
	private static final int SIMRESULTS_HEADER_QUERY = 2231;
	private static final int SIMRESULTS_PARARMETER = 2238;
	protected static final int SIMRESULTS_DATA_GENERAL = 2230;


	private static final String RUNTYPE = "RUN_TYPE";
	private static final String REFERENCE = "REFERENCE";
	private static final String PORTFOLIOS = "PORTFOLIOS";
	private static final String SCENARIO = "SCENARIO";

	@SuppressWarnings("unused")
	private SimulationResults() {
		this.runType = SIMULATION_RUN_TYPE.EOD_SIM_TYPE;
		this.portfolios = new int[] {-1};
		this.reference = "";
	}

private SIMULATION_RUN_TYPE runType;
private int[] portfolios;
private String reference;

private String simulationName;
private String scenarioColumn;
private int scenarioRow;
private int currentPortfolio;
private int baseScenario;


	/**
	 * Create SimResults Plugin 
	 * @param runType 	  defines the SimType e.g. EOD, Intra Day...
	 * @param reference   Optional reference to filter the results on
	 * @param portfolios  the portfolios to include in the selection
	 */
	public SimulationResults(SIMULATION_RUN_TYPE runType, String reference, int[] portfolios) {
		this.runType = runType;
		this.portfolios = portfolios;
		this.reference = reference;
	}


	/**
	 * Create SimResults Plugin where ALL portfolios were selected 
	 * @param intraDaySimType defines the SimType e.g. EOD, Intra Day...
	 */
	public SimulationResults(SIMULATION_RUN_TYPE intraDaySimType) {
		this(intraDaySimType,"",new int[] {-1});
	}

	/**
	 * @param name		defines the name of the SimResults to be used 
	 * @param scenario	the name of the scenario to obtain the results from
	 */
    public void setSimResultInfo(final String name, final String scenario) {
    	this.simulationName = name;
    	
    	setScenario(scenario);
    	this.scenarioColumn = "result_class";
    	this.scenarioRow = RESULT_CLASS.RESULT_GEN.toInt();
    }


    /**
     * Enable implementation to change the scenario used to pull the SimResults from
     * This uses the
     * @param scenario as the scenario name
     * <br>In the event the name provided doesn't appear to be defined, it defaults to 'Base' scenario
     */
	private void setScenario(final String scenario) {
		
/*
  		try {
			int scenarioArgument = Ref.getValue(SCENARIO TABLE, scenario);
		} catch (OException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
		if (scenario.equalsIgnoreCase("Base")) {
			baseScenario=1;
    	}
	}
    
	/**
	 * entry point for optional child to enrich behaviour processing
	 */
	protected void process() { 
	}
	
	@Override
	protected void evaluateArguments(String pluginName, Table arguments) {

		if (null == arguments) {
			return;
		}
		
		String argument = null;
		// if argument found pass value to handler, otherwise use default
		try {
			int totalParameters = arguments.getNumRows();
			for (int parameterRow = 1; parameterRow <= totalParameters; parameterRow++) {

				argument = arguments.getString("parameter_name", parameterRow);
				if (RUNTYPE.equalsIgnoreCase(argument)) {
					runType = getRunTypeArgument(arguments.getString("parameter_value", parameterRow));
					continue;
					
				} else if (PORTFOLIOS.equalsIgnoreCase(argument)) {
					argument = arguments.getString("parameter_value", parameterRow);
					if (null == argument)
						argument = concatinatePortfolios(portfolios);

					portfolios = getPortfolioArgument(argument);
					continue;
					
				} else if (SCENARIO.equalsIgnoreCase(argument) && null != argument) {
					setScenario(argument);
				}
			}
			
		} catch (OException e) {
			throw new SimulationResultsException("Unable to get plugin argument", e, SIMRESULTS_PARARMETER);
		}

	}
	
	protected int getCurrentPortfolio() {
		return currentPortfolio;
	}
	
	/**
	 * parse portfolio arguments from ReportBuilder 
	 * @param portfolios is assumed to be a Comma Separated List of portfolio to include in reported results
	 * <br><code>e.g. 2008, 2007, My Custom porfolio, 3001</code>
	 * <br>As is demonstrated either the <b>portfolio id</b> or its <b>name</b> can be populated in this list
	 * <br>
	 * @return    array of effective portfolios id's to use
	 */
	protected int[] getPortfolioArgument(String portfolios) {
		int[] runtimeArguments = null;
		if (null == portfolios 
				|| portfolios.isEmpty() 
				|| 0 == "ALL".compareToIgnoreCase(portfolios)) {
			//if -1 = ALL, otherwise check values are valid
			runtimeArguments = new int[] {-1};
			
		} else {
			//parse 
			List<String> pfolios = Arrays.asList(portfolios.split(","));
			List<Integer> runtimePortfolios =  new ArrayList<>(0);
			
			for(String portfolio : pfolios ) {
				if (!portfolio.trim().isEmpty()) {
					int portfolioArgument = 0;
					try {

						portfolioArgument = Integer.parseInt(portfolio.trim());
					} catch (NumberFormatException nfe) {
						// assume name was provided!
						try {
							portfolioArgument = Ref.getValue(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, portfolio.trim());
							
						} catch (OException e) {
							Logger.log(LogLevel.INFO, 
									LogCategory./*SimResults*/Trading,
									this, 
									String.format("Portfolio(%s) NOT found ignoring it", portfolio));
							continue;
						}
					}
					if (portfolioArgument>0)
						runtimePortfolios.add(portfolioArgument);
				}
			}
			
			Logger.log(LogLevel.DEBUG, 
					LogCategory./*SimResults*/Trading,
					this, 
					String.format("Portfolio>%s", runtimePortfolios));
			
			if (!runtimePortfolios.isEmpty()) {
				runtimeArguments = new int[runtimePortfolios.size()];
				int position = 0;
				for (int runtimePortfolio : runtimePortfolios) {
					runtimeArguments[position++] = runtimePortfolio;
				}
				
			}
		}
		return runtimeArguments;
	}
	
	/**
	 * get the ReportBuilder argument
	 * @return    the effective value assigned
	 */
	protected SIMULATION_RUN_TYPE getRunTypeArgument(String runType) {
		
		SIMULATION_RUN_TYPE runtimeArgument;
		try {
			runtimeArgument = SIMULATION_RUN_TYPE.valueOf(runType.toUpperCase().replace(" ", "_")+"_SIM_TYPE");
			
		} catch (IllegalArgumentException ia ) {
			Logger.log(LogLevel.INFO, 
					LogCategory./*SimResults*/Trading,
					this, 
					String.format("Sim Reval Type(%s) NOT found using default(%s)", runType, this.runType.toString()));
			runtimeArgument = this.runType;
		}
		return runtimeArgument;
	}
	
	


	/**
	 * populate supplied table with data from matching sim results
	 */
	@Override
	protected void generateOutputTable(final Table output) {

		Logger.log(LogLevel.DEBUG, 
				LogCategory./*SimResults*/Trading, 
				this, 
				String.format("processing for portfoios[%s]", concatinatePortfolios(portfolios)));
		try {
			Table reportData = getSavedSimulationResults(runType, portfolios);
			
			int totalRows = reportData.getNumRows();

			Table target = null;
			for (int simResult = 1; simResult <= totalRows; simResult++) {
				currentPortfolio = reportData.getInt("pfolio", simResult);
				Table results = SimResult.tableLoadSrun(
						currentPortfolio /* portfolio */, 
						runType, 
						reportData.getDateTime("last_run_time", simResult).getDate(),
						0);

				if (Table.isTableValid(results) <= 0) {
					OConsole.oprint(String.format("Unable to load Simulation Results(%s) \n", 
							Table.formatRefInt(currentPortfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE)));
					continue;
				}

				Table scenario = results.getTable(SCENARIOS_COL_ENUM.SCENARIO_RESULTS_COL.toInt(), baseScenario);
				if (scenario == null) {
					Logger.log(LogLevel.WARNING, 
							LogCategory./*SimResults*/Trading, 
							this, 
							String.format("Portfolio(%d) missing scenario data", currentPortfolio));
					continue;
				}
				Table resultsLayout = SimResult.getGenResultTables(
						scenario.getTable(scenarioColumn, scenarioRow),
						Ref.getValue(SHM_USR_TABLES_ENUM.RESULT_TYPE_TABLE,
								simulationName))
							.getTable("results", 1);

				if (resultsLayout == null) {
					Logger.log(LogLevel.WARNING, 
							LogCategory./*SimResults*/Trading, 
							this, 
							String.format("Portfolio(%d) missing SimResults data", currentPortfolio));
					continue;
				}
				
				results = addDataToResults(resultsLayout);
				if (null == target)
					target= results.cloneTable();
				results.copyRowAddAll(target);
				results.destroy();
				if (isLayoutOnly()) {
					Logger.log(LogLevel.DEBUG, 
							LogCategory./*SimResults*/Trading, 
							this, 
							"META-DATA - layout processing only");
					target.clearDataRows();
					break;
					
				} else {
					Logger.log(LogLevel.DEBUG, 
							LogCategory./*SimResults*/Trading, 
							this, 
							String.format("Portfolio %d has %d entries", currentPortfolio,resultsLayout.getNumRows()));

				}
			}

			if (null != target) {
				target.copyTableToTable(output);
				target.destroy();
			}
			
		} catch (OException e) {
			throw new SimulationResultsException("SimResults data failure",e, SIMRESULTS_RETREIVAL_FAILURE);
		}
	}

	/**
	 * override data enrichment/result set
	 * @param source
	 */
	protected Table addDataToResults(Table source) {
		
		Table target;
		try {
			
			target = Table.tableNew();
			return formatResults(source, target);

		} catch (OException e) {
			throw new SimulationResultsException("SimResults data processing failed",e, SIMRESULTS_DATA_GENERAL);
		}
	}

	/**
	 * Enable implementor to format the resulting data dependent of specific criteria
	 * There are occasions when its better to provide the reference data instead of it underlying id's 
	 * @param sourceData
	 * @param target
	 * @return
	 */
	public Table formatResults(Table sourceData, Table target) {
		//TODO convert source data
		
		//TODO populate formatted results into target
		try {
			return sourceData.copyTableToTable(target);
			
		} catch (OException e) {
			throw new RuntimeException("SimResults data processing failed",e);
		}
	}
	
	/**
	 * determine the most recently saved results matching the portfolios of the supplied simResultsType
	 */
	private Table getSavedSimulationResults(SIMULATION_RUN_TYPE simResultsType,int[] portfolios) {
		Table reportData;
		int ret = 0;
		try {
			reportData = Table.tableNew();
			ret = DBaseTable.execISql(reportData , String.format("SELECT pfolio, MAX(run_time) last_run_time " +
					"\nFROM  sim_header " +
					"\nWHERE pfolio IN (%s)" +
					" AND run_type in (%d )" + /*7  */
					"\nGROUP BY pfolio ",
					concatinatePortfolios(portfolios),
					simResultsType.toInt()));
			
		} catch (OException e) {
			throw new SimulationResultsException("SimResults request failed",e, SIMRESULTS_HEADER_QUERY);
		}
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
		{
			throw new SimulationResultsException("Unable to query SimResults!", SIMRESULTS_HEADER_INVALID);
		}
		return reportData;
	}
	
	/**
	 * return comma separated string of all supplied portfolios
	 */
	private String concatinatePortfolios(int[] portfolios) {

		StringBuffer portfoliosList = new StringBuffer();

		String prefix = "";
		for (Integer id : portfolios) {
			portfoliosList.append(prefix).append(id);
			prefix = ",";
		}
		return portfoliosList.toString();
	}



}