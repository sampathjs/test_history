package com.jm.reportbuilder.ejm;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * Super class for running data source plugin for report builder
 * 
 * This class provides placeholder functions for 
 * 1. Collecting metadata for the data source
 * 2. Generating the output data
 * 3. Formatting the output data
 */
public abstract class EJMReportDataSource implements IScript
{
	private static final String REPO_CONTEXT = "Reports";
	private static final String REPO_SUB_CONTEXT = "EJM";
	
	protected EJMReportParameter reportParameter;	
	
	public void execute(IContainerContext context)
	{
		try {
			long startTime = System.currentTimeMillis();
			
			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();
			
			/* Initialise the log file */
			setupLog();
			
			/* Set the output columns */
			setOutputColumns(returnt);
			
			/* Collect plugin meta data */
			int mode = argt.getInt("ModeFlag", 1);
			if (mode == 0) {
				return;
			}
			
			/* Read the report parameters */
			reportParameter = new EJMReportParameter(argt.getTable("PluginParameters", 1));
			
			/* Generate the report data */
			generateOutputData(returnt);
			
			/* Format the report data */
			formatOutputData(returnt);
			
			long endTime = System.currentTimeMillis();
			Logging.info(String.format("Plugin execution completed. Time elapsed (milliseconds): %d",endTime-startTime));
			
		} catch (Exception e) {
			Logging.error("Failed to execute plugin. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		}finally{
			Logging.close();
		}
		
	}

	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException {
		
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";

		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, REPO_SUB_CONTEXT);
		String logLevel = constRepo.getStringValue("logLevel","Debug");
		String logFile = constRepo.getStringValue("logFile", "EJMDataSource.log");
		String logDir = constRepo.getStringValue("logDir", abOutDir);;

		try {

			Logging.init(this.getClass(), REPO_CONTEXT, REPO_SUB_CONTEXT);

		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	
	/**
	 * Add columns to output table  
	 *  
	 * @param output
	 */
	protected abstract void setOutputColumns(Table output);  
	
	/**
	 * Generate the output data
	 *  
	 * @param output
	 * @return
	 */
	protected abstract void generateOutputData(Table output);
	
	/**
	 * Override this method in subclass to apply output formatting
	 * 
	 * @param output
	 */
	protected void formatOutputData(Table output) {
		
	};
	
}