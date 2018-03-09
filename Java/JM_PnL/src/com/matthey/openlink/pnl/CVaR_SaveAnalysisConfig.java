package com.matthey.openlink.pnl;

import com.olf.openjvs.*;
import com.openlink.util.logging.PluginLog;

public class CVaR_SaveAnalysisConfig implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		initPluginLog();

		String tsa_name;
		int row, num_rows;
		Table tsa_list, tsd = Util.NULL_TABLE;

		tsa_list = TimeSeries.analysisListAllConfigurations();
		if (Table.isTableValid(tsa_list) == 0){
			PluginLog.error("Unable to Load Time Series Configurations\n");
			OConsole.oprint("ERROR: Unable to Load Time Series Configurations\n");
			Util.exitFail();
		}

		// tsa_list.viewTable();
		num_rows = tsa_list.getNumRows();
		for (row = 1; row <= num_rows; row++){
			tsa_name = tsa_list.getString("tsa_name", row);

			tsd = TimeSeries.generateCurrentDataForAnalysisConfig(tsa_name);

			if (TimeSeries.saveData(tsd) == 0){
				PluginLog.error("Problem Encountered Saving Time Series Data for " + tsa_name + "\n");
				OConsole.oprint("ERROR: Problem Encountered Saving Time Series Data for " + tsa_name + "\n");
				tsd.destroy();
				continue;
			}			
			
			tsd.destroy();
			
			if (TimeSeries.analysisSaveDatasets(tsa_name) == 0)
			{
				PluginLog.error("Problem Encountered Saving Analysis Datasets for " + tsa_name + "\n");
				OConsole.oprint("ERROR: Problem Encountered Saving Analysis Datasets for " + tsa_name + "\n");
				tsd.destroy();
				continue;				
			} 
		}

		tsa_list.destroy();
		Util.exitSucceed();
	}
	
	/**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private void initPluginLog() throws OException 
	{	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().isEmpty()) 
		{
			logDir = abOutdir;
		}
		if (logFile.trim().isEmpty()) 
		{
			logFile = this.getClass().getName() + ".log";
		}
		try 
		{
			PluginLog.init(logLevel, logDir, logFile);
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		PluginLog.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}
} 
