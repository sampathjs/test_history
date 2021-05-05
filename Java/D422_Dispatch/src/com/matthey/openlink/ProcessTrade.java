package com.matthey.openlink;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

/*
 * History:
 * 2021-05-22	V1.0	murthv01 - EPI-1700 | Added code to allow cancellation of unapproved validated dispatch deals in In-progress status
 */
@ScriptCategory({EnumScriptCategory.TpmStep})
public class ProcessTrade extends AbstractProcessStep {

	private ConstRepository repo = null;
	private String repoContext = null;
	private String repoSubContext = null;
	
	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		
		String message = null;
		int sendEmail = 0;
		int retryCount = 0;
		int maxRetryCount = 1;
		
		int tranNum = process.getVariable("TranNum").getValueAsInt();
		int iTargetStatus = Integer.parseInt(process.getVariable("TargetStatus").getValueAsString());
		EnumTranStatus targetStatus = EnumTranStatus.retrieve(iTargetStatus);
		
		this.repoContext = process.getVariable("Context").getValueAsString();
		this.repoSubContext = process.getVariable("SubContext").getValueAsString();
		
		try {
			init();
			
			if (process.getVariable("MaxRetry") == null || "".equals(process.getVariable("MaxRetry").getValueAsString())) {
				message = "Missing TPM variable - MaxRetry";
				Logging.error(message);
				process.appendError(message);
				throw new Exception(message);
			} else {
				maxRetryCount = process.getVariable("MaxRetry").getValueAsInt();
			}
			
			if (process.getVariable("SendEmail") == null || "".equals(process.getVariable("SendEmail").getValueAsString())) {
				message = "Missing TPM variable - SendEmail";
				Logging.error(message);
				process.appendError(message);
				throw new Exception(message);
			} else {
				sendEmail = process.getVariable("SendEmail").getValueAsInt();
			}
			
			while (++retryCount <= maxRetryCount) {
				try {
					processTrade(context, tranNum, targetStatus);
					break;
				} catch(Exception ex) {
					Logging.error(String.format("Failed to process tran#%d to %s status during retry#%d : %s", tranNum, targetStatus.getName(), retryCount, ex.getMessage()));
					if (retryCount == maxRetryCount) {
						throw ex;
					}
				}
			}
			
		} catch (Exception e) {
			Logging.error(String.format("ProcessTrade step (%s workflow) failed for tran#%d. Message - %s", repoContext, tranNum, e.getMessage()));
			message = e.getMessage();
			process.appendError(message);
			
			if (sendEmail == 1) {
				sendEmail(tranNum, submitter.getEmailAddress(), targetStatus.getName(), message);
			}
			Util.exitFail();
        }finally{
        	Logging.close();
        }
		
