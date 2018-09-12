package com.olf.jm.metalstransfer.opservice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.metalstransfer.model.ConfigurationItem;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumInstrumentFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-11-02	V1.1	jwaechter	- added logic to skip check in case it is 
 *                                    being executed within the TPM 
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckForRunningMetalTransferTPM extends
AbstractTradeProcessListener {
	public static final String TPM_WORKFLOW_NAME="Metal Transfer";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {
			init (context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				if (isStrategyDeal(context, ppi) && !isUserInAllowedUserList(context)) {
					PluginLog.info("It's necessary to check if the TPM is running.");
					return checkForRunningProcess(context, ppi);
				} else {
					PluginLog.info("Skipping block logic as processed deal is "
						+	" either no Strategy or user is on the white list");
				}
			}
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			PluginLog.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
			    Files.write(Paths.get(PluginLog.getLogPath()), getStackTrace(t).getBytes(), StandardOpenOption.APPEND);
			}catch (IOException e) {
				PluginLog.error("Error printing stack frame to log file");				
			}
		}
		return PreProcessResult.succeeded();
	}
	
	private PreProcessResult checkForRunningProcess(Context context,
			PreProcessingInfo<EnumTranStatus> ppi) {
		Table runningProcesses=null;
    	try {
			com.olf.openjvs.Table rp = Tpm.retrieveWorkflows();
			runningProcesses = context.getTableFactory().fromOpenJvs(rp, true);
			rp.destroy();
		} catch (OException e) {
			throw new RuntimeException ("Error retrieving running TPM workflows ", e);
		}
    	Set<Long> transactionsOfStrategy = getAllTransactionsOfStrategy (context, ppi.getTransaction());
    	int tpmDefMetalTransfersId = context.getStaticDataFactory().getId(
    			EnumReferenceTable.TpmDefinition, TPM_WORKFLOW_NAME);
    	for (int row = runningProcesses.getRowCount()-1; row >= 0; row--) { // check if there is an already running process
    		int bpmDefId = runningProcesses.getInt("bpm_definition_id", row);
    		long itemNum = runningProcesses.getLong("item_num", row);
    		if (bpmDefId != tpmDefMetalTransfersId) {
    			continue;
    		}
    		if (transactionsOfStrategy.contains(itemNum)) {
    			return PreProcessResult.failed("The Metal Transfer TPM is already running for this strategy", false);
    		}
    	}
    	return PreProcessResult.succeeded();
	}

	private boolean isStrategyDeal(Context context,
			PreProcessingInfo<EnumTranStatus> ppi) {
		Transaction tran = ppi.getTransaction();
		Instrument ins = tran.getInstrument();
		return ins.getValueAsInt(EnumInstrumentFieldId.InstrumentType) == EnumInsType.Strategy.getValue();
	}

	private boolean isUserInAllowedUserList(Context context) {
		Person user = context.getUser();
		String csvAllowedUsers = ConfigurationItem.ALLOWED_USERS.getValue();
		for (String allowedUser : csvAllowedUsers.split(",")) {
			allowedUser = allowedUser.trim();
			if (user.getName().trim().equals(allowedUser)) {
				return true;
			}
		}
		return false;
	}


	private Set<Long> getAllTransactionsOfStrategy(Context context,
			Transaction transaction) {
		String sql = 
				"\nSELECT ab_other.tran_num"
			+	"\nFROM ab_tran ab"
			+	"\n  INNER JOIN ab_tran ab_other"
			+	"\n    ON ab_other.deal_tracking_num = ab.deal_tracking_num"
			+	"\nWHERE ab.tran_num = " + transaction.getTransactionId();
		Table sqlResult = context.getIOFactory().runSQL(sql);
		Set<Long> transactions = new TreeSet<>();
		for (int row=sqlResult.getRowCount()-1; row >= 0; row--) {
			int tranNum = sqlResult.getInt("tran_num", row);
			transactions.add((long)tranNum);
		}
		return transactions;
	}

	private static String getStackTrace(Throwable t)
    {
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
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
}
