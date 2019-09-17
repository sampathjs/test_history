package com.olf.jm.metalstransfer.report;

import java.io.File;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class AlertForQuoteTransfers implements IScript {
	private String bUnit;
	private String recipient;

	public void execute(IContainerContext arg0) throws OException {
		Utils.initialiseLog(Constants.ALERTQUOTES);
		Table reportQuoteTransfers = Util.NULL_TABLE;
		int internalBunit;
		EmailMessage mymessage = null;

		try {
			fetchTPMVariable();
			internalBunit = RefBase.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, bUnit);
			reportQuoteTransfers = fetchTransfersInQuote(internalBunit);
			// Check, If there are no records to publish
			if (reportQuoteTransfers.getNumRows() <= 0) {
				PluginLog.info("No Transfers were found in system for tran_status as 'Quotes'");
			} else {
				// Format report table
				format(reportQuoteTransfers);
				// Fetching recipient from User_const_repository
				String reciever = fetchReciepents();
				// Utility to fetch emailId against user name
				com.matthey.utilities.Utils.convertUserNamesToEmailList(reciever);
				// Creating Email Body to be published
				mymessage = createEmailMessage(reciever, reportQuoteTransfers);
			}
		} catch (OException e) {
			PluginLog.info("Error while sending email to users for Transfers pending in Quote Status" + e.getMessage());
			Util.exitFail();

		} finally {

			if (mymessage != null) {
				mymessage.dispose();
			}
			if (Table.isTableValid(reportQuoteTransfers) == 1) {
				reportQuoteTransfers.destroy();
			}
		}
	}

	private void format(Table reportQuoteTransfers) {
		try {
			reportQuoteTransfers.setColFormatAsDate("trade_date", DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			reportQuoteTransfers.setColFormatAsRef("internal_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
			reportQuoteTransfers.setColFormatAsRef("internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			reportQuoteTransfers.setColFormatAsRef("internal_contact", SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
			reportQuoteTransfers.setColFormatAsRef("tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		} catch (OException e) {
			PluginLog.error("Unable to format report data with reference tables");
			Util.exitFail();
		}

	}

	private void fetchTPMVariable() {
		long wflowId;
		try {
			wflowId = Tpm.getWorkflowId();
			PluginLog.info("Fetching TPM variables from workflowId " + wflowId);
			bUnit = getVariable(wflowId, "Int_Bunit");
			PluginLog.info("Generating report for " + bUnit);
		} catch (OException e) {
			PluginLog.info("Unable to fetch TPM variables" + e.getMessage());
			Util.exitFail();
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

			ConstRepository _constRepo = new ConstRepository("Strategy", "SapQuoteAlerts");
			this.recipient = _constRepo.getStringValue(region + "_QuoteReport");
			if (this.recipient == null || "".equals(this.recipient))
				throw new OException("Ivalid data to fetch from Const Repository");
		} catch (OException e) {
			PluginLog.error("Unable to fetch data from Const Repository" + e.getMessage());
		}
		PluginLog.info("mail recipient is " + recipient);

		return recipient;
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
				varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}

	private Table fetchTransfersInQuote(int internalBunit) throws OException {
		int sAPMTRNo = 20073;
		Table quoteTransfers = Util.NULL_TABLE;
		try {
			String sql = "select ab.deal_tracking_num,ab.tran_status,ab.internal_bunit,ab.internal_portfolio,ab.trade_date,ai.value as SAP_ID, ab.internal_contact, ab.last_update"
					+ " from ab_tran ab INNER JOIN ab_tran_info ai \n" 
					+ "ON ab.tran_num = ai.tran_num \n" 
					+ "WHERE ins_type =" + INS_TYPE_ENUM.strategy.toInt() + " \n"
					+ "AND ab.internal_bunit =" + internalBunit + "\n" 
					+ "AND ai.type_id = " + sAPMTRNo + "\n" 
					+ "AND ai.value != '' \n" 
					+ "AND ab.tran_status = "
					+ TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt() + " \n" 
					+ "AND ab.last_update >= DATEADD(DD,-1, Current_TimeStamp)";
			quoteTransfers = Table.tableNew();
			PluginLog.info("Executing sql to fetch Transfers in Quotes status \n " + sql);
			int ret = DBaseTable.execISql(quoteTransfers, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error("Failed to retrive quotes transfers, executing sql \n" + sql);
			}
		} catch (OException oe) {
			PluginLog.error("Unable to fetch deals from database, executing \n" + oe.getMessage());
			throw oe;
		}
		return quoteTransfers;

	}

	private EmailMessage createEmailMessage(String reciever, Table reportQuoteTransfers) {
		EmailMessage mymessage = null;
		String strFileName = "TransfersInQuotes";
		try {
			mymessage = EmailMessage.create();
			/* Add subject and recipients */
			String emailBodyMsg = "Attached Transfer deals are still in Quote status booked through SAP interface for " + OCalendar.formatDateInt(Util.getTradingDate())
					+ ". Can you please look into them and take appropriate actions if required";
			mymessage.addSubject("Transfer Deals in Quotes status for " + bUnit);
			mymessage.addRecipients(reciever);
			StringBuilder emailBody = new StringBuilder();
			String message = "<html> \n\r" + "<head><title> </title></head> \n\r" + "<p> Hi all,</p>\n\n" + "<p> " + emailBodyMsg + "</p>\n" + "<p>\n Thanks </p>"
					+ "<p>\n GRP Endur Support</p></body> \n\r" + "<html> \n\r";
			emailBody.append(message);
			emailBody.append("\n\r\n\r");
			mymessage.addBodyText(emailBody.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			String name = getFileName(strFileName);
			reportQuoteTransfers.printTableDumpToFile(name);
			/* Add attachment */
			if (new File(name).exists()) {
				PluginLog.info("File attachmenent found: " + name + ", attempting to attach to email..");
				mymessage.addAttachments(name, 0, null);
				int ret = mymessage.send("Mail"); // Send mail
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Unable to send mail"));
				} else {
					PluginLog.info("Mail has been send successfully");
				}
			} else {
				PluginLog.info("Unable to send the output email !!!");
				PluginLog.info("File attachmenent not found: " + name);
				Util.exitFail();
			}

		} catch (OException e) {
			PluginLog.info("Unable to create mail body" + e.getMessage());

		}
		return mymessage;
	}

	private String getFileName(String strFileName) {

		String strFilename;
		StringBuilder fileName = new StringBuilder();

		try {

			ODateTime.getServerCurrentDateTime().toString().split(" ");
			Ref.getInfo();
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append(strFileName);
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append(".csv");
		} catch (OException e) {
			PluginLog.info("Unable to format name of  Report for the day");
		}
		strFilename = fileName.toString();

		return strFilename;
	}

}
