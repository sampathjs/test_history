package com.openlink.jm.bo.docoutput;

import java.util.ArrayList;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.mail.Mail;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
*
* 2020-01-10	V1.1	-	Pramod Garg - Insert the erroneous entry in USER_jm_auto_doc_email_errors table 
* 										   if failed to make connection to mail server
* 2020-03-25	V1.2	YadavP03	- memory leaks, remove console print & formatting changes
**/

class DocOutput_wMail extends DocOutput
{
	/**
	 * Sets the Send-Output-As-Mail-Attachment flag in the super class
	 */
	private static int retryCount = 3;
	@Override
	boolean isSendMailRequested()
	{
		return true;
	}

	void sendMail(DocOutput_Base output) throws OException
	{
		Table tblProcessData = Util.NULL_TABLE;
		try
		{
			MailParams mailParams = getMailParams();
			int retryTimeoutCount = 0;
			boolean success = false;
			String mailErrorMessage = "";
			if (mailParams == null)
				throw new NullPointerException("MailParams");
			PluginLog.debug("Mail Parameters - "+mailParams.toString());

			waitForFile(output.documentExportPath);

			String  recipients = mailParams.recipients,
					subject    = mailParams.subject, 
					message    = mailParams.message, 
					sender     = mailParams.sender;

			TokenHandler token = new TokenHandler();
			token.createDateTimeMap();

			recipients = token.replaceTokens(recipients, argt.getTable("process_data", 1).getTable("user_data", 1), token.getDateTimeTokenMap(), "Recipients");
			subject    = token.replaceTokens(subject, argt.getTable("process_data", 1).getTable("user_data", 1), token.getDateTimeTokenMap(), "Subject");
			message    = token.replaceTokens(message, argt.getTable("process_data", 1).getTable("user_data", 1), token.getDateTimeTokenMap(), "Message");
			sender     = token.replaceTokens(sender, argt.getTable("process_data", 1).getTable("user_data", 1), token.getDateTimeTokenMap(), "Sender");
			tblProcessData = argt.getTable("process_data", 1);
			
					
			if (recipients.contains("%"))
				recipients = tryRetrieveSettingFromConstRepo("[EnhanceVars]", recipients, true);
			if (subject.contains("%"))
				subject = tryRetrieveSettingFromConstRepo("[EnhanceVars]", subject, true);
			if (message.contains("%"))
				message = tryRetrieveSettingFromConstRepo("[EnhanceVars]", message, true);
			if (sender.contains("%"))
				sender = tryRetrieveSettingFromConstRepo("[EnhanceVars]", sender, true);

			String intBU = token.getUserData(argt.getTable("process_data", 1).getTable("user_data", 1), "olfIntBUShortName");
			if ("JM PMM US".equals(intBU)) {
				String doNotReplyText = tryRetrieveSettingFromConstRepo("Do_Not_Reply_Email_Message_Text_US", "", false);
				message = (message.indexOf("<DoNotReplyText>") > -1) ? message.replace("<DoNotReplyText>", doNotReplyText) : message;
			}
			
			String[] recipientsArr = recipients.trim().replaceAll("\\s*,\\s*", ",").split(",");
			ArrayList<String> list = new ArrayList<String>();
			for (String r:recipientsArr)
				if (r != null && r.length() > 0)
					if (!list.contains(r))
						list.add(r);
			recipientsArr = new String[list.size()];
			for (int i = list.size(); --i >= 0;)
				recipientsArr[i] = list.get(i);

			Mail mail = new Mail(mailParams.smtpServer);
			/*
			mail.send(mailParams.recipients, 
					  mailParams.subject, 
					  mailParams.message, 
					  mailParams.sender, 
					  output.documentExportPath);
			 */
			
			while (retryTimeoutCount<retryCount) {
				try {
					mail.send(recipientsArr, subject, message, sender, output.documentExportPath);
					success = true;
					break;
				}
				
				catch (OException ex) {
					retryTimeoutCount++;
					mailErrorMessage = ex.getMessage();
					Thread.sleep(1000);
				}
				
			}
			
			if (!success)
			{
				//The attempts to connect to the smtp server failed.
				String erroMessage = "Failed to make the connection to the smtp server " +mailErrorMessage;
				String deals = loadDealsForDocument(tblProcessData.getInt("document_num", 1));
				UpdateErrorInUserTable.insertErrorRecord(tblProcessData, deals,erroMessage );
				PluginLog.error(erroMessage);
				throw new OException (erroMessage);
				
			}
			
			//mail.send(recipientsArr, subject, message, sender, output.documentExportPath);
		}
		catch (Throwable t)
		{
			throw new OException(t);
		}
	}

