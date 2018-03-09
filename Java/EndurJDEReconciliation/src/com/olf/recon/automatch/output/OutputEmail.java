package com.olf.recon.automatch.output;

import java.io.File;

import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.ReconConfig;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_AUTOMATCH_ACTIONS)
public class OutputEmail implements IScript 
{
	private Table tblArgt;
	
	/* By setting this to true - break notes will be removed from the email output */
	private boolean ignoreBreakNotes = false;
	
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		tblArgt = context.getArgumentsTable();
		Table tblData = null;
		
		ignoreBreakNotes = canIgnoreBreakNotes();
		
		try
		{
			/* This parameter specifies the prefix name of the file - configured in the Auto Match Definition builder */
			String fileNameParam = tblArgt.getString("amr_action_param1", 1);
			if (fileNameParam.equalsIgnoreCase(""))
			{
				throw new ReconciliationRuntimeException("Please specify 'amr_action_param1' in the Auto Match config - this is the filename");
			}
			
			String absoluteFilePath = getFilePath(fileNameParam);
			
			Table tblActionData = tblArgt.getTable("amr_action_data", 1);
			
			/* Remove columns that we don't need for the output data */
			tblData = tblActionData.copyTable();
			if (tblData.getColNum("_pdata") > 0) tblData.delCol("_pdata");
			if (tblData.getColNum("_cdata") > 0) tblData.delCol("_cdata");

			/* Sometimes the first column shows a zero value - looks like a core bug but in the Auto Match output (needs more analysis) - ignore for now */
			tblData.deleteWhereValue(1, 0);

			/* If ignore break notes then the email output should remove these from the csv file */
			if (ignoreBreakNotes)
			{				
				for (int row = tblData.getNumRows(); row >= 1; row--)
				{
					String reconciliationNote = tblData.getString("reconciliation_note", row);
					
					if (reconciliationNote != null)
					{
						if (reconciliationNote.length() > 1)
						{
							/* 
							 * Remove the row, as it contains a break note (meaning the issue is known) and we aren't interested
							 * in reporting it 
							 */
							tblData.delRow(row);
						}
					}
				}
			}

			/* Dump file */
			tblData.printTableDumpToFile(absoluteFilePath);
			PluginLog.info("Reconciliation data dumped to: " + absoluteFilePath);
			
			/* Any records that have a break note can be ignored as these are known defects */
			int numUnmatchedRows = tblData.getNumRows();
			int breaksCanIgnore = 0;
			for (int row = 1; row <= numUnmatchedRows; row++)
			{
				String breakNote = tblData.getString("reconciliation_note", row);
				
				if (breakNote != null && breakNote.length() > 1)
				{
					breaksCanIgnore++;
				}
			}
			
			sendEmail(absoluteFilePath, fileNameParam, numUnmatchedRows, breaksCanIgnore);
		}
		finally
		{
			if (tblData != null)
			{
				tblData.destroy();
			}
		}
	}
	
	/**
	 * Check the const repo for the parameter that indicates if we can ignore break notes or not
	 * 
	 * @return
	 * @throws OException
	 */
	private boolean canIgnoreBreakNotes() throws OException
	{
		String region = getRegion();
		ReconConfig constRepoConfig = new ReconConfig(region);

		try
		{
			String ignoreBreakNotesStr = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_IGNORE_BREAK_NOTES_IN_EMAIL);
			if (ignoreBreakNotesStr != null && ignoreBreakNotesStr.length() > 1)
			{
				ignoreBreakNotesStr = ignoreBreakNotesStr.trim();
			}
			
			if ("yes".equalsIgnoreCase(ignoreBreakNotesStr))
			{
				return true;
			}	
		}
		catch (Exception e)
		{
			PluginLog.info("Missing parameter " + Constants.CONST_REPO_VARIABLE_IGNORE_BREAK_NOTES_IN_EMAIL + " for " + getRegion());
		}
		
		/* Default case */
		return ignoreBreakNotes;
	}
	
	/**
	 * Construct the file path
	 * 
	 * @param fileNameParam
	 * @return
	 * @throws OException 
	 */
	private String getFilePath(String fileNameParam) throws OException
	{
		StringBuilder fileName = new StringBuilder();
		
		String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
		String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
		
		fileName.append(Util.reportGetDirForToday()).append("\\");
		fileName.append(fileNameParam);
		fileName.append("_");
		fileName.append(OCalendar.formatDateInt(OCalendar.today()));
		fileName.append("_");
		fileName.append(currentTime);
		fileName.append(".csv");
		
		return fileName.toString();
	}
	
	/**
	 * Send out a summary email based on the automatch output
	 * 
	 * @param attachmentPath
	 * @throws OException 
	 */
	private void sendEmail(String attachmentPath, String fileNameParam, int unmatchedRecords, int breakNotesToIgnore) throws OException
	{
		PluginLog.info("Attempting to send summary email (using configured Mail Service)..");
		
		Table tblInfo = null;
		
		try
		{
			String region = getRegion();
			ReconConfig constRepoConfig = new ReconConfig(region);
			
			String recipients = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_SUMMARY_EMAIL_RECIPIENTS);
			if (recipients == null || "".equalsIgnoreCase(recipients))
			{
				throw new ReconciliationRuntimeException("No recipient users found for Reconciliation summary email!");
			}
			
			EmailMessage mymessage = EmailMessage.create();
			
			/* Add subject and recipients */
			mymessage.addSubject("Endur / JDE - Reconciliation - " + fileNameParam + " - " + region);
			mymessage.addRecipients(recipients);
			
			StringBuilder builder = new StringBuilder();
			builder.append(region + " - This is an automated email message - please do not respond to this email.");
			builder.append("\n\n");
			
			/* Add environment details */
			tblInfo = com.olf.openjvs.Ref.getInfo();
			if (tblInfo != null)
			{
				builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
				builder.append(", on server: " + tblInfo.getString("server", 1));
				
				builder.append("\n\n");
			}
			
			builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			builder.append("\n\n");
			
			builder.append("Total of ").append(unmatchedRecords).append(" unmatched records found via Auto Match!");
			builder.append("\n\n");
			
			if (!ignoreBreakNotes)
			{
				builder.append(breakNotesToIgnore).append(" of these records are known issues and recorded under the break notes!");
				builder.append("\n\n");
				
				builder.append(unmatchedRecords - breakNotesToIgnore).append(" breaks require BAU investigation in Auto Match !");
				builder.append("\n\n");
				builder.append(attachmentPath);	
			}
			
			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			
			/* Add attachment */
			if (new File(attachmentPath).exists())
			{
				PluginLog.info("File attachmenent found: " + attachmentPath + ", attempting to attach to email..");
				mymessage.addAttachments(attachmentPath, 0, null);	
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			PluginLog.info("Email sent to: " + recipients);
		}
		catch (Exception e)
		{
			throw new ReconciliationRuntimeException("Unable to send Endur/JDE reconciliation Auto Match output email!", e);
		}
		finally
		{
			if (tblInfo != null)
			{
				tblInfo.destroy();	
			}
		}
	}	
	
	/**
	 * Get the Auto Match region parameter, if set  
	 * 
	 * @return
	 * @throws OException
	 */
	private String getRegion() throws OException
	{
		Table tblAutoMatchArgs = tblArgt.getTable("amr_parameter_table", 1);
		
		int findRow = tblAutoMatchArgs.unsortedFindString("parameter_name", "region", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		
		if (findRow > 0)
		{
			return tblAutoMatchArgs.getString("parameter_value", findRow);
		}
		
		throw new ReconciliationRuntimeException("Unable to find region parameter!");
	}
}
