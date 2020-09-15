package com.jm.eod.deletequotes;

import java.io.File;

import com.jm.exception.SapExtensionsRuntimeException;
import com.jm.utils.Constants;
import com.jm.utils.SapExtensionsConfig;
import com.jm.utils.Util;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Driven by a Trading Manager query, this plugin moves "Quote" status
 * trades to "Deleted" - this indicates the quote has expired.
 */
public class DeleteOpenQuotes implements IScript 
{
	private boolean processErrorsOcured = false;
	
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		Util.initialiseLog(Constants.LOG_OPEN_QUOTE_DELETION);
		
		Table tblArgt = context.getArgumentsTable();
		
		int queryId = -1;
		Table tblData = null;
		
		try
		{
			if (tblArgt.getNumRows() != 1)
			{
				throw new SapExtensionsRuntimeException("Delete Open Quotes - argt should have one row as an input!");
			}
			
			String queryName = tblArgt.getString("query_name", 1);
			Logging.info("Querying: " + queryName);
			queryId = Query.run(queryName);
			
			tblData = Table.tableNew("Quote trades");
			
			String sqlQuery = 
				"SELECT \n" +
					"ab.tran_num, \n" +
					"ab.version_number \n" + 
				"FROM " + Query.getResultTableForId(queryId) + " qr \n" +
				"JOIN ab_tran ab ON qr.query_result = ab.tran_num \n" + 
				"WHERE qr.unique_id = " + queryId + " \n" +
				"AND ab.current_flag = 1 \n" +
				"AND ab.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt() + "\n" +
				"AND ab.trade_date = '" + OCalendar.formatJdForDbAccess(OCalendar.today()) + "'";
		
			int ret = DBaseTable.execISql(tblData, sqlQuery);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapExtensionsRuntimeException("Unable to run query: " + sqlQuery);
			}
			
			if (tblData.getNumRows() > 0)
			{
				Logging.info("Found: " + tblData.getNumRows() + " deals to delete..");
				
				deleteTrades(tblData);
				
				if (processErrorsOcured)
				{
					String fileName = com.olf.openjvs.Util.reportGetDirForToday() + "\\" + Constants.LOG_OPEN_QUOTE_DELETION;
				
					sendEmail(fileName);
				}
			}
		}
		catch (Exception e)
		{
			throw new SapExtensionsRuntimeException("Problem encountered during DeleteOpenQuotes!", e);
		}
		finally
		{
			Logging.close();
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
			
			if (tblData != null)
			{
				tblData.destroy();
			}
		}
	}

	/**
	 * Given a set of trades in Quote status, call the Transaction.delete API on each one
	 * 
	 * @param tblData
	 * @throws OException
	 */
	private void deleteTrades(Table tblData) throws OException
	{
		int numRows = tblData.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
			int tranNum = tblData.getInt("tran_num", row);
			int versionNumber = tblData.getInt("version_number", row);
		
			try
			{
				Transaction.delete(tranNum, versionNumber);

				Logging.info("Moved tran_num: " + tranNum + " from Quote to Deleted..");	
			}
			catch (Exception e)
			{
				/* Track the error, but don't throw an exception as the log will be emailed on failiures */
				String error = "Unable to move tran_num: " + tranNum + " to status Deleted.." + e.getMessage();
				
				Logging.info(error);
				Logging.error(error);
				
				processErrorsOcured = true;
			}
		}
	}
	
	/**
	 * Send out an email to users in case of any quote failiures
	 * 
	 * @param attachmentPath
	 * @throws OException 
	 */
	private void sendEmail(String attachmentPath) throws OException
	{
		Logging.info("Attempting to send summary email (using configured Mail Service)..");
		
		Table tblInfo = null;
		EmailMessage mymessage = null;
		
		try
		{
			SapExtensionsConfig constRepoConfig = new SapExtensionsConfig(SapExtensionsConfig.CONST_REP_SUBCONTEXT_DELETEQUOTES);
			String recipients = constRepoConfig.getValue(Constants.DELETE_QUOTES_EMAIL_RECIPIENTS);
			
			if (recipients == null || "".equalsIgnoreCase(recipients))
			{
				throw new SapExtensionsRuntimeException("No recipient users found for emailing errors to!");
			}
			
			mymessage = EmailMessage.create();
			
			/* Add subject and recipients */
			mymessage.addSubject("Endur - Delete open quotes - error occured");
			mymessage.addRecipients(recipients);
			
			StringBuilder builder = new StringBuilder();
			builder.append("This is an automated email message - please do not respond to this email.");
			builder.append("\n\n");
			
			/* Add environment details */
			tblInfo = com.olf.openjvs.Ref.getInfo();
			if (tblInfo != null)
			{
				builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
				builder.append(", on server: " + tblInfo.getString("server", 1));
				
				builder.append("\n\n");
			}
			
			builder.append("Endur trading date: " + OCalendar.formatDateInt(com.olf.openjvs.Util.getTradingDate()));
			builder.append(", business date: " + OCalendar.formatDateInt(com.olf.openjvs.Util.getBusinessDate()));
			builder.append("\n\n");
			
			builder.append("Error occured during deletion of open quotes. See attached log file.");
			builder.append("\n\n");
			
			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			
			/* Add attachment */
			if (new File(attachmentPath).exists())
			{
				Logging.info("File attachmenent found: " + attachmentPath + ", attempting to attach to email..");
				mymessage.addAttachments(attachmentPath, 0, null);	
			}
			else
			{
				Logging.info("No file found under: " + attachmentPath);
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			Logging.info("Email sent to: " + recipients);
		}
		catch (Exception e)
		{
			throw new SapExtensionsRuntimeException("Unable to send Delte Open Quotes failiure email!", e);
		}
		finally
		{
			if (tblInfo != null)
			{
				tblInfo.destroy();	
			}
			
			if (mymessage != null)
			{
				mymessage.dispose();
			}
		}
	}
}
