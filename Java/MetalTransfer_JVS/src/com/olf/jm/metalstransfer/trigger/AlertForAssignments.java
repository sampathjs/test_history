package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class AlertForAssignments extends MetalTransferTriggerScript {
	private String tranNum;
	private String BalanceThreshold;
	private String TransferQty;
	private String FromAccountBalance;
	private String FromAccountName;
	private String bUnit;
	private String userName;
	private String endurUserName;
	private String recipient;

	public AlertForAssignments() throws OException {
		super();

	}

	@Override
	public void execute(IContainerContext context) throws OException {
		Utils.initialiseLog(Constants.ALERTMAILLOG);
		PluginLog.info("Attempting to send email (using configured Mail Service)..");
		String mailRecipient;
		EmailMessage mymessage = null;
		try {

			// Fetch all the TPM variables required for mail
			PluginLog.info("Fetch recipients for mail from User_const_reporsitory");
			fetchTPMVariable();
			String userName = fetchReciepents();// Fetch recipient from
												// user_const_repository
			if (userName.equalsIgnoreCase("Submitter")) {
				mailRecipient = endurUserName;
			} else {
				mailRecipient = userName;
			}
			PluginLog.info("Fetch recipients Email Id for .." + bUnit + " in User_const_reporsitory with context as Strategy and subContext as AssignmentAlerts");
			String emailID = com.matthey.utilities.Utils.convertUserNamesToEmailList(mailRecipient);
			PluginLog.info("Preparing Email Body");
			mymessage = createEmailMessage(emailID); // create mail body in HTML
														// format
			PluginLog.info("Sending email to " + emailID);
			int ret = mymessage.send("Mail"); // Send mail
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Unable to send mail"));
			}
			PluginLog.info("Email sent to: " + emailID);
		} catch (OException e) {
			PluginLog.info("Error while sending email to users" + e.getMessage());
			Util.exitFail();

		} finally {

			if (mymessage != null) {
				mymessage.dispose();
			}
		}
	}

	private String fetchReciepents() {
		String region = null;
		try {
			switch (bUnit) {
			case "JM PMM UK":
			case "JM PMM LTD":
				region = "UK";
				break;
			case "JM PMM US":
				region = "US";
				break;
			case "JM PMM HK":
				region = "HK";
				break;
			case "JM PMM CN":
				region = "CN";
				break;
			}

			ConstRepository _constRepo = new ConstRepository("Strategy", "AssignmentAlerts");
			this.recipient = _constRepo.getStringValue(region + "_Recipient");
			if (this.recipient == null || "".equals(this.recipient))
				throw new OException("Ivalid TPM defination in Const Repository");
		} catch (OException e) {
			e.getMessage();
		}
		PluginLog.info("mail recipient is " + recipient);

		return recipient;
	}

	private EmailMessage createEmailMessage(String email) {
		EmailMessage mymessage = null;
		try {
			mymessage = EmailMessage.create();
			/* Add subject and recipients */
			mymessage.addSubject("Metal Transfer Deal " + tranNum + " Falls Below Balance Threshold");
			mymessage.addRecipients(email);
			StringBuilder emailBody = new StringBuilder();
			String emailBodyMsg = "<html> \n\r" + "<head><title> Strategy " + tranNum + " reported in assignment.</title></head> \n\r" + "<p> Hi,</p>\n\n"
					+ "<p> The account balance for account " + FromAccountName + " will fall below the balance threshold of " + BalanceThreshold + " .</p>\n"
					+ "<p>If the Metal Transfer Strategy deal " + tranNum + " is booked by " + userName + " with a quantity of " + TransferQty + " </p> \n"
					+ "<p>The current account balance is " + FromAccountBalance + "</p> </body> \n\r" + "<html> \n\r";

			String html = emailBodyMsg.toString();
			emailBody.append(html);
			emailBody.append("\n\r\n\r");
			emailBody.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			// emailBody.append(",business date: " +
			// OCalendar.formatDateInt(Util.getBusinessDate()));
			emailBody.append("\n\r\n\r");
			mymessage.addBodyText(emailBody.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
		} catch (OException e) {
			PluginLog.info("Unable to create mail body" + e.getMessage());
			e.printStackTrace();
		}
		return mymessage;
	}

	protected void fetchTPMVariable() throws OException {

		long wflowId;
		try {
			wflowId = Tpm.getWorkflowId();
			PluginLog.info("Fetching TPM variables from workflowId " + wflowId);
			tranNum = getVariable(wflowId, "TranNum");
			BalanceThreshold = getVariable(wflowId, "BalanceThreshold");
			TransferQty = getVariable(wflowId, "TransferQty");
			FromAccountBalance = getVariable(wflowId, "FromAccountBalance");
			FromAccountName = getVariable(wflowId, "FromAccountName");
			bUnit = getVariable(wflowId, "bUnit");
			userName = getVariable(wflowId, "userName");
			endurUserName = getVariable(wflowId, "name");
		} catch (OException e) {
			PluginLog.info("Unable to fetch TPM variables" + e.getMessage());
			e.printStackTrace();
		}

	}

	private String getVariable(final long wflowId, final String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable = Util.NULL_TABLE;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			if (Table.isTableValid(varsAsTable) == 1 || varsAsTable.getNumRows() > 0) {
				com.olf.openjvs.Table varSub = varsAsTable.getTable("variable", 1);
				for (int row = varSub.getNumRows(); row >= 1; row--) {
					String name = varSub.getString("name", row).trim();
					String value = varSub.getString("value", row).trim();
					if (toLookFor.equals(name)) {
						return value;
					}
				}
			}
		} finally {
			if (Table.isTableValid(varsAsTable) == 1) {
				// Possible engine crash destroying table - commenting out Jira 1336
				// varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}

}
