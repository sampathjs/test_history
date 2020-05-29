package com.matthey.openlink.pnl;

import com.olf.openjvs.*;
import com.olf.jm.logging.Logging;

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
			Logging.error("Unable to Load Time Series Configurations\n");
			OConsole.oprint("ERROR: Unable to Load Time Series Configurations\n");
			Util.exitFail();
		}

		// tsa_list.viewTable();
		num_rows = tsa_list.getNumRows();
		for (row = 1; row <= num_rows; row++){
			tsa_name = tsa_list.getString("tsa_name", row);

			tsd = TimeSeries.generateCurrentDataForAnalysisConfig(tsa_name);

			if (TimeSeries.saveData(tsd) == 0){
				Logging.error("Problem Encountered Saving Time Series Data for " + tsa_name + "\n");
				OConsole.oprint("ERROR: Problem Encountered Saving Time Series Data for " + tsa_name + "\n");
				tsd.destroy();
				continue;
			}			
			
			tsd.destroy();
			
			if (TimeSeries.analysisSaveDatasets(tsa_name) == 0)
			{
				Logging.error("Problem Encountered Saving Analysis Datasets for " + tsa_name + "\n");
				OConsole.oprint("ERROR: Problem Encountered Saving Analysis Datasets for " + tsa_name + "\n");
				tsd.destroy();
				continue;				
			} 
		}
		Logging.close();
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
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().isEmpty()) 
		{
			logFile = this.getClass().getName() + ".log";
		}
		try 
		{
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}
} 