		return null;
	}
	
	private void processTrade(Context context, int tranNum, EnumTranStatus targetStatus) throws Exception {
		TradingFactory tf = context.getTradingFactory();
		try (Transaction tran = tf.retrieveTransactionById(tranNum)) {
			EnumTranStatus currStatus = tran.getTransactionStatus();
			Logging.info(String.format("Input tran #%d, version #%d, Current Status-%s", tranNum, tran.getVersionNumber(), currStatus.getName()));
			
			if (currStatus == EnumTranStatus.Validated && currStatus == targetStatus) {
				// For Dispatch Deals Incrementally save transaction to trigger 'Assign: Settlement Instruction' 
				// Ops Service so that Settlement instructions are always updated only if it is approved in TPM
				// This has to be done only in Dispatch Status 'Awaiting Shipping' and 'Left Site'
				checkAndIncrementallySaveForDispatchDeals(tran);
			} else if (currStatus.getValue() == targetStatus.getValue()) {
				Logging.info(String.format("Not processing further as target status & current status are same (%s)", targetStatus.getName()));
			} else {
				if (EnumTranStatus.Validated == targetStatus || EnumTranStatus.Deleted == targetStatus || EnumTranStatus.Cancelled == targetStatus) {
					Logging.info(String.format("Called from %s Trade step..", targetStatus.getName()));
					checkCurrStatusAndProcess(tran, targetStatus, currStatus);
				} else {
					Logging.info(String.format("Not processing further as target status - %s", targetStatus.getName()));	
				}
			}
			
		} catch(Exception e) {
			throw e;
		}
	}

	private void checkAndIncrementallySaveForDispatchDeals(Transaction tran) {

		Field dispatchStatusField = tran.getField("Dispatch Status");
		if (dispatchStatusField != null && dispatchStatusField.isApplicable() && dispatchStatusField.isReadable()) {
			String dispatchStatus = dispatchStatusField.getValueAsString();
			if ("Awaiting Shipping".equalsIgnoreCase(dispatchStatus) || "Left Site".equalsIgnoreCase(dispatchStatus)) {
				Logging.info("Incrementally saving deal to trigger 'Assign: Settlement Instruction' Ops Service for "
						+ "Dispatch Status %s", dispatchStatus);
				tran.saveIncremental();
			}
		}
	}

	private void checkCurrStatusAndProcess(Transaction tran, EnumTranStatus targetStatus, EnumTranStatus currStatus) throws Exception {
		int tranNum = tran.getTransactionId();
		
		Logging.info(String.format("Processing tran#%d to %s status", tranNum, targetStatus.getName()));
		tran.process(targetStatus);
		
		Logging.info(String.format("Successfully processed tran#%d to %s status", tranNum, targetStatus.getName()));
	}
	
	private void sendEmail(int tranNum, String submitterEmail, String targetStatus, String errorMessage) {
		Logging.info("Attempting to send email (using configured Mail Service)..");
		com.olf.openjvs.Table envInfo = Util.NULL_TABLE;

		try {
			StringBuilder sbRecipients = new StringBuilder();
			sbRecipients.append(submitterEmail);
			
			String emailRecipients = this.repo.getStringValue("email_recipients");
			if (!emailRecipients.isEmpty() && !"".equals(emailRecipients)) {
				sbRecipients.append(";");
				sbRecipients.append(emailRecipients);
			}
			
			EmailMessage mymessage = EmailMessage.create();
			mymessage.addSubject(String.format("Dispatch workflow - Failed to process trade (with tran#%d) to %s status", tranNum, targetStatus));
			mymessage.addRecipients(sbRecipients.toString());

			StringBuilder builder = new StringBuilder();
			builder.append(String.format("TPM Dispatch workflow failed to process trade (tran #%d) to %s status, ErrorMessage - %s", tranNum, targetStatus, errorMessage));
			
			envInfo = com.olf.openjvs.Ref.getInfo();
			if (envInfo != null) {
				builder.append("This information has been generated from database: " + envInfo.getString("database", 1));
				builder.append(", on server: " + envInfo.getString("server", 1));
				builder.append("\n\n");
			}

			builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			builder.append("\n\n");

			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			mymessage.send("Mail");
			mymessage.dispose();

			Logging.info(String.format("Email successfully sent to %s", sbRecipients.toString()));
			
		} catch (Exception e) {
			Logging.error(String.format("Unable to send output email. Error - %s", e.getMessage()));
			
		} finally {
			try {
				if (com.olf.openjvs.Table.isTableValid(envInfo) == 1) {
					envInfo.destroy();
				}
			} catch (OException e) {
				Logging.error(String.format("Error while destroying envInfo table object, message-%s", e.getMessage()));
			}
		}
	}
	
	/**
	 * Initialise logging 
	 * @throws Exception 
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		this.repo = new ConstRepository(this.repoContext, this.repoSubContext);
		
		String logLevel = "INFO";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = this.repo.getStringValue("logLevel", logLevel);
			logFile = this.repo.getStringValue("logFile", logFile);
			logDir = this.repo.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), this.repoContext, this.repoSubContext);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}

}

