package com.olf.jm.metalstransfer.tpm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.metalstransfer.model.ConfigurationItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTranStatus;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-09-09	V1.0	jwaechter	- Initial Version
 * 2016-09-13	V1.1	jwaechter	- defect fix in SQL
 */

/**
 * This plugin checks whether the following minimal success criteria have been
 * reached at the end of the TPM process "Metal Transfer":
 * <ol>
 *   <li> The strategy deal has been validated </li>
 *   <li> At least a single Cash Transfer deal has been booked </li>
 * </ol>
 * It should be assured that this step is executed mandatory every time the TPM
 * ends.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class MetalTransferEOPValidityCheck extends AbstractProcessStep {

	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		try {
			init (context);
			process(context, process, variables);
			
		} catch (Throwable t) {
			PluginLog.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
			    Files.write(Paths.get(PluginLog.getLogPath()), getStackTrace(t).getBytes(), StandardOpenOption.APPEND);
			}catch (IOException e) {
				PluginLog.error("Error printing stack frame to log file");				
			}
			process.appendError(t.toString(), token);
			throw t;
		}
		return null;
	}

	
	private void process(Context context, Process process, Variables variables) {
        int tranNum = process.getVariable("TranNum").getValueAsInt();
        String sql = 
        		"\nSELECT old_ab.deal_tracking_num, ab.tran_status, ISNULL(ativ.tran_num, -1) AS cash_deal"
        	+	"\nFROM ab_tran old_ab"
        	+   "\nINNER JOIN ab_tran ab "
        	+	"\n  ON ab.deal_tracking_num = old_ab.deal_tracking_num"
        	+	"\n     AND ab.current_flag = 1"
        	+   "\n LEFT OUTER JOIN ab_tran_info_view ativ"
        	+   "\n  ON ativ.value = CONVERT(varchar, ab.tran_num)"
        	+   "\n     AND ativ.type_name = 'Strategy Num'"
        	+	"\nWHERE old_ab.tran_num = " + tranNum
        	;
       try (Table sqlResult = context.getIOFactory().runSQL(sql);) {
    	   if (sqlResult == null) {
    	    	 throw new RuntimeException ("Error: could not run sql " + sql);      		   
    	   }
    	   if (sqlResult.getRowCount() == 0) {
  	    	 throw new RuntimeException ("Error: could not run sql " + sql);      		       		   
    	   }
    	   int dealTrackingNum = sqlResult.getInt("deal_tracking_num", 0);
    	   StringBuilder errorMessage = new StringBuilder ();
    	   if (sqlResult.getInt("tran_status", 0) != EnumTranStatus.Validated.getValue()) {
     		  errorMessage.append("The metal transfer strategy deal#" + dealTrackingNum 
     				  + " has not been validated.\n");
    	   }
    	   if (sqlResult.getInt("cash_deal", 0) == -1) {
      		  errorMessage.append("Not a single cash transfer deal has been booked for "
      				  + " the metal transfer stragy deal#" + dealTrackingNum + ".\n");
    	   }
    	   if (errorMessage.length() > 0) {
    		   errorMessage.append("Please check the trace log in the TPM log section "
    				 +  "and the error logs in the error log directory for details.");
    		   throw new OpenRiskException(errorMessage.toString());
    	   }
       }
	}

	private void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR");
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		if (logFile.trim().equals("")) {
			logFile = getClass().getName() + ".log";
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}


    private static String getStackTrace(Throwable t)
    {
          StringWriter sWriter = new StringWriter();
          t.printStackTrace(new PrintWriter(sWriter));
          return sWriter.getBuffer().toString();
    }
}
