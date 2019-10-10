package com.matthey.openlink.purge;

import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.fnd.UtilBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * This class purges tables specified in the USER_jm_purge_config.  
 * It will purge data based on a datetime column in a table and the specified number of days from the config
 */
public class PurgeTPMVariables implements IScript
{
	 

	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Purge";

	/**
	 * Specifies the constants' repository sub-context parameter.
	 */
	protected static final String REPO_SUB_CONTEXT = "TPMVariables";


	@Override
	public void execute(IContainerContext context) throws OException {
		// Setting up the log file.
		setupLog();

		try {
			PluginLog.info("START Running PurgeTPMVariables");

			String USER_StoredProcedureName = "USER_purge_tpm_disconnected_variables";
			Table argumentTableForStoredProcedure = Table.tableNew();
			argumentTableForStoredProcedure.addRow();
			int returnValue = DBase.runProc(USER_StoredProcedureName, argumentTableForStoredProcedure);

			PluginLog.info("FINISHED Running PurgeTPMVariables");
		} catch (Exception ex) {
			String message = "Script failed with the following error(s): " + ex.getMessage();
			PurgeUtil.printWithDateTime(message);
			Util.exitFail(message);
		} finally {
			 
		}

		 
		Util.exitSucceed();
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
		String logLevel = constRepo.getStringValue("logLevel","DEBUG");
		String logFile = constRepo.getStringValue("logFile", "PurgeTPMVariables.log");
		String logDir = constRepo.getStringValue("logDir", abOutDir);

		try {

			PluginLog.init(logLevel, logDir, logFile);

		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}         

	  
}


