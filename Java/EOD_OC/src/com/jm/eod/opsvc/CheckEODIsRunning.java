package com.jm.eod.opsvc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import com.jm.eod.opsvc.model.ConfigurationItem;
import com.olf.embedded.application.Context;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-09-19	V1.0	jwaechter	- Initial Version
 * 2016-10-19	V1.1	jwaechter	- Defect fix: added negation to
 *                                    check if TPM log entry is on the black list
 */

/**
 * This class checks if the global EOD TPM is running (the name of the TPM definition to check is taken
 * from {@link ConfigurationItem#EOD_WORKFLOW_NAME} ).
 * In case the EOD is running, the variable having name {@link ConfigurationItem#VAR_NAME_REPORT_BACK} 
 * is set to "Yes" and to "No" if not.
 * @author jwaechter
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CheckEODIsRunning extends AbstractProcessStep {
	
	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		try {
			init (context);
        	long wflowId = Tpm.getWorkflowId();
			return process(context, wflowId);
		} catch (Throwable t) {
			Logging.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
				Logging.error(getStackTrace(t).getBytes().toString());
			}catch (Exception e) {
				Logging.error("Error printing stack frame to log file");
			}
		}finally{
			Logging.close();
		}
		return null;
	}
    private Table process(Context context, long wflowId) {
    	Table runningProcesses=null;
    	try {
			com.olf.openjvs.Table rp = Tpm.retrieveWorkflows();
			runningProcesses = context.getTableFactory().fromOpenJvs(rp, true);		
			rp.destroy();
		} catch (OException e) {
			throw new RuntimeException ("Error retrieving running TPM workflows", e);
		}
    	Set<Integer> tpmDefMetalTransfersId = getDefIds(context); 
    	try {
        	for (int row = runningProcesses.getRowCount()-1; row >= 0; row--) { // check if there is an already running process
        		int bpmDefId = runningProcesses.getInt("bpm_definition_id", row);
        		if (!tpmDefMetalTransfersId.contains(bpmDefId)) {
        			continue;
        		}
        		//  EOD is running! Report back.
          		Tpm.setVariable(wflowId, ConfigurationItem.VAR_NAME_REPORT_BACK.getValue(), 
          				"Yes");      	
          		Logging.info("TPM variable '" + ConfigurationItem.VAR_NAME_REPORT_BACK.getValue()
          				+ "' has been set to 'Yes'");
          		return null;
        	}
      		Logging.info("TPM variable '" + ConfigurationItem.VAR_NAME_REPORT_BACK.getValue()
      				+ "' has been set to 'No'");
      		Tpm.setVariable(wflowId, ConfigurationItem.VAR_NAME_REPORT_BACK.getValue(), 
      				"No");
    	} catch (OException ex) {
			throw new RuntimeException ("Error setting TPM variable '" + 
						ConfigurationItem.VAR_NAME_REPORT_BACK.getValue() 
					+ "' in workflow #" + wflowId , ex);
    	}
    	return null;
	}


	private Set<Integer> getDefIds(Session session) {
		Set<Integer> ids = new HashSet<>(); 
		for (String token : ConfigurationItem.EOD_WORKFLOW_NAME.getValue().split(",")) {
			String tpmDefName = token.trim();
			
			int id = session.getStaticDataFactory().getId(
					EnumReferenceTable.TpmDefinition, tpmDefName);
			ids.add(id);
			Logging.info("TPM definition '" +  tpmDefName + "' has id #" + id);
		}
		return ids;		
	}
	private static String getStackTrace(Throwable t) {
          StringWriter sWriter = new StringWriter();
          t.printStackTrace(new PrintWriter(sWriter));
          return sWriter.getBuffer().toString();
    }
	
	private void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR") + "\\error_logs";
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = abOutdir; //ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		if (logFile.trim().equals("")) {
			logFile = getClass().getName() + ".log";
		}
		try {
			Logging.init(this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}

}
