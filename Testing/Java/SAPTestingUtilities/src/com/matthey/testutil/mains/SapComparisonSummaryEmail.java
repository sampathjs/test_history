package com.matthey.testutil.mains;

import java.io.File;

import com.matthey.testutil.BaseScript;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.enums.ValidationResults;
import com.matthey.testutil.exception.SapTestUtilException;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;

/**
 * Utility to send e-mail of comparison report
 * @author SharmV04
 *
 */
public class SapComparisonSummaryEmail extends BaseScript
{
	private String baselinePath;
	private String actualOutputFilePath;
	private String comparisonOutputFilePath;
	private String emailSubject;
	private String emailRecipients;
	private boolean isKeepAlwaysUpdated;

	@Override
	public void execute(IContainerContext context) throws OException
	{
		String keepAlwaysUpdatedString;
		try
		{
			Util.setupLog();
			Logging.info("Started executing " + this.getClass().getSimpleName());
			
			Table tblArgt = context.getArgumentsTable();

			Logging.debug("Argument table:");
			Util.printTableOnLogTable(tblArgt);
			
			baselinePath = tblArgt.getString("summary_email_baseline_path", 1);
			String actualOutputFolderPath = tblArgt.getString("summary_email_actual_folder", 1);
			actualOutputFolderPath = Util.getAbsolutePath(actualOutputFolderPath);
			actualOutputFilePath = com.matthey.testutil.common.Util.getLatestFilePath(actualOutputFolderPath);

			String comparisonOutputFolderPath = tblArgt.getString("summary_email_comparison_folder", 1);
			comparisonOutputFolderPath = Util.getAbsolutePath(comparisonOutputFolderPath);
			comparisonOutputFilePath = com.matthey.testutil.common.Util.getLatestFilePath(comparisonOutputFolderPath);

			emailSubject = tblArgt.getString("summary_email_subject", 1);
			emailRecipients = tblArgt.getString("summary_email_recipients", 1);
			keepAlwaysUpdatedString = tblArgt.getString("keep_always_updated", 1);
			Logging.debug("keep_always_updated=" + keepAlwaysUpdatedString);
			isKeepAlwaysUpdated = Boolean.valueOf(keepAlwaysUpdatedString);

			sendEmail();
		}
		catch (Exception e)
		{
			Util.printStackTrace(e);
			throw new SapTestUtilRuntimeException("Unable to send summary email", e);
		}
		finally
		{
			Logging.info("Started executing " + this.getClass().getSimpleName());
		}
	}

	/**
	 * @throws OException
	 * @throws SapTestUtilException
	 */
	private void sendEmail() throws OException, SapTestUtilException
	{
		Logging.info("Attempting to send summary email (using configured Mail Service)..");

		Table tblInfo = null;

		try
		{
			boolean anyDiscrepencies = doDiscrepenciesExist();
			if (anyDiscrepencies || isKeepAlwaysUpdated)
			{
				emailSubject += (anyDiscrepencies) ? " - Errors exist in baseline comparison" : " - all items fully matched";

				EmailMessage mymessage = EmailMessage.create();

				/* Add subject and recipients */
				mymessage.addSubject(emailSubject);
				mymessage.addRecipients(emailRecipients);

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

				builder.append("Baseline path: ").append(baselinePath);
				builder.append("\n");
				builder.append("Actual output path: ").append(actualOutputFilePath);

				mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

				/* Add attachment */
				if (new File(comparisonOutputFilePath).exists())
				{
					Logging.info("File attachmenent found: " + comparisonOutputFilePath + ", attempting to attach to email..");
					mymessage.addAttachments(comparisonOutputFilePath, 0, null);
				}

				mymessage.send("Mail");
				mymessage.dispose();

				Logging.info("Email sent to: " + emailRecipients);
			}
			else
			{
				Logging.debug("Not sending email because keep_always_updated was false and no unmatch found between " + baselinePath + " and " + actualOutputFilePath);
			}
		}
		catch (Exception e)
		{
			Util.printStackTrace(e);
			throw new SapTestUtilException("Unable to send summary output email!", e);
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
	 * @return true if there is a no <b>Matching</b> in one or more rows of comparison result table 
	 * @throws OException
	 */
	private boolean doDiscrepenciesExist() throws OException
	{
		Table tblCsvData = Table.tableNew();

		int ret = tblCsvData.inputFromCSVFile(comparisonOutputFilePath);

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + actualOutputFilePath);
		}

		/* Fix header column names! */
		com.matthey.testutil.common.Util.updateTableWithColumnNames(tblCsvData);

		if (tblCsvData.getColNum("validation_result") > 0)
		{
			int numRows = tblCsvData.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				String validationResult = tblCsvData.getString("validation_result", row);
				if (validationResult != null && !ValidationResults.MATCHING.toString().equalsIgnoreCase(validationResult))
				{
					return true;
				}
			}
		}

		return false;
	}
}