	private String loadDealsForDocument(int docNum) throws OException {
		String sql = String.format(
				"\nSELECT distinct d.deal_tracking_num "
			+   "\nFROM stldoc_details_hist d"
			+   "\nWHERE d.document_num = %d",
				docNum);
		Table sqlResult = null;			
		sqlResult = Table.tableNew("Deal nums for document");
		int ret = DBaseTable.execISql(sqlResult, sql);
		
		if (ret != OLF_RETURN_SUCCEED) {
			sqlResult = TableUtilities.destroy(sqlResult);
			throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql));
		} 
		
		StringBuilder dealNumbers = new StringBuilder();
		
		for (int dealRow=sqlResult.getNumRows(); dealRow >0; dealRow--) {
			int dealNum = sqlResult.getInt("deal_tracking_num", dealRow);
			dealNumbers.append(dealNum).append(",");
		}
		if (dealNumbers.length()>1){
			dealNumbers.deleteCharAt(dealNumbers.length()-1);
		}
		sqlResult.destroy();
		return dealNumbers.toString();
	}

	private MailParams getMailParams() throws OException
	{
		Table outputData = argt.getTable(PROCESS_DATA, 1).getTable(OUTPUT_DATA, 1);
		String recipients = null, sender = null, subject = null, message = null, test;

		int nameCol  = outputData.getColNum(OUTPUT_PARAM_NAME),
			valueCol = outputData.getColNum(OUTPUT_PARAM_VALUE);
		for (int row = outputData.getNumRows(); row > 0; --row)
		{
			if (!(test = outputData.getString(nameCol, row)).contains("Mail"))
				continue;

			if (test.contains("Mail Recipients"))
				recipients = outputData.getString(valueCol, row);
			else if (test.contains("Mail Sender"))
				sender = outputData.getString(valueCol, row);
			else if (test.contains("Mail Subject"))
				subject = outputData.getString(valueCol, row);
			else if (test.contains("Mail Message"))
				message = outputData.getString(valueCol, row);
		}

		return new MailParams(recipients, sender, subject, message);
	}

	private class MailParams
	{
		String recipients = null;
		String sender = null, subject = null, message = null, smtpServer = null;

		// prevent from invalid instantiation
		@SuppressWarnings("unused")
		private MailParams() {}

		public MailParams(String recipients, String sender, String subject, String message) throws OException
		{
			smtpServer = tryRetrieveSettingFromConstRepo(VAR_SMTP_SERVER, "", false);

			try
			{
				if (recipients == null) throw new NullPointerException("'recipients' shall not be null");
				this.recipients = recipients;
				if (sender == null) throw new NullPointerException("'sender' shall not be null");
				this.sender = sender;
				if (subject == null) throw new NullPointerException("'subject' shall not be null");
				this.subject = subject;
				if (message == null) throw new NullPointerException("'message' shall not be null");
				this.message = message;
			}
			catch (Throwable t) { throw new OException(t); }
		}

		public String toString()
		{
			String s = getClass().getSimpleName()+" - raw values";
			s += "\n\tRecipients - "+recipients;
			s += "\n\tSender     - "+sender;
			s += "\n\tSubject    - "+subject;
			s += "\n\tMessage    - "+message;
			s += "\n\tSmtpServer - "+smtpServer;

			return s;
		}
	}
}
