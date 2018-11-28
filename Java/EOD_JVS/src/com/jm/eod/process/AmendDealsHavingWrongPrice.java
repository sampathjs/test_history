package com.jm.eod.process;

import java.io.File;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class AmendDealsHavingWrongPrice implements IScript {

	public AmendDealsHavingWrongPrice() {
	}

	public void execute(IContainerContext context) throws OException {

		try {
			PluginLog.init("INFO", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\", this.getClass().getName() + ".log");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Table dealsTobeAmended = null;
		Table dealsAfterAmendment = null;
		
		try {
			// Fetching the deals where the settlement value is incorrect because of wrong prices present on deal
			dealsTobeAmended = fetchDealsHavingWrongSettlementVal();
			if (dealsTobeAmended == null || Table.isTableValid(dealsTobeAmended) != 1) {
				throw new OException("Invalid table data retrieved by executing SQL query");
			}
			
			int totalRows = dealsTobeAmended.getNumRows();
			if (totalRows > 0) {
				PluginLog.info("Num of deals having incorrect settlement value (that needs amendment) are->" + totalRows);
				amendDeals(dealsTobeAmended);
				sendEmail(dealsTobeAmended, true);
				
				//Query again after amending the deals
				dealsAfterAmendment = queryAmendedDealsAgainForIncorrectSettleVal(dealsTobeAmended);
				totalRows = dealsAfterAmendment.getNumRows();
				if (totalRows > 0) {
					PluginLog.info("After amendment, Num of deals still having incorrect settlement value->" + totalRows);
					sendEmail(dealsAfterAmendment, false);
				} else {
					PluginLog.info("After amendment, no deals are found to have wrong settlement value.");
				}
				
			} else {
				PluginLog.info("No deals having incorrect settlement value are found (that needs amendment).");
			}
			
		} catch (OException e) {
			String message = "Error in execute() method->" + e.getMessage();
			PluginLog.error(message);
			Util.exitFail(message);
			
		} finally {
			if (dealsTobeAmended != null && Table.isTableValid(dealsTobeAmended) == 1) {
				dealsTobeAmended.destroy();
			}
			
			if (dealsAfterAmendment != null && Table.isTableValid(dealsAfterAmendment) == 1) {
				dealsAfterAmendment.destroy();
			}
		}
	}

	private Table fetchDealsHavingWrongSettlementVal() throws OException {
		Table deals = Util.NULL_TABLE;
		
		try {
			deals = Table.tableNew();
			String sql = "SELECT ab.deal_tracking_num Deal_Num, "
					+ "\n ab.tran_num Tran_Num, "
					+ "\n i.name Ins_Type, "
					+ "\n ts.name Tran_Status, "
					+ "\n c.name Currency, "
					+ "\n rp.name Pay_Receive, "
					+ "\n es.actual_val Actual_Settlement_Val, "
					+ "\n (CASE WHEN es.pay_rec = 1 THEN (-1)*ujd.settlement_value ELSE ujd.settlement_value END) Correct_Settlement_Value"
					+ "\n FROM ab_tran ab"
					+ "\n JOIN (SELECT SUM(ate.para_position) actual_val, "
									+ "\n ate.tran_num, "
									+ "\n p1.pay_rec, "
									+ "\n ate.currency "
									+ "\n FROM ab_tran_event ate "
									+ "\n JOIN ab_tran ab ON (ate.tran_num = ab.tran_num AND ate.event_type = 14) "
									+ "\n JOIN parameter p1 ON (p1.ins_num = ab.ins_num AND p1.fx_flt = 1 AND p1.currency = ate.currency AND ate.ins_para_seq_num = p1.param_seq_num)  "
									+ "\n WHERE ab.tran_status IN (3) AND ab.ins_type = 30201 "
									+ "\n GROUP BY ate.tran_num, p1.pay_rec, ate.currency) es ON (es.tran_num = ab.tran_num)"
					+ "\n JOIN user_jm_jde_extract_data ujd ON (ujd.deal_num = ab.deal_tracking_num AND ujd.fixings_complete = 'Y' AND ujd.to_currency = es.currency)"
					+ "\n JOIN currency c ON (c.id_number = es.currency)"
					+ "\n JOIN trans_status ts ON (ts.trans_status_id = ab.tran_status)"
					+ "\n JOIN instruments i ON (i.id_number = ab.ins_type)"
					+ "\n JOIN rec_pay rp ON (rp.id_number = es.pay_rec)"
					+ "\n WHERE ab.tran_status IN (3) AND ab.ins_type = 30201 AND (CASE WHEN es.pay_rec = 1 THEN (-1)*ROUND(ujd.settlement_value, 6) ELSE ROUND(ujd.settlement_value, 6) END) <> ROUND(es.actual_val, 6)"
					+ "\n ORDER BY ab.deal_tracking_num DESC";

			PluginLog.info("Executing SQL->" + sql);
			DBaseTable.execISql(deals, sql);
			
			if (Table.isTableValid(deals) == 1) {
				PluginLog.info("Num of deals fetched having wrong settlement value are " + deals.getNumRows());
				return deals;
			}
			
		} catch (OException oe) {
			PluginLog.error("Error in executing fetchDealsHavingWrongSettlementVal()->" + oe.getMessage());
			deals.destroy();
			throw oe;
		}
		
		return null;
	}
	
	private Table queryAmendedDealsAgainForIncorrectSettleVal(Table tAmendedDeals) throws OException {
		Table deals = Util.NULL_TABLE;
		int qId = -1;
		
		try {
			qId = Query.tableQueryInsert(tAmendedDeals, "Deal_Num");
			deals = Table.tableNew();
			String sql = "SELECT ab.deal_tracking_num Deal_Num, "
					+ "\n ab.tran_num Tran_Num, "
					+ "\n i.name Ins_Type, "
					+ "\n ts.name Tran_Status, "
					+ "\n c.name Currency, "
					+ "\n rp.name Pay_Receive, "
					+ "\n es.actual_val Actual_Settlement_Val, "
					+ "\n (CASE WHEN es.pay_rec = 1 THEN (-1)*ujd.settlement_value ELSE ujd.settlement_value END) Correct_Settlement_Value"
					+ "\n FROM ab_tran ab"
					+ "\n JOIN (SELECT SUM(ate.para_position) actual_val, "
									+ "\n ate.tran_num, "
									+ "\n p1.pay_rec, "
									+ "\n ate.currency "
									+ "\n FROM ab_tran_event ate "
									+ "\n JOIN ab_tran ab ON (ate.tran_num = ab.tran_num AND ate.event_type = 14) "
									+ "\n JOIN parameter p1 ON (p1.ins_num = ab.ins_num AND p1.fx_flt = 1 AND p1.currency = ate.currency AND ate.ins_para_seq_num = p1.param_seq_num)  "
									+ "\n WHERE ab.tran_status IN (3) AND ab.ins_type = 30201 "
									+ "\n GROUP BY ate.tran_num, p1.pay_rec, ate.currency) es ON (es.tran_num = ab.tran_num)"
					+ "\n JOIN user_jm_jde_extract_data ujd ON (ujd.deal_num = ab.deal_tracking_num AND ujd.fixings_complete = 'Y' AND ujd.to_currency = es.currency)"
					+ "\n JOIN currency c ON (c.id_number = es.currency)"
					+ "\n JOIN trans_status ts ON (ts.trans_status_id = ab.tran_status)"
					+ "\n JOIN instruments i ON (i.id_number = ab.ins_type)"
					+ "\n JOIN rec_pay rp ON (rp.id_number = es.pay_rec)"
					+ "\n JOIN " + Query.getResultTableForId(qId) + " q ON (q.query_result = ab.deal_tracking_num AND q.unique_id = " + qId + ")"
					+ "\n WHERE ab.tran_status IN (3) AND ab.ins_type = 30201 AND (CASE WHEN es.pay_rec = 1 THEN (-1)*ROUND(ujd.settlement_value, 6) ELSE ROUND(ujd.settlement_value, 6) END) <> ROUND(es.actual_val, 6)"
					+ "\n ORDER BY ab.deal_tracking_num DESC";

			PluginLog.info("Executing SQL->" + sql);
			DBaseTable.execISql(deals, sql);
			
			if (Table.isTableValid(deals) == 1) {
				PluginLog.info("Num of amended deals (that requires SUPPORT attention) still having wrong "
						+ "settlement value are " + deals.getNumRows());
				return deals;
			}
			
		} catch (OException oe) {
			PluginLog.error("Error in executing queryAmendedDealsAgainForIncorrectSettleVal()->" + oe.getMessage());
			deals.destroy();
			throw oe;
			
		} finally {
			if (qId > 0) {
				Query.clear(qId);
			}
		}
		
		return null;
	}

	/**
	 * Amending the deals having the wrong settlement value
	 * 
	 * @param amendedDeals
	 * @throws OException
	 */
	private void amendDeals(Table amendedDeals) throws OException {
		PluginLog.info("Amending the deals having incorrect settlement value");
		int numRows = amendedDeals.getNumRows();
		int tranNum = 0;
		int dealNum = 0;
		int prevDealNum = 0;
		int retVal = 0;
		Transaction tran = Util.NULL_TRAN;

		try {
			for (int i = 1; i <= numRows; i++) {
				dealNum = amendedDeals.getInt("Deal_Num", i);
				tranNum = amendedDeals.getInt("Tran_Num", i);
				tran = Transaction.retrieve(tranNum);

				try {
					if (dealNum != prevDealNum) {
						retVal = tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);
					}
				} finally {
					if (Transaction.isNull(tran) == 0) {
						tran.destroy();
					}
				}

				if (retVal <= 0) {
					PluginLog.error("Failed to insert the transaction " + tranNum + " in the database");
					tran.destroy();
					prevDealNum = dealNum;
				}

				PluginLog.info("The deal " + dealNum + " was amended");
				prevDealNum = dealNum;
			}
			
		} catch (OException e) {
			PluginLog.error("Couldn't amend the transaction " + tranNum);
			throw new OException("Couldn't amend the transaction " + tranNum
					+ " " + e.getMessage());
		}
	}

	/**
	 * Sending the email to the mail recipients.
	 * 
	 * @param tblHistPrices
	 * @throws OException
	 */
	private void sendEmail(Table amendedDeals, boolean forDealAmend) throws OException {
		PluginLog.info("Attempting to send email (using configured Mail Service)..");
		Table envInfo = Util.NULL_TABLE;

		try {
			ConstRepository repository = new ConstRepository("Alerts", "SettlementPriceCheck");
			
			StringBuilder sb = new StringBuilder();
			String recipients1 = repository.getStringValue("email_recipients1");
			sb.append(recipients1);
			String recipients2 = repository.getStringValue("email_recipients2");
			if (!recipients2.isEmpty() & !recipients2.equals("")) {
				sb.append(";");
				sb.append(recipients2);
			}

			EmailMessage mymessage = EmailMessage.create();
			/* Add subject and recipients */
			mymessage.addSubject(getEmailSubject(forDealAmend));
			mymessage.addRecipients(sb.toString());

			StringBuilder builder = new StringBuilder();
			/* Add environment details */
			envInfo = com.olf.openjvs.Ref.getInfo();
			if (envInfo != null) {
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
			fileName.append(getFileName(forDealAmend));
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");

			strFilename = fileName.toString();
			amendedDeals.printTableDumpToFile(strFilename);

			/* Add attachment */
			if (new File(strFilename).exists()) {
				PluginLog.info("File attachment found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);
			} else {
				PluginLog.info("File attachment not found: " + strFilename);
			}

			mymessage.send("Mail");
			mymessage.dispose();

			PluginLog.info("Email sent to: " + recipients1);
			
		} catch (Exception e) {
			throw new OException("Unable to send output email! " + e.toString());
			
		} finally {
			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
		}
	}
	
	private String getEmailSubject(boolean forDealAmend) {
		String subject = null;
		
		if (forDealAmend) {
			subject = "WARNING || These deals were amended because of difference in GL and SL values present on deal & USER_jm_jde_extract_data table";
		} else {
			subject = "WARNING || These amended deals require SUPPORT team attention - still having difference in GL and SL values";
		}
		
		return subject;
	}
	
	private String getFileName(boolean forDealAmend) {
		String fileName = null;
		
		if (forDealAmend) {
			fileName = "AmendedDeals_After_Settlement_Value_Check";
		} else {
			fileName = "AmendedDeals_Requiring_SUPPORT_Team_Attention";
		}
		
		return fileName;
	}

}

