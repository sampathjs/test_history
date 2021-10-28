package com.olf.jm.monitorrollingtrades.ops;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;
import com.olf.openrisk.tpm.TpmFactory;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/*
 * History:
 * 2021-10-15   V1.0    GanapP02  EPI-1941  - WO0000000117266 - Initial version
 * 
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class MonitorRollingTrades extends AbstractTradeProcessListener {

	/** The const repository used to initialise the logging classes. */
	public ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "OpsService";

	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "MonitorRollingTrades";

	/** The User table for rolling reasons picklist */
	public static final String USER_JM_ROLLING_REASONS = "USER_jm_rolling_reasons";

	/** The tran info for rolling reasons */
	public static final String ROLLING_REASON = "Rolling Reason";

	/** The tran info for rolling reasons */
	public static final String ROLLED_FROM = "Rolled From";

	/** The TPM Variable name */
	public static final String TPM_VAR_ROLLED_FROM = "rolled_from";

	/** The TPM Variable name */
	public static final String TPM_VAR_ROLLING_REASON = "rolling_reason";

	/** The TPM Variable name */
	public static final String TPM_VAR_DEAL_NUM = "tran_num";

	/** The TPM Variable name */
	public static final String TPM_VAR_MESSAGE = "Message";

	/** The TPM definition Name */
	public static String TPM_DEFINITION;

	private static IOFactory iof;
	private static TableFactory tf;
	private static TpmFactory tpmf;

	/**
	 * Initialise the class loggers.
	 * 
	 * @throws Exception
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void init(Session session) throws Exception {

		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);
		TPM_DEFINITION = constRep.getStringValue("Tpm Definition", "Monitor-Rolled Trades");
		iof = session.getIOFactory();
		tf = session.getTableFactory();
		tpmf = session.getTpmFactory();
		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;
		try {
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getMessage());
		}
		Logging.info("**********" + this.getClass().getSimpleName() + " started **********");
	}

	@Override
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {

		boolean runPostProcess = false;
		boolean isCancelled = false;
		try {
			init(context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {

				Logging.info(this.getClass().getSimpleName() + " started pre process run");
				Transaction tran = ppi.getTransaction();
				Logging.info("Processing for transaction with tran num %s", tran.getTransactionId());

				RollingTrade trade = new RollingTrade(context, tran);
				if (trade.isRollingTrade()
						&& ("".equalsIgnoreCase(trade.getRolledFrom()) || "".equalsIgnoreCase(trade.getRollingReason()))) {
					if (askReasonForRolling(trade)) {
						isCancelled = true;
					} else {
						setReasonToTran(trade, tran);
					}
				}
				if (trade.isRolledMultipleTimes() || isCancelled) {
					runPostProcess = true;
					Table clientDataTable = clientData.getTable("ClientData Table", 0);
					if (!clientDataTable.isValidColumn("deal_num")) {
						clientDataTable.addColumn("deal_num", EnumColType.Int);
					}
					if (!clientDataTable.isValidColumn("is_cancelled")) {
						clientDataTable.addColumn("is_cancelled", EnumColType.Int);
					}
					int row = clientDataTable.addRows(1);
					clientDataTable.setInt("deal_num", row, tran.getDealTrackingId());
					clientDataTable.setInt("is_cancelled", row, 1);
				}
			}
			Logging.info(this.getClass().getSimpleName() + " suceeeded");
		} catch (RuntimeException ex) {
			String message = this.getClass().getSimpleName() + " failed because of " + ex.toString();
			Logging.error(message);
		} catch (Exception ex) {
			String message = this.getClass().getSimpleName() + " failed because of " + ex.toString();
			Logging.error(message);
		} finally {
			Logging.info(this.getClass().getSimpleName() + " ended pre process run");
			Logging.close();
		}
		return PreProcessResult.succeeded(runPostProcess);
	}

	@Override
	public void postProcess(final Session session, final PostProcessingInfo<EnumTranStatus>[] infoArray, final boolean succeeded,
			final Table clientData) {
		try {
			init(session);
			Logging.info(this.getClass().getSimpleName() + " started post process run");
			for (PostProcessingInfo<EnumTranStatus> pi : infoArray) {
				Transaction tran = null;
				tran = session.getTradingFactory().retrieveTransactionById(pi.getTransactionId());
				int dealNum = tran.getDealTrackingId();
				Table clientDataTable = clientData.getTable("ClientData Table", 0);
				int row = clientDataTable.find(0, dealNum, 0);
				if(row >=0) {
					boolean isCancelled = clientDataTable.getInt("is_cancelled", row) == 1;
					triggerTpm(session, tran, isCancelled);	
				}
				Logging.info("Processing for transaction with tran num %s", tran.getTransactionId());
			}
		} catch (RuntimeException ex) {
			Logging.error(ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
			Logging.error(this.getClass().getName() + " ended with status failed");
			session.logStatus("Failed");
			throw ex;
		} catch (Exception ex) {
			String message = this.getClass().getSimpleName() + " failed because of " + ex.toString();
			Logging.error(message);
		} finally {
			Logging.info(this.getClass().getSimpleName() + " ended in post process");
			Logging.close();
		}
	}

	private void setReasonToTran(RollingTrade trade, Transaction tran) {

		Field rollingReason = tran.getField(ROLLING_REASON);
		rollingReason.setValue(trade.getRollingReason());

		Field rolledFrom = tran.getField(ROLLED_FROM);
		rolledFrom.setValue(trade.getRolledFrom());
	}

	private void triggerTpm(Session session, Transaction tran, boolean isCancelled) {

		ProcessDefinition tpmWorkflow;
		try {
			Logging.info(String.format("Run TPM! %s", TPM_DEFINITION));
			tpmWorkflow = tpmf.getProcessDefinition(TPM_DEFINITION);
		} catch (OpenRiskException olf) {
			Logging.info(String.format("Tran#%d, Monitor Rolled Trades workflow failure: %s", tran.getTransactionId(), olf.getMessage()));
			throw olf;
		}
		String cancelledMsg = "Cancelled by user, " + ROLLED_FROM + " and " + ROLLING_REASON + " were not entered on the deal";
		Variable tranNum = tpmf.createVariable(TPM_VAR_DEAL_NUM, EnumFieldType.Int, String.valueOf(tran.getTransactionId()));
		Variable rolledFrom = tpmf.createVariable(TPM_VAR_ROLLED_FROM, EnumFieldType.Int, tran.getField(ROLLED_FROM).getValueAsString());
		Variable rollingReason = tpmf.createVariable(TPM_VAR_ROLLING_REASON, EnumFieldType.String, tran.getField(ROLLING_REASON).getValueAsString());
		Variable message = tpmf.createVariable(TPM_VAR_MESSAGE, EnumFieldType.String, isCancelled ? cancelledMsg : "NA");
		Variables vars = tpmf.createVariables();
		vars.add(tranNum);
		vars.add(rolledFrom);
		vars.add(rollingReason);
		vars.add(message);
		Process process = tpmWorkflow.start(vars);
		Logging.info(String.format("%s(%d) STARTED", process.getDefinition().getName(), process.getId()));
	}

	private boolean askReasonForRolling(RollingTrade trade) throws Exception {

		String selectedReason = null;
		String selectedRolledFrom = null;
		boolean isCancelled = false;
		try {

			String defaultRolledFrom = "".equalsIgnoreCase(trade.getRolledFrom()) ? "" : trade.getRolledFrom();
			com.olf.openjvs.Table defaultRollingReason = com.olf.openjvs.Table.tableNew("JVS Default Selection");
			defaultRollingReason.addCol("rolling_reason", COL_TYPE_ENUM.COL_STRING);
			defaultRollingReason.addRow();
			defaultRollingReason.setString("rolling_reason", 1,
					"".equalsIgnoreCase(trade.getRollingReason()) ? "" : trade.getRollingReason());

			Table rollingReasons = iof.getUserTable(USER_JM_ROLLING_REASONS, true).retrieveTable();
			com.olf.openjvs.Table askTable = com.olf.openjvs.Table.tableNew("JVS Ask Table");
			Ask.setTextEdit(askTable, ROLLED_FROM, defaultRolledFrom, ASK_TEXT_DATA_TYPES.ASK_STRING, "", 1);
			Ask.setAvsTable(askTable, tf.toOpenJvs(rollingReasons, true), "Rolling Reasons", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
					2, defaultRollingReason);
			String message = "\nThis trade appears to be a rolled trade and " + ROLLED_FROM + " or " + ROLLING_REASON + " are missing "
					+ "\n\nEnter reason for rolling the transaction or deal number from which it is being Rolled";
			int ret = Ask.viewTable(askTable, "FX trade Rolling trade details", message);
			if (ret <= 0) {
				Logging.info("User pressed cancel. Triggering TPM");
				askTable.destroy();
				rollingReasons.dispose();
				isCancelled = true;
			} else {
				selectedRolledFrom = askTable.getTable("return_value", 1).getString("return_value", 1);
				selectedReason = askTable.getTable("return_value", 2).getString("return_value", 1);
				askTable.destroy();
			}
		} catch (OException e) {
			throw new RuntimeException(e);
		}
		trade.setRolledFrom(selectedRolledFrom);
		trade.setRollingReason(selectedReason);

		return isCancelled;
	}
}