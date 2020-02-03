package com.jm.sc.bo.process;

import com.matthey.utilities.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Called from a TPM workflow - Auto Invoice Processing EOD UK_SA.
 * Cancels any existing PMM UK Cash trades with document in Approval Required status.
 * Perform cancellation only on the last day of the month, On other days - returns with out doing anything.
 * Save (in today's directory) & email excel report of the trades cancelled.
 * 
 * @author agrawa01
 *
 */
public class CancelPendingApprovalCharges implements IScript {

	private static final String CONTEXT = "BackOffice";
	private static final String TPM_VAR_NAME_CANCEL_CHARGE_SUBCONTEXT = "CancelPAChargesStepSubContext";
	private static final String CONST_REPO_VAR_NAME_EMAIL_SERVICE = "emailServiceName";
	private static final String CONST_REPO_VAR_NAME_EMAIL_RECIPIENTS = "emailRecipients";
	private static final String CONST_REPO_VAR_NAME_OUTPUT_FILE = "outputFileName";
	private static final String CONST_REPO_VAR_NAME_INTERNAL_BUNIT = "internalBU";
	private static final String CONST_REPO_VAR_NAME_DOC_STATUS = "docStatus";
	
	private ConstRepository constRepo = null;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table tData = Util.NULL_TABLE;
		
		try {
			Table tblArgt = context.getArgumentsTable();
			if (tblArgt == null || tblArgt.getNumRows() == 0) {
				throw new RuntimeException("Invalid argt table specified, no CancelPAChargesStepSubContext parameter!");
			}
			String subContext = tblArgt.getString(TPM_VAR_NAME_CANCEL_CHARGE_SUBCONTEXT, 1);
			
			initPluginLog(subContext);
			PluginLog.info("Plugin execution starts------");
			String emailService = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_EMAIL_SERVICE, "Mail");
			String recipients = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_EMAIL_RECIPIENTS);
			String fileName =  this.constRepo.getStringValue(CONST_REPO_VAR_NAME_OUTPUT_FILE, this.getClass().getName());
			String internalBU = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_INTERNAL_BUNIT);
			String docStatus = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_DOC_STATUS);
			
			PluginLog.info("Const Repo Values: InternalBU #" + internalBU + ", DocStatus #" + docStatus + ", EmailRecipients #" + recipients);
			if (internalBU == null || internalBU.isEmpty() || docStatus == null || docStatus.isEmpty()
					|| recipients == null || recipients.isEmpty()) {
				throw new RuntimeException("No value found for const repo variables - internalBU, docStatus, emailRecipients");
			}
			
			if (!isTodayLastDayOfMonth()) {
				return;
			}
			
			tData = fetchChargesInApprovalReqd(internalBU, docStatus);
			int rows = tData.getNumRows();
			PluginLog.info("No. of CASH Tran Charges (with document in Approval Required) retrieved are #" + rows);
			
			if (rows == 0) {
				PluginLog.info("No documents (for CASH Tran Charges) found to be in Approval Required status.");
				return;
			}
			
			addColumnsToOutputData(tData);
			PluginLog.info("Processing all Charges to Cancelled status...");
			cancelTrans(tData);
			
			String outputFileName = Util.reportGetDirForToday() + "\\" + fileName + "_" + System.currentTimeMillis() + ".xlsx";
			tData.excelSave(outputFileName);
			PluginLog.info("Output file saved to the directory: " + outputFileName);
			
			generateEmail(recipients, outputFileName, emailService);
			PluginLog.info("Email sent successfully");
			
		} catch (OException oe) {
			PluginLog.error("Exception occurred with message- " + oe.getMessage());
			throw oe;
			
		} finally {
			PluginLog.info("Plugin execution ends------");
			if (Table.isTableValid(tData) == 1) {
				tData.destroy();
			}
		}
	}

	protected void generateEmail(String recipients, String attachment, String emailService) throws OException {
		Utils.sendEmail(recipients, getEmailSubject(), getEmailBody(), attachment, emailService);
	}
	
	/**
	 * 
	 * @return
	 * @throws OException
	 */
	private boolean isTodayLastDayOfMonth() throws OException {
		int today = OCalendar.today();
		int lastDayOfMonth = OCalendar.getLgbd(OCalendar.getEOM(today) + 1);
		
		if (today == lastDayOfMonth) {
			return true;
		}
		
		PluginLog.info("Today #" + OCalendar.formatDateInt(today) + " is not last day #" + OCalendar.formatDateInt(lastDayOfMonth) + " of Month");
		return false;
	}

	private void addColumnsToOutputData(Table tData) throws OException {
		tData.addCol("tran_current_status", COL_TYPE_ENUM.COL_STRING);
		tData.addCol("status", COL_TYPE_ENUM.COL_STRING);
		tData.addCol("message", COL_TYPE_ENUM.COL_STRING);
	}

	/**
	 * 
	 * @param tData
	 * @throws OException
	 */
	private void cancelTrans(Table tData) throws OException {
		Transaction tran = Util.NULL_TRAN;
		int rows = tData.getNumRows();
		
		for (int row = 1; row <= rows; row++) {
			int tranNum = tData.getInt("tran_num", row);
			try {
				tran = Transaction.retrieve(tranNum);
				tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED);
				PluginLog.info("Successfully cancelled transaction(TranNum #" + tranNum + ")");
				
				tData.setString("status", row, "SUCCESS");
				tData.setString("tran_current_status", row, TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.name());
				
			} catch (OException oe) {
				String message = "Error in cancelling trade(TranNum #" + tranNum + "), Message- " + oe.getMessage(); 
				PluginLog.error(message);
				tData.setString("status", row, "FAILURE");
				tData.setString("message", row, message);
			
			} finally {
				if (Transaction.isNull(tran) != 1) {
					tran.destroy();
				}
			}
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or constant repository.
	 */
	protected void initPluginLog(String subContext) throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR");
		try {
			String logLevel = "INFO";
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir =  abOutDir + "\\error_logs";

			this.constRepo = new ConstRepository(CONTEXT, subContext);
			
			logLevel = this.constRepo.getStringValue("logLevel", logLevel);
			logDir   = this.constRepo.getStringValue("logDir", logDir);
			
			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new OException("Error initialising logging: " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param internalBU
	 * @param docStatus
	 * @return
	 * @throws OException
	 */
	protected Table fetchChargesInApprovalReqd(String internalBU, String docStatus) throws OException {
		Table tData = Util.NULL_TABLE;
		
		StringBuilder sbIntBUIds = new StringBuilder();
		String[] intBUs = internalBU.split(",");
		for (String bu : intBUs) {
			sbIntBUIds.append(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, bu.trim())).append(",");
		}
		
		if (sbIntBUIds.length() > 0) {
			sbIntBUIds.setLength(sbIntBUIds.length() -  1);
		}
		
		String sSQL = "SELECT DISTINCT ab.deal_tracking_num"
					+ ", ab.tran_num"
					+ ", ab.reference"
					+ ", ab.cflow_type"
					+ ", ab.personnel_id"
					+ ", ab.external_bunit"
					+ ", ab.trade_date"
					+ ", sh.document_num"
					+ ", '" + docStatus + "' AS doc_status "
				+ " FROM ab_tran ab "
				+ " INNER JOIN stldoc_details sd ON ab.deal_tracking_num = sd.deal_tracking_num AND ab.tran_num = sd.tran_num "
				+ " INNER JOIN stldoc_header sh ON sd.document_num = sh.document_num AND sh.doc_type = 1 "
				+ " WHERE ab.current_flag = 1 "
					+ " AND ab.internal_bunit IN (" + sbIntBUIds.toString() + ")"
					+ " AND ab.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()
					+ " AND ab.ins_sub_type = " + INS_SUB_TYPE.cash_transaction.toInt()
					+ " AND sh.doc_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, docStatus);
		
		PluginLog.debug("Executing SQL query: " + sSQL);
		tData = Table.tableNew();
		DBaseTable.execISql(tData, sSQL);
		
		if (Table.isTableValid(tData) != 1 || tData.getNumRows() < 0) {
			throw new OException("Error in executing SQL query:" + sSQL);
		}
		
		tData.defaultFormat();
		return tData;
	}
	
	/**
	 * 
	 * @return
	 * @throws OException
	 */
	protected String getEmailBody() throws OException {
		StringBuilder sb = new StringBuilder();
		Table envInfo = Util.NULL_TABLE;
		
		try {
			envInfo = com.olf.openjvs.Ref.getInfo();
			
			sb.append("This information has been generated from database: " + envInfo.getString("database", 1));
			sb.append(", on server: " + envInfo.getString("server", 1));
			sb.append("<br/><br/>");
			sb.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			sb.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			
			return "PFA Cash charges cancelled by the automated process as they were not approved by the cut off "
					+ "time (6:30 PM) on the last day of the month.<br/>"
					+ "Please contact the Endur Support team for any concerns. <br/><br/>"
					+ sb.toString();
			
		} finally {
			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
		}
	}
	
	protected String getEmailSubject() {
		return "Charges Cancelled (in Approval Required document status) - Summary Email";
	}

}
