package com.olf.jm.metalstatements.rb.plugin;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public abstract class MetalStatementsDataSource extends AbstractGenericScript {

	private static final String REPO_CONTEXT = "EOM";
	private static final String REPO_SUB_CONTEXT = "MetalStatements";
	
	protected MetalStatementsParameter parameter = null;
	
	@Override
	public Table execute(Session session, ConstTable table) {
		
		try {
			/* Initialise the log file */
			setupLog();
			
			Table parameters = table.getTable("PluginParameters", 0);
			setParameters(session, parameters);
			
			Table output = session.getTableFactory().createTable();
			
			/* Set the output columns */
			setOutputColumns(output);
			
			/* Generate the report data */
			output = buildOutput(session, output);
			
			/* Format the report data */
			formatOutputData(output);
			
			return output;
			
		} catch (Exception e) {
			Logging.error("Failed to execute data source. An exception has occurred: " + e.getMessage());
			throw new RuntimeException(e);
		}finally{
			Logging.close();
		}
	}

	protected void setParameters(Session session, Table parameters) throws Exception {
		this.parameter = new MetalStatementsParameter(parameters);
	}
	
	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws Exception {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";

		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, REPO_SUB_CONTEXT);
		String logLevel = constRepo.getStringValue("logLevel","DEBUG");
		String logFile = constRepo.getStringValue("logFile", "EOMMetalStatementPlugins.log");
		String logDir = constRepo.getStringValue("logDir", abOutDir);

		try {
			Logging.init( this.getClass(), REPO_CONTEXT, REPO_SUB_CONTEXT);

		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	
	/**
	 * Build the output data
	 *  
	 * @param output
	 * @return
	 */
	protected abstract Table buildOutput(Session session, Table output);
	
	/**
	 * Add columns to output table  
	 *  
	 * @param output
	 */
	protected abstract void setOutputColumns(Table output);
	
	/**
	 * Override this method in subclass to apply output formatting
	 * 
	 * @param output
	 */
	protected void formatOutputData(Table output) {
		
	};

}
