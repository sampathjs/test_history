package com.openlink.matthey.simresults;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

/**
 * Produce ReportBuilder data source for SimResults {@value #SIM_RESULTS}<br>
 * Obtain results from scenario {@value #SCENARIO} 
 *
 */
public class PNL_DetailByLeg extends SimulationResults
{

	private static final String PORTFOLIO = "pfolio";
	private static final String SCENARIO = "Base";
	private static final String SIM_RESULTS = "PNL Detail";


	public PNL_DetailByLeg() {
		super(SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE);	
		
		setSimResultInfo(SIM_RESULTS, SCENARIO);

	}

		
	@Override
	protected Table addDataToResults(Table source) {
		Logger.log(LogLevel.DEBUG, 
				LogCategory./*SimResults*/Trading, 
				this, 
				String.format("Enrich data with portfolio id[%s][%d]",PORTFOLIO, getCurrentPortfolio()));

		try {
			source.addCol(PORTFOLIO, COL_TYPE_ENUM.COL_INT);
			source.setColValInt(PORTFOLIO, getCurrentPortfolio());
			
		} catch (OException e) {
			throw new SimulationResultsException(String.format("%s data enrichment failed", this.getClass().getSimpleName()),e, SIMRESULTS_DATA_GENERAL);
		}
		return source;
	}

}
