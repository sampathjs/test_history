package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.metalstransfer.utils.TransfersValidationSql;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.sc.bo.docproc.BO_CommonLogic.Query;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.misc.TableUtilities;
import com.olf.jm.logging.Logging;

/*This tasks fetches all strategy having issues as below:
 * Strategy is Validated, Cash is Cancelled
 * Strategy is Deleted, Cash is Validated
 * Strategy is New, Cash deal does not exist
 * Strategy is New, Cash is Validated
 * and all strategy where expected cash deals are not generated.
 * Functionality: scripts pulls all the strategy and mark the status as "Pending" in User table. 
 */
public class ReprocessValidationFailures implements IScript {
	private static int qid = 0;
	private static final String status = "Pending";
	private String retry_limit;
	ConstRepository _constRepo;
	int iReportingStartDate;
	String bufferTime;
	String timeWindow;

	public void execute(IContainerContext context) throws OException {
		try {
			init();
			Logging.info("Inserting deals to be processed in query_result");
			qid = getQueryID();
			if (qid > 0) {
				Table endurExtract = fetchDataForAllStrategies(qid);
				filterValidationErrors(endurExtract);
				endurExtract = TableUtilities.destroy(endurExtract);
			}

		} catch (Exception e) {
			Logging.error("Unable to process data and report for invalid strategy \n" + e.getMessage(), e, false);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			Util.exitFail();
		} finally {
			Query.clear(qid);
			Logging.close();
		}

	}

	// takes qid as input and fetches all data from Endur for processing
	private Table fetchDataForAllStrategies(int qid) throws OException {
		Table endurExtract = null;
		String sql = null;
		try {
			sql = TransfersValidationSql.fetchDataForAllStrategies(qid);
			endurExtract = getData(sql);
		} catch (Exception e) {
			String errMsg = "Unable to execute Sql. \n" + sql + "\n" + e.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		}
		return endurExtract;

	}

	// Initialise Const repo variables and log file
	private void init() throws OException {
		try {
			Logging.init(this.getClass(), "Alerts", "TransferValidation");
			_constRepo = new ConstRepository("Alerts", "TransferValidation");
			bufferTime = _constRepo.getStringValue("bufferTime");
			Logging.info("bufferTime  is " + bufferTime + " minutes configured in User_const_repository");
			retry_limit = _constRepo.getStringValue("retry_limit");
			Logging.info("Limit for retry is " + retry_limit + " configured in User_const_repository");
			// Logging.info("Deals to be excluded from reporting are "+
			// strExcludedTrans+ " configured in User_const_repository");
			iReportingStartDate = _constRepo.getDateValue("reporting_start_date");
			Logging.info("reporting start date is  " + iReportingStartDate + " configured in User_const_repository");
			timeWindow = _constRepo.getStringValue("timeWindow");
			Logging.info("Deals booked for " + timeWindow
					+ " days will be considered in reporting, configured in User_const_repository");
		} catch (OException e) {
			String errMsg = "Unable to initialize const repository variables. \n" + e.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		}

	}

