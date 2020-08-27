package com.olf.jm.metalstransfer.opservice;

import java.io.PrintWriter;
import java.io.StringWriter;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.metalstransfer.model.ConfigurationItem;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openrisk.application.Session;
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
public class CheckForRunningMetalTransferTPM extends AbstractTradeProcessListener {
	public static final String TPM_WORKFLOW_NAME = "Metal Transfer";
	public static final String STRATEGY_RUNNING = "Running";
	public static final String STRATEGY_ASSIGNMENT = "Assignment";
	public static final String STRATEGY_SUCCEEDED = "Succeeded";
	int tranStatus = 0;

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {

			init(context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				int strategyDeal = ppi.getTransaction().getTransactionId();
				Logging.info("Process started for strategy deal " + strategyDeal, false);
				if (strategyDeal <= 0) {
					Logging.info(
							"Deal is getting booked for first time,amendment check is not required. Hence skipping the check...");
				}
				if (isStrategyDeal(context, ppi) && strategyDeal > 0) {
					Logging.info("It's necessary to check if the TPM is running.");
					return checkForRunningProcess(context, strategyDeal);
				} else {
					Logging.info("Skipping block logic as processed, deal is in Pending status in User_strategy_deals");
				}
			}
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			Logging.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
				Logging.error("Error executing " + this.getClass().getName() + ":\n "
						+ getStackTrace(t).getBytes().toString());
			} catch (Exception e) {
				Logging.error("Error printing stack frame to log file", e);
			}
		} finally {
			Logging.info("Process finished for strategy deal ", false);
			Logging.close();
		}
		return PreProcessResult.succeeded();
	}

	private PreProcessResult checkForRunningProcess(Context context, int strategyDeal) throws OException {

		boolean server = isServer(context);
		String message;
		String statusOfStrategy = getAllTransactionsOfStrategy(context, strategyDeal);

		if (isNullOrEmpty(statusOfStrategy)) {

			Logging.info(
					"Strategy deal " + strategyDeal + " is not stamped to user_strategy_deals and can be amended.. ",
					false);

		} else if (server) {
			Logging.info("Process on strategy deal is with server user, skipping the amendment checks..", false);
		} else {

			if (tranStatus != TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()) {
				message = "The Metal Transfer TPM is already and tran_status is "
						+ Ref.getName(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, tranStatus)
						+ " for this strategy \n Amendment is not allowed at this stage.";
				Logging.info(message, false);
				return PreProcessResult.failed(message, false);

			}

			if (statusOfStrategy.equals(STRATEGY_RUNNING) && !server) {
				message = "The Metal Transfer TPM is already running for this strategy. \n Amendment is not allowed at this stage.";
				Logging.info(message, false);
				return PreProcessResult.failed(message, false);
			}
			if (statusOfStrategy.equals(STRATEGY_SUCCEEDED) && !server) {
				message = "The Metal Transfer TPM is already processed for this strategy \n Amendment is not allowed at this stage.";
				Logging.info(message, false);
				return PreProcessResult.failed(message, false);

			}
			if (statusOfStrategy.equals(STRATEGY_ASSIGNMENT) && !server) {
				message = "Metal Transfer TPM is already running for this strategy and currently is in assignment state. \n Amendment is not allowed at this stage.";
				Logging.info(message, false);
				return PreProcessResult.failed(message, false);

			}
		}
		return PreProcessResult.succeeded();

	}

	private boolean isServer(Context context) throws OException {

		int userId = 0;
		int query_id = 0;
		String sql = null;
		Table personnel = null;
		String queryName = "ServerUsers";
		try {
			query_id = Query.run(queryName);
			userId = Ref.getUserId();
			String userName = Ref.getName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, userId);
			Logging.info("Submitter username is " + userName);
			sql = "SELECT  1 FROM query_result WHERE  unique_id = " + query_id + " AND query_result = " + userId;
			personnel = context.getIOFactory().runSQL(sql);
			if (personnel.getRowCount() > 0) {
				return true;
			}
		} catch (OException e) {
			Logging.error("Failed to execute sql " + sql);
			throw e;
		} finally {
			Query.clear(query_id);
			if (personnel.isValidRow(0)) {
				personnel.dispose();
			}
		}

		return false;
	}

	private static boolean isNullOrEmpty(String str) {
		if (str != null && !str.isEmpty())
			return false;
		return true;
	}

	private boolean isStrategyDeal(Context context, PreProcessingInfo<EnumTranStatus> ppi) {
		Transaction tran = ppi.getTransaction();
		Instrument ins = tran.getInstrument();
		return ins.getValueAsInt(EnumInstrumentFieldId.InstrumentType) == EnumInsType.Strategy.getValue();
	}

	private String getAllTransactionsOfStrategy(Context context, int strategyDeal) throws OException {
		String sql = null;
		Table sqlResult = null;
		try {
			sql = "\nSELECT ab.tran_num, us.status,ab.tran_status" + "\nFROM ab_tran ab"
					+ "\n  INNER JOIN user_strategy_deals us" + "\n    ON ab.deal_tracking_num = us.deal_num"
					+ "\nWHERE ab.tran_num = " + strategyDeal + "\nAND ab.current_flag = 1"
					+ "\n AND us.process_type = 'NEW'";
			sqlResult = context.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() <= 0) {
				return null;
			}
			String status = sqlResult.getString("status", 0);
			tranStatus = sqlResult.getInt("tran_status", 0);

			return status;
		} finally {
			if (sqlResult.isValidRow(0)) {
				sqlResult.dispose();
			}

		}
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
		String logDir = abOutdir; // ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		if (logFile.trim().equals("")) {
			logFile = getClass().getName() + ".log";
		}
		try {
			Logging.init(this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
}
