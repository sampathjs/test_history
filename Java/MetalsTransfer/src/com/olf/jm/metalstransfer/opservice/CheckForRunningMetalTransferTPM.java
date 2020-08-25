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
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-11-02	V1.1	jwaechter	- added logic to skip check in case it is 
 * 2020-08-25	V1.2	VishwN01	- Changing the logic to point user table instead of TPM and decide whether amendment is allowed or not.                                   
                                  	  being executed within the TPM 
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckForRunningMetalTransferTPM extends
AbstractTradeProcessListener {
	public static final String TPM_WORKFLOW_NAME="Metal Transfer";
	public static final String STRATEGY_RUNNING ="Running";
	public static final String STRATEGY_ASSIGNMENT="Assignment";
	public static final String STRATEGY_SUCCEEDED="Succeeded";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {
			init (context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				if (isStrategyDeal(context, ppi)) {
					Logging.info("It's necessary to check if the TPM is running.");
					return checkForRunningProcess(context, ppi);
				} else {
					Logging.info("Skipping block logic as processed, deal is in Pending status in User_strategy_deals");
				}
			}
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			Logging.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
				Logging.error("Error executing " + this.getClass().getName() + ":\n " + getStackTrace(t).getBytes().toString());			
			}catch (Exception e) {
				Logging.error("Error printing stack frame to log file",e);				
			}
		}finally{
			Logging.close();
		}
		return PreProcessResult.succeeded();
	}
	
	private PreProcessResult checkForRunningProcess(Context context,
			PreProcessingInfo<EnumTranStatus> ppi) throws OException {
		
    	String statusOfStrategy = getAllTransactionsOfStrategy (context, ppi.getTransaction());
    	
    	if (statusOfStrategy.equals(STRATEGY_RUNNING)){
    			return PreProcessResult.failed("The Metal Transfer TPM is already running for this strategy", false);
    		}
    	if (statusOfStrategy.equals(STRATEGY_SUCCEEDED)){
    		    return PreProcessResult.failed("The Metal Transfer TPM is already processed for this strategy", false);
    	}
    	if (statusOfStrategy.equals(STRATEGY_ASSIGNMENT)){
    		    return PreProcessResult.failed("The Metal Transfer TPM is already running for this strategy and currently is in assignment state", false);
    	}
    	
    	return PreProcessResult.succeeded();
	}


	private boolean isStrategyDeal(Context context,
			PreProcessingInfo<EnumTranStatus> ppi) {
		Transaction tran = ppi.getTransaction();
		Instrument ins = tran.getInstrument();
		return ins.getValueAsInt(EnumInstrumentFieldId.InstrumentType) == EnumInsType.Strategy.getValue();
	}


	private String getAllTransactionsOfStrategy(Context context,
			Transaction transaction) {
		String sql = 
				"\nSELECT ab.tran_num, us.status"
			+	"\nFROM ab_tran ab"
			+	"\n  INNER JOIN user_strategy_deals us"
			+	"\n    ON ab.deal_tracking_num = us.deal_num"
			+	"\nWHERE ab.tran_num = " + transaction.getTransactionId()
			+	"\n AND us.process_type = 'NEW'";
		Table sqlResult = context.getIOFactory().runSQL(sql);
			String status = sqlResult.getString("status", 0);
			
		return status;
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
			Logging.init( this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
}
