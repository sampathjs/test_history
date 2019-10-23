package com.jm.eod.process;

/********************************************************************************
 * Script Name: AmendDealsHavingWrongTaxRate
 * Script Type: Main
 *  
 * Parameters : 
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1                  SonnyR01    Amending the deals where the tax amount is incorrect
 * 
 ********************************************************************************/

import java.io.File;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author SonnyR01
 * 
 */

@ScriptAttributes(allowNativeExceptions = false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class AmendDealsHavingWrongTaxRate implements IScript
{

	public AmendDealsHavingWrongTaxRate()
	{
	}

	public void execute(IContainerContext context) throws OException
	{

		try
		{

			PluginLog.init("INFO", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\", "EO.log");

		}
		catch (Exception e)
		{

			throw new RuntimeException(e);
		}

		String incorrectTax;

		Table amendedDeals = Table.tableNew();

		try
		{

			// Fetching the deals where the tax amount is incorrect

			incorrectTax = "SELECT DISTINCT ab.deal_tracking_num deal_num, \n " + " ab.tran_num , \n" + "ativ.value Metal_Price_Spread, \n" + "i.name Ins_Type, \n" + "ts.name Tran_Status, \n" + "tr.rate_name Tax_Type ,\n"
					+ "ate1.para_position Net_Amount,\n" + "it.effective_rate Correct_Tax_Rate , \n" + " (it.effective_rate * ate1.para_position) Correct_Tax_Amount, \n"
					+ "CAST(it.collection_date  as date) Tax_Payment_Date, c.name Tax_Currency, ate.para_position Actual_Tax_Amount,\n"
					+ "(CASE WHEN tr.add_subtract_id = 1 THEN ROUND( ( ate.para_position / ate1.para_position ), 5) ELSE -1 * ROUND(( ate.para_position / ate1.para_position ), 5) END) Actual_Tax_Rate \n" + " FROM   ab_tran ab \n"
					+ " JOIN ins_tax it ON (it.tran_num = ab.tran_num AND it.taxable_amount != 0 AND it.tax_status != 2) \n"
					+ " JOIN ab_tran_event ate ON (ate.tran_num = it.tran_num AND ate.ins_para_seq_num = it.param_seq_num AND ate.currency = it.tax_currency AND ate.event_type = 98 AND ate.ins_seq_num = it.tax_seq_num) \n"
					+ " JOIN ab_tran_event ate1 ON (ate1.tran_num = ate.tran_num AND ate1.ins_para_seq_num = ate.ins_para_seq_num AND ate1.currency = ate.currency AND ate1.event_type = 14 AND ate1.ins_seq_num = ate.ins_seq_num) \n"
					+ " JOIN tax_rate tr ON (tr.tax_rate_id = it.tax_rate_id) \n" + " JOIN trans_status ts ON (ts.trans_status_id = ab.tran_status) \n" + " JOIN currency c ON (c.id_number = it.tax_currency) \n"
					+ " JOIN Instruments i on  ( i.id_number = ab.ins_type) \n" + " LEFT JOIN ab_tran_info_view ativ on (ativ.tran_num = ab.tran_num and ativ.type_id = 20085)\n" + " WHERE  ab.tran_status IN ( 3 ) \n "
					+ " AND (CASE WHEN tr.add_subtract_id = 1 THEN ROUND( ( ate.para_position / ate1.para_position ), 6) ELSE -1 * ROUND(( ate.para_position / ate1.para_position ), 6) END) <> ROUND(it.effective_rate, 6) \n"
					+ " AND it.tax_currency IN ( 52, 57 )  AND ab.ins_type = 30201 \n" + " ORDER  BY ab.deal_tracking_num DESC ";

			DBaseTable.execISql(amendedDeals, incorrectTax);

		}

		catch (OException e)
		{

			PluginLog.error("Couldn't retrieve the transaction from the database ");
			Util.exitFail("Couldn't retrieve the transaction from the database " + e.getMessage());

		}

		try
		{

			if (amendedDeals.getNumRows() > 0)
			{
				PluginLog.info("Entries found " + amendedDeals.getNumRows());
				amendDeals(amendedDeals);
				sendEmail(amendedDeals);
			}

		}

		catch (OException e)
		{

			PluginLog.error("Couldn't amend the deals" + e.getMessage());
			Util.exitFail("Couldn't amend/send email for the deals" + e.getMessage());

		}

		finally
		{

			amendedDeals.destroy();
		}
	}

	/**
	 * Amending the deals having the incorrect tax amount
	 * 
	 * @param amendedDeals
	 * @throws OException
	 */
	private void amendDeals(Table amendedDeals) throws OException
	{
		PluginLog.info("Amending the formula deals having incorrect tax amount");

		int numRows = amendedDeals.getNumRows();
		int tranNum = 0;
		int dealNum = 0;
		int prevDealNum = 0;
		int retVal = 0;
		Transaction tran = Util.NULL_TRAN;

		try
		{

			for (int i = 1; i <= numRows; i++)
			{

				dealNum = amendedDeals.getInt("deal_num", i);

				tranNum = amendedDeals.getInt("tran_num", i);

				tran = Transaction.retrieve(tranNum);

				if (dealNum != prevDealNum)
				{

					retVal = tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);

				}

				if (retVal <= 0)
				{

					PluginLog.error("Failed to insert the transaction " + tranNum + "in the database");
					tran.destroy();
					prevDealNum = dealNum;
				}

				PluginLog.info("The deal " + dealNum + " was amended");

				prevDealNum = dealNum;

			}

		}

		catch (OException e)
		{

			PluginLog.error("Couldn't amend the transaction " + tranNum);
			throw new OException("Couldn't amend the transaction " + tranNum + " " + e.getMessage());

		}

		finally
		{

			if (Transaction.isNull(tran) != 0)
			{

				tran.destroy();
			}

		}

	}

	/**
	 * Sending the email to the mail recipients
	 * @param tblHistPrices
	 * @throws OException
	 */
	private void sendEmail(Table amendedDeals) throws OException
	{
		PluginLog.info("Attempting to send email (using configured Mail Service)..");

		Table envInfo = Util.NULL_TABLE;

		try
		{
			ConstRepository repository = new ConstRepository("Alerts", "TaxRate");

			StringBuilder sb = new StringBuilder();

			String recipients1 = repository.getStringValue("email_recipients1");

			sb.append(recipients1);
			String recipients2 = repository.getStringValue("email_recipients2");

			if (!recipients2.isEmpty() & !recipients2.equals(""))
			{

				sb.append(";");
				sb.append(recipients2);
			}

			EmailMessage mymessage = EmailMessage.create();

			/* Add subject and recipients */
			mymessage.addSubject("WARNING || These deals were amended because of wrong tax amount on them ");

			mymessage.addRecipients(sb.toString());

			StringBuilder builder = new StringBuilder();

			/* Add environment details */
			envInfo = com.olf.openjvs.Ref.getInfo();
			if (envInfo != null)
			{
				builder.append("This information has been generated from database: " + envInfo.getString("database", 1));
				builder.append(", on server: " + envInfo.getString("server", 1));

				builder.append("\n\n");
			}

			builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			builder.append("\n\n");

			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

			String strFilename;

			StringBuilder fileName = new StringBuilder();

			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];

			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append("AmendedDeals After the Tax Rate check");
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");

			strFilename = fileName.toString();

			amendedDeals.printTableDumpToFile(strFilename);

			/* Add attachment */
			if (new File(strFilename).exists())
			{
				PluginLog.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);
			}
			else
			{
				PluginLog.info("File attachmenent not found: " + strFilename);
			}

			mymessage.send("Mail");
			mymessage.dispose();

			PluginLog.info("Email sent to: " + recipients1);
		}
		catch (Exception e)
		{

			throw new OException("Unable to send output email! " + e.toString());
		}
		finally
		{
			if (Table.isTableValid(envInfo) == 1)
			{
				envInfo.destroy();
			}
		}
	}

}
