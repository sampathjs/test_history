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
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.recon.enums.EndurAutoMatchStatus;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.ReconConfig;
import com.olf.jm.logging.Logging;

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
		com.olf.recon.utils.Util.initialiseLog();
		
		Table tblOutputData = null;
	
		try
		{
			ignoreBreakNotes = canIgnoreBreakNotes();
			Logging.debug("Can ignore break notes? " + ignoreBreakNotes);
			
			/* This parameter specifies the prefix name of the file - configured in the Auto Match Definition builder */
			String fileNameParam = tblArgt.getString("amr_action_param1", 1);
			if (fileNameParam.equalsIgnoreCase(""))
			{
				throw new ReconciliationRuntimeException("Please specify 'amr_action_param1' in the Auto Match config - this is the filename");
			}
			
			String absoluteFilePath = getFilePath(fileNameParam);
			Logging.debug("Absolute filepath constructed as: " + absoluteFilePath);
			
			/* This Auto Match table contains all the comparison results */
			Table tblActionData = tblArgt.getTable("amr_action_data", 1);
			
			/* 
			 * Manipulate output data (on a copy)
			 * 1. First row is an aggregated row for monetary amounts - not interested in this so remove
			 * 2. Remove the comparison columns as not interested in reporting these, they're sub tables
			 */
			tblOutputData = tblActionData.copyTable();
			if (tblOutputData.getNumRows() > 0) tblOutputData.deleteWhereValue(1, 0);
			extractMatchStatus(tblOutputData);			
			if (tblOutputData.getColNum("_pdata") > 0) tblOutputData.delCol("_pdata");
			if (tblOutputData.getColNum("_cdata") > 0) tblOutputData.delCol("_cdata");

			/* Remove any rows that are not "Unmatched" */
			removeNonUnmatchedData(tblOutputData);
			
			/* Ignore break notes */
			ignoreBreakNotes(tblOutputData);

			/* Dump file */
			tblOutputData.printTableDumpToFile(absoluteFilePath);
			Logging.info("Reconciliation data dumped to: " + absoluteFilePath);
			
			/* Any records that have a break note can be ignored as these are known defects */
			int numUnmatchedRows = tblOutputData.getNumRows();
			int breaksCanIgnore = 0;
			for (int row = 1; row <= numUnmatchedRows; row++)
			{
				String breakNote = tblOutputData.getString("reconciliation_note", row);
				
				if (breakNote != null && breakNote.length() > 1)
				{
					breaksCanIgnore++;
				}
			}
			
			sendEmail(absoluteFilePath, fileNameParam, numUnmatchedRows, breaksCanIgnore);
		}
		catch (Exception e)
		{
			String message = "Error encountered during email output of reconciliation: " + e.getMessage();
			Logging.info(message);
			Logging.error(message);
		}
		finally
		{
			Logging.close();
			if (tblOutputData != null)
			{
				tblOutputData.destroy();
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
			Logging.info("Missing parameter " + Constants.CONST_REPO_VARIABLE_IGNORE_BREAK_NOTES_IN_EMAIL + " for " + getRegion());
		}
		
		/* Default case */
		return ignoreBreakNotes;
	}
	
	/**
	 * If ignore break notes then the email output should remove these from the csv file. These
	 * are breaks that are known/recorded and thus can be ignored for reporting purposes
	 * 
	 * @param tblOutputData
	 * @throws OException
	 */
	private void ignoreBreakNotes(Table tblOutputData) throws OException 
	{
		if (ignoreBreakNotes)
		{				
			for (int row = tblOutputData.getNumRows(); row >= 1; row--)
			{
				String reconciliationNote = tblOutputData.getString("reconciliation_note", row);
				
				if (reconciliationNote != null && reconciliationNote.length() > 1)
				{
					tblOutputData.delRow(row);
				}
			}
		}
	}
	
	/**
	 * Construct the file path for the output CSV data
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
		Logging.info("Attempting to send summary email (using configured Mail Service)..");
		
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
				Logging.info("File attachmenent found: " + attachmentPath + ", attempting to attach to email..");
				mymessage.addAttachments(attachmentPath, 0, null);	
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			Logging.info("Email sent to: " + recipients);
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

	/**
	 * The match status for each row is nested inside an Auto Match subtable.
	 * This functions extracts this status fields and stamps it outside of the subtable
	 * 
	 * @param tblOutputData
	 * @throws OException
	 */
	private void extractMatchStatus(Table tblOutputData) throws OException 
	{
		Logging.info("Extracting match status per record..");
		
		tblOutputData.addCol("match_status", COL_TYPE_ENUM.COL_INT);
		tblOutputData.setColTitle("match_status", "Match Status");
		
		int numRows = tblOutputData.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
			Table tblMatchData = tblOutputData.getTable("_pdata", row);
			if (tblMatchData == null) tblMatchData = tblOutputData.getTable("_cdata", row);
			
			if (tblMatchData == null)
			{
				continue;
			}
			
			int matchStatus = tblMatchData.getInt("_ms", 1);
			tblOutputData.setInt("match_status", row, matchStatus);
		}
	}
	
	/**
	 * Remove any rows that are _not_ "Unmatched" as we are only
	 * interested in reporting Unmatched rows to end users
	 * 
	 * @param tblOutputData
	 * @throws OException
	 */
	private void removeNonUnmatchedData(Table tblOutputData) throws OException 
	{
		Logging.info("Removing non Unmatched rows from the output..");
		
		for (int row = tblOutputData.getNumRows(); row >= 1; row--)
		{
			int matchStatus = tblOutputData.getInt("match_status", row);
			
			//if (matchStatus != EndurAutoMatchStatus.UNMATCHED.toInt())
			if (matchStatus != EndurAutoMatchStatus.UNMATCHED.toInt() && matchStatus != EndurAutoMatchStatus.MATCEHD_DUPLICATE.toInt())
			{
				tblOutputData.delRow(row);
			}
		}
		
		tblOutputData.delCol("match_status");
	}
}
