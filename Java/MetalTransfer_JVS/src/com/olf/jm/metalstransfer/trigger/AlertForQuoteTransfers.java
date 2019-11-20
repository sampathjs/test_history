package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class AlertForQuoteTransfers implements IScript {
	private ConstRepository constRep;
	private static final String strFileName = "TransfersInQuotes";
	private static final String CONST_REPOSITORY_CONTEXT = "SapQuoteAlerts";
	private String bUnit;
	
	public void execute(IContainerContext arg0) throws OException {
		Utils.initialiseLog(Constants.ALERTQUOTES);	
		Table reportQuoteTransfers = Util.NULL_TABLE;
		
		String mailServiceName = "Mail";
		
		try {
			fetchTPMVariable();
			int internalBunit = RefBase.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, bUnit);
			reportQuoteTransfers = fetchTransfersInQuote(internalBunit);
			int count = reportQuoteTransfers.getNumRows();
			// Check, If there are no records to publish
			if (count <= 0) {
				PluginLog.info("No Transfers were found in system for tran_status as 'Quotes'");
			} else {
				
				PluginLog.info("Fetching recipient from User_const_repository");
				String reciever = fetchReciepents();
				// Utility to fetch emailId against user name
				String emailId = com.matthey.utilities.Utils.convertUserNamesToEmailList(reciever);
				// Creating Email Body to be published
				String message = getEmailBody();
				String subject = getEmailSubject();
			 	eMailBody(reportQuoteTransfers);
				String fileToAttach = com.matthey.utilities.FileUtils.getFilePath(strFileName);
				reportQuoteTransfers.printTableDumpToFile(fileToAttach);
				boolean ret = com.matthey.utilities.Utils.sendEmail(emailId, subject, message, fileToAttach, mailServiceName);
				if (!ret) {
					PluginLog.error("Failed to send alert for Transfers in quotes status \n");
				}PluginLog.info("Mail is successfully sent to "+ emailId +" and report contains "+count+" strategy deals of "+bUnit);
			}
		} catch (OException e) {
			PluginLog.error("Error while sending email to users for Transfers pending in Quote Status for BU " +bUnit + ". \n"+ e.getMessage());
			Util.exitFail();

		} finally {

			if (Table.isTableValid(reportQuoteTransfers) == 1) {
				reportQuoteTransfers.destroy();
			}
		}
	}

	private String getEmailBody() throws OException {
		 String body = "Attached Transfer deals are still in Quote status booked through SAP interface for " 
					+ OCalendar.formatDateInt(Util.getTradingDate())
					+ ". Can you please look into them and take appropriate actions if required";
		 
		 return "<html> \n\r"
					+ "<head><title> </title></head> \n\r"
					+ "<p> Hi all,</p>\n\n" 
					+ "<p> " + body + "</p>\n"
					+ "<p>\n Thanks </p>"
					+ "<p>\n GRP Endur Support</p></body> \n\r" 
					+ "<html> \n\r";
	}

	private String getEmailSubject() {
		return  "Transfer Deals in Quotes status for " + bUnit;
		
	}

	private void eMailBody( Table reportQuoteTransfers) throws OException {
		 
		try{		
		PluginLog.info("Format report data");
		reportQuoteTransfers.setColFormatAsDate("trade_date",    DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		reportQuoteTransfers.setColFormatAsRef("internal_bunit",     SHM_USR_TABLES_ENUM.PARTY_TABLE);
		reportQuoteTransfers.setColFormatAsRef("internal_portfolio",     SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		reportQuoteTransfers.setColFormatAsRef("tran_status",     SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		
	} catch (OException e) {
		PluginLog.error("Unable to format report data with reference tables"+e.getMessage());
		throw e;
	}
	}

	private void fetchTPMVariable() throws OException {
		long wflowId;
		try {
			wflowId = Tpm.getWorkflowId();
			PluginLog.info("Fetching TPM variables from workflowId " + wflowId);
			bUnit = com.matthey.utilities.TpmUtils.getTpmVariableValue(wflowId, "Int_Bunit");
			PluginLog.info("Generating report for "+bUnit);
		} catch (OException e) {
			PluginLog.info("Unable to fetch TPM variables" + e.getMessage());
			throw e;
		}
	}

	private String fetchReciepents() throws OException {
		String recipient = null;
		try {
			String region = com.matthey.utilities.Utils.getRegion(bUnit);
			String CONST_REPOSITORY_SUBCONTEXT = region;
			constRep = new ConstRepository(CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
			recipient = constRep.getStringValue("recipients");
			if (recipient == null || "".equals(recipient))
				throw new OException("Ivalid data to fetch from Const Repository");
		} catch (OException e) {
			PluginLog.error("Unable to fetch data from Const Repository" + e.getMessage());
			throw e;
		}
		PluginLog.info("mail recipient is " + recipient);

		return recipient;
	}

	

	private Table fetchTransfersInQuote(int internalBunit) throws OException {
		Table quoteTransfers = Util.NULL_TABLE;
		int type_id = 20073;
		try {
			String sql = "SELECT ab.deal_tracking_num,ab.tran_status,ab.internal_bunit,ab.internal_portfolio,ab.trade_date,ab.last_update,ai.value as SAP_ID \n"
					+ "FROM ab_tran ab \n"
					+"INNER JOIN ab_tran_info ai \n"
					+"ON ab.tran_num = ai.tran_num \n"
					+ "WHERE ab.ins_type =" + INS_TYPE_ENUM.strategy.toInt() + " \n"
						+ "AND ab.tran_status = "+ TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt() + " \n" 
						+ "AND ab.last_update >= DATEADD(DD,-1, Current_TimeStamp)\n"
						+ "AND ab.internal_bunit = "+internalBunit+"\n"
						+ "AND ai.type_id ="+type_id;
			quoteTransfers = Table.tableNew();
			PluginLog.info("Executing sql to fetch Transfers in Quotes status \n "+sql);
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
	
}