/********************************************************************************
 * Script Name:   SaveAnalysisData
 * Script Type:   main
 * Status:        complete
 *
 * Revision History:
 * 1.0 - 
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.openlink.alertbroker.AlertBroker;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;

/**
 * Save the computed Correlations and Volatilities as one would do using Time
 * Series>Save Analysis Datasets menu item on the Market Manager main screen.
 * 
 * This script need an entry in const repository: Context "EOD", Sub-context
 * "TimeSeries", String "tsa_name" Values can be found in the tsa_name column in
 * the tseries_analysis_cfg table It will exit with a status of FAILED if the
 * data couldn't be saved It will exit with a status of SUCCEEDED if data are
 * saved
 * 
 * @author rlatif
 * @version 1.0
 * @category EOD Dependencies: setup via Constants Repository
 */
public class SaveAnalysisData implements IScript {
	ConstRepository repository = null;

	public void execute(IContainerContext context) throws OException {
		repository = new ConstRepository("EOD", "TimeSeries");

		initPluginLog();

		// 'try'-wrap for unexpected errors: e.g. within use of database
		// functions in jvs
		try {
			String analysisConfig = repository.getStringValue("tsa_name");
			saveComputedCorrelationsAndVolatilities(analysisConfig);
		} catch (OException oe) {
			String strMessage = "Unexpected: " + oe.getMessage();
			Logging.error(strMessage);
			AlertBroker.sendAlert("EOD-STS-004", strMessage);
		}finally{
			Logging.close();
		}

	}

	void initPluginLog() throws OException {
		String logLevel = repository.getStringValue("logLevel", "Error");
		String logFile = repository.getStringValue("logFile", "");
		String logDir = repository.getStringValue("logDir", "");

		try {
			Logging.init(this.getClass(), repository.getContext(),repository.getSubcontext());
		} catch (Exception ex) {
			String strMessage = getClass().getSimpleName()
					+ " - Failed to initialize log.";
			OConsole.oprint(strMessage + "\n");
			AlertBroker.sendAlert("EOD-STS-001", strMessage);
			Util.exitFail();
		}
	}

	// Save computed volatilities and correlations the same way using the GUI:
	// Time Series>Save Analysis Datasets menu item on the Market Manager main
	// screen.
	void saveComputedCorrelationsAndVolatilities(String analysisConfig)
			throws OException {

		if (TimeSeries.analysisSaveDatasets(analysisConfig) == 0) {
			String strMessage = "Computed correlations and volatilities: failed to save.";
			Logging.error(strMessage);
			AlertBroker.sendAlert("EOD-STS-003", strMessage);
		} else {
			Logging.info("Computed correlations and volatilities saved.");
		}
	}
}