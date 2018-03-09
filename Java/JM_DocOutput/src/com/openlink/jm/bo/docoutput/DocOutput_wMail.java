package com.openlink.jm.bo.docoutput;

import java.util.ArrayList;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.jm.bo.docoutput.DocOutput;
import com.openlink.jm.bo.docoutput.DocOutput_Base;
import com.openlink.jm.bo.docoutput.TokenHandler;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.mail.Mail;

class DocOutput_wMail extends DocOutput
{
	/**
	 * Sets the Send-Output-As-Mail-Attachment flag in the super class
	 */
	@Override
	boolean isSendMailRequested()
	{
		return true;
	}

	void sendMail(DocOutput_Base output) throws OException
	{
		try
		{
			MailParams mailParams = getMailParams();
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

			if (recipients.contains("%"))
				recipients = tryRetrieveSettingFromConstRepo("[EnhanceVars]", recipients, true);
			if (subject.contains("%"))
				subject = tryRetrieveSettingFromConstRepo("[EnhanceVars]", subject, true);
			if (message.contains("%"))
				message = tryRetrieveSettingFromConstRepo("[EnhanceVars]", message, true);
			if (sender.contains("%"))
				sender = tryRetrieveSettingFromConstRepo("[EnhanceVars]", sender, true);

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

			mail.send(recipientsArr, subject, message, sender, output.documentExportPath);
		}
		catch (Throwable t)
		{
			throw new OException(t);
		}
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