	/*
	 * In case issues are reported with retry_count less than 3, this process
	 * will update status to "pending" in user_strategy_deals and allow intraday
	 * task to be re-trigger for the deal.
	 */
	private void processStamping(Table finalDataToProcess) throws OException {
		Table stampData = null;
		try {
			stampData = Table.tableNew("stampData");
			String what = "deal_num,tran_num,tran_status,status,last_updated,version_number,retry_count";
			finalDataToProcess.delCol("Description");
			stampData.setTableName("USER_strategy_deals");
			int retval = DBUserTable.structure(stampData);
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.structure() failed"));
			}
			stampData.select(finalDataToProcess, what, "retry_count LT " + retry_limit);
			if (stampData.getNumRows() <= 0) {
				Logging.info("No issues were found for reprocessing.");
			} else {
				process(stampData);
				Logging.info(
						" status updated to 'Pending' user_strategy_deals for reprocessing the reported strategy deals.");
			}
		} catch (OException oe) {
			String errMsg = "Unable to update data in user table \n " + oe.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		} finally {
			finalDataToProcess = TableUtilities.destroy(finalDataToProcess);
		}
	}

	/*
	 * In case issues are reported with retry_count more than 2, this process
	 * will send email to recipients configured in Const_repository asking for
	 * manual validation and allow intraday task to be re-trigger for the deal.
	 */
	private void processReporting(Table finalDataToProcess) throws OException {
		Table reportData = null;
		try {
			reportData = Table.tableNew("Report Data");
			// reportData.select(finalDataToProcess, "*", "retry_count GE "+
			// retry_limit);
			if (reportData.getNumRows() <= 0) {
				Logging.info("No issues were found for email reporting");
			} else {
				Logging.info(finalDataToProcess.getNumRows() + " issues were found for email reporting.");
				reportData.setColFormatAsRef("tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
				Logging.info(
						"Sending mail to configured users in const repository ('Alerts','TransferValidation','emailRecipients')");
				emailToUser(reportData);
			}
		} catch (OException oe) {
			String errMsg = "Unable to send mail \n " + oe.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		} finally {
			reportData = TableUtilities.destroy(reportData);
		}
	}

	/*
	 * Fetches all deals to be reconcilled for the systLimitDate configured in
	 * const_repository insert in query_result and returns the qid.
	 */
	private int getQueryID() throws OException {
		Table dataToProcess = null;
		try {
			String symtLimitDate = _constRepo.getStringValue("symtLimitDate", "-1m");
			if (symtLimitDate == null || "".equals(symtLimitDate)) {
				throw new OException("Ivalid value in Const Repository");
			}
			dataToProcess = Table.tableNew("dataToProcess");
			String Sql = TransfersValidationSql.strategyForValidation(symtLimitDate);
			dataToProcess = getData(Sql);
			if (dataToProcess.getNumRows() > 0) {
				qid = Query.tableQueryInsert(dataToProcess, 1);
				Logging.info("Query Id is " + qid);
			}
		} catch (OException oe) {
			String errMsg = "Table query insert failed. \n" + oe.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		} finally {
			dataToProcess = TableUtilities.destroy(dataToProcess);
		}
		return qid;
	}

	/*
	 * Function checks data for validation issues as per defined conditions
	 * below and returns the table having error to be processed. 1. When
	 * strategy is DELETED and Cash Deal is in validated state. 2. When strategy
	 * is Cancelled and Cash Deal is in validated state. 3. When strategy is in
	 * validated and Cash deal count is not less than 2 for validated deals. 4.
	 * When strategy is in New and Cash deal is available. 5. When strategy is
	 * in New and No cash deal is created but the user_strategy_deals is updated
	 * with Succeeded status. 6. When generated cash deals are less than
	 * expected, either the cash deals are not generated or Tax deals are not
	 * generated.
	 */
	private void filterValidationErrors(Table reportData) throws OException {
		Table validationForTaxData = null;
		Table filterValidationIssues = null;

		try {
			if (reportData.getNumRows() > 0) {
				filterValidationIssues = reportData.cloneTable();
				filterValidationIssues.addCol("Description", COL_TYPE_ENUM.COL_STRING);
				int rowCount = reportData.getNumRows();
				int finalRow = 0;
				for (int row = 1; row <= rowCount; row++) {
					
					int StrategyDealNum = reportData.getInt("StrategyDealNum", row);
					int strategyStatus = reportData.getInt("StrategyTranStatus", row);
					int cashTranStatus = reportData.getInt("CashTranStatus", row);
					int cashDealCount = reportData.getInt("CountOfCashDeal", row);
					String processStatus = reportData.getString("status", row);
					String reason = null;

					if (TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt() == strategyStatus
							&& TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == cashTranStatus) {
						reason = "Strategies found where deal is in deleted state and relevant cash deal is in Validate state";
						Logging.info(StrategyDealNum +" was found with reason "+reason);

					} else if (TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() == strategyStatus
							&& TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == cashTranStatus) {
						reason = "Strategies found where deal is in cancelled state and relevant cash deal is in Validate state";
						Logging.info(StrategyDealNum +" was found with reason "+reason);

					} else if (TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == strategyStatus
							&& (TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == cashTranStatus
									|| TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt() == cashTranStatus)
							&& cashDealCount < 2) {
						reason = "When strategy is in validated or Matured and Cash deal count is less than 2 for validated cash deals";
						Logging.info(StrategyDealNum +" was found with reason "+reason);

					} else if (TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() == strategyStatus
							&& TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == cashTranStatus) {
						reason = "When strategy is in New and Cash deal is available";
						Logging.info(StrategyDealNum +" was found with reason "+reason);

					} else if (TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() == strategyStatus
							&& TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() != cashTranStatus
							&& "Succeeded" == processStatus) {
						reason = "When strategy is in New and No cash deal is created but the user_strategy_deals is updated with Succeeded status";
						Logging.info(StrategyDealNum +" was found with reason "+reason);

					}

					if (reason != null) {
						int retval = reportData.copyRowAdd(row, filterValidationIssues);
						if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
							Logging.error("Failed to merge validationIssues in final data to be processed.");
						}
						filterValidationIssues.setString("Description", ++finalRow, reason);
					}
				}
				filterValidationIssues.delCol("StrategyDealNum");
				filterValidationIssues.delCol("StrategyTranStatus");
				filterValidationIssues.delCol("CountOfCashDeal");
				filterValidationIssues.delCol("CashTranStatus");
				processReporting(filterValidationIssues);
				if (filterValidationIssues.getNumRows() > 0){
					Logging.info("Sending emails to user for validation issues ");
					emailToUser(filterValidationIssues);
				}
			}
			// Case 6: When generated cash deals are less than expected, either
			// the cash deals are not generated or Tax deals are not generated

			String validationForTax = TransfersValidationSql.checkForTaxDeals(qid, bufferTime);
			validationForTaxData = getData(validationForTax);
			int taxIssuesCount = validationForTaxData.getNumRows();
			if (taxIssuesCount > 0) {
				String reason = "Expected and Actual cash deals count is not matching";
				Logging.info(taxIssuesCount + " " + reason);
				Logging.info("Sending emails to user for tax issues ");
				emailToUser(validationForTaxData);
			}
			Logging.info(taxIssuesCount + " tax issues were found for reporting and reprocessing.");
		} catch (Exception e) {
			String errMsg = "Unable to process data and report for invalid strategy \n" + e.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		} finally {
			validationForTaxData = TableUtilities.destroy(validationForTaxData);
			filterValidationIssues = TableUtilities.destroy(filterValidationIssues);
		}

	}

	// Sends mail to defined user
	private void emailToUser(Table reportData) throws OException {
		String mailServiceName = "Mail";
		try {
			String reciever = _constRepo.getStringValue("emailRecipients");
			String FileName = _constRepo.getStringValue("FileName");
			String emailId = com.matthey.utilities.Utils.convertUserNamesToEmailList(reciever);
			String message = getEmailBody();
			String subject = _constRepo.getStringValue("mailSubject");
			String fileToAttach = com.matthey.utilities.FileUtils.getFilePath(FileName);
			reportData.printTableDumpToFile(fileToAttach);
			boolean ret = com.matthey.utilities.Utils.sendEmail(emailId, subject, message, fileToAttach,
					mailServiceName);
			if (!ret) {
				Logging.error("Failed to send alert for invalid strategy deals \n");
			}
			Logging.info("Mail is successfully sent to " + emailId + " and report contains " + reportData.getNumRows()
					+ " strategy deals ");

		} catch (OException e) {
			String errMsg = "Unable to send mail to users \n" + e.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		}

	}

	private String getEmailBody() throws OException {
		String emailBodyText = _constRepo.getStringValue("emailBodyText");
		String emailBodyText1 = _constRepo.getStringValue("emailBodyText1");
		String body = emailBodyText + " " + OCalendar.formatDateInt(Util.getTradingDate()) + " " + emailBodyText1;

		return "<html> \n\r" + "<head><title> </title></head> \n\r" + "<p> Hi all,</p>\n\n" + "<p> " + body + "</p>\n"
				+ "<p>\n Thanks </p>" + "<p>\n GRP Endur Support</p></body> \n\r" + "<html> \n\r";
	}

	private void process(Table stampData) throws OException {
		int numRows;
		try {
			numRows = stampData.getNumRows();
			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			for (int row = 1; row <= numRows; row++) {
				int tranNum = stampData.getInt("tran_num", row);
				Logging.info("Working on " + tranNum + " stamping status to 'Pending'\n");
				int retry_count = stampData.getInt("retry_count", row);
				stampData.setString("status", row, status);
				stampData.setInt("retry_count", row, retry_count + 1);
				stampData.setDateTime("last_updated", 1, extractDateTime);
			}
			stampData.group("deal_num,tran_num, tran_status");
			DBUserTable.update(stampData);
		} catch (OException e) {
			String errMsg = "Error while updating user table \n" + e.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		} finally {
			stampData = TableUtilities.destroy(stampData);
		}
	}

	private Table getData(String sql) throws OException {
		Table transfersToReprocess = null;
		try {
			transfersToReprocess = Table.tableNew(sql);
			Logging.info("Executing sql \n " + sql);
			int ret = DBaseTable.execISql(transfersToReprocess, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error("Failed to execute sql \n" + sql);
			}
		} catch (OException oe) {
			String errMsg = "Unable to execute data from database, executing \n" + oe.getMessage();
			Logging.error(errMsg);
			throw new OException(errMsg);
		}
		return transfersToReprocess;
	}

}