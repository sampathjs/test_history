package com.jm.sc.bo.alerts;

import com.matthey.utilities.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * Abstract class inherited by DailyChargesPendingApproval & MonthlyChargesPendingApproval classes.
 * Used to fetch & email PMM UK Cash trades (Ins_Type - CASH Tran) with document in Approval Required status.
 * 
 * @author agrawa01
 *
 */
public class ChargesPendingApproval implements IScript {

	private static final String CONTEXT = "BackOffice";
	private static final String SUB_CONTEXT = "ChargesPendingApproval";
	
	private static final String CONST_REPO_VAR_NAME_EMAIL_SERVICE = "emailServiceName";
	private static final String CONST_REPO_VAR_NAME_EMAIL_RECIPIENTS = "EmailRecipients";
	private static final String CONST_REPO_VAR_NAME_OUTPUT_FILE = "OutputFileName";
	private static final String CONST_REPO_VAR_NAME_INTERNAL_BUNIT = "internalBU";
	private static final String CONST_REPO_VAR_NAME_DOC_STATUS = "docStatus";
	private static final String TPM_VARIABLE_NAME_RUN_TYPE = "RunType";
	protected ConstRepository constRepo = null;
	
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table tData = Util.NULL_TABLE;
		try {
			String outputFileVarName = "";
			String emailRecipientsVarName = "";
			initPluginLog();
			
			Logging.info("Plugin execution starts------");
			
			Table tblArgt = context.getArgumentsTable();
			if (tblArgt == null || tblArgt.getNumRows() == 0) {
				throw new RuntimeException("Invalid argt table specified, no RunType parameter!");
			}
			
			String runType = tblArgt.getString(TPM_VARIABLE_NAME_RUN_TYPE, 1);
			if ("Daily".equals(runType) || "Monthly".equals(runType)) {
				outputFileVarName = runType.toLowerCase() + CONST_REPO_VAR_NAME_OUTPUT_FILE;
				emailRecipientsVarName = runType.toLowerCase() + CONST_REPO_VAR_NAME_EMAIL_RECIPIENTS;
			} else {
				throw new RuntimeException("Invalid value(" + runType + ") found for RunType parameter");
			}
			
			Logging.info("Running for RunType #" + runType);
			String emailService = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_EMAIL_SERVICE, "Mail");
			String internalBU = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_INTERNAL_BUNIT);
			String docStatus = this.constRepo.getStringValue(CONST_REPO_VAR_NAME_DOC_STATUS);
			String outputFileName =  this.constRepo.getStringValue(outputFileVarName, this.getClass().getName());
			String recipients = this.constRepo.getStringValue(emailRecipientsVarName);
			
			Logging.info("Const Repo Values: InternalBU #" + internalBU + ", DocStatus #" + docStatus + ", OutputFile #" + outputFileName 
					+ ", EmailRecipients #" + recipients);
			if (internalBU == null || internalBU.isEmpty() || docStatus == null || docStatus.isEmpty() 
					|| recipients == null || recipients.isEmpty()) {
				throw new RuntimeException("No value found for const repo variables - internalBU, docStatus, (daily/monthly)EmailRecipients");
			}
			
			tData = fetchChargesInApprovalReqd(internalBU, docStatus);
			int rows = tData.getNumRows();
			Logging.info("No. of CASH Tran Charges (with document in Approval Required) retrieved are #" + rows);
			
			if (rows == 0) {
				Logging.info("No documents (for CASH Tran Charges) found to be in Approval Required status.");
				return;
			}
			
			String outputFile = Util.reportGetDirForToday() + "\\" + outputFileName + "_" + System.currentTimeMillis() + ".xlsx";
			tData.excelSave(outputFile);
			Logging.info("Output file saved to the directory: " + outputFile);
			
			generateEmail(runType, recipients, outputFile, emailService);
			Logging.info("Email sent successfully");
			
		} catch (OException oe) {
			Logging.error("Exception occurred with message- " + oe.getMessage());
			throw oe;
			
		} finally {
			Logging.info("Plugin execution ends------");
			Logging.close();
			this.constRepo = null;
			if (Table.isTableValid(tData) == 1) {
				tData.destroy();
			}
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
		
		Logging.debug("Executing SQL query: " + sSQL);
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
	 * @param runType
	 * @param recipients
	 * @param attachment
	 * @throws OException
	 */
	protected void generateEmail(String runType, String recipients, String attachment, String emailService) throws OException {
		Logging.info("Sending email to: " + recipients + " with attachment: " + attachment);
		Utils.sendEmail(recipients, getEmailSubject(runType), getEmailBody(), attachment, emailService);
	}
	
	protected String getEmailSubject(String runType) {
		return "Charges in Approval Required document status - " + runType + " Email";
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
			sb.append("<br/>");
			sb.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			sb.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			
			return "PFA Cash charges which are pending approval. <br/><br/>"
					+ "Note: The cash charges will be deleted by an automated process if they not approved by the end of business hours on the last "
					+ "day of the month (before 6:30 PM). To approve, please query for these deals in the BO desktop and move the status "
					+ "from 'Approval Required' to 'Approved' status.<br/><br/>"
					+ sb.toString();
			
		} finally {
			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or constant repository.
	 */
	protected void initPluginLog() throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR");
		try {
			String logLevel = "INFO";
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir =  abOutDir + "\\error_logs";

			this.constRepo = new ConstRepository(CONTEXT, SUB_CONTEXT);
			
			logLevel = this.constRepo.getStringValue("logLevel", logLevel);
			logDir   = this.constRepo.getStringValue("logDir", logDir);
			
			Logging.init(this.getClass(), CONTEXT, SUB_CONTEXT);
		} catch (Exception e) {
			throw new OException("Error initialising logging: " + e.getMessage());
		}
	}

}
